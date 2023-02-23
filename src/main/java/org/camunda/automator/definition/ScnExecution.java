package org.camunda.automator.definition;

import java.util.ArrayList;
import java.util.List;

public class ScnExecution {

  private ScnHead scnHead;
  private final List<ScnStep> steps = new ArrayList<>();
  /**
   * Name of this execution
   */
  private String name;
  /**
   * Number of process instance to create
   */
  private Integer numberProcessInstances;

  /**
   * Number of thread in parallel to execute all process instances
   */
  private int numberOfThreads;
  /**
   * Note: when the object is un-serialized from JSON, scnHead is null
   *
   * @param scnHead
   */
  protected ScnExecution(ScnHead scnHead) {
    this.scnHead = scnHead;
  }



  /* ******************************************************************** */
  /*                                                                      */
  /*  Creator and setter to help the API                                  */
  /*                                                                      */
  /* ******************************************************************** */

  public static ScnExecution createExecution(ScnHead scnHead) {
    return new ScnExecution(scnHead);
  }

  /**
   * After UnSerialize, all link to parent are not restored
   *
   * @param scnHead head of scenario
   */
  public void afterUnSerialize(ScnHead scnHead) {
    this.scnHead = scnHead;
    for (ScnStep scnStep : steps) {
      scnStep.afterUnSerialize(this);
    }
  }

  /**
   * Add a step in the scenario
   *
   * @param step
   * @return
   */
  public ScnExecution addStep(ScnStep step) {
    steps.add(step);
    return this;
  }
  /**
   * Ask this execution to execute a number of process instance.
   *
   * @param numberProcessInstances
   * @return
   */
  public ScnExecution setNumberProcessInstances(int numberProcessInstances) {
    this.numberProcessInstances = numberProcessInstances;
    return this;
  }



  /* ******************************************************************** */
  /*                                                                      */
  /*  getter                                                              */
  /*                                                                      */
  /* ******************************************************************** */

  public List<ScnStep> getSteps() {
    return steps;
  }

  public int getNumberProcessInstances() {
    return numberProcessInstances==null? 1 : numberProcessInstances;
  }

  public ScnHead getScnHead() {
    return scnHead;
  }

  public String getName() {
    return name;
  }

  public ScnExecution setName(String name) {
    this.name = name;
    return this;
  }

  public int getNumberOfThreads() {
    return (numberOfThreads<=0? 1 : numberOfThreads);
  }
}
