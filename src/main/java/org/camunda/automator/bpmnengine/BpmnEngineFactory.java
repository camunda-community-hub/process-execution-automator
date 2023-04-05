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
import org.camunda.automator.engine.AutomatorException;

/**
 * This can't be a Component, to be used in AutomatorAPI
 */
public class BpmnEngineFactory {

  public static BpmnEngineFactory getInstance() {
    return new BpmnEngineFactory();
  }

  public BpmnEngine getEngineFromConfiguration(BpmnEngineConfiguration engineConfiguration, BpmnEngineConfiguration.BpmnServerDefinition serverDefinition) throws
      AutomatorException {
    BpmnEngine engine = null;
    switch (serverDefinition.camundaEngine) {
    case CAMUNDA_7 ->
      engine = new BpmnEngineCamunda7(engineConfiguration,serverDefinition);

    case CAMUNDA_8 ->
      engine = new BpmnEngineCamunda8(engineConfiguration,serverDefinition);

    case CAMUNDA_8_SAAS ->
      engine = new BpmnEngineCamunda8(engineConfiguration,serverDefinition);

    case DUMMY ->
      engine = new BpmnEngineDummy(engineConfiguration);

    }
    if (engine == null) {
      throw new AutomatorException("No engine is defined : use [" + BpmnEngineConfiguration.CamundaEngine.CAMUNDA_7 + ","
          + BpmnEngineConfiguration.CamundaEngine.DUMMY + "] values");
    }
    engine.init();
    return engine;
  }
}
