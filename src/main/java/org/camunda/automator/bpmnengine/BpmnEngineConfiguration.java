/**
 * Create an empty scenario.
 * The scenario can be created from scratch by the caller
 * It can be loaded from a JSON file too.
 *
 * @return
 */
package org.camunda.automator.bpmnengine;

public class BpmnEngineConfiguration {

  public String zeebeGatewayAddress;
  public String zeebeSecurityPlainText;
  public String zeebeCloudRegister;
  public String zeebeCloudRegion;
  public String zeebeCloudClusterId;
  public String zeebeCloudClientId;
  public String clientSecret;
  public String serverUrl;
  public CamundaEngine camundaEngine;

  public boolean logDebug = false;

  public static BpmnEngineConfiguration getZeebeSaas(String zeebeGatewayAddress, String zeebeSecurityPlainText) {
    BpmnEngineConfiguration bpmEngineConfiguration = new BpmnEngineConfiguration();
    bpmEngineConfiguration.camundaEngine = CamundaEngine.CAMUNDA_8;
    bpmEngineConfiguration.zeebeGatewayAddress = zeebeGatewayAddress;
    bpmEngineConfiguration.zeebeSecurityPlainText = zeebeSecurityPlainText;
    return bpmEngineConfiguration;
  }

  public static BpmnEngineConfiguration getCamunda7(String serverUrl) {
    BpmnEngineConfiguration bpmEngineConfiguration = new BpmnEngineConfiguration();
    bpmEngineConfiguration.camundaEngine = CamundaEngine.CAMUNDA_7;
    bpmEngineConfiguration.serverUrl = serverUrl;
    return bpmEngineConfiguration;
  }

  public static BpmnEngineConfiguration getDummy() {
    BpmnEngineConfiguration bpmEngineConfiguration = new BpmnEngineConfiguration();
    bpmEngineConfiguration.camundaEngine = CamundaEngine.DUMMY;
    return bpmEngineConfiguration;
  }

  public enum CamundaEngine {CAMUNDA_7, CAMUNDA_8, DUMMY}

}
