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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

@Component
@Configuration
@ConfigurationProperties(prefix = "automator")
@PropertySource("classpath:application.yaml")
@EnableConfigurationProperties
public class ConfigurationBpmEngine {
  static Logger logger = LoggerFactory.getLogger(ConfigurationBpmEngine.class);
  @Value("${logDebug:false}")
  public boolean logDebug = false;

  @Value("#{'${serversConnection}'.split(';')}")
  public List<String> serversConnection;

  @Value("${serversList}")
  public List<Object> serversList;

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
        String serverDetails = "Server Type[" + server.serverType + "] " + switch (server.serverType) {
          case CAMUNDA_8 -> "ZeebeadressGateway [" + server.zeebeGatewayAddress + "]";
          case CAMUNDA_8_SAAS -> "ZeebeClientId [" + server.zeebeCloudClientId + "] ClusterId["
              + server.zeebeCloudClusterId + "] RegionId[" + server.zeebeCloudRegion + "]";
          case CAMUNDA_7 -> "Camunda7URL [" + server.camunda7ServerUrl + "]";
          case DUMMY -> "Dummy";
        };
        logger.info(serverDetails);
      }
    } catch (Exception e) {
      logger.error("Error during initialization");
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
  public ConfigurationBpmEngine.BpmnServerDefinition getByServerName(String serverName) throws AutomatorException {
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
  public ConfigurationBpmEngine.BpmnServerDefinition getByServerType(CamundaEngine serverType)
      throws AutomatorException {
    Optional<BpmnServerDefinition> first = allServers.stream()
        .filter(t -> sameType(t.serverType, serverType))
        .findFirst();
    return first.isPresent() ? first.get() : null;
  }




  /* ******************************************************************** */
  /*                                                                      */
  /*  Different information in the YAML                                   */
  /*                                                                      */
  /* ******************************************************************** */

  /**
   * Explode the configuration serverConnections
   *
   * @return
   * @throws AutomatorException
   */
  private List<BpmnServerDefinition> getFromServersConnectionList() throws AutomatorException {
    // not possible to use a Stream: decode throw an exception
    List<BpmnServerDefinition> list = new ArrayList<>();
    for (String s : serversConnection) {
      if (s.isEmpty())
        continue;
      BpmnServerDefinition bpmnServerDefinition = decodeServerConnection(s);
      if (bpmnServerDefinition.serverType == null) {
        logger.error("Server Type can't be detected in string [{}]", s);
        continue;
      }

      list.add(bpmnServerDefinition);
    }
    return list;
  }

  private List<BpmnServerDefinition> getFromServersList() throws AutomatorException {
    for (Object server : serversList) {
      // TODO : Explode this source
    }
    return Collections.emptyList();
  }

  /**
   * DecodeServerConnection
   *
   * @param connectionString connection string
   * @return a ServerDefinition
   * @throws AutomatorException on any error
   */
  private BpmnServerDefinition decodeServerConnection(String connectionString) throws AutomatorException {
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

      } else if (CamundaEngine.CAMUNDA_8_SAAS.equals(bpmnServerDefinition.serverType)) {
        bpmnServerDefinition.zeebeCloudRegister = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.zeebeCloudRegion = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.zeebeCloudClusterId = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.zeebeCloudClientId = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.clientSecret = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.operateUrl = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.operateUserName = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.operateUserPassword = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.taskListUrl = (st.hasMoreTokens() ? st.nextToken() : null);
      }
      return bpmnServerDefinition;
    } catch (Exception e) {
      throw new AutomatorException("Can't decode string [" + connectionString + "] " + e.getMessage());
    }
  }

  /**
   * Get the list from the serverConfiguration Explode the variable
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

      camunda7.workerMaxJobsActive = parseInt("Camunda7.workerMaxJobsActive",
          configurationServersEngine.C7WorkerMaxJobsActive, -1);
      list.add(camunda7);
      logger.info("Configuration: Camunda7 Name[{}] url[{}] MaxJobsActive[{}]", camunda7.name,
          camunda7.camunda7ServerUrl, camunda7.workerMaxJobsActive);
    }
    if (hasValue(configurationServersEngine.zeebeGatewayAddress)) {
      BpmnServerDefinition camunda8 = new BpmnServerDefinition();
      camunda8.serverType = CamundaEngine.CAMUNDA_8;
      camunda8.name = configurationServersEngine.zeebeName;
      camunda8.zeebeGatewayAddress = configurationServersEngine.zeebeGatewayAddress;
      camunda8.workerExecutionThreads = parseInt("Camunda8.workerExecutionThreads",
          configurationServersEngine.workerExecutionThreads, 101);
      camunda8.workerMaxJobsActive = parseInt("Camunda8.workerMaxJobsActive",
          configurationServersEngine.C8WorkerMaxJobsActive, -1);
      camunda8.operateUrl = configurationServersEngine.operateUrl;
      camunda8.operateUserName = configurationServersEngine.operateUserName;
      camunda8.operateUserPassword = configurationServersEngine.operateUserPassword;
      camunda8.taskListUrl = configurationServersEngine.taskListUrl;
      list.add(camunda8);
      logger.info(
          "Configuration: Camunda8 Name[{}] zeebeGateway[{}] MaxJobsActive[{}] WorkerThreads[{}] " + "OperateURL[{}]",
          camunda8.name, camunda8.camunda7ServerUrl, camunda8.workerMaxJobsActive, camunda8.workerExecutionThreads,
          camunda8.operateUrl);

    }
    if (hasValue(configurationServersEngine.zeebeCloudRegister)) {
      BpmnServerDefinition camunda8 = new BpmnServerDefinition();

      camunda8.zeebeCloudRegister = configurationServersEngine.zeebeCloudRegister;
      camunda8.zeebeCloudRegion = configurationServersEngine.zeebeCloudRegion;
      camunda8.zeebeCloudClusterId = configurationServersEngine.zeebeCloudClusterId;
      camunda8.zeebeCloudClientId = configurationServersEngine.zeebeCloudClientId;
      camunda8.clientSecret = configurationServersEngine.clientSecret;
      camunda8.operateUrl = configurationServersEngine.operateUrl;
      camunda8.operateUserName = configurationServersEngine.operateUserName;
      camunda8.operateUserPassword = configurationServersEngine.operateUserPassword;
      camunda8.taskListUrl = configurationServersEngine.taskListUrl;
      list.add(camunda8);

    }
    return list;
  }




  /* ******************************************************************** */
  /*                                                                      */
  /*  Toolbox                                                             */
  /*                                                                      */
  /* ******************************************************************** */

  private int parseInt(String label, String value, int defaultValue) {
    try {
      if (value.equals("''"))
        return defaultValue;
      return Integer.parseInt(value);
    } catch (Exception e) {
      logger.error("Can't parse value [{}] at [{}]", value, label);
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
    public String zeebeCloudRegister;
    public String zeebeCloudRegion;
    public String zeebeCloudClusterId;
    public String zeebeCloudClientId;
    public String clientSecret;

    public Integer workerExecutionThreads = Integer.valueOf(100);
    public Integer workerMaxJobsActive = Integer.valueOf(-1);

    /**
     * Connection to Operate
     */
    public String operateUserName;
    public String operateUserPassword;
    public String operateUrl;
    public String taskListUrl;

    /**
     * Camunda 7
     */
    public String camunda7ServerUrl;
    public String camunda7UserName;
    public String camunda7Password;
  }

}
