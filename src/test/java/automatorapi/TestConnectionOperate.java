package automatorapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.operate.CamundaOperateClient;
import io.camunda.operate.CamundaOperateClientConfiguration;
import io.camunda.operate.auth.JwtAuthentication;
import io.camunda.operate.auth.JwtCredential;
import io.camunda.operate.model.FlowNodeInstance;
import io.camunda.operate.search.FlowNodeInstanceFilter;
import io.camunda.operate.search.SearchQuery;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.camunda.automator.bpmnengine.camunda8.LocalTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.URL;
import java.util.List;

public class TestConnectionOperate {

    static Logger logger = LoggerFactory.getLogger(LocalTest.class);

    @Test
    public void init() {
        logger.info("LocalTest soon");

        try {
            String clientId = "operate";
            String clientSecret = "XhB0HZub9B";

            String audience = "operate-api";
            String scope = ""; // can be omitted if not required
            URL operateUrl = URI.create("http://localhost:8081").toURL();
            URL authUrl =
                    URI.create(
                                    "http://35.237.139.124:8080/auth/realms/camunda-platform/protocol/openid-connect/token")
                            .toURL();

            JwtCredential credentials =
                    new JwtCredential(clientId, clientSecret, audience, authUrl, scope);
            ObjectMapper objectMapper = new ObjectMapper();
            JwtAuthentication authentication = new JwtAuthentication(credentials);
            CamundaOperateClientConfiguration configuration =
                    new CamundaOperateClientConfiguration(
                            authentication, operateUrl, objectMapper, HttpClients.createDefault());
            CamundaOperateClient operateClient = new CamundaOperateClient(configuration);


            // Ok, fine, now search
            FlowNodeInstanceFilter flownodeFilter = FlowNodeInstanceFilter.builder()
                    .processInstanceKey(Long.valueOf(3444))
                    .build();

            SearchQuery flowNodeQuery = new SearchQuery.Builder().filter(flownodeFilter).size(100).build();
            // Operate client does not support the multithreading very well (execption in jackson library)
            List<FlowNodeInstance> flowNodes= operateClient.searchFlowNodeInstances(flowNodeQuery);
            logger.info("LocalTest: flowNodes {}", flowNodes);


        } catch (Exception e) {
            logger.error("LocalTest: error during executing query {}", e.getMessage(), e);
        }
    }
}
