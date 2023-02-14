package automatorapi;

import org.camunda.automator.AutomatorAPI;
import org.camunda.automator.bpmnengine.BpmnEngineConfiguration;
import org.camunda.automator.definition.ScnExecution;
import org.camunda.automator.definition.ScnHead;
import org.camunda.automator.definition.ScnStep;
import org.camunda.automator.engine.RunParameters;
import org.camunda.automator.engine.ScnRunResult;
import org.junit.jupiter.api.Test;

import java.io.File;

public class SimpleUserTask {

  @Test
  public void SimpleUserTaskAPI() {
    AutomatorAPI automatorApi = AutomatorAPI.getInstance();

    ScnHead scenario = automatorApi.createScenario()
        .setProcessId("SimpleUserTask")
        .setName("Simple User Task");

    ScnExecution execution = ScnExecution.createExecution(scenario) //
        .setName("dummy") // name
        .setNumberProcessInstances(2); // number of process instance to generate



    execution.addStep(ScnStep.createStepCreate(execution, "StartEvent_Review"));
    RunParameters runParameters = new RunParameters();
    runParameters.logLevel = RunParameters.LOGLEVEL.DEBUG;

    BpmnEngineConfiguration engineConfiguration = BpmnEngineConfiguration.getDummy();
    ScnRunResult scenarioExecutionResult = automatorApi.executeScenario(engineConfiguration, runParameters, scenario);
    assert (scenarioExecutionResult.isSuccess());
  }

  @Test
  public void SimpleUserTaskScenario() {
    try {
      AutomatorAPI automatorApi = AutomatorAPI.getInstance();
      File userTaskFile = new File("./test/resources/simpleusertask/AutomatorSimpleUserTask.json");
      ScnHead scenario = automatorApi.loadFromFile(userTaskFile);
    } catch (Exception e) {
      assert (false);
    }

  }
}
