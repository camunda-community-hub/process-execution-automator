package org.camunda.automator.bpmnengine.camunda8;


import io.camunda.tasklist.dto.Variable;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.response.Topology;
import io.camunda.zeebe.client.api.worker.JobHandler;
import io.camunda.zeebe.client.api.worker.JobWorkerBuilderStep1;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProvider;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProviderBuilder;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.configuration.BpmnEngineList;
import org.camunda.automator.definition.ScenarioDeployment;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.flow.FixedBackoffSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.*;

public class BpmnEngineCamunda8 implements BpmnEngine {

    public static final String THIS_IS_A_COMPLETE_IMPOSSIBLE_VARIABLE_NAME = "ThisIsACompleteImpossibleVariableName";
    public static final String SAAS_AUTHENTICATE_URL = "https://login.cloud.camunda.io/oauth/token";
    private final Logger logger = LoggerFactory.getLogger(BpmnEngineCamunda8.class);
    boolean hightFlowMode = false;
    /**
     * It is not possible to search user task for a specific processInstance. So, to realize this, a marker is created in each process instance. Retrieving the user task,
     * the process instance can be found and correction can be done
     */
    Map<String, Long> cacheProcessInstanceMarker = new HashMap<>();
    Random random = new Random(System.currentTimeMillis());
    private final BpmnEngineList.BpmnServerDefinition serverDefinition;
    private ZeebeClient zeebeClient;
    private final TaskListClient taskListClient;
    private final OperateClient operateClient;


    private BpmnEngineCamunda8(BpmnEngineList.BpmnServerDefinition serverDefinition, BenchmarkStartPiExceptionHandlingStrategy exceptionHandlingStrategy) {
        this.serverDefinition = serverDefinition;
        this.taskListClient = new TaskListClient(this);
        this.operateClient = new OperateClient(this);
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
        return new BpmnEngineCamunda8(serverDefinition, benchmarkStartPiExceptionHandlingStrategy);
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
        BpmnEngineList.BpmnServerDefinition serverDefinition = new BpmnEngineList.BpmnServerDefinition();
        serverDefinition.serverType = BpmnEngineList.CamundaEngine.CAMUNDA_8;
        serverDefinition.zeebeGatewayAddress = zeebeSelfGatewayAddress;
        serverDefinition.zeebeGrpcAddress = zeebeGrpcAddress;
        serverDefinition.zeebeRestAddress = zeebeRestAddress;
        serverDefinition.zeebePlainText = zeebePlainText;
        /*
         * Connection to Operate
         */
        serverDefinition.operateUserName = operateUserName;
        serverDefinition.operateUserPassword = operateUserPassword;
        serverDefinition.operateUrl = operateUrl;
        serverDefinition.taskListUrl = tasklistUrl;

        return new BpmnEngineCamunda8(serverDefinition, benchmarkStartPiExceptionHandlingStrategy);
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

        BpmnEngineList.BpmnServerDefinition serverDefinition = new BpmnEngineList.BpmnServerDefinition();
        serverDefinition.serverType = BpmnEngineList.CamundaEngine.CAMUNDA_8;


        /*
         * SaaS Zeebe
         */
        serverDefinition.zeebeSaasRegion = zeebeSaasCloudRegion;
        serverDefinition.zeebeSaasClusterId = zeebeSaasCloudClusterId;
        serverDefinition.zeebeClientId = zeebeSaasCloudClientId;
        serverDefinition.zeebeClientSecret = zeebeSaasClientSecret;
        serverDefinition.authenticationUrl = zeebeSaasAuthenticationUrl;
        serverDefinition.zeebeAudience = zeebeSaasAudience;

        /*
         * Connection to Operate
         */
        serverDefinition.operateUserName = operateUserName;
        serverDefinition.operateUserPassword = operateUserPassword;
        serverDefinition.operateUrl = operateUrl;
        serverDefinition.taskListUrl = tasklistUrl;
        return new BpmnEngineCamunda8(serverDefinition, benchmarkStartPiExceptionHandlingStrategy);
    }

    @Override
    public void init() {
        // nothing to do there
    }

