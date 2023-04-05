package automatorapi;

import org.camunda.automator.AutomatorAPI;
import org.camunda.automator.bpmnengine.BpmnEngineConfiguration;
import org.camunda.automator.bpmnengine.BpmnEngineConfigurationInstance;
import org.camunda.automator.definition.ScenarioExecution;
import org.camunda.automator.definition.Scenario;
import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.RunParameters;
import org.camunda.automator.engine.RunResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

public class SimpleUserTask {

  @Autowired
  AutomatorAPI automatorApi;

  @Test
  public void SimpleUserTaskAPI() {

    Scenario scenario = automatorApi.createScenario()
        .setProcessId("SimpleUserTask")
        .setName("Simple User Task");

    ScenarioExecution execution = ScenarioExecution.createExecution(scenario) //
        .setName("dummy") // name
        .setNumberProcessInstances(2); // number of process instance to generate



    execution.addStep(ScenarioStep.createStepCreate(execution, "StartEvent_Review"));
    RunParameters runParameters = new RunParameters();
    runParameters.logLevel = RunParameters.LOGLEVEL.DEBUG;

    BpmnEngineConfiguration engineConfiguration = BpmnEngineConfigurationInstance.getDummy();
    RunResult scenarioExecutionResult = automatorApi.executeScenario(automatorApi.getBpmnEngine(engineConfiguration, engineConfiguration.servers.get(0)), runParameters, scenario);
    assert (scenarioExecutionResult.isSuccess());
  }

  @Test
  public void SimpleUserTaskScenario() {
    try {
      File userTaskFile = new File("./test/resources/simpleusertask/AutomatorSimpleUserTask.json");
      Scenario scenario = automatorApi.loadFromFile(userTaskFile);
    } catch (Exception e) {
      assert (false);
    }

  }
}
