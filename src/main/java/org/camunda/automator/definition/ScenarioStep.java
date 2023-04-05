/* ******************************************************************** */
/*                                                                      */
/*  ScenarioStep                                                    */
/*                                                                      */
/*  One step in the scenario                                                    */

/* ******************************************************************** */
package org.camunda.automator.definition;

import org.camunda.automator.engine.AutomatorException;

import java.util.Collections;
import java.util.Map;

public class ScenarioStep {

  private ScenarioExecution scnExecution;
  private Step type;
  private String taskId;
  /**
   * to execute a service task in C8, topic is mandatory
   */
  private String topic;
  private Map<String, Object> variables = Collections.emptyMap();

  /**
   * each component contains an operations, to fulfill variables
   * operations; stringtodate()
   */
  private final Map<String, String> variablesOperation = Collections.emptyMap();

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
    scenarioStep.taskId = starterId;
    return scenarioStep;
  }

  public static ScenarioStep createStepUserTask(ScenarioExecution scnExecution, String activityId) {
    ScenarioStep scenarioStep = new ScenarioStep(scnExecution);
    scenarioStep.type = Step.USERTASK;
    scenarioStep.taskId = activityId;
    return scenarioStep;
  }

  public Step getType() {
    return type;
  }

  public ScenarioStep setType(Step type) {
    this.type = type;
    return this;
  }

  public String getTaskId() {
    return taskId;
  }

  public ScenarioStep setTaskId(String taskId) {
    this.taskId = taskId;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public String getTopic() {
    return topic;
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

  public Map<String, String> getVariablesOperations() {
    return variablesOperation == null ? Collections.emptyMap() : variablesOperation;
  }

  public Map<String, Object> getVariables() {
    return variables == null ? Collections.emptyMap() : variables;
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
    return numberOfExecutions == null ? 1 : numberOfExecutions;
  }

  public void setNumberOfExecutions(int numberOfExecutions) {
    this.numberOfExecutions = numberOfExecutions;
  }

  protected void afterUnSerialize(ScenarioExecution scnExecution) {
    this.scnExecution = scnExecution;
  }

  public enum Step {STARTEVENT, USERTASK, SERVICETASK, MESSAGE, ENDEVENT, EXCLUSIVEGATEWAY, PARALLELGATEWAY}

  /* ******************************************************************** */
  /*                                                                      */
  /*  Check consistence                                                              */
  /*                                                                      */
  /* ******************************************************************** */

  public void checkConsistence() throws AutomatorException {
    if (getTaskId() == null || getTaskId().trim().isEmpty())
      throw new AutomatorException("Step taskId is mandatory");
    switch (type) {
    case SERVICETASK -> {
      if (getTopic() == null || getTopic().trim().isEmpty())
        throw new AutomatorException("Step.SERVICETASK: " + getTaskId() + " topic is mandatory");
    }
    }
  }

}
