package org.camunda.automator.engine;

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.configuration.BpmnEngineList;
import org.camunda.automator.definition.Scenario;
import org.camunda.automator.definition.ScenarioDeployment;
import org.camunda.automator.definition.ScenarioExecution;
import org.camunda.automator.definition.ScenarioTool;
import org.camunda.automator.engine.flow.RunScenarioFlows;
import org.camunda.automator.engine.unit.RunScenarioUnit;
import org.camunda.automator.engine.unit.RunScenarioVerification;
import org.camunda.automator.services.ServiceAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

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
     * @param executionId executionId provided by the caller
     * @param runResult if null, then a new one is created. Else this one is fulfill
     * @return the result object
     */
    public RunResult executeTheScenario(String executionId, RunResult runResult) {
        if (runResult==null)
            runResult = new RunResult(this, executionId);

        // control
        if (scenario.typeScenario == null) {
            runResult.addError(null, "TypeScenario undefined");
        }
        // ------------ unit scenario
        if (scenario.typeScenario.equals(Scenario.TYPESCENARIO.UNIT)) {
            if (scenario.getExecutions() == null || scenario.getExecutions().isEmpty()) {
                runResult.addError(null, "TypeScenario[" + Scenario.TYPESCENARIO.UNIT + "] must have a list of [executions]");
                return runResult;
            }
            // force information in execution
            for (ScenarioExecution execution : scenario.getExecutions()) {
                execution.setNumberProcessInstances(1);
                execution.setNumberOfThreads(1);
            }
            // Verification must be moved to true
            runParameters.setVerification(true);


            // ------------- flow scenario
        } else if (scenario.typeScenario.equals(Scenario.TYPESCENARIO.FLOW)) {
            if (scenario.getFlowControl() == null)
                runResult.addError(null, "TypeScenario[" + Scenario.TYPESCENARIO.FLOW + "] must have a list of [flowControl]");
            if (scenario.getFlows() == null || scenario.getFlows().isEmpty())
                runResult.addError(null, "TypeScenario[" + Scenario.TYPESCENARIO.FLOW + "] must have a list of [flows]");
        }
        if (runResult.hasErrors())
            return runResult;

        logger.info("RunScenario: ------ Deployment ({})", runParameters.isDeploymentProcess());
        if (runParameters.isDeploymentProcess())
            runResult.merge(executeDeployment(executionId));
        logger.info("RunScenario: ------ End deployment ");

        // verification is inside execution
        runExecutions(executionId, runResult);
        return runResult;
    }

    /**
     * run only the deployments on the process - test to verify the engine is performed
     *
     * @return result of deployment
     */
    protected RunResult executeDeployment(String executionId) {
        RunResult result = new RunResult(this, executionId);

        // first, do we have to deploy something?
        if (scenario.getDeployments() != null) {
            for (ScenarioDeployment deployment : scenario.getDeployments()) {

                boolean sameTypeServer = false;
                if (deployment.serverType.equals(BpmnEngineList.CamundaEngine.CAMUNDA_7)) {
                    sameTypeServer = bpmnEngine.getTypeCamundaEngine().equals(BpmnEngineList.CamundaEngine.CAMUNDA_7);
                } else if (deployment.serverType.equals(BpmnEngineList.CamundaEngine.CAMUNDA_8)) {
                    sameTypeServer = bpmnEngine.getTypeCamundaEngine().equals(BpmnEngineList.CamundaEngine.CAMUNDA_8)
                            || bpmnEngine.getTypeCamundaEngine().equals(BpmnEngineList.CamundaEngine.CAMUNDA_8_SAAS);
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
                } else {
                    logger.info("RunScenario: can't Deploy ({}), not the same server", deployment.processFile);

                }
            }
        }
        return result;
    }

    /**
     * Execute the scenario.
     * Note: this method is multi-thread safe.
     * Note: if the execution has verification AND runParameters.execution == true, then the verification is started
     *
     * @param runResult give the runTesult to be completed
     * @return the execution
     */
    public RunResult runExecutions(String executionId,RunResult runResult) {

        runResult.setStartDate(new Date());

        // the scenario can be an Execution or a Flow
        if (scenario.typeScenario.equals(Scenario.TYPESCENARIO.UNIT)) {

            // each execution is run in a different thread
            ExecutorService executor = Executors.newFixedThreadPool(scenario.getExecutions().size());

            List<Future<?>> listFutures = new ArrayList<>();
            logger.info("RunScenario: ------ execution UNIT scenario [{}] {} execution on {} Threads", scenario.getName(),
                    scenario.getExecutions().size(), runParameters.getNumberOfThreadsPerScenario());

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
                    // We want to keep separate all results, in case of a Unit Test
                    runResult.add((RunResult) scnRunResult);
                }

            } catch (ExecutionException ee) {
                runResult.addError(null, "Error during executing in parallel " + ee.getMessage());

            } catch (Exception e) {
                runResult.addError(null, "Error during executing in parallel " + e.getMessage());
            }
            logger.info("RunScenario: ------ End execution");
            runResult.setEndDate(new Date());

        }
        if (scenario.typeScenario.equals(Scenario.TYPESCENARIO.FLOW)) {
            logger.info("RunScenario: ------ execution FLOW scenario [{}]", scenario.getName());
            RunScenarioFlows scenarioFlows = new RunScenarioFlows(serviceAccess, this);
            scenarioFlows.execute(runResult);
            logger.info("RunScenario: ------ End execution");
            runResult.setEndDate(new Date());
        }

        return runResult;
    }

    /**
     * for one execution, run verifications
     *
     * @param scnExecution execution to check
     * @return result of execution
     */
    public RunResult runVerifications(ScenarioExecution scnExecution, String executionId) {
        RunResult result = new RunResult(this, executionId);

        RunScenarioVerification verifications = new RunScenarioVerification(scnExecution);
        result.merge(verifications.runVerifications(this, result.getFirstProcessInstanceId()));
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
            RunScenarioUnit scnRunExecution = new RunScenarioUnit(runScenario, scnExecution);
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
