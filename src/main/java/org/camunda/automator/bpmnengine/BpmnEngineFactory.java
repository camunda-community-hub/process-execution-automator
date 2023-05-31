/* ******************************************************************** */
/*                                                                      */
/*  BpmnEngineFactory                                                    */
/*                                                                      */
/*  Generate the client to the engine                                    */
/* ******************************************************************** */
package org.camunda.automator.bpmnengine;

import org.camunda.automator.bpmnengine.camunda7.BpmnEngineCamunda7;
import org.camunda.automator.bpmnengine.camunda8.BpmnEngineCamunda8;
import org.camunda.automator.bpmnengine.dummy.BpmnEngineDummy;
import org.camunda.automator.configuration.ConfigurationBpmEngine;
import org.camunda.automator.engine.AutomatorException;

/**
 * This can't be a Component, to be used in AutomatorAPI
 */
public class BpmnEngineFactory {

  public static BpmnEngineFactory getInstance() {
    return new BpmnEngineFactory();
  }

  public BpmnEngine getEngineFromConfiguration(ConfigurationBpmEngine engineConfiguration,
                                               ConfigurationBpmEngine.BpmnServerDefinition serverDefinition)
      throws AutomatorException {
    BpmnEngine engine = switch (serverDefinition.serverType) {
      case CAMUNDA_7 -> new BpmnEngineCamunda7(engineConfiguration, serverDefinition);

      case CAMUNDA_8 -> new BpmnEngineCamunda8(engineConfiguration, serverDefinition);

      case CAMUNDA_8_SAAS -> new BpmnEngineCamunda8(engineConfiguration, serverDefinition);

      case DUMMY -> new BpmnEngineDummy(engineConfiguration);

    };

    engine.init();
    return engine;
  }
}
