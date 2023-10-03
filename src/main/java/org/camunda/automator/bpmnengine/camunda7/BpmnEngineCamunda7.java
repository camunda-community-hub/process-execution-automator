package org.camunda.automator.bpmnengine.camunda7;

import io.camunda.operate.search.DateFilter;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.configuration.BpmnEngineList;
import org.camunda.automator.definition.ScenarioDeployment;
import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.flow.FixedBackoffSupplier;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.backoff.ExponentialBackoffStrategy;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.community.rest.client.api.DeploymentApi;
import org.camunda.community.rest.client.api.EngineApi;
import org.camunda.community.rest.client.api.ExternalTaskApi;
import org.camunda.community.rest.client.api.ProcessDefinitionApi;
import org.camunda.community.rest.client.api.ProcessInstanceApi;
import org.camunda.community.rest.client.api.TaskApi;
import org.camunda.community.rest.client.api.VariableInstanceApi;
import org.camunda.community.rest.client.dto.CompleteExternalTaskDto;
import org.camunda.community.rest.client.dto.CompleteTaskDto;
import org.camunda.community.rest.client.dto.DeploymentWithDefinitionsDto;
import org.camunda.community.rest.client.dto.ExternalTaskDto;
import org.camunda.community.rest.client.dto.ExternalTaskQueryDto;
import org.camunda.community.rest.client.dto.LockExternalTaskDto;
import org.camunda.community.rest.client.dto.ProcessInstanceDto;
import org.camunda.community.rest.client.dto.ProcessInstanceQueryDto;
import org.camunda.community.rest.client.dto.ProcessInstanceQueryDtoSorting;
import org.camunda.community.rest.client.dto.ProcessInstanceWithVariablesDto;
import org.camunda.community.rest.client.dto.StartProcessInstanceDto;
import org.camunda.community.rest.client.dto.TaskDto;
import org.camunda.community.rest.client.dto.TaskQueryDto;
import org.camunda.community.rest.client.dto.TaskQueryDtoSorting;
import org.camunda.community.rest.client.dto.UserIdDto;
import org.camunda.community.rest.client.dto.VariableInstanceDto;
import org.camunda.community.rest.client.dto.VariableInstanceQueryDto;
import org.camunda.community.rest.client.dto.VariableValueDto;
import org.camunda.community.rest.client.invoker.ApiCallback;
import org.camunda.community.rest.client.invoker.ApiClient;
import org.camunda.community.rest.client.invoker.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * connection to a Camunda 7 server. This is one object created by the engine, and then one "init() " call.
 * After, all methods are call in a multi-threads environment
 */
public class BpmnEngineCamunda7 implements BpmnEngine {

  public static final int SEARCH_MAX_SIZE = 100;
  private final Logger logger = LoggerFactory.getLogger(BpmnEngineCamunda7.class);
  private final String serverUrl;
  private final String userName;
  private final String password;
  private final int workerMaxJobsActive;
  private final boolean logDebug;
  ApiClient apiClient = null;
  ProcessDefinitionApi processDefinitionApi;
  TaskApi taskApi;
  ExternalTaskApi externalTaskApi;
  ProcessInstanceApi processInstanceApi;
  VariableInstanceApi variableInstanceApi;
  DeploymentApi deploymentApi;
  EngineApi engineApi;
  private int count = 0;

  public BpmnEngineCamunda7(BpmnEngineList engineConfiguration, BpmnEngineList.BpmnServerDefinition serverDefinition) {
    this.serverUrl = serverDefinition.camunda7ServerUrl;
    this.userName = serverDefinition.camunda7UserName;
    this.password = serverDefinition.camunda7Password;
    this.workerMaxJobsActive = serverDefinition.workerMaxJobsActive;
    this.logDebug = engineConfiguration.getLogDebug();
    init();
  }

  /**
   * P
   *
   * @param serverUrl is  "http://localhost:8080/engine-rest"
   */
  public BpmnEngineCamunda7(String serverUrl, String userName, String password, boolean logDebug) {
    this.serverUrl = serverUrl;
    this.userName = userName;
    this.password = password;
    this.workerMaxJobsActive = 1;
    this.logDebug = logDebug;
    init();
  }

