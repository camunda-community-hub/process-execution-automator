package org.camunda.automator.definition;

import java.time.Duration;

public class ScenarioVerificationPerformance implements ScenarioVerificationBasic {
    public String fromFlowNode;
    public String fromMarker;
    public String toFlowNode;
    public String toMarker;
    public String duration;
    public String description;

    public void setFromFlowNode(String fromFlowNode) {
        this.fromFlowNode = fromFlowNode;
    }

    public void setToFlowNode(String toFlowNode) {
        this.toFlowNode = toFlowNode;
    }

    public String getFromFlowNode() {
        return fromFlowNode;
    }

    public String getToFlowNode() {
        return toFlowNode;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public Marker getMarker(String marker) {
        try {
            if (marker == null)
                return Marker.BEGIN;
            return Marker.valueOf(marker);
        } catch (Exception e) {
            return Marker.BEGIN;
        }
    }

    public Marker getFromMarker() {
        return getMarker(fromMarker);
    }

    public void setFromMarker(String fromMarker) {
        this.fromMarker = fromMarker;
    }

    public Marker getToMarker() {
        return getMarker(toMarker);
    }

    public void setToMarker(String toMarker) {
        this.toMarker = toMarker;
    }

    public long getDurationInMs() {
        return Duration.parse(duration).toMillis();
    }

    public String getSynthesis() {
        return "PerformanceCheck [" + fromFlowNode + "("+fromMarker+")] => [" + toFlowNode + "("+toMarker+")] in [" + getDurationInMs() + " ms]";
    }

    public String getTypeVerification() {
        return "PERFORMANCE";
    }
    public enum Marker {BEGIN, END}

}


