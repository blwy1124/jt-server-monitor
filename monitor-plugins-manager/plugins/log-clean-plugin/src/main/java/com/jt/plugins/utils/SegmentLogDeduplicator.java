package com.jt.plugins.utils;

import com.jt.plugins.common.log.PluginLogger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
 * @CreateTime: 2026-02-26  15:38
 * @Description: TODO
 * @Version: 1.0
 */
/**
 * 段落日志去重器
 * 专门处理带有明确开始/结束标记的多行日志段落
 */
public class SegmentLogDeduplicator {

    private static final PluginLogger logger = PluginLogger.getLogger("log-clean-plugin");

    // 段落边界模式
    private static final Pattern START_PATTERN = Pattern.compile("【日志开始：(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})】");
    private static final Pattern END_PATTERN = Pattern.compile("【日志结束：(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})】");
    private static final Pattern SYSTEM_PATTERN = Pattern.compile("【([^】]+)系统】");

    /**
     * 处理段落日志去重
     * @param lines 日志行列表
     * @param timeWindowSeconds 时间窗口（秒）
     * @return 去重后的行列表
     */
    public List<String> deduplicateSegmentLogs(List<String> lines, int timeWindowSeconds) {
        long startTime = System.currentTimeMillis();

        // 解析日志段落
        List<LogSegment> segments = parseLogSegments(lines);
        logger.info("解析出 {} 个日志段落", segments.size());

        // 对段落进行去重
        Map<String, LogSegment> uniqueSegments = new LinkedHashMap<>();
        List<String> result = new ArrayList<>();
        int duplicateCount = 0;

        for (LogSegment segment : segments) {
            String segmentKey = generateSegmentKey(segment);

            if (!uniqueSegments.containsKey(segmentKey)) {
                // 首次出现，保留
                uniqueSegments.put(segmentKey, segment);
                result.addAll(segment.getLines());
                logger.debug("首次出现段落，key: {}", segmentKey);
            } else {
                // 已存在的段落，检查时间窗口
                LogSegment existingSegment = uniqueSegments.get(segmentKey);
                if (isWithinTimeWindow(existingSegment.getStartTime(), segment.getStartTime(), timeWindowSeconds)) {
                    // 时间窗口内，视为重复，跳过
                    duplicateCount++;
                    logger.debug("时间窗口内重复段落，跳过 key: {}", segmentKey);
                } else {
                    // 超出时间窗口，但仍保留首次出现的内容
                    duplicateCount++;
                    logger.debug("超出时间窗口但仍保留首次内容，key: {}", segmentKey);
                }
            }
        }

        long endTime = System.currentTimeMillis();
        logger.info("段落日志去重完成 - 原始段落数: {}, 去重后: {}, 重复段落数: {}, 耗时: {}ms",
                segments.size(), uniqueSegments.size(), duplicateCount, (endTime - startTime));

        return result;
    }

    /**
     * 全局去重（整个文件范围内的去重）
     */
    public List<String> deduplicateSegmentLogsGlobal(List<String> lines) {
        long startTime = System.currentTimeMillis();
        
        // 解析日志段落
        List<LogSegment> segments = parseLogSegments(lines);
        logger.info("解析出 {} 个日志段落", segments.size());
        
        // 全局去重（不考虑时间因素）
        Map<String, LogSegment> uniqueSegments = new LinkedHashMap<>();
        List<String> result = new ArrayList<>();
        int duplicateCount = 0;
        
        for (LogSegment segment : segments) {
            String segmentKey = generateSegmentKey(segment);
            
            if (!uniqueSegments.containsKey(segmentKey)) {
                // 首次出现，保留
                uniqueSegments.put(segmentKey, segment);
                result.addAll(segment.getLines());
                logger.debug("首次出现段落，key: {}", segmentKey);
            } else {
                // 已存在，视为重复（全局去重）
                duplicateCount++;
                logger.debug("全局重复，跳过 key: {}", segmentKey);
            }
        }
        
        long endTime = System.currentTimeMillis();
        logger.info("段落日志全局去重完成 - 原始段落数: {}, 去重后: {}, 重复段落数: {}, 耗时: {}ms",
               segments.size(), uniqueSegments.size(), duplicateCount, (endTime - startTime));
        
        return result;
    }

