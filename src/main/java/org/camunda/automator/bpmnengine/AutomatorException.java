package org.camunda.automator.bpmnengine;

import org.camunda.community.rest.client.invoker.ApiException;

public class AutomatorException extends Exception {
  public int code;
  public String message;

  public AutomatorException(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public AutomatorException(String message) {
    this.message = message;
  }

  public AutomatorException(String message, ApiException exception) {
    this.code = exception.getCode();
    this.message = message + " : " + exception.getMessage();
  }

  public String getMessage() {
    return message;
  }

  public int getCode() {
    return code;
  }
}
