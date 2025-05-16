package org.camunda.automator.api;

import org.camunda.automator.AutomatorAPI;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.configuration.BpmnEngineList;
import org.camunda.automator.configuration.ConfigurationStartup;
import org.camunda.automator.content.ContentManager;
import org.camunda.automator.definition.Scenario;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunParameters;
import org.camunda.automator.engine.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ToolboxRest {
    private static final Logger logger = LoggerFactory.getLogger(ToolboxRest.class.getName());
    private final AutomatorAPI automatorAPI;
    BpmnEngineList bpmnEngineList;

    public ToolboxRest(ConfigurationStartup configurationStartup, ContentManager contentManager,
                       AutomatorAPI automatorAPI, BpmnEngineList bpmnEngineList) {
        this.bpmnEngineList = bpmnEngineList;
        this.automatorAPI = automatorAPI;

    }

    /**
     * Connect to the BPM Engine
     *
     * @param scenario      scenario to use to connect
     * @param runParameters running parameters
     * @param result        result of the connection
     * @return BPMN Engine
     */
    protected BpmnEngine connectToEngine(Scenario scenario, RunParameters runParameters, Map<String, Object> result) {
        BpmnEngine bpmnEngine = null;
        boolean pleaseTryAgain;
        int countEngineIsNotReady = 0;
        String message = "";

        do {
            pleaseTryAgain = false;
            countEngineIsNotReady++;
            try {
                if (scenario.getServerName() != null && !scenario.getServerName().isEmpty()) {
                    message += "ScenarioServerName[" + scenario.getServerName() + "];";
                    bpmnEngine = automatorAPI.getBpmnEngineFromScenario(scenario, bpmnEngineList);
                } else {
                    if (runParameters.getServerName() == null) {
                        result = completeMessage(result, RunResult.StatusTest.ENGINE_NOT_EXIST, "Engine [" + runParameters.getServerName() + "] does not exist in the list");
                        return null;
                    }


                    message += "ConfigurationServerName[" + runParameters.getServerName() + "];";
                    BpmnEngineList.BpmnServerDefinition serverDefinition = bpmnEngineList.getByServerName(
                            runParameters.getServerName());
                    if (serverDefinition == null) {
                        result = completeMessage(result, RunResult.StatusTest.ENGINE_NOT_EXIST, "Engine [" + runParameters.getServerName() + "] does not exist in the list");
                        return null;
                    }

                    if (runParameters.showLevelMonitoring()) {
                        logger.info("Run scenario with Server {}", serverDefinition.getSynthesis());
                    }
                    bpmnEngine = automatorAPI.getBpmnEngine(serverDefinition, true);
                }
                if (runParameters.showLevelDashboard()) {
                    logger.info("Scenario [{}] Connect to BpmnEngine {}", scenario.getName(), message);
                }

                if (!bpmnEngine.isReady()) {
                    bpmnEngine.connection();
                }
            } catch (AutomatorException e) {
                pleaseTryAgain = true;
                message += "EXCEPT " + e.getMessage();
            }
            if (pleaseTryAgain && countEngineIsNotReady < 5) {
                logger.info(
                        "Scenario [{}] file[{}] No BPM ENGINE running [{}] tentative:{}/10. Sleep 30s. Scenario reference serverName[{}]",
                        message, countEngineIsNotReady, scenario.getName(), scenario.getName(), scenario.getServerName());
                try {
                    logger.info("Sleep 10 s - wait the engine start");
                    Thread.sleep(((long) 1000) * 10);
                    logger.info("Wake up");
                } catch (InterruptedException e) {
                    // nothing to do
                }
            }
        } while (pleaseTryAgain && countEngineIsNotReady < 10);
        return bpmnEngine;
    }

    protected Map<String, Object> completeMessage(Map<String, Object> result, RunResult.StatusTest status, String complement) {
        result.put("status", status.toString());
        result.put("complement", complement);
        return result;
    }

}
