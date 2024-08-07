/**
 * ConfigurationBpmEngine
 * Configuration are defined in the application.yaml file.
 *
 * @return
 */
package org.camunda.automator.configuration;

import org.camunda.automator.engine.AutomatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

@Component

public class BpmnEngineList {
  public static final int DEFAULT_VALUE_EXECUTION_THREADS = 100;
  public static final int DEFAULT_VALUE_MAX_JOBS_ACTIVE = -1;
  public static final String CONF_WORKER_MAX_JOBS_ACTIVE = "workerMaxJobsActive";
  public static final String CONF_WORKER_EXECUTION_THREADS = "workerExecutionThreads";
  public static final String CONF_TASK_LIST_URL = "taskListUrl";
  public static final String CONF_TASK_LIST_USER = "taskListUserName";
  public static final String CONF_TASK_LIST_PASSWORD = "taskListUserPassword";
  public static final String CONF_TASK_LIST_CLIENT_ID = "taskListClientId";
  public static final String CONF_TASK_LIST_CLIENT_SECRET = "taskListClientSecret";

  public static final String CONF_IDENTITY_URL = "identityUrl";
  public static final String CONF_OPERATE_URL = "operateUrl";
  public static final String CONF_OPERATE_USER_PASSWORD = "operateUserPassword";
  public static final String CONF_OPERATE_USER_NAME = "operateUserName";
  public static final String CONF_AUTHENTICATIONURL = "authenticationUrl";
  public static final String CONF_OPERATE_CLIENT_ID = "operateClientId";
  public static final String CONF_OPERATE_CLIENT_SECRET = "operateClientSecret";
  public static final String CONF_OPERATE_AUDIENCE = "operateAudientce";

  public static final String CONF_ZEEBE_GATEWAY_ADDRESS = "zeebeGatewayAddress";
  public static final String CONF_URL = "url";
  public static final String CONF_TYPE = "type";
  public static final String CONF_TYPE_V_CAMUNDA_8 = "camunda8";
  public static final String CONF_TYPE_V_CAMUNDA_8_SAAS = "camunda8Saas";
  public static final String CONF_TYPE_V_CAMUNDA_7 = "camunda7";

  public static final String CONF_ZEEBE_SAAS_REGION = "region";
  public static final String CONF_ZEEBE_SECRET = "zeebeClientSecret";
  public static final String CONF_ZEEBE_SAAS_CLUSTER_ID = "clusterId";
  public static final String CONF_ZEEBE_CLIENT_ID = "zeebeClientId";
  public static final String CONF_ZEEBE_AUDIENCE = "zeebeAudience";
  public static final String ZEEBE_DEFAULT_AUDIENCE = "zeebe.camunda.io";

  static Logger logger = LoggerFactory.getLogger(BpmnEngineList.class);

  @Autowired
  ConfigurationServersEngine configurationServersEngine;

  private List<BpmnServerDefinition> allServers = new ArrayList<>();

  @PostConstruct
  public void init() {
    allServers = new ArrayList<>();

    try {
      // get From Server Connection
      allServers.addAll(getFromServerConfiguration());

      // decode the serverConnections
      allServers.addAll(getFromServersConnectionList());

      // decode serversList
      allServers.addAll(getFromServersList());

      // log all servers detected
      logger.info("ConfigurationBpmEngine: servers detected : {} ", allServers.size());
      for (BpmnServerDefinition server : allServers) {
        String serverDetails = "Configuration Server Type[" + server.serverType + "] ";
        if (server.serverType == null) {
          logger.error("ServerType not declared for server [{}]", server.name);
          return;
        }

        serverDetails += switch (server.serverType) {
          case CAMUNDA_8 -> "ZeebeadressGateway [" + server.zeebeGatewayAddress + "]";
          case CAMUNDA_8_SAAS ->
              "ZeebeClientId [" + server.zeebeClientId + "] ClusterId[" + server.zeebeSaasClusterId + "] RegionId["
                  + server.zeebeSaasRegion + "]";
          case CAMUNDA_7 -> "Camunda7URL [" + server.camunda7ServerUrl + "]";
          case DUMMY -> "Dummy";
        };
        logger.info(serverDetails);
      }
    } catch (Exception e) {
      logger.error("Error during initialization : {}", e.getMessage());
    }
  }

