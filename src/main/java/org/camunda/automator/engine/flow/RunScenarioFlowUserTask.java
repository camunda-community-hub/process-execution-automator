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
import java.util.List;

public class RunScenarioFlowUserTask extends RunScenarioFlowBasic {

  Logger logger = LoggerFactory.getLogger(RunScenarioFlowUserTask.class);
  private STATUS status = STATUS.RUNNING;
  private final TaskScheduler scheduler;

  public RunScenarioFlowUserTask(TaskScheduler scheduler,
                                 ScenarioStep scenarioStep,
                                 int index,
                                 RunScenario runScenario,
                                 RunResult runResult) {
    super(scenarioStep, index, runScenario, runResult);
    this.scheduler = scheduler;

  }

  @Override
  public void execute() {

    RunScenarioFlowUserTask.UserTaskRunnable startEventRunnable = new RunScenarioFlowUserTask.UserTaskRunnable(
        scheduler, getScenarioStep(), runResult, getRunScenario(), this);
    scheduler.schedule(startEventRunnable, Instant.now());
  }

  @Override
  public STATUS getStatus() {
    return status;
  }

  @Override
  public int getCurrentNumberOfThreads() {
    return 1;
  }

  @Override
  public void pleaseStop() {
    logger.info("Ask Stopping [" + getId() + "]");

    status = STATUS.STOPPING;
    // wait 1 second
    try {
      Thread.sleep(1000);
    } catch (Exception e) {
      // do nothing
    }
  }

  /**
   * StartEventRunnable
   */
  class UserTaskRunnable implements Runnable {

    private final TaskScheduler scheduler;
    private final ScenarioStep scenarioStep;
    private final RunResult runResult;
    private final RunScenario runScenario;
    private final RunScenarioFlowUserTask flowUserTask;

    private final int nbOverloaded = 0;
    private final int totalCreation = 0;
    private final int totalCreationGoal = 0;
    private final int totalFailed = 0;

    public UserTaskRunnable(TaskScheduler scheduler,
                            ScenarioStep scenarioStep,
                            RunResult runResult,
                            RunScenario runScenario,
                            RunScenarioFlowUserTask flowUserTask) {
      this.scheduler = scheduler;
      this.scenarioStep = scenarioStep;
      this.runResult = runResult;
      this.runScenario = runScenario;
      this.flowUserTask = flowUserTask;
    }

    @Override
    public void run() {
      Long waitingTimeInMs = null;
      if (getScenarioStep().getWaitingTime() != null) {
        Duration duration = Duration.parse(getScenarioStep().getWaitingTime());
        waitingTimeInMs = duration.toMillis();
      }
      if (waitingTimeInMs == null)
        waitingTimeInMs = 1L;

      long beginTimeWait = System.currentTimeMillis();
      while (flowUserTask.status == STATUS.RUNNING) {
        try {
          List<String> listActivities;
          do {

            listActivities = getRunScenario().getBpmnEngine().searchUserTasks(getScenarioStep().getTaskId(), 10);

            if (listActivities.isEmpty()) {
              try {
                Thread.sleep(10000);
              } catch (InterruptedException e) {
                // nothing to do here
              }
            }
          } while (listActivities.isEmpty() && System.currentTimeMillis() - beginTimeWait < waitingTimeInMs);

          if (!listActivities.isEmpty()) {
            for (String taskInstanceId : listActivities) {
              getRunScenario().getBpmnEngine()
                  .executeUserTask(taskInstanceId, getScenarioStep().getUserId(),
                      RunZeebeOperation.getVariablesStep(getRunScenario(), getScenarioStep()));
            }
          }
        } catch (AutomatorException e) {
          logger.error("Error task[" + getScenarioStep().getTaskId() + " : " + e.getMessage());

          getRunResult().registerAddErrorStepExecution();
        }
      }
      flowUserTask.status = STATUS.STOPPED;
    }
  }
}
