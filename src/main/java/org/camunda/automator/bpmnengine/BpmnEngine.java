package org.camunda.automator.bpmnengine;

import org.camunda.automator.definition.ScenarioDeployment;
import org.camunda.automator.engine.AutomatorException;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface BpmnEngine {

  /**
   * init or reinit the connection
   * @throws Exception
   */
  void init() throws AutomatorException;

  String createProcessInstance(String processId, String starterEventId, Map<String, Object> variables)
      throws AutomatorException;

  List<String> searchUserTasks(String processInstanceId, String userTaskName, Integer maxResult) throws AutomatorException;

  String executeUserTask(String activityId, String userId, Map<String, Object> variables) throws AutomatorException;

  List<String> searchServiceTasks(String processInstanceId, String userTaskName, Integer maxResult) throws AutomatorException;

  String executeServiceTask(String activityId, String userId, Map<String, Object> variables) throws AutomatorException;

  /**
   * Deploy a process on the server
   * @param processFile process to deploy
   * @param policy
   * @return the processID
   * @Exception error error during the deployement
   */
  public String deployProcess(File processFile, ScenarioDeployment.Policy policy)  throws AutomatorException;

  public BpmnEngineConfiguration.CamundaEngine getServerDefinition();

}