  /**
   * Add an explicit server, by the API
   *
   * @param bpmnEngineConfiguration server to add in the list
   */
  public void addExplicitServer(BpmnServerDefinition bpmnEngineConfiguration) {
    allServers.add(bpmnEngineConfiguration);
  }

  public List<BpmnServerDefinition> getListServers() {
    return allServers;
  }

  /**
   * get a server by its name
   *
   * @param serverName serverName
   * @return the server, or null
   * @throws AutomatorException on any error
   */
  public BpmnEngineList.BpmnServerDefinition getByServerName(String serverName) throws AutomatorException {
    Optional<BpmnServerDefinition> first = allServers.stream().filter(t -> t.name.equals(serverName)).findFirst();
    return first.isPresent() ? first.get() : null;
  }

  /**
   * get a server by its type
   *
   * @param serverType type of server CAMUNDA 8 ? 7 ?
   * @return a server
   * @throws AutomatorException on any error
   */
  public BpmnEngineList.BpmnServerDefinition getByServerType(CamundaEngine serverType)  {
    Optional<BpmnServerDefinition> first = allServers.stream()
        .filter(t -> sameType(t.serverType, serverType))
        .findFirst();
    return first.isPresent() ? first.get() : null;
  }

  public boolean getLogDebug() {
    return configurationServersEngine.logDebug;
  }

  /* ******************************************************************** */
  /*                                                                      */
  /*  Different information in the YAML                                   */
  /*                                                                      */
  /* ******************************************************************** */

  /**
   * Explode the configuration serverConnections
   *
   * @return list of server definition
   * @throws AutomatorException any errors
   */
  private List<BpmnServerDefinition> getFromServersConnectionList() throws AutomatorException {
    // not possible to use a Stream: decode throw an exception
    List<BpmnServerDefinition> list = new ArrayList<>();
    int count = 0;
    for (String s : configurationServersEngine.serversConnection) {
      count++;
      if (s.isEmpty())
        continue;
      BpmnServerDefinition bpmnServerDefinition = decodeServerConnection(s, "Range in ConnectionString: #" + count);
      if (bpmnServerDefinition.serverType == null) {
        logger.error("Server Type can't be detected in string [{}]", s);
        continue;
      }

      list.add(bpmnServerDefinition);
    }
    return list;
  }

