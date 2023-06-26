/* ******************************************************************** */
/*                                                                      */
/*  RunScenarioFlowServiceTask                                          */
/*                                                                      */
/*  Execute a service task                                              */
/* ******************************************************************** */
package org.camunda.automator.engine.flow;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.CompleteJobCommandStep1;
import io.camunda.zeebe.client.api.command.FinalCommandStep;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.api.worker.JobWorker;
import io.camunda.zeebe.client.api.worker.JobWorkerBuilderStep1;
import io.camunda.zeebe.spring.client.jobhandling.CommandWrapper;
import org.camunda.automator.bpmnengine.camunda8.BenchmarkCompleteJobExceptionHandlingStrategy;
import org.camunda.automator.bpmnengine.camunda8.BpmnEngineCamunda8;
import org.camunda.automator.bpmnengine.camunda8.refactoring.RefactoredCommandWrapper;
import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Semaphore;

public class RunScenarioFlowServiceTask extends RunScenarioFlowBasic {
  private final TaskScheduler scheduler;

  private static final TrackActiveWorker trackActiveWorkers = new TrackActiveWorker();
  private static final TrackActiveWorker trackAsynchronousWorkers = new TrackActiveWorker();

  Logger logger = LoggerFactory.getLogger(RunScenarioFlowServiceTask.class);
  private JobWorker jobWorker;
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
    if (jobWorker == null)
      return;
    if (jobWorker.isClosed())
      return;
    jobWorker.close();

    Duration durationSleep = getScenarioStep().getWaitingTimeDuration(Duration.ZERO);
    long expectedEndTime = System.currentTimeMillis() + durationSleep.toMillis();
    while (!jobWorker.isClosed() && System.currentTimeMillis() < expectedEndTime) {
      jobWorker.close();
      try {
        Thread.sleep(500);
      } catch (Exception e) {
        // do nothing
      }
    }
    logger.info("[" + getId() + "] " + (jobWorker.isClosed() ? "stopped" : "Fail to stop"));

    jobWorker = null;
  }

  @Override
  public STATUS getStatus() {
    if (jobWorker == null)
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
    if (!(getRunScenario().getBpmnEngine() instanceof BpmnEngineCamunda8)) {
      logger.error("This function required a Camunda 8 Engine");
      return;
    }

    // calculate the lock duration: this is <numberOfThreads> *
    Duration durationSleep = getScenarioStep().getWaitingTimeDuration(Duration.ZERO);
    durationSleep = durationSleep.plusSeconds(10);
    ZeebeClient zeebeClient = ((BpmnEngineCamunda8) getRunScenario().getBpmnEngine()).getZeebeClient();

    JobWorkerBuilderStep1.JobWorkerBuilderStep3 step3 = zeebeClient.newWorker()
        .jobType(getScenarioStep().getTopic())
        .handler(new SimpleDelayCompletionHandler(this))
        .timeout(durationSleep)
        .name(getId());

    if (getScenarioStep().getFixedBackOffDelay() > 0) {
      step3.backoffSupplier(new FixedBackoffSupplier(getScenarioStep().getFixedBackOffDelay()));
    }
    jobWorker = step3.open();
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
   *
   */
  public class SimpleDelayCompletionHandler implements JobHandler {

    private final RunScenarioFlowServiceTask flowServiceTask;
    private final Duration durationSleep;

    public SimpleDelayCompletionHandler(RunScenarioFlowServiceTask flowServiceTask) {
      this.flowServiceTask = flowServiceTask;
      durationSleep = flowServiceTask.getScenarioStep().getWaitingTimeDuration(Duration.ZERO);
    }

    @Override
    public void handle(JobClient jobClient, ActivatedJob activatedJob) throws Exception {
      switch (getScenarioStep().getModeExecution()) {
      case WAIT -> manageWaitExecution(jobClient, activatedJob, durationSleep.toMillis());
      case ASYNCHRONOUS -> manageAsynchronousExecution(jobClient, activatedJob);
      case ASYNCHRONOUSLIMITED -> manageAsynchronousLimitedExecution(jobClient, activatedJob);
      }
    }

    /**
     * Manage execution. Sleep the time, then execute.
     * @param jobClient job client
     * @param activatedJob activated job
     * @param  waitTimeInMs time to wait
     */
    private void manageWaitExecution(JobClient jobClient, ActivatedJob activatedJob, long waitTimeInMs) {
      long begin = System.currentTimeMillis();
      try {
        if (getRunScenario().getRunParameters().deepTracking)
          trackActiveWorkers.movement(1);

        if (waitTimeInMs > 0)
          Thread.sleep(waitTimeInMs);

        /*
        jobClient.newCompleteCommand(activatedJob.getKey())
            .variables(RunZeebeOperation.getVariablesStep(getRunScenario(), flowServiceTask.getScenarioStep()))
            .send()
            .join();
        */

        CompleteJobCommandStep1 completeCommand = jobClient.newCompleteCommand(activatedJob.getKey());
        CommandWrapper command = new RefactoredCommandWrapper((FinalCommandStep) completeCommand,
            activatedJob.getDeadline(), activatedJob.toString(), exceptionHandlingStrategy);

        command.executeAsync();

        flowServiceTask.runResult.registerAddStepExecution();

      } catch (Exception e) {
        logger.error("Error task[" + flowServiceTask.getId() + " " + activatedJob.getKey() + " : " + e.getMessage());

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

    /**
     * Asynchronous: schedule the execution, after the sleep time, and release immediately the method
     * @param jobClient job client
     * @param activatedJob activated job
     */
    private void manageAsynchronousExecution(JobClient jobClient, ActivatedJob activatedJob) {
      if (getRunScenario().getRunParameters().deepTracking)
        trackAsynchronousWorkers.movement(1);
      flowServiceTask.scheduler.schedule(new Runnable() {
        @Override
        public void run() {
          manageWaitExecution(jobClient, activatedJob, 0);
          if (getRunScenario().getRunParameters().deepTracking)
            trackAsynchronousWorkers.movement(-1);
        }
      }, Instant.now().plusMillis(durationSleep.toMillis()));
    }

    /**
     * Manage asynchrously, but be sure that we have maximum a number of threads. If we reach this max number of threads, then we sleep
     * The number of thread = client.numberOfThread
     * @param jobClient job client
     * @param activatedJob activated job
     */
    private void manageAsynchronousLimitedExecution(JobClient jobClient, ActivatedJob activatedJob) {
      // we register
      try {
        flowServiceTask.semaphore.acquire();
        if (getRunScenario().getRunParameters().isLevelMonitoring()) {
          logger.info("task[{}] Semaphore acquire", getId());
        }
      } catch (Exception e) {
        jobClient.newThrowErrorCommand(activatedJob.getKey());
        return;
      }
      // Ok, now we can run that asynchronous
      flowServiceTask.scheduler.schedule(new Runnable() {
        @Override
        public void run() {
          manageWaitExecution(jobClient, activatedJob, 0);
          flowServiceTask.semaphore.release();
          if (getRunScenario().getRunParameters().isLevelMonitoring()) {
            logger.info("task[{}] Semaphore release", getId());
          }
        }
      }, Instant.now().plusMillis(durationSleep.toMillis()));

    }

  }
}