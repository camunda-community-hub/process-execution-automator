package org.camunda.automator.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:application.yaml")
@Configuration
public class ConfigurationServersEngine {

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
  public String clientSecret;

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

}

