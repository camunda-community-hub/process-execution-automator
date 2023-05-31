package org.camunda.automator.bpmnengine.dummy;

import io.camunda.operate.search.DateFilter;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.configuration.ConfigurationBpmEngine;
import org.camunda.automator.definition.ScenarioDeployment;
import org.camunda.automator.engine.AutomatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BpmnEngineDummy implements BpmnEngine {

  private final ConfigurationBpmEngine engineConfiguration;
  private final Logger logger = LoggerFactory.getLogger(BpmnEngineDummy.class);

  public BpmnEngineDummy(ConfigurationBpmEngine engineConfiguration) {
    this.engineConfiguration = engineConfiguration;
  }

  @Override
  public void init() {
    logger.info("BpmnEngineDummy.Init:");
  }

  @Override
  public String createProcessInstance(String processId, String starterEventId, Map<String, Object> variables)
      throws AutomatorException {
    logger.info("BpmnEngineDummy.CreateProcessInstance: Process[" + processId + "] StartEvent[" + starterEventId + "]");
    return "111";
  }

  @Override
  public void endProcessInstance(String processInstanceId, boolean cleanAll) throws AutomatorException {

  }

  @Override
  public List<String> searchUserTasks(String processInstanceId, String userTaskId, int maxResult)
      throws AutomatorException {
    logger.info("BpmnEngineDummy.searchForActivity: Process[" + processInstanceId + "] taskName[" + userTaskId + "]");
    return List.of("5555");
  }

  @Override
  public void executeUserTask(String userTaskId, String userId, Map<String, Object> variables)
      throws AutomatorException {
    logger.info("BpmnEngineDummy.executeUserTask: activityId[" + userTaskId + "]");
  }

  @Override
  public List<String> searchServiceTasks(String processInstanceId, String serviceTaskId, String topic, int maxResult)
      throws AutomatorException {
    return Collections.emptyList();
  }

  @Override
  public void executeServiceTask(String serviceTaskId, String workerId, Map<String, Object> variables)
      throws AutomatorException {
  }

  @Override
  public List<TaskDescription> searchTasksByProcessInstanceId(String processInstanceId, String taskId, int maxResult)
      throws AutomatorException {
    return Collections.emptyList();
  }

  @Override
  public List<ProcessDescription> searchProcessInstanceByVariable(String processId,
                                                                  Map<String, Object> filterVariables,
                                                                  int maxResult) throws AutomatorException {
    return Collections.emptyList();
  }

  @Override
  public Map<String, Object> getVariables(String processInstanceId) throws AutomatorException {
    return Collections.emptyMap();
  }



  /* ******************************************************************** */
  /*                                                                      */
  /*  CountInformation                                                    */
  /*                                                                      */
  /* ******************************************************************** */

  @Override
  public long countNumberOfProcessInstancesCreated(String processName, DateFilter startDate, DateFilter endDate)
      throws AutomatorException {
    throw new AutomatorException("Not yet implemented");
  }

  @Override
  public long countNumberOfProcessInstancesEnded(String processName, DateFilter startDate, DateFilter endDate)
      throws AutomatorException {
    throw new AutomatorException("Not yet implemented");
  }

  public long countNumberOfTasks(String processId, String taskId) throws AutomatorException {
    throw new AutomatorException("Not yet implemented");
  }

  @Override
  public String deployBpmn(File processFile, ScenarioDeployment.Policy policy) throws AutomatorException {
    return null;
  }

  @Override
  public ConfigurationBpmEngine.CamundaEngine getTypeCamundaEngine() {
    return ConfigurationBpmEngine.CamundaEngine.DUMMY;
  }

  @Override
  public String getSignature() {
    return ConfigurationBpmEngine.CamundaEngine.DUMMY.toString();
  }

  public void turnHighFlowMode(boolean hightFlowMode) {
  }

}
