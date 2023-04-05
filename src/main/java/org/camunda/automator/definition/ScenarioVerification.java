package org.camunda.automator.definition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScenarioVerification {
  List<ScenarioVerificationTask> activities = new ArrayList<>();
  List<ScenarioVerificationVariable> variables = new ArrayList<>();

  private final ScenarioExecution scenarioExecution;

  public Map<String,Object> searchProcessInstanceByVariable;


  protected ScenarioVerification(ScenarioExecution scenarioExecution) {
    this.scenarioExecution = scenarioExecution;
  }

  public List<ScenarioVerificationTask> getActivities() {
    return activities;
  }

  public void setActivities(List<ScenarioVerificationTask> activities) {
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
