package org.camunda.automator.engine.unit;

import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenario;
import org.camunda.automator.engine.RunZeebeOperation;

public class RunScenarioUnitStartEvent {

  private final RunScenario runScenario;

  protected RunScenarioUnitStartEvent(RunScenario runScenario) {
    this.runScenario = runScenario;
  }

  /**
   * Start Event
   *
   * @param result result to complete and return
   * @param step   step to execute
   * @return result completed
   */
  public RunResult startEvent(RunResult result, ScenarioStep step) {
    try {
      result.addProcessInstanceId(step.getScnExecution().getScnHead().getProcessId(), runScenario.getBpmnEngine()
          .createProcessInstance(step.getScnExecution().getScnHead().getProcessId(), step.getTaskId(), // activityId
              RunZeebeOperation.getVariablesStep(runScenario, step))); // resolve variables
    } catch (AutomatorException e) {
      result.addError(step, "Error at creation " + e.getMessage());
    }
    return result;
  }

}
