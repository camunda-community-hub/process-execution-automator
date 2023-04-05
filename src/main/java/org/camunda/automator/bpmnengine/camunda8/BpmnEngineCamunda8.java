package org.camunda.automator.bpmnengine.camunda8;

import io.camunda.operate.CamundaOperateClient;
import io.camunda.operate.dto.FlownodeInstance;
import io.camunda.operate.dto.FlownodeInstanceState;
import io.camunda.operate.dto.ProcessInstance;
import io.camunda.operate.exception.OperateException;
import io.camunda.operate.search.FlownodeInstanceFilter;
import io.camunda.operate.search.ProcessInstanceFilter;
import io.camunda.operate.search.SearchQuery;
import io.camunda.operate.search.VariableFilter;
import io.camunda.tasklist.CamundaTaskListClient;
import io.camunda.tasklist.dto.Pagination;
import io.camunda.tasklist.dto.Task;
import io.camunda.tasklist.dto.TaskList;
import io.camunda.tasklist.dto.TaskSearch;
import io.camunda.tasklist.dto.TaskState;
import io.camunda.tasklist.dto.Variable;
import io.camunda.tasklist.exception.TaskListException;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import io.camunda.zeebe.client.api.response.ActivateJobsResponse;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.DeploymentEvent;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.bpmnengine.BpmnEngineConfiguration;
import org.camunda.automator.definition.ScenarioDeployment;
import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.AutomatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class BpmnEngineCamunda8 implements BpmnEngine {

  public static final String THIS_IS_A_COMPLETE_IMPOSSIBLE_VARIABLE_NAME = "ThisIsACompleteImpossibleVariableName";
  private final Logger logger = LoggerFactory.getLogger(BpmnEngineCamunda8.class);

  private final BpmnEngineConfiguration engineConfiguration;
  private final BpmnEngineConfiguration.BpmnServerDefinition serverDefinition;

  private ZeebeClient zeebeClient;
  private CamundaOperateClient operateClient;
  private CamundaTaskListClient taskClient;

  private BpmnEngineConfiguration.CamundaEngine typeCamundaEngine;

  public BpmnEngineCamunda8(BpmnEngineConfiguration engineConfiguration,
                            BpmnEngineConfiguration.BpmnServerDefinition serverDefinition) {
    this.engineConfiguration = engineConfiguration;
    this.serverDefinition = serverDefinition;
  }

  @Override
  public void init() throws AutomatorException {
    final String defaultAddress = "localhost:26500";
    final String envVarAddress = System.getenv("ZEEBE_ADDRESS");

    final ZeebeClientBuilder clientBuilder;
    io.camunda.operate.auth.AuthInterface saOperate;
    io.camunda.tasklist.auth.AuthInterface saTaskList;

    if (serverDefinition.zeebeCloudRegister != null) {
      /* Connect to Camunda Cloud Cluster, assumes that credentials are set in environment variables.
       * See JavaDoc on class level for details
       */
      clientBuilder = ZeebeClient.newClientBuilder();
      saOperate = new io.camunda.operate.auth.SaasAuthentication(serverDefinition.zeebeCloudClientId,
          serverDefinition.clientSecret);
      saTaskList = new io.camunda.tasklist.auth.SaasAuthentication(serverDefinition.zeebeCloudClientId,
          serverDefinition.clientSecret);

      typeCamundaEngine = BpmnEngineConfiguration.CamundaEngine.CAMUNDA_8_SAAS;

      // Camunda 8 Self Manage
    } else if (serverDefinition.zeebeGatewayAddress != null) {
      // connect to local deployment; assumes that authentication is disabled
      clientBuilder = ZeebeClient.newClientBuilder()
          .gatewayAddress(serverDefinition.zeebeGatewayAddress)
          .usePlaintext();
      saOperate = new io.camunda.operate.auth.SimpleAuthentication(serverDefinition.operateUserName,
          serverDefinition.operateUserPassword, serverDefinition.operateUrl);
      saTaskList = new io.camunda.tasklist.auth.SimpleAuthentication(serverDefinition.operateUserName,
          serverDefinition.operateUserPassword);
      typeCamundaEngine = BpmnEngineConfiguration.CamundaEngine.CAMUNDA_8;
    } else
      throw new AutomatorException("Invalid configuration");

    try {
      zeebeClient = clientBuilder.build();
      operateClient = new CamundaOperateClient.Builder().operateUrl(serverDefinition.operateUrl)
          .authentication(saOperate)
          .build();

      taskClient = new CamundaTaskListClient.Builder().taskListUrl(serverDefinition.tasklistUrl)
          .authentication(saTaskList)
          .build();
      //get tasks assigned to demo

    } catch (Exception e) {
      throw new AutomatorException("Can't connect to Zeebe " + e.getMessage());
    }

  }

  Map<String, Long> cacheProcessInstanceMarker = new HashMap<>();

  /* ******************************************************************** */
  /*                                                                      */
  /*  Manage process instance                                             */
  /*                                                                      */
  /* ******************************************************************** */

  @Override
  public String createProcessInstance(String processId, String starterEventId, Map<String, Object> variables)
      throws AutomatorException {
    try {
      String marker = getUniqueMarker(processId, starterEventId);

      variables.put(THIS_IS_A_COMPLETE_IMPOSSIBLE_VARIABLE_NAME, marker);
      ProcessInstanceEvent workflowInstanceEvent = zeebeClient.newCreateInstanceCommand()
          .bpmnProcessId(processId)
          .latestVersion()
          .variables(variables)
          .send()
          .join();
      Long processInstanceId = workflowInstanceEvent.getProcessInstanceKey();
      cacheProcessInstanceMarker.put(marker, processInstanceId);
      return String.valueOf(processInstanceId);
    } catch (Exception e) {
      throw new AutomatorException("Can't create in process [" + processId + "]");
    }
  }

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


  /* ******************************************************************** */
  /*                                                                      */
  /*  User tasks                                                          */
  /*                                                                      */
  /* ******************************************************************** */

  @Override
  public List<String> searchUserTasks(String processInstanceId, String userTaskId, int maxResult)
      throws AutomatorException {
    try {
      /*
      // impossible to filter by the task name/ task type, so be ready to get a lot of flowNode and search the correct onee
      FlownodeInstanceFilter flownodeFilter = new FlownodeInstanceFilter.Builder().processInstanceKey(Long.valueOf(processInstanceId)) // filter on the process instance
          .state(FlownodeInstanceState.ACTIVE) // only active task
          .build();

      SearchQuery flownodeQuery = new SearchQuery.Builder().filter(flownodeFilter).size(1000).build();
      List<FlownodeInstance> flownodes = operateClient.searchFlownodeInstances(flownodeQuery);

      List<FlownodeInstance> listUserTask = flownodes.stream()
          .filter(t -> t.getType().equals("USER_TASK"))
          .collect(Collectors.toList());
      for (FlownodeInstance flownodeInstance : listUserTask) {
        Task task = taskClient.getTask(String.valueOf(flownodeInstance.getKey()));
        String taskId = task.getId();
      }
*/
      Long processInstanceIdLong = Long.valueOf(processInstanceId);

      TaskSearch taskSearch = new TaskSearch();
      taskSearch.setState(TaskState.CREATED);
      taskSearch.setAssigned(Boolean.FALSE);
      taskSearch.setWithVariables(true);
      taskSearch.setPagination(new Pagination().setPageSize(maxResult));

      TaskList tasksList = taskClient.getTasks(taskSearch);
      List<String> listTasksResult = new ArrayList<>();
      do {
        listTasksResult.addAll(tasksList.getItems().stream().filter(t -> {
              List<Variable> listVariables = t.getVariables();
              Optional<Variable> markerTask = listVariables.stream()
                  .filter(v -> v.getName().equals(THIS_IS_A_COMPLETE_IMPOSSIBLE_VARIABLE_NAME))
                  .findFirst();

              if (markerTask.isEmpty())
                return false;
              Long processInstanceIdTask = cacheProcessInstanceMarker.get(markerTask.get().getValue());
              return (processInstanceIdLong.equals(processInstanceIdTask));
            }).map(Task::getId) // Task to ID
            .toList());

        tasksList = taskClient.after(tasksList);
      } while (tasksList.size() > 0);

      return listTasksResult;

    } catch (TaskListException e) {
      throw new AutomatorException("Can't search users task " + e.getMessage());
    }
  }

  @Override
  public void executeUserTask(String userTaskId, String userId, Map<String, Object> variables)
      throws AutomatorException {
    try {
      taskClient.claim(userTaskId, serverDefinition.operateUserName);
      taskClient.completeTask(userTaskId, variables);
    } catch (TaskListException e) {
      throw new AutomatorException("Can't execute task [" + userTaskId + "]");
    }

    return;
  }



  /* ******************************************************************** */
  /*                                                                      */
  /*  Service tasks                                                       */
  /*                                                                      */
  /* ******************************************************************** */

  @Override
  public List<String> searchServiceTasks(String processInstanceId, String serviceTaskId, String topic, int maxResult)
      throws AutomatorException {
    try {
      long processInstanceIdLong = Long.valueOf(processInstanceId);
      ActivateJobsResponse jobsResponse = zeebeClient.newActivateJobsCommand()
          .jobType(topic)
          .maxJobsToActivate(10000)
          .workerName( Thread.currentThread().getName())
          .send()
          .join();
      List<String> listJobsId = new ArrayList<>();

      for (ActivatedJob job : jobsResponse.getJobs()) {
        if (job.getProcessInstanceKey() == processInstanceIdLong)
          listJobsId.add(String.valueOf(job.getKey()));
        else {
          zeebeClient.newFailCommand(job.getKey()).retries(2).send().join();
        }
      }
      return listJobsId;

    } catch (Exception e) {
      throw new AutomatorException("Can't search users task " + e.getMessage());
    }
  }

  @Override
  public void executeServiceTask(String serviceTaskId, String workerId, Map<String, Object> variables)
      throws AutomatorException {
    try {
      zeebeClient.newCompleteCommand(Long.valueOf(serviceTaskId)).send().join();
    } catch (Exception e) {
      throw new AutomatorException("Can't execute service task " + e.getMessage());
    }
  }




  /* ******************************************************************** */
  /*                                                                      */
  /*  generic search                                                       */
  /*                                                                      */
  /* ******************************************************************** */

  @Override
  public List<TaskDescription> searchTasksByProcessInstanceId(String processInstanceId, String taskId, int maxResult)
      throws AutomatorException {
    try {
      // impossible to filter by the task name/ task tyoe, so be ready to get a lot of flowNode and search the correct onee
      FlownodeInstanceFilter flownodeFilter = new FlownodeInstanceFilter.Builder().processInstanceKey(
          Long.valueOf(processInstanceId)).build();

      SearchQuery flownodeQuery = new SearchQuery.Builder().filter(flownodeFilter).size(maxResult).build();
      List<FlownodeInstance> flownodes = operateClient.searchFlownodeInstances(flownodeQuery);
      List<TaskDescription> taskList = flownodes.stream().filter(t -> taskId.equals(t.getFlowNodeId())).map(t -> {
        TaskDescription taskDescription = new TaskDescription();
        taskDescription.taskId = t.getFlowNodeId();
        taskDescription.type = getTaskType(t.getType()); // to implement
        taskDescription.isCompleted = FlownodeInstanceState.COMPLETED.equals(t.getState()); // to implement
        return taskDescription;
      }).toList();

      return taskList;

    } catch (OperateException e) {
      throw new AutomatorException("Can't search users task " + e.getMessage());
    }
  }

  public List<ProcessDescription> searchProcessInstanceByVariable(String processId,
                                                                  Map<String, Object> filterVariables,
                                                                  int maxResult) throws AutomatorException {
    try {
      // impossible to filter by the task name/ task tyoe, so be ready to get a lot of flowNode and search the correct onee
      ProcessInstanceFilter processInstanceFilter = new ProcessInstanceFilter.Builder().bpmnProcessId(processId)
          .build();

      SearchQuery processInstanceQuery = new SearchQuery.Builder().filter(processInstanceFilter)
          .size(maxResult)
          .build();
      List<ProcessInstance> listProcessInstances = operateClient.searchProcessInstances(processInstanceQuery);

      List<ProcessDescription> listProcessInstanceFind = new ArrayList<>();
      // now, we have to filter based on variableName/value

      for (ProcessInstance processInstance : listProcessInstances) {
        Map<String, Object> processVariables = getVariables(processInstance.getKey().toString());
        List<Map.Entry<String, Object>> entriesNotFiltered = filterVariables.entrySet()
            .stream()
            .filter(
                t -> processVariables.containsKey(t.getKey()) && processVariables.get(t.getKey()).equals(t.getValue()))
            .toList();

        if (entriesNotFiltered.isEmpty()) {

          ProcessDescription processDescription = new ProcessDescription();
          processDescription.processInstanceId = processInstance.getKey().toString();

          listProcessInstanceFind.add(processDescription);
        }
      }
      return listProcessInstanceFind;
    } catch (OperateException e) {
      throw new AutomatorException("Can't search users task " + e.getMessage());
    }
  }

  private ScenarioStep.Step getTaskType(String taskTypeC8) {
    if (taskTypeC8.equals("SERVICE_TASK"))
      return ScenarioStep.Step.SERVICETASK;
    else if (taskTypeC8.equals("USER_TASK"))
      return ScenarioStep.Step.USERTASK;
    else if (taskTypeC8.equals("START_EVENT"))
      return ScenarioStep.Step.STARTEVENT;
    else if (taskTypeC8.equals("END_EVENT"))
      return ScenarioStep.Step.ENDEVENT;
    else if (taskTypeC8.equals("EXCLUSIVE_GATEWAY"))
      return ScenarioStep.Step.EXCLUSIVEGATEWAY;
    else if (taskTypeC8.equals("PARALLEL_GATEWAY"))
      return ScenarioStep.Step.PARALLELGATEWAY;

    return null;
  }

  @Override
  public Map<String, Object> getVariables(String processInstanceId) throws AutomatorException {
    try {
      // impossible to filter by the task name/ task tyoe, so be ready to get a lot of flowNode and search the correct onee
      VariableFilter variableFilter = new VariableFilter.Builder().processInstanceKey(Long.valueOf(processInstanceId))
          .build();

      SearchQuery variableQuery = new SearchQuery.Builder().filter(variableFilter).build();
      List<io.camunda.operate.dto.Variable> listVariables = operateClient.searchVariables(variableQuery);

      Map<String, Object> variables = new HashMap<>();
      listVariables.forEach(t -> variables.put(t.getName(), t.getValue()));

      return variables;
    } catch (OperateException e) {
      throw new AutomatorException("Can't search variables task " + e.getMessage());
    }
  }



  /* ******************************************************************** */
  /*                                                                      */
  /*  Deployment                                                          */
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
    } catch(Exception e) {
      throw new AutomatorException("Can't deploy "+e.getMessage());
    }
  }


  /* ******************************************************************** */
  /*                                                                      */
  /*  get server definition                                               */
  /*                                                                      */
  /* ******************************************************************** */

  @Override
  public BpmnEngineConfiguration.CamundaEngine getServerDefinition() {
    return typeCamundaEngine;
  }

  Random random = new Random(System.currentTimeMillis());

  private String getUniqueMarker(String processId, String starterEventId) {
    return processId + "-" + random.nextInt(1000000);
  }
}
