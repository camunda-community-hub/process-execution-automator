package org.camunda.automator.engine;

import org.camunda.automator.AutomatorCLI;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.bpmnengine.BpmnEngineConfiguration;
import org.camunda.automator.bpmnengine.BpmnEngineFactory;
import org.camunda.automator.definition.Scenario;
import org.camunda.automator.services.ServiceAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
@Configuration
@ConfigurationProperties(prefix = "automator.execution")
public class SchedulerExecution {

  @Value("${automator.execution.run-at-startup}")
  public boolean runAtStartup;
  @Value("${automator.execution.scenario-path}")
  public String scenarioPath;
  @Value("${automator.execution.server.type}")
  public String serverType;
  @Value("${automator.execution.server.url}")
  public String serverUrl;
  Logger logger = LoggerFactory.getLogger(SchedulerExecution.class);

  @Autowired
  ServiceAccess serviceAccess;

  @PostConstruct
  public void init() {
    // We run the CLI, do nothing
    if (AutomatorCLI.isRunningCLI)
      return;
    // read the configuration, and start the execution
    if (scenarioPath != null && serverType != null && runAtStartup) {
      // execute all test now
      BpmnEngineConfiguration bpmnEngineConfiguration = getEngineConfiguration();
      if (bpmnEngineConfiguration == null) {
        logger.error("Unknown configuration");
        return;
      }
      BpmnEngine bpmnEngine;
      try {
        bpmnEngine = BpmnEngineFactory.getInstance().getEngineFromConfiguration(bpmnEngineConfiguration);
      } catch (Exception e) {
        logger.error("SchedulerExecution.init: Server connection Initialization error");
        return;
      }
      RunParameters runParameters = new RunParameters();
      runParameters.logLevel = RunParameters.LOGLEVEL.MONITORING;

      // parse all file and sub directory so search scenarion
      List<File> listScenarioFile = collectScenario(new File(scenarioPath));
      for (File fileScenario : listScenarioFile) {
        try {
          Scenario scenario = Scenario.createFromFile(fileScenario);
          RunScenario scenarioExecution = new RunScenario(scenario, bpmnEngine, runParameters, serviceAccess);
          RunResult scenarioExecutionResult = scenarioExecution.runScenario();
        } catch (Exception e) {
          logger.error("Error processing [" + fileScenario.getAbsolutePath() + "] " + e.getMessage());
        }
      }
    }
  }

  private BpmnEngineConfiguration getEngineConfiguration() {

    BpmnEngineConfiguration.CamundaEngine engineType = BpmnEngineConfiguration.CamundaEngine.valueOf(
        serverType.toUpperCase());
    if (engineType.equals(BpmnEngineConfiguration.CamundaEngine.CAMUNDA_7)) {
      return BpmnEngineConfiguration.getCamunda7(serverUrl);
    }
    return null;
  }

  private List<File> collectScenario(File folder) {
    List<File> listFiles = new ArrayList<>();
    File[] filesListInFolder = folder.listFiles();
    for (File file : filesListInFolder) {
      if (file.isDirectory()) {
        listFiles.addAll(collectScenario(file));
      }
      if (file.getName().endsWith(".json"))
        listFiles.add(file);
    }
    return listFiles;
  }
}
