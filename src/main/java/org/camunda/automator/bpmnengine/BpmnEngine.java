package org.camunda.automator.bpmnengine;

import io.camunda.operate.search.DateFilter;
import org.camunda.automator.configuration.ConfigurationBpmEngine;
import org.camunda.automator.definition.ScenarioDeployment;
import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.AutomatorException;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface BpmnEngine {

  /**
   * init or reinit the connection
   *
   * @throws Exception in case of error
   */
  void init() throws AutomatorException;

  /* ******************************************************************** */
  /*                                                                      */
  /*  Manage process instance                                             */
  /*                                                                      */
  /* ******************************************************************** */
  void turnHighFlowMode(boolean hightFlowMode);

  /**
   * @param processId      Process ID (BPMN ID : ExpenseNode)
   * @param starterEventId BPMN ID (startEvent)
   * @param variables      List of variables to create the process instance
   * @return a processInstanceId
   * @throws AutomatorException in case of error
   */
  String createProcessInstance(String processId, String starterEventId, Map<String, Object> variables)
      throws AutomatorException;

  /**
   * we finish with this processinstanceid, engine can clean it
   *
   * @param processInstanceId Process instance Id to clean
   * @param cleanAll          if true, the process instance must be clean.
   * @throws AutomatorException in case of error
   */
  void endProcessInstance(String processInstanceId, boolean cleanAll) throws AutomatorException;


  /* ******************************************************************** */
  /*                                                                      */
  /*  User task                                                           */
  /*                                                                      */
  /* ******************************************************************** */

  /**
   * @param processInstanceId Process Instance Id
   * @param userTaskId        BPMN Id (Review)
   * @param maxResult         maximum result to return.
   * @return list of taskId
   * @throws AutomatorException in case of error
   */
  List<String> searchUserTasks(String processInstanceId, String userTaskId, int maxResult) throws AutomatorException;

  /**
   * @param userTaskId BPMN Id (Review)
   * @param userId     User id who executes the task
   * @param variables  variable to update
   * @throws AutomatorException in case of error
   */
  void executeUserTask(String userTaskId, String userId, Map<String, Object> variables) throws AutomatorException;


  /* ******************************************************************** */
  /*                                                                      */
  /*  Service tasks                                                       */
  /*                                                                      */
  /* ******************************************************************** */

  /**
   * @param processInstanceId process instance ID
   * @param serviceTaskId     BPMN IP (Review)
   * @param topic             topic to search to execute the service task
   * @param maxResult         maximum result
   * @return list of taskId
   * @throws AutomatorException in case of error
   */
  List<String> searchServiceTasks(String processInstanceId, String serviceTaskId, String topic, int maxResult)
      throws AutomatorException;

  /**
   * Execute a service task
   *
   * @param serviceTaskId BPMN ID (Review)
   * @param workerId      Worker who execute the task
   * @param variables     variable to updates
   * @throws AutomatorException in case of error
   */
  void executeServiceTask(String serviceTaskId, String workerId, Map<String, Object> variables)
      throws AutomatorException;

  /* ******************************************************************** */
  /*                                                                      */
  /*  Generic tasks                                                       */
  /*                                                                      */
  /* ******************************************************************** */

  /**
   * Search task.
   *
   * @param processInstanceId filter on the processInstanceId. may be null
   * @param taskId            filter on the taskId
   * @param maxResult         maximum Result
   * @return List of task description
   * @throws AutomatorException in case of error
   */
  List<TaskDescription> searchTasksByProcessInstanceId(String processInstanceId, String taskId, int maxResult)
      throws AutomatorException;

  /**
   * Search process instance by a variable content
   *
   * @param processId       BPMN Process ID
   * @param filterVariables Variable name
   * @param maxResult       maxResult
   * @return list of ProcessInstance which match the filter
   * @throws AutomatorException in case of error
   */
  List<ProcessDescription> searchProcessInstanceByVariable(String processId,
                                                           Map<String, Object> filterVariables,
                                                           int maxResult) throws AutomatorException;

  /**
   * Get variables of a process instanceId
   *
   * @param processInstanceId the process instance ID
   * @return variables attached to the process instance ID
   * @throws AutomatorException in case of error
   */
  Map<String, Object> getVariables(String processInstanceId) throws AutomatorException;

  /* ******************************************************************** */
  /*                                                                      */
  /*  CountInformation                                                    */
  /*                                                                      */
  /* ******************************************************************** */
  long countNumberOfProcessInstancesCreated(String processId, DateFilter startDate, DateFilter endDate)
      throws AutomatorException;

  long countNumberOfProcessInstancesEnded(String processId, DateFilter startDate, DateFilter endDate)
      throws AutomatorException;

  long countNumberOfTasks(String processId, String taskId) throws AutomatorException;

  /* ******************************************************************** */
  /*                                                                      */
  /*  Deployment                                                          */
  /*                                                                      */
  /* ******************************************************************** */

  /**
   * Deploy a BPMN file (may contains multiple processes)
   *
   * @param processFile process to deploy
   * @param policy      policy to deploy the process
   * @return the deploymentId
   * @throws AutomatorException in case of error
   */
  String deployBpmn(File processFile, ScenarioDeployment.Policy policy) throws AutomatorException;


  /* ******************************************************************** */
  /*                                                                      */
  /*  get server definition                                               */
  /*                                                                      */
  /* ******************************************************************** */

  ConfigurationBpmEngine.CamundaEngine getTypeCamundaEngine();

  /**
   * return the signature of the engine, to log it for example
   *
   * @return signature of the engine
   */
  String getSignature();

  class TaskDescription {
    public String processInstanceId;
    public String taskId;
    public ScenarioStep.Step type;
    public boolean isCompleted;
  }

  class ProcessDescription {
    public String processInstanceId;
  }
}
