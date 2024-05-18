package org.camunda.automator.services.dataoperation;

import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunScenario;
import org.springframework.stereotype.Component;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

@Component
public class DataOperationStringToDate extends DataOperation {

  public static final String FCT_LOCALDATETIME = "LOCALDATETIME";
  public static final String FCT_DATETIME = "DATETIME";
  public static final String FCT_DATE = "DATE";
  public static final String FCT_ZONEDATETIME = "ZONEDATETIME";
  public static final String FCT_LOCALDATE = "LOCALDATE";
  // visit https://docs.camunda.io/docs/components/modeler/bpmn/timer-events/#time-date
  // 2019-10-01T12:00:00Z
  public static final String ISO_8601_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
  public static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd";

  @Override
  public boolean match(String value) {
    return matchFunction(value, "stringtodate");
  }

  @Override
  public String getName() {
    return "StringToDate";
  }

  @Override
  public String getHelp() {
    return "stringtodate(" + FCT_LOCALDATETIME + "|" + FCT_DATETIME + "|" + FCT_DATE + "|" + FCT_ZONEDATETIME + "|"
        + FCT_LOCALDATE + ", dateSt)";
  }

  @Override
  public Object execute(String value, RunScenario runScenario, int index) throws AutomatorException {
    List<String> args = extractArgument(value, true);

    if (args.size() != 2) {
      throw new AutomatorException("Bad argument: " + getHelp());
    }
    String formatArgs = args.get(0).toUpperCase(Locale.ROOT);
    String valueArgs = args.get(1);
    try {
      if (FCT_LOCALDATETIME.equals(formatArgs))
        return LocalDateTime.parse(valueArgs);

      else if (FCT_DATETIME.equals(formatArgs)) {
        SimpleDateFormat isoFormat = new SimpleDateFormat(ISO_8601_DATETIME_FORMAT);
        return isoFormat.parse(valueArgs); // Date
      } else if (FCT_DATE.equals(formatArgs)) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(ISO_8601_DATE_FORMAT);
        dateFormat.setLenient(false);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.parse(valueArgs, new ParsePosition(0));
      } else if (FCT_ZONEDATETIME.equals(formatArgs)) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ISO_8601_DATETIME_FORMAT);
        return ZonedDateTime.parse(valueArgs, formatter);
      } else if (FCT_LOCALDATE.equals(formatArgs)) {
        return LocalDate.parse(valueArgs, DateTimeFormatter.ofPattern(ISO_8601_DATE_FORMAT));
      } else
        throw new AutomatorException("Unknown date formatter [" + formatArgs + "]");
    } catch (Exception e) {
      throw new AutomatorException(
          "parsing error function[" + formatArgs + "] value[" + valueArgs + "] : " + e.getMessage());
    }

  }
}
