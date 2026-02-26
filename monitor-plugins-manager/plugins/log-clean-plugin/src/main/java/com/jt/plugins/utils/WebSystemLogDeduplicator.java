package com.jt.plugins.utils;

import com.jt.plugins.common.log.PluginLogger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
 * @CreateTime: 2026-02-26  15:58
 * @Description: TODO
 * @Version: 1.0
 */

/**
 * WEB系统日志去重器
 * 专门处理带星号分隔符的WEB系统日志格式
 */
public class WebSystemLogDeduplicator {

    private static final PluginLogger logger = PluginLogger.getLogger("log-clean-plugin");

    // 日志格式模式定义
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("\\*{10,}\\【([^】]+)系统】\\*{10,}");
    private static final Pattern START_TIME_PATTERN = Pattern.compile("【日志开始：(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})】");
    private static final Pattern END_TIME_PATTERN = Pattern.compile("【日志结束：(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})】");
    private static final Pattern EXCEPTION_PATTERN = Pattern.compile("在函数\\s+(.+?):\\s+(.+?)(?=\\s*【日志结束】|$)");

    /**
     * WEB系统日志去重主方法
     */
    public List<String> deduplicateWebSystemLogs(List<String> lines, int timeWindowSeconds) {
        long startTime = System.currentTimeMillis();
        
        // 解析日志段落
        List<WebLogSegment> segments = parseWebLogSegments(lines);
        logger.info("解析出 {} 个WEB系统日志段落", segments.size());
        
        // 基于语义和时间窗口去重
        Map<String, WebLogSegment> uniqueSegments = new LinkedHashMap<>();
        List<String> result = new ArrayList<>();
        int duplicateCount = 0;
        
        for (WebLogSegment segment : segments) {
            String semanticKey = generateSemanticKey(segment);
            
            if (!uniqueSegments.containsKey(semanticKey)) {
                // 首次出现，保留
                uniqueSegments.put(semanticKey, segment);
                result.addAll(segment.getLines());
                logger.debug("首次出现段落，语义键: {}, 时间: {}", 
                        semanticKey, segment.getStartTime());
            } else {
                // 已存在的段落，检查时间窗口
                WebLogSegment existingSegment = uniqueSegments.get(semanticKey);
                boolean withinWindow = isWithinTimeWindow(existingSegment.getStartTime(), 
                                                    segment.getStartTime(), timeWindowSeconds);
                
                logger.debug("检查时间窗口 - 语义键: {}, 首次时间: {}, 当前时间: {}, 时间差: {}秒, 窗口: {}秒, 结果: {}",
                        semanticKey,
                        existingSegment.getStartTime(),
                        segment.getStartTime(),
                        segment.getStartTime() != null && existingSegment.getStartTime() != null ? 
                            Math.abs(java.time.Duration.between(existingSegment.getStartTime(), segment.getStartTime()).getSeconds()) : -1,
                        timeWindowSeconds,
                        withinWindow);
                
                if (withinWindow) {
                    // 时间窗口内，视为重复
                    duplicateCount++;
                    logger.debug("时间窗口内重复，跳过语义键: {}", semanticKey);
                } else {
                    // 超出时间窗口，保留新的段落
                    uniqueSegments.put(semanticKey, segment);
                    result.addAll(segment.getLines());
                    logger.debug("超出时间窗口，保留新段落，语义键: {}", semanticKey);
                }
            }
        }
        
        long endTime = System.currentTimeMillis();
        logger.info("WEB系统日志去重完成 - 原始段落数: {}, 去重后: {}, 重复段落数: {}, 耗时: {}ms",
               segments.size(), uniqueSegments.size(), duplicateCount, (endTime - startTime));
        
        return result;
    }

