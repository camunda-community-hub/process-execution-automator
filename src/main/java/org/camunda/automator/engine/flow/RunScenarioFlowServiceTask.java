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

import java.time.Duration;

public class RunScenarioFlowServiceTask extends RunScenarioFlowBasic {
  Logger logger = LoggerFactory.getLogger(RunScenarioFlowServiceTask.class);

  private JobWorker jobWorker;
  private boolean stopping;

  @Autowired
  private BenchmarkCompleteJobExceptionHandlingStrategy exceptionHandlingStrategy;

  public RunScenarioFlowServiceTask(ScenarioStep scenarioStep,
                                    int index,
                                    RunScenario runScenario,
                                    RunResult runResult) {
    super(scenarioStep, index, runScenario, runResult);
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

  /**
   * Register the worker
   */

  private void registerWorker() {
    if (!(getRunScenario().getBpmnEngine() instanceof BpmnEngineCamunda8)) {
      logger.error("This function required a Camunda 8 Engine");
      return;
    }

    ZeebeClient zeebeClient = ((BpmnEngineCamunda8) getRunScenario().getBpmnEngine()).getZeebeClient();

    JobWorkerBuilderStep1.JobWorkerBuilderStep3 step3 = zeebeClient.newWorker()
        .jobType(getScenarioStep().getTopic())
        .handler(new SimpleDelayCompletionHandler(this))
        .name(getId());

    if (getScenarioStep().getFixedBackOffDelay() > 0) {
      step3.backoffSupplier(new FixedBackoffSupplier(getScenarioStep().getFixedBackOffDelay()));
    }
    jobWorker = step3.open();
  }

  public class SimpleDelayCompletionHandler implements JobHandler {

    private final RunScenarioFlowServiceTask flowServiceTask;
    private final Duration durationSleep;

    public SimpleDelayCompletionHandler(RunScenarioFlowServiceTask flowServiceTask) {
      this.flowServiceTask = flowServiceTask;
      durationSleep = flowServiceTask.getScenarioStep().getWaitingTimeDuration(Duration.ZERO);
    }

    @Override
    public void handle(JobClient jobClient, ActivatedJob activatedJob) throws Exception {

      long begin = System.currentTimeMillis();
      Thread.sleep(durationSleep.toMillis());

      try {
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

      }
      long end = System.currentTimeMillis();

      if (getRunScenario().getRunParameters().isLevelMonitoring()) {
        logger.info(
            "Execute task[" + getId() + "] in " + (end - begin) + " ms" + " Sleep [" + durationSleep.getSeconds()
                + " s]");
      }

    }
  }
}