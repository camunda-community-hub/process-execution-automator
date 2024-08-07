package org.camunda.automator.bpmnengine;

import org.camunda.automator.configuration.BpmnEngineList;

/**
 * Generate BpmnEngineConfiguration for different servers
 */
public class BpmnEngineConfigurationInstance {

  public static BpmnEngineList getZeebeSaas(String zeebeGatewayAddress, String zeebeSecurityPlainText) {
    BpmnEngineList bpmEngineConfiguration = new BpmnEngineList();

    BpmnEngineList.BpmnServerDefinition serverDefinition = new BpmnEngineList.BpmnServerDefinition();
    serverDefinition.serverType = BpmnEngineList.CamundaEngine.CAMUNDA_8;
    serverDefinition.zeebeGatewayAddress = zeebeGatewayAddress;
    serverDefinition.zeebeSecurityPlainText = zeebeSecurityPlainText;

    bpmEngineConfiguration.addExplicitServer(serverDefinition);
    return bpmEngineConfiguration;
  }

  public static BpmnEngineList getCamunda7(String serverUrl) {
    BpmnEngineList bpmEngineConfiguration = new BpmnEngineList();

    BpmnEngineList.BpmnServerDefinition serverDefinition = new BpmnEngineList.BpmnServerDefinition();
    serverDefinition.serverType = BpmnEngineList.CamundaEngine.CAMUNDA_7;
    serverDefinition.camunda7ServerUrl = serverUrl;

    bpmEngineConfiguration.addExplicitServer(serverDefinition);

    return bpmEngineConfiguration;
  }

  public static BpmnEngineList getCamunda8(String zeebeGatewayAddress) {
    BpmnEngineList bpmEngineConfiguration = new BpmnEngineList();

    BpmnEngineList.BpmnServerDefinition serverDefinition = new BpmnEngineList.BpmnServerDefinition();
    serverDefinition.serverType = BpmnEngineList.CamundaEngine.CAMUNDA_8;
    serverDefinition.zeebeGatewayAddress = zeebeGatewayAddress;

    bpmEngineConfiguration.addExplicitServer(serverDefinition);

    return bpmEngineConfiguration;
  }

  public static BpmnEngineList getCamundaSaas8(String zeebeCloudRegister,
                                               String zeebeCloudRegion,
                                               String zeebeCloudClusterId,
                                               String zeebeCloudClientId) {
    BpmnEngineList bpmEngineConfiguration = new BpmnEngineList();

    BpmnEngineList.BpmnServerDefinition serverDefinition = new BpmnEngineList.BpmnServerDefinition();
    serverDefinition.serverType = BpmnEngineList.CamundaEngine.CAMUNDA_8;
    serverDefinition.zeebeSaasRegion = zeebeCloudRegion;
    serverDefinition.zeebeSaasClusterId = zeebeCloudClusterId;
    serverDefinition.zeebeClientId = zeebeCloudClientId;

    bpmEngineConfiguration.addExplicitServer(serverDefinition);

    return bpmEngineConfiguration;
  }

  public static BpmnEngineList getDummy() {
    BpmnEngineList bpmEngineConfiguration = new BpmnEngineList();

    BpmnEngineList.BpmnServerDefinition serverDefinition = new BpmnEngineList.BpmnServerDefinition();
    serverDefinition.serverType = BpmnEngineList.CamundaEngine.DUMMY;

    bpmEngineConfiguration.addExplicitServer(serverDefinition);

    return bpmEngineConfiguration;
  }

}
