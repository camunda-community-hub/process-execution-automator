package org.camunda.automator.definition;

import java.util.ArrayList;
import java.util.List;

public class ScenarioVerification {
  List<ScenarioVerificationActivity> activities = new ArrayList<>();

  private final ScenarioExecution scenarioExecution;

  protected ScenarioVerification(ScenarioExecution scenarioExecution) {
    this.scenarioExecution = scenarioExecution;
  }

  public List<ScenarioVerificationActivity> getActivities() {
    return activities;
  }

  public void setActivities(List<ScenarioVerificationActivity> activities) {
    this.activities = activities;
  }

  public ScenarioExecution getScenarioExecution() {
    return scenarioExecution;
  }

}
