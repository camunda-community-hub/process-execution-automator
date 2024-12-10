/* ******************************************************************** */
/*                                                                      */
/*  ScenarioFlowControl                                                 */
/*                                                                      */
/* Parameters to control the Flow execution                             */
/* ******************************************************************** */
package org.camunda.automator.definition;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class ScenarioFlowControl {
    private String duration;

    private Integer increaseStep;
    private List<Objective> objectives;

    public Duration getDuration() {
        try {
            return Duration.parse(duration);
        } catch (Exception e) {
            return Duration.ofMinutes(10);
        }
    }

    public Integer getIncreaseStep() {
        return increaseStep;
    }

    public List<Objective> getObjectives() {
        return objectives == null ? Collections.emptyList() : objectives;
    }

    public static class Objective {
        public int index;
        public String name;
        public String label;
        public TYPEOBJECTIVE type;
        public String processId;
        public String taskId;
        public String period;
        public Integer value;
        public Integer standardDeviation;

        public int getStandardDeviation() {
            return standardDeviation == null ? 0 : standardDeviation;
        }

        public String getInformation() {
            String information = (name == null ? "" : name + "-") + (label == null ? "" : label);
            if (information.length() > 0)
                return information;
            return (type == null ? "NoType" : type.toString()) + " period[" + period + "] value:[" + value + "]";
        }

        public enum TYPEOBJECTIVE {CREATED, ENDED, USERTASK, FLOWRATEUSERTASKMN}
    }
}
