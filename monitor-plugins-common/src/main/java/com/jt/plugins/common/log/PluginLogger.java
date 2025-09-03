// 文件路径: monitor-plugins-common/src/main/java/com/jt/plugins/common/log/PluginLogger.java
package com.jt.plugins.common.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件日志记录工具类
 * 支持每个插件独立记录日志，错误日志同时输出到控制台
 */
public class PluginLogger {

    private static final ConcurrentHashMap<String, PluginLogger> loggerCache = new ConcurrentHashMap<>();
    private static final String LOG_BASE_PATH = getLogBasePath();

    private final Logger pluginLogger;
    private final String pluginName;

    private PluginLogger(String pluginName) {
        this.pluginName = pluginName;
        this.pluginLogger = createPluginLogger(pluginName);
    }

    /**
     * 获取插件日志记录器
     * @param pluginName 插件名称
     * @return PluginLogger实例
     */
    public static PluginLogger getLogger(String pluginName) {
        return loggerCache.computeIfAbsent(pluginName, PluginLogger::new);
    }

    /**
     * 获取日志基础路径
     * 优先级：系统临时目录 > 用户主目录 > 当前目录
     */
    private static String getLogBasePath() {
        String logPath = null;

//        // 首先尝试使用系统临时目录
//        try {
//            String tempDir = System.getProperty("java.io.tmpdir");
//            if (tempDir != null && !tempDir.isEmpty()) {
//                logPath = new File(tempDir, "jt-monitor-logs").getAbsolutePath();
//            }
//        } catch (Exception e) {
//            // 忽略异常，尝试其他位置
//        }
//
//        // 如果临时目录不可用，尝试用户主目录
//        if (logPath == null) {
//            try {
//                String userHome = System.getProperty("user.home");
//                if (userHome != null && !userHome.isEmpty()) {
//                    logPath = new File(userHome, "jt-monitor-logs").getAbsolutePath();
//                }
//            } catch (Exception e) {
//                // 忽略异常，使用当前目录
//            }
//        }

        // 如果以上都不可用，使用当前目录
        if (logPath == null) {
            logPath = System.getProperty("user.dir") + File.separator + "logs";
        }

        return logPath;
    }

    /**
     * 为插件创建独立的日志记录器
     */
    private Logger createPluginLogger(String pluginName) {
        // 创建插件日志目录
        createPluginLogDirectory(pluginName);

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        // 创建文件Appender
        RollingFileAppender<ch.qos.logback.classic.spi.ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(loggerContext);
        fileAppender.setName(pluginName.toUpperCase() + "_FILE");
        fileAppender.setFile(LOG_BASE_PATH + "/" + pluginName + "/" + pluginName + ".log");

        // 设置滚动策略
        TimeBasedRollingPolicy<ch.qos.logback.classic.spi.ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
        rollingPolicy.setContext(loggerContext);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern(LOG_BASE_PATH + "/" + pluginName + "/" + pluginName + ".%d{yyyy-MM-dd}.log");
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

        // 获取插件日志记录器并添加appender
        ch.qos.logback.classic.Logger pluginLogger = loggerContext.getLogger(pluginName);
        pluginLogger.addAppender(fileAppender);

        // 对于ERROR和WARN级别的日志，同时输出到控制台
        pluginLogger.setAdditive(true); // 允许向上传播到根logger（根logger有控制台appender）

        return pluginLogger;
    }

    /**
     * 创建插件日志目录
     */
    private void createPluginLogDirectory(String pluginName) {
        File logDir = new File(LOG_BASE_PATH, pluginName);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
    }

    /**
     * 记录调试日志
     */
    public void debug(String message) {
        pluginLogger.debug(message);
    }

    /**
     * 记录调试日志（带参数）
     */
    public void debug(String format, Object... arguments) {
        pluginLogger.debug(MessageFormatter.arrayFormat(format, arguments).getMessage());
    }

    /**
     * 记录信息日志
     */
    public void info(String message) {
        pluginLogger.info(message);
    }

    /**
     * 记录信息日志（带参数）
     */
    public void info(String format, Object... arguments) {
        pluginLogger.info(MessageFormatter.arrayFormat(format, arguments).getMessage());
    }

    /**
     * 记录警告日志
     */
    public void warn(String message) {
        pluginLogger.warn(message);
    }

    /**
     * 记录警告日志（带参数）
     */
    public void warn(String format, Object... arguments) {
        pluginLogger.warn(MessageFormatter.arrayFormat(format, arguments).getMessage());
    }

    /**
     * 记录错误日志
     */
    public void error(String message) {
        pluginLogger.error(message);
    }

    /**
     * 记录错误日志（带参数）
     */
    public void error(String format, Object... arguments) {
        pluginLogger.error(MessageFormatter.arrayFormat(format, arguments).getMessage());
    }

    /**
     * 记录异常日志
     */
    public void error(String message, Throwable throwable) {
        pluginLogger.error(message, throwable);
    }

    /**
     * 记录异常日志（带参数）
     */
    public void error(String format, Throwable throwable, Object... arguments) {
        pluginLogger.error(MessageFormatter.arrayFormat(format, arguments).getMessage(), throwable);
    }
}
