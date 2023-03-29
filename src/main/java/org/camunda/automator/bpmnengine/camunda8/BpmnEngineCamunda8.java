package org.camunda.automator.bpmnengine.camunda8;

import io.camunda.operate.CamundaOperateClient;
import io.camunda.operate.dto.FlownodeInstance;
import io.camunda.operate.dto.FlownodeInstanceState;
import io.camunda.operate.exception.OperateException;
import io.camunda.operate.search.FlownodeInstanceFilter;
import io.camunda.operate.search.SearchQuery;
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
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.client.api.worker.JobWorkerBuilderStep1;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.bpmnengine.BpmnEngineConfiguration;
import org.camunda.automator.definition.ScenarioDeployment;
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
import java.util.stream.Collectors;

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
  public void endProcessInstance(String processInstanceid, boolean cleanAll) throws AutomatorException {
    // clean in the cache
    List<String> markers = cacheProcessInstanceMarker.entrySet()
        .stream()
        .filter(t -> t.getValue().equals(Long.valueOf(processInstanceid)))
        .map(Map.Entry::getKey)
        .toList();
    markers.forEach(t -> cacheProcessInstanceMarker.remove(t));

  }

  @Override
  public List<String> searchUserTasks(String processInstanceId, String userTaskName, Integer maxResult)
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
      taskSearch.setPagination(new Pagination().setPageSize(100));

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
  public String executeUserTask(String activityId, String userId, Map<String, Object> variables)
      throws AutomatorException {
    try {
      taskClient.claim(activityId, serverDefinition.operateUserName);
      taskClient.completeTask(activityId, variables);
    } catch (TaskListException e) {
      throw new AutomatorException("Can't execute task [" + activityId + "]");
    }

    return null;
  }

  @Override
  public List<String> searchServiceTasks(String processInstanceId, String serviceTaskName, Integer maxResult)
      throws AutomatorException {
    try {
      // impossible to filtre by the task name/ task tyoe, so be ready to get a lot of flowNode and search the correct onee
      FlownodeInstanceFilter flownodeFilter = new FlownodeInstanceFilter.Builder().processInstanceKey(
          Long.valueOf(processInstanceId)).state(FlownodeInstanceState.ACTIVE).build();

      SearchQuery flownodeQuery = new SearchQuery.Builder().filter(flownodeFilter).size(100).build();
      List<FlownodeInstance> flownodes = operateClient.searchFlownodeInstances(flownodeQuery);
      List<String> taskList = flownodes.stream()
          .filter(t -> t.getType().equals("SERVICE_TASK"))
          .map(t -> t.getKey().toString())
          .toList();

      return taskList;

    } catch (OperateException e) {
      throw new AutomatorException("Can't search users task " + e.getMessage());
    }
  }

  @Override
  public String executeServiceTask(String activityId, String userId, Map<String, Object> variables)
      throws AutomatorException {

    throw new AutomatorException("Impossible to execute a Service task based on the acticityID in Camunda 8");
    /*try {
       zeebeClient.newCompleteCommand( -- we don't have the jobWorker --).variables(variables).send().join();
    }catch(Exception e) {
      throw new AutomatorException("Can't execute service task " + e.getMessage());

    }
    return null;

     */
  }

  @Override
  public String deployProcess(File processFile, ScenarioDeployment.Policy policy) throws AutomatorException {
    return null;
  }

  @Override
  public BpmnEngineConfiguration.CamundaEngine getServerDefinition() {
    return typeCamundaEngine;
  }

  Random random = new Random(System.currentTimeMillis());

  private String getUniqueMarker(String processId, String starterEventId) {
    return processId + "-" + random.nextInt(1000000);
  }
}
