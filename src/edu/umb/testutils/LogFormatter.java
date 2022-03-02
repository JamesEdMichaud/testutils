package edu.umb.testutils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%-7s: ", record.getLevel().getLocalizedName()));

        LocalDateTime time = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(record.getMillis()),
                TimeZone.getDefault().toZoneId()
        );
        sb.append(time.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))).append(" ");

        String name = record.getSourceClassName();
        if (name != null) {
            sb.append(name.substring(name.lastIndexOf(".")+1));
        } else {
            sb.append(record.getLoggerName());
        }

        if (record.getSourceMethodName() != null) {
            sb.append(".");
            sb.append(record.getSourceMethodName());
        }
        sb.append(": ");
        String message = record.getMessage();
        if (message.contains("\n")) {
            message = String.format("\n    %s", message.replace("\n", "\n    "));
        }
        sb.append(message).append("\n");
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ex) {
                // Do nothing?
            }
        }
        return sb.toString();
    }
}
