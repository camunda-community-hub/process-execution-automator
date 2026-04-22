package org.camunda.automator.bpmnengine.camunda8;

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.engine.AutomatorException;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface OperateClientInt {

    BpmnEngine.ConnectionStatus testAdminConnection();

    void connectOperate(StringBuilder analysis) throws AutomatorException;

    List<String> activateServiceTasks(String processInstanceId, String serviceTaskId, String topic, int maxResult)
            throws AutomatorException;

    List<BpmnEngine.TaskDescription> searchTasksByProcessInstanceId(String processInstanceId, String filterTaskId, int maxResult)
            throws AutomatorException;

    List<BpmnEngine.ProcessDescription> searchProcessInstanceByVariable(String processId,
                                                                        Map<String, Object> filterVariables,
                                                                        int maxResult) throws AutomatorException;

    Map<String, Object> getVariables(String processInstanceId) throws AutomatorException;

    long countNumberOfProcessInstancesCreated(String processId, Date startDate, Date endDate)
            throws AutomatorException;

    long countNumberOfProcessInstancesEnded(String processId, Date startDate, Date endDate)
            throws AutomatorException;

    long countNumberOfTasks(String processId, String taskId) throws AutomatorException;

}
