package cn.com.helei.bot_father.util.log;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AppendLogger {

    private final Logger log;

    private final StringBuilder format = new StringBuilder();


    public AppendLogger(Class<?> clazz) {
        log = LoggerFactory.getLogger(clazz);
    }

    public AppendLogger append(Object context) {
        if (!format.isEmpty()) format.append(" ");

        format.append(context);

        return this;
    }

    public void info(Object context) {
        log.info("\033[32m" + getPrefix(context) + "\033[0m");
    }

    public void warn(Object context) {
        log.warn("\033[33m" + getPrefix(context) + "\033[0m");
    }

    public void error(Object context) {
        log.error("\033[31m" + getPrefix(context) + "\033[0m");
    }

    public void error(Object context, Throwable e) {
        log.error("\033[31m" + getPrefix(context) + "\033[0m", e);
    }

    private @NotNull String getPrefix(Object context) {
        return format + " - " + context;
    }
}
