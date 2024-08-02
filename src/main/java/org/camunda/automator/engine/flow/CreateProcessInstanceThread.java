package org.camunda.automator.engine.flow;

import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenario;
import org.camunda.automator.engine.RunZeebeOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CreateProcessInstanceThread {
  private final int executionBatchNumber;
  private final ScenarioStep scenarioStep;
  private final RunScenario runScenario;
  private final RunResult runResult;
  private final Logger logger = LoggerFactory.getLogger(CreateProcessInstanceThread.class);
  private final List<StartProcess> listStartProcess = new ArrayList<>();

  /**
   * @param executionBatchNumber Each time a new batch is running, this number increase
   * @param scenarioStep         scenario step
   * @param runScenario          scenario
   * @param runResult            result to fulfill
   */
  public CreateProcessInstanceThread(int executionBatchNumber,
                                     ScenarioStep scenarioStep,
                                     RunScenario runScenario,
                                     RunResult runResult) {
    this.executionBatchNumber = executionBatchNumber;
    this.scenarioStep = scenarioStep;
    this.runScenario = runScenario;
    this.runResult = runResult;
  }

  /**
   * After the duration, we stop
   *
   * @param durationToCreateProcessInstances maximum duration to produce all PI
   */
  public void createProcessInstances(Duration durationToCreateProcessInstances) {

    int numberOfThreads = scenarioStep.getNbWorkers() == 0 ? 1 : scenarioStep.getNbWorkers();

    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    int totalNumberOfPi = 0;

    int processInstancePerThread = (int) Math.ceil(1.0 * scenarioStep.getNumberOfExecutions() / numberOfThreads);
    // Submit tasks to the executor
    for (int i = 0; i < numberOfThreads; i++) {
      int numberOfProcessInstanceToStart = Math.min(processInstancePerThread,
          scenarioStep.getNumberOfExecutions() - totalNumberOfPi);
      totalNumberOfPi += numberOfProcessInstanceToStart;
      StartProcess task = new StartProcess(executionBatchNumber, i, numberOfProcessInstanceToStart,
          durationToCreateProcessInstances, scenarioStep, runScenario, runResult);
      executor.submit(task);
      listStartProcess.add(task);
    }
    // Shut down the executor and wait for all tasks to complete
    executor.shutdown();
    try {
      executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (InterruptedException e) {
      logger.error("Error during waiting for the end of all tasks");
    }
  }

  public List<String> getListProcessInstances() {
    return listStartProcess.stream().flatMap(t -> t.listProcessInstances.stream()).collect(Collectors.toList());
  }


  public int getNumberOfRunningThreads() {
    return (int) listStartProcess.stream().filter(t->t.isRunning()).count();
  }

  public int getTotalCreation() {
    return listStartProcess.stream().mapToInt(t -> t.nbCreation).sum();
  }

  public int getTotalFailed() {
    return listStartProcess.stream().mapToInt(t -> t.nbFailed).sum();
  }

  /**
   * return true if the creation overload the durationToCreate: we can't create all PI in the duration
   *
   * @return true if it wasn't possible to create all PI during the duration
   */
  public boolean isOverload() {
    return listStartProcess.stream().anyMatch(t -> t.isOverload);
  }

  /**
   * This subclass start numberOfProcessInstanceToStart of process instances.
   * Multiple threads doing the same operation are running at the same time.
   */
  private class StartProcess implements Runnable {
    private final ScenarioStep scenarioStep;
    private final RunResult runResult;
    private final RunScenario runScenario;
    private final int executionBatchNumber;
    private final int indexInBatch;
    int numberOfProcessInstanceToStart;
    List<String> listProcessInstances = new ArrayList<>();
    int nbCreation = 0;
    int nbFailed = 0;
    /**
     * the batch number
     */
    boolean isOverload = false;

    boolean isRunning = false;

    Duration durationToCreateProcessInstances;

    /**
     * @param executionBatchNumber
     * @param indexInBatch                     the component number, when multiple component where generated to handle the flow
     * @param numberOfProcessInstanceToStart number of process instance to start by this object
     * @param durationToCreateProcessInstances duration max allowed to create process instance
     * @param scenarioStep step to use to create the process instance
     * @param runScenario scenario to use
     * @param runResult result object to save information
     */
    public StartProcess(int executionBatchNumber,
                        int indexInBatch,
                        int numberOfProcessInstanceToStart,
                        Duration durationToCreateProcessInstances,
                        ScenarioStep scenarioStep,
                        RunScenario runScenario,
                        RunResult runResult) {
      this.executionBatchNumber = executionBatchNumber;
      this.indexInBatch = indexInBatch;
      this.durationToCreateProcessInstances = durationToCreateProcessInstances;
      this.numberOfProcessInstanceToStart = numberOfProcessInstanceToStart;
      this.runResult = runResult;
      this.runScenario = runScenario;
      this.scenarioStep = scenarioStep;
    }

    /**
     * This thread will create numberOfProcessInstanceToStart, but it monitor the time, and if the time is over
     * the Duration, it stop
     */
    @Override
    public void run() {
      isRunning=true;
      boolean alreadyLoggedError = false;
      isOverload = false;
      long begin = System.currentTimeMillis();
      for (int i = 0; i < numberOfProcessInstanceToStart; i++) {

        // operation
        try {
          Map<String, Object> variables = RunZeebeOperation.getVariablesStep(runScenario, scenarioStep, indexInBatch);
          String processInstance = runScenario.getBpmnEngine()
              .createProcessInstance(scenarioStep.getProcessId(), scenarioStep.getTaskId(), // activityId
                  variables);

          if (runScenario.getRunParameters().showLevelDebug())
            logger.info("batch_#{} Create ProcessInstance:{} Variables {}", executionBatchNumber, processInstance,
                variables);

          if (listProcessInstances.size() < 21)
            listProcessInstances.add(processInstance);
          nbCreation++;
          runResult.registerAddProcessInstance(scenarioStep.getProcessId(), true);

        } catch (AutomatorException e) {
          if (!alreadyLoggedError)
            runResult.addError(scenarioStep,
                "batch_#" + executionBatchNumber + "-" + scenarioStep.getId() + " Error at creation: [" + e.getMessage()
                    + "]");
          alreadyLoggedError = true;
          nbFailed++;
          runResult.registerAddProcessInstance(scenarioStep.getProcessId(), false);
        }
        // do we have to stop the execution?
        long currentTimeMillis = System.currentTimeMillis();
        Duration durationCurrent = durationToCreateProcessInstances.minusMillis(currentTimeMillis - begin);
        if (durationCurrent.isNegative()) {
          // log only at the debug mode (thread per thread), in monitoring log only at batch level
          if (runScenario.getRunParameters().showLevelDebug()) {
            // take too long to create the required process instance, so stop now.
            logger.info("batch_#{} {} Over the duration. Created {} when expected {} in {} ms",
                executionBatchNumber,
                scenarioStep.getId(),
                nbCreation,
                numberOfProcessInstanceToStart, currentTimeMillis - begin);
          }
          isOverload = true;
          break;
        }
      }

      isRunning=false;
    }
    public boolean isRunning() {
      return isRunning;

    }
  }
}
