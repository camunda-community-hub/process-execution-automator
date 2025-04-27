/* ******************************************************************** */
/*                                                                      */
/*  Scenario                                                            */
/*                                                                      */
/*  Store a scenario. It is a list of order to execute                  */
/* ******************************************************************** */
package org.camunda.automator.definition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.camunda.automator.engine.AutomatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * the Scenario Head group a scenario definition
 */
public class Scenario {
    static Logger logger = LoggerFactory.getLogger(Scenario.class);

    private final List<ScenarioDeployment> deployments = new ArrayList<>();
    private final List<ScenarioStep> flows = new ArrayList<>();
    /**
     * Type UNIT
     */
    private final List<ScenarioExecution> executions = new ArrayList<>();

    public TYPESCENARIO typeScenario;

    /**
     * Type FLOW
     */
    private ScenarioWarmingUp warmingUp;
    private ScenarioFlowControl flowControl;
    private String name;
    private String version;
    private String processName;
    private String processId;
    /**
     * Server to run the scenario (optional, will be overide by the configuration)
     */
    private String serverName;
    private String serverType;
    /**
     * This value is fulfill only if the scenario was read from a file
     */
    private String scenarioFile = null;

    public static Scenario createFromJson(String jsonContent) throws AutomatorException {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        try {
            Gson gson = builder.create();
            Scenario scenario = gson.fromJson(jsonContent, Scenario.class);
            if (scenario == null) {
                logger.error("Scenario: Can't build scenario from content [{}]", jsonContent);
                return null;
            }
            scenario.afterUnSerialize();
            return scenario;
        } catch (Exception e) {
            logger.error("Scenario: can't unparse Json content [{}] : {}", jsonContent,e.getMessage(),e);
            throw new AutomatorException("Scenario: can't unparse GSon file:" + e.getMessage());
        }
    }

    public static Scenario createFromFile(Path scenarioFile) throws AutomatorException {
        try {

            Scenario scenario = createFromInputStream(new FileInputStream(scenarioFile.toFile()), scenarioFile.toAbsolutePath().toString());
            scenario.scenarioFile = scenarioFile.toAbsolutePath().toString();
            scenario.initialize();
            return scenario;

        } catch (FileNotFoundException e) {
            throw new AutomatorException("Can't access file [" + scenarioFile.getFileName() + "] " + e.getMessage());
        } catch (AutomatorException e) {
            throw e;
        }
    }

    /**
     * Load the scenario from a File
     *
     * @param scenarioInput InputStream to read
     * @return the scenario
     * @throws AutomatorException if file cannot be read or it's not a Json file
     */
    public static Scenario createFromInputStream(InputStream scenarioInput, String origin) throws AutomatorException {
        logger.info("Load Scenario [{}] from InputStream", origin);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(scenarioInput))) {
            StringBuilder jsonContent = new StringBuilder();
            String st;
            while ((st = br.readLine()) != null)
                jsonContent.append(st);

            Scenario scnHead = createFromJson(jsonContent.toString());
            if (scnHead == null) {
                throw new AutomatorException("Scenario: can't load from JSON [" + jsonContent + "] ");
            }
            scnHead.initialize();
            return scnHead;
        } catch (IOException e) {
            logger.error("CreateScenarioFromInputString: origin[{}] error {} ", origin, e.getMessage(), e);
            throw new AutomatorException("Can't load content from [" + origin + "] " + e.getMessage());
        }

    }

    /**
     * Initialize the scenario and complete it
     */
    private void initialize() {
    }

    /**
     * Add a new execution
     *
     * @return the scenario itself
     */
    public Scenario addExecution(ScenarioExecution scnExecution) {
        executions.add(scnExecution);
        return this;
    }

    public List<ScenarioExecution> getExecutions() {
        return executions;
    }

    public List<ScenarioStep> getFlows() {
        return flows;
    }

    public ScenarioWarmingUp getWarmingUp() {
        return warmingUp;
    }

    public ScenarioFlowControl getFlowControl() {
        return flowControl;
    }

    public List<ScenarioDeployment> getDeployments() {
        return deployments;
    }

    public String getName() {
        return name;
    }

    public Scenario setName(String name) {
        this.name = name;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public String getProcessName() {
        return processName;
    }

    public String getProcessId() {
        return processId;
    }

    public Scenario setProcessId(String processId) {
        this.processId = processId;
        return this;
    }

    public File getScenarioFile() {
        try {
            return new File(scenarioFile);
        } catch (Exception e) {
            logger.error("Can't access file [{}] : {} ", scenarioFile, e.getMessage(),e);
            return null;
        }
    }

    public String getServerName() {
        if (serverName == null || serverName.isEmpty())
            return null;
        return serverName;
    }

    private void afterUnSerialize() {
        // Attention, now we have to manually set the tree relation
        for (ScenarioExecution scnExecution : getExecutions()) {
            scnExecution.afterUnSerialize(this);
        }
    }


    /**
     * Return JSON information for the scenario
     * @param details
     * @return
     */
    public Map<String, Object> getJson( boolean details ) {
        HashMap jsonMap = new HashMap();
        jsonMap.putAll( Map.of("name", name == null ? "" : name,//
                "server", serverName == null ? "" : serverName, //
                "serverType", serverType == null ? "" : serverType, //
                "processId", processId == null ? "" : processId, //
                "typeScenario", typeScenario == null ? "" : typeScenario.toString()));
        if (!details)
            return jsonMap;

        jsonMap.put("executions", getExecutions().stream().map(ScenarioExecution::getJson).toList());
        return jsonMap;
    }

    public enum TYPESCENARIO {FLOW, UNIT}

}
