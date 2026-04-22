package org.camunda.automator.api;

import org.camunda.automator.AutomatorAPI;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.configuration.ConfigurationBpmnEngineList;
import org.camunda.automator.configuration.ConfigurationServersEngine;
import org.camunda.automator.configuration.ConfigurationStartup;
import org.camunda.automator.content.ContentManager;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunScenarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("pea")


public class ServersController {

    public static final String JSON_SCENARIO_NAME = "scenarioName";
    public static final String JSON_SERVER_NAME = "serverName";
    public static final String JSON_ID = "id";
    public static final String JSON_STATUS = "status";
    public static final String JSON_GENERAL_STATUS = "generalStatus";
    public static final String JSON_NAME = "name";
    public static final String JSON_PROCESSINSTANCESID = "processInstancesId";
    public static final String JSON_DETAIL = "detail";
    public static final String JSON_ERRORS = "errors";
    public static final String JSON_MESSAGE = "message";
    public static final String JSON_INFO = "info";
    public static final String JSON_STATUS_V_NOTEXIST = "NOTEXIST";
    public static final String JSON_ANALYSE = "analyse";
    public static final String JSON_ANALYSE_HUMAN = "AnalyseHuman";
    public static final String JSON_CONNECTION_ADMIN = "admin";
    public static final String JSON_CONNECTION_TASKLIST = "tasklist";
    public static final String JSON_CONNECTION_ENGINE = "engine";

    private static final Logger logger = LoggerFactory.getLogger(ServersController.class.getName());

    private final ConfigurationStartup configurationStartup;
    private final ContentManager contentManager;
    private final AutomatorAPI automatorAPI;
    private final RunScenarioService runScenarioService;
    private final ConfigurationServersEngine configurationServersEngine;
    private final ConfigurationBpmnEngineList configurationBpmnEngineList;

    public ServersController(ConfigurationStartup configurationStartup,
                             ContentManager contentManager,
                             AutomatorAPI automatorAPI,
                             RunScenarioService runScenarioService,
                             ConfigurationServersEngine configurationServersEngine,
                             ConfigurationBpmnEngineList configurationBpmnEngineList) {
        this.configurationStartup = configurationStartup;
        this.contentManager = contentManager;
        this.automatorAPI = automatorAPI;
        this.runScenarioService = runScenarioService;
        this.configurationServersEngine = configurationServersEngine;
        this.configurationBpmnEngineList = configurationBpmnEngineList;
    }

    @GetMapping(value = "/api/server/list", produces = "application/json")
    public Map<String, Object> getServerList() {

        List<ConfigurationBpmnEngineList.BpmnServerDefinition> listServers = configurationBpmnEngineList.getListServers();
        logger.info("ServerController: getServerList listServers:[{}] : [{}]", listServers.size(),
                listServers.stream().map(ConfigurationBpmnEngineList.BpmnServerDefinition::getName)
                        .collect(Collectors.joining(";")));

        return Map.of("preferateServer", configurationStartup.getServerName(),
                "servers",
                listServers.stream()
                        .sorted(Comparator.comparing(ConfigurationBpmnEngineList.BpmnServerDefinition::getServerType)
                                .thenComparing(ConfigurationBpmnEngineList.BpmnServerDefinition::getName))
                        .map(ConfigurationBpmnEngineList.BpmnServerDefinition::getMapSynthesis)
                        .toList());
    }


    @PostMapping(value = "/api/server/setpreferate", produces = "application/json")
    public Map<String, Object> setPreferate(@RequestParam(name = "serverName") String serverName) {
        configurationStartup.setServerName(serverName);
        return Map.of("preferateServer", configurationStartup.getServerName());
    }

    @GetMapping(value = "/api/server/testconnection", produces = "application/json")
    public Map<String, Object> testConnection(@RequestParam(name = "serverName") String serverName) {
        Map<String, Object> result = new HashMap<>();
        ConfigurationBpmnEngineList.BpmnServerDefinition serverDefinition = null;
        BpmnEngine bpmnEngine = null;
        try {
            serverDefinition = configurationBpmnEngineList.getByServerName(serverName);

            if (serverDefinition == null) {
                result.put(JSON_CONNECTION_ENGINE, Map.of(JSON_STATUS, "NOT_EXIST", JSON_SERVER_NAME, serverName));
                result.put(JSON_CONNECTION_ADMIN, Map.of(JSON_STATUS, "NOT_EXIST", JSON_SERVER_NAME, serverName));
                result.put(JSON_CONNECTION_TASKLIST, Map.of(JSON_STATUS, "NOT_EXIST", JSON_SERVER_NAME, serverName));
                return result;
            }

            // The method will connect now to the engine, and will return an exception
            bpmnEngine = automatorAPI.getBpmnEngine(serverDefinition, true);
            boolean zeebeIsReady = bpmnEngine.isReady();
            result.put(JSON_CONNECTION_ENGINE, Map.of(JSON_STATUS, zeebeIsReady ? "OK" : "FAIL"));
        } catch (AutomatorException e) {
            // Get the message, and turn it into a list of String to help analysis

            result.put(JSON_CONNECTION_ENGINE, java.util.Map.of(JSON_STATUS, "FAIL",
                    JSON_ANALYSE, e.getMessage(),
                    JSON_ANALYSE_HUMAN, Arrays.asList(e.getMessage().split("; "))));

        }

        if (bpmnEngine == null) {
            result.put(JSON_CONNECTION_ADMIN, java.util.Map.of(JSON_STATUS, BpmnEngine.CONNECTION_STATUS.FAIL,
                    JSON_ANALYSE_HUMAN, List.of("No engine connection for server [" + serverName + "]")));
        } else {
            BpmnEngine.ConnectionStatus connectionStatus = bpmnEngine.testAdminConnection();
            result.put(JSON_CONNECTION_ADMIN, java.util.Map.of(JSON_STATUS, connectionStatus.status.toString(),
                    JSON_ANALYSE, connectionStatus.message,
                    JSON_ANALYSE_HUMAN, Arrays.asList(connectionStatus.message.split("; "))));
        }

        if (bpmnEngine == null) {
            result.put(JSON_CONNECTION_TASKLIST, java.util.Map.of(JSON_STATUS, BpmnEngine.CONNECTION_STATUS.FAIL,
                    JSON_ANALYSE_HUMAN, List.of("No engine connection for server [" + serverName + "]")));
        } else {
            BpmnEngine.ConnectionStatus connectionStatus = bpmnEngine.testTaskListConnection();
            result.put(JSON_CONNECTION_TASKLIST, java.util.Map.of(JSON_STATUS, connectionStatus.status.toString(),
                    JSON_ANALYSE, connectionStatus.message,
                    JSON_ANALYSE_HUMAN, Arrays.asList(connectionStatus.message.split("; "))));
        }

        return result;


    }

    @GetMapping(value = "/api/servers/connection", produces = "application/json")
    public List<Map<String, Object>> getListServerWithConnection() {
        return configurationBpmnEngineList.getListServers().stream().map(
                t -> {
                    Map<String, Object> result = t.getMapSynthesis();

                    try {
                        BpmnEngine bpmnEngine = automatorAPI.getBpmnEngine(t, true);
                        bpmnEngine.connection();
                        result.put("connection", "OK");
                    } catch (AutomatorException e) {
                        result.put("connection", "FAILED");
                        result.put("message", e.getMessage());
                    }
                    return result;
                }).toList();
    }


}
