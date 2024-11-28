/* ******************************************************************** */
/*                                                                      */
/*  RunScenarioWarmingUp                                                */
/*                                                                      */
/*  Manage the warming up of a scenario                                 */
/* ******************************************************************** */
package org.camunda.automator.engine.flow;

import io.camunda.operate.search.DateFilter;
import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.definition.ScenarioWarmingUp;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenario;
import org.camunda.automator.services.ServiceAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RunScenarioWarmingUp {
    private final ServiceAccess serviceAccess;
    private final RunScenario runScenario;
    Logger logger = LoggerFactory.getLogger(RunScenarioWarmingUp.class);

    List<RunScenarioFlowServiceTask> listWarmingUpServiceTask = new ArrayList<>();
    List<RunScenarioFlowUserTask> listWarmingUpUserTask = new ArrayList<>();

    RunScenarioWarmingUp(ServiceAccess serviceAccess, RunScenario runScenario) {
        this.serviceAccess = serviceAccess;
        this.runScenario = runScenario;
    }

    /**
     * warmingUp
     * Do it!
     *
     * @param runResult populate the runResult
     */
    public void warmingUp(RunResult runResult) {
        ScenarioWarmingUp warmingUp = runScenario.getScenario().getWarmingUp();
        if (warmingUp == null) {
            logger.info("WarmingUp not present in scenario");
            return;
        }
        if (!runScenario.getRunParameters().isWarmingUp()) {
            logger.info("WarmingUp present, but not allowed to start");
            return;
        }
        long beginTime = System.currentTimeMillis();

        // If no duration is set, then 10 Mn max
        long endWarmingUp =
                beginTime + (warmingUp.getDuration().toMillis() > 0 ? warmingUp.getDuration().toMillis() : 1000 * 60 * 10);

        listWarmingUpServiceTask.clear();
        listWarmingUpUserTask.clear();
        List<StartEventWarmingUpRunnable> listWarmingUpStartEvent = new ArrayList<>();
        List<ScenarioStep> listOperationWarmingUp = warmingUp.getOperations();
        if (warmingUp.useServiceTasks && runScenario.getRunParameters().isServiceTask()) {
            listOperationWarmingUp.addAll(runScenario.getScenario()
                    .getFlows()
                    .stream()
                    .filter(t -> t.getType().equals(ScenarioStep.Step.SERVICETASK))
                    .toList());
        }
        if (warmingUp.useUserTasks && runScenario.getRunParameters().isUserTask()) {
            listOperationWarmingUp.addAll(runScenario.getScenario()
                    .getFlows()
                    .stream()
                    .filter(t -> t.getType().equals(ScenarioStep.Step.USERTASK))
                    .toList());
        }

        logger.info("WarmingUp: Start ---- {} operations (Scenario/Policy: serviceTask:{}/{} userTask: {}/{})",
                listOperationWarmingUp.size(), // size of operations
                warmingUp.useServiceTasks, // scenario allow service task?
                runScenario.getRunParameters().isServiceTask(), // pod can run service task?
                warmingUp.useUserTasks, runScenario.getRunParameters().isUserTask() // pod can run User Task?
        );

        for (ScenarioStep scenarioStep : listOperationWarmingUp) {
            switch (scenarioStep.getType()) {
                case STARTEVENT -> {
                    logger.info("WarmingUp: StartEvent GeneratePI[{}] Frequency[{}] EndWarmingUp[{}]",
                            scenarioStep.getNumberOfExecutions(), scenarioStep.getFrequency(), scenarioStep.getEndWarmingUp());
                    StartEventWarmingUpRunnable startEventWarmingUpRunnable = new StartEventWarmingUpRunnable(
                            serviceAccess.getTaskScheduler("warmingUp"), scenarioStep, 0, runScenario, runResult);
                    listWarmingUpStartEvent.add(startEventWarmingUpRunnable);
                    startEventWarmingUpRunnable.start();
                }
                case SERVICETASK -> {
                    logger.info("WarmingUp: Start Service Task topic[{}]", scenarioStep.getTopic());
                    RunScenarioFlowServiceTask task = new RunScenarioFlowServiceTask(serviceAccess.getTaskScheduler("serviceTask"),
                            scenarioStep, runScenario, new RunResult(runScenario));
                    task.execute();
                    listWarmingUpServiceTask.add(task);
                }
                case USERTASK -> {
                    logger.info("WarmingUp: Start User Task taskId[{}]", scenarioStep.getTaskId());
                    RunScenarioFlowUserTask userTask = new RunScenarioFlowUserTask(serviceAccess.getTaskScheduler("userTask"),
                            scenarioStep, 0, runScenario, new RunResult(runScenario));
                    userTask.execute();
                    listWarmingUpUserTask.add(userTask);
                }
                default -> logger.info("WarmingUp: Unknown [{}]", scenarioStep.getType());

            }
        }

        // check if we reach the end of the warming up
        boolean warmingUpIsFinish = false;
        while (!warmingUpIsFinish) {
            long currentTime = System.currentTimeMillis();
            String analysis = "Limit warmupDuration in " + (endWarmingUp - currentTime) / 1000 + " s, ";
            if (currentTime >= endWarmingUp) {
                analysis += "OVER_MAXIMUM";
                warmingUpIsFinish = true;
            }
            boolean allIsFinished = true;
            for (StartEventWarmingUpRunnable startRunnable : listWarmingUpStartEvent) {
                analysis += "/ warmingUp[" + startRunnable.scenarioStep.getTaskId() + "] instanceCreated["
                        + startRunnable.nbInstancesCreated + "]";

                // the warmup finished boolean is not made here: each runnable (separate thread) check it.
                if (startRunnable.warmingUpFinished) {
                    analysis += " FINISH " + startRunnable.warmingUpFinishedAnalysis;
                } else {
                    analysis += " NOT_FINISH " + startRunnable.warmingUpNotFinishedAnalysis;
                    allIsFinished = false;
                }

            }
            if (allIsFinished) {
                warmingUpIsFinish = true;
            }
            logger.info("WarmingUpFinished? {} analysis:[{}]", warmingUpIsFinish, analysis);
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

        // now warmup is finished
        logger.info("WarmingUp: Complete ----");
    }

    public List<RunScenarioFlowServiceTask> getListWarmingUpServiceTask() {
        return listWarmingUpServiceTask;
    }

    public List<RunScenarioFlowUserTask> getListWarmingUpUserTask() {
        return listWarmingUpUserTask;
    }

    public List<RunScenarioFlowBasic> getListWarmingUpTask() {
        return Stream.concat(listWarmingUpServiceTask.stream(), listWarmingUpUserTask.stream())
                .collect(Collectors.toList());
    }

    /**
     * StartEventRunnable
     * Must be runnable because we will schedule it.
     */
    class StartEventWarmingUpRunnable implements Runnable {

        private final TaskScheduler scheduler;
        private final ScenarioStep scenarioStep;
        private final RunScenario runScenario;
        private final RunResult runResult;
        public boolean stop = false;
        public boolean warmingUpFinished = false;
        public String warmingUpFinishedAnalysis = "";
        public String warmingUpNotFinishedAnalysis = "Not verified yet";
        public int nbInstancesCreated = 0;
        private int nbOverloaded = 0;
        private int executionBatchNumber = 1;

        public StartEventWarmingUpRunnable(TaskScheduler scheduler,
                                           ScenarioStep scenarioStep,
                                           int index,
                                           RunScenario runScenario,
                                           RunResult runResult) {
            this.scheduler = scheduler;
            this.scenarioStep = scenarioStep;
            this.runScenario = runScenario;
            this.runResult = runResult;
        }

        public void pleaseStop(boolean stop) {
            this.stop = stop;
        }

        /**
         * Start it in a new tread
         */
        public void start() {
            scheduler.schedule(this, Instant.now());

        }

        @Override
        public void run() {
            if (stop) {
                return;
            }
            executionBatchNumber++;
            // check if the condition is reach
            CheckFunctionResult checkFunctionResult = null;
            if (scenarioStep.getEndWarmingUp() != null) {
                checkFunctionResult = endCheckFunction(scenarioStep.getEndWarmingUp(), runResult);
                if (checkFunctionResult.goalReach) {
                    warmingUpFinishedAnalysis += "GoalReach[" + checkFunctionResult.analysis + "]";
                    warmingUpNotFinishedAnalysis = "";
                    warmingUpFinished = true;
                    return;
                } else {
                    warmingUpNotFinishedAnalysis = checkFunctionResult.analysis();
                }
            }
            // continue to generate PI
            long begin = System.currentTimeMillis();
            CreateProcessInstanceThread createProcessInstanceThread = new CreateProcessInstanceThread(executionBatchNumber,
                    scenarioStep, runScenario, runResult);

            Duration durationWarmup;
            if (scenarioStep.getFrequency() == null || scenarioStep.getFrequency().isEmpty()) {
                durationWarmup = Duration.ofHours(1);
            } else {
                durationWarmup = Duration.parse(scenarioStep.getFrequency());
            }

            createProcessInstanceThread.createProcessInstances(durationWarmup);

            long end = System.currentTimeMillis();
            // one step generation?
            if (scenarioStep.getFrequency() == null || scenarioStep.getFrequency().isEmpty()) {
                if (runScenario.getRunParameters().showLevelMonitoring()) {
                    logger.info("WarmingUp:StartEvent Create[{}] in {} ms (oneShoot) listPI(max20): ",
                            scenarioStep.getNumberOfExecutions(), (end - begin),
                            createProcessInstanceThread.getListProcessInstances().stream().collect(Collectors.joining(",")));
                }
                warmingUpFinishedAnalysis += "GoalOneShoot";
                warmingUpFinished = true;
                return;
            }

            if (createProcessInstanceThread.isOverload()) {
                nbOverloaded++;
            }
            Duration durationToWait;
            if (scenarioStep.getFrequency() == null || scenarioStep.getFrequency().isEmpty()) {
                durationToWait = Duration.ZERO;
            } else {
                durationToWait = durationWarmup.minusMillis(end - begin);
                if (durationToWait.isNegative()) {
                    durationToWait = Duration.ZERO;
                }
            }
            if (runScenario.getRunParameters().showLevelMonitoring()) {
                logger.info("Warmingup batch_#{} Create real/scenario[{}/{}] in {} ms Sleep[{} s] {}", // log
                        executionBatchNumber, // batch
                        createProcessInstanceThread.getTotalCreation(), scenarioStep.getNumberOfExecutions(),
                        // Number of creation request
                        (end - begin), // duration
                        durationToWait.getSeconds(),  // Sleep for the frequency
                        (checkFunctionResult == null ? "" : "EndWarmingUp:" + checkFunctionResult.analysis));
            }
            scheduler.schedule(this, Instant.now().plusMillis(durationToWait.toMillis()));
        }

        /**
         * Check the function
         *
         * @param function  function to check
         * @param runResult runResult to fulfill information
         * @return status
         */
        private CheckFunctionResult endCheckFunction(String function, RunResult runResult) {
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
                } else if ("EndEventThreshold".equalsIgnoreCase(functionName)) {
                    String endId = st.hasMoreTokens() ? st.nextToken() : "";
                    Integer threshold = st.hasMoreTokens() ? Integer.valueOf(st.nextToken()) : 0;

                    long value = runScenario.getBpmnEngine()
                            .countNumberOfProcessInstancesEnded(runScenario.getScenario().getProcessId(),
                                    new DateFilter(runResult.getStartDate()), new DateFilter(new Date()));
                    return new CheckFunctionResult(value >= threshold,
                            "End[" + endId + "] value [" + value + "] / threshold[" + threshold + "]");

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
