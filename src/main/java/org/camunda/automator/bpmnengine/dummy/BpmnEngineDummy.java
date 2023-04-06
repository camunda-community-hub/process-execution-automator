package org.camunda.automator.bpmnengine.dummy;

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.bpmnengine.BpmnEngineConfiguration;
import org.camunda.automator.definition.ScenarioDeployment;
import org.camunda.automator.engine.AutomatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

public class BpmnEngineDummy implements BpmnEngine {

  private final BpmnEngineConfiguration engineConfiguration;
  private final Logger logger = LoggerFactory.getLogger(BpmnEngineDummy.class);

  public BpmnEngineDummy(BpmnEngineConfiguration engineConfiguration) {
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
    return null;
  }

  @Override
  public void executeServiceTask(String serviceTaskId, String workerId, Map<String, Object> variables)
      throws AutomatorException {
  }

  @Override
  public List<TaskDescription> searchTasksByProcessInstanceId(String processInstanceId, String taskId, int maxResult)
      throws AutomatorException {
    return null;
  }

  @Override
  public List<ProcessDescription> searchProcessInstanceByVariable(String processId,
                                                                  Map<String, Object> filterVariables, int maxResult) throws AutomatorException {
    return null;
  }

  @Override
  public Map<String, Object> getVariables(String processInstanceId) throws AutomatorException {
    return null;
  }




  @Override
  public String deployBpmn(File processFile, ScenarioDeployment.Policy policy) throws AutomatorException {
    return null;
  }

  @Override
  public BpmnEngineConfiguration.CamundaEngine getTypeCamundaEngine() {
    return BpmnEngineConfiguration.CamundaEngine.DUMMY;
  }

}
