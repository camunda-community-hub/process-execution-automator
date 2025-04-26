package org.camunda.automator.api;

import org.camunda.automator.AutomatorAPI;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.configuration.ConfigurationStartup;
import org.camunda.automator.content.ContentManager;
import org.camunda.automator.definition.Scenario;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunParameters;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("pea")

public class UnitTestController {
    public static final String JSON_SCENARIO_NAME = "scenarioName";
    public static final String JSON_SCENARIO_FILE_NAME = "fileName";

    public static final String JSON_SERVER_NAME = "serverName";
    public static final String JSON_ID = "id";
    public static final String JSON_STATUS = "status";
    public static final String JSON_RESULT = "result";
    public static final String JSON_NAME = "name";
    public static final String JSON_PROCESSINSTANCESID = "processInstancesId";
    public static final String JSON_DETAIL = "detail";
    public static final String JSON_ERRORS = "errors";
    public static final String JSON_MESSAGE = "message";
    public static final String JSON_INFO = "info";
    private static final Logger logger = LoggerFactory.getLogger(UnitTestController.class.getName());
    public static final String JSON_STATUS_V_NOTEXIST = "NOTEXIST";
    public static final String JSON_STATUS_V_NOBPMNSERVER = "NOBPMNSERVER";
    public static final String JSON_STATUS_V_EXECUTED = "EXECUTED";
    public static final String JSON_STATUS_V_INPROGRESS = "INPROGRESS";
    public static final String JSON_START_DATE = "startDate";
    public static final String JSON_END_DATE = "endDate";


    private final ConfigurationStartup configurationStartup;
    private final ContentManager contentManager;
    private final AutomatorAPI automatorAPI;
    private final RunScenarioService runScenarioService;

    ToolboxRest toolboxRest;

    public UnitTestController(ConfigurationStartup configurationStartup,
                              ContentManager contentManager,
                              AutomatorAPI automatorAPI,
                              ToolboxRest toolboxRest,
                              RunScenarioService runScenarioService) {
        this.configurationStartup = configurationStartup;
        this.contentManager = contentManager;
        this.automatorAPI = automatorAPI;
        this.toolboxRest = toolboxRest;
        this.runScenarioService = runScenarioService;
    }

    @PostMapping(value = "/api/unittest/run", produces = "application/json")
    public Map<String, Object> runUnitTest(@RequestParam(name = "name") String scenarioName, @RequestParam(name = "server", required = false) String serverName, @RequestParam(name = "wait", required = false) Boolean wait) {
        logger.info("ServerController: runUnitTest scenario[{}] Wait? {}", scenarioName, wait != null && wait);
        return startTest(scenarioName, serverName, Boolean.TRUE.equals(wait));
    }

