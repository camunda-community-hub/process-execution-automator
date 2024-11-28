/* ******************************************************************** */
/*                                                                      */
/*  RunZeebeOperation                                                   */
/*                                                                      */
/*  Different tool to execute operation                                 */
/* ******************************************************************** */
package org.camunda.automator.engine;

import org.camunda.automator.definition.ScenarioStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class RunZeebeOperation {
    private static final Logger logger = LoggerFactory.getLogger(RunZeebeOperation.class);

    // Static method only
    private RunZeebeOperation() {
    }

    /**
     * Resolve variables
     */
    public static Map<String, Object> getVariablesStep(RunScenario runScenario, ScenarioStep step, int index)
            throws AutomatorException {
        Map<String, Object> variablesCompleted = new HashMap<>();
        variablesCompleted.putAll(step.getVariables());

        // execute all operations now
        for (Map.Entry<String, String> entryOperation : step.getVariablesOperations().entrySet()) {
            if (runScenario.getRunParameters().showLevelDebug())
                logger.info("Scenario Key[{}] Value[{}] Step {}", entryOperation.getKey(), entryOperation.getValue(),
                        step.getInformation());
            variablesCompleted.put(entryOperation.getKey(),
                    runScenario.getServiceAccess().serviceDataOperation.execute(entryOperation.getValue(), runScenario,
                            "Step " + step.getInformation(), index));
        }
        if (runScenario.getRunParameters().showLevelDebug() && !variablesCompleted.isEmpty())
            logger.info("SetVariable [{}] {}", step.getVariables(), step.getInformation());

        return variablesCompleted;
    }
}