  /**
   * getFromServerList
   * in configuration, give a list of server.
   *
   * @return the list of available server
   * @throws AutomatorException in case of error
   */
  private List<BpmnServerDefinition> getFromServersList() throws AutomatorException {
    List<BpmnServerDefinition> serverList = new ArrayList<>();

    int count = 0;
    for (Map<String, Object> serverMap : configurationServersEngine.getServersList()) {
      count++;
      BpmnServerDefinition bpmnServerDefinition = new BpmnServerDefinition();
      bpmnServerDefinition.name = getString("name", serverMap, null, "ServerList #" + count, true);
      String contextLog = "ServerList #" + count + " Name [" + bpmnServerDefinition.name + "]";
      bpmnServerDefinition.workerMaxJobsActive = getInteger(CONF_WORKER_MAX_JOBS_ACTIVE, serverMap,
          DEFAULT_VALUE_MAX_JOBS_ACTIVE, contextLog);

      if (CONF_TYPE_V_CAMUNDA_7.equalsIgnoreCase(getString(CONF_TYPE, serverMap, null, contextLog, true))) {
        bpmnServerDefinition.serverType = CamundaEngine.CAMUNDA_7;
        bpmnServerDefinition.camunda7ServerUrl = getString(CONF_URL, serverMap, null, contextLog, true);
        if (bpmnServerDefinition.camunda7ServerUrl == null)
          throw new AutomatorException(
              "Incorrect Definition - [url] expected for [" + CONF_TYPE_V_CAMUNDA_7 + "] type " + contextLog);
      }


      if (CONF_TYPE_V_CAMUNDA_8.equalsIgnoreCase(getString(CONF_TYPE, serverMap, null, contextLog, true))) {
        bpmnServerDefinition.serverType = CamundaEngine.CAMUNDA_8;
        bpmnServerDefinition.zeebeGatewayAddress = getString(CONF_ZEEBE_GATEWAY_ADDRESS, serverMap, null, contextLog,
            true);
        bpmnServerDefinition.zeebeClientId = getString(CONF_ZEEBE_CLIENT_ID, serverMap, null, contextLog,
            false);
        bpmnServerDefinition.zeebeClientSecret = getString(CONF_ZEEBE_SECRET, serverMap, null, contextLog,
            false);
        bpmnServerDefinition.zeebeAudience = getString(CONF_ZEEBE_AUDIENCE, serverMap, ZEEBE_DEFAULT_AUDIENCE, contextLog, false);
        bpmnServerDefinition.authenticationUrl = getString(CONF_AUTHENTICATIONURL, serverMap, null, contextLog,
            false);

        bpmnServerDefinition.identityUrl = getString(CONF_IDENTITY_URL, serverMap, null, contextLog, false);
        bpmnServerDefinition.operateUrl = getString(CONF_OPERATE_URL, serverMap, null, contextLog, false);
        bpmnServerDefinition.operateUserName = getString(CONF_OPERATE_USER_NAME, serverMap, "Demo", contextLog, false);
        bpmnServerDefinition.operateUserPassword = getString(CONF_OPERATE_USER_PASSWORD, serverMap, "Demo", contextLog,
            false);
        bpmnServerDefinition.operateClientId = getString(CONF_OPERATE_CLIENT_ID, serverMap, null, contextLog, false);
        bpmnServerDefinition.operateClientSecret = getString(CONF_OPERATE_CLIENT_SECRET, serverMap, null, contextLog,
            false);
        bpmnServerDefinition.operateAudience = getString(CONF_OPERATE_AUDIENCE, serverMap, null, contextLog, false);

        bpmnServerDefinition.taskListUrl = getString(CONF_TASK_LIST_URL, serverMap, null, contextLog, false);
        bpmnServerDefinition.taskListUserName = getString(CONF_TASK_LIST_USER, serverMap, null, contextLog, false);
        bpmnServerDefinition.taskListUserPassword = getString(CONF_TASK_LIST_PASSWORD, serverMap, null, contextLog, false);
        bpmnServerDefinition.taskListClientId = getString(CONF_TASK_LIST_CLIENT_ID, serverMap, null, contextLog, false);
        bpmnServerDefinition.taskListClientSecret = getString(CONF_TASK_LIST_CLIENT_SECRET, serverMap, null, contextLog,
            false);
        bpmnServerDefinition.workerExecutionThreads = getInteger(CONF_WORKER_EXECUTION_THREADS, serverMap,
            DEFAULT_VALUE_EXECUTION_THREADS, contextLog);
        if (bpmnServerDefinition.zeebeGatewayAddress == null)
          throw new AutomatorException(
              "Incorrect Definition - [zeebeGatewayAddress] expected for [" + CONF_TYPE_V_CAMUNDA_8 + "] type");
      }


      if (CONF_TYPE_V_CAMUNDA_8_SAAS.equalsIgnoreCase(getString(CONF_TYPE, serverMap, null, contextLog, true))) {
        bpmnServerDefinition.serverType = CamundaEngine.CAMUNDA_8_SAAS;
        bpmnServerDefinition.zeebeSaasRegion = getString(CONF_ZEEBE_SAAS_REGION, serverMap, null, contextLog, true);
        bpmnServerDefinition.zeebeSaasClusterId = getString(CONF_ZEEBE_SAAS_CLUSTER_ID, serverMap, null, contextLog,
            true);
        bpmnServerDefinition.zeebeClientId = getString(CONF_ZEEBE_CLIENT_ID, serverMap, null, contextLog,
            true);
        bpmnServerDefinition.zeebeClientSecret = getString(CONF_ZEEBE_SECRET, serverMap, null, contextLog,
            true);
        bpmnServerDefinition.zeebeAudience = getString(CONF_ZEEBE_AUDIENCE, serverMap, ZEEBE_DEFAULT_AUDIENCE, contextLog, true);
        bpmnServerDefinition.authenticationUrl = getString(CONF_AUTHENTICATIONURL, serverMap, "https://login.cloud.camunda.io/oauth/token", contextLog,
            false);

        bpmnServerDefinition.workerExecutionThreads = getInteger(CONF_WORKER_EXECUTION_THREADS, serverMap,
            DEFAULT_VALUE_EXECUTION_THREADS, contextLog);
        bpmnServerDefinition.operateUserName = getString(CONF_OPERATE_USER_NAME, serverMap, null, contextLog, false);
        bpmnServerDefinition.operateUserPassword = getString(CONF_OPERATE_USER_PASSWORD, serverMap, null, contextLog,
            false);
        bpmnServerDefinition.operateUrl = getString(CONF_OPERATE_URL, serverMap, null, contextLog, false);
        bpmnServerDefinition.taskListUrl = getString(CONF_TASK_LIST_URL, serverMap, null, contextLog, false);
        bpmnServerDefinition.taskListClientId = getString(CONF_TASK_LIST_CLIENT_ID, serverMap, null, contextLog, false);
        bpmnServerDefinition.taskListClientSecret = getString(CONF_TASK_LIST_CLIENT_SECRET, serverMap, null, contextLog,false);

        if (bpmnServerDefinition.zeebeSaasRegion == null || bpmnServerDefinition.zeebeClientSecret == null
            || bpmnServerDefinition.zeebeSaasClusterId == null || bpmnServerDefinition.zeebeClientId == null)
          throw new AutomatorException(
              "Incorrect Definition - [zeebeCloudRegister],[zeebeCloudRegion], [zeebeClientSecret},[zeebeCloudClusterId],[zeebeCloudClientId]  expected for [Camunda8SaaS] type");
      }
      serverList.add(bpmnServerDefinition);
    }
    return serverList;
  }

