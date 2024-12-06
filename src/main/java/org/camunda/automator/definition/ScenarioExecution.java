package org.camunda.automator.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A scenario execution pilot one execution of a scenarioHead
 */
public class ScenarioExecution {

    private final List<ScenarioStep> steps = new ArrayList<>();
    private Scenario scnHead;
    private ScenarioVerification verifications;

    /**
     * Name of this execution
     */
    private String name;
    /**
     * Number of process instance to create
     */
    private Integer numberProcessInstances;

    /**
     * Number of thread in parallel to execute all process instances. Default is 1
     */
    private Integer numberOfThreads;
    private Policy policy;
    /**
     * if set to false, this execution is skipped
     */
    private Boolean execution;

    /**
     * Note: when the object is un-serialized from JSON, scnHead is null
     *
     * @param scenario root information
     */
    protected ScenarioExecution(Scenario scenario) {
        this.scnHead = scenario;
    }

    public static ScenarioExecution createExecution(Scenario scnHead) {
        return new ScenarioExecution(scnHead);
    }



    /* ******************************************************************** */
    /*                                                                      */
    /*  Creator and setter to help the API                                  */
    /*                                                                      */
    /* ******************************************************************** */

    /**
     * After UnSerialize, all link to parent are not restored
     *
     * @param scnHead head of scenario
     */
    public void afterUnSerialize(Scenario scnHead) {
        this.scnHead = scnHead;
        for (ScenarioStep scnStep : steps) {
            scnStep.afterUnSerialize(this);
        }
    }

    /**
     * Add a step in the scenario
     *
     * @param step step part of the scenario
     * @return this object
     */
    public ScenarioExecution addStep(ScenarioStep step) {
        steps.add(step);
        return this;
    }

    public List<ScenarioStep> getSteps() {
        return steps == null ? Collections.emptyList() : steps;
    }

    public ScenarioVerification getVerifications() {
        return verifications;
    }



    /* ******************************************************************** */
    /*                                                                      */
    /*  getter                                                              */
    /*                                                                      */
    /* ******************************************************************** */

    public int getNumberProcessInstances() {
        return numberProcessInstances == null ? 1 : numberProcessInstances;
    }

    /**
     * Ask this execution to execute a number of process instance.
     *
     * @param numberProcessInstances number of process instance to execute
     * @return this object
     */
    public ScenarioExecution setNumberProcessInstances(int numberProcessInstances) {
        this.numberProcessInstances = numberProcessInstances;
        return this;
    }

    public Scenario getScnHead() {
        return scnHead;
    }

    public String getName() {
        return name;
    }

    public ScenarioExecution setName(String name) {
        this.name = name;
        return this;
    }


    public int getNumberOfThreads() {
        return (numberOfThreads == null ? 1 : numberOfThreads <= 0 ? 1 : numberOfThreads);
    }

    public void setNumberOfThreads(Integer numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    public Policy getPolicy() {
        return (policy == null ? Policy.STOPATFIRSTERROR : policy);
    }

    public boolean isExecution() {
        return execution == null || Boolean.TRUE.equals(execution);
    }

    /**
     * Decide what to do when an error is find: stop or continue?
     * default is STOPATFIRSTERROR
     */
    public enum Policy {STOPATFIRSTERROR, CONTINUE}
}
