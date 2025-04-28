package org.camunda.automator.definition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Name of this execution
     */
    private String description;
    /**
     * Number of process instance to create
     */
    private Integer numberProcessInstances;

    /**
     * Number of threads in parallel to execute all process instances. Default is 1
     */
    private Integer numberOfThreads;
    private Policy policy;
    /**
     * if set to false, this execution is skipped
     */
    private Boolean execution;

    /**
     * Note: when the object is unserialized from JSON, scnHead is null
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
     * After UnSerialize, all links to parent are not restored
     *
     * @param scnHead head of the scenario
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
        return steps;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Map<String, Object> getJson() {

        HashMap jsonMap = new HashMap();
        jsonMap.put("name", name);
        jsonMap.put("policy", policy.toString());
        jsonMap.put("description", description);
        jsonMap.put("numberProcessInstances", numberProcessInstances);
        jsonMap.put("numberOfThreads", numberOfThreads);

        jsonMap.put("steps", steps.stream().map(
                t -> {
                    return Map.of("type", getSecureValue(t.getType()),
                            "processId", getSecureValue(t.getProcessId()),
                            "taskId", getSecureValue(t.getTaskId()),
                            "topic", getSecureValue(t.getTopic()),
                            "numberOfExecutions", t.getNumberOfExecutions(),
                            "nbThreads", t.getNbThreads(),
                            "synthesis", t.getSynthesis());
                }
        ).toList());

        Map<String, Object> verificationMap = new HashMap();
        jsonMap.put("verifications", verificationMap);
        verificationMap.put("activities",
                verifications.getActivities().stream().map(
                        t -> {
                            return Map.of("type", getSecureValue(t.getType()),
                                    "taskId", getSecureValue(t.getTaskId()),
                                    "synthesis", t.getSynthesis());
                        }
                ).toList());
        verificationMap.put("variables",
                verifications.getVariables().stream().map(
                        t -> {
                            return Map.of("name", getSecureValue(t.getName()),
                                    "value", getSecureValue(t.getValue()),
                                    "synthesis", t.getSynthesis());
                        }
                ).toList());
        verificationMap.put("performances",
                verifications.getPerformances().stream().map(
                        t -> {
                            return Map.of("description", getSecureValue(t.getDescription()),
                                    "fromFlowNode", getSecureValue(t.getFromFlowNode()),
                                    "fromMarker", getSecureValue(t.getFromMarker()),
                                    "toFlowNode", getSecureValue(t.getToFlowNode()),
                                    "toMarker", getSecureValue(t.getToMarker()),
                                    "durationMs", getSecureValue(t.getDurationInMs()),
                                    "synthesis", t.getSynthesis()
                            );
                        }
                ).toList());
        return jsonMap;
    }

    private Object getSecureValue(Object info) {
        return info == null ? "" : info;
    }

    /**
     * Decide what to do when an error is find: stop or continue?
     * default is STOPATFIRSTERROR
     */
    public enum Policy {STOPATFIRSTERROR, CONTINUE}
}
