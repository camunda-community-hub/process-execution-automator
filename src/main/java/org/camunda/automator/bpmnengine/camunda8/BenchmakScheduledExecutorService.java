package org.camunda.automator.bpmnengine.camunda8;

import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
public class BenchmakScheduledExecutorService {


    public ScheduledExecutorService getScheduledExecutorService() {
        return Executors.newScheduledThreadPool(4);
    }
}