  @Override
  public void init() {
    apiClient = new ApiClient();
    apiClient.setBasePath(serverUrl);
    if (!userName.trim().isEmpty()) {
      apiClient.setUsername(userName);
      apiClient.setPassword(password);
    } else {
    }

    processDefinitionApi = new ProcessDefinitionApi(apiClient);

    taskApi = new TaskApi(apiClient);

    externalTaskApi = new ExternalTaskApi(apiClient);

    processInstanceApi = new ProcessInstanceApi(apiClient);

    variableInstanceApi = new VariableInstanceApi(apiClient);

    deploymentApi = new DeploymentApi(apiClient);

    engineApi = new EngineApi(apiClient);
  }

  public void connection() throws AutomatorException {
    count++;
    // we verify if we have the connection
    // logger.info("Connection to Camunda7 server[{}] User[{}] password[***]", serverUrl, userName);
    if (count > 2)
      return;
    try {
      engineApi.getProcessEngineNames();
      logger.info("Connection successfully to Camunda7 [{}] ", apiClient.getBasePath());
    } catch (ApiException e) {
      logger.error("Can't connect Camunda7 server[{}] User[{}]: {}", apiClient.getBasePath(), userName, e.toString());
      throw new AutomatorException("Can't connect to Camunda7 [" + apiClient.getBasePath() + "] : " + e);
    }
  }

  public void disconnection() throws AutomatorException {
    // nothing to do here
  }

