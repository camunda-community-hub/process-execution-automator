/* ******************************************************************** */
/*                                                                      */
/*  BpmnEngineFactory                                                    */
/*                                                                      */
/*  Generate the client to the engine                                    */
/* ******************************************************************** */
package org.camunda.automator.bpmnengine;

import org.camunda.automator.bpmnengine.camunda7.BpmnEngineCamunda7;
import org.camunda.automator.bpmnengine.dummy.BpmnEngineDummy;

/**
 * This can't be a Component, to be used in AutomatorAPI
 */
public class BpmnEngineFactory {

  public static BpmnEngineFactory getInstance() {
    return new BpmnEngineFactory();
  }

  public BpmnEngine getEngineFromConfiguration(BpmnEngineConfiguration engineConfiguration) throws Exception {
    BpmnEngine engine = null;
    switch (engineConfiguration.camundaEngine) {
    case CAMUNDA_7 -> {
      engine = new BpmnEngineCamunda7(engineConfiguration);
    }
    case DUMMY -> {
      engine = new BpmnEngineDummy(engineConfiguration);
    }
    }
    if (engine == null) {
      throw new Exception("No engine is defined : use [" + BpmnEngineConfiguration.CamundaEngine.CAMUNDA_7 + ","
          + BpmnEngineConfiguration.CamundaEngine.DUMMY + "] values");
    }
    engine.init();
    return engine;
  }
}
