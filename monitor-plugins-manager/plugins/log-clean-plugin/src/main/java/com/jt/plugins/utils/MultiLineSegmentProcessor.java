package com.jt.plugins.utils;

import com.jt.plugins.common.log.PluginLogger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.utils
 * @Author: 别来无恙qb
 * @CreateTime: 2026-02-26  15:21
 * @Description: TODO
 * @Version: 1.0
 */
/**
 * 多行段落处理器
 * 处理多行日志段落的去重逻辑
 */
public class MultiLineSegmentProcessor {
    
    private static final PluginLogger logger = PluginLogger.getLogger("log-clean-plugin");
    
    private final SegmentLogDeduplicator segmentDeduplicator;
    private final WebSystemLogDeduplicator webSystemDeduplicator;
    
    public MultiLineSegmentProcessor() {
        this.segmentDeduplicator = new SegmentLogDeduplicator();
        this.webSystemDeduplicator = new WebSystemLogDeduplicator();
    }
    
    /**
     * 检测日志格式类型
     */
    public LogFormatType detectLogFormat(List<String> lines) {
        // 检测WEB系统格式
        if (isWebSystemFormat(lines)) {
            return LogFormatType.WEB_SYSTEM;
        }
        
        // 检测通用段落格式
        if (segmentDeduplicator.isSegmentLogFormat(lines)) {
            return LogFormatType.SEGMENT_LOG;
        }
        
        // 检测多行段落
        if (detectMultiLineSegments(lines)) {
            return LogFormatType.MULTI_LINE;
        }
        
        return LogFormatType.SINGLE_LINE;
    }
    
    /**
     * 检测是否为WEB系统日志格式
     */
    private boolean isWebSystemFormat(List<String> lines) {
        int starSeparators = 0;
        int webSystemMarkers = 0;
        
        for (String line : lines) {
            if (line != null) {
                if (line.contains("**********************************")) {
                    starSeparators++;
                }
                if (line.contains("【WEB系统】")) {
                    webSystemMarkers++;
                }
            }
        }
        
        return starSeparators > 0 && webSystemMarkers > 0;
    }
    
    /**
     * 处理不同类型格式的日志
     */
    public List<String> processSegments(List<String> lines, int timeWindowSeconds) {
        LogFormatType formatType = detectLogFormat(lines);
        
        logger.info("检测到日志格式类型: {}", formatType);
        logger.info("使用时间窗口: {}秒", timeWindowSeconds);
        
        switch (formatType) {
            case WEB_SYSTEM:
                logger.info("使用WEB系统日志去重器，时间窗口: {}秒", timeWindowSeconds);
                return webSystemDeduplicator.deduplicateWebSystemLogs(lines, timeWindowSeconds);
                
            case SEGMENT_LOG:
                logger.info("使用段落日志去重器，时间窗口: {}秒", timeWindowSeconds);
                return segmentDeduplicator.deduplicateSegmentLogs(lines, timeWindowSeconds);
                
            case MULTI_LINE:
                logger.info("使用通用多行段落处理，时间窗口: {}秒", timeWindowSeconds);
                return processGenericSegments(lines, timeWindowSeconds);
                
            default:
                logger.info("使用语义去重处理，时间窗口: {}秒", timeWindowSeconds);
                SemanticAnalyzer analyzer = new SemanticAnalyzer();
                return analyzer.performSemanticDeduplication(lines, timeWindowSeconds);
        }
    }
    
    /**
     * 全局段落处理（不考虑时间因素）
     */
    public List<String> processSegmentsGlobal(List<String> lines) {
        LogFormatType formatType = detectLogFormat(lines);
        
        switch (formatType) {
            case WEB_SYSTEM:
                logger.info("全局去重 - WEB系统格式");
                return webSystemDeduplicator.deduplicateWebSystemLogsGlobal(lines);
                
            case SEGMENT_LOG:
                logger.info("全局去重 - 段落日志格式");
                return segmentDeduplicator.deduplicateSegmentLogsGlobal(lines);
                
            case MULTI_LINE:
                logger.info("全局去重 - 通用多行段落");
                return processGenericSegmentsGlobal(lines);
                
            default:
                logger.info("全局去重 - 语义去重");
                SemanticAnalyzer analyzer = new SemanticAnalyzer();
                return analyzer.performGlobalSemanticDeduplication(lines);
        }
    }
    
    /**
     * 检测是否存在多行段落
     */
    public boolean detectMultiLineSegments(List<String> lines) {
        // 检测特定的日志格式特征
        int segmentMarkers = 0;
        int consecutiveLines = 0;
        
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            
            // 检测段落标记
            if (line.contains("【日志开始：") || line.contains("【日志结束：")) {
                segmentMarkers++;
            }
            
            // 检测连续的非空行
            if (!line.trim().isEmpty()) {
                consecutiveLines++;
            } else {
                consecutiveLines = 0;
            }
        }
        
