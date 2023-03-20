package org.camunda.automator.bpmnengine.camunda8;

import io.camunda.operate.CamundaOperateClient;
import io.camunda.operate.auth.AuthInterface;
import io.camunda.operate.auth.SaasAuthentication;
import io.camunda.operate.auth.SimpleAuthentication;
import io.camunda.operate.dto.FlownodeInstance;
import io.camunda.operate.dto.FlownodeInstanceState;
import io.camunda.operate.exception.OperateException;
import io.camunda.operate.search.FlownodeInstanceFilter;
import io.camunda.operate.search.SearchQuery;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.ZeebeClientBuilder;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.bpmnengine.BpmnEngineConfiguration;
import org.camunda.automator.definition.ScenarioDeployment;
import org.camunda.automator.engine.AutomatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BpmnEngineCamunda8 implements BpmnEngine {

  private final Logger logger = LoggerFactory.getLogger(BpmnEngineCamunda8.class);

  private final BpmnEngineConfiguration engineConfiguration;
  private final BpmnEngineConfiguration.BpmnServerDefinition serverDefinition;


  private ZeebeClient zeebeClient;
  private CamundaOperateClient operateClient;

  private BpmnEngineConfiguration.CamundaEngine typeCamundaEngine;


  public BpmnEngineCamunda8(BpmnEngineConfiguration engineConfiguration, BpmnEngineConfiguration.BpmnServerDefinition serverDefinition) {
    this.engineConfiguration = engineConfiguration;
    this.serverDefinition = serverDefinition;
  }

  @Override
  public void init() throws AutomatorException {
    final String defaultAddress = "localhost:26500";
    final String envVarAddress = System.getenv("ZEEBE_ADDRESS");

    final ZeebeClientBuilder clientBuilder;
    AuthInterface sa;
    if (serverDefinition.zeebeCloudRegister != null) {
      /* Connect to Camunda Cloud Cluster, assumes that credentials are set in environment variables.
       * See JavaDoc on class level for details
       */
      clientBuilder = ZeebeClient.newClientBuilder();
      sa = new SaasAuthentication(serverDefinition.zeebeCloudClientId, serverDefinition.clientSecret);
      typeCamundaEngine= BpmnEngineConfiguration.CamundaEngine.CAMUNDA_8_SAAS;
    } else if (serverDefinition.zeebeGatewayAddress != null) {
      // connect to local deployment; assumes that authentication is disabled
      clientBuilder = ZeebeClient.newClientBuilder()
          .gatewayAddress(serverDefinition.zeebeGatewayAddress)
          .usePlaintext();
      sa = new SimpleAuthentication(serverDefinition.operateUserName, serverDefinition.operateUserPassword,
          serverDefinition.operateUrl);
      typeCamundaEngine= BpmnEngineConfiguration.CamundaEngine.CAMUNDA_8;
    } else
      throw new AutomatorException("Invalid configuration");

    try {
      zeebeClient = clientBuilder.build();
      operateClient = new CamundaOperateClient.Builder().operateUrl(serverDefinition.operateUrl)
          .authentication(sa)
          .build();
    } catch (Exception e) {
      throw new AutomatorException("Can't connect to Zeebe " + e.getMessage());
    }

  }

  @Override
  public String createProcessInstance(String processId, String starterEventId, Map<String, Object> variables)
      throws AutomatorException {
    zeebeClient.newCreateInstanceCommand().bpmnProcessId(processId).latestVersion().variables(variables).send().join();
    return null;
  }

  @Override
  public List<String> searchUserTasks(String processInstanceId, String userTaskName, Integer maxResult)
      throws AutomatorException {
    try {
      // impossible to filtre by the task name/ task tyoe, so be ready to get a lot of flowNode and search the correct onee
      FlownodeInstanceFilter flownodeFilter = new FlownodeInstanceFilter.Builder().processInstanceKey(
          Long.valueOf(processInstanceId)).state(FlownodeInstanceState.ACTIVE).build();

      SearchQuery flownodeQuery = new SearchQuery.Builder().filter(flownodeFilter).size(100000).build();
      List<FlownodeInstance> flownodes = operateClient.searchFlownodeInstances(flownodeQuery);
      return flownodes.stream()
          .filter(t -> t.getType().equals("USERTASK"))
          .map(t -> t.getFlowNodeId())
          .collect(Collectors.toList());

    } catch (OperateException e) {
      throw new AutomatorException("Can't search users task " + e.getMessage());
    }
  }

  @Override
  public String executeUserTask(String activityId, String userId, Map<String, Object> variables)
      throws AutomatorException {
     return null;
  }

  @Override
  public List<String> searchServiceTasks(String processInstanceId, String userTaskName, Integer maxResult)
      throws AutomatorException {
    try {
      // impossible to filtre by the task name/ task tyoe, so be ready to get a lot of flowNode and search the correct onee
      FlownodeInstanceFilter flownodeFilter = new FlownodeInstanceFilter.Builder().processInstanceKey(
          Long.valueOf(processInstanceId)).state(FlownodeInstanceState.ACTIVE).build();

      SearchQuery flownodeQuery = new SearchQuery.Builder().filter(flownodeFilter).size(100000).build();
      List<FlownodeInstance> flownodes = operateClient.searchFlownodeInstances(flownodeQuery);
      return flownodes.stream()
          .filter(t -> t.getType().equals("USERTASK"))
          .map(t -> t.getFlowNodeId())
          .collect(Collectors.toList());

    } catch (OperateException e) {
      throw new AutomatorException("Can't search users task " + e.getMessage());
    }  }

  @Override
  public String executeServiceTask(String activityId, String userId, Map<String, Object> variables)
      throws AutomatorException {
    zeebeClient.newCompleteCommand(Long.valueOf(activityId)).variables(variables).send();
    return null;
  }

  @Override
  public String deployProcess(File processFile, ScenarioDeployment.Policy policy) throws AutomatorException {
    return null;
  }

  @Override
  public BpmnEngineConfiguration.CamundaEngine getServerDefinition() {
    return typeCamundaEngine;
  }
}
