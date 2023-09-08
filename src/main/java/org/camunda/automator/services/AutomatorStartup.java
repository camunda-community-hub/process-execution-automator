package org.camunda.automator.services;

import org.camunda.automator.AutomatorAPI;
import org.camunda.automator.AutomatorCLI;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.configuration.ConfigurationBpmEngine;
import org.camunda.automator.configuration.ConfigurationStartup;
import org.camunda.automator.definition.Scenario;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunParameters;
import org.camunda.automator.engine.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.time.Instant;
import java.util.List;

@Service
public class AutomatorStartup {
  static Logger logger = LoggerFactory.getLogger(AutomatorStartup.class);

  @Autowired
  ConfigurationStartup configurationStartup;

  @Autowired
  AutomatorAPI automatorAPI;

  @Autowired
  AutomatorCLI automatorCLI;

  @Autowired
  ConfigurationBpmEngine engineConfiguration;

  @Autowired
  ServiceAccess serviceAccess;

  @PostConstruct
  public void init() {
    if (AutomatorCLI.isRunningCLI)
      return;

    AutomatorSetupRunnable automatorSetupRunnable = new AutomatorSetupRunnable(configurationStartup, automatorAPI,
        automatorCLI, engineConfiguration);
    serviceAccess.getTaskScheduler("AutomatorSetup").schedule(automatorSetupRunnable, Instant.now());

  }

  /**
   * AutomatorSetupRunnable - run in parallel
   */
  class AutomatorSetupRunnable implements Runnable {

    ConfigurationStartup configurationStartup;

    AutomatorAPI automatorAPI;

    AutomatorCLI automatorCLI;

    ConfigurationBpmEngine engineConfiguration;

    public AutomatorSetupRunnable(ConfigurationStartup configurationStartup,
                                  AutomatorAPI automatorAPI,
                                  AutomatorCLI automatorCLI,
                                  ConfigurationBpmEngine engineConfiguration) {
      this.configurationStartup = configurationStartup;
      this.automatorAPI = automatorAPI;
      this.automatorCLI = automatorCLI;
      this.engineConfiguration = engineConfiguration;
    }

    @Override
    public void run() {

      RunParameters runParameters = new RunParameters();
      runParameters.setExecution(true)
          .setLogLevel(configurationStartup.getLogLevelEnum())
          .setCreation(configurationStartup.isPolicyExecutionCreation())
          .setServicetask(configurationStartup.isPolicyExecutionServiceTask())
          .setUsertask(configurationStartup.isPolicyExecutionUserTask())
          .setWarmingUp(configurationStartup.isPolicyExecutionWarmingUp())
          .setDeploymentProcess(configurationStartup.isPolicyDeployProcess())
          .setDeepTracking(configurationStartup.deepTracking());
      List<String> filterService = configurationStartup.getFilterService();
      if (filterService != null) {
        runParameters.setFilterExecutionServiceTask(filterService);
      }

      logger.info(
          "AutomatorStartup parameters warmingUp[{}] creation:[{}] serviceTask:[{}] userTask:[{}] ScenarioPath[{}] logLevel[{}] waitWarmingUpServer[{} s]",
          runParameters.isWarmingUp(), runParameters.isCreation(), runParameters.isServicetask(),
          runParameters.isUsertask(), configurationStartup.scenarioPath, configurationStartup.logLevel,
          configurationStartup.getWarmingUpServer().toMillis() / 1000);

      try {
        String currentPath = new java.io.File(".").getCanonicalPath();
        logger.info("Local Path[{}]", currentPath);
      } catch (Exception e) {
        logger.error("Can't access Local Path : {} ", e.getMessage());
      }
      if (configurationStartup.getWarmingUpServer().getSeconds() > 30)
        logger.info("Warmup: wait.... {} s", configurationStartup.getWarmingUpServer().getSeconds());

      try {
        Thread.sleep(configurationStartup.getWarmingUpServer().toMillis());
      } catch (Exception e) {
        // do nothing
      }
      if (configurationStartup.getWarmingUpServer().getSeconds() > 30)
        logger.info("Warmup: start now");

      for (String scenarioFileName : configurationStartup.getScenarioAtStartup()) {
        File scenarioFile = new File(configurationStartup.scenarioPath + "/" + scenarioFileName);
        if (!scenarioFile.exists()) {
          logger.error("Can't find [{}/{}]", configurationStartup.scenarioPath, scenarioFileName);
          continue;
        }

        try {
          Scenario scenario = automatorAPI.loadFromFile(scenarioFile);
          logger.info("Start scenario [{}]", scenario.getName());

          // BpmnEngine: find the correct one referenced in the scenario
          int countEngineIsNotReady = 0;
          BpmnEngine bpmnEngine = null;
          boolean pleaseTryAgain;
          do {
            pleaseTryAgain = false;
            countEngineIsNotReady++;
            String message = "";
            try {
              if (runParameters.isLevelMonitoring()) {
                logger.info("Connect to Bpmn Engine Type{}", scenario.getServerType());
              }
              bpmnEngine = automatorAPI.getBpmnEngineFromScenario(scenario, engineConfiguration);
              if (!bpmnEngine.isReady()) {
                bpmnEngine.connection();
              }
            } catch (AutomatorException e) {
              pleaseTryAgain = true;
              message = e.getMessage();
            }
            if (pleaseTryAgain && countEngineIsNotReady < 10) {
              logger.info(
                  "Scenario [{}] file[{}] No BPM ENGINE running [{}] tentative:{}/10. Sleep 30s. Scenario reference serverName[{}] serverType[{}]",
                  message, countEngineIsNotReady, scenario.getName(), scenarioFile.getName(), scenario.getServerName(),
                  scenario.getServerType());
              try {
                Thread.sleep(((long) 1000) * 30);
              } catch (InterruptedException e) {
                // nothing to do
              }
            }
          } while (pleaseTryAgain && countEngineIsNotReady < 10);

          if (bpmnEngine == null) {
            logger.error(
                "Scenario [{}] file[{}] No BPM ENGINE running. Scenario reference serverName[{}] serverType[{}]",
                scenario.getName(), scenarioFile.getName(), scenario.getServerName(), scenario.getServerType());
            continue;
          }

          bpmnEngine.turnHighFlowMode(true);
          logger.info("Scenario [{}] file[{}] use BpmnEngine {}", scenario.getName(), scenarioFile.getName(),
              bpmnEngine.getSignature());
          RunResult scenarioExecutionResult = automatorAPI.executeScenario(bpmnEngine, runParameters, scenario);
          logger.info("AutomatorStartup: end scenario [{}] in {} ms", scenario.getName(),
              scenarioExecutionResult.getTimeExecution());
          bpmnEngine.turnHighFlowMode(false);

        } catch (AutomatorException e) {
          logger.error("Error during execution [{}]: {}", scenarioFileName, e.getMessage());
        }
      }
    }
  }

}
