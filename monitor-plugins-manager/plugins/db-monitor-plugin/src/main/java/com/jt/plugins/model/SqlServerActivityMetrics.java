// 文件路径: D:\IdeaProject\jt-server-monitor\monitor-plugins-manager\plugins\db-monitor-plugin\src\main\java\com\jt\plugin\model\SqlServerActivityMetrics.java

package com.jt.plugins.model;

import com.alibaba.fastjson.JSONObject;

/**
 * SQL Server活动监视器指标数据模型
 */
public class SqlServerActivityMetrics {
    
    // 进程信息
    private int totalProcesses;
    private int runningProcesses;
    private int suspendedProcesses;
    private int blockedProcesses;
    
    // 等待统计
    private int totalWaits;
    private double averageWaitTime;
    
    // CPU使用率
    private double cpuUsagePercent;
    
    // 内存使用
    private long memoryUsedMB;
    private long memoryAvailableMB;
    
    // I/O统计
    private long diskReadsPerSec;
    private long diskWritesPerSec;
    private long networkIOBytesPerSec;
    
    // 数据库统计
    private int activeTransactions;
    private int batchRequestsPerSec;
    private int sqlCompilationsPerSec;
    private int sqlRecompilationsPerSec;
    
    // 缓冲池统计
    private double bufferCacheHitRatio;
    private long pageLifeExpectancy;
    
    // 时间戳
    private long collectionTime;
    
    // Getters and Setters
    public int getTotalProcesses() { return totalProcesses; }
    public void setTotalProcesses(int totalProcesses) { this.totalProcesses = totalProcesses; }
    
    public int getRunningProcesses() { return runningProcesses; }
    public void setRunningProcesses(int runningProcesses) { this.runningProcesses = runningProcesses; }
    
    public int getSuspendedProcesses() { return suspendedProcesses; }
    public void setSuspendedProcesses(int suspendedProcesses) { this.suspendedProcesses = suspendedProcesses; }
    
    public int getBlockedProcesses() { return blockedProcesses; }
    public void setBlockedProcesses(int blockedProcesses) { this.blockedProcesses = blockedProcesses; }
    
    public int getTotalWaits() { return totalWaits; }
    public void setTotalWaits(int totalWaits) { this.totalWaits = totalWaits; }
    
    public double getAverageWaitTime() { return averageWaitTime; }
    public void setAverageWaitTime(double averageWaitTime) { this.averageWaitTime = averageWaitTime; }
    
    public double getCpuUsagePercent() { return cpuUsagePercent; }
    public void setCpuUsagePercent(double cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }
    
    public long getMemoryUsedMB() { return memoryUsedMB; }
    public void setMemoryUsedMB(long memoryUsedMB) { this.memoryUsedMB = memoryUsedMB; }
    
    public long getMemoryAvailableMB() { return memoryAvailableMB; }
    public void setMemoryAvailableMB(long memoryAvailableMB) { this.memoryAvailableMB = memoryAvailableMB; }
    
    public long getDiskReadsPerSec() { return diskReadsPerSec; }
    public void setDiskReadsPerSec(long diskReadsPerSec) { this.diskReadsPerSec = diskReadsPerSec; }
    
    public long getDiskWritesPerSec() { return diskWritesPerSec; }
    public void setDiskWritesPerSec(long diskWritesPerSec) { this.diskWritesPerSec = diskWritesPerSec; }
    
    public long getNetworkIOBytesPerSec() { return networkIOBytesPerSec; }
    public void setNetworkIOBytesPerSec(long networkIOBytesPerSec) { this.networkIOBytesPerSec = networkIOBytesPerSec; }
    
    public int getActiveTransactions() { return activeTransactions; }
    public void setActiveTransactions(int activeTransactions) { this.activeTransactions = activeTransactions; }
    
    public int getBatchRequestsPerSec() { return batchRequestsPerSec; }
    public void setBatchRequestsPerSec(int batchRequestsPerSec) { this.batchRequestsPerSec = batchRequestsPerSec; }
    
    public int getSqlCompilationsPerSec() { return sqlCompilationsPerSec; }
    public void setSqlCompilationsPerSec(int sqlCompilationsPerSec) { this.sqlCompilationsPerSec = sqlCompilationsPerSec; }
    
    public int getSqlRecompilationsPerSec() { return sqlRecompilationsPerSec; }
    public void setSqlRecompilationsPerSec(int sqlRecompilationsPerSec) { this.sqlRecompilationsPerSec = sqlRecompilationsPerSec; }
    
    public double getBufferCacheHitRatio() { return bufferCacheHitRatio; }
    public void setBufferCacheHitRatio(double bufferCacheHitRatio) { this.bufferCacheHitRatio = bufferCacheHitRatio; }
    
    public long getPageLifeExpectancy() { return pageLifeExpectancy; }
    public void setPageLifeExpectancy(long pageLifeExpectancy) { this.pageLifeExpectancy = pageLifeExpectancy; }
    
    public long getCollectionTime() { return collectionTime; }
    public void setCollectionTime(long collectionTime) { this.collectionTime = collectionTime; }
    
    /**
     * 转换为JSON对象
     */
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("totalProcesses", totalProcesses);
        json.put("runningProcesses", runningProcesses);
        json.put("suspendedProcesses", suspendedProcesses);
        json.put("blockedProcesses", blockedProcesses);
        json.put("totalWaits", totalWaits);
        json.put("averageWaitTime", averageWaitTime);
        json.put("cpuUsagePercent", cpuUsagePercent);
        json.put("memoryUsedMB", memoryUsedMB);
        json.put("memoryAvailableMB", memoryAvailableMB);
        json.put("diskReadsPerSec", diskReadsPerSec);
        json.put("diskWritesPerSec", diskWritesPerSec);
        json.put("networkIOBytesPerSec", networkIOBytesPerSec);
        json.put("activeTransactions", activeTransactions);
        json.put("batchRequestsPerSec", batchRequestsPerSec);
        json.put("sqlCompilationsPerSec", sqlCompilationsPerSec);
        json.put("sqlRecompilationsPerSec", sqlRecompilationsPerSec);
        json.put("bufferCacheHitRatio", bufferCacheHitRatio);
        json.put("pageLifeExpectancy", pageLifeExpectancy);
        json.put("collectionTime", collectionTime);
        return json;
    }
}
