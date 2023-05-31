/* ******************************************************************** */
/*                                                                      */
/*  ScenarioFlowControl                                                 */
/*                                                                      */
/* Parameters to control the Flow execution                             */
/* ******************************************************************** */
package org.camunda.automator.definition;

import java.time.Duration;
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
    return objectives;
  }

  public static class Objective {
    public String label;
    public TYPEOBJECTIVE type;
    public String processId;
    public String taskId;
    public String period;
    public Integer value;

    public enum TYPEOBJECTIVE {CREATED, ENDED, USERTASK}
  }
}
