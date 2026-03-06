// 文件路径: D:\IdeaProject\jt-server-monitor\monitor-plugins-manager\plugins\db-monitor-plugin\src\main\java\com\jt\plugin\collector\SqlServerMetricsCollector.java

package com.jt.plugins.collector;

import com.jt.plugins.mapper.SqlServerMetricsMapper;
import com.jt.plugins.model.SqlServerActivityMetrics;
import com.jt.plugins.config.MyBatisConfig;
import com.jt.plugins.common.log.PluginLogger;
import com.jt.plugins.model.SqlServerLockInfo;
import com.jt.plugins.model.SqlServerProcessDetail;
import org.apache.ibatis.session.SqlSession;

import javax.sql.DataSource;
import java.util.ArrayList;
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
            } else {
                metrics.setTotalProcesses(-1);
                metrics.setRunningProcesses(-1);
                metrics.setSuspendedProcesses(-1);
                metrics.setBlockedProcesses(-1);
                logger.error("采集进程信息失败：返回结果为空");
            }
        } catch (Exception e) {
            metrics.setTotalProcesses(-1);
            metrics.setRunningProcesses(-1);
            metrics.setSuspendedProcesses(-1);
            metrics.setBlockedProcesses(-1);
            logger.error("采集进程信息失败", e);
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
            } else {
                metrics.setTotalWaits(-1);
                metrics.setAverageWaitTime(-1.0);
                logger.error("采集等待统计失败：返回结果为空");
            }
        } catch (Exception e) {
            metrics.setTotalWaits(-1);
            metrics.setAverageWaitTime(-1.0);
            logger.error("采集等待统计失败", e);
        }
    }

    /**
     * 采集性能计数器
     */
    private void collectPerformanceCounters(SqlServerMetricsMapper mapper, SqlServerActivityMetrics metrics) {
        try {
            List<Map<String, Object>> counters = mapper.getPerformanceCounters();
            if (counters != null && !counters.isEmpty()) {
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
                            metrics.setBufferCacheHitRatio(value / 100.0);
                            break;
                        case "Page life expectancy":
                            metrics.setPageLifeExpectancy(value);
                            break;
                    }
                }
            } else {
                metrics.setBatchRequestsPerSec(-1);
                metrics.setSqlCompilationsPerSec(-1);
                metrics.setSqlRecompilationsPerSec(-1);
                metrics.setBufferCacheHitRatio(-1.0);
                metrics.setPageLifeExpectancy(-1);
                logger.error("采集性能计数器失败：返回结果为空");
            }
        } catch (Exception e) {
            metrics.setBatchRequestsPerSec(-1);
            metrics.setSqlCompilationsPerSec(-1);
            metrics.setSqlRecompilationsPerSec(-1);
            metrics.setBufferCacheHitRatio(-1.0);
            metrics.setPageLifeExpectancy(-1);
            logger.error("采集性能计数器失败", e);
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
            } else {
                metrics.setMemoryUsedMB(-1);
                metrics.setMemoryAvailableMB(-1);
                logger.error("采集内存统计失败：返回结果为空");
            }
        } catch (Exception e) {
            metrics.setMemoryUsedMB(-1);
            metrics.setMemoryAvailableMB(-1);
            logger.error("采集内存统计失败", e);
        }
    }

    /**
     * 采集 I/O 统计
     */
    private void collectIOStats(SqlServerMetricsMapper mapper, SqlServerActivityMetrics metrics) {
        try {
            Map<String, Object> ioStats = mapper.getIOStats();
            if (ioStats != null) {
                metrics.setDiskReadsPerSec(getLongValue(ioStats, "disk_reads"));
                metrics.setDiskWritesPerSec(getLongValue(ioStats, "disk_writes"));
            } else {
                metrics.setDiskReadsPerSec(-1);
                metrics.setDiskWritesPerSec(-1);
                logger.error("采集 I/O 统计失败：返回结果为空");
            }
        } catch (Exception e) {
            metrics.setDiskReadsPerSec(-1);
            metrics.setDiskWritesPerSec(-1);
            logger.error("采集 I/O 统计失败", e);
        }
    }

    /**
     * 采集额外指标
     */
    private void collectAdditionalMetrics(SqlServerMetricsMapper mapper, SqlServerActivityMetrics metrics) {
        try {
            // CPU 使用率
            Double cpuUsage = mapper.getCpuUsagePercent();
            if (cpuUsage != null) {
                metrics.setCpuUsagePercent(cpuUsage);
            } else {
                metrics.setCpuUsagePercent(-1.0);
                logger.error("采集 CPU 使用率失败：返回结果为空");
            }

            // 活跃事务数
            Integer activeTransactions = mapper.getActiveTransactions();
            if (activeTransactions != null) {
                metrics.setActiveTransactions(activeTransactions);
            } else {
                metrics.setActiveTransactions(-1);
                logger.error("采集活跃事务数失败：返回结果为空");
            }

            // 采集进程详细信息
            collectProcessDetails(mapper, metrics);

            // 采集锁信息
            collectLockInformation(mapper, metrics);

        } catch (Exception e) {
            metrics.setCpuUsagePercent(-1.0);
            metrics.setActiveTransactions(-1);
            logger.error("采集额外指标失败", e);
        }
    }

    /**
     * 采集进程详细信息
     */
    private void collectProcessDetails(SqlServerMetricsMapper mapper, SqlServerActivityMetrics metrics) {
        try {
            List<Map<String, Object>> details = mapper.getProcessDetails();
            logger.info("进程详情查询结果：{} 条", details == null ? "null" : details.size());

            if (details != null && !details.isEmpty()) {
                List<SqlServerProcessDetail> processDetails = new ArrayList<>();
                for (Map<String, Object> row : details) {
                    SqlServerProcessDetail detail = new SqlServerProcessDetail();
                    try {
                        detail.setSessionId(getIntValue(row, "session_id"));
                        detail.setLoginName(getStringValue(row, "login_name"));
                        detail.setHostName(getStringValue(row, "host_name"));
                        detail.setProgramName(getStringValue(row, "program_name"));
                        detail.setDatabaseName(getStringValue(row, "database_name"));
                        detail.setStatus(getStringValue(row, "status"));
                        detail.setCommand(getStringValue(row, "command"));
                        detail.setCpuTime(getLongValue(row, "cpu_time"));
                        detail.setLogicalReads(getLongValue(row, "logical_reads"));
                        detail.setWrites(getLongValue(row, "writes"));
                        detail.setElapsedTime(getLongValue(row, "elapsed_time"));
                        detail.setLastSqlText(getStringValue(row, "last_sql_text"));
                        detail.setStartTime(getStringValue(row, "start_time"));

                        Object blockingId = row.get("blocking_session_id");
                        if (blockingId instanceof Number) {
                            detail.setBlockingSessionId(((Number) blockingId).longValue());
                        }

                        Object waitType = row.get("wait_type");
                        if (waitType != null) {
                            detail.setWaitType(waitType.toString());
                        }

                        Object waitTime = row.get("wait_time");
                        if (waitTime instanceof Number) {
                            detail.setWaitTime(((Number) waitTime).longValue());
                        }

                        processDetails.add(detail);
                        logger.debug("成功添加进程详情：sessionId={}, program={}, sql={}",
                                detail.getSessionId(), detail.getProgramName(),
                                detail.getLastSqlText().substring(0, Math.min(50, detail.getLastSqlText().length())) + "...");
                    } catch (Exception e) {
                        logger.warn("处理单个进程详情时出错：row={}, error={}", row, e.getMessage());
                    }

                }
                metrics.setProcessDetails(processDetails);
                logger.info("成功采集到{}个进程的详细信息", processDetails.size());
            } else {
                logger.warn("未采集到进程详细信息，可能是查询结果为空或 SQL 执行失败");
            }
        } catch (Exception e) {
            logger.error("采集进程详细信息失败", e);
        }
    }

    /**
     * 采集锁信息
     */
    private void collectLockInformation(SqlServerMetricsMapper mapper, SqlServerActivityMetrics metrics) {
        try {
            List<Map<String, Object>> locks = mapper.getLockInformation();
            logger.info("锁信息查询结果：{} 条", locks == null ? "null" : locks.size());

            if (locks != null && !locks.isEmpty()) {
                List<SqlServerLockInfo> lockInfos = new ArrayList<>();
                for (Map<String, Object> row : locks) {
                    SqlServerLockInfo lockInfo = new SqlServerLockInfo();
                    try {
                        lockInfo.setRequestSessionId(getIntValue(row, "request_session_id"));
                        lockInfo.setResourceType(getStringValue(row, "resource_type"));
                        lockInfo.setResourceDescription(getStringValue(row, "resource_description"));
                        lockInfo.setRequestMode(getStringValue(row, "request_mode"));
                        lockInfo.setRequestStatus(getStringValue(row, "request_status"));
                        lockInfo.setDatabaseName(getStringValue(row, "database_name"));
                        lockInfo.setObjectName(getStringValue(row, "object_name"));
                        lockInfo.setSqlText(getStringValue(row, "sql_text"));

                        Object waitDuration = row.get("wait_duration_ms");
                        if (waitDuration instanceof Number) {
                            lockInfo.setWaitDuration(((Number) waitDuration).longValue());
                        }

                        lockInfos.add(lockInfo);
                        logger.debug("成功添加锁信息：sessionId={}, resource={}, sql={}",
                                lockInfo.getRequestSessionId(), lockInfo.getResourceType(),
                                lockInfo.getSqlText().substring(0, Math.min(50, lockInfo.getSqlText().length())) + "...");
                    } catch (Exception e) {
                        logger.warn("处理单个锁信息时出错：row={}, error={}", row, e.getMessage());
                    }

                }
                metrics.setLockInfos(lockInfos);
                logger.info("成功采集到{}个锁的信息", lockInfos.size());
            } else {
                logger.info("当前无活跃锁（这是正常状态）");
            }
        } catch (Exception e) {
            logger.error("采集锁信息失败", e);
        }
    }

    /**
     * 直接获取进程详细信息（独立调用）
     */
    public List<Map<String, Object>> getProcessDetailsDirectly() {
        SqlSession sqlSession = null;
        try {
            sqlSession = MyBatisConfig.getSqlSession();
            SqlServerMetricsMapper mapper = sqlSession.getMapper(SqlServerMetricsMapper.class);
            List<Map<String, Object>> details = mapper.getProcessDetails();
            logger.info("直接查询进程详情：{} 条", details == null ? 0 : details.size());
            return details;
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
    }

    /**
     * 直接获取锁信息（独立调用）
     */
    public List<Map<String, Object>> getLockInformationDirectly() {
        SqlSession sqlSession = null;
        try {
            sqlSession = MyBatisConfig.getSqlSession();
            SqlServerMetricsMapper mapper = sqlSession.getMapper(SqlServerMetricsMapper.class);
            List<Map<String, Object>> locks = mapper.getLockInformation();
            logger.info("直接查询锁信息：{} 条", locks == null ? 0 : locks.size());
            return locks;
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
    }

    /**
     * 直接获取阻塞链信息（独立调用）
     */
    public List<Map<String, Object>> getBlockingChainDirectly() {
        SqlSession sqlSession = null;
        try {
            sqlSession = MyBatisConfig.getSqlSession();
            SqlServerMetricsMapper mapper = sqlSession.getMapper(SqlServerMetricsMapper.class);
            List<Map<String, Object>> chain = mapper.getBlockingChain();
            logger.info("直接查询阻塞链：{} 条", chain == null ? 0 : chain.size());
            return chain;
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
    }

    /**
     * 直接获取最耗资源的 SQL（独立调用）
     */
    public List<Map<String, Object>> getTopResourceConsumingSqlDirectly() {
        SqlSession sqlSession = null;
        try {
            sqlSession = MyBatisConfig.getSqlSession();
            SqlServerMetricsMapper mapper = sqlSession.getMapper(SqlServerMetricsMapper.class);
            List<Map<String, Object>> topSql = mapper.getTopResourceConsumingSql();
            logger.info("直接查询最耗资源 SQL: {} 条", topSql == null ? 0 : topSql.size());
            return topSql;
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
    }

    /**
     * 直接获取活动连接列表（独立调用）
     */
    public List<Map<String, Object>> getActiveConnectionsDirectly() {
        SqlSession sqlSession = null;
        try {
            sqlSession = MyBatisConfig.getSqlSession();
            SqlServerMetricsMapper mapper = sqlSession.getMapper(SqlServerMetricsMapper.class);
            List<Map<String, Object>> connections = mapper.getActiveConnections();
            logger.info("直接查询活动连接：{} 条", connections == null ? 0 : connections.size());
            return connections;
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
        }
    }

    /**
     * 直接获取按程序和 SQL 分组的连接统计（独立调用）
     */
    public List<Map<String, Object>> getConnectionsGroupedByProgramAndSqlDirectly() {
        SqlSession sqlSession = null;
        try {
            sqlSession = MyBatisConfig.getSqlSession();
            SqlServerMetricsMapper mapper = sqlSession.getMapper(SqlServerMetricsMapper.class);
            List<Map<String, Object>> groupedStats = mapper.getConnectionsGroupedByProgramAndSql();
            logger.info("直接查询分组连接统计：{} 组", groupedStats == null ? 0 : groupedStats.size());
            return groupedStats;
        } finally {
            if (sqlSession != null) {
                sqlSession.close();
            }
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
