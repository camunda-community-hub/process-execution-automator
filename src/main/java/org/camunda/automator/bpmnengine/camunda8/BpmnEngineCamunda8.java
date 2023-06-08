package org.camunda.automator.bpmnengine.camunda8;

import io.camunda.operate.CamundaOperateClient;
import io.camunda.operate.dto.FlownodeInstance;
import io.camunda.operate.dto.FlownodeInstanceState;
import io.camunda.operate.dto.ProcessInstance;
import io.camunda.operate.dto.ProcessInstanceState;
import io.camunda.operate.dto.SearchResult;
import io.camunda.operate.exception.OperateException;
import io.camunda.operate.search.DateFilter;
import io.camunda.operate.search.FlownodeInstanceFilter;
import io.camunda.operate.search.ProcessInstanceFilter;
import io.camunda.operate.search.SearchQuery;
import io.camunda.operate.search.Sort;
import io.camunda.operate.search.SortOrder;
import io.camunda.operate.search.VariableFilter;
import io.camunda.tasklist.CamundaTaskListClient;
import io.camunda.tasklist.dto.Pagination;
import io.camunda.tasklist.dto.Task;
import io.camunda.tasklist.dto.TaskList;
import io.camunda.tasklist.dto.TaskSearch;
import io.camunda.tasklist.dto.TaskState;
import io.camunda.tasklist.dto.Variable;
import io.camunda.tasklist.exception.TaskListException;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import io.camunda.zeebe.client.api.command.FinalCommandStep;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.bpmnengine.camunda8.refactoring.RefactoredCommandWrapper;
import org.camunda.automator.configuration.ConfigurationBpmEngine;
import org.camunda.automator.definition.ScenarioDeployment;
import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.AutomatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class BpmnEngineCamunda8 implements BpmnEngine {

  public static final String THIS_IS_A_COMPLETE_IMPOSSIBLE_VARIABLE_NAME = "ThisIsACompleteImpossibleVariableName";
  public static final int SEARCH_MAX_SIZE = 100;
  private final Logger logger = LoggerFactory.getLogger(BpmnEngineCamunda8.class);

  private final ConfigurationBpmEngine.BpmnServerDefinition serverDefinition;
  boolean hightFlowMode = false;
  /**
   * It is not possible to search user task for a specfic processInstance. So, to realize this, a marker is created in each process instance. Retrieving the user task,
   * the process instance can be found and correction can be done
   */
  Map<String, Long> cacheProcessInstanceMarker = new HashMap<>();
  Random random = new Random(System.currentTimeMillis());
  private ZeebeClient zeebeClient;
  private CamundaOperateClient operateClient;
  private CamundaTaskListClient taskClient;
  @Autowired
  private BenchmarkStartPiExceptionHandlingStrategy exceptionHandlingStrategy;
  private ConfigurationBpmEngine.CamundaEngine typeCamundaEngine;

  /**
   * Constructor from existing object
   *
   * @param engineConfiguration configuration for this engine
   * @param serverDefinition    server definition
   */
  public BpmnEngineCamunda8(ConfigurationBpmEngine engineConfiguration,
                            ConfigurationBpmEngine.BpmnServerDefinition serverDefinition) {
    this.serverDefinition = serverDefinition;

  }

  /**
   * Constructor to specify a Self Manage Zeebe Address por a Zeebe Saas
   *
   * @param zeebeSelfGatewayAddress    Self Manage : zeebe address
   * @param zeebeSelfSecurityPlainText Self Manage: Plain text
   * @param zeebeSaasCloudRegister     Saas Cloud Register information
   * @param zeebeSaasCloudRegion       Saas Cloud region
   * @param zeebeSaasCloudClusterId    Saas Cloud ClusterID
   * @param zeebeSaasCloudClientId     Saas Cloud ClientID
   * @param zeebeSaasClientSecret      Saas Cloud Client Secret
   * @param operateUrl                 URL to access Operate
   * @param operateUserName            Operate user name
   * @param operateUserPassword        Operate password
   * @param tasklistUrl                Url to access TaskList
   */
  public BpmnEngineCamunda8(String zeebeSelfGatewayAddress,
                            String zeebeSelfSecurityPlainText,
                            String zeebeSaasCloudRegister,
                            String zeebeSaasCloudRegion,
                            String zeebeSaasCloudClusterId,
                            String zeebeSaasCloudClientId,
                            String zeebeSaasClientSecret,
                            String operateUrl,
                            String operateUserName,
                            String operateUserPassword,
                            String tasklistUrl) {
    this.serverDefinition = new ConfigurationBpmEngine.BpmnServerDefinition();
    this.serverDefinition.zeebeGatewayAddress = zeebeSelfGatewayAddress;
    this.serverDefinition.zeebeSecurityPlainText = zeebeSelfSecurityPlainText;

    /*
     * SaaS Zeebe
     */
    this.serverDefinition.zeebeCloudRegister = zeebeSaasCloudRegister;
    this.serverDefinition.zeebeCloudRegion = zeebeSaasCloudRegion;
    this.serverDefinition.zeebeCloudClusterId = zeebeSaasCloudClusterId;
    this.serverDefinition.zeebeCloudClientId = zeebeSaasCloudClientId;
    this.serverDefinition.clientSecret = zeebeSaasClientSecret;

    /*
     * Connection to Operate
     */
    this.serverDefinition.operateUserName = operateUserName;
    this.serverDefinition.operateUserPassword = operateUserPassword;
    this.serverDefinition.operateUrl = operateUrl;
    this.serverDefinition.taskListUrl = tasklistUrl;

  }

  @Override
  public void init() throws AutomatorException {

    final String defaultAddress = "localhost:26500";
    final String envVarAddress = System.getenv("ZEEBE_ADDRESS");

    // connection is critical, so let build the analysis
    StringBuilder analysis = new StringBuilder();
    analysis.append("ZeebeConnection: ");
    this.typeCamundaEngine = ConfigurationBpmEngine.CamundaEngine.CAMUNDA_8;
    if (this.serverDefinition.zeebeCloudRegister != null && !this.serverDefinition.zeebeCloudRegister.trim().isEmpty())
      this.typeCamundaEngine = ConfigurationBpmEngine.CamundaEngine.CAMUNDA_8_SAAS;

    final ZeebeClientBuilder clientBuilder;
    io.camunda.operate.auth.AuthInterface saOperate;
    io.camunda.tasklist.auth.AuthInterface saTaskList;

    if (this.serverDefinition.zeebeCloudRegister != null && !this.serverDefinition.zeebeCloudRegister.trim()
        .isEmpty()) {
      analysis.append("Saas ClientId[");
      analysis.append(serverDefinition.zeebeCloudClientId);
      analysis.append("]");
      /* Connect to Camunda Cloud Cluster, assumes that credentials are set in environment variables.
       * See JavaDoc on class level for details
       */
      clientBuilder = ZeebeClient.newClientBuilder();
      saOperate = new io.camunda.operate.auth.SaasAuthentication(serverDefinition.zeebeCloudClientId,
          serverDefinition.clientSecret);
      saTaskList = new io.camunda.tasklist.auth.SaasAuthentication(serverDefinition.zeebeCloudClientId,
          serverDefinition.clientSecret);

      typeCamundaEngine = ConfigurationBpmEngine.CamundaEngine.CAMUNDA_8_SAAS;

      // Camunda 8 Self Manage
    } else if (serverDefinition.zeebeGatewayAddress != null && !this.serverDefinition.zeebeGatewayAddress.trim()
        .isEmpty()) {
      analysis.append("GatewayAddress [");
      analysis.append(serverDefinition.zeebeGatewayAddress);
      analysis.append("]");

      // connect to local deployment; assumes that authentication is disabled
      clientBuilder = ZeebeClient.newClientBuilder()
          .gatewayAddress(serverDefinition.zeebeGatewayAddress)
          .usePlaintext();
      saOperate = new io.camunda.operate.auth.SimpleAuthentication(serverDefinition.operateUserName,
          serverDefinition.operateUserPassword, serverDefinition.operateUrl);
      saTaskList = new io.camunda.tasklist.auth.SimpleAuthentication(serverDefinition.operateUserName,
          serverDefinition.operateUserPassword);
      typeCamundaEngine = ConfigurationBpmEngine.CamundaEngine.CAMUNDA_8;
    } else
      throw new AutomatorException("Invalid configuration");

    try {
      analysis.append("ExecutionThread[");
      analysis.append(serverDefinition.workerExecutionThreads);
      analysis.append("] MaxJobsActive[");
      analysis.append(serverDefinition.workerMaxJobsActive);
      analysis.append("] ");
      clientBuilder.numJobWorkerExecutionThreads(serverDefinition.workerExecutionThreads);
      clientBuilder.defaultJobWorkerMaxJobsActive(serverDefinition.workerMaxJobsActive);
      zeebeClient = clientBuilder.build();
      analysis.append("Zeebe connection with success,");

      operateClient = new CamundaOperateClient.Builder().operateUrl(serverDefinition.operateUrl)
          .authentication(saOperate)
          .build();
      analysis.append("OperateConnection with success,");

      // TaskList is not mandatory
      if (serverDefinition.taskListUrl != null && !serverDefinition.taskListUrl.isEmpty()) {
        taskClient = new CamundaTaskListClient.Builder().taskListUrl(serverDefinition.taskListUrl)
            .authentication(saTaskList)
            .build();
        analysis.append("TasklistConnection with success,");
      }
      //get tasks assigned to demo
      logger.info(analysis.toString());

    } catch (Exception e) {
      throw new AutomatorException("Can't connect to Zeebe " + e.getMessage() + " - Analysis:" + analysis);
    }
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  Manage process instance                                             */
  /*                                                                      */
  /* ******************************************************************** */

  /**
   * HighFlowMode: when true, the class does not save anything, to reduce the footprint
   *
   * @param hightFlowMode true or false
   */
  public void turnHighFlowMode(boolean hightFlowMode) {
    this.hightFlowMode = hightFlowMode;
  }

  @Override
  public String createProcessInstance(String processId, String starterEventId, Map<String, Object> variables)
      throws AutomatorException {
    try {
      String marker = null;
      if (!hightFlowMode) {
        marker = getUniqueMarker(processId, starterEventId);
        variables.put(THIS_IS_A_COMPLETE_IMPOSSIBLE_VARIABLE_NAME, marker);
      }

      FinalCommandStep createCommand = zeebeClient.newCreateInstanceCommand()
          .bpmnProcessId(processId)
          .latestVersion()
          .variables(variables);
      RefactoredCommandWrapper command = new RefactoredCommandWrapper(createCommand,
          System.currentTimeMillis() + 5 * 60 * 1000,
          // 5 minutes
          "CreatePi" + processId, exceptionHandlingStrategy);

      ProcessInstanceEvent workflowInstanceEvent = (ProcessInstanceEvent) command.executeSync();
      Long processInstanceId = workflowInstanceEvent.getProcessInstanceKey();
      if (!hightFlowMode) {
        cacheProcessInstanceMarker.put(marker, processInstanceId);
      }
      return String.valueOf(processInstanceId);
    } catch (Exception e) {
      throw new AutomatorException("Can't create in process [" + processId + "] :" + e.getMessage());
    }
  }

  public String createProcessInstanceSimple(String processId, String starterEventId, Map<String, Object> variables)
      throws AutomatorException {
    try {
      String marker = null;
      if (!hightFlowMode) {
        marker = getUniqueMarker(processId, starterEventId);
        variables.put(THIS_IS_A_COMPLETE_IMPOSSIBLE_VARIABLE_NAME, marker);
      }

      ProcessInstanceEvent workflowInstanceEvent = zeebeClient.newCreateInstanceCommand()
          .bpmnProcessId(processId)
          .latestVersion()
          .variables(variables)
          .send()
          .join();
      Long processInstanceId = workflowInstanceEvent.getProcessInstanceKey();
      if (!hightFlowMode) {
        cacheProcessInstanceMarker.put(marker, processInstanceId);
      }
      return String.valueOf(processInstanceId);
    } catch (Exception e) {
      throw new AutomatorException("Can't create in process [" + processId + "] :" + e.getMessage());
    }
  }


  /* ******************************************************************** */
  /*                                                                      */
  /*  User tasks                                                          */
  /*                                                                      */
  /* ******************************************************************** */

  @Override
  public void endProcessInstance(String processInstanceId, boolean cleanAll) throws AutomatorException {
    // clean in the cache
    List<String> markers = cacheProcessInstanceMarker.entrySet()
        .stream()
        .filter(t -> t.getValue().equals(Long.valueOf(processInstanceId)))
        .map(Map.Entry::getKey)
        .toList();
    markers.forEach(t -> cacheProcessInstanceMarker.remove(t));

  }

  @Override
  public List<String> searchUserTasks(String processInstanceId, String userTaskId, int maxResult)
      throws AutomatorException {
    try {
      // impossible to filter by the task name/ task type, so be ready to get a lot of flowNode and search the correct one
      Long processInstanceIdLong = Long.valueOf(processInstanceId);

      TaskSearch taskSearch = new TaskSearch();
      taskSearch.setState(TaskState.CREATED);
      taskSearch.setAssigned(Boolean.FALSE);
      taskSearch.setWithVariables(true);
      taskSearch.setPagination(new Pagination().setPageSize(maxResult));

      TaskList tasksList = taskClient.getTasks(taskSearch);
      List<String> listTasksResult = new ArrayList<>();
      do {
        listTasksResult.addAll(tasksList.getItems().stream().filter(t -> {
              List<Variable> listVariables = t.getVariables();
              Optional<Variable> markerTask = listVariables.stream()
                  .filter(v -> v.getName().equals(THIS_IS_A_COMPLETE_IMPOSSIBLE_VARIABLE_NAME))
                  .findFirst();

              if (markerTask.isEmpty())
                return false;
              Long processInstanceIdTask = cacheProcessInstanceMarker.get(markerTask.get().getValue());
              return (processInstanceIdLong.equals(processInstanceIdTask));
            }).map(Task::getId) // Task to ID
            .toList());

        tasksList = taskClient.after(tasksList);
      } while (tasksList.size() > 0);

      return listTasksResult;

    } catch (TaskListException e) {
      throw new AutomatorException("Can't search users task " + e.getMessage());
    }
  }



  /* ******************************************************************** */
  /*                                                                      */
  /*  Service tasks                                                       */
  /*                                                                      */
  /* ******************************************************************** */

  @Override
  public void executeUserTask(String userTaskId, String userId, Map<String, Object> variables)
      throws AutomatorException {
    try {
      taskClient.claim(userTaskId, serverDefinition.operateUserName);
      taskClient.completeTask(userTaskId, variables);
    } catch (TaskListException e) {
      throw new AutomatorException("Can't execute task [" + userTaskId + "]");
    }
  }

  @Override
  public List<String> searchServiceTasks(String processInstanceId, String serviceTaskId, String topic, int maxResult)
      throws AutomatorException {
    try {
      long processInstanceIdLong = Long.parseLong(processInstanceId);
      ActivateJobsResponse jobsResponse = zeebeClient.newActivateJobsCommand()
          .jobType(topic)
          .maxJobsToActivate(10000)
          .workerName(Thread.currentThread().getName())
          .send()
          .join();
      List<String> listJobsId = new ArrayList<>();

      for (ActivatedJob job : jobsResponse.getJobs()) {
        if (job.getProcessInstanceKey() == processInstanceIdLong)
          listJobsId.add(String.valueOf(job.getKey()));
        else {
          zeebeClient.newFailCommand(job.getKey()).retries(2).send().join();
        }
      }
      return listJobsId;

    } catch (Exception e) {
      throw new AutomatorException("Can't search users task " + e.getMessage());
    }
  }




  /* ******************************************************************** */
  /*                                                                      */
  /*  generic search                                                       */
  /*                                                                      */
  /* ******************************************************************** */

  @Override
  public void executeServiceTask(String serviceTaskId, String workerId, Map<String, Object> variables)
      throws AutomatorException {
    try {
      zeebeClient.newCompleteCommand(Long.valueOf(serviceTaskId)).send().join();
    } catch (Exception e) {
      throw new AutomatorException("Can't execute service task " + e.getMessage());
    }
  }

  @Override
  public List<TaskDescription> searchTasksByProcessInstanceId(String processInstanceId, String taskId, int maxResult)
      throws AutomatorException {
    try {
      // impossible to filter by the task name/ task tyoe, so be ready to get a lot of flowNode and search the correct onee
      FlownodeInstanceFilter flownodeFilter = new FlownodeInstanceFilter.Builder().processInstanceKey(
          Long.valueOf(processInstanceId)).build();

      SearchQuery flownodeQuery = new SearchQuery.Builder().filter(flownodeFilter).size(maxResult).build();
      List<FlownodeInstance> flownodes = operateClient.searchFlownodeInstances(flownodeQuery);
      return flownodes.stream().filter(t -> taskId.equals(t.getFlowNodeId())).map(t -> {
        TaskDescription taskDescription = new TaskDescription();
        taskDescription.taskId = t.getFlowNodeId();
        taskDescription.type = getTaskType(t.getType()); // to implement
        taskDescription.isCompleted = FlownodeInstanceState.COMPLETED.equals(t.getState()); // to implement
        return taskDescription;
      }).toList();

    } catch (OperateException e) {
      throw new AutomatorException("Can't search users task " + e.getMessage());
    }
  }

  public List<ProcessDescription> searchProcessInstanceByVariable(String processId,
                                                                  Map<String, Object> filterVariables,
                                                                  int maxResult) throws AutomatorException {
    try {
      // impossible to filter by the task name/ task tyoe, so be ready to get a lot of flowNode and search the correct onee
      ProcessInstanceFilter processInstanceFilter = new ProcessInstanceFilter.Builder().bpmnProcessId(processId)
          .build();

      SearchQuery processInstanceQuery = new SearchQuery.Builder().filter(processInstanceFilter)
          .size(maxResult)
          .build();
      List<ProcessInstance> listProcessInstances = operateClient.searchProcessInstances(processInstanceQuery);

      List<ProcessDescription> listProcessInstanceFind = new ArrayList<>();
      // now, we have to filter based on variableName/value

      for (ProcessInstance processInstance : listProcessInstances) {
        Map<String, Object> processVariables = getVariables(processInstance.getKey().toString());
        List<Map.Entry<String, Object>> entriesNotFiltered = filterVariables.entrySet()
            .stream()
            .filter(
                t -> processVariables.containsKey(t.getKey()) && processVariables.get(t.getKey()).equals(t.getValue()))
            .toList();

        if (entriesNotFiltered.isEmpty()) {

          ProcessDescription processDescription = new ProcessDescription();
          processDescription.processInstanceId = processInstance.getKey().toString();

          listProcessInstanceFind.add(processDescription);
        }
      }
      return listProcessInstanceFind;
    } catch (OperateException e) {
      throw new AutomatorException("Can't search users task " + e.getMessage());
    }
  }

  private ScenarioStep.Step getTaskType(String taskTypeC8) {
    if (taskTypeC8.equals("SERVICE_TASK"))
      return ScenarioStep.Step.SERVICETASK;
    else if (taskTypeC8.equals("USER_TASK"))
      return ScenarioStep.Step.USERTASK;
    else if (taskTypeC8.equals("START_EVENT"))
      return ScenarioStep.Step.STARTEVENT;
    else if (taskTypeC8.equals("END_EVENT"))
      return ScenarioStep.Step.ENDEVENT;
    else if (taskTypeC8.equals("EXCLUSIVE_GATEWAY"))
      return ScenarioStep.Step.EXCLUSIVEGATEWAY;
    else if (taskTypeC8.equals("PARALLEL_GATEWAY"))
      return ScenarioStep.Step.PARALLELGATEWAY;

    return null;
  }

  @Override
  public Map<String, Object> getVariables(String processInstanceId) throws AutomatorException {
    try {
      // impossible to filter by the task name/ task tyoe, so be ready to get a lot of flowNode and search the correct onee
      VariableFilter variableFilter = new VariableFilter.Builder().processInstanceKey(Long.valueOf(processInstanceId))
          .build();

      SearchQuery variableQuery = new SearchQuery.Builder().filter(variableFilter).build();
      List<io.camunda.operate.dto.Variable> listVariables = operateClient.searchVariables(variableQuery);

      Map<String, Object> variables = new HashMap<>();
      listVariables.forEach(t -> variables.put(t.getName(), t.getValue()));

      return variables;
    } catch (OperateException e) {
      throw new AutomatorException("Can't search variables task " + e.getMessage());
    }
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  CountInformation                                                    */
  /*                                                                      */
  /* ******************************************************************** */
  public long countNumberOfProcessInstancesCreated(String processId, DateFilter startDate, DateFilter endDate)
      throws AutomatorException {
    SearchQuery.Builder queryBuilder = new SearchQuery.Builder();
    try {
      int cumul = 0;
      SearchResult<ProcessInstance> searchResult = null;
      queryBuilder = queryBuilder.filter(new ProcessInstanceFilter.Builder().bpmnProcessId(processId).build());
      queryBuilder.sort(new Sort("key", SortOrder.ASC));
      int maxLoop = 0;
      do {
        maxLoop++;
        if (searchResult != null && !searchResult.getItems().isEmpty()) {
          queryBuilder.searchAfter(searchResult.getSortValues());
        }
        SearchQuery searchQuery = queryBuilder.build();
        searchQuery.setSize(SEARCH_MAX_SIZE);
        searchResult = operateClient.searchProcessInstanceResults(searchQuery);

        cumul += searchResult.getItems().stream().filter(t -> t.getStartDate().after(startDate.getDate())).count();

      } while (searchResult.getItems().size() >= SEARCH_MAX_SIZE && maxLoop < 1000);
      return cumul;
    } catch (Exception e) {
      throw new AutomatorException("Search countNumberProcessInstanceCreated " + e.getMessage());
    }
  }

  public long countNumberOfProcessInstancesEnded(String processId, DateFilter startDate, DateFilter endDate)
      throws AutomatorException {
    SearchQuery.Builder queryBuilder = new SearchQuery.Builder();
    try {
      int cumul = 0;
      SearchResult<ProcessInstance> searchResult = null;

      queryBuilder = queryBuilder.filter(new ProcessInstanceFilter.Builder().bpmnProcessId(processId)
          // .startDate(startDate)
          // .endDate(endDate)
          .state(ProcessInstanceState.COMPLETED).build());

      queryBuilder.sort(new Sort("key", SortOrder.ASC));
      int maxLoop = 0;
      do {
        maxLoop++;
        if (searchResult != null && !searchResult.getItems().isEmpty()) {
          queryBuilder.searchAfter(searchResult.getSortValues());
        }
        SearchQuery searchQuery = queryBuilder.build();
        searchQuery.setSize(SEARCH_MAX_SIZE);
        searchResult = operateClient.searchProcessInstanceResults(searchQuery);
        cumul += searchResult.getItems().stream().filter(t -> t.getStartDate().after(startDate.getDate())).count();

      } while (searchResult.getItems().size() >= SEARCH_MAX_SIZE && maxLoop < 1000);
      return cumul;

    } catch (Exception e) {
      throw new AutomatorException("Search countNumberProcessEnded " + e.getMessage());
    }
  }
  /* ******************************************************************** */
  /*                                                                      */
  /*  Deployment                                                          */
  /*                                                                      */
  /* ******************************************************************** */

  public long countNumberOfTasks(String processId, String taskId) throws AutomatorException {

    try {

      int cumul = 0;
      SearchResult<FlownodeInstance> searchResult = null;
      int maxLoop = 0;
      do {
        maxLoop++;

        SearchQuery.Builder queryBuilder = new SearchQuery.Builder();
        queryBuilder = queryBuilder.filter(new FlownodeInstanceFilter.Builder().flowNodeId(taskId).build());
        queryBuilder.sort(new Sort("key", SortOrder.ASC));
        if (searchResult != null && !searchResult.getItems().isEmpty()) {
          queryBuilder.searchAfter(searchResult.getSortValues());
        }
        SearchQuery searchQuery = queryBuilder.build();
        searchQuery.setSize(SEARCH_MAX_SIZE);
        searchResult = operateClient.searchFlownodeInstanceResults(searchQuery);
        cumul += (long) searchResult.getItems().size();
      } while (searchResult.getItems().size() >= SEARCH_MAX_SIZE && maxLoop < 1000);
      return cumul;
    } catch (Exception e) {
      throw new AutomatorException("Search countNumberProcessEnded " + e.getMessage());
    }
  }


  /* ******************************************************************** */
  /*                                                                      */
  /*  get server definition                                               */
  /*                                                                      */
  /* ******************************************************************** */

  @Override
  public String deployBpmn(File processFile, ScenarioDeployment.Policy policy) throws AutomatorException {
    try {
      DeploymentEvent event = zeebeClient.newDeployResourceCommand()
          .addResourceFile(processFile.getAbsolutePath())
          .send()
          .join();

      return String.valueOf(event.getKey());
    } catch (Exception e) {
      throw new AutomatorException("Can't deploy " + e.getMessage());
    }
  }

  @Override
  public ConfigurationBpmEngine.CamundaEngine getTypeCamundaEngine() {
    return typeCamundaEngine;
  }

  @Override
  public String getSignature() {
    String signature = typeCamundaEngine.toString() + " ";
    if (typeCamundaEngine.equals(ConfigurationBpmEngine.CamundaEngine.CAMUNDA_8_SAAS))
      signature += "Cloud[" + serverDefinition.zeebeCloudRegister + "] ClientId[" + serverDefinition.zeebeCloudClientId
          + "] ClusterId[" + serverDefinition.zeebeCloudClusterId + "]";
    else
      signature += "Address[" + serverDefinition.zeebeGatewayAddress + "]";
    signature += " numJobWorkerExecutionThreads[" + serverDefinition.workerExecutionThreads + "] workerMaxJobsActive["
        + serverDefinition.workerMaxJobsActive + "]";
    return signature;
  }

  private String getUniqueMarker(String processId, String starterEventId) {
    return processId + "-" + random.nextInt(1000000);
  }

  public ZeebeClient getZeebeClient() {
    return zeebeClient;
  }
}
