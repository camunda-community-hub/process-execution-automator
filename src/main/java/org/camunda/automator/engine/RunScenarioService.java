package org.camunda.automator.engine;

/**
 * This class saved and manage all running scenario.
 * Then, it will be possible to come to that class and ask all running scenario.
 */

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.definition.Scenario;
import org.camunda.automator.services.ServiceAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class RunScenarioService {
    Map<String, RunResult> cacheRunScenario = new HashMap<>();
    @Autowired
    ServiceAccess serviceAccess;


    public String createExecutionId(Scenario scenario) {
        return System.currentTimeMillis() + "." + scenario.getName();
    }


    public RunResult executeScenario(BpmnEngine bpmnEngine, RunParameters runParameters, Scenario scenario, boolean asynchronous) {
        RunResult runResult = null;

        String executionId = createExecutionId(scenario);

        if (asynchronous) {
            // Create now he executionId
            RunScenario runScenario = new RunScenario(scenario, bpmnEngine, runParameters, serviceAccess);
            runResult = new RunResult(runScenario, executionId);
            runResult.setStartDate(new Date());
            cacheRunScenario.put(executionId, runResult);

            // so the tread use the executionId to fulfill the result
            Thread thread = new Thread(() -> executeScenarioInternal(bpmnEngine, runParameters, scenario, executionId));
            thread.start();
            // Create an arbiratry runResult here. What is important is to return the executionId
        } else {
            runResult = executeScenarioInternal(bpmnEngine, runParameters, scenario, executionId);
        }
        return runResult;
    }

    /**
     * Execute a test
     *
     * @param bpmnEngine
     * @param runParameters
     * @param scenario
     * @return
     */
    private RunResult executeScenario(BpmnEngine bpmnEngine, RunParameters runParameters, Scenario scenario) {
        String executionId = createExecutionId(scenario);

        return executeScenarioInternal(bpmnEngine, runParameters, scenario, executionId);
    }

    private RunResult executeScenarioInternal(BpmnEngine bpmnEngine, RunParameters runParameters, Scenario scenario, String executionId) {
        RunScenario runScenario = null;
        try {
            runScenario = new RunScenario(scenario, bpmnEngine, runParameters, serviceAccess);
        } catch (Exception e) {
            RunResult runResult = new RunResult(runScenario, executionId);
            runResult.addError(null, "Initialization error");
            cacheRunScenario.put(executionId, runResult);

            return runResult;
        }

        // Now run the scenario
        RunResult runResult = new RunResult(runScenario, executionId);
        runResult.setStartDate(new Date());
        cacheRunScenario.put(executionId, runResult);

        runResult.merge(runScenario.executeTheScenario(executionId));
        return runResult;
    }


    public RunResult executeDeployment(BpmnEngine bpmnEngine, RunParameters runParameters, Scenario scenario) {

        String executionId = createExecutionId(scenario);

        RunScenario runScenario = new RunScenario(scenario, bpmnEngine, runParameters, serviceAccess);
        // so now the runResult is available, and can be query
        RunResult runResult = new RunResult(runScenario, executionId);
        cacheRunScenario.put(executionId, runResult);

        runResult.merge(runScenario.executeDeployment(executionId));
        return runResult;
    }


    public RunResult getFromExecutionId(String executionId) {
        return cacheRunScenario.get(executionId);
    }

    public Collection<RunResult> getRunResult() {
        return cacheRunScenario.values();
    }

    public RunResult getByExecutionId(String executionId) {
        return cacheRunScenario.get(executionId);
    }

    public void clearAll() {
        cacheRunScenario.clear();
    }
}