  /**
   * DecodeServerConnection
   *
   * @param connectionString connection string
   * @return a ServerDefinition
   * @throws AutomatorException on any error
   */
  private BpmnServerDefinition decodeServerConnection(String connectionString, String contextLog)
      throws AutomatorException {
    StringTokenizer st = new StringTokenizer(connectionString, ",");
    BpmnServerDefinition bpmnServerDefinition = new BpmnServerDefinition();
    bpmnServerDefinition.name = (st.hasMoreTokens() ? st.nextToken() : null);
    try {
      bpmnServerDefinition.serverType = st.hasMoreTokens() ? CamundaEngine.valueOf(st.nextToken()) : null;
      if (CamundaEngine.CAMUNDA_7.equals(bpmnServerDefinition.serverType)) {
        bpmnServerDefinition.camunda7ServerUrl = (st.hasMoreTokens() ? st.nextToken() : null);

      } else if (CamundaEngine.CAMUNDA_8.equals(bpmnServerDefinition.serverType)) {
        bpmnServerDefinition.zeebeGatewayAddress = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.operateUrl = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.operateUserName = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.operateUserPassword = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.taskListUrl = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.workerExecutionThreads = (st.hasMoreTokens() ?
            parseInt(CONF_WORKER_EXECUTION_THREADS, st.nextToken(), DEFAULT_VALUE_EXECUTION_THREADS, contextLog) :
            null);
        bpmnServerDefinition.workerMaxJobsActive = (st.hasMoreTokens() ?
            parseInt(CONF_WORKER_MAX_JOBS_ACTIVE, st.nextToken(), DEFAULT_VALUE_MAX_JOBS_ACTIVE, contextLog) :
            null);

      } else if (CamundaEngine.CAMUNDA_8_SAAS.equals(bpmnServerDefinition.serverType)) {
        bpmnServerDefinition.zeebeSaasRegion = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.zeebeSaasClusterId = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.zeebeClientId = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.zeebeClientSecret = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.zeebeAudience = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.operateClientId = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.operateClientSecret = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.taskListClientId = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.taskListClientSecret = (st.hasMoreTokens() ? st.nextToken() : null);

        bpmnServerDefinition.workerExecutionThreads = (st.hasMoreTokens() ?
            parseInt(CONF_WORKER_EXECUTION_THREADS, st.nextToken(), DEFAULT_VALUE_EXECUTION_THREADS, contextLog) :
            null);
        bpmnServerDefinition.workerMaxJobsActive = (st.hasMoreTokens() ?
            parseInt(CONF_WORKER_MAX_JOBS_ACTIVE, st.nextToken(), DEFAULT_VALUE_MAX_JOBS_ACTIVE, contextLog) :
            null);
      }
      return bpmnServerDefinition;
    } catch (Exception e) {
      throw new AutomatorException("Can't decode string [" + connectionString + "] " + e.getMessage());
    }
  }

