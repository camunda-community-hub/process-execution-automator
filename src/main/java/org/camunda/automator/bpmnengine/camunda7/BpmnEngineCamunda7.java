package org.camunda.automator.bpmnengine.camunda7;

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.bpmnengine.BpmnEngineConfiguration;
import org.camunda.automator.definition.ScenarioDeployment;
import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.community.rest.client.api.DeploymentApi;
import org.camunda.community.rest.client.api.ExternalTaskApi;
import org.camunda.community.rest.client.api.ProcessDefinitionApi;
import org.camunda.community.rest.client.api.ProcessInstanceApi;
import org.camunda.community.rest.client.api.TaskApi;
import org.camunda.community.rest.client.dto.CompleteExternalTaskDto;
import org.camunda.community.rest.client.dto.CompleteTaskDto;
import org.camunda.community.rest.client.dto.DeploymentWithDefinitionsDto;
import org.camunda.community.rest.client.dto.ExternalTaskDto;
import org.camunda.community.rest.client.dto.ExternalTaskQueryDto;
import org.camunda.community.rest.client.dto.LockExternalTaskDto;
import org.camunda.community.rest.client.dto.ProcessInstanceDto;
import org.camunda.community.rest.client.dto.ProcessInstanceQueryDto;
import org.camunda.community.rest.client.dto.ProcessInstanceWithVariablesDto;
import org.camunda.community.rest.client.dto.StartProcessInstanceDto;
import org.camunda.community.rest.client.dto.TaskDto;
import org.camunda.community.rest.client.dto.TaskQueryDto;
import org.camunda.community.rest.client.dto.UserIdDto;
import org.camunda.community.rest.client.dto.VariableValueDto;
import org.camunda.community.rest.client.invoker.ApiCallback;
import org.camunda.community.rest.client.invoker.ApiClient;
import org.camunda.community.rest.client.invoker.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * connection to a Camunda 7 server. This is one object created by the engine, and then one "init() " call.
 * After, all methods are call in a multi-threads environment
 */
public class BpmnEngineCamunda7 implements BpmnEngine {

  private final Logger logger = LoggerFactory.getLogger(BpmnEngineCamunda7.class);

  private final String serverUrl;
  private final boolean logDebug;

  ApiClient client = null;
  ProcessDefinitionApi processDefinitionApi;
  TaskApi taskApi;
  ExternalTaskApi externalTaskApi;
  ProcessInstanceApi processInstanceApi;
  DeploymentApi deploymentApi;

  public BpmnEngineCamunda7(BpmnEngineConfiguration engineConfiguration,
                            BpmnEngineConfiguration.BpmnServerDefinition serverDefinition) {
    this.serverUrl = serverDefinition.serverUrl;
    this.logDebug = engineConfiguration.logDebug;
    init();
  }

  @Override
  public void init() {
    client = new ApiClient();
    client.setBasePath(serverUrl);
    processDefinitionApi = new ProcessDefinitionApi();
    taskApi = new TaskApi();
    externalTaskApi = new ExternalTaskApi();
    processInstanceApi = new ProcessInstanceApi();
    deploymentApi = new DeploymentApi();
  }

  /**
   * P
   *
   * @param serverUrl is  "http://localhost:8080/engine-rest"
   */
  public BpmnEngineCamunda7(String serverUrl, boolean logDebug) {
    this.serverUrl = serverUrl;
    this.logDebug = logDebug;
    init();
  }


  /* ******************************************************************** */
  /*                                                                      */
  /*  Process Instance                                                    */
  /*                                                                      */
  /* ******************************************************************** */

  @Override
  public String createProcessInstance(String processId, String starterEventId, Map<String, Object> variables)
      throws AutomatorException {
    if (logDebug) {
      logger.info("BpmnEngine7.CreateProcessInstance: Process[" + processId + "] StartEvent[" + starterEventId + "]");
    }
    Map<String, VariableValueDto> variablesApi = new HashMap<>();
    for (Map.Entry<String, Object> entry : variables.entrySet()) {
      variablesApi.put(entry.getKey(), new VariableValueDto().value(entry.getValue()));
    }
    try {
      ProcessInstanceWithVariablesDto processInstanceDto = processDefinitionApi.startProcessInstanceByKey(processId,
          new StartProcessInstanceDto().variables(variablesApi));
      return processInstanceDto.getId();
    } catch (ApiException e) {
      throw new AutomatorException(
          "Can't create process instance in [" + processId + "] StartEvent[" + starterEventId + "]", e);
    }
  }

  @Override
  public void endProcessInstance(String processInstanceId, boolean cleanAll) throws AutomatorException {
    // To nothing at this moment
  }


  /* ******************************************************************** */
  /*                                                                      */
  /*  User task                                                           */
  /*                                                                      */
  /* ******************************************************************** */

