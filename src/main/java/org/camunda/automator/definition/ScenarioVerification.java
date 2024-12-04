package org.camunda.automator.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ScenarioVerification {
    private final ScenarioExecution scenarioExecution;
    /**
     * List of duration to check
     * Maybe null due the Gson deserializer if there is no definition
     */
    private final List<ScenarioVerificationPerformance> performances = new ArrayList<>();
    /**
     * List of activities to check
     * Maybe null due the Gson deserializer if there is no definition
     */
    private List<ScenarioVerificationTask> activities = new ArrayList<>();
    /**
     * List of Variables to check
     * Maybe null due the Gson deserializer if there is no definition
     */
    private List<ScenarioVerificationVariable> variables = new ArrayList<>();
    /**
     * Variable to search the process instance, if only the verification is running
     * Maybe null due the Gson deserializer if there is no definition
     */
    private Map<String, Object> searchProcessInstanceByVariable;

    protected ScenarioVerification(ScenarioExecution scenarioExecution) {
        this.scenarioExecution = scenarioExecution;
    }

    public List<ScenarioVerificationTask> getActivities() {
        return activities == null ? Collections.emptyList() : activities;
    }

    public void setActivities(List<ScenarioVerificationTask> activities) {
        this.activities = activities;
    }

    public Map<String, Object> getSearchProcessInstanceByVariable() {
        return searchProcessInstanceByVariable == null ? Collections.emptyMap() : searchProcessInstanceByVariable;
    }

    public List<ScenarioVerificationVariable> getVariables() {
        return variables == null ? Collections.emptyList() : variables;
    }

    public void setVariables(List<ScenarioVerificationVariable> variables) {
        this.variables = variables;
    }

    public List<ScenarioVerificationPerformance> getPerformances() {
        return performances == null ? Collections.emptyList() : performances;
    }

    public ScenarioExecution getScenarioExecution() {
        return scenarioExecution;
    }

}
