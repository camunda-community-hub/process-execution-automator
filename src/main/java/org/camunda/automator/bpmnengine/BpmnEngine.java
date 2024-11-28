package org.camunda.automator.bpmnengine;

import io.camunda.operate.search.DateFilter;
import io.camunda.zeebe.client.api.worker.JobWorker;
import org.camunda.automator.configuration.BpmnEngineList;
import org.camunda.automator.definition.ScenarioDeployment;
import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.flow.FixedBackoffSupplier;
import org.camunda.bpm.client.topic.TopicSubscription;

import java.io.File;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface BpmnEngine {

    /**
     * init the engine. This method will
     *
     * @throws Exception in case of error
     */
    void init();

    void connection() throws AutomatorException;

    void disconnection() throws AutomatorException;

    /**
     * Engine is ready. If not, a connection() method must be call
     *
     * @return true if the engine is ready
     */
    boolean isReady();

    /* ******************************************************************** */
    /*                                                                      */
    /*  Manage process instance                                             */
    /*                                                                      */
    /* ******************************************************************** */
    void turnHighFlowMode(boolean hightFlowMode);

    /**
     * @param processId      Process ID (BPMN ID : ExpenseNode)
     * @param starterEventId BPMN ID (startEvent)
     * @param variables      List of variables to create the process instance
     * @return a processInstanceId
     * @throws AutomatorException in case of error
     */
    String createProcessInstance(String processId, String starterEventId, Map<String, Object> variables)
            throws AutomatorException;

    /**
     * we finish with this processinstanceid, engine can clean it
     *
     * @param processInstanceId Process instance Id to clean
     * @param cleanAll          if true, the process instance must be clean.
     * @throws AutomatorException in case of error
     */
    void endProcessInstance(String processInstanceId, boolean cleanAll) throws AutomatorException;


    /* ******************************************************************** */
    /*                                                                      */
    /*  User task                                                           */
    /*                                                                      */
    /* ******************************************************************** */

    /**
     * @param processInstanceId Process Instance Id
     * @param filterTaskId      If not null, list if filtered to return only this task
     * @param maxResult         maximum result to return.
     * @return list of taskId
     * @throws AutomatorException in case of error
     */
    List<String> searchUserTasksByProcessInstance(String processInstanceId, String filterTaskId, int maxResult)
            throws AutomatorException;

    /**
     * Return a list of task
     *
     * @param userTaskId userTaskId
     * @param maxResult  maxResult returned
     * @return list of TaskId
     * @throws AutomatorException in case of error
     */
    List<String> searchUserTasks(String userTaskId, int maxResult) throws AutomatorException;

    /**
     * @param userTaskId BPMN Id (Review)
     * @param userId     User id who executes the task
     * @param variables  variable to update
     * @throws AutomatorException in case of error
     */
    void executeUserTask(String userTaskId, String userId, Map<String, Object> variables) throws AutomatorException;


    /* ******************************************************************** */
    /*                                                                      */
    /*  Service tasks                                                       */
    /*                                                                      */
    /* ******************************************************************** */

    /**
     * @param workerId        workerId
     * @param topic           topic to register
     * @param streamEnabled   true if the stream enable is open
     * @param lockTime        lock time for the job
     * @param jobHandler      C7: must implement ExternalTaskHandler. C8: must implement JobHandler
     * @param backoffSupplier backOffStrategy
     * @return list of Service Task
     */
    RegisteredTask registerServiceTask(String workerId,
                                       String topic,
                                       boolean streamEnabled,
                                       Duration lockTime,
                                       Object jobHandler,
                                       FixedBackoffSupplier backoffSupplier);

    /**
     * @param processInstanceId process instance ID
     * @param serviceTaskId     BPMN IP (Review)
     * @param topic             topic to search to execute the service task
     * @param maxResult         maximum result
     * @return list of taskId
     * @throws AutomatorException in case of error
     */
    List<String> searchServiceTasks(String processInstanceId, String serviceTaskId, String topic, int maxResult)
            throws AutomatorException;

    /**
     * Execute a service task
     *
     * @param serviceTaskId BPMN ID (Review)
     * @param workerId      Worker who execute the task
     * @param variables     variable to updates
     * @throws AutomatorException in case of error
     */
    void executeServiceTask(String serviceTaskId, String workerId, Map<String, Object> variables)
            throws AutomatorException;

    /**
     * Search task.
     *
     * @param processInstanceId filter on the processInstanceId. may be null
     * @param taskId            filter on the taskId
     * @param maxResult         maximum Result
     * @return List of task description
     * @throws AutomatorException in case of error
     */
    List<TaskDescription> searchTasksByProcessInstanceId(String processInstanceId, String taskId, int maxResult)
            throws AutomatorException;

    /* ******************************************************************** */
    /*                                                                      */
    /*  Generic tasks                                                       */
    /*                                                                      */
    /* ******************************************************************** */

    /**
     * Search process instance by a variable content
     *
     * @param processId       BPMN Process ID
     * @param filterVariables Variable name
     * @param maxResult       maxResult
     * @return list of ProcessInstance which match the filter
     * @throws AutomatorException in case of error
     */
    List<ProcessDescription> searchProcessInstanceByVariable(String processId,
                                                             Map<String, Object> filterVariables,
                                                             int maxResult) throws AutomatorException;

    /**
     * Get variables of a process instanceId
     *
     * @param processInstanceId the process instance ID
     * @return variables attached to the process instance ID
     * @throws AutomatorException in case of error
     */
    Map<String, Object> getVariables(String processInstanceId) throws AutomatorException;

    /* ******************************************************************** */
    /*                                                                      */
    /*  CountInformation                                                    */
    /*                                                                      */
    /* ******************************************************************** */
    long countNumberOfProcessInstancesCreated(String processId, DateFilter startDate, DateFilter endDate)
            throws AutomatorException;

    long countNumberOfProcessInstancesEnded(String processId, DateFilter startDate, DateFilter endDate)
            throws AutomatorException;

    long countNumberOfTasks(String processId, String taskId) throws AutomatorException;

    /**
     * Deploy a BPMN file (may contains multiple processes)
     *
     * @param processFile process to deploy
     * @param policy      policy to deploy the process
     * @return the deploymentId
     * @throws AutomatorException in case of error
     */
    String deployBpmn(File processFile, ScenarioDeployment.Policy policy) throws AutomatorException;

    /* ******************************************************************** */
    /*                                                                      */
    /*  Deployment                                                          */
    /*                                                                      */
    /* ******************************************************************** */

    BpmnEngineList.CamundaEngine getTypeCamundaEngine();


    /* ******************************************************************** */
    /*                                                                      */
    /*  get server definition                                               */
    /*                                                                      */
    /* ******************************************************************** */

    /**
     * return the signature of the engine, to log it for example
     *
     * @return signature of the engine
     */
    String getSignature();

    int getWorkerExecutionThreads();

    class RegisteredTask {
        public TopicSubscription topicSubscription;
        public JobWorker jobWorker;

        public boolean isNull() {
            return topicSubscription == null && jobWorker == null;
        }

        public boolean isClosed() {
            if (jobWorker != null)
                return jobWorker.isClosed();
            return topicSubscription == null;
        }

        public void close() {
            if (jobWorker != null)
                jobWorker.close();
            if (topicSubscription != null) {
                topicSubscription.close();
                topicSubscription = null;
            }
        }
    }

    class TaskDescription {
        public String processInstanceId;
        public String taskId;
        public ScenarioStep.Step type;
        public boolean isCompleted;
        public Date startDate;
        public Date endDate;
    }

    class ProcessDescription {
        public String processInstanceId;
    }
}
