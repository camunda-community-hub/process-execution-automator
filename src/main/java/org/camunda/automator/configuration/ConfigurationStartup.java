package org.camunda.automator.configuration;

import org.camunda.automator.engine.RunParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Component
@Configuration
public class ConfigurationStartup {
    static Logger logger = LoggerFactory.getLogger(ConfigurationStartup.class);
    @Value("${automator.startup.scenarioPath}")
    public String scenarioPath;
    @Value("${automator.startup.logLevel:MONITORING}")
    public String logLevel;
    @Value("${automator.startup.deeptracking:false}")
    public boolean deepTracking;
    @Value("${automator.startup.policyExecution:DEPLOYPROCESS|WARMINGUP|CREATION|SERVICETASK|USERTASK}")
    public String policyExecution;

    @Value("${automator.startEvent.nbThreads:#{null}}")
    public Integer startEventNbThreads;

    /**
     * it may be necessary to wait the other component to warm up
     */
    @Value("${automator.startup.waitWarmUpServer:PT0S}")
    public String waitWarmupServer;

    @Value("${automator.startup.serverName}")
    private String serverName;

    @Value("#{'${automator.startup.scenarioFileAtStartup:}'.split(';')}")
    private List<String> scenarioFileAtStartup;

    @Value("${automator.startup.scenarioResourceAtStartup:}")
    private Resource scenarioResourceAtStartup;

    @Value("#{'${automator.startup.filterService:}'.split(';')}")
    private List<String> filterService;

    public String getServerName() {
        return serverName;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public RunParameters.LOGLEVEL getLogLevelEnum() {
        try {
            return RunParameters.LOGLEVEL.valueOf(logLevel);
        } catch (Exception e) {
            logger.error("Unknow LogLevel (automator.startup.loglevel) : [{}} ", logLevel, e);
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

    public Integer getStartEventNbThreads() {
        return startEventNbThreads;
    }

    public List<String> getScenarioFileAtStartup() {
        return recalibrateAfterSplit(scenarioFileAtStartup);
    }

    /**
     * Return the name for the variable scenarioAtStartup
     *
     * @return the name
     */
    public String getScenarioFileAtStartupName() {
        return "automator.startup.scenarioAtStartup";
    }

    /**
     * Return the list of collection - only one at this moment
     *
     * @return list of scenario detected as a resource
     */
    public List<Resource> getScenarioResourceAtStartup() {
        return Collections.singletonList(scenarioResourceAtStartup);
    }

    /**
     * return the name of the resourceAtStartup variable name
     *
     * @return name of the variable
     */
    public String getScenarioResourceAtStartupName() {
        return "automator.startup.scenarioResourceAtStartup";
    }

    public List<String> getFilterService() {
        return recalibrateAfterSplit(filterService);
    }

    public Duration getWarmingUpServer() {
        try {
            return Duration.parse(waitWarmupServer);
        } catch (Exception e) {
            logger.error("Can't parse warmup [{}]", waitWarmupServer, e);
            return Duration.ZERO;
        }
    }

    private List<String> recalibrateAfterSplit(List<String> originalList) {
        if (originalList.size() == 1 && originalList.get(0).isEmpty())
            return Collections.emptyList();
        return originalList;
    }
}
