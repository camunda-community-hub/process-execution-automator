package org.camunda.automator;

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
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@SpringBootApplication

@Component
public class AutomatorCLI implements CommandLineRunner {
    public static boolean isRunningCLI = false;
    static Logger logger = LoggerFactory.getLogger(AutomatorCLI.class);
    @Autowired
    AutomatorAPI automatorAPI;
    @Autowired
    BpmnEngineList engineConfiguration;

    @Autowired
    ConfigurationStartup configurationStartup;

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
        logOutLn("Usage: <option> <action> <parameter>");
        logOutLn("  -s, --server <serverName>");
        logOutLn("    Which server to use in the configuration");
        logOutLn("  -e, --engine ConnectionUrlString");
        logOutLn("    CAMUNDA7;<URL>");
        logOutLn("    CAMUNDA8;CLOUD;<region>;<clusterId>;<clientIs>;<clientSecret>");
        logOutLn("      CAMUNDA8;LOCAL;<gateway>;<plaintext>");
        logOutLn("  -l, --level <DEBUG|COMPLETE|MONITORING|MAIN|NOTHING>");
        logOutLn("       Define the level of log (MONITORING is the default)");
        logOutLn("  -n, --numberofexecution <number>");
        logOutLn("     override the number of execution for the scenario");
        logOutLn("  -d, --deploy <TRUE|FALSE>");
        logOutLn("     Allow deployment of process is defined in the scenario (default is TRUE)");

        logOutLn("  -x, --execute");
        logOutLn("     execute the scenario");
        logOutLn("  -v, --verification");
        logOutLn("     verify the scenario");
        logOutLn("  -f, --fullreport");
        logOutLn("     Full report");

        logOutLn("");
        logOutLn("ACTIONS: ");
        logOutLn("   run <scenarioFile>");
        logOutLn("       execute one scenario");
        logOutLn("   recursive <folder>");
        logOutLn("      all *.json in the folder and sub-folder are monitored and executed");

    }

    private static BpmnEngineList decodeConfiguration(String propertiesFileName) throws Exception {
        throw new Exception("Not yet implemented");
    }

    private static List<Path> detectRecursiveScenario(Path folderRecursive) {
        List<Path> listFiles = new ArrayList<>();
        try (Stream<Path> files = Files.list(folderRecursive)) {
            // Iterate over all files in the directory
            files.forEach(file -> {
                if (Files.isRegularFile(file)) {
                    listFiles.add(file);
                }
                if (Files.isDirectory(file)) {
                    listFiles.addAll(detectRecursiveScenario(file));
                }
            });
        } catch (IOException e) {
            logger.error("During detection scenario file: {}", e.getMessage());
        }
        return listFiles;
    }

    /**
     * To reduce the number of warning
     *
     * @param message message to log out
     */
    private static void logOutLn(String message) {
        System.out.println(message);
    }

    public void run(String[] args) {
        if (!isRunningCLI)
            return;
        Path scenarioFile = null;
        Path folderRecursive = null;

        RunParameters runParameters = new RunParameters();
        runParameters.setExecution(true)
                .setServerName(configurationStartup.getServerName())
                .setLogLevel(configurationStartup.getLogLevelEnum())
                .setCreation(configurationStartup.isPolicyExecutionCreation())
                .setServiceTask(configurationStartup.isPolicyExecutionServiceTask())
                .setUserTask(configurationStartup.isPolicyExecutionUserTask())
                .setWarmingUp(configurationStartup.isPolicyExecutionWarmingUp())
                .setDeploymentProcess(configurationStartup.isPolicyDeployProcess())
                .setDeepTracking(configurationStartup.deepTracking())
                .setStartEventNbThreads(configurationStartup.getStartEventNbThreads());
        List<String> filterService = configurationStartup.getFilterService();
        if (filterService != null) {
            runParameters.setFilterExecutionServiceTask(filterService);
        }
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
                        throw new AutomatorException("Bad usage : -c <ServerName>");
                    serverName = args[i + 1];
                    i++;
                } else if ("-e".equals(args[i]) || "--engine".equals(args[i])) {
                    if (args.length < i + 1)
                        throw new AutomatorException("Bad usage : -e <ConnectionUrlString>");
                    engineConfiguration = decodeConfiguration(args[i + 1]);
                    i++;
                } else if ("-l".equals(args[i]) || "--level".equals(args[i])) {
                    if (args.length < i + 1)
                        throw new AutomatorException("Bad usage : -l <DEBUG|MONITORING|MAIN|NOTHING>");
                    runParameters.setLogLevel(RunParameters.LOGLEVEL.valueOf(args[i + 1]));
                    i++;
                } else if ("-n".equals(args[i]) || "--numberofexecution".equals(args[i])) {
                    if (args.length < i + 1)
                        throw new AutomatorException("Bad usage : n <numberofexecution>");
                    overrideNumberOfExecution = Integer.parseInt(args[i + 1]);
                    i++;
                } else if ("-d".equals(args[i]) || "--deploy".equals(args[i])) {
                    if (args.length < i + 1)
                        throw new AutomatorException("Bad usage : -d TRUE|FALSE");
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
                        throw new AutomatorException("Bad usage : run <scenarioFile>");
                    action = ACTION.RUN;
                    scenarioFile = Paths.get(args[i + 1]);
                    i++;
                } else if ("recursive".equals(args[i])) {
                    if (args.length < i + 1)
                        throw new AutomatorException("Bad usage : recursive <folder>");
                    action = ACTION.RECURSIVE;
                    folderRecursive = Paths.get(args[i + 1]);
                    i++;
                } else {
                    printUsage();
                    throw new AutomatorException("Bad usage : unknown parameters [" + args[i] + "]");
                }
                i++;
            }

            if (action == null) {
                throw new AutomatorException("Bad usage : missing action (" + ACTION.RUN + ")");
            }
            if (!runParameters.isExecution() && !runParameters.isVerification()) {
                runParameters.setExecution(true); // default
            }

            // get the correct server configuration
            BpmnEngineList.BpmnServerDefinition serverDefinition = null;

            serverDefinition = engineConfiguration.getByServerName(serverName);
            if (serverDefinition == null) {
                throw new AutomatorException("Check configuration: Server name (from parameter)[" + serverName
                        + "] does not exist in the list of servers in application.yaml file");
            }

            long beginTime = System.currentTimeMillis();
            BpmnEngine bpmnEngine = automatorAPI.getBpmnEngine(serverDefinition, true);

            switch (action) {
                case RUN -> {
                    Scenario scenario = automatorAPI.loadFromFile(scenarioFile);
                    BpmnEngine bpmnEngineScenario = automatorAPI.getBpmnEngine(serverDefinition, true);

                    // execution
                    RunResult scenarioExecutionResult = automatorAPI.executeScenario(
                            bpmnEngineScenario == null ? bpmnEngine : bpmnEngineScenario, runParameters, scenario);

                    logger.info(scenarioExecutionResult.getSynthesis(runParameters.isFullDetailsSynthesis()));
                }
                case RECURSIVE -> {
                    List<Path> listScenario = detectRecursiveScenario(folderRecursive);
                    for (Path scenarioFileIndex : listScenario) {
                        Scenario scenario = automatorAPI.loadFromFile(scenarioFileIndex);
                        BpmnEngine bpmnEngineScenario = automatorAPI.getBpmnEngine(serverDefinition, true);
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
