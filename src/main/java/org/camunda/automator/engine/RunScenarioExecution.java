package org.camunda.automator.engine;

import org.camunda.automator.definition.ScenarioExecution;
import org.camunda.automator.definition.ScenarioStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Execute an exection in a runscenario
 */
public class RunScenarioExecution {
  private final Logger logger = LoggerFactory.getLogger(RunScenarioExecution.class);

  private String agentName = "";

  private RunScenario runScenario;
  private ScenarioExecution scnExecution;

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
      RunScenarioExecution.ScnThreadCallable scnExecutionCallable = new RunScenarioExecution.ScnThreadCallable(
          "AutomatorThread-" + i, this);

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
   * @param result     result to complete and return
   * @param step       step to execute
   * @return result completed
   */
  public RunResult startEvent(RunResult result, ScenarioStep step) {
    try {
      result.addProcessInstanceId(
          runScenario.getBpmnEngine().createProcessInstance(step.getScnExecution().getScnHead().getProcessId(),
              step.getActivityId(), // activityId
              getVariablesStep(step))); // resolve variables
    } catch (AutomatorException e) {
      result.addError(step, "Error at creation " + e.getMessage());
    }
    return result;
  }

  /**
   * Execute User task
   *
   * @param result     result to complete and return
   * @param step       step to execute
   * @return result completed
   */
  public RunResult executeUserTask(RunResult result, ScenarioStep step) {
    if (step.getDelay() != null) {
      Duration duration = Duration.parse(step.getDelay());
      try {
        Thread.sleep(duration.toMillis());
      } catch (InterruptedException e) {
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

          listActivities = runScenario.getBpmnEngine().searchUserTasks(result.getFirstProcessInstanceId(), step.getActivityId(), 1);

          if (listActivities.isEmpty()) {
            try {
              Thread.sleep(500);
            } catch (InterruptedException e) {
            }
          }
        } while (listActivities.isEmpty() && System.currentTimeMillis() - beginTimeWait < waitingTimeInMs);

        if (listActivities.isEmpty()) {
          result.addError(step, "No user task show up task[" + step.getActivityId() + "] processInstance["
              + result.getFirstProcessInstanceId() + "]");
          return result;
        }
        runScenario.getBpmnEngine().executeUserTask(listActivities.get(0), step.getUserId(), getVariablesStep(step));
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
   * @param result     result to complete and return
   * @param step       step to execute
   * @return result completed
   */
  public RunResult executeServiceTask(RunResult result, ScenarioStep step) {
    if (step.getDelay() != null) {
      Duration duration = Duration.parse(step.getDelay());
      try {
        Thread.sleep(duration.toMillis());
      } catch (InterruptedException e) {
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

          listActivities = runScenario.getBpmnEngine().searchServiceTasks(result.getFirstProcessInstanceId(), step.getActivityId(), 1);

          if (listActivities.isEmpty()) {
            try {
              Thread.sleep(500);
            } catch (InterruptedException e) {
            }
          }
        } while (listActivities.isEmpty() && System.currentTimeMillis() - beginTimeWait < waitingTimeInMs);

        if (listActivities.isEmpty()) {
          result.addError(step, "No service task show up task[" + step.getActivityId() + "] processInstance["
              + result.getFirstProcessInstanceId() + "]");
          return result;
        }
        runScenario.getBpmnEngine().executeServiceTask(listActivities.get(0), step.getUserId(), getVariablesStep(step));
      } catch (AutomatorException e) {
        result.addError(step, e.getMessage());
        return result;
      }
    }

    return result;

  }
  /**
   * Resolve variables
   */
  private Map<String, Object> getVariablesStep(ScenarioStep step) throws AutomatorException {
    Map<String, Object> variablesCompleted = new HashMap<>();
    variablesCompleted.putAll(step.getVariables());

    // execute all operations now

    for (Map.Entry<String, String> entryOperation : step.getVariablesOperations().entrySet()) {
      variablesCompleted.put(entryOperation.getKey(),
          runScenario.getServiceAccess().serviceDataOperation.execute(entryOperation.getValue(), runScenario));
    }

    return variablesCompleted;
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  ScnThreadCallable                                                   */
  /*                                                                      */
  /* ******************************************************************** */

  private class ScnThreadCallable implements Callable {
    private final String agentName;
    private final RunScenarioExecution scnRunExecution;


    private RunResult scnRunResult;

    ScnThreadCallable(String agentName,
                      RunScenarioExecution scnRunExecution) {
      this.agentName = agentName;
      this.scnRunExecution = scnRunExecution;
    }

    public Object call() throws Exception {
      scnRunResult = new RunResult(scnRunExecution.runScenario);

      if (scnRunExecution.runScenario.getRunParameters().isLevelMonitoring())
        logger.info("ScnRunExecution.StartExecution [" + scnRunExecution.runScenario.getScenario().getName() + "] agent[" + agentName + "]");

      for (ScenarioStep step : scnExecution.getSteps()) {
        long timeBegin = System.currentTimeMillis();
        if (step.getType() == null) {
          scnRunResult.addError(step, "Unknown type");
          continue;
        }
        switch (step.getType()) {
        case STARTEVENT -> {
          scnRunResult = scnRunExecution.startEvent(scnRunResult, step);
        }
        case USERTASK -> {
          // wait for the user Task
          scnRunResult = scnRunExecution.executeUserTask(scnRunResult, step);
        }
        case SERVICETASK -> {
          // wait for the user Task
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
          return scnRunResult;
      }
      if (scnRunExecution.runScenario.getRunParameters().isLevelMonitoring())
        logger.info("ScnRunExecution.EndExecution [" + scnExecution.getName() + "] agent[" + agentName + "]");
      return scnRunResult;
    }

    public RunResult getScnRunResult() {
      return scnRunResult;
    }

  }
}
