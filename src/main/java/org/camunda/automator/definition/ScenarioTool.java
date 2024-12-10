package org.camunda.automator.definition;

import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunScenario;

import java.io.File;
import java.io.IOException;

public class ScenarioTool {

    public static File loadFile(String fileName, RunScenario runScenario) throws AutomatorException {
        File file = new File(fileName);
        if (file.exists())
            return file;

        // maybe the file is present under the scenario path?
        if (runScenario.getScenario().getScenarioFile() != null) {
            File pathScenario = runScenario.getScenario().getScenarioFile().getParentFile();
            file = new File(pathScenario + "/" + fileName);
            if (file.exists())
                return file;
        }
        String currentPath = null;

        try {
            currentPath = new File(".").getCanonicalPath();
        } catch (IOException e) {
        }

        throw new AutomatorException("File [" + fileName + "] does not exist - current path =[" + currentPath + "]");

    }
}
