/* ******************************************************************** */
/*                                                                      */
/*  ScenarioExecutionResult                                                    */
/*                                                                      */
/*  Collect the result of an execution                                  */
/* ******************************************************************** */
package org.camunda.automator.engine;

import org.camunda.automator.definition.ScnHead;
import org.camunda.automator.definition.ScnStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScnRunResult {
  Logger logger = LoggerFactory.getLogger(ScnRunResult.class);

  /**
   * Scenario attached to this execution
   */
  private final ScnHead scenario;
  /**
   * List of error. If empty, the scenario was executed with success
   */
  private final List<ErrorDescription> listErrors = new ArrayList<>();
  private final List<StepExecution> listDetailsSteps = new ArrayList<>();
  /**
   * process instance started for this execution. The executionResult stand for only one process instance
   */
  private final List<String> listProcessInstancesId = new ArrayList<>();

  private int numberOfProcessInstances = 0;
  private int numberOfSteps = 0;
  /**
   * Time to execute it
   */
  private long timeExecution;

  private RunParameters runParameters;

  public ScnRunResult(ScnHead scenario, RunParameters runParameters) {
    this.scenario = scenario;
    this.runParameters = runParameters;
  }


  /* ******************************************************************** */
  /*                                                                      */
  /*  method used during the execution to collect information             */
  /*                                                                      */
  /* ******************************************************************** */

  /**
   * Add the process instance - this is mandatory to
   *
   * @param processInstanceId
   */
  public void addProcessInstanceId(String processInstanceId) {
    this.listProcessInstancesId.add(processInstanceId);
    numberOfProcessInstances++;
  }

  public void addTimeExecution(long timeToAdd) {
    this.timeExecution += timeToAdd;
  }

  public void addStepExecution(ScnStep step, long timeExecution) {
    addTimeExecution(timeExecution);
    numberOfSteps++;
    if (runParameters.isLevelStoreDetails()) {
      StepExecution scenarioExecution = new StepExecution(this);
      scenarioExecution.step = step;
      listDetailsSteps.add(scenarioExecution);
    }
  }

  public List<ErrorDescription> getListErrors() {
    return listErrors;
  }

  public void addError(ScnStep step, String explanation) {
    this.listErrors.add(new ErrorDescription(step, explanation));
    logger.error("scnResult: " + (step == null ? "" : step.getType().toString()) + " error " + explanation);

  }

  public void addError(ScnStep step, AutomatorException e) {
    this.listErrors.add(new ErrorDescription(step, e.getMessage()));
  }

  /**
   * Merge the result in this result
   *
   * @param result
   */
  public void add(ScnRunResult result) {
    addTimeExecution(result.getTimeExecution());
    listErrors.addAll(result.listErrors);
    numberOfProcessInstances += result.numberOfProcessInstances;
    numberOfSteps += result.numberOfSteps;
    // we collect the list only if the level is low
    if (runParameters.isLevelStoreDetails()) {
      listDetailsSteps.addAll(result.listDetailsSteps);
      listProcessInstancesId.addAll(result.listProcessInstancesId);
    }
  }

  public boolean isSuccess() {
    return listErrors.isEmpty();
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

  /**
   * @return
   */
  public String getSynthesis(boolean fullDetail) {
    StringBuilder synthesis = new StringBuilder();
    synthesis.append(listErrors.isEmpty() ? "SUCCESS " : "FAIL    ");
    synthesis.append(scenario.getName());
    synthesis.append("(");
    synthesis.append(scenario.getProcessId());
    synthesis.append("): ");

    synthesis.append(timeExecution);
    synthesis.append(" timeExecution(ms), ");
    synthesis.append(numberOfProcessInstances);
    synthesis.append(" processInstancesCreated, ");
    synthesis.append(numberOfSteps);
    synthesis.append(" stepsExecuted, ");

    // add errors
    synthesis.append(listErrors.stream() // stream
        .map(t -> {
          return (t.step != null ? t.step.toString() : "") + t.explanation;
        }).collect(Collectors.joining(",")));

    // add full details
    if (fullDetail && numberOfProcessInstances==listProcessInstancesId.size()) {
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
    public ScnStep step;
    public long timeExecution;
    private final ScnRunResult scenarioExecutionResult;

    public StepExecution(ScnRunResult scenarioExecutionResult) {
      this.scenarioExecutionResult = scenarioExecutionResult;
    }

    public void addError(ErrorDescription error) {
      listErrors.add(error);
    }
  }

  public static class ErrorDescription {
    ScnStep step;
    String explanation;

    public ErrorDescription(ScnStep step, String explanation) {
      this.step = step;
      this.explanation = explanation;
    }
  }

}
