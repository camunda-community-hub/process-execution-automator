package org.camunda.automator.engine;

import java.util.Collections;
import java.util.List;

public class RunParameters {
  public LOGLEVEL logLevel = LOGLEVEL.MONITORING;

  public int numberOfThreadsPerScenario = 10;

  /**
   * Execute the scenario (execution part): create process instance, execute user & service task
   */
  public boolean execution = false;

  /**
   * On execution, it's possible to pilot each item, one by one
   */
  public boolean creation = true;
  public boolean servicetask = true;
  public boolean usertask = true;
  /**
   * Verify the scenario (verification part) : check that tasks exist
   */
  public boolean verification = false;

  /**
   * After the execution, clean the processInstance
   */
  public boolean clearAllAfter = false;

  /**
   * Allow any deployment
   */
  public boolean deploymentProcess = true;

  public boolean fullDetailsSythesis = false;
  public List<String> filterServiceTask = Collections.emptyList();
  /**
   * Load the scenario path here. Some functions may be relative to this path
   */
  public String scenarioPath;

  public boolean warmingUp =true;

  public int getNumberOfThreadsPerScenario() {
    return (numberOfThreadsPerScenario <= 0 ? 1 : numberOfThreadsPerScenario);
  }

  public boolean isLevelDebug() {
    return getLogLevelAsNumber() >= 4;
  }

  public boolean isLevelInfo() {
    return getLogLevelAsNumber() >= 3;
  }

  public boolean isLevelMonitoring() {
    return getLogLevelAsNumber() >= 2;
  }

  public void setFilterExecutionServiceTask(List<String> filterServiceTask) {
    this.filterServiceTask = filterServiceTask;
  }

  public boolean blockExecutionServiceTask(String topic) {
    // no filter: execute everything
    if (filterServiceTask.isEmpty())
      return false;
    // filter in place: only if the topic is registered
    return !filterServiceTask.contains(topic);
  }



  private int getLogLevelAsNumber() {
    switch (logLevel) {
    case NOTHING -> {
      return 0;
    }
    case MAIN -> {
      return 1;
    }
    case MONITORING -> {
      return 2;
    }
    case INFO -> {
      return 3;
    }
    case DEBUG -> {
      return 4;
    }
    }
    return 0;
  }

  public enum LOGLEVEL {DEBUG, INFO, MONITORING, MAIN, NOTHING}

}
