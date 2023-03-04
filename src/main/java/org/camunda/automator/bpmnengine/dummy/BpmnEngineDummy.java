package org.camunda.automator.bpmnengine.dummy;

import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.bpmnengine.BpmnEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class BpmnEngineDummy implements BpmnEngine {

  private final BpmnEngineConfiguration engineConfiguration;
  private final Logger logger = LoggerFactory.getLogger(BpmnEngineDummy.class);

  public BpmnEngineDummy(BpmnEngineConfiguration engineConfiguration) {
    this.engineConfiguration = engineConfiguration;
  }

  @Override
  public void init() {
    logger.info("BpmnEngineDummy.Init:");
  }

  @Override
  public String createProcessInstance(String processId, String starterEventId, Map<String, Object> variables)
      throws AutomatorException {
    logger.info("BpmnEngineDummy.CreateProcessInstance: Process[" + processId + "] StartEvent[" + starterEventId + "]");
    return "111";
  }

  @Override
  public List<String> SearchForUserTasks(String processId, String userTaskName, Integer maxResult)
      throws AutomatorException {
    logger.info("BpmnEngineDummy.searchForActivity: Process[" + processId + "] taskName[" + userTaskName + "]");
    return List.of("5555");
  }

  @Override
  public String executeUserTask(String activityId, String userId, Map<String, Object> variables)
      throws AutomatorException {

    logger.info("BpmnEngineDummy.executeUserTask: activityId[" + activityId + "]");
    return "444";
  }

  @Override
  public List<String> SearchForServiceTasks(String processId, String userTaskName, Integer maxResult)
      throws AutomatorException {
    return null;
  }

  @Override
  public String executeServiceTask(String activityId, String userId, Map<String, Object> variables)
      throws AutomatorException {
    return null;
  }
}
