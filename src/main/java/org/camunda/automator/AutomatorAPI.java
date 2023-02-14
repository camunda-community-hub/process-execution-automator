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
import org.camunda.automator.definition.ScnHead;
import org.camunda.automator.engine.RunParameters;
import org.camunda.automator.engine.ScnRunHead;
import org.camunda.automator.engine.ScnRunResult;

import java.io.File;

public class AutomatorAPI {

  public static AutomatorAPI getInstance() {
    return new AutomatorAPI();
  }

  /**
   * Create an empty scenario.
   * The scenario can be created from scratch by the caller
   *
   * @return the scenario
   * @See scenario class to create from scratch a scenario
   */
  public ScnHead createScenario() {
    return new ScnHead();
  }

  /**
   * Load the scenario from a file
   *
   * @param scenarioFile file to read the scenario
   * @return the scenario
   * @throws Exception
   */
  public ScnHead loadFromFile(File scenarioFile) throws Exception {
    return ScnHead.createFromFile(scenarioFile);
  }

  /**
   * Execute a scenario
   *
   * @param engineConfiguration the configuration to connect the Camunda engine
   * @param scenario            the scenario to execute
   */
  public ScnRunResult executeScenario(BpmnEngineConfiguration engineConfiguration,
                                      RunParameters runParameters,
                                      ScnHead scenario) {
    ScnRunHead scenarioExecution = new ScnRunHead(scenario);

    try {
      BpmnEngine bpmnEngine = BpmnEngineFactory.getInstance().getEngineFromConfiguration(engineConfiguration);
      engineConfiguration.logDebug = runParameters.logLevel == RunParameters.LOGLEVEL.DEBUG;
      return scenarioExecution.runScenario(bpmnEngine, runParameters);
    } catch (Exception e) {
      ScnRunResult result = new ScnRunResult(scenario, runParameters);
      result.addError(null, "Initialization error");
      return result;
    }
  }
}
