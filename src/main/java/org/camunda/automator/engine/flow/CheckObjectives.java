/* ******************************************************************** */
/*                                                                      */
/*  CheckObjective                                                    */
/*                                                                      */
/*  Check if an objective is reach                                    */
/* ******************************************************************** */
package org.camunda.automator.engine.flow;

import io.camunda.operate.search.DateFilter;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.definition.ScenarioFlowControl;
import org.camunda.automator.engine.AutomatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

public class CheckObjectives {
  private final BpmnEngine bpmnEngine;
  private final DateFilter startDateFilter;
  private final DateFilter endDateFilter;
  private final Map<String, Long> processInstancesCreatedMap;
  Logger logger = LoggerFactory.getLogger(CheckObjectives.class);

  public CheckObjectives(BpmnEngine bpmnEngine,
                         Date startTestDate,
                         Date endTestDate,
                         Map<String, Long> processInstancesCreatedMap) {
    this.bpmnEngine = bpmnEngine;
    this.startDateFilter = new DateFilter(startTestDate);
    this.endDateFilter = new DateFilter(endTestDate);
    this.processInstancesCreatedMap = processInstancesCreatedMap;
  }

  /**
   * Check the objectif, and return an analysis string; If the string is empty, the objectif is reach
   *
   * @param objective objective to verify
   * @return empty if the objective is Ok, else an analysis
   */
  public ObjectiveResult check(ScenarioFlowControl.Objective objective) {
    return switch (objective.type) {
      case CREATED -> checkObjectiveCreated(objective);
      case ENDED -> checkObjectiveEnded(objective);
      case USERTASK -> checkObjectiveUserTask(objective);
    };
  }

  private ObjectiveResult checkObjectiveCreated(ScenarioFlowControl.Objective objective) {
    ObjectiveResult objectiveResult = new ObjectiveResult();
    objectiveResult.objectiveValue = objective.value;
    if (objective.value <= 0) {
      objectiveResult.success = true;
      objectiveResult.analysis.append("No value to reach");
      return objectiveResult;
    }
    try {
      long processInstancesCreated = bpmnEngine.countNumberOfProcessInstancesCreated(objective.processId,
          startDateFilter, endDateFilter);
      objectiveResult.realValue = processInstancesCreatedMap.getOrDefault(objective.processId, Long.valueOf(0));

      if (objectiveResult.realValue != processInstancesCreated) {
        logger.info("process [{}] PI Created registered {} found in database {}", objective.processId,
            objectiveResult.realValue, processInstancesCreated);
      }
      if (objectiveResult.realValue < objective.value) {
        objectiveResult.analysis.append("Fail: ");
        objectiveResult.analysis.append(objective.label);
        objectiveResult.analysis.append(" : ");
        objectiveResult.analysis.append(objective.value);
        objectiveResult.analysis.append(" creation expected, ");
        objectiveResult.analysis.append(processInstancesCreated);
        objectiveResult.analysis.append(" created (");
        objectiveResult.analysis.append((int) (100.0 * processInstancesCreated / objective.value));
        objectiveResult.analysis.append(" %), ");
        objectiveResult.success = false;
      }
    } catch (AutomatorException e) {
      objectiveResult.success = false;
      objectiveResult.analysis.append("Can't search countNumberOfProcessInstancesCreated " + e.getMessage());
    }
    return objectiveResult;
  }

  private ObjectiveResult checkObjectiveEnded(ScenarioFlowControl.Objective objective) {
    ObjectiveResult objectiveResult = new ObjectiveResult();
    objectiveResult.objectiveValue = objective.value;
    if (objective.value <= 0) {
      objectiveResult.success = true;
      objectiveResult.analysis.append("No value to reach");
      return objectiveResult;
    }
    try {
      objectiveResult.realValue = bpmnEngine.countNumberOfProcessInstancesEnded(objective.processId, startDateFilter,
          endDateFilter);
      if (objectiveResult.realValue < objective.value) {
        objectiveResult.analysis.append("Fail: ");
        objectiveResult.analysis.append(objective.label);
        objectiveResult.analysis.append(" : ");
        objectiveResult.analysis.append(objective.value);
        objectiveResult.analysis.append(" ended expected, ");
        objectiveResult.analysis.append(objectiveResult.realValue);
        objectiveResult.analysis.append(" created (");
        objectiveResult.analysis.append((int) (100.0 * objectiveResult.realValue / objective.value));
        objectiveResult.analysis.append(" %), ");
        objectiveResult.success = false;
      }

    } catch (AutomatorException e) {
      objectiveResult.success = false;
      objectiveResult.analysis.append("Can't search NumberOfProcessInstanceEnded ");
      objectiveResult.analysis.append(e.getMessage());
    }
    return objectiveResult;
  }

  private ObjectiveResult checkObjectiveUserTask(ScenarioFlowControl.Objective objective) {
    ObjectiveResult objectiveResult = new ObjectiveResult();
    objectiveResult.objectiveValue = objective.value;
    if (objective.value <= 0) {
      objectiveResult.success = true;
      objectiveResult.analysis.append("No value to reach");
      return objectiveResult;
    }
    try {
      objectiveResult.realValue = bpmnEngine.countNumberOfTasks(objective.processId, objective.taskId);
      if (objectiveResult.realValue < objective.value) {
        objectiveResult.analysis.append("Fail: ");
        objectiveResult.analysis.append(objective.label);
        objectiveResult.analysis.append(" : ");
        objectiveResult.analysis.append(objective.value);
        objectiveResult.analysis.append(" tasks expected, ");
        objectiveResult.analysis.append(objectiveResult.realValue);
        objectiveResult.analysis.append(" found (");
        objectiveResult.analysis.append((int) (100.0 * objectiveResult.realValue / objective.value));
        objectiveResult.analysis.append(" %), ");
        objectiveResult.success = false;
      }
    } catch (AutomatorException e) {
      objectiveResult.success = false;
      objectiveResult.analysis.append("Can't search NumberOfProcessInstanceEnded ");
      objectiveResult.analysis.append(e.getMessage());
    }
    return objectiveResult;
  }

  public class ObjectiveResult {
    public StringBuilder analysis = new StringBuilder();
    public boolean success = true;
    public long objectiveValue;
    public long realValue;
  }

}
