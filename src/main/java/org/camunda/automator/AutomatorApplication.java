package org.camunda.automator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan

public class AutomatorApplication {

  public static void main(String[] args) {

    SpringApplication.run(AutomatorApplication.class, args);
    // thanks to Spring, the class CherryJobRunnerFactory is active. All runners (worker, connectors) start then
  }
  // https://docs.camunda.io/docs/components/best-practices/development/writing-good-workers/

}
