package cn.com.vortexa.script_node.util.log;

import cn.com.vortexa.common.util.DiscardingBlockingQueue;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

public class AppendLogger {

    private final Logger log;

    private final StringBuilder format = new StringBuilder();

    @Getter
    private final DiscardingBlockingQueue<LogContent> logCache = new DiscardingBlockingQueue<>(
            AutoBotConfig.LOG_CACHE_COUNT);

    @Setter
    private Consumer<LogContent> beforePrintHandler;

    public AppendLogger(Class<?> clazz, String scriptNodeName, String botName, String botKey) throws IOException {
        log = LoggerFactory.getLogger(clazz);

        append("scriptNode{%s}-botName{%s}-botKey{%s}".formatted(scriptNodeName, botName, botKey));
    }

    public AppendLogger append(Object context) {
        if (!format.isEmpty()) {
            format.append(" ");
        }

        format.append(context);

        return this;
    }

    public void info(Object context) {
        log.info("\033[32m" + getPrefix(LogType.INFO, context) + "\033[0m");
    }

    public void debug(Object context) {
        log.info("\033[90m" + getPrefix(LogType.DEBUG, context) + "\033[0m");
    }

    public void warn(Object context) {
        log.warn("\033[33m" + getPrefix(LogType.WARNING, context) + "\033[0m");
    }

    public void error(Object context) {
        log.error("\033[31m" + getPrefix(LogType.ERROR, context) + "\033[0m");
    }

    public void error(Object context, Throwable e) {
        log.error("\033[31m" + getPrefix(LogType.ERROR, context) + "\033[0m", e);
    }

    private @NotNull String getPrefix(LogType type, Object context) {
        String logStr = format + " - " + context;
        LogContent logContent = new LogContent(System.currentTimeMillis(), type, logStr);

        try {
            logCache.put(logContent);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (beforePrintHandler != null) {
            beforePrintHandler.accept(logContent);
        }
        return logStr;
    }

    public enum LogType {
        DEBUG,
        SUCCESS,
        INFO,
        ERROR,
        WARNING
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LogContent {
        private long datetime;

        private LogType type;

        private String content;
    }
}
