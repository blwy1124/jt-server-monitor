package com.jt.plugins.common.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.util.Iterator;

/**
 * 插件日志配置类
 * 统一管理所有插件的日志配置
 */
@Configuration
public class PluginLoggerConfig {

    private static final String LOG_HOME = getLogBasePath();
    private LoggerContext loggerContext;

    @PostConstruct
    public void init() {
        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        configureLogging();
    }

    @PreDestroy
    public void destroy() {
        if (loggerContext != null) {
            loggerContext.stop();
        }
    }

    /**
     * 获取日志基础路径
     * 优先级：系统临时目录 > 用户主目录 > 当前目录
     */
    private static String getLogBasePath() {
        String logPath = null;

        // 首先尝试使用系统临时目录
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            if (tempDir != null && !tempDir.isEmpty()) {
                logPath = new File(tempDir, "jt-monitor-logs").getAbsolutePath();
            }
        } catch (Exception e) {
            // 忽略异常，尝试其他位置
        }

        // 如果临时目录不可用，尝试用户主目录
        if (logPath == null) {
            try {
                String userHome = System.getProperty("user.home");
                if (userHome != null && !userHome.isEmpty()) {
                    logPath = new File(userHome, "jt-monitor-logs").getAbsolutePath();
                }
            } catch (Exception e) {
                // 忽略异常，使用当前目录
            }
        }

        // 如果以上都不可用，使用当前目录
        if (logPath == null) {
            logPath = System.getProperty("user.dir") + File.separator + "logs";
        }

        return logPath;
    }

    /**
     * 配置日志系统
     */
    private void configureLogging() {
        // 创建日志目录
        createLogDirectory("global");

        // 配置全局应用日志
        configureGlobalApplicationLogger();

        // 配置根日志记录器的控制台输出
        configureRootLogger();
    }

    /**
     * 配置全局应用日志（记录所有控制台输出）
     */
    private void configureGlobalApplicationLogger() {
        // 创建文件Appender
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("GLOBAL_APPLICATION_FILE");
        fileAppender.setFile(LOG_HOME + "/global/application.log");

        // 设置滚动策略
        TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
        rollingPolicy.setContext(loggerContext);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern(LOG_HOME + "/global/application.%d{yyyy-MM-dd}.log");
        rollingPolicy.setMaxHistory(30);
        rollingPolicy.start();

        fileAppender.setRollingPolicy(rollingPolicy);

        // 设置编码器
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%logger{50}] - %msg%n");
        encoder.start();

        fileAppender.setEncoder(encoder);
        fileAppender.start();

        // 获取根日志记录器并添加appender，这样可以记录所有日志
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger("ROOT");
        rootLogger.addAppender(fileAppender);
    }

    /**
     * 配置根日志记录器的控制台输出
     */
    private void configureRootLogger() {
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger("ROOT");
        // 确保根logger有控制台appender
        boolean hasConsoleAppender = false;
        Iterator<Appender<ILoggingEvent>> appenderIterator = rootLogger.iteratorForAppenders();
        while (appenderIterator.hasNext()) {
            Appender<ILoggingEvent> appender = appenderIterator.next();
            if ("CONSOLE".equals(appender.getName())) {
                hasConsoleAppender = true;
                break;
            }
        }

        if (!hasConsoleAppender) {
            ConsoleAppender<ILoggingEvent> consoleAppender = createConsoleAppender();
            rootLogger.addAppender(consoleAppender);
        }
    }

    /**
     * 创建控制台Appender
     */
    private ConsoleAppender<ILoggingEvent> createConsoleAppender() {
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(loggerContext);
        consoleAppender.setName("CONSOLE");

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%logger{50}] - %msg%n");
        encoder.start();

        consoleAppender.setEncoder(encoder);
        consoleAppender.start();

        return consoleAppender;
    }

    /**
     * 创建日志目录
     */
    private void createLogDirectory(String dirName) {
        File logDir = new File(LOG_HOME, dirName);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
    }

    /**
     * 创建控制台Appender Bean
     */
    @Bean
    public ConsoleAppender<ILoggingEvent> consoleAppender() {
        return createConsoleAppender();
    }
}
