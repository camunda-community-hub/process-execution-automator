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
  public void execute() {
    stopping = false;
    isRunning = true;
    StartEventRunnable startEventRunnable = new StartEventRunnable(scheduler, getScenarioStep(), getRunScenario(), this,
        runResult);
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
    return 0;
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
    private int totalCreationGoal = 0;
    private int totalFailed = 0;

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
      executionBatchNumber++;
      if (flowStartEvent.stopping) {
        if (runScenario.getRunParameters().showLevelMonitoring()) {
          logger.info("Stop now [" + getId() + "]");
          if (nbOverloaded > 0)
            runResult.addError(scenarioStep,
                "Overloaded " + nbOverloaded + " TotalCreation " + totalCreation + " Goal " + totalCreationGoal
                    + " Process[" + scenarioStep.getProcessId() + "] Can't create PI at the required frequency");
          if (totalFailed > 0)
            runResult.addError(scenarioStep,
                "Failed " + totalFailed + " Process[" + scenarioStep.getProcessId() + "] Can't create PI ");

        }
        // notify my parent that I stop now
        flowStartEvent.isRunning = false;
        return;
      }
      Duration durationToCreateProcessInstances = Duration.parse(scenarioStep.getFrequency());

      long begin = System.currentTimeMillis();
      boolean isOverloadSection = false;

      totalCreationGoal += scenarioStep.getNumberOfExecutions();

      // generate process instance
      CreateProcessInstanceThread createProcessInstanceThread = new CreateProcessInstanceThread(executionBatchNumber,
          scenarioStep, runScenario, runResult);
      createProcessInstanceThread.startProcessInstance(durationToCreateProcessInstances);
      totalCreation += createProcessInstanceThread.getTotalCreation();
      totalFailed += createProcessInstanceThread.getTotalCreation();
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
        logger.info("Step #" + executionBatchNumber + "-" + getId() // id
            + "] Create (real/scenario)[" + createProcessInstanceThread.getTotalCreation() + "/"
            + scenarioStep.getNumberOfExecutions() // creation/target
            + (isOverloadSection ? "OVERLOAD" : "") // Overload marker
            + "] Failed[" + createProcessInstanceThread.getTotalCreation() // failed
            + "] in " + (end - begin) + " ms " // time of operation
            + " Sleep[" + durationToWait.getSeconds() + " s] listPI(max20): " + listProcessInstances.stream()
            .collect(Collectors.joining(",")));
      }

      // Wait to restart
      scheduler.schedule(this, Instant.now().plusMillis(durationToWait.toMillis()));

    }
  }

}
