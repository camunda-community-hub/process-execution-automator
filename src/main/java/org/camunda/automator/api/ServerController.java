package org.camunda.automator.api;

import org.camunda.automator.AutomatorAPI;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.configuration.BpmnEngineList;
import org.camunda.automator.configuration.ConfigurationStartup;
import org.camunda.automator.content.ContentManager;
import org.camunda.automator.engine.AutomatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("pea")

public class ServerController {
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
    private static final Logger logger = LoggerFactory.getLogger(ServerController.class.getName());
    BpmnEngineList bpmnEngineList;
    ToolboxRest toolboxRest;
    HashMap<String, Map<String, Object>> cacheExecution = new HashMap<>();
    private final ConfigurationStartup configurationStartup;
    private final ContentManager contentManager;
    private final AutomatorAPI automatorAPI;


    public ServerController(ConfigurationStartup configurationStartup, ContentManager contentManager,
                            AutomatorAPI automatorAPI, BpmnEngineList bpmnEngineList,
                            ToolboxRest toolboxRest) {
        this.configurationStartup = configurationStartup;
        this.contentManager = contentManager;
        this.automatorAPI = automatorAPI;
        this.bpmnEngineList = bpmnEngineList;
        this.toolboxRest = toolboxRest;
    }

    @GetMapping(value = "/api/servers/list", produces = "application/json")
    public List<Map<String, Object>> getListServer() {
        return bpmnEngineList.getListServers().stream().map(
                BpmnEngineList.BpmnServerDefinition::getMapSynthesis).toList();
    }

    @GetMapping(value = "/api/servers/connection", produces = "application/json")
    public List<Map<String, Object>> getListServerWithConnection() {
        return bpmnEngineList.getListServers().stream().map(
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


    public enum StatusTest {SCENARIO_NOT_EXIST, ENGINE_NOT_EXIST, SUCCESS, FAIL}
}
