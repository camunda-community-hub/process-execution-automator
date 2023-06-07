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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunObjectives {
  private final BpmnEngine bpmnEngine;
  private DateFilter startDateFilter;
  private DateFilter endDateFilter;
  private final Map<String, Long> processInstancesCreatedMap;

  private final Map<Integer, List<SavePhoto>> flowRateMnObjective = new HashMap<>();


  private long lastHeartBeat;
  private final List<ScenarioFlowControl.Objective> listObjectives;
  Logger logger = LoggerFactory.getLogger(RunObjectives.class);

  public RunObjectives(List<ScenarioFlowControl.Objective> listObjectives,
                       BpmnEngine bpmnEngine,
                       Map<String, Long> processInstancesCreatedMap) {
    this.listObjectives = listObjectives;
    this.bpmnEngine = bpmnEngine;
    this.processInstancesCreatedMap = processInstancesCreatedMap;

    for (int i=0;i<listObjectives.size();i++) {
      listObjectives.get(i).index=i;
    }
  }

  public void setStartDate(Date startTestDate) {
    this.startDateFilter = new DateFilter(startTestDate);
    this.lastHeartBeat = System.currentTimeMillis();
  }

  public void setEndDate(Date endTestDate) {
    this.endDateFilter = new DateFilter(endTestDate);
  }

  /**
   * heartbeat
   */
  public void heartBeat() {
    long currentTime = System.currentTimeMillis();
    // only one minutes
    if (currentTime - lastHeartBeat < 1000 * 60)
      return;

    // one minutes: do we have a FLOWRATEUSERTASKMN objective
    for (ScenarioFlowControl.Objective objective : listObjectives) {
      if (ScenarioFlowControl.Objective.TYPEOBJECTIVE.FLOWRATEUSERTASKMN.equals(objective.type)) {
        // get the value
        SavePhoto currentPhoto = new SavePhoto();
        try {
          currentPhoto.nbOfTasks = bpmnEngine.countNumberOfTasks(objective.processId, objective.taskId);
        } catch (AutomatorException e) {
          logger.error("Can't get NumberOfTask ");
        }
        List<SavePhoto> listValues = flowRateMnObjective.getOrDefault(objective.index, new ArrayList<>());
        SavePhoto previousPhoto = listValues.isEmpty()? new SavePhoto() : listValues.get(listValues.size()-1);
        currentPhoto.delta = currentPhoto.nbOfTasks -previousPhoto.nbOfTasks;
        listValues.add(currentPhoto);
        flowRateMnObjective.put(objective.index, listValues);
        logger.info("heartBeat: FlowRateUserTaskMn [{}] prev {} current {} delta {} expected {} in {} s",
            objective.label,
            previousPhoto.nbOfTasks, currentPhoto.nbOfTasks, currentPhoto.delta, objective.value,
            (currentTime-lastHeartBeat)/1000);
      }
    }
    lastHeartBeat=currentTime;
  }

  /**
   * Check the objective, and return an analysis string; If the string is empty, the objectif is reach
   *
   * @return empty if the objective is Ok, else an analysis
   */
  public List<ObjectiveResult> check() {
    List<ObjectiveResult> listCheck = new ArrayList<>();
    for(ScenarioFlowControl.Objective objective: listObjectives) {
      listCheck.add(switch (objective.type) {
        case CREATED -> checkObjectiveCreated(objective);
        case ENDED -> checkObjectiveEnded(objective);
        case USERTASK -> checkObjectiveUserTask(objective);
        case FLOWRATEUSERTASKMN -> checkObjectiveFlowRate(objective);
      });
    }
    return listCheck;
  }

  /**
   * Creation: does the number of process instance was created?
   *
   * @param objective objective to reach
   * @return result
   */
  private ObjectiveResult checkObjectiveCreated(ScenarioFlowControl.Objective objective) {
    ObjectiveResult objectiveResult = new ObjectiveResult(objective);
    objectiveResult.objectiveValue = objective.value;
    if (objective.value <= 0) {
      objectiveResult.success = true;
      objectiveResult.analysis+="No value to reach";
      return objectiveResult;
    }
    try {
      long processInstancesCreated = bpmnEngine.countNumberOfProcessInstancesCreated(objective.processId,
          startDateFilter, endDateFilter);
      objectiveResult.realValue = processInstancesCreatedMap.getOrDefault(objective.processId, 0L);

      if (objectiveResult.realValue != processInstancesCreated) {
        logger.info("processID [{}] PI Created[{}} FoundInEngine[{}]", objective.processId,
            objectiveResult.realValue, processInstancesCreated);
      }
      if (processInstancesCreated < objective.value) {
        objectiveResult.analysis+="Fail: "+objective.label+": ObjectiveCreation["+objective.value+"] Created["+processInstancesCreated+"] ("+(int) (100.0 * processInstancesCreated / objective.value)+" %), ";
        objectiveResult.success = false;
      }
    } catch (AutomatorException e) {
      objectiveResult.success = false;
      objectiveResult.analysis+="Can't search countNumberOfProcessInstancesCreated " + e.getMessage();
    }
    return objectiveResult;
  }

  /**
   * ObjectiveEnded : does process ended?
   * @param objective objective to reach
   * @return result
   */
  private ObjectiveResult checkObjectiveEnded(ScenarioFlowControl.Objective objective) {
    ObjectiveResult objectiveResult = new ObjectiveResult(objective);
    objectiveResult.objectiveValue = objective.value;
    if (objective.value <= 0) {
      objectiveResult.success = true;
      objectiveResult.analysis +="No value to reach";
      return objectiveResult;
    }
    try {
      objectiveResult.realValue = bpmnEngine.countNumberOfProcessInstancesEnded(objective.processId, startDateFilter,
          endDateFilter);
      if (objectiveResult.realValue < objective.value) {
        objectiveResult.analysis +=
            "Fail: " + objective.label + " : " + objective.value + " ended expected, " + objectiveResult.realValue
                + " created (" + (int) (100.0 * objectiveResult.realValue / objective.value) + " %), ";
        objectiveResult.success = false;
      }

    } catch (AutomatorException e) {
      objectiveResult.success = false;
      objectiveResult.analysis += "Can't search NumberOfProcessInstanceEnded: " + e.getMessage();
    }
    return objectiveResult;
  }

  /**
   * UserTask: does user tasks are present?
   * @param objective objective to reach
   * @return result
   */
  private ObjectiveResult checkObjectiveUserTask(ScenarioFlowControl.Objective objective) {
    ObjectiveResult objectiveResult = new ObjectiveResult(objective);
    objectiveResult.objectiveValue = objective.value;
    if (objective.value <= 0) {
      objectiveResult.success = true;
      objectiveResult.analysis += "No value to reach";
      return objectiveResult;
    }
    try {
      objectiveResult.realValue = bpmnEngine.countNumberOfTasks(objective.processId, objective.taskId);
      if (objectiveResult.realValue < objective.value) {
        objectiveResult.analysis += "Fail: " + objective.label + " : [" + objective.value + "] tasks expected, ";
        objectiveResult.analysis +=
            objectiveResult.realValue + " found (" + (int) (100.0 * objectiveResult.realValue / objective.value)
                + " %), ";
        objectiveResult.success = false;
      }
    } catch (AutomatorException e) {
      objectiveResult.success = false;
      objectiveResult.analysis += "Can't search NumberOfProcessInstanceEnded: " + e.getMessage();
    }
    return objectiveResult;
  }

  /**
   * FlowRate
   * @param objective objective to reach
   * @return result
   */
  private ObjectiveResult checkObjectiveFlowRate(ScenarioFlowControl.Objective objective) {
    ObjectiveResult objectiveResult = new ObjectiveResult(objective);
    // check all values
    try {
      long lowThreshold = (long) (((double)objective.value) * (1.0 - ((double)objective.getStandardDeviation()) / 100.0));
      objectiveResult.objectiveValue = objective.value;
      objectiveResult.analysis +=
          "Threshold[" + objective.value + "] standardDeviation[" + objective.getStandardDeviation() + "] LowThreshold["
              + lowThreshold + "]";
      long sumValues = 0;
      List<SavePhoto> listValues = flowRateMnObjective.getOrDefault(objective.index, new ArrayList<>());
      if (listValues.isEmpty()) {
        objectiveResult.analysis += "No values";
        objectiveResult.success = false;
        return objectiveResult;
      }

      StringBuilder valuesString = new StringBuilder();
      int numberUnderThreshold=0;
      int count=0;
      for (SavePhoto photo : listValues) {
        sumValues += photo.delta;
        count++;
        if (count==50) {
          valuesString.append("... TooManyValues[");
          valuesString.append(listValues.size());
          valuesString.append("]");
        }
        if (count<50) {
          valuesString.append(photo.delta);
          valuesString.append(",");
        }

        if (photo.delta < lowThreshold) {
          numberUnderThreshold++;
        }
      }
      if (numberUnderThreshold>0) {
        objectiveResult.analysis += "NumberOrValueUnderThreshold[" + numberUnderThreshold + "], values: "+valuesString.toString();
        objectiveResult.success = false;
      }
      // the total must be at the value
      long averageValue = (long) ( ((double)sumValues) / listValues.size());
      objectiveResult.realValue=averageValue;
      if (averageValue < objective.value) {
        objectiveResult.analysis += "AverageUnderObjective[" + averageValue + "]";
        objectiveResult.success = false;
      } else {
        objectiveResult.analysis += "AverageReach[" + averageValue + "]";
      }
    } catch(Exception e) {
      logger.error("Error during checkFlowRateObjective {}",e.getMessage());
      objectiveResult.success=false;
    }
    return objectiveResult;

  }

  public static class ObjectiveResult {
    public String analysis = "";
    public boolean success = true;
    public long objectiveValue;
    public long realValue;
    ScenarioFlowControl.Objective objective;

    public ObjectiveResult(ScenarioFlowControl.Objective objective) {
      this.objective = objective;
    }
  }

  /**
   * Key is the Objective Index
   * Value is a list of two information:
   * - the reference value in the slot
   * - the
   */
  public static class SavePhoto {
    public long nbOfTasks =0;
    public long delta=0;

  }


}
