/* ******************************************************************** */
/*                                                                      */
/*  RunZeebeOperation                                                   */
/*                                                                      */
/*  Different tool to execute operation                                 */
/* ******************************************************************** */
package org.camunda.automator.engine;

import org.camunda.automator.definition.ScenarioStep;

import java.util.HashMap;
import java.util.Map;

public class RunZeebeOperation {

  /**
   * Resolve variables
   */
  public static Map<String, Object> getVariablesStep(RunScenario runScenario, ScenarioStep step)
      throws AutomatorException {
    Map<String, Object> variablesCompleted = new HashMap<>();
    variablesCompleted.putAll(step.getVariables());

    // execute all operations now
    for (Map.Entry<String, String> entryOperation : step.getVariablesOperations().entrySet()) {
      variablesCompleted.put(entryOperation.getKey(),
          runScenario.getServiceAccess().serviceDataOperation.execute(entryOperation.getValue(), runScenario));
    }

    return variablesCompleted;
  }
}
