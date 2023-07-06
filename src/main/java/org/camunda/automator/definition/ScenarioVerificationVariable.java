package org.camunda.automator.definition;

public class ScenarioVerificationVariable implements ScenarioVerificationBasic {
  public String name;
  public Object value;

  public String getSynthesis() {
    return "VariableCheck [" + name + "]=[" + value + "]";
  }

}
