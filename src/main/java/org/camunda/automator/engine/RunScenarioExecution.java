package org.camunda.automator.engine;

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.definition.ScenarioExecution;
import org.camunda.automator.definition.ScenarioStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * one ExecutionStep in a runScenario
 */
public class RunScenarioExecution {
  private final Logger logger = LoggerFactory.getLogger(RunScenarioExecution.class);
  private final RunScenario runScenario;
  private final ScenarioExecution scnExecution;
  private String agentName = "";

  public RunScenarioExecution(RunScenario runScenario, ScenarioExecution scnExecution) {
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

    if (runScenario.getRunParameters().isLevelMonitoring()) {
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

    if (runScenario.getRunParameters().isLevelMonitoring()) {
      logger.info("ScnRunExecution." + agentName + ": End Execution [" + scnExecution.getName() + "] success? "
          + resultExecution.isSuccess());
    }
    return resultExecution;
  }

  /**
   * Start Event
   *
   * @param result result to complete and return
   * @param step   step to execute
   * @return result completed
   */
  public RunResult startEvent(RunResult result, ScenarioStep step) {
    try {
      result.addProcessInstanceId(step.getScnExecution().getScnHead().getProcessId(),
          runScenario.getBpmnEngine()
          .createProcessInstance(step.getScnExecution().getScnHead().getProcessId(), step.getTaskId(), // activityId
              RunZeebeOperation.getVariablesStep(runScenario, step))); // resolve variables
    } catch (AutomatorException e) {
      result.addError(step, "Error at creation " + e.getMessage());
    }
    return result;
  }

  /**
   * Execute User task
   *
   * @param result result to complete and return
   * @param step   step to execute
   * @return result completed
   */
  public RunResult executeUserTask(RunResult result, ScenarioStep step) {

    if (step.getDelay() != null) {
      Duration duration = Duration.parse(step.getDelay());
      try {
        Thread.sleep(duration.toMillis());
      } catch (InterruptedException e) {
        // don't need to do anything here
      }
    }
    Long waitingTimeInMs = null;
    if (step.getWaitingTime() != null) {
      Duration duration = Duration.parse(step.getWaitingTime());
      waitingTimeInMs = duration.toMillis();
    }
    if (waitingTimeInMs == null)
      waitingTimeInMs = 5L * 60 * 1000;

    for (int index = 0; index < step.getNumberOfExecutions(); index++) {
      long beginTimeWait = System.currentTimeMillis();
      try {
        List<String> listActivities;
        do {

          listActivities = runScenario.getBpmnEngine()
              .searchUserTasks(result.getFirstProcessInstanceId(), step.getTaskId(), 1);

          if (listActivities.isEmpty()) {
            try {
              Thread.sleep(500);
            } catch (InterruptedException e) {
              // nothing to do here
            }
          }
        } while (listActivities.isEmpty() && System.currentTimeMillis() - beginTimeWait < waitingTimeInMs);

        if (listActivities.isEmpty()) {
          result.addError(step, "No user task show up task[" + step.getTaskId() + "] processInstance["
              + result.getFirstProcessInstanceId() + "]");
          return result;
        }
        runScenario.getBpmnEngine()
            .executeUserTask(listActivities.get(0), step.getUserId(),
                RunZeebeOperation.getVariablesStep(runScenario, step));
      } catch (AutomatorException e) {
        result.addError(step, e.getMessage());
        return result;
      }
    }

    return result;

  }

  /**
   * Execute User task
   *
   * @param result result to complete and return
   * @param step   step to execute
   * @return result completed
   */
  public RunResult executeServiceTask(RunResult result, ScenarioStep step) {
    if (step.getDelay() != null) {
      Duration duration = Duration.parse(step.getDelay());
      try {
        Thread.sleep(duration.toMillis());
      } catch (InterruptedException e) {
        // nothing to do
      }
    }
    Long waitingTimeInMs = null;
    if (step.getWaitingTime() != null) {
      Duration duration = Duration.parse(step.getWaitingTime());
      waitingTimeInMs = duration.toMillis();
    }
    if (waitingTimeInMs == null)
      waitingTimeInMs = Long.valueOf(5 * 60 * 1000);

    for (int index = 0; index < step.getNumberOfExecutions(); index++) {
      long beginTimeWait = System.currentTimeMillis();
      try {
        List<String> listActivities;
        do {

          listActivities = runScenario.getBpmnEngine()
              .searchServiceTasks(result.getFirstProcessInstanceId(), step.getTaskId(), step.getTopic(), 1);

          if (listActivities.isEmpty()) {
            try {
              Thread.sleep(500);
            } catch (InterruptedException e) {
            }
          }
        } while (listActivities.isEmpty() && System.currentTimeMillis() - beginTimeWait < waitingTimeInMs);

        if (listActivities.isEmpty()) {
          result.addError(step, "No service task show up task[" + step.getTaskId() + "] processInstance["
              + result.getFirstProcessInstanceId() + "]");
          return result;
        }
        runScenario.getBpmnEngine()
            .executeServiceTask(listActivities.get(0), step.getUserId(),
                RunZeebeOperation.getVariablesStep(runScenario, step));
      } catch (AutomatorException e) {
        result.addError(step, e.getMessage());
        return result;
      }
    }

    return result;

  }



  /* ******************************************************************** */
  /*                                                                      */
  /*  ScnThreadCallable : execute one Execution per thread                                                   */
  /*                                                                      */
  /* ******************************************************************** */

  private class ScnThreadExecutionCallable implements Callable {
    private final String agentName;
    private final RunScenarioExecution scnRunExecution;
    private final RunParameters runParameters;

    private RunResult scnRunResult;

    ScnThreadExecutionCallable(String agentName, RunScenarioExecution scnRunExecution, RunParameters runParameters) {
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
      if (runParameters.execution)
        runExecution();

      // two uses case here:
      // Execution AND verifications: for each process Instance created, a verification is running
      // Only VERIFICATION: the verification ojbect define a filter to search existing process instance. Verification is perform againts this list
      if (runParameters.verification && (scnExecution.getVerifications() != null)) {
        if (runParameters.execution) {
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
              scnRunResult.addProcessInstanceId(scnExecution.getScnHead().getProcessId(), processInstance.processInstanceId);
            }
            runVerifications();
          }
        }

      }
      // we finish with this process instance
      if (scnRunResult.getFirstProcessInstanceId() != null && runParameters.clearAllAfter)
        runScenario.getBpmnEngine()
            .endProcessInstance(scnRunResult.getFirstProcessInstanceId(), runParameters.clearAllAfter);
      return scnRunResult;
    }

