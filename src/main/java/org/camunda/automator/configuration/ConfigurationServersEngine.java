package org.camunda.automator.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties("automator")
public class ConfigurationServersEngine {

    // @ V alue("${automator.logDebug:false}")
    public boolean logDebug = false;

    @Value("#{'${automator.serversConnection}'.split(';')}")
    public List<String> serversConnection;

    public List<Map<String, Object>> serversList;

    @Value("${automator.servers.camunda7.url:''}")
    public String camunda7Url;
    @Value("${automator.servers.camunda7.username:}")
    public String camunda7UserName;
    @Value("${automator.servers.camunda7.password:}")
    public String camunda7Password;
    @Value("${automator.servers.camunda7.name:''}")
    public String camunda7Name;
    @Value("${automator.servers.camunda7.workerMaxJobsActive:''}")
    public String C7WorkerMaxJobsActive;

    @Value("${automator.servers.camunda8.name:''}")
    public String zeebeName;
    @Value("${automator.servers.camunda8.zeebeGatewayAddress:''}")
    public String zeebeGatewayAddress;
    @Value("${automator.servers.camunda8.operateUrl:''}")
    public String zeebeOperateUrl;
    @Value("${automator.servers.camunda8.operateUserName:''}")
    public String zeebeOperateUserName;
    @Value("${automator.servers.camunda8.operateUserPassword:''}")
    public String zeebeOperateUserPassword;

    @Value("${automator.servers.camunda8.taskListUrl:''}")
    public String zeebeTaskListUrl;
    @Value("${automator.servers.camunda8.taskListUserName:''}")
    public String zeebeTaskListUserName;
    @Value("${automator.servers.camunda8.taskListUserPassword:''}")
    public String zeebeTaskListUserPassword;

    @Value("${automator.servers.camunda8.workerExecutionThreads:''}")
    public String zeebeWorkerExecutionThreads;
    @Value("${automator.servers.camunda8.workerMaxJobsActive:''}")
    public String zeebeWorkerMaxJobsActive;

    @Value("${automator.servers.camunda8Saas.region:''}")
    public String zeebeSaasRegion;
    @Value("${automator.servers.camunda8Saas.clusterId:''}")
    public String zeebeSaasClusterId;
    @Value("${automator.servers.camunda8Saas.clientId:''}")
    public String zeebeSaasClientId;

    @Value("${automator.servers.camunda8Saas.secret:''}")
    public String zeebeSaasClientSecret;

    @Value("${automator.servers.camunda8Saas.oAuthUrl:''}")
    public String zeebeSaasOAuthUrl;
    @Value("${automator.servers.camunda8Saas.audience:''}")
    public String zeebeSaasAudience;

    @Value("${automator.servers.camunda8Saas.operateUrl:''}")
    public String zeebeSaasOperateUrl;
    @Value("${automator.servers.camunda8Saas.operateUserName:''}")
    public String zeebeSaasOperateUserName;
    @Value("${automator.servers.camunda8Saas.operateUserPassword:''}")
    public String zeebeSaasOperateUserPassword;
    @Value("${automator.servers.camunda8Saas.taskListUrl:''}")
    public String zeebeSaasTaskListUrl;
    @Value("${automator.servers.camunda8Saas.workerExecutionThreads:''}")
    public String zeebeSaasWorkerExecutionThreads;
    @Value("${automator.servers.camunda8Saas.workerMaxJobsActive:''}")
    public String zeebeSaasWorkerMaxJobsActive;

    public List<Map<String, Object>> getServersList() {
        return serversList;
    }

    // this method is mandatory for Spring to get the value and to force it as a List<Map<>>
    public void setServersList(List<Map<String, Object>> serversList) {
        this.serversList = serversList;
    }

}

