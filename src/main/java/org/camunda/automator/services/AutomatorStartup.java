package org.camunda.automator.services;

import org.camunda.automator.AutomatorAPI;
import org.camunda.automator.AutomatorCLI;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.configuration.BpmnEngineList;
import org.camunda.automator.configuration.ConfigurationStartup;
import org.camunda.automator.definition.Scenario;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunParameters;
import org.camunda.automator.engine.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
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
  BpmnEngineList engineConfiguration;

  @Autowired
  ServiceAccess serviceAccess;

  @PostConstruct
  public void init() {
    if (AutomatorCLI.isRunningCLI)
      return;

    logger.info("AutomatorStartup-start");
    AutomatorSetupRunnable automatorSetupRunnable = new AutomatorSetupRunnable(configurationStartup, automatorAPI,
        automatorCLI, engineConfiguration);

    // start the automator startup immediately, and Spring can continue to start the application
    serviceAccess.getTaskScheduler("AutomatorSetup").schedule(automatorSetupRunnable, Instant.now());

  }

  private void runFixedWarmup() {
    // Fixed Warmup
    if (configurationStartup.getWarmingUpServer().getSeconds() > 30) {
      logger.info("WarmupFixedTime: wait.... {} s", configurationStartup.getWarmingUpServer().getSeconds());

      try {
        Thread.sleep(configurationStartup.getWarmingUpServer().toMillis());
      } catch (Exception e) {
        // do nothing
      }
      logger.info("WarmupFixedTime: end");
    }
  }

  /**
   * Load all scenario. List of File or Resource
   *
   * @return list of scenario
   */
  private List<Object> registerScenario() {
    List<Object> scenarioList = new ArrayList<>();
    // File
    if (configurationStartup.getScenarioFileAtStartup().isEmpty()) {
      logger.info("No scenario [File] from variable {} given", configurationStartup.getScenarioFileAtStartupName());
    } else {
      logger.info("Detect {} scenario [File] from variable [{}] ScenarioPath[{}]",
          configurationStartup.getScenarioFileAtStartup().size(), configurationStartup.getScenarioFileAtStartupName(),
          configurationStartup.scenarioPath);

      for (String scenarioFileName : configurationStartup.getScenarioFileAtStartup()) {
        logger.info("Register scenario [File] [{}]", scenarioFileName);

        File scenarioFile = new File(configurationStartup.scenarioPath + "/" + scenarioFileName);
        if (!scenarioFile.exists()) {
          scenarioFile = new File(scenarioFileName);
        }
        if (!scenarioFile.exists()) {
          logger.error("ScenarioFile: Can't find File [{}/{}] or [{}]", configurationStartup.scenarioPath,
              scenarioFileName, scenarioFileName);
          continue;
        }
        scenarioList.add(scenarioFile);
      }
    }
    // Resource
    if (configurationStartup.getScenarioResourceAtStartup().isEmpty()) {
      logger.info("No scenario [Resource] from variable {} given",
          configurationStartup.getScenarioResourceAtStartupName());
    } else {
      logger.info("Detect {} scenario [Resource] from variable [{}]",
          configurationStartup.getScenarioResourceAtStartup().size(),
          configurationStartup.getScenarioResourceAtStartupName());

      for (Resource resource : configurationStartup.getScenarioResourceAtStartup()) {
        if (resource != null) {
          logger.info("Load scenario [Resource] from [{}]", resource.getDescription());
          scenarioList.add(resource);
        }
      }
    }

    return scenarioList;
  }

  /**
   * AutomatorSetupRunnable - run in parallel
   */
  class AutomatorSetupRunnable implements Runnable {

    ConfigurationStartup configurationStartup;

    AutomatorAPI automatorAPI;

    AutomatorCLI automatorCLI;

    BpmnEngineList engineConfiguration;

    public AutomatorSetupRunnable(ConfigurationStartup configurationStartup,
                                  AutomatorAPI automatorAPI,
                                  AutomatorCLI automatorCLI,
                                  BpmnEngineList engineConfiguration) {
      this.configurationStartup = configurationStartup;
      this.automatorAPI = automatorAPI;
      this.automatorCLI = automatorCLI;
      this.engineConfiguration = engineConfiguration;
    }

    @Override
    public void run() {

      RunParameters runParameters = new RunParameters();
      runParameters.setExecution(true)
          .setServerName(configurationStartup.getServerName())
          .setLogLevel(configurationStartup.getLogLevelEnum())
          .setCreation(configurationStartup.isPolicyExecutionCreation())
          .setServiceTask(configurationStartup.isPolicyExecutionServiceTask())
          .setUserTask(configurationStartup.isPolicyExecutionUserTask())
          .setWarmingUp(configurationStartup.isPolicyExecutionWarmingUp())
          .setDeploymentProcess(configurationStartup.isPolicyDeployProcess())
          .setDeepTracking(configurationStartup.deepTracking());
      List<String> filterService = configurationStartup.getFilterService();
      if (filterService != null) {
        runParameters.setFilterExecutionServiceTask(filterService);
      }

      logger.info(
          "AutomatorStartup parameters serverName[{}] warmingUp[{}] creation:[{}] serviceTask:[{}] userTask:[{}] ScenarioPath[{}] logLevel[{}] waitWarmingUpServer[{} s]",
          runParameters.getServerName(), runParameters.isWarmingUp(), runParameters.isCreation(),
          runParameters.isServiceTask(), runParameters.isUserTask(), configurationStartup.scenarioPath,
          configurationStartup.logLevel, configurationStartup.getWarmingUpServer().toMillis() / 1000);

      try {
        String currentPath = new java.io.File(".").getCanonicalPath();
        logger.info("Local Path[{}]", currentPath);
      } catch (Exception e) {
        logger.error("Can't access Local Path : {} ", e.getMessage());
      }

      runFixedWarmup();

      // Load scenario
      List<Object> scenarioList = registerScenario();

      // now proceed all scenario
      for (Object scenarioObject : scenarioList) {
        Scenario scenario = null;
        if (scenarioObject instanceof File scenarioFile)
          try {
            scenario = automatorAPI.loadFromFile(scenarioFile);
          } catch (Exception e) {
            logger.error("Error during accessing InputStream from File [{}]: {}", scenarioFile.getAbsolutePath(),
                e.getMessage());
          }
        else if (scenarioObject instanceof Resource scenarioResource) {
          try {
            scenario = automatorAPI.loadFromInputStream(scenarioResource.getInputStream(),
                scenarioResource.getDescription());
          } catch (Exception e) {
            logger.error("Error during accessing InputStream from resource [{}]: {}", scenarioResource.getDescription(),
                e.getMessage());
          }
        }
        if (scenario == null)
          continue;
        logger.info("Start scenario [{}] on (1)ScenarioServerName[{}] (2)ConfigurationServerName[{}]",
            scenario.getName(), scenario.getServerName(), runParameters.getServerName());

        // BpmnEngine: find the correct one referenced in the scenario
        int countEngineIsNotReady = 0;
        BpmnEngine bpmnEngine = null;
        boolean pleaseTryAgain;
        String message = "";
        do {
          pleaseTryAgain = false;
          countEngineIsNotReady++;
          try {
            if (scenario.getServerName() != null && !scenario.getServerName().isEmpty()) {
              message += "ScenarioServerName[" + scenario.getServerName() + "];";
              bpmnEngine = automatorAPI.getBpmnEngineFromScenario(scenario, engineConfiguration);
            } else {
              if (runParameters.getServerName() == null)
                throw new AutomatorException("No Server define in configuration");
              message += "ConfigurationServerName[" + runParameters.getServerName() + "];";
              BpmnEngineList.BpmnServerDefinition serverDefinition = engineConfiguration.getByServerName(
                  runParameters.getServerName());
              if (serverDefinition == null)
                throw new AutomatorException(
                    "Server [" + runParameters.getServerName() + "] does not exist in the list");

              if (runParameters.showLevelMonitoring())
              {
                logger.info("Run scenario with Server {}", serverDefinition.getSynthesis());
              }
              bpmnEngine = automatorAPI.getBpmnEngine(serverDefinition, true);
            }
            if (runParameters.showLevelDashboard()) {
              logger.info("Scenario [{}] Connect to BpmnEngine {}", scenario.getName(), message);
            }

            if (!bpmnEngine.isReady()) {
              bpmnEngine.connection();
            }
          } catch (AutomatorException e) {
            pleaseTryAgain = true;
            message += "EXCEPT " + e.getMessage();
          }
          if (pleaseTryAgain && countEngineIsNotReady < 10) {
            logger.info(
                "Scenario [{}] file[{}] No BPM ENGINE running [{}] tentative:{}/10. Sleep 30s. Scenario reference serverName[{}]",
                message, countEngineIsNotReady, scenario.getName(), scenario.getName(), scenario.getServerName());
            try {
              Thread.sleep(((long) 1000) * 30);
            } catch (InterruptedException e) {
              // nothing to do
            }
          }
        } while (pleaseTryAgain && countEngineIsNotReady < 10);

        if (bpmnEngine == null) {
          logger.error("Scenario [{}] file[{}] Server {} No BPM ENGINE running.", scenario.getName(),
              scenario.getName(), message);
          continue;
        }

        bpmnEngine.turnHighFlowMode(true);
        logger.info("Scenario [{}] file[{}] use BpmnEngine {}", scenario.getName(), scenario.getName(),
            bpmnEngine.getSignature());
        RunResult scenarioExecutionResult = automatorAPI.executeScenario(bpmnEngine, runParameters, scenario);
        logger.info("AutomatorStartup: end scenario [{}] in {} ms", scenario.getName(),
            scenarioExecutionResult.getTimeExecution());
        bpmnEngine.turnHighFlowMode(false);

      }
    }
  }
}

