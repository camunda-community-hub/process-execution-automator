package org.camunda.automator.engine.unit;

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.definition.ScenarioExecution;
import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunParameters;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * one ExecutionStep in a runScenario
 */
public class RunScenarioUnit {
  private final Logger logger = LoggerFactory.getLogger(RunScenarioUnit.class);
  private final RunScenario runScenario;
  private final ScenarioExecution scnExecution;
  private String agentName = "";

  public RunScenarioUnit(RunScenario runScenario, ScenarioExecution scnExecution) {
    this.runScenario = runScenario;
    this.scnExecution = scnExecution;
  }

  public void setAgentName(String name) {
    this.agentName = name;
  }

  /**
   * Execute the scenario.
   * Note: this method is multi thread safe.
   * Each execution has its own Thread
   *
   * @return the execution
   */
  public RunResult runExecution() {
    RunResult resultExecution = new RunResult(runScenario);

    if (runScenario.getRunParameters().showLevelMonitoring()) {
      logger.info("ScnRunExecution." + agentName + ": Start Execution [" + scnExecution.getName() + "] ");
    }
    ExecutorService executor = Executors.newFixedThreadPool(scnExecution.getNumberOfThreads());

    List<Future<?>> listFutures = new ArrayList<>();

    for (int i = 0; i < scnExecution.getNumberProcessInstances(); i++) {
      ScnThreadExecutionCallable scnExecutionCallable = new ScnThreadExecutionCallable("AutomatorThread-" + i, this,
          runScenario.getRunParameters());

      listFutures.add(executor.submit(scnExecutionCallable));
    }

    // wait the end of all executions
    try {
      for (Future<?> f : listFutures) {
        Object scnRunResult = f.get();
        resultExecution.add((RunResult) scnRunResult);

      }

    } catch (Exception e) {
      resultExecution.addError(null, "Error during executing in parallel " + e.getMessage());
    }

    if (runScenario.getRunParameters().showLevelMonitoring()) {
      logger.info("ScnRunExecution." + agentName + ": End Execution [" + scnExecution.getName() + "] success? "
          + resultExecution.isSuccess());
    }
    return resultExecution;
  }






  /* ******************************************************************** */
  /*                                                                      */
  /*  ScnThreadCallable : execute one Execution per thread                                                   */
  /*                                                                      */
  /* ******************************************************************** */

  private class ScnThreadExecutionCallable implements Callable {
    private final String agentName;
    private final RunScenarioUnit scnRunExecution;
    private final RunParameters runParameters;

    private RunResult scnRunResult;

    ScnThreadExecutionCallable(String agentName, RunScenarioUnit scnRunExecution, RunParameters runParameters) {
      this.agentName = agentName;
      this.scnRunExecution = scnRunExecution;
      this.runParameters = runParameters;
    }

    /**
     * run one execution.
     *
     * @return the result object
     * @throws Exception in case of error
     */
    public Object call() throws Exception {
      scnRunResult = new RunResult(scnRunExecution.runScenario);
      if (runParameters.isExecution())
        runExecution();

      // two uses case here:
      // Execution AND verifications: for each process Instance created, a verification is running
      // Only VERIFICATION: the verification ojbect define a filter to search existing process instance. Verification is perform againts this list
      if (runParameters.isVerification() && (scnExecution.getVerifications() != null)) {
        if (runParameters.isExecution()) {
          // we just finish executing process instance, so wait 30 S to let the engine finish
          try {
            Thread.sleep(30 * 1000);
          } catch (Exception e) {
            // nothing to do
          }
          runVerifications();
        } else {
          // use the search criteria
          if (scnExecution.getVerifications().getSearchProcessInstanceByVariable().isEmpty()) {
            scnRunResult.addVerification(null, false, "No Search Instance by Variable is defined");
          } else {
            List<BpmnEngine.ProcessDescription> listProcessInstances = runScenario.getBpmnEngine()
                .searchProcessInstanceByVariable(scnExecution.getScnHead().getProcessId(),
                    scnExecution.getVerifications().getSearchProcessInstanceByVariable(), 100);

            for (BpmnEngine.ProcessDescription processInstance : listProcessInstances) {
              scnRunResult.addProcessInstanceId(scnExecution.getScnHead().getProcessId(),
                  processInstance.processInstanceId);
            }
            runVerifications();
          }
        }

      }
      // we finish with this process instance
      if (scnRunResult.getFirstProcessInstanceId() != null && runParameters.isClearAllAfter())
        runScenario.getBpmnEngine()
            .endProcessInstance(scnRunResult.getFirstProcessInstanceId(), runParameters.isClearAllAfter());
      return scnRunResult;
    }

    public void runExecution() {

      RunScenarioUnitServiceTask serviceTask = new RunScenarioUnitServiceTask(runScenario);
      RunScenarioUnitUserTask userTask = new RunScenarioUnitUserTask(runScenario);
      RunScenarioUnitStartEvent startEvent = new RunScenarioUnitStartEvent(runScenario);

      if (scnRunExecution.runScenario.getRunParameters().showLevelMonitoring())
        logger.info(
            "ScnRunExecution.StartExecution [" + scnRunExecution.runScenario.getScenario().getName() + "] agent["
                + agentName + "]");

      for (ScenarioStep step : scnExecution.getSteps()) {

        if (scnRunExecution.runScenario.getRunParameters().showLevelDebug())
          logger.info(
              "ScnRunExecution.StartExecution.Execute [" + scnRunExecution.runScenario.getScenario().getName() + "."
                  + step.getTaskId() + " agent[" + agentName + "]");

        try {
          step.checkConsistence();
        } catch (AutomatorException e) {
          scnRunResult.addError(step, e.getMessage());
          continue;
        }
        long timeBegin = System.currentTimeMillis();
        if (step.getType() == null) {
          scnRunResult.addError(step, "Unknown type");
          continue;
        }
        switch (step.getType()) {
        case STARTEVENT -> {
          if (scnRunExecution.runScenario.getRunParameters().isCreation())
            scnRunResult = startEvent.startEvent(scnRunResult, step);
        }
        case USERTASK -> {
          // wait for the user Task
          if (scnRunExecution.runScenario.getRunParameters().isUserTask())
            scnRunResult = userTask.executeUserTask(step, scnRunResult);
        }
        case SERVICETASK -> {
          // wait for the user Task
          if (scnRunExecution.runScenario.getRunParameters().isServiceTask()) {
            scnRunResult = serviceTask.executeServiceTask(step, scnRunResult);
          }
        }

        case ENDEVENT -> {
        }

        case MESSAGE -> {
        }
        }
        long timeEnd = System.currentTimeMillis();
        scnRunResult.addStepExecution(step, timeEnd - timeBegin);

        if (!scnRunResult.isSuccess() && ScenarioExecution.Policy.STOPATFIRSTERROR.equals(scnExecution.getPolicy()))
          return;
      }
      if (scnRunExecution.runScenario.getRunParameters().showLevelMonitoring())
        logger.info("ScnRunExecution.EndExecution [" + scnExecution.getName() + "] agent[" + agentName + "]");
    }

    /**
     * Run the verification just after the execution, on the process instances created
     */
    public void runVerifications() {
      RunScenarioVerification verifications = new RunScenarioVerification(scnExecution);
      for (String processInstanceId : scnRunResult.getProcessInstanceId()) {
        scnRunResult.add(verifications.runVerifications(scnRunExecution.runScenario, processInstanceId));
      }
    }

    public RunResult getScnRunResult() {
      return scnRunResult;
    }

  }
}
