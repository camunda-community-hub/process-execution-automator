package org.camunda.automator.engine;

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.configuration.ConfigurationBpmEngine;
import org.camunda.automator.definition.Scenario;
import org.camunda.automator.definition.ScenarioDeployment;
import org.camunda.automator.definition.ScenarioExecution;
import org.camunda.automator.definition.ScenarioTool;
import org.camunda.automator.engine.flow.RunScenarioFlows;
import org.camunda.automator.services.ServiceAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This object executes a scenario, in a context. Context is
 * - the scenario to execute
 * - the BPMN Engine to access
 * - the RunParameters
 */
public class RunScenario {
  private final Scenario scenario;
  private final BpmnEngine bpmnEngine;
  private final RunParameters runParameters;
  private final ServiceAccess serviceAccess;
  Logger logger = LoggerFactory.getLogger(RunScenario.class);

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
   * A scenario is composed of
   * - deployment
   * - execution (which contains the verifications)
   * <p>
   * these steps are controlled by the runParameters
   *
   * @return tue result object
   */
  public RunResult runScenario() {
    RunResult result = new RunResult(this);
    if (runParameters.deploymentProcess)
      result.add(runDeployment());
    // verification is inside execution
    result.add(runExecutions());
    return result;
  }

  /**
   * run only the deployments on the process - test to verify the engine is performed
   *
   * @return result of deployment
   */
  public RunResult runDeployment() {
    RunResult result = new RunResult(this);

    // first, do we have to deploy something?
    if (scenario.getDeployments() != null) {
      for (ScenarioDeployment deployment : scenario.getDeployments()) {
        boolean sameTypeServer = false;
        if (deployment.serverType.equals(ConfigurationBpmEngine.CamundaEngine.CAMUNDA_7)) {
          sameTypeServer = bpmnEngine.getTypeCamundaEngine().equals(ConfigurationBpmEngine.CamundaEngine.CAMUNDA_7);
        } else if (deployment.serverType.equals(ConfigurationBpmEngine.CamundaEngine.CAMUNDA_8)) {
          sameTypeServer = bpmnEngine.getTypeCamundaEngine().equals(ConfigurationBpmEngine.CamundaEngine.CAMUNDA_8)
              || bpmnEngine.getTypeCamundaEngine().equals(ConfigurationBpmEngine.CamundaEngine.CAMUNDA_8_SAAS);
        }
        if (sameTypeServer) {
          try {
            long begin = System.currentTimeMillis();
            File processFile = ScenarioTool.loadFile(deployment.processFile, this);
            logger.info("Deploy process[{}] on {}", processFile.getName(), bpmnEngine.getSignature());
            result.addDeploymentProcessId(bpmnEngine.deployBpmn(processFile, deployment.policy));
            result.addTimeExecution(System.currentTimeMillis() - begin);
          } catch (AutomatorException e) {
            result.addError(null, "Can't deploy process [" + deployment.processFile + "] " + e.getMessage());
          }
        }
      }
    }
    return result;
  }

  /**
   * Execute the scenario.
   * Note: this method is multi thread safe.
   * Note: if the execution has verification AND runParameters.execution == true, then the verification is started
   *
   * @return the execution
   */
  public RunResult runExecutions() {
    RunResult result = new RunResult(this);
    // each execution is run in a different thread
    ExecutorService executor = Executors.newFixedThreadPool(runParameters.getNumberOfThreadsPerScenario());

    // the scenario can be an Execution or a Flow
    if (!scenario.getExecutions().isEmpty()) {
      List<Future<?>> listFutures = new ArrayList<>();

      for (int i = 0; i < scenario.getExecutions().size(); i++) {
        ScenarioExecution scnExecution = scenario.getExecutions().get(i);
        ScnExecutionCallable scnExecutionCallable = new ScnExecutionCallable("Agent-" + i, this, scnExecution,
            runParameters);

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
    }
    if (!scenario.getFlows().isEmpty()) {
      RunScenarioFlows scenarioFlows = new RunScenarioFlows(serviceAccess, this);
      scenarioFlows.execute(result);
    }

    return result;
  }

  /**
   * for one execution, run verifications
   *
   * @param scnExecution execution to check
   * @return result of execution
   */
  public RunResult runVerifications(ScenarioExecution scnExecution) {
    RunResult result = new RunResult(this);

    RunScenarioVerification verifications = new RunScenarioVerification(scnExecution);
    result.add(verifications.runVerifications(this, result.getFirstProcessInstanceId()));
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
    private final RunParameters runParameters;

    private RunResult scnRunResult;

    ScnExecutionCallable(String agentName,
                         RunScenario runScenario,
                         ScenarioExecution scnExecution,
                         RunParameters runParameters) {
      this.agentName = agentName;
      this.runScenario = runScenario;
      this.scnExecution = scnExecution;
      this.runParameters = runParameters;
    }

    @Override
    public Object call() {
      RunScenarioExecution scnRunExecution = new RunScenarioExecution(runScenario, scnExecution);
      scnRunExecution.setAgentName(agentName);

      /**
       * Execution AND verifications are processed
       * An execution may be MULTIPLE process instance, and each must be verified
       */
      scnRunResult = scnRunExecution.runExecution();

      return scnRunResult;
    }

    public RunResult getScnRunResult() {
      return scnRunResult;
    }
  }

}
