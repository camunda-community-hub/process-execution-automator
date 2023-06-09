package org.camunda.automator.configuration;

import org.camunda.automator.engine.RunParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@PropertySource("classpath:application.yaml")
@Configuration
public class ConfigurationStartup {
  static Logger logger = LoggerFactory.getLogger(ConfigurationStartup.class);

  @Value("#{'${automator.startup.scenarioAtStartup}'.split(';')}")
  public List<String> scenarioAtStartup;

  @Value("#{'${automator.startup.filterService}'.split(';')}")
  public List<String> filterService;

  @Value("${automator.startup.scenarioPath}")
  public String scenarioPath;

  @Value("${automator.startup.logLevel:MONITORING}")
  public String logLevel;

  @Value("${automator.startup.deeptracking:false}")
  public boolean deepTracking;

  @Value("${automator.startup.policyExecution:DEPLOYPROCESS|WARMINGUP|CREATION|SERVICETASK|USERTASK}")
  public String policyExecution;

  /**
   * it may be necessary to wait the other component to warm up
   */
  @Value("${automator.startup.waitWarmUpServer:PT0S}")
  public String waitWarmupServer;

  public void setLogLevel(String logLevel) {
    this.logLevel = logLevel;
  }

  public RunParameters.LOGLEVEL getLogLevelEnum() {
    try {
      return RunParameters.LOGLEVEL.valueOf(logLevel);
    } catch (Exception e) {
      logger.error("Unknow LogLevel (automator.startup.loglevel) : [{}} ", logLevel);
      return RunParameters.LOGLEVEL.MONITORING;
    }
  }

  public boolean deepTracking() {
    return deepTracking;
  }

  public boolean isPolicyExecutionCreation() {
    String policyExtended = "|" + policyExecution + "|";
    return policyExtended.contains("|CREATION|");
  }

  public boolean isPolicyExecutionServiceTask() {
    String policyExtended = "|" + policyExecution + "|";
    return policyExtended.contains("|SERVICETASK|");
  }

  public boolean isPolicyExecutionUserTask() {
    String policyExtended = "|" + policyExecution + "|";
    return policyExtended.contains("|USERTASK|");
  }

  public boolean isPolicyExecutionWarmingUp() {
    String policyExtended = "|" + policyExecution + "|";
    return policyExtended.contains("|WARMINGUP|");
  }

  public boolean isPolicyDeployProcess() {
    String policyExtended = "|" + policyExecution + "|";
    return policyExtended.contains("|DEPLOYPROCESS|");
  }

  public List<String> getFilterService() {
    return filterService;
  }

  public Duration getWarmingUpServer() {
    try {
      return Duration.parse(waitWarmupServer);
    } catch (Exception e) {
      logger.error("Can't parse warmup [{}]", waitWarmupServer);
      return Duration.ZERO;
    }
  }
}
