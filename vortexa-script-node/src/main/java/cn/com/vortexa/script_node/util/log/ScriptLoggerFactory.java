package cn.com.vortexa.script_node.util.log;

import ch.qos.logback.classic.*;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import cn.com.vortexa.common.util.FileUtil;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ScriptLoggerFactory {

    private static final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

    public static Logger getScriptLogger(String scriptNodeName, String botKey) throws IOException {
        String loggerName = "script." + scriptNodeName + "." + botKey;

        // 查找已存在 logger，防止重复创建
        Logger logger = context.exists(loggerName);
        if (logger != null) {
            return logger;
        }

        String dir = FileUtil.createLogsDir(scriptNodeName, botKey);

        // 创建 logger
        logger = context.getLogger(loggerName);
        logger.setAdditive(false); // 不向 root 传递

        // ===== 公用 encoder =====
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level - %msg%n");
        encoder.start();

        // ===== ConsoleAppender 控制台输出 =====
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(context);
        consoleAppender.setName("CONSOLE-" + scriptNodeName + "-" + botKey);
        consoleAppender.setEncoder(encoder);
        consoleAppender.start();

        // ===== RollingFileAppender 文件滚动输出 =====
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(context);
        fileAppender.setName("FILE-" +  scriptNodeName + "-" + botKey);
        fileAppender.setFile(dir + File.separator  + botKey + ".log");

        // 滚动策略
        TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
        rollingPolicy.setContext(context);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern(dir + File.separator  + botKey + ".%d{yyyy-MM-dd}.%i.log");
        rollingPolicy.setMaxHistory(7); // 保留 7 天
        rollingPolicy.setTotalSizeCap(FileSize.valueOf("50MB")); // 最大总量

        // 按大小切分
        SizeAndTimeBasedFNATP<ILoggingEvent> triggeringPolicy = new SizeAndTimeBasedFNATP<>();
        triggeringPolicy.setMaxFileSize(FileSize.valueOf("10MB"));
        rollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(triggeringPolicy);

        rollingPolicy.start();
        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.setEncoder(encoder);
        fileAppender.start();

        // ===== logger 添加两个 appender =====
        logger.addAppender(consoleAppender);
        logger.addAppender(fileAppender);
        logger.setLevel(Level.INFO);

        return logger;
    }
}
