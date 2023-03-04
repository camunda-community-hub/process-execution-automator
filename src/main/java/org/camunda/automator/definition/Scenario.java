/* ******************************************************************** */
/*                                                                      */
/*  Scenario                                                     */
/*                                                                      */
/*  Store a scenario                                                    */
/* a scenario is a list of order to execute
/* ******************************************************************** */
package org.camunda.automator.definition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
  private final List<ScenarioVerification> verifications = new ArrayList<>();
  private String name;
  private String version;
  private String processName;
  private String processId;
  private String modeVerification;

  /**
   * This value is fulfill only if the scenario was read from a file
   */
  private transient File scenarioFile=null;


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
   * @throws Exception
   */
  public static Scenario createFromFile(File scenarioFile) throws AutomatorException {
    try {
      BufferedReader br = new BufferedReader(new FileReader(scenarioFile));
      StringBuilder jsonContent = new StringBuilder();
      String st;
      while ((st = br.readLine()) != null)
        jsonContent.append(st);

      Scenario scnHead= createFromJson(jsonContent.toString());
      scnHead.scenarioFile = scenarioFile;
      return scnHead;
    } catch (Exception e) {
      throw new AutomatorException("Can't read ["+scenarioFile.getAbsolutePath()+"] "+ e.getMessage());
    }

  }

  /**
   * Add a new execution
   *
   * @return
   */
  public Scenario addExecution(ScenarioExecution scnExecution) {
    executions.add(scnExecution);
    return this;
  }

  public List<ScenarioExecution> getExecutions() {
    return executions;
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

  public File getScenarioFile() {
    return scenarioFile;
  }
  public Scenario setProcessId(String processId) {
    this.processId = processId;
    return this;
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
