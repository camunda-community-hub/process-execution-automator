package org.camunda.automator.engine;

import org.camunda.community.rest.client.invoker.ApiException;

public class AutomatorException extends Exception {
    public int code;
    public String codeSt;
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
        this.message = message + " : " + exception.getMessage() + " " + exception.getResponseBody();
    }

    public AutomatorException(String code, String message) {
        this.codeSt = code;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }
}
