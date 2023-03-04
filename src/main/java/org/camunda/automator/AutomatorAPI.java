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
   * @See scenario class to create from scratch a scenario
   */
  public Scenario createScenario() {
    return new Scenario();
  }

  /**
   * Load the scenario from a file
   *
   * @param scenarioFile file to read the scenario
   * @return the scenario
   * @throws Exception
   */
  public Scenario loadFromFile(File scenarioFile) throws Exception {
    return Scenario.createFromFile(scenarioFile);
  }

  /**
   * Execute a scenario
   *
   * @param engineConfiguration the configuration to connect the Camunda engine
   * @param scenario            the scenario to execute
   */
  public RunResult executeScenario(BpmnEngineConfiguration engineConfiguration,
                                   RunParameters runParameters,
                                   Scenario scenario) {
    RunScenario runScenario = null;
    try {
      BpmnEngine bpmnEngine = BpmnEngineFactory.getInstance().getEngineFromConfiguration(engineConfiguration);
      runScenario = new RunScenario(scenario, bpmnEngine, runParameters, serviceAccess);
    } catch (Exception e) {
      RunResult result = new RunResult(runScenario);
      result.addError(null, "Initialization error");
      return result;
    }

    engineConfiguration.logDebug = runParameters.logLevel == RunParameters.LOGLEVEL.DEBUG;
    return runScenario.runScenario();

  }
}
