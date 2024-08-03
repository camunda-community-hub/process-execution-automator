package org.camunda.automator.bpmnengine.camunda8;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.operate.CamundaOperateClient;
import io.camunda.operate.CamundaOperateClientConfiguration;
import io.camunda.operate.auth.JwtAuthentication;
import io.camunda.operate.auth.JwtCredential;
import io.camunda.operate.auth.SimpleAuthentication;
import io.camunda.operate.auth.SimpleCredential;
import io.camunda.operate.auth.TokenResponseMapper;
import io.camunda.operate.exception.OperateException;
import io.camunda.operate.model.FlowNodeInstance;
import io.camunda.operate.model.FlowNodeInstanceState;
import io.camunda.operate.model.ProcessInstance;
import io.camunda.operate.model.ProcessInstanceState;
import io.camunda.operate.model.SearchResult;
import io.camunda.operate.search.DateFilter;
import io.camunda.operate.search.FlowNodeInstanceFilter;
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
import io.camunda.zeebe.client.api.response.Topology;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.api.worker.JobWorkerBuilderStep1;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProvider;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProviderBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.bpmnengine.camunda8.refactoring.RefactoredCommandWrapper;
import org.camunda.automator.configuration.BpmnEngineList;
import org.camunda.automator.definition.ScenarioDeployment;
import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.flow.FixedBackoffSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class BpmnEngineCamunda8 implements BpmnEngine {

  public static final String THIS_IS_A_COMPLETE_IMPOSSIBLE_VARIABLE_NAME = "ThisIsACompleteImpossibleVariableName";
  public static final int SEARCH_MAX_SIZE = 100;
  private final Logger logger = LoggerFactory.getLogger(BpmnEngineCamunda8.class);

  private BpmnEngineList.BpmnServerDefinition serverDefinition;
  boolean hightFlowMode = false;
  /**
   * It is not possible to search user task for a specfic processInstance. So, to realize this, a marker is created in each process instance. Retrieving the user task,
   * the process instance can be found and correction can be done
   */
  Map<String, Long> cacheProcessInstanceMarker = new HashMap<>();
  Random random = new Random(System.currentTimeMillis());
  private ZeebeClient zeebeClient;
  private CamundaOperateClient operateClient;
  private CamundaTaskListClient  taskClient;

  private final  BenchmarkStartPiExceptionHandlingStrategy exceptionHandlingStrategy;
  // Default
  private BpmnEngineList.CamundaEngine typeCamundaEngine = BpmnEngineList.CamundaEngine.CAMUNDA_8;

  private BpmnEngineCamunda8(BenchmarkStartPiExceptionHandlingStrategy exceptionHandlingStrategy ) {
    this.exceptionHandlingStrategy = exceptionHandlingStrategy;
  }

  /**
   * Constructor from existing object
   *
   * @param serverDefinition server definition
   * @param logDebug         if true, operation will be log as debug level
   */
  public static BpmnEngineCamunda8 getFromServerDefinition(BpmnEngineList.BpmnServerDefinition serverDefinition,
                                                           BenchmarkStartPiExceptionHandlingStrategy benchmarkStartPiExceptionHandlingStrategy,
                                                           boolean logDebug) {
    BpmnEngineCamunda8 bpmnEngineCamunda8 = new BpmnEngineCamunda8(benchmarkStartPiExceptionHandlingStrategy);
    bpmnEngineCamunda8.serverDefinition = serverDefinition;
    return bpmnEngineCamunda8;

  }

  /**
   * Constructor to specify a Self Manage Zeebe Address por a Zeebe Saas
   *
   * @param zeebeSelfGatewayAddress    Self Manage : zeebe address
   * @param zeebeSelfSecurityPlainText Self Manage: Plain text
   * @param operateUrl                 URL to access Operate
   * @param operateUserName            Operate user name
   * @param operateUserPassword        Operate password
   * @param tasklistUrl                Url to access TaskList
   */
  public static BpmnEngineCamunda8 getFromCamunda8(String zeebeSelfGatewayAddress,
                                                   String zeebeSelfSecurityPlainText,
                                                   String operateUrl,
                                                   String operateUserName,
                                                   String operateUserPassword,
                                                   String tasklistUrl,
                                                   BenchmarkStartPiExceptionHandlingStrategy benchmarkStartPiExceptionHandlingStrategy) {
    BpmnEngineCamunda8 bpmnEngineCamunda8 = new BpmnEngineCamunda8(benchmarkStartPiExceptionHandlingStrategy);
    bpmnEngineCamunda8.serverDefinition = new BpmnEngineList.BpmnServerDefinition();
    bpmnEngineCamunda8.serverDefinition.serverType = BpmnEngineList.CamundaEngine.CAMUNDA_8;
    bpmnEngineCamunda8.serverDefinition = new BpmnEngineList.BpmnServerDefinition();
    bpmnEngineCamunda8.serverDefinition.zeebeGatewayAddress = zeebeSelfGatewayAddress;
    bpmnEngineCamunda8.serverDefinition.zeebeSecurityPlainText = zeebeSelfSecurityPlainText;


    /*
     * Connection to Operate
     */
    bpmnEngineCamunda8.serverDefinition.operateUserName = operateUserName;
    bpmnEngineCamunda8.serverDefinition.operateUserPassword = operateUserPassword;
    bpmnEngineCamunda8.serverDefinition.operateUrl = operateUrl;
    bpmnEngineCamunda8.serverDefinition.taskListUrl = tasklistUrl;
    return bpmnEngineCamunda8;

  }

  /**
   * Constructor to specify a Self Manage Zeebe Address por a Zeebe Saas
   *
   * @param zeebeSaasCloudRegister  Saas Cloud Register information
   * @param zeebeSaasCloudRegion    Saas Cloud region
   * @param zeebeSaasCloudClusterId Saas Cloud ClusterID
   * @param zeebeSaasCloudClientId  Saas Cloud ClientID
   * @param zeebeSaasClientSecret   Saas Cloud Client Secret
   * @param operateUrl              URL to access Operate
   * @param operateUserName         Operate user name
   * @param operateUserPassword     Operate password
   * @param tasklistUrl             Url to access TaskList
   */
  public static BpmnEngineCamunda8 getFromCamunda8SaaS(
      String zeebeSaasCloudRegister,
      String zeebeSaasCloudRegion,
      String zeebeSaasCloudClusterId,
      String zeebeSaasCloudClientId,
      String zeebeSaasOAuthUrl,
      String zeebeSaasAudience,
      String zeebeSaasClientSecret,
      String operateUrl,
      String operateUserName,
      String operateUserPassword,
      String tasklistUrl,
      BenchmarkStartPiExceptionHandlingStrategy benchmarkStartPiExceptionHandlingStrategy) {
    BpmnEngineCamunda8 bpmnEngineCamunda8 = new BpmnEngineCamunda8(benchmarkStartPiExceptionHandlingStrategy);
    bpmnEngineCamunda8.serverDefinition = new BpmnEngineList.BpmnServerDefinition();
    bpmnEngineCamunda8.serverDefinition.serverType = BpmnEngineList.CamundaEngine.CAMUNDA_8;


    /*
     * SaaS Zeebe
     */
    bpmnEngineCamunda8.serverDefinition.zeebeSaasRegion = zeebeSaasCloudRegion;
    bpmnEngineCamunda8.serverDefinition.zeebeSaasClusterId = zeebeSaasCloudClusterId;
    bpmnEngineCamunda8.serverDefinition.zeebeSaasClientId = zeebeSaasCloudClientId;
    bpmnEngineCamunda8.serverDefinition.zeebeSaasClientSecret = zeebeSaasClientSecret;
    bpmnEngineCamunda8.serverDefinition.zeebeSaasOAuthUrl = zeebeSaasOAuthUrl;
    bpmnEngineCamunda8.serverDefinition.zeebeSaasAudience = zeebeSaasAudience;

    /*
     * Connection to Operate
     */
    bpmnEngineCamunda8.serverDefinition.operateUserName = operateUserName;
    bpmnEngineCamunda8.serverDefinition.operateUserPassword = operateUserPassword;
    bpmnEngineCamunda8.serverDefinition.operateUrl = operateUrl;
    bpmnEngineCamunda8.serverDefinition.taskListUrl = tasklistUrl;
    return bpmnEngineCamunda8;
  }

  @Override
  public void init() {
    // nothing to do there
  }

  public void connection() throws AutomatorException {

    final String defaultAddress = "localhost:26500";
    final String envVarAddress = System.getenv("ZEEBE_ADDRESS");

    // connection is critical, so let build the analysis
    StringBuilder analysis = new StringBuilder();
    boolean isOk = true;

    isOk = stillOk(serverDefinition.name, "ZeebeConnection", analysis, false, isOk);
    this.typeCamundaEngine = this.serverDefinition.serverType;

    final ZeebeClientBuilder clientBuilder;
    CamundaOperateClientConfiguration configurationOperate;
    io.camunda.tasklist.auth.AuthInterface saTaskList;

    // ---------------------------- Camunda Saas
    if (BpmnEngineList.CamundaEngine.CAMUNDA_8_SAAS.equals(this.typeCamundaEngine)) {
      String gatewayAddressCloud =
          serverDefinition.zeebeSaasClusterId + "." + serverDefinition.zeebeSaasRegion + ".zeebe.camunda.io:443";
      stillOk(gatewayAddressCloud, "GatewayAddress", analysis, false, true);
      stillOk(serverDefinition.zeebeSaasClientId, "ClientId", analysis, false, true);

      /* Connect to Camunda Cloud Cluster, assumes that credentials are set in environment variables.
       * See JavaDoc on class level for details
       */
      isOk = stillOk(serverDefinition.zeebeSaasOAuthUrl, "OAutorisationServerUrl", analysis, true, isOk);
      isOk = stillOk(serverDefinition.zeebeSaasClientId, "ClientId", analysis, true, isOk);
      isOk = stillOk(serverDefinition.zeebeSaasClientSecret, "ClientSecret", analysis, true, isOk);

      try {
        String audience = serverDefinition.zeebeSaasAudience != null ? serverDefinition.zeebeSaasAudience : "";
        OAuthCredentialsProvider credentialsProvider = new OAuthCredentialsProviderBuilder() // formatting
            .authorizationServerUrl(serverDefinition.zeebeSaasOAuthUrl)
            .audience(audience)
            .clientId(serverDefinition.zeebeSaasClientId)
            .clientSecret(serverDefinition.zeebeSaasClientSecret)
            .build();

        clientBuilder = ZeebeClient.newClientBuilder()
            .gatewayAddress(gatewayAddressCloud)
            .credentialsProvider(credentialsProvider);

      } catch (Exception e) {
        zeebeClient = null;
        throw new AutomatorException(
            "Bad credential [" + serverDefinition.name + "] Analysis:" + analysis + " fail : " + e.getMessage());
      }
      try {
        URL authUrl = URI.create("https://login.cloud.camunda.io/oauth/token").toURL();
        URL operateUrl = URI.create("https://" + serverDefinition.zeebeSaasRegion + ".operate.camunda.io/"
            + serverDefinition.zeebeSaasClusterId).toURL();

        JwtCredential credentials = new JwtCredential(serverDefinition.zeebeSaasClientId,
            serverDefinition.zeebeSaasClientSecret, "operate.camunda.io", authUrl);
        ObjectMapper objectMapper = new ObjectMapper();

        JwtAuthentication authentication = new io.camunda.operate.auth.JwtAuthentication(credentials,
            (TokenResponseMapper) objectMapper);
        CamundaOperateClientConfiguration configuration = new CamundaOperateClientConfiguration(authentication,
            operateUrl, objectMapper,
            (org.apache.hc.client5.http.impl.classic.CloseableHttpClient) HttpClients.createDefault());

        configurationOperate = new CamundaOperateClientConfiguration(authentication, operateUrl, objectMapper,
            HttpClients.createDefault());


        saTaskList = new io.camunda.tasklist.auth.SaasAuthentication(serverDefinition.zeebeSaasClientId,
            serverDefinition.zeebeSaasClientSecret);
      } catch (Exception e) {
        zeebeClient = null;
        logger.error("Can't connect to SaaS environemnt[{}] Analysis:{} : {}", serverDefinition.name, analysis, e);
        throw new AutomatorException(
            "Can't connect to SaaS environment[" + serverDefinition.name + "] Analysis:" + analysis + " fail : "
                + e.getMessage());
      }
      typeCamundaEngine = BpmnEngineList.CamundaEngine.CAMUNDA_8_SAAS;

      //---------------------------- Camunda 8 Self Manage
    } else if (serverDefinition.zeebeGatewayAddress != null && !this.serverDefinition.zeebeGatewayAddress.trim()
        .isEmpty()) {
      isOk = stillOk(serverDefinition.zeebeGatewayAddress, "GatewayAddress", analysis, true, isOk);

      // connect to local deployment; assumes that authentication is disabled
      clientBuilder = ZeebeClient.newClientBuilder()
          .gatewayAddress(serverDefinition.zeebeGatewayAddress)
          .usePlaintext();
      try {
        if (serverDefinition.operateAuthenticationUrl != null) {
          URL operateUrl = URI.create(serverDefinition.operateUrl).toURL();
          // "http://localhost:18080/auth/realms/camunda-platform/protocol/openid-connect/token"
          URL authUrl = URI.create(serverDefinition.operateAuthenticationUrl).toURL();

          JwtCredential credentials = new JwtCredential(serverDefinition.operateClientId,
              serverDefinition.operateClientSecret, serverDefinition.operateAudience, authUrl);
          ObjectMapper objectMapper = new ObjectMapper();
          JwtAuthentication authentication = new JwtAuthentication(credentials, (TokenResponseMapper) objectMapper);
          configurationOperate = new CamundaOperateClientConfiguration(authentication, operateUrl, objectMapper,
              HttpClients.createDefault());
        } else {
          URL operateUrl = URI.create(serverDefinition.operateUrl).toURL();
          SimpleCredential credentials = new SimpleCredential(serverDefinition.operateUserName,
              serverDefinition.operateUserPassword, operateUrl, Duration.ofMinutes(10));
          SimpleAuthentication authentication = new SimpleAuthentication(credentials);
          ObjectMapper objectMapper = new ObjectMapper();
          configurationOperate = new CamundaOperateClientConfiguration(authentication, operateUrl, objectMapper,
              HttpClients.createDefault());
        }
      } catch (Exception e) {
        zeebeClient = null;
        logger.error("Can't connect to SaaS environment[{}] Analysis:{} : {}", serverDefinition.name, analysis, e);
        throw new AutomatorException(
            "Can't connect to SaaS environemnt[" + serverDefinition.name + "] Analysis:" + analysis + " fail : "
                + e.getMessage());
      }

      saTaskList = new io.camunda.tasklist.auth.SimpleAuthentication(serverDefinition.operateUserName,
          serverDefinition.operateUserPassword);
      typeCamundaEngine = BpmnEngineList.CamundaEngine.CAMUNDA_8;
    } else
      throw new AutomatorException("Invalid configuration");

    // ---------------- connection
    try {
      isOk = stillOk(serverDefinition.workerExecutionThreads, "ExecutionThread", analysis, false, isOk);

      analysis.append(" ExecutionThread[");
      analysis.append(serverDefinition.workerExecutionThreads);
      analysis.append("] MaxJobsActive[");
      analysis.append(serverDefinition.workerMaxJobsActive);
      analysis.append("] ");
      if (serverDefinition.workerMaxJobsActive == -1) {
        serverDefinition.workerMaxJobsActive = serverDefinition.workerExecutionThreads;
        analysis.append("No workerMaxJobsActive defined, align to the number of threads, ");
      }
      if (serverDefinition.workerExecutionThreads > serverDefinition.workerMaxJobsActive) {
        logger.error(
            "Camunda8 [{}] Incorrect definition: the workerExecutionThreads {} must be <= workerMaxJobsActive {} , else ZeebeClient will not fetch enough jobs to feed threads",
            serverDefinition.name, serverDefinition.workerExecutionThreads, serverDefinition.workerMaxJobsActive);
      }

      if (!isOk)
        throw new AutomatorException("Invalid configuration " + analysis.toString());

      clientBuilder.numJobWorkerExecutionThreads(serverDefinition.workerExecutionThreads);
      clientBuilder.defaultJobWorkerMaxJobsActive(serverDefinition.workerMaxJobsActive);

      analysis.append("Zeebe connection...");
      zeebeClient = clientBuilder.build();

      // simple test
      Topology join = zeebeClient.newTopologyRequest().send().join();

      // Actually, if an error arrived, an exception is thrown
      analysis.append(join != null ? "successfully, " : "error, ");

      // -------------------------------------- Operate
      if (serverDefinition.operateUrl != null && !serverDefinition.operateUrl.isEmpty()) {
        isOk = stillOk(serverDefinition.operateUrl, "operateUrl", analysis, false, isOk);
        analysis.append("Operate connection...");
        operateClient = new CamundaOperateClient(configurationOperate);

        analysis.append("successfully, ");
      } else
        analysis.append("No operate connection required, ");

      // -------------------------------------- TaskList
      // TaskList is not mandatory
      if (serverDefinition.taskListUrl != null && !serverDefinition.taskListUrl.isEmpty()) {
        isOk = stillOk(serverDefinition.taskListUrl, "taskListUrl", analysis, false, isOk);
        analysis.append("Tasklist ...");

        taskClient = new CamundaTaskListClient.Builder().taskListUrl(serverDefinition.taskListUrl)
            .authentication(saTaskList)
            .build();
        analysis.append("successfully, ");
      } else
        analysis.append("No Tasklist connection required, ");

      //get tasks assigned to demo
      logger.info(analysis.toString());

    } catch (Exception e) {
      zeebeClient = null;
      logger.error("Can't connect to Server[{}] Analysis:{} : {}", serverDefinition.name, analysis, e);
      throw new AutomatorException(
          "Can't connect to Server[" + serverDefinition.name + "] Analysis:" + analysis + " Fail : " + e.getMessage());
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
    return zeebeClient != null;
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  Manage process instance                                             */
  /*                                                                      */
  /* ******************************************************************** */

  /**
   * HighFlowMode: when true, the class does not save anything, to reduce the footprint
   *
   * @param highFlowMode true or false
   */
  public void turnHighFlowMode(boolean highFlowMode) {
    this.hightFlowMode = highFlowMode;
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
      throw new AutomatorException("CreateProcessInstance Error[" + processId + "] :" + e.getMessage());
    }
  }

  public String createProcessInstanceDirect(String processId, String starterEventId, Map<String, Object> variables)
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
  public List<String> searchUserTasksByProcessInstance(String processInstanceId, String userTaskId, int maxResult)
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

        if (tasksList.size() > 0)
          tasksList = taskClient.after(tasksList);
      } while (tasksList.size() > 0);

      return listTasksResult;

    } catch (TaskListException e) {
      throw new AutomatorException("Can't search users task " + e.getMessage());
    }
  }

  @Override
  public List<String> searchUserTasks(String userTaskId, int maxResult) throws AutomatorException {
    try {
      // impossible to filter by the task name/ task type, so be ready to get a lot of flowNode and search the correct one

      TaskSearch taskSearch = new TaskSearch();
      taskSearch.setState(TaskState.CREATED);
      taskSearch.setAssigned(Boolean.FALSE);
      taskSearch.setWithVariables(true);
      taskSearch.setPagination(new Pagination().setPageSize(maxResult));

      TaskList tasksList = taskClient.getTasks(taskSearch);
      List<String> listTasksResult = new ArrayList<>();
      do {
        listTasksResult.addAll(tasksList.getItems().stream().map(Task::getId) // Task to ID
            .toList());

        if (tasksList.size() > 0)
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
  public RegisteredTask registerServiceTask(String workerId,
                                            String topic,
                                            boolean streamEnabled,
                                            Duration lockTime,
                                            Object jobHandler,
                                            FixedBackoffSupplier backoffSupplier) {
    if (!(jobHandler instanceof JobHandler)) {
      logger.error("handler is not a JobHandler implementation, can't register the worker [{}], topic [{}]", workerId,
          topic);
      return null;
    }
    if (topic == null) {
      logger.error("topic must not be null, can't register the worker [{}]", workerId);
      return null;

    }
    RegisteredTask registeredTask = new RegisteredTask();

    JobWorkerBuilderStep1.JobWorkerBuilderStep3 step3 = zeebeClient.newWorker()
        .jobType(topic)
        .handler((JobHandler) jobHandler)
        .timeout(lockTime)
        .streamEnabled(streamEnabled)
        .name(workerId);

    if (backoffSupplier != null) {
      step3.backoffSupplier(backoffSupplier);
    }
    registeredTask.jobWorker = step3.open();
    return registeredTask;
  }

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
      if (operateClient == null) {
        throw new AutomatorException("No Operate connection was provided");
      }
      long processInstanceIdLong = Long.parseLong(processInstanceId);

      ProcessInstanceFilter processInstanceFilter = ProcessInstanceFilter.builder().parentKey(processInstanceIdLong)
          .build();

      SearchQuery processInstanceQuery = new SearchQuery.Builder().filter(processInstanceFilter).size(100).build();

      List<ProcessInstance> listProcessInstances = operateClient.searchProcessInstances(processInstanceQuery);
      Set<Long> setProcessInstances = listProcessInstances.stream()
          .map(ProcessInstance::getKey)
          .collect(Collectors.toSet());
      setProcessInstances.add(processInstanceIdLong);

      ActivateJobsResponse jobsResponse = zeebeClient.newActivateJobsCommand()
          .jobType(topic)
          .maxJobsToActivate(10000)
          .workerName(Thread.currentThread().getName())
          .send()
          .join();
      List<String> listJobsId = new ArrayList<>();

      for (ActivatedJob job : jobsResponse.getJobs()) {
        if (setProcessInstances.contains(job.getProcessInstanceKey()))
          listJobsId.add(String.valueOf(job.getKey()));
        else {
          zeebeClient.newFailCommand(job.getKey()).retries(2).send().join();
        }
      }
      return listJobsId;

    } catch (Exception e) {
      throw new AutomatorException("Can't search service task topic[" + topic + "] : " + e.getMessage());
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
      zeebeClient.newCompleteCommand(Long.valueOf(serviceTaskId)).variables(variables).send().join();
    } catch (Exception e) {
      throw new AutomatorException("Can't execute service task " + e.getMessage());
    }
  }

  public void throwBpmnServiceTask(String serviceTaskId,
                                   String workerId,
                                   String errorCode,
                                   String errorMessage,
                                   Map<String, Object> variables) throws AutomatorException {
    try {
      zeebeClient.newThrowErrorCommand(Long.valueOf(serviceTaskId))
          .errorCode(errorCode)
          .errorMessage(errorMessage)
          .variables(variables)
          .send()
          .join();
    } catch (Exception e) {
      throw new AutomatorException("Can't execute service task " + e.getMessage());
    }
  }

  @Override
  public List<TaskDescription> searchTasksByProcessInstanceId(String processInstanceId, String taskId, int maxResult)
      throws AutomatorException {
    try {
      if (operateClient == null) {
        throw new AutomatorException("No Operate connection was provided");
      }

      // impossible to filter by the task name/ task tyoe, so be ready to get a lot of flowNode and search the correct onee
      FlowNodeInstanceFilter flownodeFilter = FlowNodeInstanceFilter.builder().processInstanceKey(
          Long.valueOf(processInstanceId)).build();

      SearchQuery flownodeQuery = new SearchQuery.Builder().filter(flownodeFilter).size(maxResult).build();
      List<FlowNodeInstance> flownodes = operateClient.searchFlowNodeInstances(flownodeQuery);
      return flownodes.stream().filter(t -> taskId.equals(t.getFlowNodeId())).map(t -> {
        TaskDescription taskDescription = new TaskDescription();
        taskDescription.taskId = t.getFlowNodeId();
        taskDescription.type = getTaskType(t.getType()); // to implement
        taskDescription.isCompleted = FlowNodeInstanceState.COMPLETED.equals(t.getState()); // to implement
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
      if (operateClient == null) {
        throw new AutomatorException("No Operate connection was provided");
      }

      // impossible to filter by the task name/ task tyoe, so be ready to get a lot of flowNode and search the correct onee
      ProcessInstanceFilter processInstanceFilter = ProcessInstanceFilter.builder().bpmnProcessId(processId)
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
      if (operateClient == null) {
        throw new AutomatorException("No Operate connection was provided");
      }

      // impossible to filter by the task name/ task tyoe, so be ready to get a lot of flowNode and search the correct onee
      VariableFilter variableFilter = VariableFilter.builder().processInstanceKey(Long.valueOf(processInstanceId))
          .build();

      SearchQuery variableQuery = new SearchQuery.Builder().filter(variableFilter).build();
      List<io.camunda.operate.model.Variable> listVariables = operateClient.searchVariables(variableQuery);

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
    if (operateClient == null) {
      throw new AutomatorException("No Operate connection was provided");
    }

    SearchQuery.Builder queryBuilder = new SearchQuery.Builder();
    try {
      int cumul = 0;
      SearchResult<ProcessInstance> searchResult = null;
      queryBuilder = queryBuilder.filter(ProcessInstanceFilter.builder().bpmnProcessId(processId).build());
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
    if (operateClient == null) {
      throw new AutomatorException("No Operate connection was provided");
    }

    SearchQuery.Builder queryBuilder = new SearchQuery.Builder();
    try {
      int cumul = 0;
      SearchResult<ProcessInstance> searchResult = null;

      queryBuilder = queryBuilder.filter(ProcessInstanceFilter.builder().bpmnProcessId(processId)
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
    if (operateClient == null) {
      throw new AutomatorException("No Operate connection was provided");
    }

    try {

      int cumul = 0;
      SearchResult<FlowNodeInstance> searchResult = null;
      int maxLoop = 0;
      do {
        maxLoop++;

        SearchQuery.Builder queryBuilder = new SearchQuery.Builder();
        queryBuilder = queryBuilder.filter(FlowNodeInstanceFilter.builder().flowNodeId(taskId).build());
        queryBuilder.sort(new Sort("key", SortOrder.ASC));
        if (searchResult != null && !searchResult.getItems().isEmpty()) {
          queryBuilder.searchAfter(searchResult.getSortValues());
        }
        SearchQuery searchQuery = queryBuilder.build();
        searchQuery.setSize(SEARCH_MAX_SIZE);
        searchResult = operateClient.searchFlowNodeInstanceResults(searchQuery);
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
  public BpmnEngineList.CamundaEngine getTypeCamundaEngine() {
    return typeCamundaEngine;
  }

  @Override
  public String getSignature() {
    String signature = typeCamundaEngine.toString() + " ";
    if (typeCamundaEngine.equals(BpmnEngineList.CamundaEngine.CAMUNDA_8_SAAS))
      signature +=
          "Cloud ClientId[" + serverDefinition.zeebeSaasClientId + "] ClusterId[" + serverDefinition.zeebeSaasClusterId
              + "]";
    else
      signature += "Address[" + serverDefinition.zeebeGatewayAddress + "]";
    signature += " numJobWorkerExecutionThreads[" + serverDefinition.workerExecutionThreads + "] workerMaxJobsActive["
        + serverDefinition.workerMaxJobsActive + "]";
    return signature;
  }

  @Override
  public int getWorkerExecutionThreads() {
    return serverDefinition != null ? serverDefinition.workerExecutionThreads : 0;
  }

  private String getUniqueMarker(String processId, String starterEventId) {
    return processId + "-" + random.nextInt(1000000);
  }

  public ZeebeClient getZeebeClient() {
    return zeebeClient;
  }

  /**
   * add in analysis and check the consistence
   *
   * @param value       value to check
   * @param message     name of parameter
   * @param analysis    analysis builder
   * @param check       true if the value must not be null or empty
   * @param wasOkBefore previous value, is returned if this check is Ok
   * @return previous value is ok false else
   */
  private boolean stillOk(Object value, String message, StringBuilder analysis, boolean check, boolean wasOkBefore) {
    analysis.append(message);
    analysis.append(" [");
    analysis.append(value);
    analysis.append("]");

    if (check) {
      if (value == null || (value instanceof String && ((String) value).isEmpty())) {
        analysis.append("No ");
        analysis.append(message);
        return false;
      }
    }
    return wasOkBefore;
  }

}
