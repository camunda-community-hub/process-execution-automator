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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RunResult {
  /**
   * Scenario attached to this execution
   */
  private final RunScenario runScenario;
  /**
   * List of error. If empty, the scenario was executed with success
   */
  private final List<ErrorDescription> listErrors = new ArrayList<>();
  private final List<StepExecution> listDetailsSteps = new ArrayList<>();
  private final List<VerificationStatus> listVerifications = new ArrayList<>();
  /**
   * process instance started for this execution. The executionResult stand for only one process instance
   */
  private final List<String> listProcessInstancesId = new ArrayList<>();
  private final List<String> listProcessIdDeployed = new ArrayList<>();
  /**
   * Keep a photo of process instance created/failed per processid
   */
  private final Map<String, RecordCreationPI> recordCreationPIMap = new HashMap<>();
  Logger logger = LoggerFactory.getLogger(RunResult.class);
  private int numberOfSteps = 0;
  private int numberOfErrorSteps = 0;

  /**
   * Time to execute it
   */
  private long timeExecution;

  private Date startDate;
  private Date endDate;

  public RunResult(RunScenario runScenario) {
    this.runScenario = runScenario;

  }

  public Date getStartDate() {
    return this.startDate;
  }

  public void setStartDate(Date date) {
    this.startDate = date;
  }

  public Date getEndDate() {
    return this.endDate;
  }

  public void setEndDate(Date date) {
    this.endDate = date;
  }

  /**
   * Add the process instance - this is mandatory to
   *
   * @param processInstanceId processInstanceId to add
   */
  public void addProcessInstanceId(String processId, String processInstanceId) {
    this.listProcessInstancesId.add(processInstanceId);

    RecordCreationPI create = recordCreationPIMap.getOrDefault(processId, new RecordCreationPI(processId));
    create.nbCreated++;
    recordCreationPIMap.put(processId, create);
  }


  /* ******************************************************************** */
  /*                                                                      */
  /*  method used during the execution to collect information             */
  /*                                                                      */
  /* ******************************************************************** */

  /**
   * large flow: just register the number of PI
   */
  public void registerAddProcessInstance(String processId, boolean withSuccess) {
    RecordCreationPI create = recordCreationPIMap.getOrDefault(processId, new RecordCreationPI(processId));
    if (withSuccess)
      create.nbCreated++;
    else
      create.nbFailed++;
    recordCreationPIMap.put(processId, create);
  }

  public void addTimeExecution(long timeToAdd) {
    this.timeExecution += timeToAdd;
  }

  public void addStepExecution(ScenarioStep step, long timeExecution) {
    addTimeExecution(timeExecution);
    numberOfSteps++;
    if (runScenario.getRunParameters().showLevelInfo()) {
      StepExecution scenarioExecution = new StepExecution(this);
      scenarioExecution.step = step;
      listDetailsSteps.add(scenarioExecution);
    }
  }

  /**
   * large flow: just register the number of execution
   */
  public void registerAddStepExecution() {
    numberOfSteps++;
  }

  public void registerAddErrorStepExecution() {
    numberOfErrorSteps++;

  }

  public List<ErrorDescription> getListErrors() {
    return listErrors;
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  Errors                                                              */
  /*                                                                      */
  /* ******************************************************************** */

  public void addError(ScenarioStep step, String explanation) {
    this.listErrors.add(new ErrorDescription(step, explanation));
    logger.error((step == null ? "" : step.getType().toString()) + " " + explanation);

  }

  public void addError(ScenarioStep step, AutomatorException e) {
    this.listErrors.add(new ErrorDescription(step, e.getMessage()));
  }

  public void addVerification(ScenarioVerificationBasic verification, boolean isSuccess, String message) {
    VerificationStatus verificationStatus = new VerificationStatus();
    verificationStatus.verification = verification;
    verificationStatus.isSuccess = isSuccess;
    verificationStatus.message = message;
    this.listVerifications.add(verificationStatus);
  }

public boolean hasErrors() {
    return ! listErrors.isEmpty();
}

  /* ******************************************************************** */
  /*                                                                      */
  /*  Verifications                                                     */
  /*                                                                      */
  /* ******************************************************************** */

  public List<VerificationStatus> getListVerifications() {
    return listVerifications;
  }

  /**
   * Merge the result in this result
   *
   * @param result the result object
   */
  public void add(RunResult result) {
    addTimeExecution(result.getTimeExecution());
    listErrors.addAll(result.listErrors);
    listVerifications.addAll(result.listVerifications);
    for (Map.Entry<String, RecordCreationPI> entry : result.recordCreationPIMap.entrySet()) {
      RecordCreationPI currentReference = recordCreationPIMap.getOrDefault(entry.getKey(),
          new RecordCreationPI(entry.getKey()));
      currentReference.nbFailed += entry.getValue().nbFailed;
      currentReference.nbCreated += entry.getValue().nbCreated;

      recordCreationPIMap.put(entry.getKey(), currentReference);
    }
    numberOfSteps += result.numberOfSteps;
    numberOfErrorSteps += result.numberOfErrorSteps;
    // we collect the list only if the level is low
    if (runScenario.getRunParameters() != null && runScenario.getRunParameters().showLevelInfo()) {
      listDetailsSteps.addAll(result.listDetailsSteps);
      listProcessInstancesId.addAll(result.listProcessInstancesId);
    }
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  merge                                                               */
  /*                                                                      */
  /* ******************************************************************** */

  public boolean isSuccess() {
    long nbVerificationErrors = listVerifications.stream().filter(t -> !t.isSuccess).count();
    return listErrors.isEmpty() && nbVerificationErrors == 0;
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  method to get information                                           */
  /*                                                                      */
  /* ******************************************************************** */

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

  public Map<String, RecordCreationPI> getRecordCreationPI() {
    return recordCreationPIMap;
  }

  public long getRecordCreationPIAllProcesses() {
    long sum = 0;
    for (RecordCreationPI value : recordCreationPIMap.values())
      sum += value.nbCreated;
    return sum;
  }

  public int getNumberOfSteps() {
    return numberOfSteps;
  }

  public int getNumberOfErrorSteps() {
    return numberOfErrorSteps;
  }

  /**
   * @return a synthesis
   */
  public String getSynthesis(boolean fullDetail) {
    StringBuilder synthesis = new StringBuilder();
    synthesis.append((isSuccess() && ! hasErrors()) ? "SUCCESS " : "FAIL    ");
    synthesis.append(runScenario.getScenario().getName());
    synthesis.append("(");
    synthesis.append(runScenario.getScenario().getProcessId());
    synthesis.append("): ");

    StringBuilder append = synthesis.append(timeExecution);
    synthesis.append(" timeExecution(ms), ");
    RecordCreationPI recordCreationPI = recordCreationPIMap.get(runScenario.getScenario().getProcessId());
    synthesis.append(recordCreationPI==null? 0 : recordCreationPI.nbCreated);
    synthesis.append(" PICreated, ");
    synthesis.append(recordCreationPI==null? 0 : recordCreationPI.nbFailed);
    synthesis.append(" PIFailed, ");
    synthesis.append(numberOfSteps);
    synthesis.append(" stepsExecuted, ");
    synthesis.append(numberOfErrorSteps);
    synthesis.append(" errorStepsExecuted, ");

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
    if (fullDetail) {
      synthesis.append(" ListOfPICreated: ");

      synthesis.append(listProcessInstancesId.stream() // stream
          .collect(Collectors.joining(",")));
    }
    return synthesis.toString();

  }

  public static class StepExecution {
    public final List<ErrorDescription> listErrors = new ArrayList<>();
    private final RunResult scenarioExecutionResult;
    public ScenarioStep step;
    public long timeExecution;

    public StepExecution(RunResult scenarioExecutionResult) {
      this.scenarioExecutionResult = scenarioExecutionResult;
    }

    public void addError(ErrorDescription error) {
      listErrors.add(error);
    }
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  local class                                                         */
  /*                                                                      */
  /* ******************************************************************** */

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

  public static class RecordCreationPI {
    public String processId;
    public long nbCreated = 0;
    public long nbFailed = 0;

    public RecordCreationPI(String processId) {
      this.processId = processId;
    }

    public void add(RecordCreationPI record) {
      if (record == null)
        return;
      nbCreated += record.nbCreated;
      nbFailed += record.nbFailed;
    }
  }

  public class VerificationStatus {
    public ScenarioVerificationBasic verification;
    public boolean isSuccess;
    public String message;
  }
}