  /**
   * Get the list from the serverConfiguration. If the variable exist, then use the value. Easy to configure for K8
   *
   * @return list of BpmnServer
   */
  private List<BpmnServerDefinition> getFromServerConfiguration() {
    List<BpmnServerDefinition> list = new ArrayList<>();

    // get the direct list
    if (hasValue(configurationServersEngine.camunda7Url)) {
      BpmnServerDefinition camunda7 = new BpmnServerDefinition();
      camunda7.serverType = CamundaEngine.CAMUNDA_7;
      camunda7.name = configurationServersEngine.camunda7Name;
      camunda7.camunda7ServerUrl = configurationServersEngine.camunda7Url;
      camunda7.camunda7UserName = configurationServersEngine.camunda7UserName;
      camunda7.camunda7Password = configurationServersEngine.camunda7Password;

      camunda7.workerMaxJobsActive = parseInt("Camunda7." + CONF_WORKER_MAX_JOBS_ACTIVE,
          configurationServersEngine.C7WorkerMaxJobsActive, DEFAULT_VALUE_MAX_JOBS_ACTIVE, "");
      camunda7.workerExecutionThreads = parseInt("Camunda7." + CONF_WORKER_EXECUTION_THREADS,
          configurationServersEngine.C7WorkerMaxJobsActive, DEFAULT_VALUE_EXECUTION_THREADS, "");

      camunda7.workerMaxJobsActive = parseInt("Camunda7." + CONF_WORKER_MAX_JOBS_ACTIVE,
          configurationServersEngine.C7WorkerMaxJobsActive, DEFAULT_VALUE_MAX_JOBS_ACTIVE, "");
      list.add(camunda7);
      logger.info("Configuration: Camunda7 Name[{}] url[{}] MaxJobsActive[{}]", camunda7.name,
          camunda7.camunda7ServerUrl, camunda7.workerMaxJobsActive);
    }
    if (hasValue(configurationServersEngine.zeebeGatewayAddress)) {
      BpmnServerDefinition camunda8 = new BpmnServerDefinition();
      camunda8.serverType = CamundaEngine.CAMUNDA_8;
      camunda8.name = configurationServersEngine.zeebeName;
      camunda8.zeebeGatewayAddress = configurationServersEngine.zeebeGatewayAddress;
      camunda8.workerExecutionThreads = parseInt("Camunda8." + CONF_WORKER_EXECUTION_THREADS,
          configurationServersEngine.zeebeWorkerExecutionThreads, DEFAULT_VALUE_EXECUTION_THREADS, "");
      camunda8.workerMaxJobsActive = parseInt("Camunda8." + CONF_WORKER_MAX_JOBS_ACTIVE,
          configurationServersEngine.zeebeWorkerMaxJobsActive, DEFAULT_VALUE_MAX_JOBS_ACTIVE, "");
      camunda8.operateUrl = configurationServersEngine.zeebeOperateUrl;
      camunda8.operateUserName = configurationServersEngine.zeebeOperateUserName;
      camunda8.operateUserPassword = configurationServersEngine.zeebeOperateUserPassword;
      camunda8.taskListUrl = configurationServersEngine.zeebeTaskListUrl;
      list.add(camunda8);
      logger.info(
          "Configuration: Camunda8 Name[{}] zeebeGateway[{}] MaxJobsActive[{}] WorkerThreads[{}] " + "OperateURL[{}]",
          camunda8.name, camunda8.camunda7ServerUrl, camunda8.workerMaxJobsActive, camunda8.workerExecutionThreads,
          camunda8.operateUrl);

    }
    if (hasValue(configurationServersEngine.zeebeSaasClusterId)) {
      BpmnServerDefinition camunda8 = new BpmnServerDefinition();
      camunda8.serverType = CamundaEngine.CAMUNDA_8_SAAS;
      camunda8.name = configurationServersEngine.zeebeName;
      camunda8.zeebeSaasRegion = configurationServersEngine.zeebeSaasRegion;
      camunda8.zeebeSaasClusterId = configurationServersEngine.zeebeSaasClusterId;
      camunda8.zeebeClientId = configurationServersEngine.zeebeSaasClientId;
      camunda8.zeebeClientSecret = configurationServersEngine.zeebeSaasClientSecret;
      camunda8.authenticationUrl = configurationServersEngine.zeebeSaasOAuthUrl;
      camunda8.zeebeAudience = configurationServersEngine.zeebeSaasAudience;
      camunda8.operateUrl = configurationServersEngine.zeebeOperateUrl;
      camunda8.operateUserName = configurationServersEngine.zeebeOperateUserName;
      camunda8.operateUserPassword = configurationServersEngine.zeebeOperateUserPassword;
      camunda8.taskListUrl = configurationServersEngine.zeebeTaskListUrl;
      list.add(camunda8);

    }
    return list;
  }




