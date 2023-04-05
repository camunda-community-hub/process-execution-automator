package org.camunda.automator.bpmnengine;

import java.util.ArrayList;

/**
 * Generate BpmnEngineConfiguration for different servers
 */
public class BpmnEngineConfigurationInstance {

  public static BpmnEngineConfiguration getZeebeSaas(String zeebeGatewayAddress, String zeebeSecurityPlainText) {
    BpmnEngineConfiguration bpmEngineConfiguration = new BpmnEngineConfiguration();

    BpmnEngineConfiguration.BpmnServerDefinition serverDefinition = new BpmnEngineConfiguration.BpmnServerDefinition();
    serverDefinition.camundaEngine = BpmnEngineConfiguration.CamundaEngine.CAMUNDA_8;
    serverDefinition.zeebeGatewayAddress = zeebeGatewayAddress;
    serverDefinition.zeebeSecurityPlainText = zeebeSecurityPlainText;

    bpmEngineConfiguration.servers = new ArrayList<>();
    bpmEngineConfiguration.servers.add( serverDefinition);
    return bpmEngineConfiguration;
  }

  public static BpmnEngineConfiguration getCamunda7(String serverUrl) {
    BpmnEngineConfiguration bpmEngineConfiguration = new BpmnEngineConfiguration();

    BpmnEngineConfiguration.BpmnServerDefinition serverDefinition = new BpmnEngineConfiguration.BpmnServerDefinition();
    serverDefinition.camundaEngine = BpmnEngineConfiguration.CamundaEngine.CAMUNDA_7;
    serverDefinition.serverUrl = serverUrl;

    bpmEngineConfiguration.servers = new ArrayList<>();
    bpmEngineConfiguration.servers.add( serverDefinition);

    return bpmEngineConfiguration;
  }

  public static BpmnEngineConfiguration getCamunda8(String zeebeGatewayAddress) {
    BpmnEngineConfiguration bpmEngineConfiguration = new BpmnEngineConfiguration();

    BpmnEngineConfiguration.BpmnServerDefinition serverDefinition = new BpmnEngineConfiguration.BpmnServerDefinition();
    serverDefinition.camundaEngine = BpmnEngineConfiguration.CamundaEngine.CAMUNDA_8;
    serverDefinition.zeebeGatewayAddress = zeebeGatewayAddress;

    bpmEngineConfiguration.servers = new ArrayList<>();
    bpmEngineConfiguration.servers.add( serverDefinition);

    return bpmEngineConfiguration;
  }

  public static BpmnEngineConfiguration getCamundaSaas8(String zeebeCloudRegister,
                                                        String zeebeCloudRegion,
                                                        String zeebeCloudClusterId,
                                                        String zeebeCloudClientId) {
    BpmnEngineConfiguration bpmEngineConfiguration = new BpmnEngineConfiguration();

    BpmnEngineConfiguration.BpmnServerDefinition serverDefinition = new BpmnEngineConfiguration.BpmnServerDefinition();
    serverDefinition.camundaEngine = BpmnEngineConfiguration.CamundaEngine.CAMUNDA_8;
    serverDefinition.zeebeCloudRegister = zeebeCloudRegister;
    serverDefinition.zeebeCloudRegion = zeebeCloudRegion;
    serverDefinition.zeebeCloudClusterId = zeebeCloudClusterId;
    serverDefinition.zeebeCloudClientId = zeebeCloudClientId;

    bpmEngineConfiguration.servers = new ArrayList<>();
    bpmEngineConfiguration.servers.add( serverDefinition);


    return bpmEngineConfiguration;
  }

  public static BpmnEngineConfiguration getDummy() {
    BpmnEngineConfiguration bpmEngineConfiguration = new BpmnEngineConfiguration();

    BpmnEngineConfiguration.BpmnServerDefinition serverDefinition = new BpmnEngineConfiguration.BpmnServerDefinition();
    serverDefinition.camundaEngine = BpmnEngineConfiguration.CamundaEngine.DUMMY;

    bpmEngineConfiguration.servers = new ArrayList<>();
    bpmEngineConfiguration.servers.add( serverDefinition);

    return bpmEngineConfiguration;
  }

}
