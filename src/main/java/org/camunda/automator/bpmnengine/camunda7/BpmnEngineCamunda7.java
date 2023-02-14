package org.camunda.automator.bpmnengine.camunda7;

import org.camunda.automator.bpmnengine.AutomatorException;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.bpmnengine.BpmnEngineConfiguration;
import org.camunda.community.rest.client.api.ProcessDefinitionApi;
import org.camunda.community.rest.client.api.TaskApi;
import org.camunda.community.rest.client.dto.CompleteTaskDto;
import org.camunda.community.rest.client.dto.ProcessInstanceWithVariablesDto;
import org.camunda.community.rest.client.dto.StartProcessInstanceDto;
import org.camunda.community.rest.client.dto.TaskDto;
import org.camunda.community.rest.client.dto.TaskQueryDto;
import org.camunda.community.rest.client.dto.UserIdDto;
import org.camunda.community.rest.client.dto.VariableValueDto;
import org.camunda.community.rest.client.invoker.ApiClient;
import org.camunda.community.rest.client.invoker.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BpmnEngineCamunda7 implements BpmnEngine {

  private final Logger logger = LoggerFactory.getLogger(BpmnEngineCamunda7.class);

  private final BpmnEngineConfiguration engineConfiguration;

  ApiClient client = null;
  ProcessDefinitionApi processDefinitionApi;
  TaskApi taskApi;

  public BpmnEngineCamunda7(BpmnEngineConfiguration engineConfiguration) {
    this.engineConfiguration = engineConfiguration;
  }

  @Override
  public void init() {
    client = new ApiClient();
    client.setBasePath(engineConfiguration.serverUrl);
    processDefinitionApi = new ProcessDefinitionApi();
    taskApi = new TaskApi();
  }

  @Override
  public String createProcessInstance(String processId, String starterEventId, Map<String, Object> variables)
      throws AutomatorException {
    if (engineConfiguration.logDebug) {
      logger.info("BpmnEngine7.CreateProcessInstance: Process[" + processId + "] StartEvent[" + starterEventId + "]");
    }
    Map<String, VariableValueDto> variablesApi = new HashMap<>();
    for (Map.Entry<String, Object> entry : variables.entrySet()) {
      variablesApi.put(entry.getKey(), new VariableValueDto().value(entry.getValue()));
    }
    try {
      ProcessInstanceWithVariablesDto processInstanceDto = processDefinitionApi.startProcessInstanceByKey(processId,
          new StartProcessInstanceDto().variables(variablesApi));
      return processInstanceDto.getId();
    } catch (ApiException e) {
      throw new AutomatorException("Can't create process instance ", e);
    }
  }

  @Override
  public List<String> searchForActivity(String processInstanceId, String taskName, Integer maxResult)
      throws AutomatorException {
    if (engineConfiguration.logDebug) {
      logger.info("BpmnEngine7.searchForActivity: Process[" + processInstanceId + "] taskName[" + taskName + "]");
    }
    TaskQueryDto taskQueryDto = new TaskQueryDto();
    taskQueryDto.addProcessInstanceIdInItem(processInstanceId);
    taskQueryDto.addTaskDefinitionKeyInItem(taskName);
    List<TaskDto> taskDtos = null;
    try {
      taskDtos = taskApi.queryTasks(0, maxResult, taskQueryDto);
    } catch (ApiException e) {
      throw new AutomatorException("Can't searchTask", e);
    }
    return taskDtos.stream().map(t -> t.getId()).toList();
  }

  @Override
  public String executeUserTask(String taskId, String userId, Map<String, Object> variables) throws AutomatorException {

    if (engineConfiguration.logDebug) {
      logger.info("BpmnEngine7.executeUserTask: activityId[" + taskId + "]");
    }
    try {
      UserIdDto userIdDto = new UserIdDto();
      userIdDto.setUserId(userId == null ? "automator" : userId);
      taskApi.claim(taskId, userIdDto);
      Map<String, VariableValueDto> variablesApi = new HashMap<>();
      for (Map.Entry<String, Object> entry : variables.entrySet()) {
        variablesApi.put(entry.getKey(), new VariableValueDto().value(entry.getValue()));
      }

      taskApi.complete(taskId, new CompleteTaskDto().variables(variablesApi));
      return null;
    } catch (ApiException e) {
      throw new AutomatorException("Can't execute task", e);
    }
  }

}
