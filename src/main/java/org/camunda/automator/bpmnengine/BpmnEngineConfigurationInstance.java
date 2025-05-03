package org.camunda.automator.bpmnengine;

import org.camunda.automator.configuration.BpmnEngineList;

/**
 * Generate BpmnEngineConfiguration for different servers
 */
public class BpmnEngineConfigurationInstance {

    public static BpmnEngineList getZeebeSaas(String name, String description, String zeebeGrpcGatewayAddress, Boolean zeebePlainText) {
        BpmnEngineList bpmEngineConfiguration = new BpmnEngineList();

        BpmnEngineList.BpmnServerDefinition serverDefinition = BpmnEngineList.BpmnServerDefinition.getInstance(name, description, BpmnEngineList.CamundaEngine.CAMUNDA_8);
        serverDefinition.zeebeGrpcAddress = zeebeGrpcGatewayAddress;
        serverDefinition.zeebePlainText = zeebePlainText;

        bpmEngineConfiguration.addExplicitServer(serverDefinition);
        return bpmEngineConfiguration;
    }

    public static BpmnEngineList getCamunda7(String name, String description, String serverUrl) {
        BpmnEngineList bpmEngineConfiguration = new BpmnEngineList();

        BpmnEngineList.BpmnServerDefinition serverDefinition = BpmnEngineList.BpmnServerDefinition.getInstance(name, description, BpmnEngineList.CamundaEngine.CAMUNDA_7);
        serverDefinition.camunda7ServerUrl = serverUrl;

        bpmEngineConfiguration.addExplicitServer(serverDefinition);

        return bpmEngineConfiguration;
    }

    public static BpmnEngineList getCamunda8(String name, String description, String zeebeGrpcAddress, String zeebeRestAddress) {
        BpmnEngineList bpmEngineConfiguration = new BpmnEngineList();

        BpmnEngineList.BpmnServerDefinition serverDefinition = BpmnEngineList.BpmnServerDefinition.getInstance(name, description, BpmnEngineList.CamundaEngine.CAMUNDA_8);
        serverDefinition.zeebeGrpcAddress = zeebeGrpcAddress;
        serverDefinition.zeebeGrpcAddress = zeebeGrpcAddress;
        serverDefinition.zeebeRestAddress = zeebeRestAddress;

        bpmEngineConfiguration.addExplicitServer(serverDefinition);

        return bpmEngineConfiguration;
    }

    public static BpmnEngineList getCamundaSaas8(String name,
                                                 String description,
                                                 String zeebeCloudRegister,
                                                 String zeebeCloudRegion,
                                                 String zeebeCloudClusterId,
                                                 String zeebeCloudClientId) {
        BpmnEngineList bpmEngineConfiguration = new BpmnEngineList();

        BpmnEngineList.BpmnServerDefinition serverDefinition = BpmnEngineList.BpmnServerDefinition.getInstance(name, description,BpmnEngineList.CamundaEngine.CAMUNDA_8_SAAS);
        serverDefinition.zeebeSaasRegion = zeebeCloudRegion;
        serverDefinition.zeebeSaasClusterId = zeebeCloudClusterId;
        serverDefinition.zeebeClientId = zeebeCloudClientId;

        bpmEngineConfiguration.addExplicitServer(serverDefinition);

        return bpmEngineConfiguration;
    }

    public static BpmnEngineList getDummy() {
        BpmnEngineList bpmEngineConfiguration = new BpmnEngineList();

        BpmnEngineList.BpmnServerDefinition serverDefinition = BpmnEngineList.BpmnServerDefinition.getInstance("Dummy", "Dummy instance",BpmnEngineList.CamundaEngine.DUMMY);

        bpmEngineConfiguration.addExplicitServer(serverDefinition);

        return bpmEngineConfiguration;
    }

}
