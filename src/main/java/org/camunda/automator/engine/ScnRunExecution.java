package org.camunda.automator.engine;

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.definition.ScnExecution;
import org.camunda.automator.definition.ScnStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ScnRunExecution {
  private final Logger logger = LoggerFactory.getLogger(ScnRunExecution.class);

  private String agentName = "";

  public void setAgentName(String name) {
    this.agentName = name;
  }

  /**
   * Execute the scenario.
   * Note: this method is multi thread safe.
   *
   * @param bpmnEngine engine to connect
   * @return the execution
   */
  public ScnRunResult runExecution(ScnExecution scnExecution, BpmnEngine bpmnEngine, RunParameters runParameters) {
    ScnRunResult resultExecution = new ScnRunResult(scnExecution.getScnHead(), runParameters);

    if (runParameters.isLevelMonitoring()) {
      logger.info("ScnRunExecution." + agentName + ": Start Execution [" + scnExecution.getName() + "] ");
    }
    ExecutorService executor = Executors.newFixedThreadPool(scnExecution.getNumberOfThreads());

    List<Future<?>> listFutures = new ArrayList<>();

    for (int i = 0; i < scnExecution.getNumberProcessInstances(); i++) {
      ScnRunExecution.ScnThreadCallable scnExecutionCallable = new ScnRunExecution.ScnThreadCallable(
          "AutomatorThread-" + i, scnExecution, this, bpmnEngine, runParameters);

      listFutures.add(executor.submit(scnExecutionCallable));
    }

    // wait the end of all executions
    try {
      for (Future<?> f : listFutures) {
        Object scnRunResult = f.get();
        resultExecution.add((ScnRunResult) scnRunResult);

      }

    } catch (Exception e) {
      resultExecution.addError(null, "Error during executing in parallel " + e.getMessage());
    }

    if (runParameters.isLevelMonitoring()) {
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
   * @param bpmnEngine engine to contact
   * @return result completed
   */
  public ScnRunResult startEvent(ScnRunResult result, ScnStep step, BpmnEngine bpmnEngine) {
    try {
      result.addProcessInstanceId(
          bpmnEngine.createProcessInstance(step.getScnExecution().getScnHead().getProcessId(), step.getActivityId(),
              step.getVariables()));
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
   * @param bpmnEngine engine to contact
   * @return result completed
   */
  public ScnRunResult executeUserTask(ScnRunResult result, ScnStep step, BpmnEngine bpmnEngine) {
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


    for (int index=0;index<step.getNumberOfExecutions();index++) {
      long beginTimeWait = System.currentTimeMillis();
      try {
        List<String> listActivities;
        do {

          listActivities = bpmnEngine.searchForActivity(result.getFirstProcessInstanceId(), step.getActivityId(), 1);

          if (listActivities.isEmpty()) {
            try {
              Thread.sleep(500);
            } catch (InterruptedException e) {
            }
          }
        } while (listActivities.isEmpty() && System.currentTimeMillis() - beginTimeWait < waitingTimeInMs);

        if (listActivities.isEmpty()) {
          result.addError(step, "No user task show up task[" + step.getActivityId() + "] processInstance[" + result.getFirstProcessInstanceId() + "]");
          return result;
        }
        bpmnEngine.executeUserTask(listActivities.get(0), step.getUserId(), step.getVariables());
      } catch (AutomatorException e) {
        result.addError(step, e.getMessage());
        return result;
      }
    }

    return result;

  }
  /* ******************************************************************** */
  /*                                                                      */
  /*  ScnThreadCallable                                                   */
  /*                                                                      */
  /* ******************************************************************** */

  private class ScnThreadCallable implements Callable {
    private final String agentName;
    private final ScnExecution scnExecution;
    private final ScnRunExecution scnRunExecution;

    private final BpmnEngine bpmnEngine;
    private final RunParameters runParameters;

    private ScnRunResult scnRunResult;

    ScnThreadCallable(String agentName,
                      ScnExecution scnExecution,
                      ScnRunExecution scnRunExecution,
                      BpmnEngine bpmnEngine,
                      RunParameters runParameters) {
      this.agentName = agentName;
      this.scnExecution = scnExecution;
      this.scnRunExecution = scnRunExecution;
      this.bpmnEngine = bpmnEngine;
      this.runParameters = runParameters;
    }

    public Object call() throws Exception {
      scnRunResult = new ScnRunResult(scnExecution.getScnHead(), runParameters);

      if (runParameters.isLevelMonitoring())
        logger.info("ScnRunExecution.StartExecution [" + scnExecution.getName() + "] agent[" + agentName + "]");

      for (ScnStep step : scnExecution.getSteps()) {
        long timeBegin = System.currentTimeMillis();
        if (step.getType() == null) {
          scnRunResult.addError(step, "Unknown type");
          continue;
        }
        switch (step.getType()) {
        case STARTEVENT -> {
          scnRunResult = scnRunExecution.startEvent(scnRunResult, step, bpmnEngine);
        }
        case USERTASK -> {
          // wait for the user Task
          scnRunResult = scnRunExecution.executeUserTask(scnRunResult, step, bpmnEngine);
        }
        case ENDEVENT -> {
        }

        case MESSAGE -> {
        }
        }
        long timeEnd = System.currentTimeMillis();
        scnRunResult.addStepExecution(step, timeEnd - timeBegin);

        if (! scnRunResult.isSuccess() && ScnExecution.Policy.STOPATFIRSTERROR.equals(scnExecution.getPolicy()))
          return scnRunResult;
      }
      if (runParameters.isLevelMonitoring())
        logger.info("ScnRunExecution.EndExecution [" + scnExecution.getName() + "] agent[" + agentName + "]");
      return scnRunResult;
    }

    public ScnRunResult getScnRunResult() {
      return scnRunResult;
    }

  }
}
