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
  private final int index;

  RunScenarioFlowBasic(ScenarioStep scenarioStep, int index, RunScenario runScenario, RunResult runResult) {
    this.index = index;
    this.scenarioStep = scenarioStep;
    this.runScenario = runScenario;
    this.runResult = runResult;
  }

  /**
   * Return the index of this basicFlow
   *
   * @return index
   */
  public int getIndex() {
    return index;
  }

  public String getId() {
    String id = scenarioStep.getType() + " ";
    id += switch (scenarioStep.getType()) {
      case STARTEVENT -> scenarioStep.getProcessId() + "-" + scenarioStep.getTaskId();
      case SERVICETASK -> scenarioStep.getTopic();
      default -> "";
    };
    return id + "#" + getIndex();
  }

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

  /**
   * The flow must stop now
   */
  public abstract void pleaseStop();

  public enum STATUS {RUNNING, STOPPING, STOPPED}

}
