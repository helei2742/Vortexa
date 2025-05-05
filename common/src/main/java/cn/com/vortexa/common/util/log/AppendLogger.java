package cn.com.vortexa.common.util.log;

import cn.com.vortexa.common.util.AnsiColor;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;

public class AppendLogger {
    private final Logger log;

    private final StringBuilder format = new StringBuilder();

    public AppendLogger(String scriptNodeName, String botName, String botKey) throws IOException {
        log = ScriptLoggerFactory.getScriptLogger(scriptNodeName, botKey);
        append(AnsiColor.colorize("scriptNode{%s}botName{%s}botKey{%s}".formatted(
                scriptNodeName, botName, botKey
        ), AnsiColor.CYAN));
    }

    public AppendLogger append(Object context) {
        if (!format.isEmpty()) {
            format.append(" ");
        }

        format.append(context);

        return this;
    }

    public void info(Object context) {
        log.info(getPrefix(LogType.INFO, AnsiColor.colorize(context.toString(), AnsiColor.GREEN)));
    }

    public void debug(Object context) {
        log.info(getPrefix(LogType.DEBUG, AnsiColor.colorize(context.toString(), AnsiColor.GRAY)));
    }

    public void warn(Object context) {
        log.warn(getPrefix(LogType.WARNING, AnsiColor.colorize(context.toString(), AnsiColor.YELLOW)));
    }

    public void error(Object context) {
        log.error(getPrefix(LogType.ERROR, AnsiColor.colorize(context.toString(), AnsiColor.RED)));
    }

    public void error(Object context, Throwable e) {
        log.error(getPrefix(LogType.ERROR, AnsiColor.colorize(context.toString(), AnsiColor.RED)), e);
    }

    private @NotNull String getPrefix(LogType type, Object context) {
        return format + " - " + context;
    }

    public enum LogType {
        DEBUG,
        SUCCESS,
        INFO,
        ERROR,
        WARNING
    }
}
