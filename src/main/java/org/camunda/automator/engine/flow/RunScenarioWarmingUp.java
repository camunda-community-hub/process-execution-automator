/* ******************************************************************** */
/*                                                                      */
/*  RunScenarioWarmingUp                                                */
/*                                                                      */
/*  Manage the warming up of a scenario                                 */
/* ******************************************************************** */
package org.camunda.automator.engine.flow;

import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.definition.ScenarioWarmingUp;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenario;
import org.camunda.automator.engine.RunZeebeOperation;
import org.camunda.automator.services.ServiceAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class RunScenarioWarmingUp {
  private final ServiceAccess serviceAccess;
  private final RunScenario runScenario;
  Logger logger = LoggerFactory.getLogger(RunScenarioWarmingUp.class);

  RunScenarioWarmingUp(ServiceAccess serviceAccess, RunScenario runScenario) {
    this.serviceAccess = serviceAccess;
    this.runScenario = runScenario;
  }

  /**
   * warmingUp
   * Do it!
   */
  public void warmingUp() {
    ScenarioWarmingUp warmingUp = runScenario.getScenario().getWarmingUp();
    if (warmingUp == null) {
      logger.info("WarmingUp not present");
      return;
    }
    if (!runScenario.getRunParameters().warmingUp) {
      logger.info("WarmingUp present, but not allowed to start");
      return;
    }
    long beginTime = System.currentTimeMillis();

    // If no duration is set, then 10 Mn max
    long endWarmingUp =
        beginTime + (warmingUp.getDuration().toMillis() > 0 ? warmingUp.getDuration().toMillis() : 1000 * 60 * 10);

    logger.info("WarmingUp: Start ---- {} operations ", warmingUp.getOperations().size());

    List<RunScenarioFlowServiceTask> listWarmingUpServiceTask = new ArrayList<>();
    List<StartEventWarmingUpRunnable> listWarmingUpStartEvent = new ArrayList<>();
    for (ScenarioStep scenarioStep : warmingUp.getOperations()) {
      switch (scenarioStep.getType()) {
      case STARTEVENT -> {
        logger.info("WarmingUp: StartEvent Generate {} Frequency [{}] EndWarmingUp [{}]",
            scenarioStep.getNumberOfExecutions(), scenarioStep.getFrequency(), scenarioStep.getEndWarmingUp());

        StartEventWarmingUpRunnable startEventWarmingUpRunnable = new StartEventWarmingUpRunnable(
            serviceAccess.getTaskScheduler("warmingUp"), scenarioStep, runScenario);
        listWarmingUpStartEvent.add(startEventWarmingUpRunnable);
        startEventWarmingUpRunnable.run();
      }
      case SERVICETASK -> {
        logger.info("WarmingUp: Start Service Task topic[{}]", scenarioStep.getTopic());
        RunScenarioFlowServiceTask task = new RunScenarioFlowServiceTask(serviceAccess.getTaskScheduler("serviceTask"),
            scenarioStep, 0, runScenario, new RunResult(runScenario));
        task.execute();
        listWarmingUpServiceTask.add(task);
      }
      default -> {
        logger.info("WarmingUp: Unknown [{}]", scenarioStep.getType());
      }
      }
    }

    // check if we reach the end of the warming up
    boolean warmingUpIsFinish = false;
    while (!warmingUpIsFinish) {
      long currentTime = System.currentTimeMillis();
      String analysis = " max in " + (endWarmingUp - currentTime) / 1000 + " s, ";
      if (currentTime >= endWarmingUp) {
        analysis += "Over maximum duration,";
        warmingUpIsFinish = true;
      }
      boolean allIsFinished = true;
      for (StartEventWarmingUpRunnable startRunnable : listWarmingUpStartEvent) {
        analysis += "warmingUp[" + startRunnable.scenarioStep.getTaskId() + "] instanceCreated["
            + startRunnable.nbInstancesCreated + "]";
        if (startRunnable.warmingUpFinished) {
          analysis += " FINISH!:" + startRunnable.warmingUpFinishedAnalysis;
        } else {
          allIsFinished = false;
        }

      }
      if (allIsFinished) {
        warmingUpIsFinish = true;
      }
      logger.info("WarmingUpFinished? {} analysis: {}", warmingUpIsFinish, analysis);
      if (!warmingUpIsFinish) {
        try {
          Thread.sleep(1000L * 15);
        } catch (InterruptedException e) {
          // do not care
        }
      }
    }

    // stop everything
    for (StartEventWarmingUpRunnable startRunnable : listWarmingUpStartEvent) {
      startRunnable.pleaseStop(true);
    }

    // stop all tasks now
    for (RunScenarioFlowServiceTask task : listWarmingUpServiceTask) {
      task.pleaseStop();
    }
    // now warmup is finished
    logger.info("WarmingUp: Complete ----");

  }

  /**
   * StartEventRunnable
   */
  class StartEventWarmingUpRunnable implements Runnable {

    private final TaskScheduler scheduler;
    private final ScenarioStep scenarioStep;
    private final RunScenario runScenario;
    public boolean stop = false;
    public boolean warmingUpFinished = false;
    public String warmingUpFinishedAnalysis = "";
    public int nbInstancesCreated = 0;
    private int nbOverloaded = 0;

    public StartEventWarmingUpRunnable(TaskScheduler scheduler, ScenarioStep scenarioStep, RunScenario runScenario) {
      this.scheduler = scheduler;
      this.scenarioStep = scenarioStep;
      this.runScenario = runScenario;
    }

    public void pleaseStop(boolean stop) {
      this.stop = stop;
    }

    @Override
    public void run() {
      if (stop) {
        return;
      }
      // check if the condition is reach
      CheckFunctionResult checkFunctionResult = null;
      if (scenarioStep.getEndWarmingUp() != null) {
        checkFunctionResult = endCheckFunction(scenarioStep.getEndWarmingUp());
        if (checkFunctionResult.goalReach) {
          warmingUpFinishedAnalysis += "GoalReach[" + checkFunctionResult.analysis + "]";
          warmingUpFinished = true;
          return;
        }
      }
      // continue to generate PI
      long begin = System.currentTimeMillis();
      List<String> listProcessInstance = new ArrayList<>();
      try {
        for (int i = 0; i < scenarioStep.getNumberOfExecutions(); i++) {
          String processInstance = runScenario.getBpmnEngine()
              .createProcessInstance(scenarioStep.getProcessId(), scenarioStep.getTaskId(), // activityId
                  RunZeebeOperation.getVariablesStep(runScenario, scenarioStep));
          nbInstancesCreated++;
          if (listProcessInstance.size() < 21)
            listProcessInstance.add(processInstance);
        }
      } catch (AutomatorException e) {
        logger.error("Error at creation: [{}]", e.getMessage());
      }
      long end = System.currentTimeMillis();
      // one step generation?
      if (scenarioStep.getFrequency() == null || scenarioStep.getFrequency().isEmpty()) {
        if (runScenario.getRunParameters().isLevelMonitoring()) {
          logger.info("WarmingUp:StartEvent Create[{}] in {} " + " ms" + " (oneShoot) listPI(max20): ",
              scenarioStep.getNumberOfExecutions(), (end - begin),
              listProcessInstance.stream().collect(Collectors.joining(",")));
        }
        warmingUpFinishedAnalysis += "GoalOneShoot";
        warmingUpFinished = true;
        return;
      }

      Duration duration = Duration.parse(scenarioStep.getFrequency());
      duration = duration.minusMillis(end - begin);
      if (duration.isNegative()) {
        duration = Duration.ZERO;
        nbOverloaded++;
      }

      if (runScenario.getRunParameters().isLevelMonitoring()) {
        logger.info(
            "Warmingup Create[" + scenarioStep.getNumberOfExecutions() + "] in " + (end - begin) + " ms" + " Sleep ["
                + duration.getSeconds() + " s]" + (checkFunctionResult == null ?
                "" :
                "EndWarmingUp:" + checkFunctionResult.analysis));
      }
      scheduler.schedule(this, Instant.now().plusMillis(duration.toMillis()));
    }

    private CheckFunctionResult endCheckFunction(String function) {
      try {
        int posParenthesis = function.indexOf("(");
        String functionName = function.substring(0, posParenthesis);
        String parameters = function.substring(posParenthesis + 1);
        parameters = parameters.substring(0, parameters.length() - 1);
        StringTokenizer st = new StringTokenizer(parameters, ",");
        if ("UserTaskThreshold".equalsIgnoreCase(functionName)) {
          String taskId = st.hasMoreTokens() ? st.nextToken() : "";
          Integer threshold = st.hasMoreTokens() ? Integer.valueOf(st.nextToken()) : 0;
          long value = runScenario.getBpmnEngine().countNumberOfTasks(runScenario.getScenario().getProcessId(), taskId);
          return new CheckFunctionResult(value >= threshold,
              "Task[" + taskId + "] value [" + value + "] / threshold[" + threshold + "]");
        }
        logger.error("Unknown function [{}]", functionName);
        return new CheckFunctionResult(false, "Unknown function");
      } catch (AutomatorException e) {
        logger.error("Error during warmingup {}", e.getMessage());
        return new CheckFunctionResult(false, "Exception " + e.getMessage());
      }
    }

    /**
     * UserTaskTask(<taskId>,<numberOfTaskExpected>)
     */
    public record CheckFunctionResult(boolean goalReach, String analysis) {
    }
  }
}
