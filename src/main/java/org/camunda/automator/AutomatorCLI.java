package org.camunda.automator;

import org.camunda.automator.bpmnengine.BpmnEngineConfiguration;
import org.camunda.automator.definition.ScnHead;
import org.camunda.automator.engine.RunParameters;
import org.camunda.automator.engine.ScnRunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AutomatorCLI {
  static Logger logger = LoggerFactory.getLogger(AutomatorCLI.class);



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

  public static void main(String[] args) {
    File scenarioFile = null;
    File folderRecursive = null;
    BpmnEngineConfiguration engineConfiguration = BpmnEngineConfiguration.getCamunda7(
        "http://localhost:8080/engine-rest");

    RunParameters runParameters = new RunParameters();
    Integer overrideNumberOfExecution = null;
    int i = 0;
    ACTION action = null;
    try {
      while (i < args.length) {
        if ("-h".equals(args[i]) || "--help".equals(args[i])) {
          System.out.println("Usage: <option> <action> <parameter>");
          System.out.println("  -c, --conf <file>");
          System.out.println("    configuration file contains connection to engine");
          System.out.println("  -e, --engine ConnectionUrlString");
          System.out.println("    CAMUNDA7;<URL>");
          System.out.println("    CAMUNDA8;CLOUD;<region>;<clusterId>;<clientIs>;<clientSecret>");
          System.out.println("      CAMUNDA8;LOCAL;<gateway>;<plaintext>");
          System.out.println("  -l, --level <DEBUG|COMPLETE|MONITORING|MAIN|NOTHING>");
          System.out.println("       Define the level of log (MONITORING is the default)");
          System.out.println("  -n, --numberofexecution <number>");
          System.out.println("     override the number of execution for the scenario");
          System.out.println();
          System.out.println("ACTIONS: ");
          System.out.println("   run <scenarioFile>");
          System.out.println("       execute one scenario");
          System.out.println("   recursive <folder>");
          System.out.println("      all *.json in the folder and sub-folder are monitored and executed");
          return;
        } else if ("-c".equals(args[i]) || "--conf".equals(args[i])) {
          if (args.length < i + 1)
            throw new Exception("Bad usage : -c <ConfigurationFile>");
          engineConfiguration = readConfiguration(args[i + 1]);
          i++;
        } else if ("-e".equals(args[i]) || "--engine".equals(args[i])) {
          if (args.length < i + 1)
            throw new Exception("Bad usage : -e <ConnectionUrlString>");
          engineConfiguration = decodeConfiguration(args[i + 1]);
          i++;
        } else if ("-l".equals(args[i]) || "--level".equals(args[i])) {
          if (args.length < i + 1)
            throw new Exception("Bad usage : -l <DEBUG|MONITORING|MAIN|NOTHING>");
          runParameters.logLevel = RunParameters.LOGLEVEL.valueOf(args[i + 1]);
          i++;
        }
        if ("-n".equals(args[i]) || "--numberofexecution".equals(args[i])) {
          if (args.length < i + 1)
            throw new Exception("Bad usage : n <numberofexecution>");
          overrideNumberOfExecution = Integer.parseInt(args[i + 1]);
          i++;
        } else if ("run".equals(args[i])) {
          if (args.length < i + 1)
            throw new Exception("Bad usage : run <scenarioFile>");
          action = ACTION.RUN;
          scenarioFile = new File(args[i + 1]);
          i++;
        } else if ("recursive".equals(args[i])) {
          if (args.length < i + 1)
            throw new Exception("Bad usage : recursive <folder>");
          action = ACTION.RECURSIV;
          folderRecursive = new File(args[i + 1]);
          i++;

        }
        i++;

      }

      if (action == null) {
        throw new Exception("Bad usage : missing action (" + ACTION.RUN + ")");
      }

      AutomatorAPI automatorAPI = new AutomatorAPI();

      long beginTime = System.currentTimeMillis();
      switch (action) {
      case RUN -> {
        ScnHead scenario = automatorAPI.loadFromFile(scenarioFile);
        ScnRunResult scenarioExecutionResult = automatorAPI.executeScenario(engineConfiguration, runParameters,
            scenario);

        logger.info(scenarioExecutionResult.getSynthesis(true));
      }
      case RECURSIV -> {
        List<File> listScenario = detectRecursiveScenario(folderRecursive);
        for (File scenarioFileIndex : listScenario) {
          ScnHead scenario = automatorAPI.loadFromFile(scenarioFileIndex);
          ScnRunResult scenarioExecutionResult = automatorAPI.executeScenario(engineConfiguration, runParameters,
              scenario);

          logger.info(scenarioExecutionResult.getSynthesis(false));
        }
      }
      }
      logger.info("That's all folks! "+(System.currentTimeMillis()-beginTime)+" ms.");

    } catch (Exception e) {
      logger.error("Error during execution " + e);
    }

  }

  private static BpmnEngineConfiguration readConfiguration(String propertiesFileName) throws Exception {
    throw new Exception("Not yet implemented");
  }

  private static BpmnEngineConfiguration decodeConfiguration(String propertiesFileName) throws Exception {
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

  public enum ACTION {RUN, RECURSIV}
}