        boolean hasSegments = segmentMarkers > 0 || consecutiveLines > 2;
        logger.info("多行段落检测结果: {}, 段落标记数: {}, 最大连续行数: {}", 
                   hasSegments, segmentMarkers, consecutiveLines);
        return hasSegments;
    }
    
    /**
     * 通用多行段落处理
     */
    private List<String> processGenericSegments(List<String> lines, int timeWindowSeconds) {
        // 这里可以保留原有的通用处理逻辑
        // 或者直接调用段落去重器的通用方法
        return segmentDeduplicator.deduplicateSegmentLogs(lines, timeWindowSeconds);
    }
    
    /**
     * 通用多行段落全局去重
     */
    public List<String> processGenericSegmentsGlobal(List<String> lines) {
        // 可以复用现有的段落处理逻辑，但不进行时间窗口检查
        return segmentDeduplicator.deduplicateSegmentLogsGlobal(lines);
    }
    
    private static final Pattern TIME_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[\\sT]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,3})?");
    private static final Pattern LEVEL_PATTERN = Pattern.compile("\\[(error|warn|info|debug|trace)\\]", Pattern.CASE_INSENSITIVE);

    /**
     * 将日志行分组成段落
     */
    private List<LogSegment> groupIntoSegments(List<String> lines) {
        List<LogSegment> segments = new ArrayList<>();
        LogSegment currentSegment = null;

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                if (currentSegment != null) {
                    currentSegment.addLine(line);
                } else {
                    LogSegment emptySegment = new LogSegment();
                    emptySegment.addLine(line);
                    segments.add(emptySegment);
                }
                continue;
            }

            LocalDateTime lineTime = extractTime(line);

            if (lineTime != null) {
                // 新的时间戳，开始新段落
                if (currentSegment != null) {
                    segments.add(currentSegment);
                }
                currentSegment = new LogSegment(lineTime);
                currentSegment.addLine(line);
            } else {
                // 无时间戳的行，添加到当前段落
                if (currentSegment != null) {
                    currentSegment.addLine(line);
                } else {
                    currentSegment = new LogSegment();
                    currentSegment.addLine(line);
                }
            }
        }

        if (currentSegment != null) {
            segments.add(currentSegment);
        }

        return segments;
    }

    /**
     * 生成段落键
     */
    private String generateSegmentKey(LogSegment segment) {
        StringBuilder key = new StringBuilder();

        String firstLine = segment.getFirstLine();
        if (firstLine != null) {
            Matcher levelMatcher = LEVEL_PATTERN.matcher(firstLine);
            key.append(levelMatcher.find() ? levelMatcher.group(1).toLowerCase() : "no_level");
            key.append("|");

            String coreMessage = firstLine.replaceAll(TIME_PATTERN.pattern(), "[TIME]")
                    .replaceAll("\\d+", "[NUM]")
                    .replaceAll("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}", "[UUID]")
                    .replaceAll("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}", "[IP]");

            key.append(coreMessage.hashCode());
        }

        return key.toString();
    }

    /**
     * 提取时间
     */
    private static LocalDateTime extractTime(String logLine) {
        Matcher timeMatcher = TIME_PATTERN.matcher(logLine);
        if (timeMatcher.find()) {
            try {
                String timeStr = timeMatcher.group();
                return LocalDateTime.parse(timeStr.replace("T", " "),
                        timeStr.contains(".") ?
                                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS") :
                                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e) {
                logger.debug("时间解析失败: {}", logLine);
            }
        }
        return null;
    }

    /**
     * 判断是否在时间窗口内
     */
    private boolean isWithinTimeWindow(LocalDateTime time1, LocalDateTime time2, int seconds) {
        if (time1 == null || time2 == null) return true;
        long diffSeconds = Math.abs(java.time.Duration.between(time1, time2).getSeconds());
        return diffSeconds <= seconds;
    }

    /**
     * 日志段落封装类
     */
    private static class LogSegment {
        private final List<String> lines;
        private final LocalDateTime startTime;
        private LocalDateTime endTime;

        public LogSegment() {
            this.lines = new ArrayList<>();
            this.startTime = null;
            this.endTime = null;
        }

        public LogSegment(LocalDateTime startTime) {
            this.lines = new ArrayList<>();
            this.startTime = startTime;
            this.endTime = startTime;
        }

        public void addLine(String line) {
            lines.add(line);
            LocalDateTime lineTime = MultiLineSegmentProcessor.extractTime(line);
            if (lineTime != null && (endTime == null || lineTime.isAfter(endTime))) {
                endTime = lineTime;
            }
        }

        public List<String> getLines() { return lines; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public String getFirstLine() { return lines.isEmpty() ? null : lines.get(0); }
        public int getLineCount() { return lines.size(); }
    }
    
    /**
     * 日志格式类型枚举
     */
    public enum LogFormatType {
        WEB_SYSTEM,      // WEB系统格式（带星号分隔符）
        SEGMENT_LOG,     // 通用段落格式
        MULTI_LINE,      // 多行段落
        SINGLE_LINE      // 单行日志
    }
}
