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

  @PostConstruct
  public void init() {
    if (AutomatorCLI.isRunningCLI)
      return;
    RunParameters runParameters = new RunParameters();
    runParameters.execution = true;
    runParameters.logLevel = configurationStartup.getLogLevelEnum();
    runParameters.creation = configurationStartup.isPolicyExecutionCreation();
    runParameters.servicetask = configurationStartup.isPolicyExecutionServiceTask();
    runParameters.usertask = configurationStartup.isPolicyExecutionUserTask();

    List<String> filterService = configurationStartup.getFilterService();
    if (filterService != null) {
      runParameters.setFilterExecutionServiceTask(filterService);
    }

    logger.info(
        "AutomatorStartup parameters creation:[{}] serviceTask:[{}] userTask:[{}] ScenarioPath[{}] logLevel[{}] waitWarmup[{} s]",
        runParameters.creation, runParameters.servicetask, runParameters.usertask, configurationStartup.scenarioPath,
        configurationStartup.logLevel, configurationStartup.getWarmup().toMillis() / 1000);

    try {
      String currentPath = new java.io.File(".").getCanonicalPath();
      logger.info("Local Path[{}]", currentPath);
    } catch (Exception e) {
      logger.error("Can't access Local Path : {} ", e.getMessage());
    }
    if (configurationStartup.getWarmup().getSeconds() > 30)
      logger.info("Warmup: wait.... {} s", configurationStartup.getWarmup().getSeconds());

    try {
      Thread.sleep(configurationStartup.getWarmup().toMillis());
    } catch (Exception e) {
    }
    if (configurationStartup.getWarmup().getSeconds() > 30)
      logger.info("Warmup: start now");

    for (String scenarioFileName : configurationStartup.scenarioAtStartup) {
      File scenarioFile = new File(configurationStartup.scenarioPath + "/" + scenarioFileName);
      if (!scenarioFile.exists()) {
        logger.error("Can't find [" + configurationStartup.scenarioPath + "/" + scenarioFileName + "]");
        continue;
      }
      Scenario scenario = null;

      try {
        scenario = automatorAPI.loadFromFile(scenarioFile);
        logger.info("Start scenario [{}]", scenario.getName());

        // BpmnEngine: find the correct one referenced in the scenario
        int countEngineIsNotReady = 0;
        BpmnEngine bpmnEngine = null;
        boolean pleaseTryAgain = false;
        do {
          countEngineIsNotReady++;

          try {
            if (runParameters.isLevelMonitoring()) {
              logger.info("Connect to Bpmn Engine");
            }
            bpmnEngine = automatorAPI.getBpmnEngineFromScenario(scenario, engineConfiguration);
          } catch (AutomatorException e) {
            pleaseTryAgain = true;
          }
          if (pleaseTryAgain && countEngineIsNotReady < 10) {
            logger.info(
                "Scenario [{}] file[{}] No BPM ENGINE running Sleep 30s. Scenario reference serverName[{}] serverType[{}]",
                scenario.getName(), scenarioFile.getName(), scenario.getServerName(), scenario.getServerType());
            try {
              Thread.sleep(1000 * 30);
            } catch (InterruptedException e) {
            }
          }
        } while (pleaseTryAgain && countEngineIsNotReady < 10);

        if (bpmnEngine == null) {
          logger.error("Scenario [{}] file[{}] No BPM ENGINE running. Scenario reference serverName[{}] serverType[{}]",
              scenario.getName(), scenarioFile.getName(), scenario.getServerName(), scenario.getServerType());
          continue;
        }

        bpmnEngine.turnHighFlowMode(true);
        logger.info("Scenario [{}] file[{}] use BpmnEngine {}", scenario.getName(), scenarioFile.getName(),
            bpmnEngine.getSignature());
        RunResult scenarioExecutionResult = automatorAPI.executeScenario(bpmnEngine, runParameters, scenario);
        logger.info("AutomatorStartup: end scenario [{}]", scenario.getName());
        bpmnEngine.turnHighFlowMode(false);

      } catch (AutomatorException e) {
        logger.error("Error during execution [{}]: {}", scenarioFileName, e.getMessage());
      }
    }
  }
}
