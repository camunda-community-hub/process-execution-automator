/* ******************************************************************** */
/*                                                                      */
/*  RunScenarioFlowStartEvent                                           */
/*                                                                      */
/*  Create process instance in Flow                                     */
/* ******************************************************************** */
package org.camunda.automator.engine.flow;

import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenario;
import org.camunda.automator.engine.RunZeebeOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RunScenarioFlowStartEvent extends RunScenarioFlowBasic {
  private final TaskScheduler scheduler;
  Logger logger = LoggerFactory.getLogger(RunScenarioFlowStartEvent.class);
  private boolean stopping;
  private boolean isRunning;

  public RunScenarioFlowStartEvent(TaskScheduler scheduler,
                                   ScenarioStep scenarioStep,
                                   int index,
                                   RunScenario runScenario,
                                   RunResult runResult) {
    super(scenarioStep, index, runScenario, runResult);
    this.scheduler = scheduler;
  }

  @Override
  public void execute() {
    stopping = false;
    isRunning = true;
    Duration duration = Duration.parse(getScenarioStep().getFrequency());

    StartEventRunnable startEventRunnable = new StartEventRunnable(scheduler, getScenarioStep(), runResult,
        getRunScenario(), this);
    scheduler.schedule(startEventRunnable, Instant.now());
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

    public StartEventRunnable(TaskScheduler scheduler,
                              ScenarioStep scenarioStep,
                              RunResult runResult,
                              RunScenario runScenario,
                              RunScenarioFlowStartEvent flowStartEvent) {
      this.scheduler = scheduler;
      this.scenarioStep = scenarioStep;
      this.runResult = runResult;
      this.runScenario = runScenario;
      this.flowStartEvent = flowStartEvent;
    }

    @Override
    public void run() {
      if (flowStartEvent.stopping) {
        if (runScenario.getRunParameters().isLevelMonitoring()) {
          logger.info("Stop now [" + getId() + "]");
          if (nbOverloaded > 0)
            runResult.addError(scenarioStep, "Overloaded " + nbOverloaded + " Process[" + scenarioStep.getProcessId()
                + "] Can't create PI at the required frequency");
        }
        // notify my parent that I stop now
        flowStartEvent.isRunning = false;
        return;
      }
      long begin = System.currentTimeMillis();
      int nbCreation=0;
      int nbFailed=0;
      List<String> listProcessInstances = new ArrayList<>();
      for (int i = 0; i < scenarioStep.getNumberOfExecutions(); i++) {
        try {
          String processInstance = runScenario.getBpmnEngine()
              .createProcessInstance(scenarioStep.getProcessId(), scenarioStep.getTaskId(), // activityId
                  RunZeebeOperation.getVariablesStep(runScenario, scenarioStep));
          if (listProcessInstances.size() < 21)
            listProcessInstances.add(processInstance);
          nbCreation++;
          runResult.registerAddProcessInstance(scenarioStep.getProcessId(),true);
        } catch (AutomatorException e) {
          runResult.addError(scenarioStep, "Error at creation: [" + e.getMessage() + "]");
          nbFailed++;
          runResult.registerAddProcessInstance(scenarioStep.getProcessId(),false);
        }
      }
      long end = System.currentTimeMillis();
      Duration duration = Duration.parse(scenarioStep.getFrequency());
      duration = duration.minusMillis(end - begin);
      if (duration.isNegative()) {
        duration = Duration.ZERO;
        nbOverloaded++;
      }

      if (runScenario.getRunParameters().isLevelMonitoring()) {
        logger.info("[" + getId() // id
            + "] Create (real/scenario)[" + nbCreation+"/"+scenarioStep.getNumberOfExecutions() // creation/target
            + "] Failed["+nbFailed // failed
            + "] in " + (end - begin) + " ms" // time of operation
            + " Sleep[" + duration.getSeconds() + " s] listPI(max20): " + listProcessInstances.stream()
            .collect(Collectors.joining(",")));
      }
      scheduler.schedule(this, Instant.now().plusMillis(duration.toMillis()));

    }
  }
}
