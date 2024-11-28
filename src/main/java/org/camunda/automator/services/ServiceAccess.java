package org.camunda.automator.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

/**
 * This class reference all services, and can be pass in any new object to give access to all services
 */
@Service
@Configuration
public class ServiceAccess {

    private final Logger logger = LoggerFactory.getLogger(ServiceAccess.class);
    @Autowired
    public ServiceDataOperation serviceDataOperation;
    @Value("${scheduler.poolSize}")
    private int schedulerPoolSize;

    /**
     * Executor to run everything that is scheduled (also @Scheduled)
     */
    public TaskScheduler getTaskScheduler(String schedulerName) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(schedulerPoolSize);
        scheduler.setThreadNamePrefix(schedulerName);
        scheduler.initialize();
        return scheduler;
    }
}
