package org.camunda.automator.engine;

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.definition.ScnExecution;
import org.camunda.automator.definition.ScnHead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This can't be a Component, to be used in AutomatorAPI
 * <p>
 * The scenario execute different steps, and report the result.
 */
public class ScnRunHead {
  private final ScnHead scenario;
  Logger logger = LoggerFactory.getLogger(ScnRunHead.class);

  /**
   * @param scenario scenario to be executed
   */
  public ScnRunHead(ScnHead scenario) {
    this.scenario = scenario;
  }

  /**
   * Execute the scenario.
   * Note: this method is multi thread safe.
   *
   * @param bpmnEngine    engine to connect
   * @param runParameters different parameters to run the scenation
   * @return the execution
   */
  public ScnRunResult runScenario(BpmnEngine bpmnEngine, RunParameters runParameters) {
    ScnRunResult result = new ScnRunResult(scenario, runParameters);

    // each execution is run in a different thread
    ExecutorService executor = Executors.newFixedThreadPool(runParameters.getNumberOfThreadsPerScenario());

    List<Future<?>> listFutures = new ArrayList<>();

    for (int i = 0; i < scenario.getExecutions().size(); i++) {
      ScnExecution scnExecution = scenario.getExecutions().get(i);
      ScnExecutionCallable scnExecutionCallable = new ScnExecutionCallable("Agent-" + i, scnExecution, bpmnEngine,
          runParameters);

      listFutures.add(executor.submit(scnExecutionCallable));
    }

    // wait the end of all executions
    try {
      for (Future<?> f : listFutures) {
        Object scnRunResult = f.get();
        result.add((ScnRunResult)scnRunResult);
      }

    } catch (ExecutionException ee) {
      result.addError(null, "Error during executing in parallel " + ee.getMessage());

    } catch (Exception e) {
      result.addError(null, "Error during executing in parallel " + e.getMessage());
    }

    return result;
  }

  private static class ScnExecutionCallable implements Callable {
    private final String agentName;
    private final ScnExecution scnExecution;
    private final BpmnEngine bpmnEngine;
    private final RunParameters runParameters;

    private ScnRunResult scnRunResult;

    ScnExecutionCallable(String agentName,
                         ScnExecution scnExecution,
                         BpmnEngine bpmnEngine,
                         RunParameters runParameters) {
      this.agentName = agentName;
      this.scnExecution = scnExecution;
      this.bpmnEngine = bpmnEngine;
      this.runParameters = runParameters;
    }

    @Override
    public Object call() {
      ScnRunExecution scnRunExecution = new ScnRunExecution();
      scnRunExecution.setAgentName(agentName);
      scnRunResult = scnRunExecution.runExecution(scnExecution, bpmnEngine, runParameters);
      return scnRunResult;
    }

    public ScnRunResult getScnRunResult() {
      return scnRunResult;
    }
  }

}
