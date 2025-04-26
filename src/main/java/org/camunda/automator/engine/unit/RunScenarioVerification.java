package org.camunda.automator.engine.unit;

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.definition.*;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RunScenarioVerification {
    private final ScenarioExecution scnExecution;
    private final Logger logger = LoggerFactory.getLogger(RunScenarioVerification.class);

    public RunScenarioVerification(ScenarioExecution scnExecution) {
        this.scnExecution = scnExecution;
    }

    public RunResult runVerifications(RunScenario runScenario, String processInstanceId) {
        RunResult runResult = new RunResult(runScenario, null);

        // we get a processInstanceId now
        ScenarioVerification verifications = scnExecution.getVerifications();
        for (ScenarioVerificationTask activity : verifications.getActivities()) {
            checkTask(runScenario, processInstanceId, activity, runResult);
        }
        for (ScenarioVerificationVariable variable : verifications.getVariables()) {
            checkVariable(runScenario, processInstanceId, variable, runResult);
        }
        for (ScenarioVerificationPerformance performance : verifications.getPerformances()) {
            checkPerformance(runScenario, processInstanceId, performance, runResult);
        }
        return runResult;

    }

    /**
     * Check to see if verificationActivity is correct or not
     *
     * @param runScenario          scenario to pilot the verification
     * @param processInstanceId    ProcessInstanceId to check
     * @param verificationActivity activity to check
     * @param result               the result object
     */
    private void checkTask(RunScenario runScenario,
                           String processInstanceId,
                           ScenarioVerificationTask verificationActivity,
                           RunResult result) {
        try {
            StringBuilder message = new StringBuilder();

            List<BpmnEngine.TaskDescription> listTaskDescriptions = runScenario.getBpmnEngine()
                    .searchTasksByProcessInstanceId(processInstanceId, verificationActivity.taskId, 100);
            message.append("CheckTask: PID[");
            message.append(processInstanceId);
            message.append("] VerifTaskName[");
            message.append(verificationActivity.taskId);

            // Does a type is expected?
            if (verificationActivity.getType() != null) {
                message.append("] type[");
                message.append(verificationActivity.getType());
                listTaskDescriptions = listTaskDescriptions.stream()
                        .filter(t -> (checkTypeTask(verificationActivity.getType(), t.type))) //
                        .toList();
            }

            // Does a state is expected?
            if (verificationActivity.state != null) {
                message.append("] State[");
                message.append(verificationActivity.state);
                listTaskDescriptions = listTaskDescriptions.stream()
                        .filter(t -> ((t.isCompleted && ScenarioVerificationTask.StepState.COMPLETED.toString()
                                .equals(verificationActivity.state.toString())) || (!t.isCompleted
                                && ScenarioVerificationTask.StepState.ACTIVE.toString().equals(verificationActivity.state.toString()))))
                        .toList();
            }

            // Now, check the result

            boolean isSuccess = listTaskDescriptions.size() == verificationActivity.getNumberOfTasks();

            message.append("] NumberExpectedTask: ");
            message.append(verificationActivity.getNumberOfTasks());
            message.append(" Found: ");
            message.append(listTaskDescriptions.size());
            message.append(" status: ");
            message.append(isSuccess);
            result.addVerification(verificationActivity, isSuccess, message.toString());

            if (runScenario.getRunParameters().showLevelMonitoring())
                logger.info("ScnScenarioVerification.CheckActivity [{}] Success {} - {} ", verificationActivity.getTaskId(),
                        isSuccess, message);
        } catch (AutomatorException e) {
            result.addVerification(verificationActivity, false, "Error " + e.getMessage());
        }

    }

    private boolean checkTypeTask(ScenarioStep.Step verificationType, ScenarioStep.Step taskType) {
        // No verification asked: open bar
        if (verificationType == null)
            return true;
        if (taskType == null)
            return false;
        if (verificationType.equals(taskType))
            return true;
        // if the verification is just TASK, this open a lot of option (all are considered as task)
        if (verificationType.equals(ScenarioStep.Step.TASK)
                && (taskType.equals(ScenarioStep.Step.SERVICETASK)
                || taskType.equals(ScenarioStep.Step.USERTASK)
                || taskType.equals(ScenarioStep.Step.SCRIPTTASK)))
        return true;
        return false;
    }

    private void checkVariable(RunScenario runScenario,
                               String processInstanceId,
                               ScenarioVerificationVariable verificationActivity,
                               RunResult result) {
        try {
            StringBuilder message = new StringBuilder();

            Map<String, Object> variables = runScenario.getBpmnEngine().getVariables(processInstanceId);
            message.append("CheckVariable: PID[");
            message.append(processInstanceId);
            message.append("] ExpectVariable[");
            message.append(verificationActivity.name);
            message.append("] ExpectedValue[");
            message.append(verificationActivity.value);

            boolean isSuccess = false;
            if (variables.containsKey(verificationActivity.name)) {
                Object value = variables.get(verificationActivity.name);
                message.append("] value[");
                message.append(value);
                if (value == null && verificationActivity.value == null)
                    isSuccess = true;
                else if (value == null || verificationActivity.value == null) {
                    isSuccess = false;
                } else {
                    // None of them is null here
                    isSuccess = value.toString().equals(verificationActivity.value.toString());
                }
            } else
                isSuccess = false;

            message.append("] status: ");
            message.append(isSuccess);

            result.addVerification(verificationActivity, isSuccess, message.toString());

            if (runScenario.getRunParameters().showLevelDebug())
                logger.info("ScnScenarioVerification.CheckVariable [{}] Success {} - {} ", verificationActivity.name,
                        isSuccess, message);
        } catch (AutomatorException e) {
            result.addVerification(verificationActivity, false, "Error " + e.getMessage());
        }
    }


    /**
     * checkPerformance
     *
     * @param runScenario          scenario to check
     * @param processInstanceId    processInstanceId
     * @param verificationActivity verificationActivity
     * @param result               complete the result
     */
    private void checkPerformance(RunScenario runScenario,
                                  String processInstanceId,
                                  ScenarioVerificationPerformance verificationActivity,
                                  RunResult result) {
        try {
            StringBuilder message = new StringBuilder();

            List<BpmnEngine.TaskDescription> listTaskDescriptions = runScenario.getBpmnEngine()
                    .searchTasksByProcessInstanceId(processInstanceId, null, 100);
            message.append("CheckPerformance: PID[");
            message.append(processInstanceId);
            message.append(" :");
            message.append(verificationActivity.getSynthesis());

            Optional<BpmnEngine.TaskDescription> taskFrom = listTaskDescriptions.stream().filter(t -> (t.taskId.equals(verificationActivity.fromFlowNode))).findFirst();
            Optional<BpmnEngine.TaskDescription> taskTo = listTaskDescriptions.stream().filter(t -> (t.taskId.equals(verificationActivity.toFlowNode))).findFirst();

            boolean isSuccess = true;

            if (!taskFrom.isPresent() || !taskTo.isPresent()) {
                isSuccess = false;
                message.append("Missing task: From [" + taskFrom.isPresent() + "] To [" + taskTo.isPresent() + "]");
            } else {
                Date dateFrom = verificationActivity.getFromMarker() == ScenarioVerificationPerformance.Marker.BEGIN ? taskFrom.get().startDate : taskFrom.get().endDate;
                Date endFrom = verificationActivity.getToMarker() == ScenarioVerificationPerformance.Marker.BEGIN ? taskTo.get().startDate : taskTo.get().endDate;
                long durationExecution = Math.abs(endFrom.getTime() - dateFrom.getTime());

                isSuccess = durationExecution < verificationActivity.getDurationInMs();
                message.append("ExpectExecution(ms): ");
                message.append(verificationActivity.getDurationInMs());
                message.append(" Execution(ms): ");
                message.append(durationExecution);
            }
            message.append(" status: ");
            message.append(isSuccess);

            result.addVerification(verificationActivity, isSuccess, message.toString());

            if (runScenario.getRunParameters().showLevelMonitoring())
                logger.info("ScnScenarioVerification.CheckActivity [{}] Success {} - {} ", verificationActivity.getSynthesis(),
                        isSuccess, message);
        } catch (AutomatorException e) {
            result.addVerification(verificationActivity, false, "Error " + e.getMessage());
        }
    }


}
