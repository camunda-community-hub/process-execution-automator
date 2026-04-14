package org.camunda.automator.definition;

/**
 * The activity may be a task, or a message event
 */
public class ScenarioVerificationActivity implements ScenarioVerificationBasic {
    private final ScenarioVerification scenarioVerification;
    public ScenarioStep.Step type;
    public String taskId;
    public Integer numberOfTasks;
    public StepState state;

    public ScenarioVerificationActivity(ScenarioVerification scenarioVerification) {
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
        return numberOfTasks == null ? 1 : numberOfTasks;
    }

    public void setNumberOfTasks(int numberOfTasks) {
        this.numberOfTasks = numberOfTasks;
    }

    public ScenarioVerification getScenarioVerification() {
        return scenarioVerification;
    }

    public String getSynthesis() {
        return "TaskCheck[" + taskId + "]"
                + (state == null ? "" : (" State["+state.toString() + "]"));
    }

    public String getTypeVerification() {
        return "TASK";
    }

    public enum StepState {COMPLETED, ACTIVE}

}
