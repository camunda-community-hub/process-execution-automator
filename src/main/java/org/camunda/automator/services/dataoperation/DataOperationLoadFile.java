package org.camunda.automator.services.dataoperation;

import org.camunda.automator.definition.ScenarioDeployment;
import org.camunda.automator.definition.ScenarioTool;
import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunScenario;
import org.springframework.stereotype.Component;

import org.camunda.bpm.engine.variable.value.FileValue;
import org.camunda.bpm.engine.variable.Variables;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Component
public class DataOperationLoadFile extends DataOperation {
  @Override
  public boolean match(String value) {
    return matchFunction(value, "loadfile");
  }

  @Override
  public Object execute(String value, RunScenario runScenario) throws AutomatorException {
    File fileLoad = loadFile(value,runScenario);
    if (fileLoad==null)
      return null;

    FileValue typedFileValue = Variables
        .fileValue(fileLoad.getName())
        .file(fileLoad)
        // .mimeType("text/plain")
        // .encoding("UTF-8")
        .create();
    return typedFileValue;

  }


  private File loadFile(String value, RunScenario runScenario)  throws AutomatorException {
    List<String> args = extractArgument(value, true);

    File fileToLoad = null;
    if (args.size() != 1) {
      throw new AutomatorException("Bad argument: loadfile(<fileName>)");
    }
    String formatArgs = args.get(0);

    return ScenarioTool.loadFile( formatArgs, runScenario);

  }
}
