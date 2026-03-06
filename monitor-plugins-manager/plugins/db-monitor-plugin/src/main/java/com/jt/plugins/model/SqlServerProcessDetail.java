package com.jt.plugins.model;

import com.alibaba.fastjson.JSONObject;

/**
 * SQL Server 进程详情
 */
public class SqlServerProcessDetail {
    private int sessionId;
    private String loginName;
    private String hostName;
    private String programName;
    private String databaseName;
    private String status;
    private String command;
    private long cpuTime;
    private long logicalReads;
    private long writes;
    private long elapsedTime;
    private String lastSqlText;
    private Long blockingSessionId;
    private String waitType;
    private Long waitTime;
    private String startTime;
    
    // Getters and Setters
    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }
    public String getLoginName() { return loginName; }
    public void setLoginName(String loginName) { this.loginName = loginName; }
    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }
    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }
    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public long getCpuTime() { return cpuTime; }
    public void setCpuTime(long cpuTime) { this.cpuTime = cpuTime; }
    public long getLogicalReads() { return logicalReads; }
    public void setLogicalReads(long logicalReads) { this.logicalReads = logicalReads; }
    public long getWrites() { return writes; }
    public void setWrites(long writes) { this.writes = writes; }
    public long getElapsedTime() { return elapsedTime; }
    public void setElapsedTime(long elapsedTime) { this.elapsedTime = elapsedTime; }
    public String getLastSqlText() { return lastSqlText; }
    public void setLastSqlText(String lastSqlText) { this.lastSqlText = lastSqlText; }
    public Long getBlockingSessionId() { return blockingSessionId; }
    public void setBlockingSessionId(Long blockingSessionId) { this.blockingSessionId = blockingSessionId; }
    public String getWaitType() { return waitType; }
    public void setWaitType(String waitType) { this.waitType = waitType; }
    public Long getWaitTime() { return waitTime; }
    public void setWaitTime(Long waitTime) { this.waitTime = waitTime; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("sessionId", sessionId);
        json.put("loginName", loginName);
        json.put("hostName", hostName);
        json.put("programName", programName);
        json.put("databaseName", databaseName);
        json.put("status", status);
        json.put("command", command);
        json.put("cpuTime", cpuTime);
        json.put("logicalReads", logicalReads);
        json.put("writes", writes);
        json.put("elapsedTime", elapsedTime);
        json.put("lastSqlText", lastSqlText);
        json.put("blockingSessionId", blockingSessionId);
        json.put("waitType", waitType);
        json.put("waitTime", waitTime);
        json.put("startTime", startTime);
        return json;
    }
}
