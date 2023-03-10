package org.camunda.automator.bpmnengine;

import org.camunda.automator.engine.AutomatorException;

import java.util.List;
import java.util.Map;

public interface BpmnEngine {

  void init() throws Exception;

  String createProcessInstance(String processId, String starterEventId, Map<String, Object> variables)
      throws AutomatorException;

  List<String> SearchForUserTasks(String processId, String userTaskName, Integer maxResult) throws AutomatorException;

  String executeUserTask(String activityId, String userId, Map<String, Object> variables) throws AutomatorException;

  List<String> SearchForServiceTasks(String processId, String userTaskName, Integer maxResult) throws AutomatorException;

  String executeServiceTask(String activityId, String userId, Map<String, Object> variables) throws AutomatorException;

}
