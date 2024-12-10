package org.camunda.automator;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AutomatorRest {
    public static final String JSON_SCENARIO_NAME = "scenarioName";
    public static final String JSON_SERVER_NAME = "serverName";
    public static final String JSON_ID = "id";
    public static final String JSON_STATUS = "status";
    public static final String JSON_GENERAL_STATUS = "generalStatus";
    public static final String JSON_NAME = "name";
    public static final String JSON_PROCESSINSTANCESID = "processInstancesId";
    public static final String JSON_DETAIL = "detail";
    public static final String JSON_MESSAGE = "message";
    public static final String JSON_INFO = "info";
    private static final Logger logger = LoggerFactory.getLogger(AutomatorRest.class.getName());
    public static final String JSON_STATUS_V_NOTEXIST = "NOTEXIST";
    private final ConfigurationStartup configurationStartup;
    private final ContentManager contentManager;
    private final AutomatorAPI automatorAPI;
    BpmnEngineList engineConfiguration;
    HashMap<String, Map<String, Object>> cacheExecution = new HashMap<>();

    public AutomatorRest(ConfigurationStartup configurationStartup, ContentManager contentManager, AutomatorAPI automatorAPI, BpmnEngineList engineConfiguration) {
        this.configurationStartup = configurationStartup;
        this.contentManager = contentManager;
        this.automatorAPI = automatorAPI;
        this.engineConfiguration = engineConfiguration;
    }

    @PostMapping(value = "/api/unittest/run", produces = "application/json")
    public Map<String, Object> runUnitTest(@RequestParam(name = "name") String scenarioName, @RequestParam(name = "server", required = false) String serverName, @RequestParam(name = "wait", required = false) Boolean wait) {
        logger.info("AutomatorRest: runUnitTest scenario[{}] Wait? {}", scenarioName, wait != null && wait);

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("senarioName", scenarioName);

        String unitTestId = String.valueOf(System.currentTimeMillis());
        resultMap.put("id", unitTestId);

        if (Boolean.TRUE.equals(wait)) {
            startTest(scenarioName, serverName, unitTestId);
            resultMap = cacheExecution.get(unitTestId);
            return resultMap;

        } else {
            Thread thread = new Thread(() -> startTest(scenarioName, serverName, unitTestId));
            thread.start();
        }

        return resultMap;
    }

    @GetMapping(value = "/api/unittest/get", produces = "application/json")
    public Map<String, Object> getUnitTest(@RequestParam(name = "id") String unitTestId) {
        Map<String, Object> resultTest = cacheExecution.get(unitTestId);
        if (resultTest != null) {
            return resultTest;
        } else {
            return Map.of(AutomatorRest.JSON_STATUS, JSON_STATUS_V_NOTEXIST);
        }
    }

    @GetMapping(value = "/api/unittest/list", produces = "application/json")
    public List<Map<String, Object>> getListUnitTest() {
        List<Map<String, Object>> listUnitTest = new ArrayList<>();
        for (Map.Entry entryTest : cacheExecution.entrySet()) {
            if (entryTest.getValue() instanceof Map<?, ?> resultMap) {
                listUnitTest.add(Map.of(AutomatorRest.JSON_ID, entryTest.getKey(),
                        JSON_SCENARIO_NAME, getSecureValue(resultMap.get(JSON_SCENARIO_NAME))));
            } else {
                listUnitTest.add(Map.of(JSON_ID, entryTest.getKey(),
                        JSON_SCENARIO_NAME, ""));
            }
        }
        return listUnitTest;
    }


    /**
     * Start a test
     */
    private void startTest(String scenarioName, String serverName, String unitTestId) {

        Map<String, Object> resultMap = new HashMap<>();
        cacheExecution.put(unitTestId, resultMap);

        RunParameters runParameters = new RunParameters();
        runParameters.setExecution(true)
                .setServerName(serverName == null || scenarioName.isEmpty() ? configurationStartup.getServerName() : serverName)
                .setLogLevel(configurationStartup.getLogLevelEnum());
        resultMap.put(JSON_ID, unitTestId);
        resultMap.put(JSON_SERVER_NAME, runParameters.getServerName());
        resultMap.put(JSON_SCENARIO_NAME, scenarioName);

        logger.info("AutomatorRest: Start Test scenario[{}] unitTestId[{}] serverName[{}] ", scenarioName, unitTestId,
                runParameters.getServerName());

        // now proceed the scenario
        try {
            Scenario scenario = null;
            Path scenarioFile = contentManager.getFromName(scenarioName);
            try {
                scenario = automatorAPI.loadFromFile(scenarioFile);
            } catch (Exception e) {
                logger.error("Error during accessing InputStream from File [{}]: {}", scenarioFile.toAbsolutePath(),
                        e.getMessage(),e);
            }
            if (scenario == null) {
                resultMap.put(JSON_STATUS, "NOTEXIST");
                return;
            }

            logger.info("Start scenario [{}] on (1)ScenarioServerName[{}] (2)ConfigurationServerName[{}]",
                    scenario.getName(), scenario.getServerName(), runParameters.getServerName());

            // BpmnEngine: find the correct one referenced in the scenario
            String message = "";
            BpmnEngine bpmnEngine = connectToEngine(scenario, runParameters, resultMap);
            if (bpmnEngine == null) {
                logger.error("Scenario [{}] file[{}] Server {} No BPM ENGINE running.", scenario.getName(),
                        scenario.getName(), message);
                return;
            }

            bpmnEngine.turnHighFlowMode(false);
            logger.info("Scenario [{}] file[{}] use BpmnEngine {}", scenario.getName(), scenario.getName(),
                    bpmnEngine.getSignature());
            RunResult scenarioExecutionResult = automatorAPI.executeScenario(bpmnEngine, runParameters, scenario);
            logger.info("AutomatorRest: end scenario [{}] in {} ms", scenario.getName(),
                    scenarioExecutionResult.getTimeExecution());

            resultMap.put(JSON_STATUS, "EXECUTED");
            resultMap.putAll(resultToJson(scenarioExecutionResult));

        } catch (Exception e) {
            logger.error("During execute unit Test : {}", e.getMessage(),e);
            resultMap.put("error", e.getMessage());
        }
    }

    /**
     * @param runResult result to transform in JSON
     * @return result ready for a JSON format
     */
    private Map<String, Object> resultToJson(RunResult runResult) {
        Map<String, Object> resultMap = new HashMap<>();

        resultMap.put(JSON_GENERAL_STATUS, runResult.isSuccess() ? StatusTest.SUCCESS : StatusTest.FAIL);
        List<Map<String, Object>> listVerificationsJson = new ArrayList<>();
        for (RunResult runResultUnit : runResult.getListRunResults()) {
            if (runResultUnit.getScnExecution() == null) {
                continue;
            }
            Map<String, Object> recordResult = new HashMap<>();
            listVerificationsJson.add(recordResult);
            recordResult.put(JSON_PROCESSINSTANCESID, String.join(", ", runResultUnit.getListProcessInstancesId()));
            recordResult.put(JSON_NAME, runResultUnit.getScnExecution().getName());
            recordResult.put(JSON_STATUS, runResultUnit.isSuccess() ? StatusTest.SUCCESS : StatusTest.FAIL);
            recordResult.put(JSON_DETAIL, runResultUnit.getListVerifications().stream()
                    .map(t -> { //
                        return Map.of(JSON_STATUS, t.isSuccess ? StatusTest.SUCCESS : StatusTest.FAIL, //
                                JSON_MESSAGE, getSecureValue(t.message), //
                                JSON_INFO, getSecureValue(t.verification.getSynthesis()));
                    })//
                    .toList());
        }
        resultMap.put("tests", listVerificationsJson);
        return resultMap;
    }

    private Map<String, Object> completeMessage(Map<String, Object> result, StatusTest status, String complement) {
        result.put("status", status.toString());
        result.put("complement", complement);
        return result;
    }

    /**
     * Connect to the BPM Engine
     *
     * @param scenario      scenario to use to connect
     * @param runParameters running parameters
     * @param result result of the connection
     * @return BPMN Engine
     */
    private BpmnEngine connectToEngine(Scenario scenario, RunParameters runParameters, Map<String, Object> result) {
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
                    bpmnEngine = automatorAPI.getBpmnEngineFromScenario(scenario, engineConfiguration);
                } else {
                    if (runParameters.getServerName() == null) {
                        result = completeMessage(result, StatusTest.ENGINE_NOT_EXIST, "Engine [" + runParameters.getServerName() + "] does not exist in the list");
                        return null;
                    }


                    message += "ConfigurationServerName[" + runParameters.getServerName() + "];";
                    BpmnEngineList.BpmnServerDefinition serverDefinition = engineConfiguration.getByServerName(
                            runParameters.getServerName());
                    if (serverDefinition == null) {
                        result = completeMessage(result, StatusTest.ENGINE_NOT_EXIST, "Engine [" + runParameters.getServerName() + "] does not exist in the list");
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
            if (pleaseTryAgain && countEngineIsNotReady < 10) {
                logger.info(
                        "Scenario [{}] file[{}] No BPM ENGINE running [{}] tentative:{}/10. Sleep 30s. Scenario reference serverName[{}]",
                        message, countEngineIsNotReady, scenario.getName(), scenario.getName(), scenario.getServerName());
                try {
                    Thread.sleep(((long) 1000) * 30);
                } catch (InterruptedException e) {
                    // nothing to do
                }
            }
        } while (pleaseTryAgain && countEngineIsNotReady < 10);
        return bpmnEngine;
    }

    /**
     * Return "" if info is null: in a Map.of(), a null pointer return an exception
     *
     * @param info value to return
     * @return info or ""
     */
    private Object getSecureValue(Object info) {
        return info == null ? "" : info;
    }

    public enum StatusTest {SCENARIO_NOT_EXIST, ENGINE_NOT_EXIST, SUCCESS, FAIL}
}