  /**
   * Engine is ready. If not, a connection() method must be call
   *
   * @return
   */
  public boolean isReady() {
    if (count > 2)
      return true;

    try {
      engineApi.getProcessEngineNames();
    } catch (ApiException e) {
      // no need to log, connect will be called
      return false;
    }
    return true;
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
    String dateString = dateToString(new Date());

    Map<String, VariableValueDto> variablesApi = new HashMap<>();
    for (Map.Entry<String, Object> entry : variables.entrySet()) {
      variablesApi.put(entry.getKey(), new VariableValueDto().value(entry.getValue()));
    }
    try {
      ProcessInstanceWithVariablesDto processInstanceDto = processDefinitionApi.startProcessInstanceByKey(processId,
          new StartProcessInstanceDto().variables(variablesApi).businessKey(dateString));
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
  public List<String> searchUserTasksByProcessInstance(String processInstanceId, String userTaskId, int maxResult)
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
  public List<String> searchUserTasks(String userTaskId, int maxResult) throws AutomatorException {
    if (logDebug) {
      logger.info("BpmnEngine7.searchForActivity: taskName[" + userTaskId + "]");
    }

    TaskQueryDto taskQueryDto = new TaskQueryDto();
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
  @Override
  public RegisteredTask registerServiceTask(String workerId,
                                            String topic,
                                            Duration lockTime,
                                            Object jobHandler,
                                            FixedBackoffSupplier backoffSupplier) {

    if (!(jobHandler instanceof ExternalTaskHandler)) {
      logger.error("handler is not a externalTaskHandler implementation, can't register the worker [{}], topic [{}]",
          workerId, topic);
      return null;
    }
    RegisteredTask registeredTask = new RegisteredTask();

    ExternalTaskClient client = ExternalTaskClient.create()
        .baseUrl(serverUrl)
        .workerId(workerId)
        .maxTasks(workerMaxJobsActive < 1 ? 1 : workerMaxJobsActive)
        .lockDuration(lockTime.toMillis())
        .asyncResponseTimeout(20000)
        .backoffStrategy(new ExponentialBackoffStrategy())
        .build();

    registeredTask.topicSubscription = client.subscribe(topic)
        .lockDuration(10000)
        .handler((ExternalTaskHandler) jobHandler)
        .open();
    return registeredTask;

  }

  /**
   * Search service task
   *
   * @param processInstanceId processInstance
   * @param serviceTaskId     task name
   * @param topic             topic to search the task
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
    return taskDtos.stream().map(t -> {
      TaskDescription taskDescription = new TaskDescription();
      taskDescription.taskId = t.getName();
      taskDescription.type = ScenarioStep.Step.USERTASK;
      taskDescription.isCompleted = true;
      return taskDescription;
    }).toList();
  }

  @Override
  public List<ProcessDescription> searchProcessInstanceByVariable(String processId,
                                                                  Map<String, Object> filterVariables,
                                                                  int maxResult) throws AutomatorException {
    return Collections.emptyList();
  }

  @Override
  public Map<String, Object> getVariables(String processInstanceId) throws AutomatorException {

    VariableInstanceQueryDto variableQuery = new VariableInstanceQueryDto();
    variableQuery.processInstanceIdIn(List.of(processInstanceId));
    try {
      List<VariableInstanceDto> variableInstanceDtos = variableInstanceApi.queryVariableInstances(0, 1000, true,
          variableQuery);

      Map<String, Object> variables = new HashMap<>();
      for (VariableInstanceDto variable : variableInstanceDtos) {
        variables.put(variable.getName(), variable.getValue());
      }
      return variables;
    } catch (ApiException e) {
      throw new AutomatorException("Can't searchVariables", e);
    }
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  CountInformation                                                    */
  /*                                                                      */
  /* ******************************************************************** */

  @Override
  public long countNumberOfProcessInstancesCreated(String processName, DateFilter startDate, DateFilter endDate)
      throws AutomatorException {

    try {
      int cumul = 0;
      ProcessInstanceQueryDto processInstanceQuery = new ProcessInstanceQueryDto();
      processInstanceQuery = processInstanceQuery.addProcessDefinitionKeyInItem(processName);
      processInstanceQuery.addSortingItem(
          new ProcessInstanceQueryDtoSorting().sortBy(ProcessInstanceQueryDtoSorting.SortByEnum.INSTANCEID)
              .sortOrder(ProcessInstanceQueryDtoSorting.SortOrderEnum.ASC));

      int maxLoop = 0;
      int firstResult = 0;
      List<ProcessInstanceDto> processInstanceDtos;
      do {
        maxLoop++;
        processInstanceDtos = processInstanceApi.queryProcessInstances(firstResult, SEARCH_MAX_SIZE,
            processInstanceQuery);
        firstResult += processInstanceDtos.size();
        cumul += processInstanceDtos.stream().filter(t -> {
          Date datePI = stringToDate(t.getBusinessKey());
          if (datePI == null)
            return false;
          return datePI.after(startDate.getDate());
        }).count();

      } while (processInstanceDtos.size() >= SEARCH_MAX_SIZE && maxLoop < 1000);
      return cumul;

    } catch (Exception e) {
      throw new AutomatorException("Error during countNumberOfProcessInstancesCreated");

    }
  }

  @Override
  public long countNumberOfProcessInstancesEnded(String processName, DateFilter startDate, DateFilter endDate)
      throws AutomatorException {
    throw new AutomatorException("Not yet implemented");
  }

  public long countNumberOfTasks(String processId, String taskId) throws AutomatorException {
    try {
      int cumul = 0;
      TaskQueryDto taskQueryDto = new TaskQueryDto();
      taskQueryDto = taskQueryDto.addProcessDefinitionKeyInItem(processId);
      taskQueryDto.addSortingItem(new TaskQueryDtoSorting().sortBy(TaskQueryDtoSorting.SortByEnum.INSTANCEID)
          .sortOrder(TaskQueryDtoSorting.SortOrderEnum.ASC));

      int maxLoop = 0;
      int firstResult = 0;
      List<TaskDto> taskDtos;
      do {
        maxLoop++;
        taskDtos = taskApi.queryTasks(firstResult, SEARCH_MAX_SIZE, taskQueryDto);

        firstResult += taskDtos.size();
        cumul += taskDtos.size();

      } while (taskDtos.size() >= SEARCH_MAX_SIZE && maxLoop < 1000);
      return cumul;

    } catch (Exception e) {
      throw new AutomatorException("Error during countNumberOfTasks");

    }
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
  public BpmnEngineList.CamundaEngine getTypeCamundaEngine() {
    return BpmnEngineList.CamundaEngine.CAMUNDA_7;
  }

  @Override
  public String getSignature() {
    return BpmnEngineList.CamundaEngine.CAMUNDA_7 + " " + "serverUrl[" + serverUrl + "]";
  }

  @Override
  public int getWorkerExecutionThreads() {
    return workerMaxJobsActive;
  }

  public void turnHighFlowMode(boolean hightFlowMode) {
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

  private String dateToString(Date date) {
    return String.valueOf(date.getTime());
  }

  private Date stringToDate(String dateSt) {
    if (dateSt == null)
      return null;
    return new Date(Long.valueOf(dateSt));
  }

  /**
   * Call back asynchronous
   */
  public static class ExternalCallBack implements ApiCallback {

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

    public enum STATUS {WAIT, FAILURE, SUCCESS}
  }
}
