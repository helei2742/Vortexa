package cn.com.vortexa.script_node.util.log;

import cn.com.vortexa.common.util.AnsiColor;
import cn.com.vortexa.common.util.DiscardingBlockingQueue;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

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