    public void runExecution() {

      if (scnRunExecution.runScenario.getRunParameters().isLevelMonitoring())
        logger.info(
            "ScnRunExecution.StartExecution [" + scnRunExecution.runScenario.getScenario().getName() + "] agent["
                + agentName + "]");

      for (ScenarioStep step : scnExecution.getSteps()) {

        if (scnRunExecution.runScenario.getRunParameters().isLevelDebug())
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
          if (scnRunExecution.runScenario.getRunParameters().creation)
            scnRunResult = scnRunExecution.startEvent(scnRunResult, step);
        }
        case USERTASK -> {
          // wait for the user Task
          if (scnRunExecution.runScenario.getRunParameters().usertask)
            scnRunResult = scnRunExecution.executeUserTask(scnRunResult, step);
        }
        case SERVICETASK -> {
          // wait for the user Task
          if (scnRunExecution.runScenario.getRunParameters().servicetask)
            scnRunResult = scnRunExecution.executeServiceTask(scnRunResult, step);
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
      if (scnRunExecution.runScenario.getRunParameters().isLevelMonitoring())
        logger.info("ScnRunExecution.EndExecution [" + scnExecution.getName() + "] agent[" + agentName + "]");
      return;
    }

    /**
     * Run the verification just after the execution, on the process isntances created
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
