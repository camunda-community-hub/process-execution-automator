/* ******************************************************************** */
/*                                                                      */
/*  AutomatorAPI                                                    */
/*                                                                      */
/*  To use the Automator as an API                                      */
/* ******************************************************************** */
package org.camunda.automator;

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.bpmnengine.BpmnEngineFactory;
import org.camunda.automator.configuration.ConfigurationBpmEngine;
import org.camunda.automator.definition.Scenario;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunParameters;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenario;
import org.camunda.automator.services.ServiceAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class AutomatorAPI {
  static Logger logger = LoggerFactory.getLogger(AutomatorAPI.class);

  @Autowired
  ServiceAccess serviceAccess;

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
  public Scenario loadFromFile(File scenarioFile) throws AutomatorException {
    return Scenario.createFromFile(scenarioFile);
  }

  /**
   * Search the engine from the scenario
   *
   * @param scenario            scenario
   * @param engineConfiguration different engine configuration
   * @return the engine, null if no engine exist, an exception if the connection is not possible
   */
  public BpmnEngine getBpmnEngineFromScenario(Scenario scenario, ConfigurationBpmEngine engineConfiguration)
      throws AutomatorException {
    try {

      if (scenario.getServerName() != null) {
        return getBpmnEngine(engineConfiguration, engineConfiguration.getByServerName(scenario.getServerName()));
      }
      if (scenario.getServerType() != null) {
        return getBpmnEngine(engineConfiguration, engineConfiguration.getByServerType(scenario.getServerType()));
      }
      return null;
    } catch (AutomatorException e) {
      logger.error("Can't connect the engine for the scenario [{}] serverName[{}] serverType[{}] : {}",
          scenario.getName(), scenario.getServerName(), scenario.getServerType(), e.getMessage());
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
    RunScenario runScenario = null;

    try {
      runScenario = new RunScenario(scenario, bpmnEngine, runParameters, serviceAccess);
    } catch (Exception e) {
      RunResult result = new RunResult(runScenario);
      result.addError(null, "Initialization error");
      return result;
    }

    RunResult runResult = new RunResult(runScenario);
    runResult.add(runScenario.runScenario());

    return runResult;
  }


  /* ******************************************************************** */
  /*                                                                      */
  /*  Additional tool                                                    */
  /*                                                                      */
  /*  Deploy Process                                                      */
  /*  Deploy a process in the server                                      */
  /* ******************************************************************** */

  public BpmnEngine getBpmnEngine(ConfigurationBpmEngine engineConfiguration,
                                  ConfigurationBpmEngine.BpmnServerDefinition serverDefinition)
      throws AutomatorException {

    return BpmnEngineFactory.getInstance().getEngineFromConfiguration(engineConfiguration, serverDefinition);
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
    RunScenario runScenario = null;
    try {
      long begin = System.currentTimeMillis();
      runScenario = new RunScenario(scenario, bpmnEngine, runParameters, serviceAccess);
      RunResult runResult = new RunResult(runScenario);
      runResult.add(runScenario.runDeployment());
      runResult.addTimeExecution(System.currentTimeMillis() - begin);
      return runResult;
    } catch (Exception e) {
      RunResult result = new RunResult(runScenario);
      result.addError(null, "Process deployment error error " + e.getMessage());
      return result;
    }

  }
}
