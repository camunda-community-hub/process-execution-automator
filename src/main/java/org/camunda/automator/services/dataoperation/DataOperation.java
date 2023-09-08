package org.camunda.automator.services.dataoperation;

import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunScenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public abstract class DataOperation {

  public abstract boolean match(String value);

  public abstract Object execute(String value, RunScenario runScenario) throws AutomatorException;

  protected boolean matchFunction(String value, String function) {
    return value.toUpperCase(Locale.ROOT).startsWith(function.toUpperCase(Locale.ROOT) + "(");
  }

  protected List<String> extractArgument(String value, boolean resolveValue) throws AutomatorException {
    List<String> listResult = new ArrayList<>();
    value = value.trim();
    // format is function(arg1, args2, arg3
    int pos = value.indexOf("(");
    if (pos == -1 || !value.endsWith(")"))
      throw new AutomatorException("Format must be function(args), received [" + value + "]");
    String args = value.substring(pos);
    args = args.substring(1, args.length() - 1);
    StringTokenizer st = new StringTokenizer(args, ",");
    while (st.hasMoreTokens())
      listResult.add(st.nextToken());

    // each args, if it start by a " or ', remove them
    if (resolveValue) {
      listResult = listResult.stream().map(t -> {
        if (t.startsWith("\"") || t.startsWith("'"))
          return t.substring(1, t.length() - 1);
        else
          return t;
      }).collect(Collectors.toList());
    }
    return listResult;
  }

  public abstract String getHelp();
}