    /**
     * 全局去重（整个文件范围内的去重）
     */
    public List<String> deduplicateWebSystemLogsGlobal(List<String> lines) {
        long startTime = System.currentTimeMillis();
        
        // 解析日志段落
        List<WebLogSegment> segments = parseWebLogSegments(lines);
        logger.info("解析出 {} 个WEB系统日志段落", segments.size());
        
        // 全局去重（不考虑时间因素）
        Map<String, WebLogSegment> uniqueSegments = new LinkedHashMap<>();
        List<String> result = new ArrayList<>();
        int duplicateCount = 0;
        
        for (WebLogSegment segment : segments) {
            String semanticKey = generateSemanticKey(segment);
            
            if (!uniqueSegments.containsKey(semanticKey)) {
                // 首次出现，保留
                uniqueSegments.put(semanticKey, segment);
                result.addAll(segment.getLines());
                logger.debug("首次出现段落，语义键: {}", semanticKey);
            } else {
                // 已存在，视为重复（全局去重）
                duplicateCount++;
                logger.debug("全局重复，跳过语义键: {}", semanticKey);
            }
        }
        
        long endTime = System.currentTimeMillis();
        logger.info("WEB系统日志全局去重完成 - 原始段落数: {}, 去重后: {}, 重复段落数: {}, 耗时: {}ms",
               segments.size(), uniqueSegments.size(), duplicateCount, (endTime - startTime));
        
        return result;
    }

    /**
     * 解析WEB系统日志段落
     */
    private List<WebLogSegment> parseWebLogSegments(List<String> lines) {
        List<WebLogSegment> segments = new ArrayList<>();
        WebLogSegment currentSegment = null;
        int lineIndex = 0;

        while (lineIndex < lines.size()) {
            String line = lines.get(lineIndex);

            // 检测段落分隔符
            Matcher separatorMatcher = SEPARATOR_PATTERN.matcher(line);
            if (separatorMatcher.find()) {
                // 发现新的段落开始
                if (currentSegment != null) {
                    segments.add(currentSegment);
                }

                String systemName = separatorMatcher.group(1);
                currentSegment = new WebLogSegment(systemName);
                currentSegment.addLine(line);
                lineIndex++;
                continue;
            }

            // 如果当前在段落中，继续收集行
            if (currentSegment != null) {
                currentSegment.addLine(line);

                // 检查是否是段落结束（下一个分隔符或文件结束）
                if (lineIndex + 1 < lines.size()) {
                    String nextLine = lines.get(lineIndex + 1);
                    if (SEPARATOR_PATTERN.matcher(nextLine).find()) {
                        // 下一行是新的分隔符，当前段落结束
                        segments.add(currentSegment);
                        currentSegment = null;
                    }
                } else {
                    // 文件结束，添加最后一个段落
                    segments.add(currentSegment);
                    currentSegment = null;
                }
            } else {
                // 不在段落中的行（如单独的空行等）
                WebLogSegment standalone = new WebLogSegment("STANDALONE");
                standalone.addLine(line);
                segments.add(standalone);
            }

            lineIndex++;
        }

        // 提取每个段落的时间和异常信息
        for (WebLogSegment segment : segments) {
            extractSegmentMetadata(segment);
        }

        return segments;
    }

