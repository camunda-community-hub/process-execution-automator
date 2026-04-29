package org.camunda.automator.bpmnengine.camunda8;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.AssignUserTaskResponse;
import io.camunda.client.api.response.CompleteUserTaskResponse;
import io.camunda.client.api.search.response.SearchResponse;
import io.camunda.client.api.search.response.UserTask;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.engine.AutomatorException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TaskListClientV2 implements TaskListClientInt {

    private final CamundaClient camundaClient;
    private final BpmnEngineCamunda8 bpmnEngineCamunda8;

    public TaskListClientV2(BpmnEngineCamunda8 bpmnEngineCamunda8) {
        this.camundaClient = bpmnEngineCamunda8.getCamundaClient();
        this.bpmnEngineCamunda8 = bpmnEngineCamunda8;
    }

    public void connectTaskList(StringBuilder analysis) throws AutomatorException {
        // nothing to do: already connected to CamundaClient
    }

    public BpmnEngine.ConnectionStatus testAdminConnection() {
        BpmnEngine.ConnectionStatus connectionStatus = new BpmnEngine.ConnectionStatus();
        connectionStatus.status = BpmnEngine.CONNECTION_STATUS.OK;
        return connectionStatus;
    }

    public List<String> searchUserTasksByProcessInstance(String processInstanceKey, String userTaskId, int maxResult) {
        SearchResponse<UserTask> tasksList = camundaClient.newUserTaskSearchRequest()
                .filter(f -> f
                        .processInstanceKey(Long.parseLong(processInstanceKey))
                        .elementId(userTaskId)
                        .assignee((String) null))
                .page(p -> p.limit(10))
                .send()
                .join();
        List<String> listTasksResult = tasksList.items().stream()
                .map(UserTask::getUserTaskKey)
                .map(String::valueOf)// Task to ID
                .collect(Collectors.toList());

        return listTasksResult;

    }

    public List<String> searchUserTasks(String userTaskId, int maxResult) throws AutomatorException {
        SearchResponse<UserTask> tasksList = camundaClient.newUserTaskSearchRequest()
                .filter(f -> f
                        .elementId(userTaskId)
                        .assignee((String) null))
                .page(p -> p.limit(10))
                .send()
                .join();
        List<String> listTasksResult = tasksList.items().stream()
                .map(UserTask::getElementId) // Task to ID
                .collect(Collectors.toList());

        return listTasksResult;
    }


    public void executeUserTask(String userTaskKey, String userId, Map<String, Object> variables) throws AutomatorException {
        try {
            AssignUserTaskResponse assignResponse =
                    camundaClient
                            .newAssignUserTaskCommand(Long.parseLong(userTaskKey))
                            .assignee(userId == null ? "ProcessExecutionAutomator" : userId)
                            .send()
                            .join();

            // 2) Complete the task (optionally with variables)
            CompleteUserTaskResponse completeResponse =
                    camundaClient
                            .newCompleteUserTaskCommand(Long.parseLong(userTaskKey))
                            .variables(variables)          // or omit if no variables
                            .send()
                            .join();
        } catch (Exception e) {
            throw new AutomatorException("Can't execute task [" + userTaskKey + "]");
        }
    }

}
