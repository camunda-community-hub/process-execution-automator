package org.camunda.automator.engine.unit;

import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenario;
import org.camunda.automator.engine.RunZeebeOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class RunScenarioUnitStartEvent {

    private final Logger logger = LoggerFactory.getLogger(RunScenarioUnitStartEvent.class);

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
            if (runScenario.getRunParameters().showLevelMonitoring()) {
                logger.info("StartEvent EventId[{}]", step.getTaskId());
            }
            String processId = step.getScnExecution().getScnHead().getProcessId();
            // There is no multithreading: index=1
            Map<String, Object> processVariables = RunZeebeOperation.getVariablesStep(runScenario, step, 1);

            String processInstanceId = runScenario.getBpmnEngine()
                    .createProcessInstance(processId, step.getTaskId(), processVariables);

            result.addProcessInstanceId(processId, processInstanceId);
        } catch (AutomatorException e) {
            result.addError(step, "Error at creation " + e.getMessage());
        }
        return result;
    }

}
