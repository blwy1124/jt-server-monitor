package com.jt.plugins.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * SQL Server 监控数据 Mapper 接口
 */
@Mapper
public interface SqlServerMetricsMapper {
    
    // ========== 汇总统计 ==========
    Map<String, Object> getProcessInfo();
    Map<String, Object> getWaitStats();
    List<Map<String, Object>> getPerformanceCounters();
    Map<String, Object> getMemoryStats();
    Map<String, Object> getIOStats();
    Double getCpuUsagePercent();
    Integer getActiveTransactions();
    
    // ========== 详细信息（新增） ==========
    
    /**
     * 获取进程详细信息列表
     */
    List<Map<String, Object>> getProcessDetails();
    
    /**
     * 获取锁信息列表
     */
    List<Map<String, Object>> getLockInformation();
    
    /**
     * 获取指定会话的 SQL 语句
     */
    Map<String, Object> getSessionSqlText(Integer sessionId);
    
    /**
     * 获取阻塞链信息
     */
    List<Map<String, Object>> getBlockingChain();
    
    /**
     * 获取最耗资源的 SQL TOP 10
     */
    List<Map<String, Object>> getTopResourceConsumingSql();
    
    /**
     * 获取活动连接列表
     */
    List<Map<String, Object>> getActiveConnections();
    
    /**
     * 按程序名称和 SQL 分组统计连接
     */
    List<Map<String, Object>> getConnectionsGroupedByProgramAndSql();
}
