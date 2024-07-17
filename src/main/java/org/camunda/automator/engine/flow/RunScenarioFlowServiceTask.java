/* ******************************************************************** */
/*                                                                      */
/*  RunScenarioFlowServiceTask                                          */
/*                                                                      */
/*  Execute a service task                                              */
/* ******************************************************************** */
package org.camunda.automator.engine.flow;

import io.camunda.zeebe.client.api.command.CompleteJobCommandStep1;
import io.camunda.zeebe.client.api.command.FinalCommandStep;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.spring.client.jobhandling.CommandWrapper;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.bpmnengine.camunda8.BenchmarkCompleteJobExceptionHandlingStrategy;
import org.camunda.automator.bpmnengine.camunda8.refactoring.RefactoredCommandWrapper;
import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenario;
import org.camunda.automator.engine.RunZeebeOperation;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class RunScenarioFlowServiceTask extends RunScenarioFlowBasic {
  private static final TrackActiveWorker trackActiveWorkers = new TrackActiveWorker();
  private static final TrackActiveWorker trackAsynchronousWorkers = new TrackActiveWorker();
  private final TaskScheduler scheduler;
  private final Semaphore semaphore;
  Logger logger = LoggerFactory.getLogger(RunScenarioFlowServiceTask.class);
  private BpmnEngine.RegisteredTask registeredTask;
  private boolean stopping;
  @Autowired
  private BenchmarkCompleteJobExceptionHandlingStrategy exceptionHandlingStrategy;

  public RunScenarioFlowServiceTask(TaskScheduler scheduler,
                                    ScenarioStep scenarioStep,
                                    RunScenario runScenario,
                                    RunResult runResult) {
    super(scenarioStep, runScenario, runResult);
    this.scheduler = scheduler;
    this.semaphore = new Semaphore(runScenario.getBpmnEngine().getWorkerExecutionThreads());

  }

  @Override
  public String getTopic() {
    return getScenarioStep().getTopic();
  }
  @Override
  public void execute() {
    registerWorker();
  }

  @Override
  public void pleaseStop() {
    logger.info("Ask Stopping [" + getId() + "]");
    stopping = true;
    if (registeredTask == null || (registeredTask.isNull()))
      return;
    if (registeredTask.isClosed()) {
      return;
    }
    registeredTask.close();

    Duration durationSleep = getScenarioStep().getWaitingTimeDuration(Duration.ZERO);
    long expectedEndTime = System.currentTimeMillis() + durationSleep.toMillis();
    while (!registeredTask.isClosed() && System.currentTimeMillis() < expectedEndTime) {
      registeredTask.close();
      try {
        Thread.sleep(500);
      } catch (Exception e) {
        // do nothing
      }
    }
    logger.info("[" + getId() + "] " + (registeredTask.isClosed() ? "stopped" : "Fail to stop"));

    registeredTask = null;
  }

  @Override
  public STATUS getStatus() {
    if (registeredTask == null)
      return STATUS.STOPPED;
    if (stopping)
      return STATUS.STOPPING;
    return STATUS.RUNNING;
  }

  @Override
  public int getCurrentNumberOfThreads() {
    return trackActiveWorkers.getCounter() + trackAsynchronousWorkers.getCounter();
  }

  /**
   * Register the worker
   */

  private void registerWorker() {
    BpmnEngine bpmnEngine = getRunScenario().getBpmnEngine();

    Duration durationSleep = getScenarioStep().getWaitingTimeDuration(Duration.ZERO);
    durationSleep = durationSleep.plusSeconds(10);

    registeredTask = bpmnEngine.registerServiceTask(getId(), // workerId
        getScenarioStep().getTopic(), // topic
        getScenarioStep().isStreamEnable(), // stream
        durationSleep, // lock time
        new SimpleDelayHandler(this), new FixedBackoffSupplier(getScenarioStep().getFixedBackOffDelay()));
  }

  private static class TrackActiveWorker {
    public int counter = 0;

    public synchronized void movement(int movement) {
      counter += movement;
    }

    public int getCounter() {
      return counter;
    }
  }

  /**
   * C7, C8 Handler
   */
  public class SimpleDelayHandler implements ExternalTaskHandler, JobHandler {
    private final RunScenarioFlowServiceTask flowServiceTask;
    private final Duration durationSleep;

    public SimpleDelayHandler(RunScenarioFlowServiceTask flowServiceTask) {
      this.flowServiceTask = flowServiceTask;
      durationSleep = flowServiceTask.getScenarioStep().getWaitingTimeDuration(Duration.ZERO);
    }

    /* C7 Management */
    @Override
    public void execute(org.camunda.bpm.client.task.ExternalTask externalTask,
                        ExternalTaskService externalTaskService) {
      switch (getScenarioStep().getModeExecution()) {
      case CLASSICAL, WAIT -> manageWaitExecution(externalTask, externalTaskService, null, null,
          durationSleep.toMillis());
      case THREAD, ASYNCHRONOUS -> manageAsynchronousExecution(externalTask, externalTaskService, null, null);
      case THREADTOKEN, ASYNCHRONOUSLIMITED -> manageAsynchronousLimitedExecution(externalTask, externalTaskService,
          null, null);
      }
    }

    /* C8 management */
    @Override
    public void handle(JobClient jobClient, ActivatedJob activatedJob) throws Exception {
      switch (getScenarioStep().getModeExecution()) {
      case CLASSICAL, WAIT -> manageWaitExecution(null, null, jobClient, activatedJob, durationSleep.toMillis());
      case THREAD, ASYNCHRONOUS -> manageAsynchronousExecution(null, null, jobClient, activatedJob);
      case THREADTOKEN, ASYNCHRONOUSLIMITED -> manageAsynchronousLimitedExecution(null, null, jobClient, activatedJob);
      }
    }

    private void manageWaitExecution(org.camunda.bpm.client.task.ExternalTask externalTask,
                                     ExternalTaskService externalTaskService,
                                     JobClient jobClient,
                                     ActivatedJob activatedJob,
                                     long waitTimeInMs) {
      long begin = System.currentTimeMillis();
      Map<String, Object> variables = new HashMap<>();
      Map<String, Object> currentVariables = new HashMap<>();
      try {
        if (getRunScenario().getRunParameters().isDeepTracking())
          trackActiveWorkers.movement(1);

        if (waitTimeInMs > 0)
          Thread.sleep(waitTimeInMs);

        variables = RunZeebeOperation.getVariablesStep(flowServiceTask.getRunScenario(),
            flowServiceTask.getScenarioStep(), 0);


        /* C7 */
        if (externalTask != null) {
          currentVariables = externalTask.getAllVariables();
          externalTaskService.complete(externalTask, variables);
        }
        /* C8 */
        if (jobClient != null) {
          currentVariables = activatedJob.getVariablesAsMap();
          CompleteJobCommandStep1 completeCommand = jobClient.newCompleteCommand(activatedJob.getKey());
          CommandWrapper command = new RefactoredCommandWrapper((FinalCommandStep) completeCommand,
              activatedJob.getDeadline(), activatedJob.toString(), exceptionHandlingStrategy);

          command.executeAsync();
        }

        flowServiceTask.runResult.registerAddStepExecution();

      } catch (Exception e) {
        logger.error(
            "Error task[{}] PI[{}] : {}",
            flowServiceTask.getId(),
            (externalTask != null ? externalTask.getProcessDefinitionKey() : activatedJob.getProcessInstanceKey()),
            e.getMessage()
            );

        flowServiceTask.runResult.registerAddErrorStepExecution();

      }
      long end = System.currentTimeMillis();

      if (getRunScenario().getRunParameters().isDeepTracking())
        trackActiveWorkers.movement(-1);

      if (getRunScenario().getRunParameters().showLevelInfo()) {
        logger.info("Executed task[{}] in {} ms PI[{}] CurrentVariable {} Variable {} Sleep [{} s]", getId(),
            end - begin,
            (externalTask != null ? externalTask.getProcessDefinitionKey() : activatedJob.getProcessInstanceKey()),
            currentVariables, variables, durationSleep.getSeconds());

      }

    }

    private void manageAsynchronousExecution(org.camunda.bpm.client.task.ExternalTask externalTask,
                                             ExternalTaskService externalTaskService,
                                             JobClient jobClient,
                                             ActivatedJob activatedJob) {
      if (getRunScenario().getRunParameters().isDeepTracking())
        trackAsynchronousWorkers.movement(1);
      flowServiceTask.scheduler.schedule(new Runnable() {
        @Override
        public void run() {
          manageWaitExecution(externalTask, externalTaskService, jobClient, activatedJob, 0);
          if (getRunScenario().getRunParameters().isDeepTracking())
            trackAsynchronousWorkers.movement(-1);
        }
      }, Instant.now().plusMillis(durationSleep.toMillis()));
    }

    private void manageAsynchronousLimitedExecution(org.camunda.bpm.client.task.ExternalTask externalTask,
                                                    ExternalTaskService externalTaskService,
                                                    JobClient jobClient,
                                                    ActivatedJob activatedJob) {
      // we register
      try {
        flowServiceTask.semaphore.acquire();
        if (getRunScenario().getRunParameters().showLevelMonitoring()) {
          logger.info("task[{}] Semaphore acquire", getId());
        }
      } catch (Exception e) {
        return;
      }
      // Ok, now we can run that asynchronous
      flowServiceTask.scheduler.schedule(new Runnable() {
        @Override
        public void run() {
          manageWaitExecution(externalTask, externalTaskService, jobClient, activatedJob, 0);
          flowServiceTask.semaphore.release();
          if (getRunScenario().getRunParameters().showLevelMonitoring()) {
            logger.info("task[{}] Semaphore release", getId());
          }
        }
      }, Instant.now().plusMillis(durationSleep.toMillis()));

    }

  }

}