package org.camunda.automator.engine;

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.definition.ScenarioExecution;
import org.camunda.automator.definition.ScenarioVerification;
import org.camunda.automator.definition.ScenarioVerificationTask;
import org.camunda.automator.definition.ScenarioVerificationVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RunScenarioVerification {
  private final ScenarioExecution scnExecution;
  private final Logger logger = LoggerFactory.getLogger(RunScenarioVerification.class);

  RunScenarioVerification(ScenarioExecution scnExecution) {
    this.scnExecution = scnExecution;
  }

  public RunResult runVerifications(RunScenario runScenario, String processInstanceId) {
    RunResult result = new RunResult(runScenario);

    // we get a processInstanceId now
    ScenarioVerification verifications = scnExecution.getVerifications();
    for (ScenarioVerificationTask activity : verifications.getActivities()) {
      checkTask(runScenario, processInstanceId, activity, result);
    }
    for (ScenarioVerificationVariable variable : verifications.getVariables()) {
      checkVariable(runScenario, processInstanceId, variable, result);
    }
    return result;

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
      if (listTaskDescriptions.size() != verificationActivity.getNumberOfTasks()) {
        message.append("CheckTask: FAILED_NOTASK Search Task PID[");
        message.append(processInstanceId);
        message.append("] expected Task Name[");
        message.append(verificationActivity.taskId);
        message.append("] Number of tasks expected: ");
        message.append(verificationActivity.getNumberOfTasks());
        message.append(", found ");
        message.append(listTaskDescriptions.size());
      }
      // check the type for each taskDescription
      List<BpmnEngine.TaskDescription> listNotExpected = listTaskDescriptions.stream()
          .filter(t -> !(verificationActivity.getType() != null && verificationActivity.getType()
              .toString()
              .equalsIgnoreCase(t.type.toString())))
          .filter(t -> ((t.isCompleted && ScenarioVerificationTask.StepState.COMPLETED.toString()
              .equals(verificationActivity.state.toString())) || (!t.isCompleted
              && ScenarioVerificationTask.StepState.ACTIVE.toString().equals(verificationActivity.state.toString()))))
          .toList();
      if (!listNotExpected.isEmpty()) {
        message.append("CheckTask: FAILED_BADTYPE PID[");
        message.append(processInstanceId);
        message.append("] Task[");
        message.append(verificationActivity.taskId);
        message.append("] type expected [");
        message.append(verificationActivity.type.toString());
        message.append("] FAILED, received ");
        message.append(listNotExpected.stream().map(t -> t.taskId + ":" + t.type.toString()).toList());
      }
      result.addVerification(verificationActivity, message.isEmpty(), message.toString());

      if (runScenario.getRunParameters().isLevelDebug())
        logger.info("ScnScenarioVerification.CheckActivity [" + verificationActivity.getTaskId() + "] Success "
            + message.isEmpty() + " - " + message);
    } catch (AutomatorException e) {
      result.addVerification(verificationActivity, false, "Error " + e.getMessage());
    }

  }

  private void checkVariable(RunScenario runScenario,
                             String processInstanceId,
                             ScenarioVerificationVariable activity,
                             RunResult result) {
  }

}
