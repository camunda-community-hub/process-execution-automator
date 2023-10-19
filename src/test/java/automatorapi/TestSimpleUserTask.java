package automatorapi;

import org.camunda.automator.AutomatorAPI;
import org.camunda.automator.bpmnengine.BpmnEngine;
import org.camunda.automator.bpmnengine.BpmnEngineConfigurationInstance;
import org.camunda.automator.configuration.BpmnEngineList;
import org.camunda.automator.definition.Scenario;
import org.camunda.automator.definition.ScenarioExecution;
import org.camunda.automator.definition.ScenarioStep;
import org.camunda.automator.engine.RunParameters;
import org.camunda.automator.engine.RunResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import java.io.File;

public class TestSimpleUserTask {

  @Autowired
  AutomatorAPI automatorApi;

  @Test
  public void SimpleUserTaskAPI() {
    if (automatorApi==null) {
      // SpringBoot didn't provide the object
      assert(true);
      return;
    }

    Scenario scenario = automatorApi.createScenario().setProcessId("SimpleUserTask").setName("Simple User Task");

    ScenarioExecution execution = ScenarioExecution.createExecution(scenario) //
        .setName("dummy") // name
        .setNumberProcessInstances(2); // number of process instance to generate

    execution.addStep(ScenarioStep.createStepCreate(execution, "StartEvent_Review"));
    RunParameters runParameters = new RunParameters();
    runParameters.setLogLevel( RunParameters.LOGLEVEL.DEBUG);
    try {

      BpmnEngineList engineConfiguration = BpmnEngineConfigurationInstance.getDummy();
      BpmnEngine bpmnEngine = automatorApi.getBpmnEngine(engineConfiguration.getListServers().get(0),true);

      RunResult scenarioExecutionResult = automatorApi.executeScenario(bpmnEngine, runParameters, scenario);
      assert (scenarioExecutionResult.isSuccess());
    } catch (Exception e) {

      assert (false);
    }
  }

  @Test
  public void SimpleUserTaskScenario() {
    try {
      if (automatorApi==null) {
        // SpringBoot didn't provide the object
        assert(true);
        return;
      }
      File userTaskFile = new File("./test/resources/simpleusertask/AutomatorSimpleUserTask.json");
      Scenario scenario = automatorApi.loadFromFile(userTaskFile);
      assert(scenario!=null);
    } catch (Exception e) {
      assert (false);
    }

  }
}
