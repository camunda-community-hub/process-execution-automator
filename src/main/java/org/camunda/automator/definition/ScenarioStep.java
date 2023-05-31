/* ******************************************************************** */
/*                                                                      */
/*  ScenarioStep                                                    */
/*                                                                      */
/*  One step in the scenario                                                    */

/* ******************************************************************** */
package org.camunda.automator.definition;

import org.camunda.automator.engine.AutomatorException;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

public class ScenarioStep {

  /**
   * each component contains an operations, to fulfill variables
   * operations; stringtodate()
   */
  private final Map<String, String> variablesOperation = Collections.emptyMap();
  private ScenarioExecution scnExecution;
  private Step type;
  private String taskId;
  /**
   * to execute a service task in C8, topic is mandatory
   */
  private String topic;
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
   * Optional, may not be set
   */
  private Integer numberOfExecutions;

  /**
   * In case of a Flow step, the frequency to execute this step, for example PT10S every 10 seconds
   */
  private String frequency;

  /**
   * In case of a Flow Step, the number of workers to execute this tasks
   */
  private final Integer nbWorkers = Integer.valueOf(1);

  /**
   * In case of FlowStep, the processId to execute the step
   */
  private String processId;

  private final Long fixedBackOffDelay = Long.valueOf(0);

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

  public ScenarioStep setUserId(String userId) {
    this.userId = userId;
    return this;
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  getter                                                              */
  /*                                                                      */
  /* ******************************************************************** */

  public String getTopic() {
    return topic;
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

  public String getWaitingTime() {
    return waitingTime;
  }

  public void setWaitingTime(String waitingTime) {
    this.waitingTime = waitingTime;
  }

  /**
   * Return the waiting time in Duration
   *
   * @return Duration, defaultDuration if error
   */
  public Duration getWaitingTimeDuration(Duration defaultDuration) {
    try {
      return Duration.parse(waitingTime);
    } catch (Exception e) {
      return defaultDuration;
    }
  }

  public ScenarioExecution getScnExecution() {
    return scnExecution;
  }

  public void setScnExecution(ScenarioExecution scnExecution) {
    this.scnExecution = scnExecution;
  }

  public int getNumberOfExecutions() {
    return numberOfExecutions == null ? 1 : numberOfExecutions;
  }

  public void setNumberOfExecutions(int numberOfExecutions) {
    this.numberOfExecutions = numberOfExecutions;
  }

  public String getFrequency() {
    return frequency;
  }

  public int getNbWorkers() {
    return nbWorkers == null || nbWorkers == 0 ? 1 : nbWorkers;
  }

  public String getProcessId() {
    return processId;
  }

  public long getFixedBackOffDelay() {
    return fixedBackOffDelay == null ? 0 : fixedBackOffDelay;
  }

  protected void afterUnSerialize(ScenarioExecution scnExecution) {
    this.scnExecution = scnExecution;
  }

  public void checkConsistence() throws AutomatorException {
    if (getTaskId() == null || getTaskId().trim().isEmpty())
      throw new AutomatorException("Step taskId is mandatory");
    switch (type) {
    case SERVICETASK -> {
      if (getTopic() == null || getTopic().trim().isEmpty())
        throw new AutomatorException("Step.SERVICETASK: " + getTaskId() + " topic is mandatory");
    }
    default -> {}
    }
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  Check consistence                                                              */
  /*                                                                      */
  /* ******************************************************************** */

  public enum Step {STARTEVENT, USERTASK, SERVICETASK, MESSAGE, ENDEVENT, EXCLUSIVEGATEWAY, PARALLELGATEWAY}

}
