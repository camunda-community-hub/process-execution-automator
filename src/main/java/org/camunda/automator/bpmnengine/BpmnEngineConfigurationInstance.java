package org.camunda.automator.bpmnengine;

import org.camunda.automator.configuration.ConfigurationBpmEngine;

/**
 * Generate BpmnEngineConfiguration for different servers
 */
public class BpmnEngineConfigurationInstance {

  public static ConfigurationBpmEngine getZeebeSaas(String zeebeGatewayAddress, String zeebeSecurityPlainText) {
    ConfigurationBpmEngine bpmEngineConfiguration = new ConfigurationBpmEngine();

    ConfigurationBpmEngine.BpmnServerDefinition serverDefinition = new ConfigurationBpmEngine.BpmnServerDefinition();
    serverDefinition.serverType = ConfigurationBpmEngine.CamundaEngine.CAMUNDA_8;
    serverDefinition.zeebeGatewayAddress = zeebeGatewayAddress;
    serverDefinition.zeebeSecurityPlainText = zeebeSecurityPlainText;

    bpmEngineConfiguration.addExplicitServer(serverDefinition);
    return bpmEngineConfiguration;
  }

  public static ConfigurationBpmEngine getCamunda7(String serverUrl) {
    ConfigurationBpmEngine bpmEngineConfiguration = new ConfigurationBpmEngine();

    ConfigurationBpmEngine.BpmnServerDefinition serverDefinition = new ConfigurationBpmEngine.BpmnServerDefinition();
    serverDefinition.serverType = ConfigurationBpmEngine.CamundaEngine.CAMUNDA_7;
    serverDefinition.serverUrl = serverUrl;

    bpmEngineConfiguration.addExplicitServer(serverDefinition);

    return bpmEngineConfiguration;
  }

  public static ConfigurationBpmEngine getCamunda8(String zeebeGatewayAddress) {
    ConfigurationBpmEngine bpmEngineConfiguration = new ConfigurationBpmEngine();

    ConfigurationBpmEngine.BpmnServerDefinition serverDefinition = new ConfigurationBpmEngine.BpmnServerDefinition();
    serverDefinition.serverType = ConfigurationBpmEngine.CamundaEngine.CAMUNDA_8;
    serverDefinition.zeebeGatewayAddress = zeebeGatewayAddress;

    bpmEngineConfiguration.addExplicitServer(serverDefinition);

    return bpmEngineConfiguration;
  }

  public static ConfigurationBpmEngine getCamundaSaas8(String zeebeCloudRegister,
                                                       String zeebeCloudRegion,
                                                       String zeebeCloudClusterId,
                                                       String zeebeCloudClientId) {
    ConfigurationBpmEngine bpmEngineConfiguration = new ConfigurationBpmEngine();

    ConfigurationBpmEngine.BpmnServerDefinition serverDefinition = new ConfigurationBpmEngine.BpmnServerDefinition();
    serverDefinition.serverType = ConfigurationBpmEngine.CamundaEngine.CAMUNDA_8;
    serverDefinition.zeebeCloudRegister = zeebeCloudRegister;
    serverDefinition.zeebeCloudRegion = zeebeCloudRegion;
    serverDefinition.zeebeCloudClusterId = zeebeCloudClusterId;
    serverDefinition.zeebeCloudClientId = zeebeCloudClientId;

    bpmEngineConfiguration.addExplicitServer(serverDefinition);

    return bpmEngineConfiguration;
  }

  public static ConfigurationBpmEngine getDummy() {
    ConfigurationBpmEngine bpmEngineConfiguration = new ConfigurationBpmEngine();

    ConfigurationBpmEngine.BpmnServerDefinition serverDefinition = new ConfigurationBpmEngine.BpmnServerDefinition();
    serverDefinition.serverType = ConfigurationBpmEngine.CamundaEngine.DUMMY;

    bpmEngineConfiguration.addExplicitServer(serverDefinition);

    return bpmEngineConfiguration;
  }

}
