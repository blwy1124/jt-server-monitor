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
 * @CreateTime: 2026-02-26  15:20
 * @Description: TODO
 * @Version: 1.0
 */

/**
 * 语义分析器
 * 负责基于语义的智能去重
 */
public class SemanticAnalyzer {

    private static final PluginLogger logger = PluginLogger.getLogger("log-clean-plugin");

    // 时间模式
    private static final Pattern TIME_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[\\sT]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,3})?");
    private static final Pattern LEVEL_PATTERN = Pattern.compile("\\b(ERROR|WARN|INFO|DEBUG|TRACE)\\b", Pattern.CASE_INSENSITIVE);

    /**
     * 执行语义去重
     * @param lines 日志行列表
     * @param timeWindowSeconds 时间窗口（秒）
     * @return 去重后的行列表
     */
    public List<String> performSemanticDeduplication(List<String> lines, int timeWindowSeconds) {
        long startTime = System.currentTimeMillis();

        // 使用LinkedHashMap保持顺序，存储首次出现的内容
        Map<String, LogEntry> firstOccurrences = new LinkedHashMap<>();
        List<String> result = new ArrayList<>();
        int duplicateCount = 0;

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                result.add(line); // 保留空行
                continue;
            }

            // 提取时间和语义特征
            LocalDateTime logTime = extractTime(line);
            String semanticKey = extractSemanticKey(line);
            String timeBucketKey = generateTimeBucketKey(semanticKey, logTime, timeWindowSeconds);

            if (!firstOccurrences.containsKey(timeBucketKey)) {
                // 首次出现，保留
                firstOccurrences.put(timeBucketKey, new LogEntry(line, logTime));
                result.add(line);
                logger.debug("首次出现: {}", semanticKey);
            } else {
                // 已存在，检查时间窗口
                LogEntry existingEntry = firstOccurrences.get(timeBucketKey);
                if (isWithinTimeWindow(existingEntry.getTime(), logTime, timeWindowSeconds)) {
                    // 时间窗口内，视为重复
                    duplicateCount++;
                    logger.debug("时间窗口内重复，跳过: {}", semanticKey);
                } else {
                    // 超出时间窗口，视为新的独立事件
                    firstOccurrences.put(timeBucketKey, new LogEntry(line, logTime));
                    result.add(line);
                    logger.debug("超出时间窗口，保留新事件: {}", semanticKey);
                }
            }
        }

        long endTime = System.currentTimeMillis();
        logger.info("语义去重完成 - 原始行数: {}, 去重后: {}, 重复行数: {}, 耗时: {}ms",
                lines.size(), result.size(), duplicateCount, (endTime - startTime));

        return result;
    }

    /**
     * 全局语义去重（整个文件范围内的去重）
     */
    public List<String> performGlobalSemanticDeduplication(List<String> lines) {
        long startTime = System.currentTimeMillis();
        
        // 使用LinkedHashMap保持顺序，存储首次出现的内容
        Map<String, String> firstOccurrences = new LinkedHashMap<>();
        List<String> result = new ArrayList<>();
        int duplicateCount = 0;
        
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                result.add(line); // 保留空行
                continue;
            }
            
            // 提取语义特征
            String semanticKey = extractSemanticKey(line);
            
            if (!firstOccurrences.containsKey(semanticKey)) {
                // 首次出现，保留
                firstOccurrences.put(semanticKey, line);
                result.add(line);
                logger.debug("首次出现: {}", semanticKey);
            } else {
                // 已存在，视为重复（全局去重）
                duplicateCount++;
                logger.debug("全局重复，跳过: {}", semanticKey);
            }
        }
        
        long endTime = System.currentTimeMillis();
        logger.info("全局语义去重完成 - 原始行数: {}, 去重后: {}, 重复行数: {}, 耗时: {}ms",
               lines.size(), result.size(), duplicateCount, (endTime - startTime));
        
        return result;
    }

    /**
     * 提取时间
     */
    private LocalDateTime extractTime(String logLine) {
        Matcher timeMatcher = TIME_PATTERN.matcher(logLine);
        if (timeMatcher.find()) {
            try {
                String timeStr = timeMatcher.group();
                DateTimeFormatter formatter = getTimeFormatter(timeStr);
                return LocalDateTime.parse(timeStr.replace("T", " "), formatter);
            } catch (Exception e) {
                logger.debug("时间解析失败: {}", logLine);
            }
        }
        return null;
    }

    /**
     * 根据时间字符串获取对应的格式化器
     */
    private DateTimeFormatter getTimeFormatter(String timeStr) {
        if (timeStr.contains("T")) {
            return DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        } else if (timeStr.contains(".")) {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        } else {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        }
    }

    /**
     * 提取语义特征键
     */
    private String extractSemanticKey(String logLine) {
        StringBuilder key = new StringBuilder();

        // 提取日志级别
        Matcher levelMatcher = LEVEL_PATTERN.matcher(logLine);
        key.append(levelMatcher.find() ? levelMatcher.group(1).toLowerCase() : "no_level");
        key.append("|");

        // 提取核心消息模式（去除可变内容）
        String corePattern = logLine.replaceAll(TIME_PATTERN.pattern(), "[TIME]")
                .replaceAll("\\d+", "[NUM]")
                .replaceAll("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}", "[UUID]")
                .replaceAll("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}", "[IP]")
                .replaceAll("用户\\d+", "[USER]")
                .replaceAll("订单[A-Za-z0-9]+", "[ORDER]")
                .trim();

        key.append(corePattern.hashCode());
        return key.toString();
    }

    /**
     * 生成时间桶键
     */
    private String generateTimeBucketKey(String semanticKey, LocalDateTime time, int timeWindowSeconds) {
        if (time == null) {
            return semanticKey + "|NO_TIME";
        }

        // 将时间分桶化
        long epochSecond = time.toEpochSecond(java.time.ZoneOffset.UTC);
        long timeBucket = epochSecond / timeWindowSeconds;

        return semanticKey + "|BUCKET_" + timeBucket;
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
     * 日志条目封装类
     */
    private static class LogEntry {
        private final String line;
        private final LocalDateTime time;

        public LogEntry(String line, LocalDateTime time) {
            this.line = line;
            this.time = time;
        }

        public String getLine() { return line; }
        public LocalDateTime getTime() { return time; }
    }
}

