package org.camunda.automator.definition;

public class ScenarioVerificationVariable implements ScenarioVerificationBasic {
    public String name;
    public Object value;

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public String getSynthesis() {
        return "VariableCheck [" + name + "]=[" + value + "]";
    }

    public String getTypeVerification() {
        return "VARIABLE";
    }
}
