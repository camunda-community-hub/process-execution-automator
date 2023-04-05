/* ******************************************************************** */
/*                                                                      */
/*  AutomatorAPI                                                    */
/*                                                                      */
/*  To use the Automator as an API                                      */
/* ******************************************************************** */
package org.camunda.automator;

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.bpmnengine.BpmnEngineConfiguration;
import org.camunda.automator.bpmnengine.BpmnEngineFactory;
import org.camunda.automator.definition.Scenario;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunParameters;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenario;
import org.camunda.automator.services.ServiceAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class AutomatorAPI {

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
   * Execute a scenario
   *
   * @param bpmnEngine Access the Camunda engine
   * @param runParameters       parameters use to run the scenario
   * @param scenario            the scenario to execute
   */
  public RunResult executeScenario(BpmnEngine  bpmnEngine,
                                   RunParameters runParameters,
                                   Scenario scenario) {
    RunScenario runScenario = null;
    try {
      runScenario = new RunScenario(scenario, bpmnEngine, runParameters, serviceAccess);
    } catch (Exception e) {
      RunResult result = new RunResult(runScenario);
      result.addError(null, "Initialization error");
      return result;
    }

    RunResult runResult = new RunResult(runScenario);
    runResult.add( runScenario.runScenario());

    return runResult;
  }


  /* ******************************************************************** */
  /*                                                                      */
  /*  Additional tool                                                    */
  /*                                                                      */
  /*  Deploy Process                                                      */
  /*  Deploy a process in the server                                      */
  /* ******************************************************************** */

  public BpmnEngine getBpmnEngine(BpmnEngineConfiguration engineConfiguration,
                                 BpmnEngineConfiguration.BpmnServerDefinition serverDefinition) {
    try {
      return BpmnEngineFactory.getInstance()
          .getEngineFromConfiguration(engineConfiguration, serverDefinition);

    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Deploy a process, bpmEngine is given by the caller
   * @param bpmnEngine Engine to deploy
   * @param scenario scenario
   * @return the result object
   */
  public RunResult deployProcess(BpmnEngine bpmnEngine,
                                 Scenario scenario) {
    RunScenario runScenario = null;
    try {
      long begin = System.currentTimeMillis();
      runScenario = new RunScenario(scenario, bpmnEngine, null, serviceAccess);
      RunResult runResult = new RunResult(runScenario);
      runResult.add(runScenario.runDeployment());
      runResult.addTimeExecution(System.currentTimeMillis() - begin);
      return runResult;
    } catch (Exception e) {
      RunResult result = new RunResult(runScenario);
      result.addError(null, "Process deployment error error "+e.getMessage());
      return result;
    }

  }
}
