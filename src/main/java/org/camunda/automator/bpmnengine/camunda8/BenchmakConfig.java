package org.camunda.automator.bpmnengine.camunda8;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class BenchmakConfig {

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService benchmarkScheduledExecutorService2() {
        return Executors.newScheduledThreadPool(4);
    }
}
