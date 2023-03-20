package org.camunda.automator.engine;


public class RunParameters {
  public LOGLEVEL logLevel = LOGLEVEL.MONITORING;

  public int numberOfThreadsPerScenario = 10;

  /**
   * Execute the scenario (execution part): create process instance, execute user & service task
   */
  public boolean execute=true;
  /**
   * Verify the scenario (verification part) : check that tasks exist
   */
  public boolean verification=false;

  /**
   * Allow any deployment
   */
  public boolean allowDeployment = true;

  public int getNumberOfThreadsPerScenario() {
    return (numberOfThreadsPerScenario<=0? 1 : numberOfThreadsPerScenario);
  }

  public boolean isLevelMonitoring() {
    return logLevel.equals(LOGLEVEL.MONITORING) || logLevel.equals(LOGLEVEL.DEBUG) || logLevel.equals(LOGLEVEL.COMPLETE);
  }

  public boolean isLevelStoreDetails() {
    return logLevel.equals(LOGLEVEL.DEBUG) || logLevel.equals(LOGLEVEL.COMPLETE);
  }
  public enum LOGLEVEL {DEBUG, COMPLETE, MONITORING, MAIN, NOTHING}

  /**
   * Load the scenario path here. Some functions may be relative to this path
   */
  public String scenarioPath;

}
