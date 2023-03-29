package org.camunda.automator.definition;

import java.util.ArrayList;
import java.util.List;

public class ScenarioVerification {
  List<ScenarioVerificationActivity> activities = new ArrayList<>();
  List<ScenarioVerificationVariable> variables = new ArrayList<>();

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

  public List<ScenarioVerificationVariable> getVariables() {
    return variables;
  }

  public void setVariables(List<ScenarioVerificationVariable> variables) {
    this.variables = variables;
  }

  public ScenarioExecution getScenarioExecution() {
    return scenarioExecution;
  }

}
