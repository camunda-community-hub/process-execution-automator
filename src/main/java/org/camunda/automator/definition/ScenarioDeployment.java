package org.camunda.automator.definition;

import org.camunda.automator.bpmnengine.BpmnEngineConfiguration;

public class ScenarioDeployment {
  /**
   * type of server
   */
  public BpmnEngineConfiguration.CamundaEngine server;
  /**
   * Type pf deployment
   */
  public String typeDeployment;
  /**
   * Name of the file
   */
  public String processFile;

  public Policy policy;

  public enum TypeDeployment {PROCESS}
  public enum Policy {ONLYNOTEXIST, ALWAYS}

}
