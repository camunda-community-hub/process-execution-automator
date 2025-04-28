package org.camunda.automator.bpmnengine.camunda8;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.operate.CamundaOperateClient;
import io.camunda.operate.CamundaOperateClientConfiguration;
import io.camunda.operate.auth.*;
import io.camunda.operate.auth.TokenResponseMapper.JacksonTokenResponseMapper;
import io.camunda.operate.exception.OperateException;
import io.camunda.operate.model.*;
import io.camunda.operate.search.*;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.configuration.BpmnEngineList;
import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.AutomatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;


public class OperateClient {

    public static final int SEARCH_MAX_SIZE = 100;
    private final Logger logger = LoggerFactory.getLogger(OperateClient.class);
    BpmnEngineCamunda8 engineCamunda8;
    private CamundaOperateClient operateClient;

    protected OperateClient(BpmnEngineCamunda8 engineCamunda8) {
        this.engineCamunda8 = engineCamunda8;
    }

    /**
     * Connect Operate
     *
     * @param analysis to cpmplete the analysis
     * @throws AutomatorException in case of error
     */
    protected void connectOperate(StringBuilder analysis) throws AutomatorException {
        BpmnEngineList.BpmnServerDefinition serverDefinition = engineCamunda8.getServerDefinition();

        if (!serverDefinition.isOperate()) {
            analysis.append("No operate connection required, ");
            return;
        }
        analysis.append("Operate connection...");

        boolean isOk = true;
        isOk = engineCamunda8.stillOk(serverDefinition.operateUrl, "operateUrl", analysis, true, true, isOk);

        // CamundaOperateClientBuilder camundaOperateClientBuilder = new CamundaOperateClientBuilder();
        CamundaOperateClientConfiguration configuration = null;
        // ---------------------------- Camunda Saas
        if (BpmnEngineList.CamundaEngine.CAMUNDA_8_SAAS.equals(serverDefinition.serverType)) {

            try {
                isOk = engineCamunda8.stillOk(serverDefinition.zeebeSaasRegion, "zeebeSaasRegion", analysis, true, true, isOk);
                isOk = engineCamunda8.stillOk(serverDefinition.zeebeSaasClusterId, "zeebeSaasClusterId", analysis, true, true, isOk);
                isOk = engineCamunda8.stillOk(serverDefinition.zeebeClientId, "zeebeClientId", analysis, true, true, isOk);
                isOk = engineCamunda8.stillOk(serverDefinition.zeebeClientSecret, "zeebeClientSecret", analysis, true, false, isOk);

                URL operateUrl = URI.create("https://" + serverDefinition.zeebeSaasRegion + ".operate.camunda.io/"
                        + serverDefinition.zeebeSaasClusterId).toURL();
                URL authUrl = URI.create("https://login.cloud.camunda.io/oauth/token").toURL();
                JwtCredential credentials =
                        new JwtCredential(serverDefinition.zeebeClientId, serverDefinition.zeebeClientSecret, "operate.camunda.io", authUrl, null);
                ObjectMapper objectMapper = new ObjectMapper();
                TokenResponseMapper tokenResponseMapper = new JacksonTokenResponseMapper(objectMapper);
                JwtAuthentication authentication = new JwtAuthentication(credentials, tokenResponseMapper);
                configuration =
                        new CamundaOperateClientConfiguration(
                                authentication, operateUrl, objectMapper, HttpClients.createDefault());


            } catch (Exception e) {
                logger.error("Can't connect to SaaS environemnt[{}] Analysis:{} : {}", serverDefinition.name, analysis, e.getMessage(), e);
                throw new AutomatorException(
                        "Can't connect to SaaS environment[" + serverDefinition.name + "] Analysis:" + analysis + " fail : "
                                + e.getMessage());
            }

            //---------------------------- Camunda 8 Self Manage
        } else if (BpmnEngineList.CamundaEngine.CAMUNDA_8.equals(serverDefinition.serverType)) {

            isOk = engineCamunda8.stillOk(serverDefinition.zeebeGatewayAddress, "GatewayAddress", analysis, true, true, isOk);

            try {
                if (serverDefinition.isAuthenticationUrl()) {
                    isOk = engineCamunda8.stillOk(serverDefinition.authenticationUrl, "authenticationUrl", analysis, true, true, isOk);
                    isOk = engineCamunda8.stillOk(serverDefinition.operateClientId, "operateClientId", analysis, true, true, isOk);
                    isOk = engineCamunda8.stillOk(serverDefinition.operateClientSecret, "operateClientSecret", analysis, true, false, isOk);

                    String scope = "";
                    URL authUrl =
                            URI.create(
                                            "http://localhost:18080/auth/realms/camunda-platform/protocol/openid-connect/token")
                                    .toURL();
                    URL operateUrl = URI.create(serverDefinition.operateUrl).toURL();
                    // bootstrapping
                    JwtCredential credentials =
                            new JwtCredential(serverDefinition.operateClientId, serverDefinition.operateClientSecret, "operate-api", authUrl, scope);
                    ObjectMapper objectMapper = new ObjectMapper();
                    TokenResponseMapper tokenResponseMapper = new JacksonTokenResponseMapper(objectMapper);
                    JwtAuthentication authentication = new JwtAuthentication(credentials, tokenResponseMapper);
                    configuration =
                            new CamundaOperateClientConfiguration(
                                    authentication, operateUrl, objectMapper, HttpClients.createDefault());

                } else {
                    // Simple authentication
                    isOk = engineCamunda8.stillOk(serverDefinition.operateUserName, "operateUserName", analysis, true, true, isOk);
                    isOk = engineCamunda8.stillOk(serverDefinition.operateUserPassword, "operateUserPassword", analysis, true, false, isOk);
                    URL operateUrl = URI.create(serverDefinition.operateUrl).toURL();

                    SimpleCredential credentials = new SimpleCredential(serverDefinition.operateUserName, serverDefinition.operateUserPassword, operateUrl, Duration.ofMinutes(10));
                    SimpleAuthentication authentication = new SimpleAuthentication(credentials);
                    ObjectMapper objectMapper = new ObjectMapper();
                    configuration = new CamundaOperateClientConfiguration(authentication, operateUrl, objectMapper, HttpClients.createDefault());
                }
            } catch (Exception e) {
                logger.error("Can't connect to SaaS environment[{}] Analysis:{} : {}", serverDefinition.name, analysis, e.getMessage(), e);
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

            operateClient = new CamundaOperateClient(configuration);

            analysis.append("successfully, ");

        } catch (Exception e) {
            logger.error("Can't connect to Server[{}] Analysis:{} : {}", serverDefinition.name, analysis, e.getMessage(), e);
            throw new AutomatorException(
                    "Can't connect to Server[" + serverDefinition.name + "] Analysis:" + analysis + " Fail : " + e.getMessage());
        }
    }

    public List<String> activateServiceTasks(String processInstanceId, String serviceTaskId, String topic, int maxResult)
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

            ActivateJobsResponse jobsResponse = engineCamunda8.getZeebeClient().newActivateJobsCommand()
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
                    engineCamunda8.getZeebeClient().newFailCommand(job.getKey()).retries(2).send().join();
                }
            }
            return listJobsId;

        } catch (Exception e) {
            throw new AutomatorException("Can't search service task topic[" + topic + "] : " + e.getMessage());
        }
    }

    /**
     * @param processInstanceId filter on the processInstanceId. may be null
     * @param filterTaskId      filter on the taskId
     * @param maxResult         maximum Result
     * @return list of Task
     * @throws AutomatorException in case of error
     */
    public List<BpmnEngine.TaskDescription> searchTasksByProcessInstanceId(String processInstanceId, String filterTaskId, int maxResult)
            throws AutomatorException {
        AutomatorException automatorException = null;

        if (operateClient == null) {
            throw new AutomatorException("No Operate connection was provided");
        }

        int retry = 0;
        while (retry < 3) {
            retry++;
            if (retry > 1)
                logger.info("searchTasksByProcessInstanceId : retry[{}] processInstanceId[{}] taskId[{}]", retry, processInstanceId, filterTaskId);
            try {
                // impossible to filter by the task name/ task tyoe, so be ready to get a lot of flowNode and search the correct onee
                FlowNodeInstanceFilter flownodeFilter = FlowNodeInstanceFilter.builder()
                        .processInstanceKey(Long.valueOf(processInstanceId))
                        .build();

                SearchQuery flowNodeQuery = new SearchQuery.Builder().filter(flownodeFilter).size(maxResult).build();
                // Operate client does not support the multithreading very well (execption in jackson library)
                List<FlowNodeInstance> flowNodes;
                synchronized (this) {
                    flowNodes = operateClient.searchFlowNodeInstances(flowNodeQuery);
                }
                return flowNodes.stream()
                        .filter(t -> filterTaskId == null || filterTaskId.equals(t.getFlowNodeId())) // Filter by name
                        .map(t -> {
                            BpmnEngine.TaskDescription taskDescription = new BpmnEngine.TaskDescription();
                            taskDescription.taskId = t.getFlowNodeId();
                            taskDescription.processInstanceId = String.valueOf(t.getProcessInstanceKey());
                            taskDescription.startDate = t.getStartDate() == null ? null : t.getStartDate().getDate();
                            taskDescription.endDate = t.getEndDate() == null ? null : t.getEndDate().getDate();
                            taskDescription.type = getTaskType(t.getType()); // to implement
                            taskDescription.isCompleted = FlowNodeInstanceState.COMPLETED.equals(t.getState()); // to implement
                            return taskDescription;
                        }).toList();

            } catch (OperateException e) {
                logger.error("Can't search TasksByProcessInstanceId: {} retry[{}]", e.getMessage(), retry, e);
                automatorException = new AutomatorException("Can't search TasksByProcessInstanceId: " + e.getMessage());

            } catch (Exception e) {
                logger.error("Can't search TasksByProcessInstanceId EXCEPTION NOT EXPECTED: {} retry[{}] ", e.getMessage(), retry, e);
                automatorException = new AutomatorException("Can't search TasksByProcessInstanceId: " + e.getMessage());
            }

            // Give some time to Operate
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } // end while
        // We must not be here
        throw automatorException;

    }

    public List<BpmnEngine.ProcessDescription> searchProcessInstanceByVariable(String processId,
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
            // Operate client does not support the multithreading very well (execption in jackson library)
            List<ProcessInstance> listProcessInstances;
            synchronized (this) {
                listProcessInstances = operateClient.searchProcessInstances(processInstanceQuery);
            }
            List<BpmnEngine.ProcessDescription> listProcessInstanceFind = new ArrayList<>();
            // now, we have to filter based on variableName/value

            for (ProcessInstance processInstance : listProcessInstances) {
                Map<String, Object> processVariables = getVariables(processInstance.getKey().toString());
                List<Map.Entry<String, Object>> entriesNotFiltered = filterVariables.entrySet()
                        .stream()
                        .filter(
                                t -> processVariables.containsKey(t.getKey()) && processVariables.get(t.getKey()).equals(t.getValue()))
                        .toList();

                if (entriesNotFiltered.isEmpty()) {

                    BpmnEngine.ProcessDescription processDescription = new BpmnEngine.ProcessDescription();
                    processDescription.processInstanceId = processInstance.getKey().toString();

                    listProcessInstanceFind.add(processDescription);
                }
            }
            return listProcessInstanceFind;
        } catch (OperateException e) {
            logger.error("Can't search searchProcessInstanceByVariable: {} ", e.getMessage(), e);
            throw new AutomatorException("Can't search searchProcessInstanceByVariable " + e.getMessage());
        }
        // We must not be here
        catch (Exception e) {
            logger.error("Can't search flowNodeByVariable EXCEPTION NOT EXPECTED: {} ", e.getMessage(), e);
            throw new AutomatorException("Can't search FlowNode: " + e.getMessage());
        }
    }


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

            // Operate client does not support the multithreading very well (execption in jackson library)
            List<io.camunda.operate.model.Variable> listVariables;
            synchronized (this) {
                listVariables = operateClient.searchVariables(variableQuery);
            }
            Map<String, Object> variables = new HashMap<>();
            listVariables.forEach(t -> variables.put(t.getName(), t.getValue()));

            return variables;
        } catch (OperateException e) {
            logger.error("Can't getVariables: {} ", e.getMessage(), e);
            throw new AutomatorException("Can't search variables task " + e.getMessage());
        }
        // We must not be here
        catch (Exception e) {
            logger.error("Can't getVariables EXCEPTION NOT EXPECTED: {} ", e.getMessage(), e);
            throw new AutomatorException("Can't getVariables: " + e.getMessage());
        }
    }

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
                // Operate client does not support the multithreading very well (execption in jackson library)
                synchronized (this) {
                    searchResult = operateClient.searchProcessInstanceResults(searchQuery);
                }
                cumul += searchResult.getItems().stream().filter(t -> t.getStartDate() != null && t.getStartDate().getDate().after(startDate)).count();

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
                // Operate client does not support the multithreading very well (execption in jackson library)
                synchronized (this) {
                    searchResult = operateClient.searchProcessInstanceResults(searchQuery);
                }
                cumul += searchResult.getItems().stream().filter(t -> t.getStartDate() != null && t.getStartDate().getDate().after(startDate)).count();

            } while (searchResult.getItems().size() >= SEARCH_MAX_SIZE && maxLoop < 1000);
            return cumul;

        } catch (Exception e) {
            throw new AutomatorException("Search countNumberProcessEnded " + e.getMessage());
        }
    }

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
                // Operate client does not support the multithreading very well (exception in jackson library)
                synchronized (this) {
                    searchResult = operateClient.searchFlowNodeInstanceResults(searchQuery);
                }
                cumul += (long) searchResult.getItems().size();
            } while (searchResult.getItems().size() >= SEARCH_MAX_SIZE && maxLoop < 1000);
            return cumul;
        } catch (Exception e) {
            throw new AutomatorException("Search countNumberProcessEnded " + e.getMessage());
        }
    }

    private ScenarioStep.Step getTaskType(String taskTypeC8) {
        return switch (taskTypeC8) {
            case "SERVICE_TASK" -> ScenarioStep.Step.SERVICETASK;
            case "USER_TASK" -> ScenarioStep.Step.USERTASK;
            case "START_EVENT" -> ScenarioStep.Step.STARTEVENT;
            case "END_EVENT" -> ScenarioStep.Step.ENDEVENT;
            case "EXCLUSIVE_GATEWAY" -> ScenarioStep.Step.EXCLUSIVEGATEWAY;
            case "PARALLEL_GATEWAY" -> ScenarioStep.Step.PARALLELGATEWAY;
            case "TASK" -> ScenarioStep.Step.TASK;
            case "SCRIPT_TASK" -> ScenarioStep.Step.SCRIPTTASK;
            default -> null;
        };

    }

}
