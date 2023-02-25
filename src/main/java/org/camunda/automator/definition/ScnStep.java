/* ******************************************************************** */
/*                                                                      */
/*  ScenarioStep                                                    */
/*                                                                      */
/*  One step in the scenario                                                    */

/* ******************************************************************** */
package org.camunda.automator.definition;

import java.util.Collections;
import java.util.Map;

public class ScnStep {

  private ScnExecution scnExecution;
  private Step type;
  private String activityId;
  private Map<String, Object> variables = Collections.emptyMap();
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

  public ScnStep(ScnExecution scnExecution) {
    this.scnExecution = scnExecution;
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  Creator and setter to help the API                                  */
  /*                                                                      */
  /* ******************************************************************** */

  public static ScnStep createStepCreate(ScnExecution scnExecution, String starterId) {
    ScnStep scenarioStep = new ScnStep(scnExecution);
    scenarioStep.type = Step.STARTEVENT;
    scenarioStep.activityId = starterId;
    return scenarioStep;
  }

  public static ScnStep createStepUserTask(ScnExecution scnExecution, String activityId) {
    ScnStep scenarioStep = new ScnStep(scnExecution);
    scenarioStep.type = Step.USERTASK;
    scenarioStep.activityId = activityId;
    return scenarioStep;
  }

  public Step getType() {
    return type;
  }

  public ScnStep setType(Step type) {
    this.type = type;
    return this;
  }

  public String getActivityId() {
    return activityId;
  }

  public ScnStep setActivityId(String activityId) {
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

  public ScnStep setUserId(String userId) {
    this.userId = userId;
    return this;
  }

  public Map<String, Object> getVariables() {
    return variables;
  }

  public ScnStep setVariables(Map<String, Object> variables) {
    this.variables = variables;
    return this;
  }

  public String getDelay() {
    return delay;
  }

  public ScnStep setDelay(String delay) {
    this.delay = delay;
    return this;
  }

  public void setScnExecution(ScnExecution scnExecution) {
    this.scnExecution = scnExecution;
  }

  public String getWaitingTime() {
    return waitingTime;
  }

  public void setWaitingTime(String waitingTime) {
    this.waitingTime = waitingTime;
  }

  public ScnExecution getScnExecution() {
    return scnExecution;
  }

  public int getNumberOfExecutions() {
    return numberOfExecutions==null? 1 : numberOfExecutions;
  }

  public void setNumberOfExecutions(int numberOfExecutions) {
    this.numberOfExecutions = numberOfExecutions;
  }

  protected void afterUnSerialize(ScnExecution scnExecution) {
    this.scnExecution = scnExecution;
  }

  public enum Step {STARTEVENT, USERTASK, MESSAGE, ENDEVENT}

}