    /**
     * 解析日志段落
     */
    private List<LogSegment> parseLogSegments(List<String> lines) {
        List<LogSegment> segments = new ArrayList<>();
        LogSegment currentSegment = null;
        LocalDateTime segmentStartTime = null;
        String systemName = null;

        for (String line : lines) {
            // 检查段落开始标记
            Matcher startMatcher = START_PATTERN.matcher(line);
            if (startMatcher.find()) {
                // 发现新的段落开始
                if (currentSegment != null) {
                    segments.add(currentSegment);
                }

                try {
                    segmentStartTime = LocalDateTime.parse(startMatcher.group(1),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } catch (DateTimeParseException e) {
                    logger.debug("时间解析失败: {}", startMatcher.group(1));
                    segmentStartTime = null;
                }

                currentSegment = new LogSegment(segmentStartTime);
                currentSegment.addLine(line);
                continue;
            }

            // 检查系统名称
            Matcher systemMatcher = SYSTEM_PATTERN.matcher(line);
            if (systemMatcher.find() && currentSegment != null) {
                systemName = systemMatcher.group(1);
                currentSegment.setSystemName(systemName);
            }

            // 检查段落结束标记
            Matcher endMatcher = END_PATTERN.matcher(line);
            if (endMatcher.find() && currentSegment != null) {
                currentSegment.addLine(line);
                segments.add(currentSegment);
                currentSegment = null;
                segmentStartTime = null;
                systemName = null;
                continue;
            }

            // 普通行，添加到当前段落
            if (currentSegment != null) {
                currentSegment.addLine(line);
            } else {
                // 不在段落中的行（如分隔符等），单独处理
                LogSegment standaloneSegment = new LogSegment(null);
                standaloneSegment.addLine(line);
                standaloneSegment.setSystemName("STANDALONE");
                segments.add(standaloneSegment);
            }
        }

        // 处理最后一个未结束的段落
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

        // 系统名称
        key.append(segment.getSystemName() != null ? segment.getSystemName() : "UNKNOWN");
        key.append("|");

        // 核心内容特征（去除时间戳和可变内容）
        String coreContent = extractCoreContent(segment);
        key.append(coreContent.hashCode());

        return key.toString();
    }

    /**
     * 提取段落核心内容
     */
    private String extractCoreContent(LogSegment segment) {
        StringBuilder content = new StringBuilder();

        for (String line : segment.getLines()) {
            // 移除时间戳
            String cleanLine = line.replaceAll("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}", "[TIME]")
                    .replaceAll("【日志开始：[^】]*】", "[START]")
                    .replaceAll("【日志结束：[^】]*】", "[END]")
                    .replaceAll("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}", "[UUID]")
                    .replaceAll("\"CSJBH\":\"[^\"]*\"", "\"CSJBH\":\"[ID]\"")
                    .trim();

            if (!cleanLine.isEmpty() && !cleanLine.equals("[START]") && !cleanLine.equals("[END]")) {
                content.append(cleanLine).append("\n");
            }
        }

        return content.toString();
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
     * 检测是否为段落日志格式
     */
    public boolean isSegmentLogFormat(List<String> lines) {
        int startMarkers = 0;
        int endMarkers = 0;
        int systemMarkers = 0;

        for (String line : lines) {
            if (line != null) {
                if (START_PATTERN.matcher(line).find()) startMarkers++;
                if (END_PATTERN.matcher(line).find()) endMarkers++;
                if (SYSTEM_PATTERN.matcher(line).find()) systemMarkers++;
            }
        }

        boolean isSegmentFormat = startMarkers > 0 && endMarkers > 0 && systemMarkers > 0;
        logger.debug("格式检测 - 开始标记: {}, 结束标记: {}, 系统标记: {}, 结果: {}",
                startMarkers, endMarkers, systemMarkers, isSegmentFormat);

        return isSegmentFormat;
    }

    /**
     * 日志段落封装类
     */
    private static class LogSegment {
        private final List<String> lines;
        private final LocalDateTime startTime;
        private String systemName;

        public LogSegment(LocalDateTime startTime) {
            this.lines = new ArrayList<>();
            this.startTime = startTime;
            this.systemName = null;
        }

        public void addLine(String line) {
            lines.add(line);
        }

        public void setSystemName(String systemName) {
            this.systemName = systemName;
        }

        public List<String> getLines() { return lines; }
        public LocalDateTime getStartTime() { return startTime; }
        public String getSystemName() { return systemName; }
        public int getLineCount() { return lines.size(); }
    }
}

