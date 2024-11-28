package org.camunda.automator.bpmnengine.camunda8;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;

@Component
public class StatisticsCollector {

    private final Date startTime = new Date();

    private final long lastPrintStartedProcessInstances = 0;
    private final long lastPrintCompletedProcessInstances = 0;
    private final long lastPrintCompletedJobs = 0;
    private final long lastPrintStartedProcessInstancesBackpressure = 0;

    private long piPerSecondGoal;

    @PostConstruct
    public void init() {
    }

    public void hintOnNewPiPerSecondGoald(long piPerSecondGoal) {
        this.piPerSecondGoal = piPerSecondGoal;
    }

    public void incStartedProcessInstances() {
    }

    public void incStartedProcessInstancesBackpressure() {
    }

    public void incCompletedProcessInstances() {
    }

    public void incCompletedProcessInstances(long startMillis, long endMillis) {
    }

    public void incCompletedJobs() {
    }

    public void incStartedProcessInstancesException(String exceptionMessage) {
    }

    public void incCompletedJobsException(String exceptionMessage) {
    }

}
