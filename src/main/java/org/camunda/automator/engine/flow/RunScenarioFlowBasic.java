/* ******************************************************************** */
/*                                                                      */
/*  RunScenarioFlowBasic                                                */
/*                                                                      */
/*  All execution derived on this class                                 */
/* When multiple worker are required for a step, multiple FlowBasic     */
/* object are created, with a different index.                          */
/* ******************************************************************** */
package org.camunda.automator.engine.flow;

import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.RunResult;
import org.camunda.automator.engine.RunScenario;

public abstract class RunScenarioFlowBasic {

    protected final RunResult runResult;
    private final ScenarioStep scenarioStep;
    private final RunScenario runScenario;

    RunScenarioFlowBasic(ScenarioStep scenarioStep, RunScenario runScenario, RunResult runResult) {
        this.scenarioStep = scenarioStep;
        this.runScenario = runScenario;
        this.runResult = runResult;
    }

    /**
     * Return an uniq ID of the step
     *
     * @return the ID of the step
     */
    public String getId() {
        return scenarioStep.getId();
    }

    /**
     * the task return the topic to address:
     * - topic for a service task
     * - taskId for a user task
     */
    public abstract String getTopic();

    public RunScenario getRunScenario() {
        return runScenario;
    }

    /**
     * The flow return the runResult given at the execution
     *
     * @return result
     */
    public RunResult getRunResult() {
        return runResult;
    }

    /**
     * The flow execute a step - return it
     *
     * @return scenarioStep
     */
    public ScenarioStep getScenarioStep() {
        return scenarioStep;
    }

    /**
     * Start the execution. Attention, only errors must be reported in the result
     */
    public abstract void execute();

    public abstract STATUS getStatus();

    public abstract int getCurrentNumberOfThreads();

    /**
     * The flow must stop now
     */
    public abstract void pleaseStop();

    public enum STATUS {RUNNING, STOPPING, STOPPED}

}
