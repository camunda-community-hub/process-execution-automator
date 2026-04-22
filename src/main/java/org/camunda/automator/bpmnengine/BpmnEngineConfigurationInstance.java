package org.camunda.automator.bpmnengine;

import org.camunda.automator.configuration.ConfigurationBpmnEngineList;

/**
 * Generate BpmnEngineConfiguration for different servers
 */
public class BpmnEngineConfigurationInstance {

    public static ConfigurationBpmnEngineList getZeebeSaas(String name, String description, String zeebeGrpcGatewayAddress, Boolean zeebePlainText) {
        ConfigurationBpmnEngineList bpmEngineConfiguration = new ConfigurationBpmnEngineList();

        ConfigurationBpmnEngineList.BpmnServerDefinition serverDefinition = ConfigurationBpmnEngineList.BpmnServerDefinition.getInstance(name, description, ConfigurationBpmnEngineList.CamundaEngine.CAMUNDA_8);
        serverDefinition.zeebeGrpcAddress = zeebeGrpcGatewayAddress;
        serverDefinition.zeebePlainText = zeebePlainText;

        bpmEngineConfiguration.addExplicitServer(serverDefinition);
        return bpmEngineConfiguration;
    }

    public static ConfigurationBpmnEngineList getCamunda7(String name, String description, String serverUrl) {
        ConfigurationBpmnEngineList bpmEngineConfiguration = new ConfigurationBpmnEngineList();

        ConfigurationBpmnEngineList.BpmnServerDefinition serverDefinition = ConfigurationBpmnEngineList.BpmnServerDefinition.getInstance(name, description, ConfigurationBpmnEngineList.CamundaEngine.CAMUNDA_7);
        serverDefinition.camunda7ServerUrl = serverUrl;

        bpmEngineConfiguration.addExplicitServer(serverDefinition);

        return bpmEngineConfiguration;
    }

    public static ConfigurationBpmnEngineList getCamunda8(String name, String description, String zeebeGrpcAddress, String zeebeRestAddress) {
        ConfigurationBpmnEngineList bpmEngineConfiguration = new ConfigurationBpmnEngineList();

        ConfigurationBpmnEngineList.BpmnServerDefinition serverDefinition = ConfigurationBpmnEngineList.BpmnServerDefinition.getInstance(name, description, ConfigurationBpmnEngineList.CamundaEngine.CAMUNDA_8);
        serverDefinition.zeebeGrpcAddress = zeebeGrpcAddress;
        serverDefinition.zeebeGrpcAddress = zeebeGrpcAddress;
        serverDefinition.zeebeRestAddress = zeebeRestAddress;

        bpmEngineConfiguration.addExplicitServer(serverDefinition);

        return bpmEngineConfiguration;
    }

    public static ConfigurationBpmnEngineList getCamundaSaas8(String name,
                                                              String description,
                                                              String zeebeCloudRegister,
                                                              String zeebeCloudRegion,
                                                              String zeebeCloudClusterId,
                                                              String zeebeCloudClientId) {
        ConfigurationBpmnEngineList bpmEngineConfiguration = new ConfigurationBpmnEngineList();

        ConfigurationBpmnEngineList.BpmnServerDefinition serverDefinition = ConfigurationBpmnEngineList.BpmnServerDefinition.getInstance(name, description, ConfigurationBpmnEngineList.CamundaEngine.CAMUNDA_8_SAAS);
        serverDefinition.zeebeSaasRegion = zeebeCloudRegion;
        serverDefinition.zeebeSaasClusterId = zeebeCloudClusterId;
        serverDefinition.zeebeClientId = zeebeCloudClientId;

        bpmEngineConfiguration.addExplicitServer(serverDefinition);

        return bpmEngineConfiguration;
    }

    public static ConfigurationBpmnEngineList getDummy() {
        ConfigurationBpmnEngineList bpmEngineConfiguration = new ConfigurationBpmnEngineList();

        ConfigurationBpmnEngineList.BpmnServerDefinition serverDefinition = ConfigurationBpmnEngineList.BpmnServerDefinition.getInstance("Dummy", "Dummy instance", ConfigurationBpmnEngineList.CamundaEngine.DUMMY);

        bpmEngineConfiguration.addExplicitServer(serverDefinition);

        return bpmEngineConfiguration;
    }

}