  @Override
  public List<String> searchUserTasks(String processInstanceId, String userTaskId, int maxResult)
      throws AutomatorException {
    if (logDebug) {
      logger.info("BpmnEngine7.searchForActivity: Process[" + processInstanceId + "] taskName[" + userTaskId + "]");
    }

    // get the list of all sub process instance
    List<String> listProcessInstance = getListSubProcessInstance(processInstanceId);

    TaskQueryDto taskQueryDto = new TaskQueryDto();
    taskQueryDto.addProcessInstanceIdInItem(processInstanceId);
    for (String subProcessInstance : listProcessInstance) {
      taskQueryDto.addProcessInstanceIdInItem(subProcessInstance);

    }
    taskQueryDto.addTaskDefinitionKeyInItem(userTaskId);
    List<TaskDto> taskDtos = null;
    try {
      taskDtos = taskApi.queryTasks(0, maxResult, taskQueryDto);
    } catch (ApiException e) {
      throw new AutomatorException("Can't searchTask", e);
    }
    return taskDtos.stream().map(TaskDto::getId).toList();
  }

  @Override
  public void executeUserTask(String userTaskId, String userId, Map<String, Object> variables)
      throws AutomatorException {

    if (logDebug) {
      logger.info("BpmnEngine7.executeUserTask: activityId[" + userTaskId + "]");
    }
    try {
      UserIdDto userIdDto = new UserIdDto();
      userIdDto.setUserId(userId == null ? "automator" : userId);
      taskApi.claim(userTaskId, userIdDto);
      Map<String, VariableValueDto> variablesApi = new HashMap<>();
      for (Map.Entry<String, Object> entry : variables.entrySet()) {
        variablesApi.put(entry.getKey(), new VariableValueDto().value(entry.getValue()));
      }

      taskApi.complete(userTaskId, new CompleteTaskDto().variables(variablesApi));
    } catch (ApiException e) {
      throw new AutomatorException("Can't execute taskId[" + userTaskId + "] with userId[" + userId + "]", e);
    }
  }


  /* ******************************************************************** */
  /*                                                                      */
  /*  Service task                                                        */
  /*                                                                      */
  /* ******************************************************************** */

  /**
   * Search service task
   *
   * @param processInstanceId processInstance
   * @param serviceTaskId     task name
   * @param topic topic to search the task
   * @param maxResult         number of result
   * @return the list of TaskId found according the criteria
   * @throws AutomatorException any error during search
   */
  @Override
  public List<String> searchServiceTasks(String processInstanceId, String serviceTaskId, String topic, int maxResult)
      throws AutomatorException {
    if (logDebug) {
      logger.info("BpmnEngine7.searchForActivity: Process[" + processInstanceId + "] taskName[" + serviceTaskId + "]");
    }

    // get the list of all sub process instance
    List<String> listProcessInstance = getListSubProcessInstance(processInstanceId);

    ExternalTaskQueryDto externalTaskQueryDto = new ExternalTaskQueryDto();
    externalTaskQueryDto.addProcessInstanceIdInItem(processInstanceId);
    for (String subProcessInstance : listProcessInstance) {
      externalTaskQueryDto.addProcessInstanceIdInItem(subProcessInstance);

    }

    externalTaskQueryDto.activityId(serviceTaskId);
    List<ExternalTaskDto> taskDtos;
    try {
      taskDtos = externalTaskApi.queryExternalTasks(0, 100, externalTaskQueryDto);
    } catch (ApiException e) {
      throw new AutomatorException("Can't searchTask", e);
    }
    return taskDtos.stream().map(ExternalTaskDto::getId).toList();
  }

  @Override
  public void executeServiceTask(String serviceTaskId, String userId, Map<String, Object> variables)
      throws AutomatorException {

    if (logDebug) {
      logger.info("BpmnEngine7.executeUserTask: activityId[" + serviceTaskId + "]");
    }
    try {

      // Fetch and lock
      String workerId = getUniqWorkerId();
      externalTaskApi.lock(serviceTaskId, new LockExternalTaskDto().workerId(workerId).lockDuration(10000L));

      Map<String, VariableValueDto> variablesApi = new HashMap<>();
      for (Map.Entry<String, Object> entry : variables.entrySet()) {
        variablesApi.put(entry.getKey(), new VariableValueDto().value(entry.getValue()));
      }

      ExternalCallBack externalCallBack = new ExternalCallBack();
      externalTaskApi.completeExternalTaskResourceAsync(serviceTaskId,
          new CompleteExternalTaskDto().variables(variablesApi).workerId(workerId), externalCallBack);

      int counter = 0;
      while (ExternalCallBack.STATUS.WAIT.equals(externalCallBack.status) && counter < 200) {
        counter++;
        try {
          Thread.sleep(200);
        } catch (InterruptedException e) {
          // we don't care
        }
      }
      if (!ExternalCallBack.STATUS.SUCCESS.equals(externalCallBack.status)) {
        throw new AutomatorException(
            "Can't execute taskId[" + serviceTaskId + "] - answer[" + externalCallBack.status + "]");
      }
    } catch (ApiException e) {
      throw new AutomatorException("Can't execute taskId[" + serviceTaskId + "] with userId[" + userId + "]", e);
    }
  }



