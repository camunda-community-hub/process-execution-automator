/**
 * Create an empty scenario.
 * The scenario can be created from scratch by the caller
 * It can be loaded from a JSON file too.
 *
 * @return
 */
package org.camunda.automator.bpmnengine;

import org.camunda.automator.engine.AutomatorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

@Configuration
@PropertySource("classpath:application.yaml")
public class BpmnEngineConfiguration {

  public static class BpmnServerDefinition {
    public String name;
    public CamundaEngine camundaEngine;

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

    /**
     * Connection to Operate
     */
    public String operateUserName;
    public String operateUserPassword;
    public String operateUrl;
    public String tasklistUrl;

    /**
     * Camunda 7
     */
    public String serverUrl;
  }

  @Value("${automator.logDebug:false}")
  public boolean logDebug = false;

  @Value("#{'${automator.serversconnection}'.split(';')}")
  public List<String> serversConnection;
  public List<BpmnServerDefinition> servers;

  public List<Map<String, Object>> serversMap;

  public enum CamundaEngine {CAMUNDA_7, CAMUNDA_8, CAMUNDA_8_SAAS, DUMMY}

  public BpmnEngineConfiguration.BpmnServerDefinition getByServerName(String serverName) throws AutomatorException  {
    // decode the serverConnections
    List<BpmnServerDefinition> listFromConnection = decodeListServersConnection();
    List<BpmnServerDefinition> allServers = new ArrayList<>();
    if (listFromConnection != null)
      allServers.addAll(listFromConnection);
    if (servers != null)
      allServers.addAll(servers);

    for (BpmnEngineConfiguration.BpmnServerDefinition serverIndex : allServers) {
      if (serverName.equals(serverIndex.name))
        return serverIndex;
    }
    return null;
  }

  public List<BpmnServerDefinition> decodeListServersConnection() throws AutomatorException  {
    if (serversConnection == null)
      return null;

    // not possible to use a Stream: decode throw an exception
    List<BpmnServerDefinition> list = new ArrayList<>();
    for (String s : serversConnection) {
      BpmnServerDefinition bpmnServerDefinition = decodeServerConnection(s);
      list.add(bpmnServerDefinition);
    }
    return list;
  }

  public BpmnServerDefinition decodeServerConnection(String connectionString) throws AutomatorException {
    StringTokenizer st = new StringTokenizer(connectionString, ",");
    BpmnServerDefinition bpmnServerDefinition = new BpmnServerDefinition();
    bpmnServerDefinition.name = (st.hasMoreTokens() ? st.nextToken() : null);
    try {
      bpmnServerDefinition.camundaEngine = (st.hasMoreTokens() ? CamundaEngine.valueOf(st.nextToken()) : null);
      if (CamundaEngine.CAMUNDA_7.equals(bpmnServerDefinition.camundaEngine)) {
        bpmnServerDefinition.serverUrl = (st.hasMoreTokens() ? st.nextToken() : null);

      } else if (CamundaEngine.CAMUNDA_8.equals(bpmnServerDefinition.camundaEngine)) {
        bpmnServerDefinition.zeebeGatewayAddress = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.operateUrl = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.operateUserName = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.operateUserPassword = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.tasklistUrl = (st.hasMoreTokens() ? st.nextToken() : null);

      } else if (CamundaEngine.CAMUNDA_8_SAAS.equals(bpmnServerDefinition.camundaEngine)) {
        bpmnServerDefinition.zeebeCloudRegister = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.zeebeCloudRegion = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.zeebeCloudClusterId = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.zeebeCloudClientId = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.clientSecret = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.operateUrl = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.operateUserName = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.operateUserPassword = (st.hasMoreTokens() ? st.nextToken() : null);
        bpmnServerDefinition.tasklistUrl = (st.hasMoreTokens() ? st.nextToken() : null);
      }
      return bpmnServerDefinition;
    } catch( Exception e){
      throw new AutomatorException("Can't decode string ["+connectionString+"] "+e.getMessage());
    }
  }

}