    /**
     * 提取段落元数据（时间、异常类型等）
     */
    private void extractSegmentMetadata(WebLogSegment segment) {
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        String exceptionType = null;
        String exceptionMessage = null;

        for (String line : segment.getLines()) {
            // 提取开始时间
            Matcher startMatcher = START_TIME_PATTERN.matcher(line);
            if (startMatcher.find() && startTime == null) {
                try {
                    startTime = LocalDateTime.parse(startMatcher.group(1),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } catch (Exception e) {
                    logger.debug("开始时间解析失败: {}", startMatcher.group(1));
                }
            }

            // 提取结束时间
            Matcher endMatcher = END_TIME_PATTERN.matcher(line);
            if (endMatcher.find() && endTime == null) {
                try {
                    endTime = LocalDateTime.parse(endMatcher.group(1),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } catch (Exception e) {
                    logger.debug("结束时间解析失败: {}", endMatcher.group(1));
                }
            }

            // 提取异常信息
            Matcher exceptionMatcher = EXCEPTION_PATTERN.matcher(line);
            if (exceptionMatcher.find() && exceptionType == null) {
                exceptionType = exceptionMatcher.group(1);
                exceptionMessage = exceptionMatcher.group(2);
            }
        }

        segment.setStartTime(startTime);
        segment.setEndTime(endTime);
        segment.setExceptionType(exceptionType);
        segment.setExceptionMessage(exceptionMessage);
    }

    /**
     * 生成语义键（用于去重比较）
     */
    private String generateSemanticKey(WebLogSegment segment) {
        StringBuilder key = new StringBuilder();

        // 系统名称
        key.append(segment.getSystemName()).append("|");

        // 异常类型
        if (segment.getExceptionType() != null) {
            key.append(segment.getExceptionType()).append("|");
        } else {
            key.append("NO_EXCEPTION|");
        }

        // 核心错误信息（去除时间和动态内容）
        String coreMessage = extractCoreErrorMessage(segment);
        key.append(coreMessage.hashCode());

        return key.toString();
    }

    /**
     * 提取核心错误信息
     */
    private String extractCoreErrorMessage(WebLogSegment segment) {
        StringBuilder message = new StringBuilder();

        for (String line : segment.getLines()) {
            // 移除时间戳、日期等动态内容
            String cleanLine = line.replaceAll("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}", "[TIME]")
                    .replaceAll("\\d{4}-\\d{2}-\\d{2}", "[DATE]")
                    .replaceAll("'[^']*'", "'[VALUE]'")  // SQL字符串值
                    .replaceAll("\\d+", "[NUM]")         // 数字
                    .replaceAll("[a-fA-F0-9]{32}", "[GUID]") // GUID
                    .replaceAll("\"[^\"]*\"", "\"[TEXT]\"") // JSON字符串
                    .trim();

            // 排除模板行
            if (!cleanLine.isEmpty() &&
                    !cleanLine.startsWith("*") &&
                    !cleanLine.contains("【日志开始") &&
                    !cleanLine.contains("【日志结束")) {
                message.append(cleanLine).append(" ");
            }
        }

        return message.toString().trim();
    }

    /**
     * 判断是否在时间窗口内（修正版本）
     */
    private boolean isWithinTimeWindow(LocalDateTime time1, LocalDateTime time2, int seconds) {
        // 如果任一时间为null，视为在窗口内（保守策略）
        if (time1 == null || time2 == null) {
            logger.debug("时间为空，视为在窗口内: time1={}, time2={}", time1, time2);
            return true;
        }
        
        long diffSeconds = Math.abs(java.time.Duration.between(time1, time2).getSeconds());
        boolean withinWindow = diffSeconds <= seconds;
        
        logger.debug("时间窗口检查: time1={}, time2={}, 差值={}秒, 窗口={}秒, 结果={}",
                time1, time2, diffSeconds, seconds, withinWindow);
        
        return withinWindow;
    }

    /**
     * WEB日志段落封装类
     */
    private static class WebLogSegment {
        private final List<String> lines;
        private final String systemName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String exceptionType;
        private String exceptionMessage;

        public WebLogSegment(String systemName) {
            this.lines = new ArrayList<>();
            this.systemName = systemName;
        }

        public void addLine(String line) {
            lines.add(line);
        }

        // Getters and Setters
        public List<String> getLines() { return lines; }
        public String getSystemName() { return systemName; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public String getExceptionType() { return exceptionType; }
        public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }
        public String getExceptionMessage() { return exceptionMessage; }
        public void setExceptionMessage(String exceptionMessage) { this.exceptionMessage = exceptionMessage; }
        public int getLineCount() { return lines.size(); }
    }
}