  /* ******************************************************************** */
  /*                                                                      */
  /*  Toolbox                                                             */
  /*                                                                      */
  /* ******************************************************************** */

  private String getString(String name,
                           Map<String, Object> recordData,
                           String defaultValue,
                           String contextLog,
                           boolean isMandatory) {
    try {
      if (!recordData.containsKey(name)) {
        if (isMandatory) {
          if (defaultValue == null)
            logger.error("{}Variable [{}] not defined in {}", contextLog, name, contextLog);
          else
            logger.info("{} Variable [{}] not defined in {}", contextLog, name, contextLog);
        }
        return defaultValue;
      }
      return (String) recordData.get(name);
    } catch (Exception e) {
      logger.error("{} Variable [{}] {} bad definition {}", contextLog, name, contextLog, e.getMessage());
      return defaultValue;
    }
  }

  private Integer getInteger(String name, Map<String, Object> recordData, Integer defaultValue, String contextLog) {
    try {
      if (!recordData.containsKey(name)) {
        if (defaultValue == null)
          logger.error("Variable [{}] not defined in {}", name, contextLog);
        else
          logger.info("Variable [{}] not defined in {}", name, contextLog);
        return defaultValue;
      }
      return (Integer) recordData.get(name);
    } catch (Exception e) {
      logger.error("Variable [{}] {} bad definition {}", name, contextLog, e.getMessage());
      return defaultValue;
    }
  }

