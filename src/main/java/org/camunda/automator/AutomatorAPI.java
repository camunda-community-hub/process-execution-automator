/* ******************************************************************** */
/*                                                                      */
/*  AutomatorAPI                                                    */
/*                                                                      */
/*  To use the Automator as an API                                      */
/* ******************************************************************** */
package org.camunda.automator;

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.bpmnengine.BpmnEngineFactory;
import org.camunda.automator.configuration.BpmnEngineList;
import org.camunda.automator.definition.Scenario;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunParameters;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenarioService;
import org.camunda.automator.services.ServiceAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Path;

@Component
public class AutomatorAPI {
    static Logger logger = LoggerFactory.getLogger(AutomatorAPI.class);

    @Autowired
    ServiceAccess serviceAccess;
    @Autowired
    private RunScenarioService runScenarioService;

    /**
     * Create an empty scenario.
     * The scenario can be created from scratch by the caller
     *
     * @return the scenario
     * see scenario class to create from scratch a scenario
     */
    public Scenario createScenario() {
        return new Scenario();
    }

    /**
     * Load the scenario from a file
     *
     * @param scenarioFile file to read the scenario
     * @return the scenario
     * @throws AutomatorException if scenario can't be read
     */
    public Scenario loadFromFile(Path scenarioFile) throws AutomatorException {
        return Scenario.createFromFile(scenarioFile);
    }

    /**
     * Create from an input Stream.
     *
     * @param scenarioInputStream inputStream
     * @param origin              origin of inputStream
     * @return scenario
     * @throws AutomatorException if scenario can't be read
     */
    public Scenario loadFromInputStream(InputStream scenarioInputStream, String origin) throws AutomatorException {
        return Scenario.createFromInputStream(scenarioInputStream, origin);
    }

    /**
     * Search the engine from the scenario
     *
     * @param scenario       scenario
     * @param bpmnEngineList different engine configuration
     * @return the engine, null if no engine exist, an exception if the connection is not possible
     */
    public BpmnEngine getBpmnEngineFromScenario(Scenario scenario, BpmnEngineList bpmnEngineList)
            throws AutomatorException {
        try {

            if (scenario.getServerName() != null) {
                return getBpmnEngine(bpmnEngineList.getByServerName(scenario.getServerName()), true);
            }

            return null;
        } catch (AutomatorException e) {
            logger.error("Can't connect the engine for the scenario [{}] serverName[{}]: {}", scenario.getName(),
                    scenario.getServerName(), e.getMessage(), e);
            throw e;
        }

    }

    /**
     * Execute a scenario
     *
     * @param bpmnEngine    Access the Camunda engine. if null, then the value in the scenario are used
     * @param runParameters parameters use to run the scenario
     * @param scenario      the scenario to execute
     */
    public RunResult executeScenario(BpmnEngine bpmnEngine, RunParameters runParameters, Scenario scenario) {
        return runScenarioService.executeScenario(bpmnEngine, runParameters, scenario, false);
    }


    /* ******************************************************************** */
    /*                                                                      */
    /*  Additional tool                                                    */
    /*                                                                      */
    /*  Deploy Process                                                      */
    /*  Deploy a process in the server                                      */
    /* ******************************************************************** */

    public BpmnEngine getBpmnEngine(BpmnEngineList.BpmnServerDefinition serverDefinition, boolean logDebug)
            throws AutomatorException {
        return BpmnEngineFactory.getInstance().getEngineFromConfiguration(serverDefinition, logDebug);
    }

    /**
     * Deploy a process, bpmEngine is given by the caller
     *
     * @param bpmnEngine    Engine to deploy
     * @param runParameters parameters used to deploy the version
     * @param scenario      scenario
     * @return the result object
     */
    public RunResult deployProcess(BpmnEngine bpmnEngine, RunParameters runParameters, Scenario scenario) {
        return runScenarioService.executeDeployment(bpmnEngine, runParameters, scenario);
    }
}
