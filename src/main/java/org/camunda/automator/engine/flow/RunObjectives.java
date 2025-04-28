/* ******************************************************************** */
/*                                                                      */
/*  CheckObjective                                                    */
/*                                                                      */
/*  Check if an objective is reach                                    */
/* ******************************************************************** */
package org.camunda.automator.engine.flow;

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.definition.ScenarioFlowControl;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RunObjectives {
    private final BpmnEngine bpmnEngine;
    private final Map<String, RunResult.RecordCreationPI> recordCreationPIMap;
    private final Map<Integer, List<SavePhoto>> flowRateMnObjective = new HashMap<>();
    private final List<ScenarioFlowControl.Objective> listObjectives;
    Logger logger = LoggerFactory.getLogger(RunObjectives.class);
    private Date startDateFilter;
    private Date endDateFilter;
    private long lastHeartBeat;

    public RunObjectives(List<ScenarioFlowControl.Objective> listObjectives,
                         BpmnEngine bpmnEngine,
                         Map<String, RunResult.RecordCreationPI> recordCreationPIMap) {
        this.listObjectives = listObjectives;
        this.bpmnEngine = bpmnEngine;
        this.recordCreationPIMap = recordCreationPIMap;

        for (int i = 0; i < listObjectives.size(); i++) {
            listObjectives.get(i).index = i;
        }
    }

    public void setStartDate(Date startTestDate) {
        this.startDateFilter = startTestDate;
        this.lastHeartBeat = System.currentTimeMillis();
    }

    public void setEndDate(Date endTestDate) {
        this.endDateFilter = endTestDate;
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
                SavePhoto previousPhoto = listValues.isEmpty() ? new SavePhoto() : listValues.get(listValues.size() - 1);
                currentPhoto.delta = currentPhoto.nbOfTasks - previousPhoto.nbOfTasks;
                listValues.add(currentPhoto);
                flowRateMnObjective.put(objective.index, listValues);
                logger.info("heartBeat: FlowRateUserTaskMn [{}] prev [{}} current [{}] delta [{}] expected [{}] in {} s",
                        objective.getInformation(), previousPhoto.nbOfTasks, currentPhoto.nbOfTasks, currentPhoto.delta,
                        objective.value, (currentTime - lastHeartBeat) / 1000);
            }
        }
        lastHeartBeat = currentTime;
    }

    /**
     * Check the objective, and return an analysis string; If the string is empty, the objectif is reach
     *
     * @return empty if the objective is Ok, else an analysis
     */
    public List<ObjectiveResult> check() {
        List<ObjectiveResult> listCheck = new ArrayList<>();
        for (ScenarioFlowControl.Objective objective : listObjectives) {
            if (objective.type == null) {
                logger.error("Objective {} does not have a type", objective.getInformation());
                ObjectiveResult objectiveResult = new ObjectiveResult(objective);
                objectiveResult.success = false;
                objectiveResult.analysis = "Error: Objective " + objective.getInformation() + " does not have a type";
                listCheck.add(objectiveResult);
                continue;
            }
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
            objectiveResult.analysis += "No value to reach";
            return objectiveResult;
        }
        try {
            long processInstancesCreatedAPI = bpmnEngine.countNumberOfProcessInstancesCreated(objective.processId,
                    startDateFilter, endDateFilter);
            RunResult.RecordCreationPI recordCreation = recordCreationPIMap.getOrDefault(objective.processId,
                    new RunResult.RecordCreationPI(objective.processId));

            objectiveResult.recordedSuccessValue = recordCreation.nbCreated;
            objectiveResult.recordedFailValue = recordCreation.nbFailed;

            int percent = (int) (100.0 * objectiveResult.recordedSuccessValue / (objective.value == 0 ? 1 : objective.value));

            objectiveResult.analysis += "Objective " + objective.getInformation() // informatin
                    + ": Goal[" + objective.value // objective
                    + "] Created(zeebeAPI)[" + processInstancesCreatedAPI // Value by the API, not really accurate
                    + "] Created(AutomatorRecord)[" + objectiveResult.recordedSuccessValue // value recorded by automator
                    + " (" + percent + " % )" // percent based on the recorded value
                    + " CreateFail(AutomatorRecord)[" + objectiveResult.recordedFailValue + "]";

            if (objectiveResult.recordedSuccessValue < objective.value) {
                objectiveResult.success = false;
            }
        } catch (AutomatorException e) {
            objectiveResult.success = false;
            objectiveResult.analysis += "Can't search countNumberOfProcessInstancesCreated " + e.getMessage();
        }
        return objectiveResult;
    }

    /**
     * ObjectiveEnded : does process ended?
     *
     * @param objective objective to reach
     * @return result
     */
    private ObjectiveResult checkObjectiveEnded(ScenarioFlowControl.Objective objective) {
        ObjectiveResult objectiveResult = new ObjectiveResult(objective);
        objectiveResult.objectiveValue = objective.value;
        if (objective.value <= 0) {
            objectiveResult.success = true;
            objectiveResult.analysis += "No value to reach";
            return objectiveResult;
        }
        try {
            objectiveResult.recordedSuccessValue = bpmnEngine.countNumberOfProcessInstancesEnded(objective.processId,
                    startDateFilter, endDateFilter);
            if (objectiveResult.recordedSuccessValue < objective.value) {
                objectiveResult.analysis +=
                        "Fail: " + objective.getInformation() + " : " + objective.value + " ended expected, "
                                + objectiveResult.recordedSuccessValue + " created (" + (int) (
                                100.0 * objectiveResult.recordedSuccessValue / objective.value) + " %), ";
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
     *
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
            objectiveResult.recordedSuccessValue = bpmnEngine.countNumberOfTasks(objective.processId, objective.taskId);
            if (objectiveResult.recordedSuccessValue < objective.value) {
                objectiveResult.analysis +=
                        "Fail: " + objective.getInformation() + " : [" + objective.value + "] tasks expected, ";
                objectiveResult.analysis +=
                        objectiveResult.recordedSuccessValue + " found (" + (int) (100.0 * objectiveResult.recordedSuccessValue
                                / objective.value) + " %), ";
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
     *
     * @param objective objective to reach
     * @return result
     */
    private ObjectiveResult checkObjectiveFlowRate(ScenarioFlowControl.Objective objective) {
        ObjectiveResult objectiveResult = new ObjectiveResult(objective);
        // check all values
        try {
            long lowThreshold = (long) (((double) objective.value) * (1.0
                    - ((double) objective.getStandardDeviation()) / 100.0));
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
            int numberUnderThreshold = 0;
            int count = 0;
            for (SavePhoto photo : listValues) {
                sumValues += photo.delta;
                count++;
                if (count == 50) {
                    valuesString.append("... TooManyValues[");
                    valuesString.append(listValues.size());
                    valuesString.append("]");
                }
                if (count < 50) {
                    valuesString.append(photo.delta);
                    valuesString.append(",");
                }

                if (photo.delta < lowThreshold) {
                    numberUnderThreshold++;
                }
            }
            if (numberUnderThreshold > 0) {
                objectiveResult.analysis +=
                        "NumberOrValueUnderThreshold[" + numberUnderThreshold + "], values: " + valuesString;
                objectiveResult.success = false;
            }
            // the total must be at the value
            long averageValue = (long) (((double) sumValues) / listValues.size());
            objectiveResult.recordedSuccessValue = averageValue;
            if (averageValue < objective.value) {
                objectiveResult.analysis += "AverageUnderObjective[" + averageValue + "]";
                objectiveResult.success = false;
            } else {
                objectiveResult.analysis += "AverageReach[" + averageValue + "]";
            }
        } catch (Exception e) {
            logger.error("Error during checkFlowRateObjective {}", e.getMessage(), e);
            objectiveResult.success = false;
        }
        return objectiveResult;

    }

    public static class ObjectiveResult {
        public String analysis = "";
        public boolean success = true;
        public long objectiveValue;
        public long recordedSuccessValue;
        public long recordedFailValue;
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
        public long nbOfTasks = 0;
        public long delta = 0;

    }

}
