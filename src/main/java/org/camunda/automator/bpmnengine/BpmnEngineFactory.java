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
import org.camunda.automator.configuration.ConfigurationBpmnEngineList;
import org.camunda.automator.engine.AutomatorException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/*
 * This can't be a Component, to be used in AutomatorAPI
 */
@Component
public class BpmnEngineFactory {

    Map<ConfigurationBpmnEngineList.CamundaEngine, BpmnEngine> cacheEngine = new EnumMap<>(ConfigurationBpmnEngineList.CamundaEngine.class);
    BenchmarkStartPiExceptionHandlingStrategy benchmarkStartPiExceptionHandlingStrategy = null;

    private BpmnEngineFactory() {
    }


    public BpmnEngine getEngineFromConfiguration(ConfigurationBpmnEngineList.BpmnServerDefinition serverDefinition, boolean logDebug)
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
