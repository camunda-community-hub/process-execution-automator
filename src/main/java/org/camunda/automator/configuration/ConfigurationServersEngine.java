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
  @Value("${automator.servers.camunda8.zeebeCloudRegister:''}")
  public String zeebeCloudRegister;
  @Value("${automator.servers.camunda8.zeebeCloudRegion:''}")
  public String zeebeCloudRegion;
  @Value("${automator.servers.camunda8.zeebeCloudClusterId:''}")
  public String zeebeCloudClusterId;
  @Value("${automator.servers.camunda8.zeebeCloudClientId:''}")
  public String zeebeCloudClientId;
  @Value("${automator.servers.camunda8.clientSecret:''}")
  public String zeebeClientSecret;
  @Value("${automator.servers.camunda8.operateUrl:''}")
  public String operateUrl;
  @Value("${automator.servers.camunda8.operateUserName:''}")
  public String operateUserName;
  @Value("${automator.servers.camunda8.operateUserPassword:''}")
  public String operateUserPassword;
  @Value("${automator.servers.camunda8.taskListUrl:''}")
  public String taskListUrl;
  @Value("${automator.servers.camunda8.workerExecutionThreads:''}")
  public String workerExecutionThreads;
  @Value("${automator.servers.camunda8.workerMaxJobsActive:''}")
  public String C8WorkerMaxJobsActive;

  public List<Map<String, Object>> getServersList() {
    return serversList;
  }

  // this method is mandatory for Spring to get the value and to force it as a List<Map<>>
  public void setServersList(List<Map<String, Object>> serversList) {
    this.serversList = serversList;
  }

}