    public void connection() throws AutomatorException {
        StringBuilder analysis = new StringBuilder();
        try {
            connectZeebe(analysis);
            operateClient.connectOperate(analysis);
            taskListClient.connectTaskList(analysis);
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

    public boolean isHightFlowMode() {
        return hightFlowMode;
    }

    @Override
    public String createProcessInstance(String processId, String starterEventId, Map<String, Object> variables)
            throws AutomatorException {
        try {
            String marker = null;
            if (!hightFlowMode) {
                marker = getUniqueMarker(processId);
                variables.put(THIS_IS_A_COMPLETE_IMPOSSIBLE_VARIABLE_NAME, marker);
            }

            ProcessInstanceEvent processInstanceEvent = zeebeClient.newCreateInstanceCommand()
                    .bpmnProcessId(processId)
                    .latestVersion()
                    .variables(variables)
                    .send().join();
            Long processInstanceId = processInstanceEvent.getProcessInstanceKey();
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
                marker = getUniqueMarker(processId);
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
        return taskListClient.searchUserTasksByProcessInstance(processInstanceId, userTaskId, maxResult);
    }

    @Override
    public List<String> searchUserTasks(String userTaskId, int maxResult) throws AutomatorException {
        return taskListClient.searchUserTasks(userTaskId, maxResult);
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
        taskListClient.executeUserTask(userTaskId, userId, variables);
    }

    @Override
    public List<String> activateServiceTasks(String processInstanceId, String serviceTaskId, String topic, int maxResult)
            throws AutomatorException {
        return operateClient.activateServiceTasks(processInstanceId, serviceTaskId, topic, maxResult);
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

    /**
     * ThrowBpmsServiceTask
     *
     * @param serviceTaskId taskId
     * @param workerId      workerId
     * @param errorCode     code to throw
     * @param errorMessage  Message to throw
     * @param variables     Variable
     * @throws AutomatorException if the message can't be throw
     */
    public void throwBpmnServiceTask(String serviceTaskId,
                                     String workerId,
                                     String errorCode,
                                     String errorMessage,
                                     Map<String, Object> variables) throws AutomatorException {
        try {
            zeebeClient.newThrowErrorCommand(Long.parseLong(serviceTaskId))
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
     * @throws AutomatorException in case of error
     */
    @Override
    public List<TaskDescription> searchTasksByProcessInstanceId(String processInstanceId, String filterTaskId, int maxResult)
            throws AutomatorException {
        return operateClient.searchTasksByProcessInstanceId(processInstanceId, filterTaskId, maxResult);
    }

    public List<ProcessDescription> searchProcessInstanceByVariable(String processId,
                                                                    Map<String, Object> filterVariables,
                                                                    int maxResult) throws AutomatorException {
        return operateClient.searchProcessInstanceByVariable(processId, filterVariables, maxResult);
    }


    @Override
    public Map<String, Object> getVariables(String processInstanceId) throws AutomatorException {
        return operateClient.getVariables(processInstanceId);
    }

    /* ******************************************************************** */
    /*                                                                      */
    /*  CountInformation                                                    */
    /*                                                                      */
    /* ******************************************************************** */
    public long countNumberOfProcessInstancesCreated(String processId, Date startDate, Date endDate)
            throws AutomatorException {
        return operateClient.countNumberOfProcessInstancesCreated(processId, startDate, endDate);
    }

    public long countNumberOfProcessInstancesEnded(String processId, Date startDate, Date endDate)
            throws AutomatorException {
        return operateClient.countNumberOfProcessInstancesEnded(processId, startDate, endDate);
    }
    /* ******************************************************************** */
    /*                                                                      */
    /*  Deployment                                                          */
    /*                                                                      */
    /* ******************************************************************** */

    public long countNumberOfTasks(String processId, String taskId) throws AutomatorException {
        return operateClient.countNumberOfTasks(processId, taskId);
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
        return serverDefinition.serverType;
    }

    @Override
    public String getSignature() {
        String signature = serverDefinition.serverType.toString() + " ";
        if (serverDefinition.serverType.equals(BpmnEngineList.CamundaEngine.CAMUNDA_8_SAAS))
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

    private String getUniqueMarker(String processId) {
        return processId + "-" + random.nextInt(1000000);
    }

    public ZeebeClient getZeebeClient() {
        return zeebeClient;
    }

    protected Long getProcessInstanceIdFromMarker(List<Variable> listVariables) {
        Optional<Variable> markerOptional = listVariables.stream()
                .filter(v -> v.getName().equals(THIS_IS_A_COMPLETE_IMPOSSIBLE_VARIABLE_NAME))
                .findFirst();
        if (markerOptional.isEmpty())
            return null;
        String marker = (String) markerOptional.get().getValue();
        return cacheProcessInstanceMarker.get(marker);
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

        ZeebeClientBuilder clientBuilder;

        // ---------------------------- Camunda Saas
        if (BpmnEngineList.CamundaEngine.CAMUNDA_8_SAAS.equals(serverDefinition.serverType)) {
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
        else if (BpmnEngineList.CamundaEngine.CAMUNDA_8.equals(serverDefinition.serverType)) {
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


    public BpmnEngineList.BpmnServerDefinition getServerDefinition() {
        return serverDefinition;
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
    protected boolean stillOk(Object value,
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