  private int parseInt(String label, String value, int defaultValue, String contextLog) {
    try {
      if (value.equals("''"))
        return defaultValue;
      return Integer.parseInt(value);
    } catch (Exception e) {
      logger.error("Can't parse value [{}] at [{}] {}", value, label, contextLog);
      return defaultValue;
    }
  }

  private boolean hasValue(String value) {
    if (value == null)
      return false;
    if (value.equals("''"))
      return false;
    return !value.trim().isEmpty();
  }

  /**
   * Compare type : CAMUNDA_8 and CAMUNDA_8_SAAS are considered as equals
   *
   * @param type1 type one to compare
   * @param type2 type two to compare
   * @return true if types are identical
   */
  private boolean sameType(CamundaEngine type1, CamundaEngine type2) {
    if (type1.equals(CamundaEngine.CAMUNDA_8_SAAS))
      type1 = CamundaEngine.CAMUNDA_8;
    if (type2.equals(CamundaEngine.CAMUNDA_8_SAAS))
      type2 = CamundaEngine.CAMUNDA_8;
    return type1.equals(type2);
  }

  public enum CamundaEngine {CAMUNDA_7, CAMUNDA_8, CAMUNDA_8_SAAS, DUMMY}

  public static class BpmnServerDefinition {
    public String name;

    public CamundaEngine serverType;

    /**
     * My Zeebe Address
     */
    public String zeebeGatewayAddress;
    public String zeebeSecurityPlainText;

    /**
     * SaaS Zeebe
     */
    public String zeebeSaasRegion;
    public String zeebeSaasClusterId;
    public String zeebeClientId;
    public String zeebeClientSecret;
    public String zeebeAudience;

    public String identityUrl;
    /**
     * Connection to Operate
     */
    public String operateUserName;
    public String operateUserPassword;
    public String operateUrl;

    // something like "http://localhost:18080/auth/realms/camunda-platform/protocol/openid-connect/token"
    public String authenticationUrl;
    public String operateClientId;
    public String operateClientSecret;
    public String operateAudience;

    public String taskListUrl;
    public String taskListUserName;
    public String taskListUserPassword;
    public String taskListClientId;
    public String taskListClientSecret;

    /**
     * Camunda 7
     */
    public String camunda7ServerUrl;
    public String camunda7UserName;
    public String camunda7Password;

    /**
     * Common Camunda 7 and Camunda8
     */
    public Integer workerExecutionThreads = Integer.valueOf(DEFAULT_VALUE_EXECUTION_THREADS);
    public Integer workerMaxJobsActive = Integer.valueOf(DEFAULT_VALUE_MAX_JOBS_ACTIVE);

    /**
     * return true if the definition have an Operate connection valid
     *
     * @return true is Operate is required
     */
    public boolean isOperate() {
      return !(operateUrl == null || operateUrl.isEmpty());
    }

    public boolean isTaskList() {
      return !(taskListUrl == null || taskListUrl.isEmpty());
    }

    public boolean isAuthenticationUrl() {
      return !(authenticationUrl == null || authenticationUrl.isEmpty());
    }

    public String getSynthesis() {
      String synthesis = serverType.name();
      if (serverType.equals(CamundaEngine.CAMUNDA_7)) {
        synthesis += " url[" + camunda7ServerUrl + "] userName[" + camunda7UserName + "]";
      }
      if (serverType.equals(CamundaEngine.CAMUNDA_8)) {
        synthesis += " address[" + zeebeGatewayAddress + "] workerThread[" + workerExecutionThreads + "] MaxJobActive["
            + workerMaxJobsActive + "]";
      }
      if (serverType.equals(CamundaEngine.CAMUNDA_8_SAAS)) {
        synthesis += " clientId[" + zeebeClientId + "] workerThread[" + workerExecutionThreads + "] MaxJobActive["
            + workerMaxJobsActive + "]";
      }
      return synthesis;
    }
  }

}
