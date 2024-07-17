/* ******************************************************************** */
/*                                                                      */
/*  RunScenarioFlows                                                    */
/*                                                                      */
/*  Execute all flows in a scenario                                     */
/* ******************************************************************** */
package org.camunda.automator.engine.flow;

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.definition.ScenarioFlowControl;
import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenario;
import org.camunda.automator.services.ServiceAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RunScenarioFlows {
  private final ServiceAccess serviceAccess;
  private final RunScenario runScenario;
  private final Map<String, Long> previousValueMap = new HashMap<>();
  Logger logger = LoggerFactory.getLogger(RunScenarioFlows.class);

  public RunScenarioFlows(ServiceAccess serviceAccess, RunScenario runScenario) {
    this.serviceAccess = serviceAccess;
    this.runScenario = runScenario;
  }

  /**
   * Execute the scenario flow
   *
   * @param runResult result to populate
   */
  public void execute(RunResult runResult) {
    // Create one executor per flow
    RunScenarioWarmingUp runScenarioWarmingUp = new RunScenarioWarmingUp(serviceAccess, runScenario);
    Map<String, RunResult.RecordCreationPI> recordCreationPIMap = new HashMap<>();
    if (runScenario.getScenario().getFlowControl() == null) {
      runResult.addError(null,
          "Scenario does not declare a [FlowControl] section. This section is mandatory for a Flow Scenario");
      return;
    }

    List<ScenarioFlowControl.Objective> listObjectives = runScenario.getScenario().getFlowControl().getObjectives();
    if (listObjectives == null)
      listObjectives = Collections.emptyList();

    RunObjectives runObjectives = new RunObjectives(listObjectives, runScenario.getBpmnEngine(), recordCreationPIMap);

    logger.info("ScenarioFlow: ------ WarmingUp");
    runScenarioWarmingUp.warmingUp(runResult);

    Date startTestDate = new Date();
    runObjectives.setStartDate(startTestDate);

    logger.info("ScenarioFlow: ------ Start");
    List<RunScenarioFlowBasic> listFlows = startExecution(runScenarioWarmingUp.getListWarmingUpTask());

    waitEndExecution(runObjectives, startTestDate, listFlows);

    Date endTestDate = new Date();
    runObjectives.setEndDate(endTestDate);
    logger.info("ScenarioFlow: ------ Stop");

    stopExecution(listFlows);

    logger.info("ScenarioFlow: ------ CollectData");
    collectInformation(listFlows, runResult, recordCreationPIMap);

    // Check with Objective now
    logger.info("ScenarioFlow: ------ CheckObjectives");
    checkObjectives(runObjectives, startTestDate, endTestDate, runResult);

    logger.info("ScenarioFlow: ------ TheEnd");
  }



  /**
   * Start execution
   *
   * @return list of Flow started
   */
  private List<RunScenarioFlowBasic> startExecution(List<RunScenarioFlowBasic> listWarmingTask) {
    List<RunScenarioFlowBasic> listFlows = new ArrayList<>();
    for (ScenarioStep scenarioStep : runScenario.getScenario().getFlows()) {
      switch (scenarioStep.getType()) {
      case STARTEVENT -> {
        if (!runScenario.getRunParameters().isCreation()) {
          logger.info("According configuration, STARTEVENT[" + scenarioStep.getProcessId() + "] is fully disabled");
        } else {
          RunScenarioFlowStartEvent runStartEvent = new RunScenarioFlowStartEvent(
              serviceAccess.getTaskScheduler(scenarioStep.getProcessId()), scenarioStep, runScenario,
              new RunResult(runScenario));
          runStartEvent.execute();
          listFlows.add(runStartEvent);
        }
      }

      case SERVICETASK -> {
        Optional<RunScenarioFlowBasic> runServiceTaskOp = getFromList(listWarmingTask, scenarioStep.getTopic());

        if (!runScenario.getRunParameters().isServiceTask()) {
          logger.info("According configuration, SERVICETASK[{}] is fully disabled", scenarioStep.getTopic());
          if (runServiceTaskOp.isPresent())
            runServiceTaskOp.get().pleaseStop();
        } else if (runScenario.getRunParameters().blockExecutionServiceTask(scenarioStep.getTopic())) {
          logger.info("According configuration, SERVICETASK[{}] is disabled (only acceptable {})",
              scenarioStep.getTopic(), runScenario.getRunParameters().getFilterServiceTask());
          if (runServiceTaskOp.isPresent())
            runServiceTaskOp.get().pleaseStop();
        } else {
          if (runServiceTaskOp.isEmpty()) {

            RunScenarioFlowServiceTask runServiceTask = new RunScenarioFlowServiceTask(
                serviceAccess.getTaskScheduler("serviceTask"), scenarioStep, runScenario, new RunResult(runScenario));

            runServiceTask.execute();
            listFlows.add(runServiceTask);
          } else {
            listFlows.add(runServiceTaskOp.get());
          }
        }
      }

      case USERTASK -> {
        Optional<RunScenarioFlowBasic> runUserTaskOpt = getFromList(listWarmingTask, scenarioStep.getTaskId());

        if (!runScenario.getRunParameters().isUserTask()) {
          logger.info("According configuration, USERTASK[{}] is fully disabled", scenarioStep.getTaskId());
          if (runUserTaskOpt.isPresent())
            runUserTaskOpt.get().pleaseStop();
        } else {
          if (runUserTaskOpt.isEmpty()) {

            RunScenarioFlowUserTask runUserTask = new RunScenarioFlowUserTask(
                serviceAccess.getTaskScheduler("userTask"), scenarioStep, 0, runScenario, new RunResult(runScenario));

            runUserTask.execute();
            listFlows.add(runUserTask);
          } else {
            listFlows.add(runUserTaskOpt.get());
          }
        }
      }
      }
    }
    return listFlows;
  }


  private Optional<RunScenarioFlowBasic> getFromList(List<RunScenarioFlowBasic> listTasks, String topic) {
    return listTasks.stream().filter(t -> t.getTopic().equals(topic)).findFirst();
  }


  /**
   * Wait end of execution.  according to the time in the scenario, wait this time
   *
   * @param runObjectives checkObjectif: we may have a Flow Objectives
   * @param listFlows     list of flows to monitor the execution
   */
  private void waitEndExecution(RunObjectives runObjectives, Date startTestDate, List<RunScenarioFlowBasic> listFlows) {
    // Then wait the delay, and kill everything after
    Duration durationExecution = runScenario.getScenario().getFlowControl().getDuration();
    Duration durationWarmingUp = Duration.ZERO;
    // if this server didn't do the warmingUp, then other server did it: we have to keep this time into account
    if (!runScenario.getRunParameters().isWarmingUp()) {
      // is the scenario has a warming up defined?
      if (runScenario.getScenario().getWarmingUp() != null)
        durationWarmingUp = runScenario.getScenario().getWarmingUp().getDuration();
    }

    long endTimeExpected =
        startTestDate.getTime() + durationExecution.getSeconds() * 1000 + durationWarmingUp.getSeconds() * 1000;

    logger.info("Start: FixedWarmingUp {} s ExecutionDuration {} s (total {} s)", durationWarmingUp.getSeconds(),
        durationExecution.getSeconds(), durationWarmingUp.getSeconds() + durationExecution.getSeconds());

    while (System.currentTimeMillis() < endTimeExpected) {
      long currentTime = System.currentTimeMillis();
      long sleepTime = Math.min(30 * 1000, endTimeExpected - currentTime);
      try {
        Thread.sleep(sleepTime);
      } catch (InterruptedException e) {
      }
      int advancement = (int) (100.0 * (currentTime - startTestDate.getTime()) / (endTimeExpected
          - startTestDate.getTime()));
      runObjectives.heartBeat();
      logRealTime(listFlows, endTimeExpected - System.currentTimeMillis(), advancement);
    }
  }

  /**
   * Stop the execution
   *
   * @param listFlows list of flows to stop
   */
  private void stopExecution(List<RunScenarioFlowBasic> listFlows) {
    logger.info("End - wait end FlowBasic");
    // now, stop all executions
    for (RunScenarioFlowBasic flowBasic : listFlows) {
      flowBasic.pleaseStop();
    }
    // wait the end of all executions
    long numberOfActives = listFlows.size();
    int count = 0;
    while (numberOfActives > 0 && count < 100) {
      count++;
      numberOfActives = listFlows.stream()
          .filter(t -> !t.getStatus().equals(RunScenarioFlowBasic.STATUS.STOPPED))
          .count();
      if (numberOfActives > 0)
        try {
          Thread.sleep(2000);
        } catch (Exception e) {
          numberOfActives = 0;
        }
    }
  }

  /**
   * Collect multiple information
   *
   * @param listFlows           list of flow
   * @param runResult           runResult to populate
   * @param recordCreationPIMap statistics
   */
  private void collectInformation(List<RunScenarioFlowBasic> listFlows,
                                  RunResult runResult,
                                  Map<String, RunResult.RecordCreationPI> recordCreationPIMap) {
    // Collect information
    logger.info("CollectData : listFlows[{}]", listFlows.size());
    for (RunScenarioFlowBasic flowBasic : listFlows) {
      RunResult runResultFlow = flowBasic.getRunResult();
      runResult.add(runResultFlow);
      if (flowBasic instanceof RunScenarioFlowStartEvent) {
        String processId = flowBasic.getScenarioStep().getProcessId();
        RunResult.RecordCreationPI recordFlow = runResultFlow.getRecordCreationPI().get(processId);
        RunResult.RecordCreationPI recordCreationPI = recordCreationPIMap.getOrDefault(processId,
            new RunResult.RecordCreationPI(processId));

        recordCreationPI.add(recordFlow);
        recordCreationPIMap.put(processId, recordCreationPI);
        logger.info("CollectData : StartEvent, processId[{}] PICreated[{}] PIFailed[{}]", processId,
            recordFlow.nbCreated, recordFlow.nbFailed);
      }
    }
  }

  /**
   * Check the objective of the scenario
   *
   * @param startTestDate date when the test start
   * @param endTestDate   date when the test end
   * @param runResult     result to populate
   */
  private void checkObjectives(RunObjectives runObjectives, Date startTestDate, Date endTestDate, RunResult runResult) {

    // Objectives ask Operate, which get the result with a delay. So, wait 1 mn
    logger.info("CollectingData... (sleep 30s)");
    try {
      Thread.sleep(1000 * 30);
    } catch (InterruptedException e) {
      // do nothing
    }

    List<RunObjectives.ObjectiveResult> listCheckResult = runObjectives.check();
    for (RunObjectives.ObjectiveResult checkResult : listCheckResult) {
      if (checkResult.success) {
        logger.info("Objective: SUCCESS type[{}] label[{}} processId[{}] reach/objective {}/{} analysis [{}}",
            checkResult.objective.type, checkResult.objective.label, checkResult.objective.processId,
            checkResult.recordedSuccessValue, checkResult.objective.value, checkResult.analysis);
      } else {
        // do not need to log the error, already done
        runResult.addError(null,
            "Objective: FAIL " + checkResult.objective.getInformation() + " type[" + checkResult.objective.type
                + "] processId[" + checkResult.objective.processId // ProcessID
                + "] reach/objective " + checkResult.recordedSuccessValue // Reach
                + "/" + checkResult.objective.value // Objective
                + " " + checkResult.analysis);
      }
    }
  }

  /**
   * Log to see the advancement
   *
   * @param listFlows          list flows running
   * @param percentAdvancement percentAdvancement of the test, according the timeframe
   */
  private void logRealTime(List<RunScenarioFlowBasic> listFlows, long timeToFinishInMs, int percentAdvancement) {
    logger.info("------------ Log advancement at {} ----- {} %, end in {} s", new Date(), percentAdvancement,
        timeToFinishInMs / 1000);

    for (RunScenarioFlowBasic flowBasic : listFlows) {

      RunResult runResultFlow = flowBasic.getRunResult();
      int currentNumberOfThreads = flowBasic.getCurrentNumberOfThreads();
      // logs only flow with a result or currently active
      if (runResultFlow.getRecordCreationPIAllProcesses() + runResultFlow.getNumberOfSteps()
          + runResultFlow.getNumberOfErrorSteps() == 0 && currentNumberOfThreads == 0)
        continue;
      long previousValue = previousValueMap.getOrDefault(flowBasic.getId(), 0L);

      ScenarioStep scenarioStep = flowBasic.getScenarioStep();
      String key = "[" + flowBasic.getId() + "] " + flowBasic.getStatus().toString() + " currentNbThreads["
          + currentNumberOfThreads + "] ";
      key += switch (scenarioStep.getType()) {
        case STARTEVENT -> "PI[" + runResultFlow.getRecordCreationPI() + "] delta[" + (
            runResultFlow.getRecordCreationPI().get(flowBasic.getScenarioStep().getProcessId()).nbCreated
                - previousValue) + "]";
        case SERVICETASK -> "StepsExecuted[" + runResultFlow.getNumberOfSteps() + "] delta [" + (
            runResultFlow.getNumberOfSteps() - previousValue) + "] StepsErrors[" + runResultFlow.getNumberOfErrorSteps()
            + "]";
        case USERTASK -> "StepsExecuted[" + runResultFlow.getNumberOfSteps() + "] delta [" + (
            runResultFlow.getNumberOfSteps() - previousValue) + "] StepsErrors[" + runResultFlow.getNumberOfErrorSteps()
            + "]";

        default -> "]";
      };
      logger.info(key);
      switch (scenarioStep.getType()) {
      case STARTEVENT -> {
        previousValueMap.put(flowBasic.getId(),
            runResultFlow.getRecordCreationPI().get(flowBasic.getScenarioStep().getProcessId()).nbCreated);
      }
      case SERVICETASK -> {
        previousValueMap.put(flowBasic.getId(), (long) runResultFlow.getNumberOfSteps());
      }
      case USERTASK -> {
        previousValueMap.put(flowBasic.getId(), (long) runResultFlow.getNumberOfSteps());
      }
      default -> {
      }
      }

    }
    int nbThreadsServiceTask = 0;
    int nbThreadsAutomator = 0;
    int nbThreadsTimeWaiting = 0;
    int nbThreadsWaiting = 0;
    int nbThreadsTimeRunnable = 0;
    for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
      boolean isZeebe = false;
      boolean isServiceTask = false;
      boolean isAutomator = false;
      for (StackTraceElement ste : entry.getValue()) {
        if (ste.getClassName().contains("io.camunda"))
          isZeebe = true;
        else if (ste.getClassName().contains(RunScenarioFlowServiceTask.SimpleDelayHandler.class.getName()))
          isServiceTask = true;
        else if (ste.getClassName().contains(".automator."))
          isAutomator = true;

        // org.camunda.automator.engine.flow.RunScenarioFlowServiceTask$SimpleDelayCompletionHandler
      }
      if (!isZeebe && !isServiceTask && !isAutomator)
        continue;

      if (isServiceTask)
        nbThreadsServiceTask++;
      else if (isAutomator)
        nbThreadsAutomator++;
      else
        // TIME_WAITING: typical for the FlowServiceTask with a sleep
        if (entry.getKey().getState().equals(Thread.State.TIMED_WAITING)) {
          // is the thread is running the service task (with a Thread.sleep?
          nbThreadsTimeWaiting++;
        } else if (entry.getKey().getState().equals(Thread.State.WAITING)) {
          nbThreadsTimeWaiting++;
        } else if (entry.getKey().getState().equals(Thread.State.RUNNABLE)) {
          nbThreadsTimeRunnable++;
        } else {
          logger.info(" {} {}", entry.getKey(), entry.getKey().getState());
          for (StackTraceElement ste : entry.getValue()) {
            logger.info("\tat {}", ste);
          }

        }
    }
    BpmnEngine bpmnEngine = runScenario.getBpmnEngine();
    int workerExecutionThreads = bpmnEngine.getWorkerExecutionThreads();
    if (nbThreadsServiceTask + nbThreadsTimeWaiting + nbThreadsWaiting + nbThreadsTimeRunnable + nbThreadsAutomator > 0)
      logger.info(
          "Threads: ServiceTaskExecution (ThreadService/maxJobActive) [{}/{}] {} % Automator[{}] TIME_WAITING[{}] WAITING[{}] RUNNABLE[{}] ",
          nbThreadsServiceTask, workerExecutionThreads,
          workerExecutionThreads == 0 ? 0 : (int) (100.0 * nbThreadsServiceTask / workerExecutionThreads),
          nbThreadsAutomator, nbThreadsTimeWaiting, nbThreadsWaiting, nbThreadsTimeRunnable);
  }
}
