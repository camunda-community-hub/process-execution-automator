/* ******************************************************************** */
/*                                                                      */
/*  ScenarioExecutionResult                                                    */
/*                                                                      */
/*  Collect the result of an execution                                  */
/* ******************************************************************** */
package org.camunda.automator.engine;

import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.definition.ScenarioVerificationBasic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RunResult {
  Logger logger = LoggerFactory.getLogger(RunResult.class);

  /**
   * Scenario attached to this execution
   */
  private final RunScenario runScenario;
  /**
   * List of error. If empty, the scenario was executed with success
   */
  private final List<ErrorDescription> listErrors = new ArrayList<>();
  private final List<StepExecution> listDetailsSteps = new ArrayList<>();

  public class VerificationStatus {
    public ScenarioVerificationBasic verification;
    public boolean isSuccess;
    public String message;
  }


  private final List<VerificationStatus> listVerifications = new ArrayList<>();

  /**
   * process instance started for this execution. The executionResult stand for only one process instance
   */
  private final List<String> listProcessInstancesId = new ArrayList<>();

  private final List<String> listProcessIdDeployed = new ArrayList<>();

  private int numberOfProcessInstances = 0;
  private int numberOfSteps = 0;
  /**
   * Time to execute it
   */
  private long timeExecution;

  public RunResult(RunScenario runScenario) {
    this.runScenario = runScenario;

  }


  /* ******************************************************************** */
  /*                                                                      */
  /*  method used during the execution to collect information             */
  /*                                                                      */
  /* ******************************************************************** */

  /**
   * Add the process instance - this is mandatory to
   *
   * @param processInstanceId processInstanceId to add
   */
  public void addProcessInstanceId(String processInstanceId) {
    this.listProcessInstancesId.add(processInstanceId);
    numberOfProcessInstances++;
  }

  public void addTimeExecution(long timeToAdd) {
    this.timeExecution += timeToAdd;
  }

  public void addStepExecution(ScenarioStep step, long timeExecution) {
    addTimeExecution(timeExecution);
    numberOfSteps++;
    if (runScenario.getRunParameters().isLevelInfo()) {
      StepExecution scenarioExecution = new StepExecution(this);
      scenarioExecution.step = step;
      listDetailsSteps.add(scenarioExecution);
    }
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  Errors                                                              */
  /*                                                                      */
  /* ******************************************************************** */

  public List<ErrorDescription> getListErrors() {
    return listErrors;
  }

  public void addError(ScenarioStep step, String explanation) {
    this.listErrors.add(new ErrorDescription(step, explanation));
    logger.error("scnResult: " + (step == null ? "" : step.getType().toString()) + " error " + explanation);

  }

  public void addError(ScenarioStep step, AutomatorException e) {
    this.listErrors.add(new ErrorDescription(step, e.getMessage()));
  }



  /* ******************************************************************** */
  /*                                                                      */
  /*  Verifications                                                     */
  /*                                                                      */
  /* ******************************************************************** */


  public void addVerification(ScenarioVerificationBasic verification, boolean isSuccess, String message) {
    VerificationStatus verificationStatus = new VerificationStatus();
    verificationStatus.verification = verification;
    verificationStatus.isSuccess = isSuccess;
    verificationStatus.message = message;
    this.listVerifications.add(verificationStatus);
  }

  public List<VerificationStatus> getListVerifications() {
    return listVerifications;
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  merge                                                               */
  /*                                                                      */
  /* ******************************************************************** */

  /**
   * Merge the result in this result
   *
   * @param result the result object
   */
  public void add(RunResult result) {
    addTimeExecution(result.getTimeExecution());
    listErrors.addAll(result.listErrors);
    listVerifications.addAll(result.listVerifications);
    numberOfProcessInstances += result.numberOfProcessInstances;
    numberOfSteps += result.numberOfSteps;
    // we collect the list only if the level is low
    if (runScenario.getRunParameters()!=null && runScenario.getRunParameters().isLevelInfo()) {
      listDetailsSteps.addAll(result.listDetailsSteps);
      listProcessInstancesId.addAll(result.listProcessInstancesId);
    }
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  method to get information                                           */
  /*                                                                      */
  /* ******************************************************************** */

  public boolean isSuccess() {
    long nbVerificationErrors = listVerifications.stream().filter(t -> !t.isSuccess).count();
    return listErrors.isEmpty() && nbVerificationErrors == 0;
  }

  public String getFirstProcessInstanceId() {
    return listProcessInstancesId.isEmpty() ? null : listProcessInstancesId.get(0);
  }

  public List<String> getProcessInstanceId() {
    return this.listProcessInstancesId;
  }

  public long getTimeExecution() {
    return timeExecution;
  }

  public void setTimeExecution(long timeExecution) {
    this.timeExecution = timeExecution;
  }

  public List<String> getProcessIdDeployed() {
    return listProcessIdDeployed;
  }

  public void addDeploymentProcessId(String processId) {
    this.listProcessIdDeployed.add(processId);
  }

  /**
   * @return a synthesis
   */
  public String getSynthesis(boolean fullDetail) {
    StringBuilder synthesis = new StringBuilder();
    synthesis.append(isSuccess() ? "SUCCESS " : "FAIL    ");
    synthesis.append(runScenario.getScenario().getName());
    synthesis.append("(");
    synthesis.append(runScenario.getScenario().getProcessId());
    synthesis.append("): ");

    synthesis.append(timeExecution);
    synthesis.append(" timeExecution(ms), ");
    synthesis.append(numberOfProcessInstances);
    synthesis.append(" processInstancesCreated, ");
    synthesis.append(numberOfSteps);
    synthesis.append(" stepsExecuted, ");

    StringBuilder errorMessage = new StringBuilder();
    // add errors
    errorMessage.append(listErrors.stream() // stream
        .map(t -> {
          return (t.step != null ? t.step.toString() : "") + t.explanation + "\n";
        }).collect(Collectors.joining(",")));

    if (fullDetail) {
      synthesis.append(errorMessage);
    }
    StringBuilder verificationMessage = new StringBuilder();
    verificationMessage.append(listVerifications.stream().map(t -> {
      return t.verification.getSynthesis() + "? " + (t.isSuccess ? "OK" : "FAIL") + " " + t.message + "\n";
    }).collect(Collectors.joining(",")));
    if (fullDetail) {
      synthesis.append(verificationMessage);
    }
    // add full details
    if (fullDetail && numberOfProcessInstances == listProcessInstancesId.size()) {
      synthesis.append(" ListOfProcessInstancesCreated: ");

      synthesis.append(listProcessInstancesId.stream() // stream
          .collect(Collectors.joining(",")));
    }
    return synthesis.toString();

  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  local class                                                         */
  /*                                                                      */
  /* ******************************************************************** */

  public static class StepExecution {
    public final List<ErrorDescription> listErrors = new ArrayList<>();
    public ScenarioStep step;
    public long timeExecution;
    private final RunResult scenarioExecutionResult;

    public StepExecution(RunResult scenarioExecutionResult) {
      this.scenarioExecutionResult = scenarioExecutionResult;
    }

    public void addError(ErrorDescription error) {
      listErrors.add(error);
    }
  }

  public static class ErrorDescription {
    public ScenarioStep step;
    public ScenarioVerificationBasic verificationBasic;
    public String explanation;

    public ErrorDescription(ScenarioStep step, String explanation) {
      this.step = step;
      this.explanation = explanation;
    }

    public ErrorDescription(ScenarioVerificationBasic verificationBasic, String explanation) {
      this.verificationBasic = verificationBasic;
      this.explanation = explanation;
    }
  }

}
