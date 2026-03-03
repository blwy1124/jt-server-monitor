// 文件路径: D:\IdeaProject\jt-server-monitor\monitor-plugins-manager\plugins\db-monitor-plugin\src\main\java\com\jt\plugin\mapper\SqlServerMetricsMapper.java

package com.jt.plugins.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * SQL Server监控数据Mapper接口
 */
@Mapper
public interface SqlServerMetricsMapper {
    
    /**
     * 获取进程信息统计
     */
    Map<String, Object> getProcessInfo();
    
    /**
     * 获取等待统计信息
     */
    Map<String, Object> getWaitStats();
    
    /**
     * 获取性能计数器数据
     */
    List<Map<String, Object>> getPerformanceCounters();
    
    /**
     * 获取内存使用统计
     */
    Map<String, Object> getMemoryStats();
    
    /**
     * 获取I/O统计信息
     */
    Map<String, Object> getIOStats();
    
    /**
     * 获取CPU使用率（通过活动请求估算）
     */
    Double getCpuUsagePercent();
    
    /**
     * 获取活跃事务数
     */
    Integer getActiveTransactions();
}
