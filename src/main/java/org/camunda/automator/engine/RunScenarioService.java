package org.camunda.automator.engine;

/**
 * This class saved and manage all running scenario.
 * <p>
 * Running a scenario consist in two main steps
 * - environment: load the scenario, connect the correct BPMNEngine
 * - execute
 * <p>
 * When a scenario need to be start, the service
 * - search and load the scenario
 * - find the correct BPMNEngine, using the configurationBpmnEngineList
 * - deploy processing, according to the scenario
 * - create a runScenario, providing scenario and BpmnEngine
 * - return a RunResult
 * <p>
 * The interface for this class is the RunResult. RunResult reference the RunScenario, but is larger: it contains information about searching/loading scenario, connected BpmnEngine
 */

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.bpmnengine.BpmnEngineFactory;
import org.camunda.automator.configuration.ConfigurationBpmnEngineList;
import org.camunda.automator.content.ContentManager;
import org.camunda.automator.definition.Scenario;
import org.camunda.automator.services.ServiceAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class RunScenarioService {
    private static final Logger logger = LoggerFactory.getLogger(RunScenarioService.class.getName());
    private final ServiceAccess serviceAccess;
    private final ContentManager contentManager;
    private final BpmnEngineFactory bpmnEngineFactory;
    private final ConfigurationBpmnEngineList configurationBpmnEngineList;
    Map<String, RunResult> cacheRunScenario = new HashMap<>();

    public RunScenarioService(ServiceAccess serviceAccess,
                              ContentManager contentManager,
                              BpmnEngineFactory bpmnEngineFactory,
                              ConfigurationBpmnEngineList configurationBpmnEngineList) {
        this.serviceAccess = serviceAccess;
        this.contentManager = contentManager;
        this.bpmnEngineFactory = bpmnEngineFactory;
        this.configurationBpmnEngineList = configurationBpmnEngineList;
    }


    /* ******************************************************************** */
    /*                                                                      */
    /*  Start execution                                                     */
    /*                                                                      */
    /*  All workers extends this class. It gives tool to access parameters, */
    /*  and the contract implementation on parameters                       */
    /* ******************************************************************** */

    /**
     * A new execution is started. scenarioName or scenario must be provided
     *
     * @return RunScenario
     */
    public RunResult startScenario(String scenarioName, Scenario scenario, RunParameters runParameters, boolean asynchronous) {
        String executionId = createExecutionId(scenarioName != null ? scenarioName : scenario.getName());

        RunResult runResult = new RunResult(scenarioName != null ? scenarioName : scenario.getName(), executionId);
        cacheRunScenario.put(executionId, runResult);

        //---------------- Load the scenario
        if (scenario == null) {
            try {
                scenario = contentManager.getFromName(scenarioName);
            } catch (AutomatorException ae) {
                logger.error("Error during accessing InputStream from scenarioName [{}]: {}", scenarioName,
                        ae.getMessage(), ae);
                runResult.addError("Can't access scenario [" + scenarioName + "]");
                runResult.setStatus(RunResult.StatusTest.SCENARIO_NOT_EXIST);
                return runResult;
            }
        }

        //---------------- get the BpmnEngine
        BpmnEngine bpmnEngine;
        try {
            bpmnEngine = connectToEngine(scenario, runParameters);
            if (bpmnEngine == null) {
                logger.error("Scenario [{}] Server [{}] No BPM ENGINE running.", scenario.getName(),
                        runParameters.getServerName());
                runResult.addError("Can't access BpmnEgnine[" + runParameters.getServerName() + "]");
                runResult.setStatusTest(RunResult.StatusTest.ENGINE_NOT_EXIST);
                return runResult;
            }
            bpmnEngine.turnHighFlowMode(false);
        } catch (AutomatorException e) {
            runResult.setStatusTest(RunResult.StatusTest.ENGINE_NOT_EXIST);
            runResult.addError(e.getMessage());
            return runResult;
        }

        // -------------- create the runScenario
        RunScenario runScenario = new RunScenario(scenario, serviceAccess, bpmnEngine, runParameters);
        runResult.setRunScenario(runScenario);

        // ---- execute the scenario now
        logger.info("StartTest: Scenario [{}] use BpmnEngine [{}] : {}",
                scenario.getName(),
                runParameters.getServerName(),
                runResult.getRunScenario().getBpmnEngine().getSignature());

        if (asynchronous) {
            // so the tread use the executionId to fulfill the result
            Thread thread = new Thread(() -> executeScenarioInternal(runScenario, runResult));
            thread.start();
        } else {
            executeScenarioInternal(runScenario, runResult);
        }
        return runResult;
    }

    /**
     * @param scenario scenario to start
     * @param runParameters parameters to run the scenario
     * @param bpmnEngine engine to run
     * @return RunResult information
     */
    public RunResult startScenario(Scenario scenario, RunParameters runParameters, BpmnEngine bpmnEngine) {
        String executionId = createExecutionId(scenario.getName());

        RunResult runResult = new RunResult(scenario.getName(), executionId);

        // Now, we create a RunScenario.
        cacheRunScenario.put(executionId, runResult);

        // -------------- create the runScenario
        RunScenario runScenario = new RunScenario(scenario, serviceAccess, bpmnEngine, runParameters);
        runResult.setRunScenario(runScenario);

        executeScenarioInternal(runScenario, runResult);
        return runResult;

    }

    private String createExecutionId(String scenarioName) {
        return System.currentTimeMillis() + "." + scenarioName;
    }

/*
    private void executeScenario(final RunResult runResult,  boolean asynchronous) {
        if (asynchronous) {
            // Create now he executionId
            runResult.setStartDate(new Date());

            // so the tread use the executionId to fulfill the result
            Thread thread = new Thread(() -> executeScenarioInternal(runResult.getRunScenario().getBpmnEngine(),
                    runResult.getRunScenario().getRunParameters(),
                    runResult.getRunScenario().getScenario(),
                    runResult.getExecutionId()));
            thread.start();
            // Create an arbiratry runResult here. What is important is to return the executionId
        } else {
            executeScenarioInternal(runResult.getRunScenario().getBpmnEngine(),
                    runResult.getRunScenario().getRunParameters(),
                    runResult.getRunScenario().getScenario(),
                    runResult.getExecutionId());
        }
    }
*/


    /**
     * Execute a test
     *
     * @return the result
     */
    private void executeScenarioInternal(final RunScenario runScenario, final RunResult runResult) {
        runResult.setStartDate(new Date());
        runScenario.executeTheScenario(runResult);
    }


    public RunResult deployProcess(Scenario scenario, RunParameters runParameters) {

        String executionId = createExecutionId(scenario.getName());
        RunResult runResult = new RunResult(scenario.getName(), executionId);
        cacheRunScenario.put(executionId, runResult);
        BpmnEngine bpmnEngine;
        try {
            bpmnEngine = getBpmnEngineFromScenario(scenario);
        } catch (AutomatorException e) {
            runResult.setStatusTest(RunResult.StatusTest.ENGINE_NOT_EXIST);
            runResult.addError(e.getMessage());
            return runResult;
        }
        RunScenario runScenario = new RunScenario(scenario, serviceAccess, bpmnEngine, runParameters);
        runResult.setRunScenario(runScenario);

        runScenario.executeDeployment(runResult);

        return runResult;
    }

    /* ******************************************************************** */
    /*                                                                      */
    /*  Access BPMNEngine                                                   */
    /*                                                                      */
    /*  All workers extends this class. It gives tool to access parameters, */
    /*  and the contract implementation on parameters                       */
    /* ******************************************************************** */

    public BpmnEngine getBpmnEngineFromScenario(Scenario scenario)
            throws AutomatorException {
        try {
            if (scenario.getServerName() != null) {
                return bpmnEngineFactory.getEngineFromConfiguration(configurationBpmnEngineList.getByServerName(scenario.getServerName()), false);
            }

            return null;
        } catch (AutomatorException e) {
            logger.error("Can't connect the engine for the scenario [{}] serverName[{}]: {}", scenario.getName(),
                    scenario.getServerName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Connect to the BPM Engine
     *
     * @param scenario      scenario to use to connect
     * @param runParameters running parameters
     * @return BPMN Engine
     */
    public BpmnEngine connectToEngine(Scenario scenario, RunParameters runParameters) throws AutomatorException {
        BpmnEngine bpmnEngine = null;
        boolean pleaseTryAgain;
        int countEngineIsNotReady = 0;
        String message = "";

        do {
            pleaseTryAgain = false;
            countEngineIsNotReady++;
            if (scenario.getServerName() != null && !scenario.getServerName().isEmpty()) {
                message += "ScenarioServerName[" + scenario.getServerName() + "];";
                bpmnEngine = getBpmnEngineFromScenario(scenario);
            } else {
                if (runParameters.getServerName() == null) {
                    throw new AutomatorException(RunResult.StatusTest.ENGINE_NOT_EXIST.toString(), "Engine [" + runParameters.getServerName() + "] does not exist in the list");
                }


                message += "ConfigurationServerName[" + runParameters.getServerName() + "];";
                ConfigurationBpmnEngineList.BpmnServerDefinition serverDefinition = configurationBpmnEngineList.getByServerName(
                        runParameters.getServerName());
                if (serverDefinition == null) {
                    throw new AutomatorException(RunResult.StatusTest.ENGINE_NOT_EXIST.toString(), "Engine [" + runParameters.getServerName() + "] does not exist in the list");
                }


                try {
                    if (runParameters.showLevelMonitoring()) {
                        logger.info("Run scenario with Server {}", serverDefinition.getSynthesis());
                    }
                    bpmnEngine = bpmnEngineFactory.getEngineFromConfiguration(serverDefinition, true);
                    if (runParameters.showLevelDashboard()) {
                        logger.info("Scenario [{}] Connect to BpmnEngine {}", scenario.getName(), message);
                    }

                    if (!bpmnEngine.isReady()) {
                        bpmnEngine.connection();
                    }
                } catch (AutomatorException e) {
                    pleaseTryAgain = true;
                    message += "EXCEPT " + e.getMessage();
                }
                if (pleaseTryAgain && countEngineIsNotReady < 5) {
                    logger.info(
                            "Scenario [{}] file[{}] No BPM ENGINE running [{}] tentative:{}/10. Sleep 30s. Scenario reference serverName[{}]",
                            message, countEngineIsNotReady, scenario.getName(), scenario.getName(), scenario.getServerName());
                    try {
                        logger.info("Sleep 10 s - wait the engine start");
                        Thread.sleep(((long) 1000) * 10);
                        logger.info("Wake up");
                    } catch (InterruptedException e) {
                        // nothing to do
                    }
                }
            }
        }
        while (pleaseTryAgain && countEngineIsNotReady < 10);
        return bpmnEngine;
    }

    public RunResult getFromExecutionId(String executionId) {
        return cacheRunScenario.get(executionId);
    }

    public Collection<RunResult> getRunResult() {
        return cacheRunScenario.values();
    }

    public RunResult getByExecutionId(String executionId) {
        return cacheRunScenario.get(executionId);
    }

    public void clearAll() {
        cacheRunScenario.clear();
    }
}