package org.camunda.automator.services.dataoperation;

import org.camunda.automator.engine.AutomatorException;
import org.camunda.automator.engine.RunScenario;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Component

public class DataOperationNow extends DataOperation {

    public static final String FCT_LOCALDATETIME = "LOCALDATETIME";
    public static final String FCT_DATE = "DATE";
    public static final String FCT_ZONEDATETIME = "ZONEDATETIME";
    public static final String FCT_LOCALDATE = "LOCALDATE";
    public static final String ISO_8601_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd";

    @Override
    public boolean match(String value) {
        return matchFunction(value, "now");
    }

    @Override
    public String getName() {
        return "Now";
    }

    @Override
    public String getHelp() {
        return "now(" + FCT_LOCALDATETIME + "|" + FCT_DATE + "|" + FCT_ZONEDATETIME + "|"
                + FCT_LOCALDATE + ")";
    }

    @Override
    public Object execute(String value, RunScenario runScenario, int index) throws AutomatorException {
        List<String> args = extractArgument(value, true);

        if (args.size() != 1) {
            throw new AutomatorException("Bad argument: " + getHelp());
        }
        String formatArgs = args.get(0).toUpperCase(Locale.ROOT);

        try {
            if (FCT_LOCALDATETIME.equals(formatArgs)) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ISO_8601_DATETIME_FORMAT);
                return LocalDateTime.now().format(formatter);
            } else if (FCT_DATE.equals(formatArgs)) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(ISO_8601_DATETIME_FORMAT);
                return dateFormat.format(new Date());
            } else if (FCT_ZONEDATETIME.equals(formatArgs)) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ISO_8601_DATETIME_FORMAT);
                return ZonedDateTime.now().format(formatter);
            } else if (FCT_LOCALDATE.equals(formatArgs)) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(ISO_8601_DATE_FORMAT);
                return LocalDate.now().format(formatter);
            } else
                throw new AutomatorException("Unknown date formatter [" + formatArgs + "]");
        } catch (Exception e) {
            throw new AutomatorException(
                    "parsing error function[" + formatArgs + "] : " + e.getMessage());
        }

    }
}