    @PostMapping(value = "/api/unittest/runall", produces = "application/json")
    public ResponseEntity<Map<String, Object>> runAllUnitTest(@RequestParam(name = "server", required = false) String serverName,
                                                              @RequestParam(name = "wait", required = false) Boolean wait,
                                                              @RequestParam(name = "failonerror", required = false) Boolean failOnError) {


        Map<String, Object> resultMap = new HashMap<>();
        List<Map<String, Object>> resultScenario = new ArrayList<>();
        resultMap.put("scenario", resultScenario);


        List<Path> listScenario = contentManager.getContentFiles();
        logger.info("UnitTestController: runAllUnitTest Wait? {} nbOfScenario:[{}] serverName[{}]",
                wait != null && wait,
                listScenario.size(),
                serverName);

        boolean allScenarioAreOk = true;
        boolean synchronous = wait == null || Boolean.TRUE.equals(wait);
        for (Path fileScenario : listScenario) {
            Map<String, Object> resultOneScenario = startTest(fileScenario.getFileName().toString(), serverName, !synchronous);
            resultScenario.add(resultOneScenario);

            String status = (String) resultOneScenario.get(JSON_RESULT);
            if (status != null && !ServerController.StatusTest.SUCCESS.toString().equals(status)) {
                allScenarioAreOk = false;
            }
        }


        if (!allScenarioAreOk && Boolean.TRUE.equals(failOnError)) {
            return new ResponseEntity<>(resultMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(resultMap, HttpStatus.OK);
    }


    @GetMapping(value = "/api/unittest/get", produces = "application/json")
    public Map<String, Object> getUnitTest(@RequestParam(name = "id") String executionId) {
        RunResult runResult = runScenarioService.getByExecutionId(executionId);
        if (runResult != null) {
            return resultToJson(runResult, false);
        } else {
            return Map.of(ServerController.JSON_STATUS, JSON_STATUS_V_NOTEXIST);
        }
    }


    @GetMapping(value = "/api/unittest/list", produces = "application/json")
    public List<Map<String, Object>> getListUnitTest(@RequestParam(name = "detail", required = false) Boolean detail) {
        List<Map<String, Object>> listUnitTest = new ArrayList<>();
        for (RunResult runResult : runScenarioService.getRunResult()) {
            listUnitTest.add(resultToJson(runResult, detail == null || Boolean.FALSE.equals(detail))
            );
        }
        return listUnitTest;
    }

    private Map<String, Object> startTest(String scenarioName, String serverName,
                                          boolean asynchronous) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put(JSON_SCENARIO_FILE_NAME, scenarioName);

        Scenario scenario = null;
        try {
            Path scenarioFile = contentManager.getFromFileName(scenarioName);
            resultMap.put(JSON_SCENARIO_FILE_NAME, scenarioFile.getFileName().toString());
            scenario = automatorAPI.loadFromFile(scenarioFile);
        } catch (AutomatorException ae) {
            logger.error("Error during accessing InputStream from scenarioName [{}]: {}", scenarioName,
                    ae.getMessage(), ae);
            resultMap.put(JSON_STATUS, JSON_STATUS_V_NOTEXIST);
            return resultMap;
        }


        RunParameters runParameters = new RunParameters();
        runParameters.setExecution(true)
                .setServerName(serverName == null ? configurationStartup.getServerName() : serverName)
                .setLogLevel(configurationStartup.getLogLevelEnum());
        BpmnEngine bpmnEngine = toolboxRest.connectToEngine(scenario, runParameters, resultMap);
        if (bpmnEngine == null) {
            logger.error("Scenario [{}] Server [{}] No BPM ENGINE running.", scenario.getName(),
                    runParameters.getServerName());
            resultMap.put(JSON_STATUS, JSON_STATUS_V_NOBPMNSERVER);
            return resultMap;
        }
        bpmnEngine.turnHighFlowMode(false);
        logger.info("StartTest: Scenario [{}] use BpmnEngine [{}] : {}",
                scenario.getName(),
                runParameters.getServerName(),
                bpmnEngine.getSignature());
        RunResult runResult = runScenarioService.executeScenario(bpmnEngine, runParameters, scenario, asynchronous);
        resultMap.put(JSON_ID, runResult.getExecutionId());
        resultMap.put(JSON_STATUS, runResult.isFinished() ? JSON_STATUS_V_EXECUTED : JSON_STATUS_V_INPROGRESS);

        logger.info("ServerController: end scenario [{}] in {} ms", scenario.getName(),
                runResult.getTimeExecution());
        resultMap.putAll(resultToJson(runResult, false));

        return resultMap;
    }


    /**
     * @param runResult result to transform in JSON
     * @return result ready for a JSON format
     */
    private Map<String, Object> resultToJson(RunResult runResult, boolean shortDescription) {
        Map<String, Object> resultMap = new HashMap<>();

        resultMap.put(JSON_RESULT, runResult.isSuccess() ? ServerController.StatusTest.SUCCESS.toString() : ServerController.StatusTest.FAIL.toString());

        resultMap.put(JSON_ID, runResult.getExecutionId());
        resultMap.put(JSON_SCENARIO_NAME, runResult.getRunScenario().getScenario().getName());
        resultMap.put(JSON_STATUS, runResult.isFinished() ? JSON_STATUS_V_EXECUTED : JSON_STATUS_V_INPROGRESS);
        resultMap.put(JSON_START_DATE, runResult.getStartDate() != null ? DateTimeFormatter.ISO_INSTANT.format(runResult.getStartDate().toInstant()) : "");
        resultMap.put(JSON_END_DATE, runResult.getEndDate() != null ? DateTimeFormatter.ISO_INSTANT.format(runResult.getEndDate().toInstant()) : "");

        if (shortDescription)
            return resultMap;

        List<Map<String, Object>> listVerificationsJson = new ArrayList<>();
        for (RunResult runResultUnit : runResult.getListRunResults()) {
            if (runResultUnit.getScnExecution() == null) {
                continue;
            }
            Map<String, Object> recordResult = new HashMap<>();
            listVerificationsJson.add(recordResult);
            recordResult.put(JSON_PROCESSINSTANCESID, String.join(", ", runResultUnit.getListProcessInstancesId()));
            recordResult.put(JSON_NAME, runResultUnit.getScnExecution().getName());
            recordResult.put(JSON_RESULT, runResultUnit.isSuccess() ? ServerController.StatusTest.SUCCESS.toString() : ServerController.StatusTest.FAIL.toString());
            recordResult.put(JSON_DETAIL, runResultUnit.getListVerifications().stream()
                    .map(t -> { //
                        return Map.of(JSON_RESULT, t.isSuccess ? ServerController.StatusTest.SUCCESS.toString() : ServerController.StatusTest.FAIL.toString(), //
                                JSON_MESSAGE, getSecureValue(t.message), //
                                JSON_INFO, getSecureValue(t.verification.getSynthesis()));
                    })//
                    .toList());
            recordResult.put(JSON_ERRORS, runResultUnit.getListErrors().stream()
                    .map(t -> { //
                        return Map.of(JSON_ID, t.step.getId(), //
                                JSON_MESSAGE, t.explanation //
                        );
                    })//
                    .toList());
        }


        resultMap.put("tests", listVerificationsJson);
        return resultMap;
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

    /*
    public ResponseEntity<Map<String, Object>> runAllUnitTestOld
            (@RequestParam(name = "server", required = false) String serverName,
             @RequestParam(name = "wait", required = false) Boolean wait,
             @RequestParam(name = "failonerror", required = false) Boolean failOnError) {
        Map<String, Object> resultMap = new HashMap<>();
        List<Map<String, Object>> resultScenario = new ArrayList<>();
        resultMap.put("scenario", resultScenario);
        String baseUnitTestId = String.valueOf(System.currentTimeMillis());

        List<Path> listScenario = contentManager.getContentFiles();
        logger.info("UnitTestController: runAllUnitTest Wait? {} scenario :{}",
                wait != null && wait,
                listScenario.size());
        boolean allScenarioAreOk = true;
        for (Path fileScenario : listScenario) {
            Map<String, Object> resultOneScenario = new HashMap<>();

            String unitTestId = baseUnitTestId + "." + fileScenario.getFileName();
            resultOneScenario.put("id", executionId);

            if (Boolean.TRUE.equals(wait)) {
                startTest(fileScenario.getFileName().toString(), serverName, executionId);

                resultOneScenario = cacheExecution.get(unitTestId);
                resultScenario.add(resultOneScenario);
                String status = (String) resultOneScenario.get(JSON_GENERAL_STATUS);
                if (!ServerController.StatusTest.SUCCESS.toString().equals(status)) {
                    allScenarioAreOk = false;
                }

            } else {
                resultScenario.add(resultOneScenario);

                Thread thread = new Thread(() -> startTest(fileScenario.getFileName().toString(), serverName, unitTestId));
                thread.start();
            }
        }

        if (!allScenarioAreOk && Boolean.TRUE.equals(failOnError)) {
            return new ResponseEntity<>(resultMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(resultMap, HttpStatus.OK);
    }

     */

}
