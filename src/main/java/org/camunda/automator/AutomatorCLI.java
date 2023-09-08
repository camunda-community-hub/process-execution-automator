package org.camunda.automator;

import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.configuration.ConfigurationBpmEngine;
import org.camunda.automator.definition.Scenario;
import org.camunda.automator.engine.RunParameters;
import org.camunda.automator.engine.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication

@Component
public class AutomatorCLI implements CommandLineRunner {
  public static boolean isRunningCLI = false;
  static Logger logger = LoggerFactory.getLogger(AutomatorCLI.class);
  @Autowired
  AutomatorAPI automatorAPI;
  @Autowired
  ConfigurationBpmEngine engineConfiguration;

  public static void main(String[] args) {
    isRunningCLI = true;
    SpringApplication app = new SpringApplication(AutomatorCLI.class);
    app.setBannerMode(Banner.Mode.OFF);
    System.exit(SpringApplication.exit(app.run(args)));
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  Usage                                                               */
  /*  -c, --conf <file>                                                   */
  /*     configuration file contains connection to engine                 */
  /*                                                                      */
  /*  -e, --engine ConnectionUrlString                                    */
  /*    CAMUNDA7;<URL>                                                    */
  /*    CAMUNDA8;CLOUD;<region>;<clusterId>;<clientIs>;<clientSecret>     */
  /*      CAMUNDA8;LOCAL;<gateway>;<plaintext>                            */
  /*                                                                      */
  /*  -d, --debug                                                         */
  /*       logs all steps                                                 */
  /*  -n, --numberofexecution <number>                                    */
  /*     override the number of execution for the scenario                */
  /*  -r, --recursive                                                     */
  /*    all *.json in the folder and sub-folder are monitored and executed*/
  /*                                                                      */
  /*    run <scenarioFile>                                                */
  /*                                                                      */
  /* ******************************************************************** */

  private static void printUsage() {
    System.out.println("Usage: <option> <action> <parameter>");
    System.out.println("  -s, --server <serverName>");
    System.out.println("    Which server to use in the configuration");
    System.out.println("  -e, --engine ConnectionUrlString");
    System.out.println("    CAMUNDA7;<URL>");
    System.out.println("    CAMUNDA8;CLOUD;<region>;<clusterId>;<clientIs>;<clientSecret>");
    System.out.println("      CAMUNDA8;LOCAL;<gateway>;<plaintext>");
    System.out.println("  -l, --level <DEBUG|COMPLETE|MONITORING|MAIN|NOTHING>");
    System.out.println("       Define the level of log (MONITORING is the default)");
    System.out.println("  -n, --numberofexecution <number>");
    System.out.println("     override the number of execution for the scenario");
    System.out.println("  -d, --deploy <TRUE|FALSE>");
    System.out.println("     Allow deployment of process is defined in the scenario (default is TRUE)");

    System.out.println("  -x, --execute");
    System.out.println("     execute the scenario");
    System.out.println("  -v, --verification");
    System.out.println("     verify the scenario");
    System.out.println("  -f, --fullreport");
    System.out.println("     Full report");

    System.out.println();
    System.out.println("ACTIONS: ");
    System.out.println("   run <scenarioFile>");
    System.out.println("       execute one scenario");
    System.out.println("   recursive <folder>");
    System.out.println("      all *.json in the folder and sub-folder are monitored and executed");

  }

  private static ConfigurationBpmEngine decodeConfiguration(String propertiesFileName) throws Exception {
    throw new Exception("Not yet implemented");
  }

  private static List<File> detectRecursiveScenario(File folderRecursive) {
    List<File> listFiles = new ArrayList<>();
    for (File file : folderRecursive.listFiles()) {
      if (file.isDirectory()) {
        listFiles.addAll(detectRecursiveScenario(file));
      } else if (file.getName().endsWith(".json")) {
        listFiles.add(file);
      }
    }
    return listFiles;
  }

  public void run(String[] args) {
    if (!isRunningCLI)
      return;
    File scenarioFile = null;
    File folderRecursive = null;

    RunParameters runParameters = new RunParameters();
    Integer overrideNumberOfExecution = null;
    int i = 0;
    ACTION action = null;
    String serverName = null;
    try {
      while (i < args.length) {
        if ("-h".equals(args[i]) || "--help".equals(args[i])) {
          printUsage();
          return;
        } else if ("-s".equals(args[i]) || "--server".equals(args[i])) {
          if (args.length < i + 1)
            throw new Exception("Bad usage : -c <ServerName>");
          serverName = args[i + 1];
          i++;
        } else if ("-e".equals(args[i]) || "--engine".equals(args[i])) {
          if (args.length < i + 1)
            throw new Exception("Bad usage : -e <ConnectionUrlString>");
          engineConfiguration = decodeConfiguration(args[i + 1]);
          i++;
        } else if ("-l".equals(args[i]) || "--level".equals(args[i])) {
          if (args.length < i + 1)
            throw new Exception("Bad usage : -l <DEBUG|MONITORING|MAIN|NOTHING>");
          runParameters.setLogLevel(RunParameters.LOGLEVEL.valueOf(args[i + 1]));
          i++;
        } else if ("-n".equals(args[i]) || "--numberofexecution".equals(args[i])) {
          if (args.length < i + 1)
            throw new Exception("Bad usage : n <numberofexecution>");
          overrideNumberOfExecution = Integer.parseInt(args[i + 1]);
          i++;
        } else if ("-d".equals(args[i]) || "--deploy".equals(args[i])) {
          if (args.length < i + 1)
            throw new Exception("Bad usage : -d TRUE|FALSE");
          runParameters.setDeploymentProcess("TRUE".equalsIgnoreCase(args[i + 1]));
          i++;
        } else if ("-x".equals(args[i]) || "--execute".equals(args[i])) {
          runParameters.setExecution(true);
        } else if ("-v".equals(args[i]) || "--verification".equals(args[i])) {
          runParameters.setVerification(true);
        } else if ("-f".equals(args[i]) || "--fullreport".equals(args[i])) {
          runParameters.setFullDetailsSynthesis(true);
        } else if ("run".equals(args[i])) {
          if (args.length < i + 1)
            throw new Exception("Bad usage : run <scenarioFile>");
          action = ACTION.RUN;
          scenarioFile = new File(args[i + 1]);
          i++;
        } else if ("recursive".equals(args[i])) {
          if (args.length < i + 1)
            throw new Exception("Bad usage : recursive <folder>");
          action = ACTION.RECURSIVE;
          folderRecursive = new File(args[i + 1]);
          i++;
        } else {
          printUsage();
          throw new Exception("Bad usage : unknown parameters [" + args[i] + "]");
        }
        i++;
      }

      if (action == null) {
        throw new Exception("Bad usage : missing action (" + ACTION.RUN + ")");
      }
      if (!runParameters.isExecution() && !runParameters.isVerification()) {
        runParameters.setExecution(true); // default
      }

      // get the correct server configuration
      ConfigurationBpmEngine.BpmnServerDefinition serverDefinition = null;
      if (serverName != null) {
        serverDefinition = engineConfiguration.getByServerName(serverName);

        if (serverDefinition == null) {
          throw new Exception("Check configuration: name[" + serverName
              + "] does not exist in the list of servers in application.yaml file");
        }
      } else {
        List<ConfigurationBpmEngine.BpmnServerDefinition> listServers = engineConfiguration.getListServers();

        serverDefinition = listServers.isEmpty() ? null : listServers.get(0);
      }
      if (serverDefinition == null) {
        throw new Exception(
            "Check configuration: configuration to access a Camunda server is missing in application.yaml");
      }

      long beginTime = System.currentTimeMillis();
      BpmnEngine bpmnEngine = automatorAPI.getBpmnEngine(engineConfiguration, serverDefinition);

      switch (action) {
      case RUN -> {
        Scenario scenario = automatorAPI.loadFromFile(scenarioFile);
        BpmnEngine bpmnEngineScenario = automatorAPI.getBpmnEngineFromScenario(scenario, engineConfiguration);
        RunResult scenarioExecutionResult = automatorAPI.executeScenario(
            bpmnEngineScenario == null ? bpmnEngine : bpmnEngineScenario, runParameters, scenario);

        logger.info(scenarioExecutionResult.getSynthesis(runParameters.isFullDetailsSynthesis()));
      }
      case RECURSIVE -> {
        List<File> listScenario = detectRecursiveScenario(folderRecursive);
        for (File scenarioFileIndex : listScenario) {
          Scenario scenario = automatorAPI.loadFromFile(scenarioFileIndex);
          BpmnEngine bpmnEngineScenario = automatorAPI.getBpmnEngineFromScenario(scenario, engineConfiguration);
          RunResult scenarioExecutionResult = automatorAPI.executeScenario(
              bpmnEngineScenario == null ? bpmnEngine : bpmnEngineScenario, runParameters, scenario);

          logger.info(scenarioExecutionResult.getSynthesis(false));
        }
      }
      }
      logger.info("That's all folks! " + (System.currentTimeMillis() - beginTime) + " ms.");

    } catch (Exception e) {
      logger.error("Error during execution " + e);
    }

  }

  public enum ACTION {RUN, RECURSIVE, VERIFY, RUNVERIFY, RECURSIVVERIFY}
}
