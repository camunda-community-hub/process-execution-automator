package org.camunda.automator.api;

import org.camunda.automator.AutomatorAPI;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.configuration.ConfigurationServersEngine;
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
import java.util.*;

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
    public static final String JSON_DESCRIPTION = "description";
    public static final String JSON_PROCESSINSTANCESID = "processInstancesId";
    public static final String JSON_DETAIL = "detail";
    public static final String JSON_ERRORS = "errors";
    public static final String JSON_MESSAGE = "message";
    public static final String JSON_TYPEVERIFICATION = "typeVerification";
    public static final String JSON_INFO = "info";
    public static final String JSON_STATUS_V_NOTEXIST = "NOTEXIST";
    public static final String JSON_STATUS_V_NOBPMNSERVER = "NOBPMNSERVER";
    public static final String JSON_STATUS_V_EXECUTED = "EXECUTED";
    public static final String JSON_STATUS_V_INPROGRESS = "INPROGRESS";
    public static final String JSON_START_DATE = "startDate";
    public static final String JSON_END_DATE = "endDate";
    private static final Logger logger = LoggerFactory.getLogger(UnitTestController.class.getName());
    private final ConfigurationStartup configurationStartup;
    private final ContentManager contentManager;
    private final AutomatorAPI automatorAPI;
    private final RunScenarioService runScenarioService;
    private final ConfigurationServersEngine configurationServersEngine;
    private final ToolboxRest toolboxRest;

    public UnitTestController(ConfigurationStartup configurationStartup,
                              ContentManager contentManager,
                              AutomatorAPI automatorAPI,
                              ToolboxRest toolboxRest,
                              RunScenarioService runScenarioService,
                              ConfigurationServersEngine configurationServersEngine) {
        this.configurationStartup = configurationStartup;
        this.contentManager = contentManager;
        this.automatorAPI = automatorAPI;
        this.toolboxRest = toolboxRest;
        this.runScenarioService = runScenarioService;
        this.configurationServersEngine = configurationServersEngine;
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
            if (status != null && !RunResult.StatusTest.SUCCESS.toString().equals(status)) {
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
            return runResult.getJson(false);
        } else {
            return Map.of(RunResult.JSON_STATUS, JSON_STATUS_V_NOTEXIST);
        }
    }


    @GetMapping(value = "/api/unittest/list", produces = "application/json")
    public List<Map<String, Object>> getListUnitTest(@RequestParam(name = "details", required = false) Boolean details) {
        logger.info("UnitTestController.GetListUnitTest details:[{}]", details);
        List<Map<String, Object>> listUnitTest = new ArrayList<>();

        List<RunResult> sortedList = runScenarioService.getRunResult().stream()
                .sorted(Comparator.comparing(RunResult::getStartDate, Comparator.nullsLast(Date::compareTo)))
                .toList();

        for (RunResult runResult : sortedList) {
            listUnitTest.add(runResult.getJson(details == null || Boolean.FALSE.equals(details))
            );
        }

        return listUnitTest;
    }

    @PutMapping("/api/unittest/clearall")
    public void clearAllTests() {
        logger.info("UnitTestController.clearAllTests");
        runScenarioService.clearAll();
    }

    private Map<String, Object> startTest(String scenarioName,
                                          String serverName,
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
            resultMap.put(JSON_RESULT, JSON_STATUS_V_NOTEXIST);
            return resultMap;
        }


        RunParameters runParameters = new RunParameters();
        String runServerName = serverName == null ? configurationStartup.getServerName() : serverName;
        resultMap.put(JSON_SERVER_NAME, runServerName);
        runParameters.setExecution(true)
                .setServerName(runServerName)
                .setLogLevel(configurationStartup.getLogLevelEnum());
        BpmnEngine bpmnEngine = toolboxRest.connectToEngine(scenario, runParameters, resultMap);
        if (bpmnEngine == null) {
            logger.error("Scenario [{}] Server [{}] No BPM ENGINE running.", scenario.getName(),
                    runParameters.getServerName());
            resultMap.put(JSON_MESSAGE, "No BPM ENGINE running from [" + runServerName + "]");
            resultMap.put(JSON_RESULT, JSON_STATUS_V_NOBPMNSERVER);
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
        resultMap.putAll(runResult.getJson(false));

        return resultMap;
    }


}
