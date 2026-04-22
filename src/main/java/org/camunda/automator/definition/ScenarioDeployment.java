package org.camunda.automator.definition;

import org.camunda.automator.configuration.ConfigurationBpmnEngineList;

public class ScenarioDeployment {
    /**
     * type of server
     */
    public ConfigurationBpmnEngineList.CamundaEngine serverType;
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
