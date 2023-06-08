/* ******************************************************************** */
/*                                                                      */
/*  RunScenarioFlows                                                    */
/*                                                                      */
/*  Execute all flows in a scenario                                     */
/* ******************************************************************** */
package org.camunda.automator.engine.flow;

import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenario;
import org.camunda.automator.services.ServiceAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunScenarioFlows {
  private final ServiceAccess serviceAccess;
  private final RunScenario runScenario;
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
    Map<String, Long> processInstancesCreatedMap = new HashMap<>();
    RunObjectives runObjectives = new RunObjectives(runScenario.getScenario().getFlowControl().getObjectives(),
        runScenario.getBpmnEngine(), processInstancesCreatedMap);

    logger.info("ScenarioFlow: ------ WarmingUp");
    runScenarioWarmingUp.warmingUp();

    Date startTestDate = new Date();
    runObjectives.setStartDate(startTestDate);

    logger.info("ScenarioFlow: ------ Start");
    List<RunScenarioFlowBasic> listFlows = startExecution();

    waitEndExecution(runObjectives, startTestDate, listFlows);

    Date endTestDate = new Date();
    runObjectives.setEndDate(endTestDate);
    logger.info("ScenarioFlow: ------ Stop Execution");

    stopExecution(listFlows);

    logger.info("ScenarioFlow: ------ Collect Data");
    collectInformation(listFlows, runResult, processInstancesCreatedMap);

    // Check with Objective now
    logger.info("ScenarioFlow: ------ Check objective");
    if (runScenario.getScenario().getFlowControl() != null
        && runScenario.getScenario().getFlowControl().getObjectives() != null) {
      checkObjectives(runObjectives, startTestDate, endTestDate, processInstancesCreatedMap, runResult);
    }
    logger.info("ScenarioFlow: ------ The end");
  }

  /**
   * Start execution
   *
   * @return list of Flow started
   */
  private List<RunScenarioFlowBasic> startExecution() {
    List<RunScenarioFlowBasic> listFlows = new ArrayList<>();
    for (ScenarioStep scenarioStep : runScenario.getScenario().getFlows()) {
      if (ScenarioStep.Step.STARTEVENT.equals(scenarioStep.getType())) {
        if (!runScenario.getRunParameters().creation) {
          logger.info("According configuration, STARTEVENT[" + scenarioStep.getProcessId() + "] Does not start");
        } else
          for (int i = 0; i < scenarioStep.getNbWorkers(); i++) {
            RunScenarioFlowStartEvent runStartEvent = new RunScenarioFlowStartEvent(
                serviceAccess.getTaskScheduler(scenarioStep.getProcessId() + "-" + i), scenarioStep, i, runScenario,
                new RunResult(runScenario));
            runStartEvent.execute();
            listFlows.add(runStartEvent);
          }
      }
      if (ScenarioStep.Step.SERVICETASK.equals(scenarioStep.getType())) {
        if (!runScenario.getRunParameters().servicetask) {
          logger.info("According configuration, SERVICETASK[{}] Does not start", scenarioStep.getTopic());
        } else if (runScenario.getRunParameters().blockExecutionServiceTask(scenarioStep.getTopic())) {
          logger.info("According configuration, SERVICETASK[{}] is block (only acceptable {})", scenarioStep.getTopic(),
              runScenario.getRunParameters().filterServiceTask);
        } else {
          RunScenarioFlowServiceTask runStartEvent = new RunScenarioFlowServiceTask(scenarioStep, 0, runScenario,
              new RunResult(runScenario));
          runStartEvent.execute();
          listFlows.add(runStartEvent);
        }
      }
    }
    return listFlows;
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
    // if this server didn't do the warmingup, then other server did it: we have to keep this time into account
    if (!runScenario.getRunParameters().warmingUp)
      durationWarmingUp = runScenario.getScenario().getWarmingUp().getDuration();

    long endTimeExpected =
        startTestDate.getTime() + durationExecution.getSeconds() * 1000 + durationWarmingUp.getSeconds() * 1000;

    logger.info("RunScenarioFlows: start execution Fixed WarmingUp {} s ExecutionDuration {} s (total {} s)",
        durationWarmingUp.getSeconds(), durationExecution.getSeconds(),
        durationWarmingUp.getSeconds() + durationExecution.getSeconds());

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
    logger.info("RunScenarioFlows: end of game - wait end FlowBasic");
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
   * @param listFlows                  list of flow
   * @param runResult                  runResult to populate
   * @param processInstancesCreatedMap statistics
   */
  private void collectInformation(List<RunScenarioFlowBasic> listFlows,
                                  RunResult runResult,
                                  Map<String, Long> processInstancesCreatedMap) {
    // Collect information
    logger.info("Collect Data : listFlows[{}]", listFlows.size());
    for (RunScenarioFlowBasic flowBasic : listFlows) {
      RunResult runResultFlow = flowBasic.getRunResult();
      runResult.add(runResultFlow);
      if (flowBasic instanceof RunScenarioFlowStartEvent) {
        String processId = flowBasic.getRunScenario().getScenario().getProcessId();
        long processInstanceCreated = processInstancesCreatedMap.getOrDefault(processId, Long.valueOf(0));
        processInstancesCreatedMap.put(processId, processInstanceCreated + runResultFlow.getNumberOfProcessInstances());
        logger.info("Collect Data : Flow is Start Event, processId[{}] processInstanceCreated[{}]", processId,
            processInstanceCreated);
      }
    }
  }

  /**
   * Check the objective of the scenario
   *
   * @param startTestDate              date when the test start
   * @param endTestDate                date when the test end
   * @param processInstancesCreatedMap statistic
   * @param runResult                  result to populate
   */
  private void checkObjectives(RunObjectives runObjectives,
                               Date startTestDate,
                               Date endTestDate,
                               Map<String, Long> processInstancesCreatedMap,
                               RunResult runResult) {

    // Objectives ask Operate, which get the result with a delay. So, wait 1 mn
    logger.info("Collecting data...");
    try {
      Thread.sleep(1000 * 60);
    } catch (InterruptedException e) {
      // do nothing
    }

    List<RunObjectives.ObjectiveResult> listCheckResult = runObjectives.check();
    for (RunObjectives.ObjectiveResult checkResult : listCheckResult) {
      if (checkResult.success) {
        logger.info("Objective: SUCCESS type {}  label [{}} processId[{}] reach {} (objective is {} ) analysis [{}}",
            checkResult.objective.type, checkResult.objective.label, checkResult.objective.processId,
            checkResult.realValue, checkResult.objective.value, checkResult.analysis);
        // do not need to log the error, already done

      } else {
        runResult.addError(null,
            "Objective: FAIL " + checkResult.objective.label + " type " + checkResult.objective.type + " processId ["
                + checkResult.objective.processId + "] " + checkResult.analysis.toString());
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
      // logs only flow with a result
      if (runResultFlow.getNumberOfProcessInstances() + runResultFlow.getNumberOfSteps()
          + runResultFlow.getNumberOfErrorSteps() == 0)
        continue;
      ScenarioStep scenarioStep = flowBasic.getScenarioStep();
      String key = "[" + flowBasic.getId() + "] " + flowBasic.getStatus().toString() + " ";
      key += switch (scenarioStep.getType()) {
        case STARTEVENT -> "PI[" + runResultFlow.getNumberOfProcessInstances() + "]";
        case SERVICETASK -> "StepsExecuted[" + runResultFlow.getNumberOfSteps() + "] StepsErrors["
            + runResultFlow.getNumberOfErrorSteps() + "]";
        default -> "]";
      };
      logger.info(key);
    }
    int nbThreadsServiceTask = 0;
    int nbThreadsAutomator=0;
    int nbThreadsTimeWaiting = 0;
    int nbThreadsWaiting = 0;
    int nbThreadsTimeRunnable = 0;
    for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
      boolean isZeebe = false;
      boolean isServiceTask = false;
      boolean isAutomator = false;
      String analysis = "";
      for (StackTraceElement ste : entry.getValue()) {
        analysis += ste.toString() + ", ";
        if (ste.getClassName().contains("io.camunda"))
          isZeebe = true;
        else if (ste.getClassName().contains(RunScenarioFlowServiceTask.SimpleDelayCompletionHandler.class.getName()))
          isServiceTask = true;
        else if (ste.getClassName().contains(".automator."))
          isAutomator = true;

        // org.camunda.automator.engine.flow.RunScenarioFlowServiceTask$SimpleDelayCompletionHandler
      }
      if (isAutomator)
        logger.info(analysis);
      if (!isZeebe && !isServiceTask && ! isAutomator)
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
        logger.info("{} {}", entry.getKey(), entry.getKey().getState());
        for (StackTraceElement ste : entry.getValue()) {
          logger.info("\tat {}", ste);
        }
      }
    }
    if (nbThreadsServiceTask + nbThreadsTimeWaiting + nbThreadsWaiting + nbThreadsTimeRunnable +nbThreadsAutomator> 0)
      logger.info("Threads: ServiceTaskExecution[{}] Automator[{}] TIME_WAITING[{}] WAITING[{}] RUNNABLE[{}] ", nbThreadsServiceTask,
          nbThreadsAutomator,
          nbThreadsTimeWaiting, nbThreadsWaiting, nbThreadsTimeRunnable);
  }
}
