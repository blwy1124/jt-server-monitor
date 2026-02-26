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
 * @CreateTime: 2026-02-26  15:20
 * @Description: TODO
 * @Version: 1.0
 */

/**
 * 基于时间的去重器
 * 处理时间相关的去重逻辑
 */
public class TimeBasedDeduplicator {

    private static final PluginLogger logger = PluginLogger.getLogger("log-clean-plugin");

    private static final Pattern TIME_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[\\sT]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,3})?");

    /**
     * 执行基于时间的去重
     * @param lines 日志行列表
     * @param timeWindowSeconds 时间窗口（秒）
     * @return 去重后的行列表
     */
    public List<String> performTimeBasedDeduplication(List<String> lines, int timeWindowSeconds) {
        long startTime = System.currentTimeMillis();

        Map<String, TimeGroup> timeGroups = new LinkedHashMap<>();
        List<String> result = new ArrayList<>();
        int duplicateCount = 0;

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                result.add(line);
                continue;
            }

            LocalDateTime logTime = extractTime(line);
            String timeKey = generateTimeKey(logTime, timeWindowSeconds);

            if (!timeGroups.containsKey(timeKey)) {
                TimeGroup newGroup = new TimeGroup(line, logTime);
                timeGroups.put(timeKey, newGroup);
                result.add(line);
            } else {
                TimeGroup existingGroup = timeGroups.get(timeKey);
                if (isWithinTimeWindow(existingGroup.getLatestTime(), logTime, timeWindowSeconds)) {
                    // 时间窗口内，视为重复
                    existingGroup.addLine(line, logTime);
                    duplicateCount++;
                } else {
                    // 超出时间窗口，创建新组
                    TimeGroup newGroup = new TimeGroup(line, logTime);
                    timeGroups.put(timeKey, newGroup);
                    result.add(line);
                }
            }
        }

        long endTime = System.currentTimeMillis();
        logger.info("时间去重完成 - 原始行数: {}, 去重后: {}, 重复行数: {}, 耗时: {}ms",
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
     * 生成时间键
     */
    private String generateTimeKey(LocalDateTime time, int timeWindowSeconds) {
        if (time == null) return "NO_TIME";

        long epochSecond = time.toEpochSecond(java.time.ZoneOffset.UTC);
        long timeBucket = epochSecond / timeWindowSeconds;
        return "TIME_BUCKET_" + timeBucket;
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
     * 时间组封装类
     */
    private static class TimeGroup {
        private final List<String> lines;
        private LocalDateTime latestTime;

        public TimeGroup(String firstLine, LocalDateTime firstTime) {
            this.lines = new ArrayList<>();
            this.lines.add(firstLine);
            this.latestTime = firstTime;
        }

        public void addLine(String line, LocalDateTime time) {
            lines.add(line);
            if (time != null && (latestTime == null || time.isAfter(latestTime))) {
                latestTime = time;
            }
        }

        public LocalDateTime getLatestTime() { return latestTime; }
        public List<String> getLines() { return lines; }
    }
}
