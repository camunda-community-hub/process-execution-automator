package org.camunda.automator.services.dataoperation;

import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataOperationGenerateList extends DataOperation {

  Logger logger = LoggerFactory.getLogger(DataOperationGenerateList.class);

  @Override
  public String getName() {
    return "GenerateList";
  }

  @Override
  public boolean match(String value) {
    return matchFunction(value, "generaterandomlist");
  }

  @Override
  public String getHelp() {
    return "generaterandomlist(<sizeOfTheList-integer>)";
  }

  @Override
  public Object execute(String value, RunScenario runScenario, int index) throws AutomatorException {

    List<String> args = extractArgument(value, true);
    List<String> listValues = new ArrayList<>();
    try {
      Integer sizeList = Integer.valueOf(args.get(0));
      for (int i = 0; i < sizeList; i++) {
        listValues.add("I" + i);
      }
    } catch (Exception e) {
      throw new AutomatorException(
          "can't generate a list: second parameters must be a Integer[" + args + "] : " + e.getMessage());

    }
    return listValues;
  }
}
