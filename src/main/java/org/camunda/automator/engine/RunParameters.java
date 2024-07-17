package org.camunda.automator.engine;

import java.util.Collections;
import java.util.List;

public class RunParameters {
  private LOGLEVEL logLevel = LOGLEVEL.MONITORING;

  private String serverName;

  private int numberOfThreadsPerScenario = 10;

  /**
   * Execute the scenario (execution part): create process instance, execute user & service task
   */
  private boolean execution = false;

  /**
   * On execution, it's possible to pilot each item, one by one
   */
  private boolean creation = true;
  private boolean servicetask = true;
  private boolean usertask = true;
  /**
   * Verify the scenario (verification part) : check that tasks exist
   */
  private boolean verification = false;

  /**
   * After the execution, clean the processInstance
   */
  private boolean clearAllAfter = false;

  /**
   * Allow any deployment
   */
  private boolean deploymentProcess = true;

  private boolean fullDetailsSynthesis = false;
  private List<String> filterServiceTask = Collections.emptyList();

  private boolean deepTracking = true;
  /**
   * Load the scenario path here. Some functions may be relative to this path
   */
  private String scenarioPath;

  private boolean warmingUp = true;

  public LOGLEVEL getLogLevel() {
    return logLevel;
  }

  public RunParameters setLogLevel(LOGLEVEL logLevel) {
    this.logLevel = logLevel;
    return this;
  }

  public String getServerName() {
    return this.serverName;
  }

  public RunParameters setServerName(String serverName) {
    this.serverName = serverName;
    return this;
  }

  public boolean isExecution() {
    return execution;
  }

  public RunParameters setExecution(boolean execution) {
    this.execution = execution;
    return this;
  }

  public boolean isCreation() {
    return creation;
  }

  public RunParameters setCreation(boolean creation) {
    this.creation = creation;
    return this;
  }

  public boolean isServiceTask() {
    return servicetask;
  }

  public RunParameters setServiceTask(boolean servicetask) {
    this.servicetask = servicetask;
    return this;
  }

  public boolean isUserTask() {
    return usertask;
  }

  public RunParameters setUserTask(boolean usertask) {
    this.usertask = usertask;
    return this;
  }

  public boolean isVerification() {
    return verification;
  }

  public RunParameters setVerification(boolean verification) {
    this.verification = verification;
    return this;
  }

  public boolean isClearAllAfter() {
    return clearAllAfter;
  }

  public RunParameters setClearAllAfter(boolean clearAllAfter) {
    this.clearAllAfter = clearAllAfter;
    return this;
  }

  public boolean isDeploymentProcess() {
    return deploymentProcess;
  }

  public RunParameters setDeploymentProcess(boolean deploymentProcess) {
    this.deploymentProcess = deploymentProcess;
    return this;
  }

  public boolean isFullDetailsSynthesis() {
    return fullDetailsSynthesis;
  }

  public RunParameters setFullDetailsSynthesis(boolean fullDetailsSynthesis) {
    this.fullDetailsSynthesis = fullDetailsSynthesis;
    return this;
  }

  public List<String> getFilterServiceTask() {
    return filterServiceTask;
  }

  public RunParameters setFilterServiceTask(List<String> filterServiceTask) {
    this.filterServiceTask = filterServiceTask;
    return this;
  }

  public boolean isDeepTracking() {
    return deepTracking;
  }

  public RunParameters setDeepTracking(boolean deepTracking) {
    this.deepTracking = deepTracking;
    return this;
  }

  public String getScenarioPath() {
    return scenarioPath;
  }

  public RunParameters setScenarioPath(String scenarioPath) {
    this.scenarioPath = scenarioPath;
    return this;
  }

  public boolean isWarmingUp() {
    return warmingUp;
  }

  public RunParameters setWarmingUp(boolean warmingUp) {
    this.warmingUp = warmingUp;
    return this;
  }

  public int getNumberOfThreadsPerScenario() {
    return (numberOfThreadsPerScenario <= 0 ? 1 : numberOfThreadsPerScenario);
  }

  public RunParameters setNumberOfThreadsPerScenario(int numberOfThreadsPerScenario) {
    this.numberOfThreadsPerScenario = numberOfThreadsPerScenario;
    return this;
  }

  public boolean showLevelDebug() {
    return getLogLevelAsNumber() >= 5;
  }

  public boolean showLevelInfo() {
    return getLogLevelAsNumber() >= 4;
  }

  public boolean showLevelMonitoring() {
    return getLogLevelAsNumber() >= 3;
  }

  public boolean showLevelDashboard() {
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
    return switch (logLevel) {
      case NOTHING -> 0;
      case MAIN -> 1;
      case DASHBOARD -> 2;
      case MONITORING -> 3;
      case INFO -> 4;
      case DEBUG -> 5;
      default -> 0;
    };
  }

  public enum LOGLEVEL {DEBUG, INFO, MONITORING, DASHBOARD, MAIN, NOTHING}

}
