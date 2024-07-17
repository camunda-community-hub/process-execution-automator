/* ******************************************************************** */
/*                                                                      */
/*  RunScenarioFlowStartEvent                                           */
/*                                                                      */
/*  Create process instance in Flow                                     */
/* ******************************************************************** */
package org.camunda.automator.engine.flow;

import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class RunScenarioFlowStartEvent extends RunScenarioFlowBasic {
  private final TaskScheduler scheduler;
  Logger logger = LoggerFactory.getLogger(RunScenarioFlowStartEvent.class);
  private boolean stopping;
  private boolean isRunning;
  /**
   * Each time we run a batch of start, execution Number increase
   */
  private int executionBatchNumber = 1;

  public RunScenarioFlowStartEvent(TaskScheduler scheduler,
                                   ScenarioStep scenarioStep,
                                   RunScenario runScenario,
                                   RunResult runResult) {
    super(scenarioStep, runScenario, runResult);
    this.scheduler = scheduler;
  }
  @Override
  public String getTopic() {
    return getScenarioStep().getTaskId();
  }


  @Override
  public String getTopic() {
    return getScenarioStep().getTaskId();
  }

  StartEventRunnable startEventRunnable;

  @Override
  public void execute() {
    stopping = false;
    isRunning = true;

    startEventRunnable = new StartEventRunnable(scheduler, getScenarioStep(), getRunScenario(), this, runResult);

    startEventRunnable.start();
  }

  @Override
  public void pleaseStop() {
    this.stopping = true;
  }

  public RunScenarioFlowBasic.STATUS getStatus() {
    if (!isRunning)
      return RunScenarioFlowBasic.STATUS.STOPPED;
    if (stopping) {
      return RunScenarioFlowBasic.STATUS.STOPPING;
    }
    return RunScenarioFlowBasic.STATUS.RUNNING;
  }

  @Override
  public int getCurrentNumberOfThreads() {
    try {
      return startEventRunnable == null ? 0 : startEventRunnable.getNumberOfRunningThreads();
    } catch (Exception e) {
      // do nothing
      logger.error("During getCurrentNumberOfThreads : {}", e);
      return 0;
    }
  }

  @Override
  public RunResult getRunResult() {
    return runResult;
  }

  public enum STATUS {RUNNING, STOPPING, STOPPED}

  /**
   * StartEventRunnable
   */
  class StartEventRunnable implements Runnable {

    private final TaskScheduler scheduler;
    private final ScenarioStep scenarioStep;
    private final RunResult runResult;
    private final RunScenario runScenario;
    private final RunScenarioFlowStartEvent flowStartEvent;

    private int nbOverloaded = 0;
    private int totalCreation = 0;
    private int totalFailed = 0;

    private CreateProcessInstanceThread createProcessInstanceThread = null;

    public StartEventRunnable(TaskScheduler scheduler,
                              ScenarioStep scenarioStep,
                              RunScenario runScenario,
                              RunScenarioFlowStartEvent flowStartEvent,
                              RunResult runResult) {
      this.scheduler = scheduler;
      this.scenarioStep = scenarioStep;
      this.runResult = runResult;
      this.runScenario = runScenario;
      this.flowStartEvent = flowStartEvent;
    }

    /**
     * Start it in a new tread
     */
    public void start() {
      scheduler.schedule(this, Instant.now());
    }

    @Override
    public void run() {
      if (flowStartEvent.stopping) {
        if (runScenario.getRunParameters().showLevelMonitoring()) {
          logger.info("Stop now [" + getId() + "]");
          if (nbOverloaded > 0)
            runResult.addError(scenarioStep,

                "Overloaded:" + "" + nbOverloaded + " TotalCreation:" + totalCreation // total creation we see
                    + " TheoricNumberExpectred:" + (scenarioStep.getNumberOfExecutions() * executionBatchNumber)
                    // expected

                    + " Process[" + scenarioStep.getProcessId() + "] Can't create PI at the required frequency");
          if (totalFailed > 0)
            runResult.addError(scenarioStep,
                "Failed " + totalFailed + " Process[" + scenarioStep.getProcessId() + "] Can't create PI ");

        }
        // notify my parent that I stop now
        flowStartEvent.isRunning = false;
        return;
      }
      executionBatchNumber++;

      Duration durationToCreateProcessInstances = Duration.parse(scenarioStep.getFrequency());

      long begin = System.currentTimeMillis();
      boolean isOverloadSection = false;

      // generate process instance in multiple threads

      createProcessInstanceThread = new CreateProcessInstanceThread(executionBatchNumber, scenarioStep, runScenario,
          runResult);


      // creates all process instances, return when finish OR when duration is reach
      createProcessInstanceThread.createProcessInstances(durationToCreateProcessInstances);

      // Now collect data for the running time
      totalCreation += createProcessInstanceThread.getTotalCreation();
      totalFailed += createProcessInstanceThread.getTotalFailed();
      List<String> listProcessInstances = createProcessInstanceThread.getListProcessInstances();
      long end = System.currentTimeMillis();

      // do we have to stop the execution?
      if (createProcessInstanceThread.isOverload()) {
        // take too long to create the required process instance, so stop now.
        nbOverloaded++;
        isOverloadSection = true;
      }

      // calculate the time to wait now
      Duration durationToWait = durationToCreateProcessInstances.minusMillis(end - begin);
      if (durationToWait.isNegative()) {
        durationToWait = Duration.ZERO;

      }


      // report now
      if (runScenario.getRunParameters().showLevelMonitoring() || createProcessInstanceThread.isOverload()) {
        logger.info("Step #{}-{}" + " Create (real/scenario)[{}/{} {}]" // Overload marker
                + " Failed[{}] in {} ms " // time of operation
                + " Sleep[{} s] ", // end message
            executionBatchNumber, getId(), createProcessInstanceThread.getTotalCreation(),
            scenarioStep.getNumberOfExecutions(), (isOverloadSection ? "OVERLOAD" : ""),
            createProcessInstanceThread.getTotalFailed(), (end - begin), durationToWait.getSeconds());
      }
      if (runScenario.getRunParameters().showLevelInfo()) {
        logger.info(" listPI(first20): " + listProcessInstances.stream().collect(Collectors.joining(",")));

      }

      // Wait to restart
      scheduler.schedule(this, Instant.now().plusMillis(durationToWait.toMillis()));

    }

    public int getNumberOfRunningThreads() {
      return createProcessInstanceThread == null ? 0 : createProcessInstanceThread.getNumberOfRunningThreads();
    }
  }

}
