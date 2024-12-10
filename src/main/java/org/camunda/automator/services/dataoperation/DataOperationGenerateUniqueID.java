package org.camunda.automator.services.dataoperation;

import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunScenario;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataOperationGenerateUniqueID extends DataOperation {

    private final long baseTimer = System.currentTimeMillis();
    private final Map<String, Long> mapUniqueId = new HashMap<>();

    @Override
    public String getName() {
        return "GenerateUniqueId";
    }

    @Override
    public boolean match(String value) {
        return matchFunction(value, "generateuniqueid");
    }

    @Override
    public String getHelp() {
        return "generateuniqueid(<prefix-String>)";
    }

    @Override
    public Object execute(String value, RunScenario runScenario, int index) throws AutomatorException {
        List<String> args = extractArgument(value, true);
        String prefix = args.get(0);
        if (prefix == null)
            prefix = "default";

        Long uniqueId = mapUniqueId.getOrDefault(prefix, 0L);
        uniqueId++;
        mapUniqueId.put(prefix, uniqueId);
        return index + "-" + uniqueId + "-" + baseTimer;

    }
}
