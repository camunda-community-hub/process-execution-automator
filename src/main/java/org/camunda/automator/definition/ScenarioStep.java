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
  /**
   * In case of a Flow Step, the number of workers to execute this tasks
   */
  private final Integer nbWorkers = Integer.valueOf(1);
  private final Long fixedBackOffDelay = Long.valueOf(0);
  /**
   * if the step is used in a WarmingUp operation, it can decide this is the time to finish it
   * Expression is
   * UserTaskThreashold(<taskId>,<numberOfTaskExpected>)
   */
  public String endWarmingUp;
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
   * In case of FlowStep, the processId to execute the step
   */
  private String processId;

  /**
   * MODE EXECUTION
   * WAIT: the worker wait the waitingTime time
   * ASYNCHRONOUS: the worker release the method, wait asynchrously the waiting time and send back the answer
   * ASYNCHRONOUSLIMITED: same as ASYNCHRONOUS, but use the maxClient information to not accept more than this number
   * In ASYNCHRONOUS, the method can potentially having millions of works in parallel (it accept <NumberOfClients> works,
   * but because it finish the method, then Zeebe Client will accept more works. So, with a waiting time of 1 mn, it may have a lot
   * of works in progress in the client.
   * This mode limit the number of current execution on the worker. it redeem immediately the method, but when we reach this
   * limitation, it froze the worker, waiting for a slot.
   */
  public enum MODEEXECUTION {WAIT, ASYNCHRONOUS, ASYNCHRONOUSLIMITED}

  private MODEEXECUTION modeExecution = MODEEXECUTION.WAIT;

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
    default -> {
    }
    }
  }

  public String getEndWarmingUp() {
    return endWarmingUp;
  }

  public MODEEXECUTION getModeExecution() {
    return modeExecution==null? MODEEXECUTION.WAIT : modeExecution;
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  Check consistence                                                              */
  /*                                                                      */
  /* ******************************************************************** */

  public enum Step {STARTEVENT, USERTASK, SERVICETASK, MESSAGE, ENDEVENT, EXCLUSIVEGATEWAY, PARALLELGATEWAY}

}
