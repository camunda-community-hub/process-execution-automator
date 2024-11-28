package org.camunda.automator.services;

import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunScenario;
import org.camunda.automator.services.dataoperation.DataOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ServiceDataOperation {
    static Logger logger = LoggerFactory.getLogger(ServiceDataOperation.class);

    @Autowired
    private List<DataOperation> listDataOperation;

    /**
     * please use the getInstance()
     */
    private ServiceDataOperation() {
    }

    /**
     * Execute the DataOperation
     *
     * @param value       value to process
     * @param runScenario scenario to get information
     * @param context     give context in the exception in case of error
     * @param index       when multiple worker does the same operation, this is the index
     * @return the value calculated
     * @throws AutomatorException in case of error
     */
    public Object execute(String value, RunScenario runScenario, String context, int index) throws AutomatorException {
        for (DataOperation dataOperation : listDataOperation) {
            if (dataOperation.match(value)) {
                if (runScenario.getRunParameters().showLevelDebug())
                    logger.info("Execute {} value[{}]", dataOperation.getName(), value);
                return dataOperation.execute(value, runScenario, index);
            }
        }

        String helpOperations = listDataOperation.stream().map(DataOperation::getHelp).collect(Collectors.joining(", "));

        throw new AutomatorException(context + "No operation for [" + value + "] - operationExpected " + helpOperations);
    }

}
