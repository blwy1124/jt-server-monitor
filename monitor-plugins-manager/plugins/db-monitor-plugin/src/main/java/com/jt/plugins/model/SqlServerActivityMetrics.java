package com.jt.plugins.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL Server 活动监视器完整指标
 */
public class SqlServerActivityMetrics {
    
    // 采集时间
    private long collectionTime;
    
    // ========== 汇总统计（原有字段） ==========
    private int totalProcesses;
    private int runningProcesses;
    private int suspendedProcesses;
    private int blockedProcesses;
    
    private int totalWaits;
    private double averageWaitTime;
    
    private int batchRequestsPerSec;
    private int sqlCompilationsPerSec;
    private int sqlRecompilationsPerSec;
    private double bufferCacheHitRatio;
    private long pageLifeExpectancy;
    
    private long memoryUsedMB;
    private long memoryAvailableMB;
    
    private long diskReadsPerSec;
    private long diskWritesPerSec;
    private long networkIOBytesPerSec;
    
    private double cpuUsagePercent;
    private int activeTransactions;
    
    // ========== 新增：详细信息列表 ==========
    private List<SqlServerProcessDetail> processDetails = new ArrayList<>();
    private List<SqlServerLockInfo> lockInfos = new ArrayList<>();
    
    // Getters and Setters for all fields
    public long getCollectionTime() { return collectionTime; }
    public void setCollectionTime(long collectionTime) { this.collectionTime = collectionTime; }
    
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
    
    public double getCpuUsagePercent() { return cpuUsagePercent; }
    public void setCpuUsagePercent(double cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }
    public int getActiveTransactions() { return activeTransactions; }
    public void setActiveTransactions(int activeTransactions) { this.activeTransactions = activeTransactions; }
    
    public List<SqlServerProcessDetail> getProcessDetails() { return processDetails; }
    public void setProcessDetails(List<SqlServerProcessDetail> processDetails) { this.processDetails = processDetails; }
    
    public List<SqlServerLockInfo> getLockInfos() { return lockInfos; }
    public void setLockInfos(List<SqlServerLockInfo> lockInfos) { this.lockInfos = lockInfos; }
    
    /**
     * 转换为 JSON 对象
     */
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        
        // 基础字段
        json.put("collectionTime", collectionTime);
        json.put("totalProcesses", totalProcesses);
        json.put("runningProcesses", runningProcesses);
        json.put("suspendedProcesses", suspendedProcesses);
        json.put("blockedProcesses", blockedProcesses);
        json.put("totalWaits", totalWaits);
        json.put("averageWaitTime", averageWaitTime);
        json.put("batchRequestsPerSec", batchRequestsPerSec);
        json.put("sqlCompilationsPerSec", sqlCompilationsPerSec);
        json.put("sqlRecompilationsPerSec", sqlRecompilationsPerSec);
        json.put("bufferCacheHitRatio", bufferCacheHitRatio);
        json.put("pageLifeExpectancy", pageLifeExpectancy);
        json.put("memoryUsedMB", memoryUsedMB);
        json.put("memoryAvailableMB", memoryAvailableMB);
        json.put("diskReadsPerSec", diskReadsPerSec);
        json.put("diskWritesPerSec", diskWritesPerSec);
        json.put("networkIOBytesPerSec", networkIOBytesPerSec);
        json.put("cpuUsagePercent", cpuUsagePercent);
        json.put("activeTransactions", activeTransactions);
        
        // 新增：详细信息
        JSONArray processDetailsArray = new JSONArray();
        if (processDetails != null) {
            for (SqlServerProcessDetail detail : processDetails) {
                processDetailsArray.add(detail.toJSON());
            }
        }
        json.put("processDetails", processDetailsArray);
        
        JSONArray lockInfoArray = new JSONArray();
        if (lockInfos != null) {
            for (SqlServerLockInfo lockInfo : lockInfos) {
                lockInfoArray.add(lockInfo.toJSON());
            }
        }
        json.put("lockInfos", lockInfoArray);
        
        return json;
    }
}
