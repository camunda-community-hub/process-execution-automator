package automatorapi;

import io.camunda.tasklist.CamundaTaskListClient;
import io.camunda.tasklist.CamundaTaskListClientBuilder;
import io.camunda.tasklist.dto.Pagination;
import io.camunda.tasklist.dto.TaskList;
import io.camunda.tasklist.dto.TaskState;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestConnectionTaskList {

    static Logger logger = LoggerFactory.getLogger(TestConnectionTaskList.class);

    @Test
    public void init() {
        logger.info("TestConnectionTaskList");

        try {
            String taskListClientId = "tasklist";
            String taskListClientSecret = "yrsX5qkt5z";
            String taskListUrl = "http://localhost:8082";

            CamundaTaskListClientBuilder taskListBuilder = CamundaTaskListClient.builder();
            // ---------------------------- Camunda Saas
            String taskListUserName = "demo";
            String taskListUserPassword = "demo";
            String taskListKeycloakUrl = "http://35.237.139.124:8080/auth/realms/camunda-platform/protocol/openid-connect/token";


            taskListBuilder.taskListUrl(taskListUrl)
                    .selfManagedAuthentication(taskListClientId, taskListClientSecret,
                            taskListKeycloakUrl);
            // taskListBuilder.zeebeClient(engineCamunda8.getZeebeClient());
            // taskListBuilder.useZeebeUserTasks();
            CamundaTaskListClient taskClient = taskListBuilder.build();

            // Check the connection
            TaskList taskList = taskClient.getTasks(false, TaskState.CREATED, false, new Pagination().setPageSize(1));
            logger.info("TaskList: list of tasks: {}", taskList);

        } catch (Exception e) {
            logger.error("Can't connect to Server{}", e.getMessage(), e);
        }
    }
}