package org.camunda.automator.bpmnengine.camunda8;

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.engine.AutomatorException;

import java.util.List;
import java.util.Map;

public interface TaskListClientInt {

    void connectTaskList(StringBuilder analysis) throws AutomatorException;

    BpmnEngine.ConnectionStatus testAdminConnection();

    List<String> searchUserTasksByProcessInstance(String processInstanceId, String userTaskId, int maxResult) throws AutomatorException;

    List<String> searchUserTasks(String userTaskId, int maxResult) throws AutomatorException;

    void executeUserTask(String taskId, String userId, Map<String, Object> variables) throws AutomatorException;

}
