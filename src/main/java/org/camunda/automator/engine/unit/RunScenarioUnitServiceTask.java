package org.camunda.automator.engine.unit;

import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenario;
import org.camunda.automator.engine.RunZeebeOperation;

import java.time.Duration;
import java.util.List;

public class RunScenarioUnitServiceTask {

  private final RunScenario runScenario;

  protected RunScenarioUnitServiceTask(RunScenario runScenario) {
    this.runScenario = runScenario;
  }

  /**
   * Execute User task
   *
   * @param result result to complete and return
   * @param step   step to execute
   * @return result completed
   */
  public RunResult executeServiceTask(RunResult result, ScenarioStep step) {
    if (step.getDelay() != null) {
      Duration duration = Duration.parse(step.getDelay());
      try {
        Thread.sleep(duration.toMillis());
      } catch (InterruptedException e) {
        // nothing to do
      }
    }
    Long waitingTimeInMs = null;
    if (step.getWaitingTime() != null) {
      Duration duration = Duration.parse(step.getWaitingTime());
      waitingTimeInMs = duration.toMillis();
    }
    if (waitingTimeInMs == null)
      waitingTimeInMs = Long.valueOf(5 * 60 * 1000);

    for (int index = 0; index < step.getNumberOfExecutions(); index++) {
      long beginTimeWait = System.currentTimeMillis();
      try {
        List<String> listActivities;
        do {

          listActivities = runScenario.getBpmnEngine()
              .searchServiceTasks(result.getFirstProcessInstanceId(), step.getTaskId(), step.getTopic(), 1);

          if (listActivities.isEmpty()) {
            try {
              Thread.sleep(500);
            } catch (InterruptedException e) {
            }
          }
        } while (listActivities.isEmpty() && System.currentTimeMillis() - beginTimeWait < waitingTimeInMs);

        if (listActivities.isEmpty()) {
          result.addError(step, "No service task show up task[" + step.getTaskId() + "] processInstance["
              + result.getFirstProcessInstanceId() + "]");
          return result;
        }
        runScenario.getBpmnEngine()
            .executeServiceTask(listActivities.get(0), step.getUserId(),
                RunZeebeOperation.getVariablesStep(runScenario, step));
      } catch (AutomatorException e) {
        result.addError(step, e.getMessage());
        return result;
      }
    }

    return result;

  }
}
