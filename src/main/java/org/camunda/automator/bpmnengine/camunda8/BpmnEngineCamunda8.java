package org.camunda.automator.bpmnengine.camunda8;

import io.camunda.common.auth.*;
import io.camunda.common.auth.identity.IdentityConfig;
import io.camunda.common.auth.identity.IdentityContainer;
import io.camunda.common.json.SdkObjectMapper;
import io.camunda.identity.sdk.Identity;
import io.camunda.identity.sdk.IdentityConfiguration;
import io.camunda.operate.CamundaOperateClient;
import io.camunda.operate.CamundaOperateClientBuilder;
import io.camunda.operate.exception.OperateException;
import io.camunda.operate.model.*;
import io.camunda.operate.search.*;
import io.camunda.tasklist.CamundaTaskListClient;
import io.camunda.tasklist.CamundaTaskListClientBuilder;
import io.camunda.tasklist.dto.Variable;
import io.camunda.tasklist.dto.*;
import io.camunda.tasklist.exception.TaskListException;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import io.camunda.zeebe.client.api.command.FinalCommandStep;
import io.camunda.zeebe.client.api.response.*;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.api.worker.JobWorkerBuilderStep1;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProvider;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProviderBuilder;
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
import java.util.*;
import java.util.stream.Collectors;

public class BpmnEngineCamunda8 implements BpmnEngine {

    public static final String THIS_IS_A_COMPLETE_IMPOSSIBLE_VARIABLE_NAME = "ThisIsACompleteImpossibleVariableName";
    public static final int SEARCH_MAX_SIZE = 100;
    public static final String SAAS_AUTHENTICATE_URL = "https://login.cloud.camunda.io/oauth/token";
    private final Logger logger = LoggerFactory.getLogger(BpmnEngineCamunda8.class);
    private final BenchmarkStartPiExceptionHandlingStrategy exceptionHandlingStrategy;
    boolean hightFlowMode = false;
    /**
     * It is not possible to search user task for a specific processInstance. So, to realize this, a marker is created in each process instance. Retrieving the user task,
     * the process instance can be found and correction can be done
     */
    Map<String, Long> cacheProcessInstanceMarker = new HashMap<>();
    Random random = new Random(System.currentTimeMillis());
    private BpmnEngineList.BpmnServerDefinition serverDefinition;
    private ZeebeClient zeebeClient;
    private CamundaOperateClient operateClient;
    private CamundaTaskListClient taskClient;
    // Default
    private BpmnEngineList.CamundaEngine typeCamundaEngine = BpmnEngineList.CamundaEngine.CAMUNDA_8;

    private BpmnEngineCamunda8(BenchmarkStartPiExceptionHandlingStrategy exceptionHandlingStrategy) {
        this.exceptionHandlingStrategy = exceptionHandlingStrategy;
    }

