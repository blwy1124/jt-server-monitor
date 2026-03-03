// 文件路径: D:\IdeaProject\jt-server-monitor\monitor-plugins-manager\plugins\db-monitor-plugin\src\main\java\com\jt\plugin\collector\SqlServerMetricsCollector.java

package com.jt.plugins.utils;

import com.jt.plugins.mapper.SqlServerMetricsMapper;
import com.jt.plugins.model.SqlServerActivityMetrics;
import com.jt.plugins.config.MyBatisConfig;
import com.jt.plugins.common.log.PluginLogger;
import org.apache.ibatis.session.SqlSession;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * SQL Server监控数据采集器（XML版本）
 */
public class SqlServerMetricsCollector {

    private static final PluginLogger logger = PluginLogger.getLogger("db-monitor-plugin");
    private DataSource dataSource;

    public SqlServerMetricsCollector(DataSource dataSource) {
        this.dataSource = dataSource;
        // 初始化MyBatis配置
        MyBatisConfig.initialize(dataSource);
    }

    /**
     * 采集SQL Server活动监视器数据
     * @return 活动监视器指标
     */
    public SqlServerActivityMetrics collectActivityMetrics() {
        SqlServerActivityMetrics metrics = new SqlServerActivityMetrics();
        metrics.setCollectionTime(System.currentTimeMillis());

        SqlSession sqlSession = null;
        try {
            sqlSession = MyBatisConfig.getSqlSession();
            SqlServerMetricsMapper mapper = sqlSession.getMapper(SqlServerMetricsMapper.class);

            // 采集各种指标
            collectProcessInfo(mapper, metrics);
            collectWaitStats(mapper, metrics);
            collectPerformanceCounters(mapper, metrics);
            collectMemoryStats(mapper, metrics);
            collectIOStats(mapper, metrics);
            collectAdditionalMetrics(mapper, metrics);

            logger.debug("SQL Server活动监视器数据采集完成");

        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }

        return metrics;
    }

    /**
     * 采集进程信息
     */
    private void collectProcessInfo(SqlServerMetricsMapper mapper, SqlServerActivityMetrics metrics) {
        try {
            Map<String, Object> processInfo = mapper.getProcessInfo();
            if (processInfo != null) {
                metrics.setTotalProcesses(getIntValue(processInfo, "total_processes"));
                metrics.setRunningProcesses(getIntValue(processInfo, "running_processes"));
                metrics.setSuspendedProcesses(getIntValue(processInfo, "suspended_processes"));
                metrics.setBlockedProcesses(getIntValue(processInfo, "blocked_processes"));
            }
        } catch (Exception e) {
            logger.warn("采集进程信息失败", e);
        }
    }

    /**
     * 采集等待统计
     */
    private void collectWaitStats(SqlServerMetricsMapper mapper, SqlServerActivityMetrics metrics) {
        try {
            Map<String, Object> waitStats = mapper.getWaitStats();
            if (waitStats != null) {
                metrics.setTotalWaits(getIntValue(waitStats, "total_waits"));
                metrics.setAverageWaitTime(getDoubleValue(waitStats, "avg_wait_time"));
            }
        } catch (Exception e) {
            logger.warn("采集等待统计失败", e);
        }
    }

    /**
     * 采集性能计数器
     */
    private void collectPerformanceCounters(SqlServerMetricsMapper mapper, SqlServerActivityMetrics metrics) {
        try {
            List<Map<String, Object>> counters = mapper.getPerformanceCounters();
            if (counters != null) {
                for (Map<String, Object> counter : counters) {
                    String counterName = getStringValue(counter, "counter_name");
                    long value = getLongValue(counter, "cntr_value");

                    switch (counterName) {
                        case "Batch Requests/sec":
                            metrics.setBatchRequestsPerSec((int) value);
                            break;
                        case "SQL Compilations/sec":
                            metrics.setSqlCompilationsPerSec((int) value);
                            break;
                        case "SQL Re-Compilations/sec":
                            metrics.setSqlRecompilationsPerSec((int) value);
                            break;
                        case "Buffer cache hit ratio":
                            metrics.setBufferCacheHitRatio(value / 100.0); // 转换为百分比
                            break;
                        case "Page life expectancy":
                            metrics.setPageLifeExpectancy(value);
                            break;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("采集性能计数器失败", e);
        }
    }

    /**
     * 采集内存统计
     */
    private void collectMemoryStats(SqlServerMetricsMapper mapper, SqlServerActivityMetrics metrics) {
        try {
            Map<String, Object> memoryStats = mapper.getMemoryStats();
            if (memoryStats != null) {
                metrics.setMemoryUsedMB(getLongValue(memoryStats, "memory_used_mb"));
                metrics.setMemoryAvailableMB(getLongValue(memoryStats, "memory_available_mb"));
            }
        } catch (Exception e) {
            logger.warn("采集内存统计失败", e);
        }
    }

    /**
     * 采集I/O统计
     */
    private void collectIOStats(SqlServerMetricsMapper mapper, SqlServerActivityMetrics metrics) {
        try {
            Map<String, Object> ioStats = mapper.getIOStats();
            if (ioStats != null) {
                metrics.setDiskReadsPerSec(getLongValue(ioStats, "disk_reads"));
                metrics.setDiskWritesPerSec(getLongValue(ioStats, "disk_writes"));
            }
        } catch (Exception e) {
            logger.warn("采集I/O统计失败", e);
        }
    }

    /**
     * 采集额外指标
     */
    private void collectAdditionalMetrics(SqlServerMetricsMapper mapper, SqlServerActivityMetrics metrics) {
        try {
            // CPU使用率
            Double cpuUsage = mapper.getCpuUsagePercent();
            if (cpuUsage != null) {
                metrics.setCpuUsagePercent(cpuUsage);
            }

            // 活跃事务数
            Integer activeTransactions = mapper.getActiveTransactions();
            if (activeTransactions != null) {
                metrics.setActiveTransactions(activeTransactions);
            }
        } catch (Exception e) {
            logger.warn("采集额外指标失败", e);
        }
    }

    // 工具方法
    private int getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
}
