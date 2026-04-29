package org.camunda.automator.engine.unit;

import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenario;
import org.camunda.automator.engine.RunZeebeOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class RunScenarioUnitMessageEvent {


    private final Logger logger = LoggerFactory.getLogger(RunScenarioUnitMessageEvent.class);

    private final RunScenario runScenario;

    protected RunScenarioUnitMessageEvent(RunScenario runScenario) {
        this.runScenario = runScenario;
    }

    /**
     * Execute User task
     *
     * @param result result to complete and return
     * @param step   step to execute
     * @return result completed
     */
    public RunResult executeMessageEvent(ScenarioStep step, RunResult result) {
        if (runScenario.getRunParameters().showLevelMonitoring()) {
            logger.info("MessageEvent TaskId[{}]", step.getTaskId());
        }
        try {
            Object correlationKey = step.getCorrelationKey();
            String messageName = step.getMessageName();
            if (messageName == null || messageName.isEmpty()) {
                result.addError(step, "No messsage name provided");
                return result;
            }
            // the correlation key and Duration may be null: start event message for example
            runScenario.getBpmnEngine().sendMessage(messageName,
                    correlationKey,
                    step.getTimeToLive(Duration.ZERO),
                    RunZeebeOperation.getVariablesStep(runScenario, step, 1));
            return result;
        } catch (AutomatorException e) {
            result.addError(step, e.getMessage());
            return result;
        }
    }
}