  /* ******************************************************************** */
  /*                                                                      */
  /*  Generic task                                                        */
  /*                                                                      */
  /* ******************************************************************** */

  @Override
  public List<TaskDescription> searchTasksByProcessInstanceId(String processInstanceId, String taskId, int maxResult)
      throws AutomatorException {
    // get the list of all sub process instance
    List<String> listProcessInstance = getListSubProcessInstance(processInstanceId);

    TaskQueryDto taskQueryDto = new TaskQueryDto();
    taskQueryDto.addProcessInstanceIdInItem(processInstanceId);
    for (String subProcessInstance : listProcessInstance) {
      taskQueryDto.addProcessInstanceIdInItem(subProcessInstance);

    }
    taskQueryDto.addTaskDefinitionKeyInItem(taskId);
    List<TaskDto> taskDtos = null;
    try {
      taskDtos = taskApi.queryTasks(0, maxResult, taskQueryDto);
    } catch (ApiException e) {
      throw new AutomatorException("Can't searchTask", e);
    }
    List<TaskDescription> taskList = taskDtos.stream().map(t -> {
      TaskDescription taskDescription = new TaskDescription();
      taskDescription.taskId = t.getName();
      taskDescription.type = ScenarioStep.Step.USERTASK;
      taskDescription.isCompleted = true;
      return taskDescription;
    }).toList();
    return taskList;
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


  /* ******************************************************************** */
  /*                                                                      */
  /*  Deployment                                                          */
  /*                                                                      */
  /* ******************************************************************** */

  @Override
  public String deployBpmn(File processFile, ScenarioDeployment.Policy policy) throws AutomatorException {
    try {
      DeploymentWithDefinitionsDto deploymentSource = deploymentApi.createDeployment(null, // tenantId
          null, // deploymentSource
          ScenarioDeployment.Policy.ONLYNOTEXIST.equals(policy), // deployChangedOnly,
          Boolean.TRUE, // enableDuplicateFiltering,
          processFile.getName(), // String deploymentName,
          new Date(), //deploymentActivationTime,
          processFile);
      return deploymentSource.getId();
    } catch (ApiException e) {
      throw new AutomatorException("Can't deploy process ", e);
    }

  }



  /* ******************************************************************** */
  /*                                                                      */
  /*  get server definition                                               */
  /*                                                                      */
  /* ******************************************************************** */

  @Override
  public BpmnEngineConfiguration.CamundaEngine getServerDefinition() {
    return BpmnEngineConfiguration.CamundaEngine.CAMUNDA_7;
  }

  /**
   * Call back asynchronous
   */
  public class ExternalCallBack implements ApiCallback {

    public enum STATUS {WAIT, FAILURE, SUCCESS}

    public STATUS status = STATUS.WAIT;
    public ApiException e;

    @Override
    public void onFailure(ApiException e, int i, Map map) {
      this.status = STATUS.FAILURE;
      this.e = e;
    }

    @Override
    public void onSuccess(Object o, int i, Map map) {
      this.status = STATUS.SUCCESS;
    }

    @Override
    public void onUploadProgress(long l, long l1, boolean b) {

    }

    @Override
    public void onDownloadProgress(long l, long l1, boolean b) {

    }
  }

  private String getUniqWorkerId() {
    return Thread.currentThread().getName() + "-" + System.currentTimeMillis();
  }

  /**
   * Collect all subprocess for a process instance
   *
   * @param rootProcessInstance root process instance
   * @return list of SubProcess ID
   * @throws AutomatorException if any errors arrive
   */
  private List<String> getListSubProcessInstance(String rootProcessInstance) throws AutomatorException {
    ProcessInstanceQueryDto processInstanceQueryDto = new ProcessInstanceQueryDto();
    processInstanceQueryDto.superProcessInstance(rootProcessInstance);
    List<ProcessInstanceDto> processInstanceDtos;
    try {
      processInstanceDtos = processInstanceApi.queryProcessInstances(0, 100000, processInstanceQueryDto);
    } catch (ApiException e) {
      throw new AutomatorException("Can't searchSubProcess", e);
    }
    return processInstanceDtos.stream().map(ProcessInstanceDto::getId).toList();
  }
}
