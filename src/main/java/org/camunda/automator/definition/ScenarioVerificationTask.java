package org.camunda.automator.definition;

public class ScenarioVerificationTask implements ScenarioVerificationBasic{
  public ScenarioStep.Step type;
  public String taskId;
  public Integer numberOfTasks;
  public enum StepState { COMPLETED, ACTIVE}
  public StepState state;

  private final ScenarioVerification scenarioVerification;

  public ScenarioVerificationTask(ScenarioVerification scenarioVerification) {
    this.scenarioVerification = scenarioVerification;
  }

  public ScenarioStep.Step getType() {
    return type;
  }

  public void setType(ScenarioStep.Step type) {
    this.type = type;
  }

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public int getNumberOfTasks() {
    return numberOfTasks==null? 1 : numberOfTasks;
  }

  public void setNumberOfTasks(int numberOfTasks) {
    this.numberOfTasks = numberOfTasks;
  }

  public ScenarioVerification getScenarioVerification() {
    return scenarioVerification;
  }

  public String getSynthesis() {
    return "ActivityCheck ["+ taskId +"] "+state.toString();
  }

}
