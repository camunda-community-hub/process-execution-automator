package org.camunda.automator.engine;

public class RunParameters {
  public LOGLEVEL logLevel = LOGLEVEL.MONITORING;

  public int numberOfThreadsPerScenario = 10;

  public int getNumberOfThreadsPerScenario() {
    return numberOfThreadsPerScenario;
  }

  public boolean isLevelMonitoring() {
    return logLevel.equals(LOGLEVEL.MONITORING) || logLevel.equals(LOGLEVEL.DEBUG) || logLevel.equals(LOGLEVEL.COMPLETE);
  }

  public boolean isLevelStoreDetails() {
    return logLevel.equals(LOGLEVEL.DEBUG) || logLevel.equals(LOGLEVEL.COMPLETE);
  }
  public enum LOGLEVEL {DEBUG, COMPLETE, MONITORING, MAIN, NOTHING}
}
