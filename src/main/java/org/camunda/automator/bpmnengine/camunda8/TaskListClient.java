package org.camunda.automator.bpmnengine.camunda8;

import io.camunda.tasklist.CamundaTaskListClient;
import io.camunda.tasklist.CamundaTaskListClientBuilder;
import io.camunda.tasklist.auth.Authentication;
import io.camunda.tasklist.auth.SimpleAuthentication;
import io.camunda.tasklist.auth.SimpleCredential;
import io.camunda.tasklist.dto.*;
import io.camunda.tasklist.exception.TaskListException;
import org.camunda.automator.configuration.BpmnEngineList;
import org.camunda.automator.engine.AutomatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskListClient {
    private final Logger logger = LoggerFactory.getLogger(TaskListClient.class);
    BpmnEngineCamunda8 engineCamunda8;
    private CamundaTaskListClient taskClient;

    protected TaskListClient(BpmnEngineCamunda8 engineCamunda8) {
        this.engineCamunda8 = engineCamunda8;
    }

    /**
     * Connect to TaskList
     *
     * @param analysis complete the analysis
     * @throws AutomatorException in case of error
     */
    public void connectTaskList(StringBuilder analysis) throws AutomatorException {

        BpmnEngineList.BpmnServerDefinition serverDefinition = engineCamunda8.getServerDefinition();
        if (!serverDefinition.isTaskList()) {
            analysis.append("No TaskList connection required, ");
            return;
        }
        analysis.append("Tasklist ...");

        boolean isOk = true;
        isOk = engineCamunda8.stillOk(serverDefinition.taskListUrl, "taskListUrl", analysis, true, true, isOk);

        CamundaTaskListClientBuilder taskListBuilder = CamundaTaskListClient.builder();
        // ---------------------------- Camunda Saas
        if (BpmnEngineList.CamundaEngine.CAMUNDA_8_SAAS.equals(serverDefinition.serverType)) {
            try {
                isOk = engineCamunda8.stillOk(serverDefinition.zeebeSaasRegion, "zeebeSaasRegion", analysis, true, true, isOk);
                isOk = engineCamunda8.stillOk(serverDefinition.zeebeSaasClusterId, "zeebeSaasClusterId", analysis, true, true, isOk);
                isOk = engineCamunda8.stillOk(serverDefinition.taskListClientId, "taskListClientId", analysis, true, true, isOk);
                isOk = engineCamunda8.stillOk(serverDefinition.taskListClientSecret, "taskListClientSecret", analysis, true, false, isOk);

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
        } else if (BpmnEngineList.CamundaEngine.CAMUNDA_8.equals(serverDefinition.serverType)) {

            if (serverDefinition.isAuthenticationUrl()) {
                isOk = engineCamunda8.stillOk(serverDefinition.taskListClientId, "taskListClientId", analysis, true, true, isOk);
                isOk = engineCamunda8.stillOk(serverDefinition.taskListClientSecret, "taskListClientSecret", analysis, true, false, isOk);
                isOk = engineCamunda8.stillOk(serverDefinition.authenticationUrl, "authenticationUrl", analysis, true, true, isOk);
                isOk = engineCamunda8.stillOk(serverDefinition.taskListKeycloakUrl, "taskListKeycloakUrl", analysis, true, true, isOk);

                taskListBuilder.taskListUrl(serverDefinition.taskListUrl)
                        .selfManagedAuthentication(serverDefinition.taskListClientId, serverDefinition.taskListClientSecret,
                                serverDefinition.taskListKeycloakUrl);
            } else {
                isOk = engineCamunda8.stillOk(serverDefinition.taskListUserName, "User", analysis, true, true, isOk);
                isOk = engineCamunda8.stillOk(serverDefinition.taskListUserPassword, "Password", analysis, true, false, isOk);
                try {
                    SimpleCredential credentials = new SimpleCredential(serverDefinition.taskListUserName,
                            serverDefinition.taskListUserPassword, new URL(serverDefinition.taskListUrl), Duration.ofHours(1000));
                    Authentication authentication = new SimpleAuthentication(credentials);
                    taskListBuilder.taskListUrl(serverDefinition.taskListUrl).authentication(authentication);
                } catch (MalformedURLException e) {
                    throw new AutomatorException("Invalid Url for TaskList: [" + serverDefinition.taskListUrl + "]");

                }
            }
        } else
            throw new AutomatorException("Invalid configuration");

        if (!isOk)
            throw new AutomatorException("Invalid configuration " + analysis);

        // ---------------- connection
        try {
            taskListBuilder.zeebeClient(engineCamunda8.getZeebeClient());
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
      isOk = engineCamunda8.stillOk(serverDefinition.taskListUrl, "taskListUrl", analysis, false, isOk);
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
                if (!engineCamunda8.isHightFlowMode()) {
                    // We check that the task is the one expected
                    listTasksResult.addAll(tasksList.getItems().stream().filter(t -> {
                                List<Variable> listVariables = t.getVariables();
                                Long processInstanceIdTask = engineCamunda8.getProcessInstanceIdFromMarker(listVariables);
                                if (processInstanceIdTask == null) {
                                    return false;
                                }
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
            logger.error("TaskListClient: error during search task [{}]", e.getMessage());
            throw new AutomatorException("Can't search users task " + e.getMessage());
        }
    }

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

    public void executeUserTask(String userTaskId, String userId, Map<String, Object> variables)
            throws AutomatorException {
        try {
            taskClient.claim(userTaskId, engineCamunda8.getServerDefinition().operateUserName);
            taskClient.completeTask(userTaskId, variables);
        } catch (TaskListException e) {
            throw new AutomatorException("Can't execute task [" + userTaskId + "]");
        } catch (Exception e) {
            throw new AutomatorException("Can't execute task [" + userTaskId + "]");
        }
    }
}
