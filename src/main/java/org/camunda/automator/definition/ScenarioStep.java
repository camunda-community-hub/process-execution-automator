/* ******************************************************************** */
/*                                                                      */
/*  ScenarioStep                                                    */
/*                                                                      */
/*  One step in the scenario                                                    */

/* ******************************************************************** */
package org.camunda.automator.definition;

import java.util.Collections;
import java.util.Map;

public class ScenarioStep {

  private ScenarioExecution scnExecution;
  private Step type;
  private String activityId;
  private Map<String, Object> variables = Collections.emptyMap();


  /**
   * each component contains an operations, to fulfill variables
   * operations; stringtodate()
   */
  private Map<String, String> variablesoperation = Collections.emptyMap();

  private String userId;

  /**
   * ISO 8601: PT10S
   */
  private String delay;

  /**
   * ISO 8601: PT10S
   */
  private String waitingTime;

  /**
   * Optional, may not b
   */
  private Integer numberOfExecutions;

  public ScenarioStep(ScenarioExecution scnExecution) {
    this.scnExecution = scnExecution;
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  Creator and setter to help the API                                  */
  /*                                                                      */
  /* ******************************************************************** */

  public static ScenarioStep createStepCreate(ScenarioExecution scnExecution, String starterId) {
    ScenarioStep scenarioStep = new ScenarioStep(scnExecution);
    scenarioStep.type = Step.STARTEVENT;
    scenarioStep.activityId = starterId;
    return scenarioStep;
  }

  public static ScenarioStep createStepUserTask(ScenarioExecution scnExecution, String activityId) {
    ScenarioStep scenarioStep = new ScenarioStep(scnExecution);
    scenarioStep.type = Step.USERTASK;
    scenarioStep.activityId = activityId;
    return scenarioStep;
  }

  public Step getType() {
    return type;
  }

  public ScenarioStep setType(Step type) {
    this.type = type;
    return this;
  }

  public String getActivityId() {
    return activityId;
  }

  public ScenarioStep setActivityId(String activityId) {
    this.activityId = activityId;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  getter                                                              */
  /*                                                                      */
  /* ******************************************************************** */

  public ScenarioStep setUserId(String userId) {
    this.userId = userId;
    return this;
  }

  public Map<String, String> getVariablesOperations()  {
    return variablesoperation ==null? Collections.emptyMap():variablesoperation;
  }

  public Map<String, Object> getVariables() {
    return variables==null? Collections.emptyMap():variables;
  }

  public ScenarioStep setVariables(Map<String, Object> variables) {
    this.variables = variables;
    return this;
  }

  public String getDelay() {
    return delay;
  }

  public ScenarioStep setDelay(String delay) {
    this.delay = delay;
    return this;
  }

  public void setScnExecution(ScenarioExecution scnExecution) {
    this.scnExecution = scnExecution;
  }

  public String getWaitingTime() {
    return waitingTime;
  }

  public void setWaitingTime(String waitingTime) {
    this.waitingTime = waitingTime;
  }

  public ScenarioExecution getScnExecution() {
    return scnExecution;
  }

  public int getNumberOfExecutions() {
    return numberOfExecutions==null? 1 : numberOfExecutions;
  }

  public void setNumberOfExecutions(int numberOfExecutions) {
    this.numberOfExecutions = numberOfExecutions;
  }

  protected void afterUnSerialize(ScenarioExecution scnExecution) {
    this.scnExecution = scnExecution;
  }

  public enum Step {STARTEVENT, USERTASK, SERVICETASK, MESSAGE, ENDEVENT}

}
