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
import io.camunda.zeebe.client.api.worker.JobWorker;
import io.camunda.zeebe.spring.client.jobhandling.CommandWrapper;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.bpmnengine.camunda7.BpmnEngineCamunda7;
import org.camunda.automator.bpmnengine.camunda8.BenchmarkCompleteJobExceptionHandlingStrategy;
import org.camunda.automator.bpmnengine.camunda8.BpmnEngineCamunda8;
import org.camunda.automator.bpmnengine.camunda8.refactoring.RefactoredCommandWrapper;
import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenario;
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
  private final TaskScheduler scheduler;

  private static final TrackActiveWorker trackActiveWorkers = new TrackActiveWorker();
  private static final TrackActiveWorker trackAsynchronousWorkers = new TrackActiveWorker();

  Logger logger = LoggerFactory.getLogger(RunScenarioFlowServiceTask.class);
  private BpmnEngine.RegisteredTask registeredTask;
  private boolean stopping;
  @Autowired
  private BenchmarkCompleteJobExceptionHandlingStrategy exceptionHandlingStrategy;

  private Semaphore semaphore;

  public RunScenarioFlowServiceTask(TaskScheduler scheduler,
                                    ScenarioStep scenarioStep,
                                    int index,
                                    RunScenario runScenario,
                                    RunResult runResult) {
    super(scenarioStep, index, runScenario, runResult);
    this.scheduler = scheduler;
    this.semaphore = new Semaphore(runScenario.getBpmnEngine().getWorkerExecutionThreads());

  }

  @Override
  public void execute() {
    registerWorker();
  }

  @Override
  public void pleaseStop() {
    logger.info("Ask Stopping [" + getId() + "]");
    stopping = true;
    if (registeredTask==null || (registeredTask.isNull()) )
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
          durationSleep, // lock time
          new SimpleDelayHandler(this), new FixedBackoffSupplier(getScenarioStep().getFixedBackOffDelay()));
    /*
    // calculate the lock duration: this is <numberOfThreads> *
    ZeebeClient zeebeClient = ((BpmnEngineCamunda8) getRunScenario().getBpmnEngine()).getZeebeClient();

    JobWorkerBuilderStep1.JobWorkerBuilderStep3 step3 = zeebeClient.newWorker()
        .jobType(getScenarioStep().getTopic())
        .handler(new SimpleDelayC8Handler(this))
        .timeout(durationSleep)
        .name(getId());

    if (getScenarioStep().getFixedBackOffDelay() > 0) {
      step3.backoffSupplier(new FixedBackoffSupplier(getScenarioStep().getFixedBackOffDelay()));
    }
    jobWorker = step3.open();
    */

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
      case WAIT -> manageWaitExecution(externalTask, externalTaskService, null, null, durationSleep.toMillis());
      case ASYNCHRONOUS -> manageAsynchronousExecution(externalTask, externalTaskService, null, null);
      case ASYNCHRONOUSLIMITED -> manageAsynchronousLimitedExecution(externalTask, externalTaskService,null,null);
      }
    }

    /* C8 management */
    @Override
    public void handle(JobClient jobClient, ActivatedJob activatedJob) throws Exception {
      switch (getScenarioStep().getModeExecution()) {
      case WAIT -> manageWaitExecution(null, null, jobClient, activatedJob, durationSleep.toMillis());
      case ASYNCHRONOUS -> manageAsynchronousExecution(null, null, jobClient, activatedJob);
      case ASYNCHRONOUSLIMITED -> manageAsynchronousLimitedExecution(null, null,jobClient, activatedJob);
      }
    }

    private void manageWaitExecution(org.camunda.bpm.client.task.ExternalTask externalTask,
                                     ExternalTaskService externalTaskService,
                                     JobClient jobClient, ActivatedJob activatedJob,
                                     long waitTimeInMs) {
      long begin = System.currentTimeMillis();
      try {
        if (getRunScenario().getRunParameters().deepTracking)
          trackActiveWorkers.movement(1);

        if (waitTimeInMs > 0)
          Thread.sleep(waitTimeInMs);

        Map<String, Object> variables = new HashMap<>();

        /* C7 */
        if (externalTask!=null) {
          externalTaskService.complete(externalTask, variables);
        }
        /* C8 */
        if (jobClient!=null) {
          CompleteJobCommandStep1 completeCommand = jobClient.newCompleteCommand(activatedJob.getKey());
          CommandWrapper command = new RefactoredCommandWrapper((FinalCommandStep) completeCommand,
              activatedJob.getDeadline(), activatedJob.toString(), exceptionHandlingStrategy);

          command.executeAsync();
        }

        flowServiceTask.runResult.registerAddStepExecution();

      } catch (Exception e) {
        logger.error(
            "Error task[" + flowServiceTask.getId() + " " + externalTask.getBusinessKey() + " : " + e.getMessage());

        flowServiceTask.runResult.registerAddErrorStepExecution();

      }
      long end = System.currentTimeMillis();

      if (getRunScenario().getRunParameters().deepTracking)
        trackActiveWorkers.movement(-1);

      if (getRunScenario().getRunParameters().isLevelMonitoring()) {
        logger.info(
            "Executed task[" + getId() + "] in " + (end - begin) + " ms" + " Sleep [" + durationSleep.getSeconds()
                + " s]");
      }
    }

    private void manageAsynchronousExecution(org.camunda.bpm.client.task.ExternalTask externalTask,
                                             ExternalTaskService externalTaskService,
                                             JobClient jobClient, ActivatedJob activatedJob) {
      if (getRunScenario().getRunParameters().deepTracking)
        trackAsynchronousWorkers.movement(1);
      flowServiceTask.scheduler.schedule(new Runnable() {
        @Override
        public void run() {
          manageWaitExecution(externalTask, externalTaskService, jobClient, activatedJob,0);
          if (getRunScenario().getRunParameters().deepTracking)
            trackAsynchronousWorkers.movement(-1);
        }
      }, Instant.now().plusMillis(durationSleep.toMillis()));
    }

    private void manageAsynchronousLimitedExecution(org.camunda.bpm.client.task.ExternalTask externalTask,
                                                    ExternalTaskService externalTaskService,
                                                    JobClient jobClient, ActivatedJob activatedJob) {
      // we register
      try {
        flowServiceTask.semaphore.acquire();
        if (getRunScenario().getRunParameters().isLevelMonitoring()) {
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
          if (getRunScenario().getRunParameters().isLevelMonitoring()) {
            logger.info("task[{}] Semaphore release", getId());
          }
        }
      }, Instant.now().plusMillis(durationSleep.toMillis()));

    }

  }

}