    /**
     * Constructor from existing object
     *
     * @param serverDefinition server definition
     * @param logDebug         if true, operation will be logged as debug level
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
     * @param zeebeSelfGatewayAddress Self Manage : zeebe address
     * @param zeebePlainText          Self Manage: Plain text
     * @param operateUrl              URL to access Operate
     * @param operateUserName         Operate user name
     * @param operateUserPassword     Operate password
     * @param tasklistUrl             Url to access TaskList
     */
    public static BpmnEngineCamunda8 getFromCamunda8(String zeebeSelfGatewayAddress,
                                                     String zeebeGrpcAddress,
                                                     String zeebeRestAddress,
                                                     Boolean zeebePlainText,
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
        bpmnEngineCamunda8.serverDefinition.zeebeGrpcAddress = zeebeGrpcAddress;
        bpmnEngineCamunda8.serverDefinition.zeebeRestAddress = zeebeRestAddress;
        bpmnEngineCamunda8.serverDefinition.zeebePlainText = zeebePlainText;


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
     * @param zeebeSaasCloudRegion    Saas Cloud region
     * @param zeebeSaasCloudClusterId Saas Cloud ClusterID
     * @param zeebeSaasCloudClientId  Saas Cloud ClientID
     * @param zeebeSaasClientSecret   Saas Cloud Client Secret
     * @param operateUrl              URL to access Operate
     * @param operateUserName         Operate user name
     * @param operateUserPassword     Operate password
     * @param tasklistUrl             Url to access TaskList
     */
    public static BpmnEngineCamunda8 getFromCamunda8SaaS(String zeebeSaasCloudRegion,
                                                         String zeebeSaasCloudClusterId,
                                                         String zeebeSaasAudience,
                                                         String zeebeSaasCloudClientId,
                                                         String zeebeSaasClientSecret,
                                                         String zeebeSaasAuthenticationUrl,
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
        bpmnEngineCamunda8.serverDefinition.zeebeClientId = zeebeSaasCloudClientId;
        bpmnEngineCamunda8.serverDefinition.zeebeClientSecret = zeebeSaasClientSecret;
        bpmnEngineCamunda8.serverDefinition.authenticationUrl = zeebeSaasAuthenticationUrl;
        bpmnEngineCamunda8.serverDefinition.zeebeAudience = zeebeSaasAudience;

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

        this.typeCamundaEngine = this.serverDefinition.serverType;
        StringBuilder analysis = new StringBuilder();
        try {
            connectZeebe(analysis);
            connectOperate(analysis);
            connectTaskList(analysis);
            logger.info("Zeebe: OK, Operate: OK, TaskList:OK {}", analysis);

        } catch (AutomatorException e) {
            zeebeClient = null;
            throw e;
        }
    }

    public void disconnection() {
        // nothing to do here
    }

    /**
     * Engine is ready. If not, a connection() method must be call
     *
     * @return true if the engine is ready
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
            boolean getAllTasks = tasksList.size() < maxResult;
            List<String> listTasksResult = new ArrayList<>();
            do {
                if (!hightFlowMode) {
                    // We check that the task is the one expected
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
                } else {
                    listTasksResult.addAll(tasksList.getItems().stream()
                            .map(Task::getId) // Task to ID
                            .toList());
                }

                if (tasksList.size() > 0 && !getAllTasks)
                    tasksList = taskClient.after(tasksList);
            } while (tasksList.size() > 0 && !getAllTasks);

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

        logger.info("Create worker[{}] Topic[{}] StreamEnabled[{}] LockTime[{}] WorkerExecutionThreads[{}] MaxJobsActive[{}]", // label
                workerId, topic, streamEnabled, lockTime,
                serverDefinition.workerExecutionThreads,
                serverDefinition.workerMaxJobsActive);


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
        } catch (Exception e) {
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

            ProcessInstanceFilter processInstanceFilter = ProcessInstanceFilter.builder()
                    .parentKey(processInstanceIdLong)
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

    /**
     * @param processInstanceId filter on the processInstanceId. may be null
     * @param filterTaskId      filter on the taskId
     * @param maxResult         maximum Result
     * @return list of Task
     * @throws AutomatorException
     */
    @Override
    public List<TaskDescription> searchTasksByProcessInstanceId(String processInstanceId, String filterTaskId, int maxResult)
            throws AutomatorException {
        try {
            if (operateClient == null) {
                throw new AutomatorException("No Operate connection was provided");
            }

            // impossible to filter by the task name/ task tyoe, so be ready to get a lot of flowNode and search the correct onee
            FlowNodeInstanceFilter flownodeFilter = FlowNodeInstanceFilter.builder()
                    .processInstanceKey(Long.valueOf(processInstanceId))
                    .build();

            SearchQuery flowNodeQuery = new SearchQuery.Builder().filter(flownodeFilter).size(maxResult).build();
            List<FlowNodeInstance> flowNodes = operateClient.searchFlowNodeInstances(flowNodeQuery);
            return flowNodes.stream()
                    .filter(t -> filterTaskId == null || filterTaskId.equals(t.getFlowNodeId())) // Filter by name
                    .map(t -> {
                        TaskDescription taskDescription = new TaskDescription();
                        taskDescription.taskId = t.getFlowNodeId();
                        taskDescription.processInstanceId = String.valueOf(t.getProcessInstanceKey());
                        taskDescription.startDate = t.getStartDate();
                        taskDescription.endDate = t.getEndDate();
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
            ProcessInstanceFilter processInstanceFilter = ProcessInstanceFilter.builder().bpmnProcessId(processId).build();

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
        else if (taskTypeC8.equals("TASK"))
            return ScenarioStep.Step.TASK;
        else if (taskTypeC8.equals("SCRIPT_TASK"))
            return ScenarioStep.Step.SCRIPTTASK;

        return null;
    }

    @Override
    public Map<String, Object> getVariables(String processInstanceId) throws AutomatorException {
        try {
            if (operateClient == null) {
                throw new AutomatorException("No Operate connection was provided");
            }

            // impossible to filter by the task name/ task tyoe, so be ready to get a lot of flowNode and search the correct onee
            VariableFilter variableFilter = VariableFilter.builder()
                    .processInstanceKey(Long.valueOf(processInstanceId))
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
    public long countNumberOfProcessInstancesCreated(String processId, Date startDate, Date endDate)
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

                cumul += searchResult.getItems().stream().filter(t -> t.getStartDate().after(startDate)).count();

            } while (searchResult.getItems().size() >= SEARCH_MAX_SIZE && maxLoop < 1000);
            return cumul;
        } catch (Exception e) {
            throw new AutomatorException("Search countNumberProcessInstanceCreated " + e.getMessage());
        }
    }

    public long countNumberOfProcessInstancesEnded(String processId, Date startDate, Date endDate)
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
                cumul += searchResult.getItems().stream().filter(t -> t.getStartDate().after(startDate)).count();

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
                    "Cloud ClientId[" + serverDefinition.zeebeClientId + "] ClusterId[" + serverDefinition.zeebeSaasClusterId
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



    /* ******************************************************************** */
    /*                                                                      */
    /*  Connection to each component                                               */
    /*                                                                      */
    /* ******************************************************************** */

    private void connectZeebe(StringBuilder analysis) throws AutomatorException {

        // connection is critical, so let build the analysis

        boolean isOk = true;

        isOk = stillOk(serverDefinition.name, "ZeebeConnection", analysis, false, true, isOk);
        this.typeCamundaEngine = this.serverDefinition.serverType;

        ZeebeClientBuilder clientBuilder;

        // ---------------------------- Camunda Saas
        if (BpmnEngineList.CamundaEngine.CAMUNDA_8_SAAS.equals(this.typeCamundaEngine)) {
            analysis.append("SaaS;");

            String gatewayAddressCloud =
                    serverDefinition.zeebeSaasClusterId + "." + serverDefinition.zeebeSaasRegion + ".zeebe.camunda.io:443";
            isOk = stillOk(gatewayAddressCloud, "GatewayAddress", analysis, true, true, isOk);
            isOk = stillOk(serverDefinition.zeebeClientId, "ClientId", analysis, true, true, isOk);

            /* Connect to Camunda Cloud Cluster, assumes that credentials are set in environment variables.
             * See JavaDoc on class level for details
             */
            isOk = stillOk(serverDefinition.authenticationUrl, "authenticationUrl", analysis, true, true, isOk);
            isOk = stillOk(serverDefinition.zeebeAudience, "zeebeAudience", analysis, true, true, isOk);
            isOk = stillOk(serverDefinition.zeebeClientId, "ClientId", analysis, true, true, isOk);
            isOk = stillOk(serverDefinition.zeebeClientSecret, "ClientSecret", analysis, true, true, isOk);

            try {

                OAuthCredentialsProvider credentialsProvider = new OAuthCredentialsProviderBuilder() // formatting
                        .authorizationServerUrl(
                                serverDefinition.authenticationUrl != null ? serverDefinition.authenticationUrl : SAAS_AUTHENTICATE_URL)
                        .audience(serverDefinition.zeebeAudience)
                        .clientId(serverDefinition.zeebeClientId)
                        .clientSecret(serverDefinition.zeebeClientSecret)
                        .build();

                clientBuilder = ZeebeClient.newClientBuilder()
                        .gatewayAddress(gatewayAddressCloud)
                        .credentialsProvider(credentialsProvider);

            } catch (Exception e) {
                zeebeClient = null;
                throw new AutomatorException(
                        "BadCredential[" + serverDefinition.name + "] Analysis:" + analysis + " : " + e.getMessage());
            }
        }

        //---------------------------- Camunda 8 Self Manage
        else if (BpmnEngineList.CamundaEngine.CAMUNDA_8.equals(this.typeCamundaEngine)) {
            analysis.append("SelfManage;");
            isOk = stillOk(serverDefinition.zeebeGatewayAddress, "GatewayAddress", analysis, true, true, isOk);
            if (serverDefinition.isAuthenticationUrl()) {
                analysis.append("WithAuthentication;");
                isOk = stillOk(serverDefinition.authenticationUrl, "authenticationUrl", analysis, true, true, isOk);
                isOk = stillOk(serverDefinition.zeebeAudience, "zeebeAudience", analysis, true, true, isOk);
                isOk = stillOk(serverDefinition.zeebeClientId, "zeebeClientId", analysis, true, true, isOk);
                isOk = stillOk(serverDefinition.zeebeClientSecret, "zeebeClientSecret", analysis, true, false, isOk);
                isOk = stillOk(serverDefinition.zeebePlainText, "zeebePlainText", analysis, true, true, isOk);

                try {
                    OAuthCredentialsProvider credentialsProvider = new OAuthCredentialsProviderBuilder() // builder
                            .authorizationServerUrl(serverDefinition.authenticationUrl)
                            .audience(serverDefinition.zeebeAudience)
                            .clientId(serverDefinition.zeebeClientId)
                            .clientSecret(serverDefinition.zeebeClientSecret)
                            .build();
                    clientBuilder = ZeebeClient.newClientBuilder()
                            .gatewayAddress(serverDefinition.zeebeGatewayAddress)
                            .grpcAddress(new URI(serverDefinition.zeebeGrpcAddress))
                            .restAddress(new URI(serverDefinition.zeebeRestAddress))
                            .defaultTenantId(serverDefinition.zeebeTenantId == null ? "<default>" : serverDefinition.zeebeTenantId)
                            .credentialsProvider(credentialsProvider);
                    if (Boolean.TRUE.equals(serverDefinition.zeebePlainText))
                        clientBuilder.usePlaintext();

                } catch (Exception e) {
                    zeebeClient = null;
                    logger.error("Can't connect to Server[{}] Analysis:{} : {}", serverDefinition.name, analysis, e);
                    throw new AutomatorException(
                            "BadCredential[" + serverDefinition.name + "] Analysis:" + analysis + " : " + e.getMessage());
                }
            } else {
                try {
                    analysis.append("NoAuthentication;");
                    // connect to local deployment; assumes that authentication is disabled
                    clientBuilder = ZeebeClient.newClientBuilder()
                            .gatewayAddress(serverDefinition.zeebeGatewayAddress);
                    if (serverDefinition.zeebeGrpcAddress != null) {
                        clientBuilder = clientBuilder.grpcAddress(new URI(serverDefinition.zeebeGrpcAddress));
                    }
                    if (serverDefinition.zeebeRestAddress != null) {
                        clientBuilder = clientBuilder.restAddress(new URI(serverDefinition.zeebeRestAddress));
                    }
                    clientBuilder = clientBuilder.usePlaintext();
                } catch (Exception e) {
                    zeebeClient = null;
                    logger.error("Can't connect to Server[{}] Analysis:{} : {}", serverDefinition.name, analysis, e);
                    throw new AutomatorException(
                            "badURL[" + serverDefinition.name + "] Analysis:" + analysis + " : " + e.getMessage());
                }
            }
        } else
            throw new AutomatorException("Invalid configuration");

        // ---------------- connection
        try {
            isOk = stillOk(serverDefinition.workerExecutionThreads, "ExecutionThread", analysis, false, true, isOk);

            analysis.append(" ExecutionThread[");
            analysis.append(serverDefinition.workerExecutionThreads);
            analysis.append("] MaxJobsActive[");
            analysis.append(serverDefinition.workerMaxJobsActive);
            analysis.append("] ");
            if (serverDefinition.workerMaxJobsActive == -1) {
                serverDefinition.workerMaxJobsActive = serverDefinition.workerExecutionThreads;
                analysis.append("No workerMaxJobsActive defined, align to ExecutionThread[");
                analysis.append(serverDefinition.workerExecutionThreads);
                analysis.append("]");
            }
            if (serverDefinition.workerExecutionThreads > serverDefinition.workerMaxJobsActive) {
                logger.error(
                        "Camunda8 [{}] Incorrect definition: the workerExecutionThreads {} must be <= workerMaxJobsActive {} , else ZeebeClient will not fetch enough jobs to feed threads",
                        serverDefinition.name, serverDefinition.workerExecutionThreads, serverDefinition.workerMaxJobsActive);
            }

            if (!isOk)
                throw new AutomatorException("Invalid configuration " + analysis);

            clientBuilder.numJobWorkerExecutionThreads(serverDefinition.workerExecutionThreads);
            clientBuilder.defaultJobWorkerMaxJobsActive(serverDefinition.workerMaxJobsActive);

            analysis.append("Zeebe connection...");
            zeebeClient = clientBuilder.build();

            // simple test
            Topology join = zeebeClient.newTopologyRequest().send().join();

            // Actually, if an error arrived, an exception is thrown

            analysis.append(join != null ? "successfully, " : "error, ");

        } catch (Exception e) {
            zeebeClient = null;
            logger.error("Can't connect to Server[{}] Analysis:{} : {}", serverDefinition.name, analysis, e);
            throw new AutomatorException(
                    "Can't connect to Server[" + serverDefinition.name + "] Analysis:" + analysis + " Fail : " + e.getMessage());
        }
    }

    /**
     * Connect Operate
     *
     * @param analysis to cpmplete the analysis
     * @throws AutomatorException in case of error
     */
    private void connectOperate(StringBuilder analysis) throws AutomatorException {
        if (!serverDefinition.isOperate()) {
            analysis.append("No operate connection required, ");
            return;
        }
        analysis.append("Operate connection...");

        boolean isOk = true;
        isOk = stillOk(serverDefinition.operateUrl, "operateUrl", analysis, true, true, isOk);

        CamundaOperateClientBuilder camundaOperateClientBuilder = new CamundaOperateClientBuilder();
        // ---------------------------- Camunda Saas
        if (BpmnEngineList.CamundaEngine.CAMUNDA_8_SAAS.equals(this.typeCamundaEngine)) {

            try {
                isOk = stillOk(serverDefinition.zeebeSaasRegion, "zeebeSaasRegion", analysis, true, true, isOk);
                isOk = stillOk(serverDefinition.zeebeSaasClusterId, "zeebeSaasClusterId", analysis, true, true, isOk);
                isOk = stillOk(serverDefinition.zeebeClientId, "zeebeClientId", analysis, true, true, isOk);
                isOk = stillOk(serverDefinition.zeebeClientSecret, "zeebeClientSecret", analysis, true, false, isOk);

                URL operateUrl = URI.create("https://" + serverDefinition.zeebeSaasRegion + ".operate.camunda.io/"
                        + serverDefinition.zeebeSaasClusterId).toURL();

                SaaSAuthenticationBuilder saaSAuthenticationBuilder = SaaSAuthentication.builder();
                JwtConfig jwtConfig = new JwtConfig();
                jwtConfig.addProduct(Product.OPERATE,
                        new JwtCredential(serverDefinition.zeebeClientId, serverDefinition.zeebeClientSecret,
                                serverDefinition.operateAudience != null ? serverDefinition.operateAudience : "operate.camunda.io",
                                serverDefinition.authenticationUrl != null ?
                                        serverDefinition.authenticationUrl :
                                        SAAS_AUTHENTICATE_URL));

                Authentication saasAuthentication = SaaSAuthentication.builder()
                        .withJwtConfig(jwtConfig)
                        .withJsonMapper(new SdkObjectMapper())
                        .build();

                camundaOperateClientBuilder.authentication(saasAuthentication)
                        .operateUrl(serverDefinition.operateUrl)
                        .setup()
                        .build();

            } catch (Exception e) {
                zeebeClient = null;
                logger.error("Can't connect to SaaS environemnt[{}] Analysis:{} : {}", serverDefinition.name, analysis, e);
                throw new AutomatorException(
                        "Can't connect to SaaS environment[" + serverDefinition.name + "] Analysis:" + analysis + " fail : "
                                + e.getMessage());
            }

            //---------------------------- Camunda 8 Self Manage
        } else if (BpmnEngineList.CamundaEngine.CAMUNDA_8.equals(this.typeCamundaEngine)) {

            isOk = stillOk(serverDefinition.zeebeGatewayAddress, "GatewayAddress", analysis, true, true, isOk);

            try {
                if (serverDefinition.isAuthenticationUrl()) {
                    isOk = stillOk(serverDefinition.authenticationUrl, "authenticationUrl", analysis, true, true, isOk);
                    isOk = stillOk(serverDefinition.operateClientId, "operateClientId", analysis, true, true, isOk);
                    isOk = stillOk(serverDefinition.operateClientSecret, "operateClientSecret", analysis, true, false, isOk);

                    IdentityConfiguration identityConfiguration = new IdentityConfiguration.Builder().withBaseUrl(
                                    serverDefinition.identityUrl)
                            .withIssuer(serverDefinition.authenticationUrl)
                            .withIssuerBackendUrl(serverDefinition.authenticationUrl)
                            .withClientId(serverDefinition.operateClientId)
                            .withClientSecret(serverDefinition.operateClientSecret)
                            .withAudience(serverDefinition.operateAudience)
                            .build();
                    Identity identity = new Identity(identityConfiguration);

                    IdentityConfig identityConfig = new IdentityConfig();
                    identityConfig.addProduct(Product.OPERATE, new IdentityContainer(identity, identityConfiguration));

                    JwtConfig jwtConfig = new JwtConfig();
                    jwtConfig.addProduct(Product.OPERATE, new JwtCredential(serverDefinition.operateClientId, // clientId
                            serverDefinition.operateClientSecret, // clientSecret
                            "zeebe-api", // audience
                            serverDefinition.authenticationUrl));

                    io.camunda.common.auth.SelfManagedAuthenticationBuilder identityAuthenticationBuilder = io.camunda.common.auth.SelfManagedAuthentication.builder();
                    identityAuthenticationBuilder.withJwtConfig(jwtConfig);
                    identityAuthenticationBuilder.withIdentityConfig(identityConfig);

                    Authentication identityAuthentication = identityAuthenticationBuilder.build();
                    camundaOperateClientBuilder.authentication(identityAuthentication)
                            .operateUrl(serverDefinition.operateUrl)
                            .setup()
                            .build();

                } else {
                    // Simple authentication
                    isOk = stillOk(serverDefinition.operateUserName, "operateUserName", analysis, true, true, isOk);
                    isOk = stillOk(serverDefinition.operateUserPassword, "operateUserPassword", analysis, true, false, isOk);

                    SimpleCredential simpleCredential = new SimpleCredential(serverDefinition.operateUrl,
                            serverDefinition.operateUserName, serverDefinition.operateUserPassword);

                    SimpleConfig jwtConfig = new io.camunda.common.auth.SimpleConfig();
                    jwtConfig.addProduct(Product.OPERATE, simpleCredential);

                    io.camunda.common.auth.SimpleAuthenticationBuilder simpleAuthenticationBuilder = SimpleAuthentication.builder();
                    simpleAuthenticationBuilder.withSimpleConfig(jwtConfig);

                    Authentication simpleAuthentication = simpleAuthenticationBuilder.build();
                    camundaOperateClientBuilder.authentication(simpleAuthentication)
                            .operateUrl(serverDefinition.operateUrl)
                            .setup()
                            .build();
                }
            } catch (Exception e) {
                logger.error("Can't connect to SaaS environment[{}] Analysis:{} : {}", serverDefinition.name, analysis, e);
                throw new AutomatorException(
                        "Can't connect to SaaS environment[" + serverDefinition.name + "] Analysis:" + analysis + " fail : "
                                + e.getMessage());
            }

        } else
            throw new AutomatorException("Invalid configuration");

        if (!isOk)
            throw new AutomatorException("Invalid configuration " + analysis);

        // ---------------- connection
        try {

            operateClient = camundaOperateClientBuilder.build();

            analysis.append("successfully, ");

        } catch (Exception e) {
            logger.error("Can't connect to Server[{}] Analysis:{} : {}", serverDefinition.name, analysis, e);
            throw new AutomatorException(
                    "Can't connect to Server[" + serverDefinition.name + "] Analysis:" + analysis + " Fail : " + e.getMessage());
        }
    }

    /**
     * Connect to TaskList
     *
     * @param analysis complete the analysis
     * @throws AutomatorException in case of error
     */
    private void connectTaskList(StringBuilder analysis) throws AutomatorException {

        if (!serverDefinition.isTaskList()) {
            analysis.append("No TaskList connection required, ");
            return;
        }
        analysis.append("Tasklist ...");

        boolean isOk = true;
        isOk = stillOk(serverDefinition.taskListUrl, "taskListUrl", analysis, true, true, isOk);

        CamundaTaskListClientBuilder taskListBuilder = CamundaTaskListClient.builder();
        // ---------------------------- Camunda Saas
        if (BpmnEngineList.CamundaEngine.CAMUNDA_8_SAAS.equals(this.typeCamundaEngine)) {
            try {
                isOk = stillOk(serverDefinition.zeebeSaasRegion, "zeebeSaasRegion", analysis, true, true, isOk);
                isOk = stillOk(serverDefinition.zeebeSaasClusterId, "zeebeSaasClusterId", analysis, true, true, isOk);
                isOk = stillOk(serverDefinition.taskListClientId, "taskListClientId", analysis, true, true, isOk);
                isOk = stillOk(serverDefinition.taskListClientSecret, "taskListClientSecret", analysis, true, false, isOk);

                String taskListUrl = "https://" + serverDefinition.zeebeSaasRegion + ".tasklist.camunda.io/"
                        + serverDefinition.zeebeSaasClusterId;

                taskListBuilder.taskListUrl(taskListUrl)
                        .saaSAuthentication(serverDefinition.taskListClientId, serverDefinition.taskListClientSecret);
            } catch (Exception e) {
                logger.error("Can't connect to SaaS environemnt[{}] Analysis:{} : {}", serverDefinition.name, analysis, e);
                throw new AutomatorException(
                        "Can't connect to SaaS environment[" + serverDefinition.name + "] Analysis:" + analysis + " fail : "
                                + e.getMessage());
            }

            //---------------------------- Camunda 8 Self Manage
        } else if (BpmnEngineList.CamundaEngine.CAMUNDA_8.equals(this.typeCamundaEngine)) {

            if (serverDefinition.isAuthenticationUrl()) {
                isOk = stillOk(serverDefinition.taskListClientId, "taskListClientId", analysis, true, true, isOk);
                isOk = stillOk(serverDefinition.taskListClientSecret, "taskListClientSecret", analysis, true, false, isOk);
                isOk = stillOk(serverDefinition.authenticationUrl, "authenticationUrl", analysis, true, true, isOk);
                isOk = stillOk(serverDefinition.taskListKeycloakUrl, "taskListKeycloakUrl", analysis, true, true, isOk);

                taskListBuilder.taskListUrl(serverDefinition.taskListUrl)
                        .selfManagedAuthentication(serverDefinition.taskListClientId, serverDefinition.taskListClientSecret,
                                serverDefinition.taskListKeycloakUrl);
            } else {
                isOk = stillOk(serverDefinition.taskListUserName, "User", analysis, true, true, isOk);
                isOk = stillOk(serverDefinition.taskListUserPassword, "Password", analysis, true, false, isOk);

                SimpleConfig simpleConf = new SimpleConfig();
                simpleConf.addProduct(Product.TASKLIST,
                        new SimpleCredential(serverDefinition.taskListUrl, serverDefinition.taskListUserName,
                                serverDefinition.taskListUserPassword));
                Authentication auth = SimpleAuthentication.builder().withSimpleConfig(simpleConf).build();

                taskListBuilder.taskListUrl(serverDefinition.taskListUrl)
                        .authentication(auth)
                        .cookieExpiration(Duration.ofSeconds(5));
            }
        } else
            throw new AutomatorException("Invalid configuration");

        if (!isOk)
            throw new AutomatorException("Invalid configuration " + analysis);

        // ---------------- connection
        try {
            taskListBuilder.zeebeClient(zeebeClient);
            taskListBuilder.useZeebeUserTasks();
            taskClient = taskListBuilder.build();

            analysis.append("successfully, ");

        } catch (Exception e) {
            logger.error("Can't connect to Server[{}] Analysis:{} : {}", serverDefinition.name, analysis, e);
            throw new AutomatorException(
                    "Can't connect to Server[" + serverDefinition.name + "] Analysis:" + analysis + " Fail : " + e.getMessage());
        }

    /* 1.6.1
    boolean isOk = true;
    io.camunda.tasklist.auth.AuthInterface saTaskList;

    // ---------------------------- Camunda Saas
    if (BpmnEngineList.CamundaEngine.CAMUNDA_8_SAAS.equals(this.typeCamundaEngine)) {
      try {
        saTaskList = new io.camunda.tasklist.auth.SaasAuthentication(serverDefinition.zeebeSaasClientId,
            serverDefinition.zeebeSaasClientSecret);
      } catch (Exception e) {
        logger.error("Can't connect to SaaS environment[{}] Analysis:{} : {}", serverDefinition.name, analysis, e);
        throw new AutomatorException(
            "Can't connect to SaaS environment[" + serverDefinition.name + "] Analysis:" + analysis + " fail : "
                + e.getMessage());
      }

      //---------------------------- Camunda 8 Self Manage
    } else if (BpmnEngineList.CamundaEngine.CAMUNDA_8.equals(this.typeCamundaEngine)) {
      saTaskList = new io.camunda.tasklist.auth.SimpleAuthentication(serverDefinition.operateUserName,
          serverDefinition.operateUserPassword);
    } else
      throw new AutomatorException("Invalid configuration");

    if (!isOk)
      throw new AutomatorException("Invalid configuration " + analysis);

    // ---------------- connection
    try {
      isOk = stillOk(serverDefinition.taskListUrl, "taskListUrl", analysis, false, isOk);
      analysis.append("Tasklist ...");

      taskClient = new CamundaTaskListClient.Builder().taskListUrl(serverDefinition.taskListUrl)
          .authentication(saTaskList)
          .build();
      analysis.append("successfully, ");
      //get tasks assigned to demo
      logger.info("Zeebe: OK, Operate: OK, TaskList:OK " + analysis);

    } catch (Exception e) {
      logger.error("Can't connect to Server[{}] Analysis:{} : {}", serverDefinition.name, analysis, e);
      throw new AutomatorException(
          "Can't connect to Server[" + serverDefinition.name + "] Analysis:" + analysis + " Fail : " + e.getMessage());
    }
    */

    }

    /**
     * add in analysis and check the consistence
     *
     * @param value                  value to check
     * @param message                name of parameter
     * @param analysis               analysis builder
     * @param check                  true if the value must not be null or empty
     * @param displayValueInAnalysis true if the value can be added in the analysis
     * @param wasOkBefore            previous value, is returned if this check is Ok
     * @return previous value is ok false else
     */
    private boolean stillOk(Object value,
                            String message,
                            StringBuilder analysis,
                            boolean check,
                            boolean displayValueInAnalysis,
                            boolean wasOkBefore) {
        analysis.append(message);
        analysis.append("[");
        analysis.append(getDisplayValue(value, displayValueInAnalysis));
        analysis.append("], ");

        if (check) {
            if (value == null || (value instanceof String valueString && valueString.isEmpty())) {
                analysis.append("No ");
                analysis.append(message);
                logger.error("Check failed {} value:[{}]", message, getDisplayValue(value, displayValueInAnalysis));
                return false;
            }
        }
        return wasOkBefore;
    }

    private String getDisplayValue(Object value, boolean displayValueInAnalysis) {
        if (value == null)
            return "null";
        if (displayValueInAnalysis)
            return value.toString();
        if (value.toString().length() <= 3)
            return "***";
        return value.toString().substring(0, 3) + "***";
    }
}
