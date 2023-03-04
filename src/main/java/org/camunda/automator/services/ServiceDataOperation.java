package org.camunda.automator.services;

import org.camunda.automator.engine.RunParameters;
import org.camunda.automator.engine.RunScenario;
import org.camunda.automator.services.dataoperation.DataOperation;
import org.camunda.automator.engine.AutomatorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceDataOperation {

  @Autowired
  private List<DataOperation> listDataOperation;

  /**
   * please use the getInstance()
   */
  private ServiceDataOperation() {
  }

  public Object execute(String value, RunScenario runScenario) throws AutomatorException {
    for (DataOperation dataOperation : listDataOperation) {
      if (dataOperation.match(value))
        return dataOperation.execute(value, runScenario);
    }
    throw new AutomatorException("No operation for ["+value+"]");
  }



}
