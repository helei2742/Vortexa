package cn.com.vortexa.script_node.util.log;

import cn.com.vortexa.common.util.DiscardingBlockingQueue;
import cn.com.vortexa.script_node.config.AutoBotConfig;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;


public class AppendLogger {

    private final Logger log;

    private final StringBuilder format = new StringBuilder();

    @Getter
    private final DiscardingBlockingQueue<String> logCache = new DiscardingBlockingQueue<>(AutoBotConfig.LOG_CACHE_COUNT);

    @Setter
    private Consumer<String> beforePrintHandler;

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

    public void debug(Object context) {
        log.info("\033[90m" + getPrefix(context) + "\033[0m");
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
        String logContent = format + " - " + context;
        try {
            logCache.put(logContent);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (beforePrintHandler != null) {
            beforePrintHandler.accept(logContent);
        }
        return logContent;
    }
}
