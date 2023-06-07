/* ******************************************************************** */
/*                                                                      */
/*  Scenario                                                            */
/*                                                                      */
/*  Store a scenario. It is a list of order to execute                  */
/* ******************************************************************** */
package org.camunda.automator.definition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.camunda.automator.configuration.ConfigurationBpmEngine;
import org.camunda.automator.engine.AutomatorException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * the Scenario Head group a scenario definition
 */
public class Scenario {

  private final List<ScenarioExecution> executions = new ArrayList<>();
  private final List<ScenarioDeployment> deployments = new ArrayList<>();
  private final List<ScenarioStep> flows = new ArrayList<>();
  private ScenarioWarmingUp warmingUp;
  private ScenarioFlowControl flowControl;

  private String name;
  private String version;
  private String processName;
  private String processId;
  private String modeVerification;

  /**
   * Server to run the scenario
   */
  private String serverName;

  private String serverType;

  /**
   * This value is fulfill only if the scenario was read from a file
   */
  private String scenarioFile = null;


  public static Scenario createFromJson(String jsonFile) {
    GsonBuilder builder = new GsonBuilder();
    builder.setPrettyPrinting();

    Gson gson = builder.create();
    Scenario scnHead = gson.fromJson(jsonFile, Scenario.class);
    scnHead.afterUnSerialize();
    return scnHead;
  }

  /**
   * Load the scenario from a File
   *
   * @param scenarioFile file to read
   * @return the scenario
   * @throws AutomatorException if file cannot be read or it's not a Json file
   */
  public static Scenario createFromFile(File scenarioFile) throws AutomatorException {
    try (BufferedReader br = new BufferedReader(new FileReader(scenarioFile))) {
      StringBuilder jsonContent = new StringBuilder();
      String st;
      while ((st = br.readLine()) != null)
        jsonContent.append(st);

      Scenario scnHead = createFromJson(jsonContent.toString());
      scnHead.scenarioFile = scenarioFile.getAbsolutePath();
      return scnHead;
    } catch (Exception e) {
      throw new AutomatorException("Can't interpret JSON [" + scenarioFile.getAbsolutePath() + "] " + e.getMessage());
    }

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

  public ScenarioWarmingUp getWarmingUp() { return warmingUp;}

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
    return new File(scenarioFile);
  }

  public String getServerName() {
    if (serverName == null || serverName.isEmpty())
      return null;
    return serverName;
  }

  public ConfigurationBpmEngine.CamundaEngine getServerType() {
    try {
      return ConfigurationBpmEngine.CamundaEngine.valueOf(serverType.toUpperCase());
    } catch (Exception e) {
      return null;
    }

  }

  public String getModeVerification() {
    return modeVerification;
  }

  private void afterUnSerialize() {
    // Attention, now we have to manually set the tree relation
    for (ScenarioExecution scnExecution : getExecutions()) {
      scnExecution.afterUnSerialize(this);
    }
  }

}
