package org.camunda.automator.engine;

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.definition.Scenario;
import org.camunda.automator.definition.ScenarioExecution;
import org.camunda.automator.services.ServiceAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This execute a scenario, in a context. Context is
 * - the scenario to execute
 * - the BPMN Engine to access
 * - the RunParameters
 */
public class RunScenario {
  Logger logger = LoggerFactory.getLogger(RunScenario.class);

  private final Scenario scenario;
  private final BpmnEngine bpmnEngine;
  private final RunParameters runParameters;

  private final ServiceAccess serviceAccess;

  /**
   * @param scenario      scenario to be executed
   * @param bpmnEngine    engine to connect
   * @param runParameters different parameters to run the scenario
   * @param serviceAccess service access to access all services, this object is created per execution
   */
  public RunScenario(Scenario scenario,
                     BpmnEngine bpmnEngine,
                     RunParameters runParameters,
                     ServiceAccess serviceAccess) {
    this.scenario = scenario;
    this.bpmnEngine = bpmnEngine;
    this.runParameters = runParameters;
    this.serviceAccess = serviceAccess;
  }

  /**
   * Execute the scenario.
   * Note: this method is multi thread safe.
   *
   * @return the execution
   */
  public RunResult runScenario() {
    RunResult result = new RunResult(this);

    // each execution is run in a different thread
    ExecutorService executor = Executors.newFixedThreadPool(runParameters.getNumberOfThreadsPerScenario());

    List<Future<?>> listFutures = new ArrayList<>();

    for (int i = 0; i < scenario.getExecutions().size(); i++) {
      ScenarioExecution scnExecution = scenario.getExecutions().get(i);
      // the execution does not want to be executed
      if (!scnExecution.isExecution())
        continue;
      ScnExecutionCallable scnExecutionCallable = new ScnExecutionCallable("Agent-" + i, this, scnExecution);

      listFutures.add(executor.submit(scnExecutionCallable));
    }

    // wait the end of all executions
    try {
      for (Future<?> f : listFutures) {
        Object scnRunResult = f.get();
        result.add((RunResult) scnRunResult);
      }

    } catch (ExecutionException ee) {
      result.addError(null, "Error during executing in parallel " + ee.getMessage());

    } catch (Exception e) {
      result.addError(null, "Error during executing in parallel " + e.getMessage());
    }

    return result;
  }

  public Scenario getScenario() {
    return scenario;
  }

  public BpmnEngine getBpmnEngine() {
    return bpmnEngine;
  }

  public RunParameters getRunParameters() {
    return runParameters;
  }

  public ServiceAccess getServiceAccess() {
    return serviceAccess;
  }




  /* ******************************************************************** */
  /*                                                                      */
  /*  Callable class                                                      */
  /*                                                                      */
  /*  Each execution are executed in different thread                     */
  /* ******************************************************************** */

  private static class ScnExecutionCallable implements Callable {
    private final String agentName;
    private final ScenarioExecution scnExecution;
    private final RunScenario runScenario;

    private RunResult scnRunResult;

    ScnExecutionCallable(String agentName,
                         RunScenario runScenario,
                         ScenarioExecution scnExecution) {
      this.agentName = agentName;
      this.runScenario = runScenario;
      this.scnExecution = scnExecution;
    }

    @Override
    public Object call() {
      RunScenarioExecution scnRunExecution = new RunScenarioExecution(runScenario,scnExecution);
      scnRunExecution.setAgentName(agentName);
      scnRunResult = scnRunExecution.runExecution();
      return scnRunResult;
    }

    public RunResult getScnRunResult() {
      return scnRunResult;
    }
  }

}
