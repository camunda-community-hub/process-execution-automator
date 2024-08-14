/* ******************************************************************** */
/*                                                                      */
/*  BpmnEngineFactory                                                    */
/*                                                                      */
/*  Generate the client to the engine                                    */
/* ******************************************************************** */
package org.camunda.automator.bpmnengine;

import org.camunda.automator.bpmnengine.camunda7.BpmnEngineCamunda7;
import org.camunda.automator.bpmnengine.camunda8.BenchmarkStartPiExceptionHandlingStrategy;
import org.camunda.automator.bpmnengine.camunda8.BpmnEngineCamunda8;
import org.camunda.automator.bpmnengine.dummy.BpmnEngineDummy;
import org.camunda.automator.configuration.BpmnEngineList;
import org.camunda.automator.engine.AutomatorException;

import java.util.EnumMap;
import java.util.Map;

/*
 * This can't be a Component, to be used in AutomatorAPI
 */
public class BpmnEngineFactory {

  private static final BpmnEngineFactory bpmnEngineFactory = new BpmnEngineFactory();
  Map<BpmnEngineList.CamundaEngine, BpmnEngine> cacheEngine = new EnumMap<>(BpmnEngineList.CamundaEngine.class);
  BenchmarkStartPiExceptionHandlingStrategy benchmarkStartPiExceptionHandlingStrategy = null;

  private BpmnEngineFactory() {
    // use the getInstance() method
  }

  public static BpmnEngineFactory getInstance() {
    return bpmnEngineFactory;
  }

  public static BpmnEngineFactory getInstance(BenchmarkStartPiExceptionHandlingStrategy benchmarkStartPiExceptionHandlingStrategy) {
    bpmnEngineFactory.benchmarkStartPiExceptionHandlingStrategy = benchmarkStartPiExceptionHandlingStrategy;
    return bpmnEngineFactory;
  }

  public BpmnEngine getEngineFromConfiguration(BpmnEngineList.BpmnServerDefinition serverDefinition, boolean logDebug)
      throws AutomatorException {
    BpmnEngine engine = cacheEngine.get(serverDefinition.serverType);
    if (engine != null)
      return engine;

    // instantiate and initialize the engine now
    synchronized (this) {
      engine = cacheEngine.get(serverDefinition.serverType);
      if (engine != null)
        return engine;

      engine = switch (serverDefinition.serverType) {
        case CAMUNDA_7 -> new BpmnEngineCamunda7(serverDefinition, logDebug);

        case CAMUNDA_8 ->
            BpmnEngineCamunda8.getFromServerDefinition(serverDefinition, benchmarkStartPiExceptionHandlingStrategy,
                logDebug);

        case CAMUNDA_8_SAAS ->
            BpmnEngineCamunda8.getFromServerDefinition(serverDefinition, benchmarkStartPiExceptionHandlingStrategy,
                logDebug);

        case DUMMY -> new BpmnEngineDummy(serverDefinition);

      };

      engine.init();
      engine.connection();
      cacheEngine.put(serverDefinition.serverType, engine);
    }
    return engine;
  }
}
