package org.camunda.automator.definition;

public class ScenarioVerificationActivity {
  public String type;
  public String activityId;
  public int numberOfTasks;

  private final ScenarioVerification scenarioVerification;

  public ScenarioVerificationActivity(ScenarioVerification scenarioVerification) {
    this.scenarioVerification = scenarioVerification;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getActivityId() {
    return activityId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  public int getNumberOfTasks() {
    return numberOfTasks;
  }

  public void setNumberOfTasks(int numberOfTasks) {
    this.numberOfTasks = numberOfTasks;
  }

  public ScenarioVerification getScenarioVerification() {
    return scenarioVerification;
  }
}
