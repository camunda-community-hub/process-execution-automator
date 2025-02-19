package org.camunda.automator.bpmnengine;

import org.camunda.automator.configuration.BpmnEngineList;

/**
 * Generate BpmnEngineConfiguration for different servers
 */
public class BpmnEngineConfigurationInstance {

    public static BpmnEngineList getZeebeSaas(String zeebeGatewayAddress, Boolean zeebePlainText) {
        BpmnEngineList bpmEngineConfiguration = new BpmnEngineList();

        BpmnEngineList.BpmnServerDefinition serverDefinition = new BpmnEngineList.BpmnServerDefinition();
        serverDefinition.serverType = BpmnEngineList.CamundaEngine.CAMUNDA_8;
        serverDefinition.zeebeGatewayAddress = zeebeGatewayAddress;
        serverDefinition.zeebePlainText = zeebePlainText;

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

    public static BpmnEngineList getCamunda8(String zeebeGatewayAddress, String zeebeGrpcAddress, String zeebeRestAddress) {
        BpmnEngineList bpmEngineConfiguration = new BpmnEngineList();

        BpmnEngineList.BpmnServerDefinition serverDefinition = new BpmnEngineList.BpmnServerDefinition();
        serverDefinition.serverType = BpmnEngineList.CamundaEngine.CAMUNDA_8;
        serverDefinition.zeebeGatewayAddress = zeebeGatewayAddress;
        serverDefinition.zeebeGrpcAddress = zeebeGrpcAddress;
        serverDefinition.zeebeRestAddress = zeebeRestAddress;

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
