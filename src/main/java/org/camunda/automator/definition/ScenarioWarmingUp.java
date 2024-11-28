/* ******************************************************************** */
/*                                                                      */
/*  ScenarioWarmingUp                                                   */
/*                                                                      */
/* Warming up the server                                                */

/* ******************************************************************** */
package org.camunda.automator.definition;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class ScenarioWarmingUp {

    /**
     * The warmingUp will take this duration maximum, except if during this time, all operations warmingUp declare the end
     * (see ScenarioStep)
     */
    public String duration;
    public List<ScenarioStep> operations;

    public boolean useServiceTasks = false;
    public boolean useUserTasks = false;

    public Duration getDuration() {
        return duration == null ? Duration.ZERO : Duration.parse(duration);
    }

    public List<ScenarioStep> getOperations() {
        return operations == null ? Collections.emptyList() : operations;
    }

}
