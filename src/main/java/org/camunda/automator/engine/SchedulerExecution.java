package org.camunda.automator.engine;

import org.camunda.automator.AutomatorCLI;
import org.camunda.automator.configuration.BpmnEngineList;
import org.camunda.automator.services.ServiceAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "automator.scheduler")
public class SchedulerExecution {

    @Value("${automator.scheduler.scenario-path:''}")
    public String scenarioPath;

    // https://www.baeldung.com/spring-boot-yaml-list
    // @Value("${automator.scheduler.colors}")
    @Autowired
    BpmnEngineList bpmnEngineConfiguration;
    Logger logger = LoggerFactory.getLogger(SchedulerExecution.class);
    @Autowired
    ServiceAccess serviceAccess;

    @PostConstruct
    public void init() {
        // We run the CLI, do nothing
        if (AutomatorCLI.isRunningCLI)
            return;
        logger.info("SchedulerExecution soon");
    }

}
