package org.camunda.automator.engine;

public class RunParameters {
  public LOGLEVEL logLevel = LOGLEVEL.MONITORING;

  public int numberOfThreadsPerScenario = 10;

  /**
   * Execute the scenario (execution part): create process instance, execute user & service task
   */
  public boolean execution = false;
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
  public boolean allowDeployment = true;

  public boolean fullDetailsSythesis = false;

  public int getNumberOfThreadsPerScenario() {
    return (numberOfThreadsPerScenario <= 0 ? 1 : numberOfThreadsPerScenario);
  }

  public enum LOGLEVEL {DEBUG, INFO, MONITORING, MAIN, NOTHING}

  public boolean isLevelDebug() {
    return getLogLevelAsNumber() >= 4;
  }

  public boolean isLevelInfo() {
    return getLogLevelAsNumber() >= 3;
  }

  public boolean isLevelMonitoring() {
    return getLogLevelAsNumber() >= 2;
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

  /**
   * Load the scenario path here. Some functions may be relative to this path
   */
  public String scenarioPath;

}
