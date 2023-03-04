package org.camunda.automator.definition;

import java.util.ArrayList;
import java.util.List;

public class ScenarioVerification {
  List<ScenarioVerificationActivity> activities = new ArrayList<>();

  private final Scenario scenario;
  protected ScenarioVerification(Scenario scenario) {
    this.scenario = scenario;
  }

  public List<ScenarioVerificationActivity> getActivities() {
    return activities;
  }

  public void setActivities(List<ScenarioVerificationActivity> activities) {
    this.activities = activities;
  }

  public Scenario getScenario() {
    return scenario;
  }

}
