/* ******************************************************************** */
/*                                                                      */
/*  RunScenarioFlows                                                    */
/*                                                                      */
/*  Execute all flows in a scenario                                     */
/* ******************************************************************** */
package org.camunda.automator.engine.flow;

import org.camunda.automator.definition.ScenarioFlowControl;
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
    // Create one executor per flows
    Date startTestDate = new Date();

    List<RunScenarioFlowBasic> listFlows = startExecution();

    waitEndExecution(startTestDate, listFlows);

    Date endTestDate = new Date();
    logger.info("RunScenarioFlows: Stop execution");
    stopExecution(listFlows);

    Map<String, Long> processInstancesCreatedMap = new HashMap<>();
    collectInformation(listFlows, runResult, processInstancesCreatedMap);

    // Check with Objective now
    logger.info("RunScenarioFlows: Check objective");
    if (runScenario.getScenario().getFlowControl() != null
        && runScenario.getScenario().getFlowControl().getObjectives() != null) {
      checkObjectives(startTestDate, endTestDate, processInstancesCreatedMap, runResult);
    }

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
   * @param listFlows list of flows to monitor the execution
   */
  private void waitEndExecution(Date startTestDate, List<RunScenarioFlowBasic> listFlows) {
    // Then wait the delay, and kill everything after
    Duration durationExecution = runScenario.getScenario().getFlowControl().getDuration();
    long endTimeExpected = startTestDate.getTime() + durationExecution.getSeconds() * 1000;
    logger.info("RunScenarioFlows: start execution for [" + durationExecution.getSeconds() + " s]");
    while (System.currentTimeMillis() < endTimeExpected) {
      long currentTime = System.currentTimeMillis();
      long sleepTime = Math.min(30 * 1000, endTimeExpected - currentTime);
      try {
        Thread.sleep(sleepTime);
      } catch (InterruptedException e) {
      }
      int advancement = (int) (100.0 * (currentTime - startTestDate.getTime()) / (endTimeExpected
          - startTestDate.getTime()));
      logRealTime(listFlows, advancement);
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
    for (RunScenarioFlowBasic flowBasic : listFlows) {
      RunResult runResultFlow = flowBasic.getRunResult();
      runResult.add(runResultFlow);
      if (flowBasic instanceof RunScenarioFlowStartEvent) {
        String processId = flowBasic.getRunScenario().getScenario().getProcessId();
        long processInstanceCreated = processInstancesCreatedMap.getOrDefault(processId, Long.valueOf(0));
        processInstancesCreatedMap.put(processId, processInstanceCreated + runResultFlow.getNumberOfProcessInstances());
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
  private void checkObjectives(Date startTestDate,
                               Date endTestDate,
                               Map<String, Long> processInstancesCreatedMap,
                               RunResult runResult) {
    CheckObjectives checkObjectives = new CheckObjectives(runScenario.getBpmnEngine(), startTestDate, endTestDate,
        processInstancesCreatedMap);

    for (ScenarioFlowControl.Objective objective : runScenario.getScenario().getFlowControl().getObjectives()) {
      CheckObjectives.ObjectiveResult check = checkObjectives.check(objective);

      if (check.success) {
        if (runScenario.getRunParameters().isLevelMonitoring()) {
          logger.info("Objective: {} type {} processId[{}] value {} reached (get {} ) ", objective.label,
              objective.type, objective.processId, objective.value, check.realValue);
          // do not need to log the error, already done
        }
      } else {
        runResult.addError(null,
            "Objective " + objective.label + " type " + objective.type + " processId [" + objective.processId
                + "] FAILED " + check.analysis.toString());
      }
    }
  }

  /**
   * Log to see the advancement
   *
   * @param listFlows          list flows running
   * @param percentAdvancement percentAdvancement of the test, according the timeframe
   */
  private void logRealTime(List<RunScenarioFlowBasic> listFlows, int percentAdvancement) {
    logger.info("------------ Log advancement at {} ----- {} %", new Date(), percentAdvancement);

    for (RunScenarioFlowBasic flowBasic : listFlows) {

      RunResult runResultFlow = flowBasic.getRunResult();
      // logs only flow with a result
      if (runResultFlow.getNumberOfProcessInstances() + runResultFlow.getNumberOfSteps() == 0)
        continue;
      ScenarioStep scenarioStep = flowBasic.getScenarioStep();
      String key = "[" + flowBasic.getId() + "] " + flowBasic.getStatus().toString() + " ";
      key += switch (scenarioStep.getType()) {
        case STARTEVENT -> "PI[" + runResultFlow.getNumberOfProcessInstances() + "]";
        case SERVICETASK -> "Steps[" + runResultFlow.getNumberOfSteps() + "]";
        default -> "]";
      };
      logger.info(key);
    }
    int nbThreadsTimeWaiting = 0;
    int nbThreadsWaiting = 0;
    int nbThreadsTimeRunnable = 0;
    for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
      boolean isZeebe = false;

      for (StackTraceElement ste : entry.getValue()) {
        if (ste.getClassName().contains("io.camunda"))
          isZeebe = true;
      }
      if (!isZeebe)
        continue;
      logger.info("{} {}", entry.getKey(), entry.getKey().getState());

      // TIME_WAITING: typical for the FlowServiceTask with a sleep
      if (entry.getKey().getState().equals(Thread.State.TIMED_WAITING)) {
        nbThreadsTimeWaiting++;
      } else if (entry.getKey().getState().equals(Thread.State.WAITING)) {
        nbThreadsWaiting++;
      } else if (entry.getKey().getState().equals(Thread.State.RUNNABLE)) {
        nbThreadsTimeRunnable++;
      } else {
        for (StackTraceElement ste : entry.getValue()) {
          logger.info("\tat {}", ste);
        }
      }
    }
    if (nbThreadsTimeWaiting > 0)
      logger.info("Threads {} TIME_WAITING, {} WAITING, {} RUNNABLE ", nbThreadsTimeWaiting, nbThreadsWaiting,
          nbThreadsTimeRunnable);
  }
}
