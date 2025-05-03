package org.camunda.automator.api;

import org.camunda.automator.AutomatorAPI;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.configuration.BpmnEngineList;
import org.camunda.automator.configuration.ConfigurationServersEngine;
import org.camunda.automator.configuration.ConfigurationStartup;
import org.camunda.automator.content.ContentManager;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunScenarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("pea")


public class ServersController {
    private static final Logger logger = LoggerFactory.getLogger(ServersController.class.getName());
    public static final String JSON_ANALYSE = "analyse";
    public static final String JSON_ANALYSE_HUMAN = "AnalyseHuman";
    private final ConfigurationStartup configurationStartup;
    private final ContentManager contentManager;
    private final AutomatorAPI automatorAPI;
    private final RunScenarioService runScenarioService;
    private final ConfigurationServersEngine configurationServersEngine;
    private final ToolboxRest toolboxRest;
    private final BpmnEngineList bpmnEngineList;

    public static final String JSON_SERVER_NAME = "serverName";
    public static final String JSON_STATUS = "status";

    public ServersController(ConfigurationStartup configurationStartup,
                             ContentManager contentManager,
                             AutomatorAPI automatorAPI,
                             ToolboxRest toolboxRest,
                             RunScenarioService runScenarioService,
                             ConfigurationServersEngine configurationServersEngine,
                             BpmnEngineList bpmnEngineList) {
        this.configurationStartup = configurationStartup;
        this.contentManager = contentManager;
        this.automatorAPI = automatorAPI;
        this.toolboxRest = toolboxRest;
        this.runScenarioService = runScenarioService;
        this.configurationServersEngine = configurationServersEngine;
        this.bpmnEngineList = bpmnEngineList;
    }

    @GetMapping(value = "/api/server/list", produces = "application/json")
    public Map<String, Object> getServerList() {
        List<BpmnEngineList.BpmnServerDefinition> listServers = bpmnEngineList.getListServers();
        logger.info("ServerController: getServerList listServers:[{}] : [{}]", listServers.size(),
        listServers.stream().map(BpmnEngineList.BpmnServerDefinition::getName)
                .collect(Collectors.joining(";")));

        ;
        return Map.of("preferateServer", configurationStartup.getServerName(),
                "servers",
                listServers.stream()
                        .sorted(Comparator.comparing(BpmnEngineList.BpmnServerDefinition::getServerType)
                                .thenComparing(BpmnEngineList.BpmnServerDefinition::getName))
                        .map(BpmnEngineList.BpmnServerDefinition::getMapSynthesis)
                        .toList());
    }

    @GetMapping(value = "/api/server/testconnection", produces = "application/json")
    public Map<String, Object> testConnection(@RequestParam(name = "serverName") String serverName) {

        BpmnEngineList.BpmnServerDefinition serverDefinition = null;
        try {
            serverDefinition = bpmnEngineList.getByServerName(serverName);

            if (serverDefinition == null) {
                return Map.of(JSON_STATUS, "NOT_EXIST",
                        JSON_SERVER_NAME, serverName);
            }

            // The method will connect now to the engine, and will return an exception
            BpmnEngine bpmnEngine = automatorAPI.getBpmnEngine(serverDefinition, true);

            boolean isReady = bpmnEngine.isReady();
            return Map.of(JSON_STATUS, isReady ? "OK" : "FAIL");

        } catch (AutomatorException e) {
            // Get the message, and turn it into a list of String to help analysis

            return Map.of(JSON_STATUS, "FAIL",
                    JSON_ANALYSE, e.getMessage(),
                    JSON_ANALYSE_HUMAN, Arrays.asList(e.getMessage().split("; ")));

        }
    }


}
