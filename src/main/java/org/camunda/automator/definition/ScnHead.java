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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ScnHead {

  private final List<ScnExecution> executions = new ArrayList<>();
  private String name;
  private String version;
  private String processName;
  private String processId;
  private String modeVerification;

  public static ScnHead createFromJson(String jsonFile) {
    GsonBuilder builder = new GsonBuilder();
    builder.setPrettyPrinting();

    Gson gson = builder.create();
    ScnHead scnHead = gson.fromJson(jsonFile, ScnHead.class);
    scnHead.afterUnSerialize();
    return scnHead;
  }

  /**
   * Load the scenario from a File
   *
   * @param scenarioFile
   * @return the scenario
   * @throws Exception
   */
  public static ScnHead createFromFile(File scenarioFile) throws Exception {
    BufferedReader br = new BufferedReader(new FileReader(scenarioFile));
    StringBuilder jsonContent = new StringBuilder();
    String st;
    while ((st = br.readLine()) != null)
      jsonContent.append(st);

    return createFromJson(jsonContent.toString());
  }

  /**
   * Add a new execution
   *
   * @return
   */
  public ScnHead addExecution(ScnExecution scnExecution) {
    executions.add(scnExecution);
    return this;
  }

  public List<ScnExecution> getExecutions() {
    return executions;
  }

  public String getName() {
    return name;
  }

  public ScnHead setName(String name) {
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

  public ScnHead setProcessId(String processId) {
    this.processId = processId;
    return this;
  }

  public String getModeVerification() {
    return modeVerification;
  }

  private void afterUnSerialize() {
    // Attention, now we have to manually set the tree relation
    for (ScnExecution scnExecution : getExecutions()) {
      scnExecution.afterUnSerialize(this);
    }
  }

}
