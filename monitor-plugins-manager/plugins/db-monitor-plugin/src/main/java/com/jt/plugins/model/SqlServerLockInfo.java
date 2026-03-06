package com.jt.plugins.model;

import com.alibaba.fastjson.JSONObject;

/**
 * SQL Server 锁信息
 */
public class SqlServerLockInfo {
    private int requestSessionId;
    private String resourceType;
    private String resourceDescription;
    private String requestMode;
    private String requestStatus;
    private String databaseName;
    private String objectName;
    private Long waitDuration;
    private String sqlText;
    
    // Getters and Setters
    public int getRequestSessionId() { return requestSessionId; }
    public void setRequestSessionId(int requestSessionId) { this.requestSessionId = requestSessionId; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getResourceDescription() { return resourceDescription; }
    public void setResourceDescription(String resourceDescription) { this.resourceDescription = resourceDescription; }
    public String getRequestMode() { return requestMode; }
    public void setRequestMode(String requestMode) { this.requestMode = requestMode; }
    public String getRequestStatus() { return requestStatus; }
    public void setRequestStatus(String requestStatus) { this.requestStatus = requestStatus; }
    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
    public String getObjectName() { return objectName; }
    public void setObjectName(String objectName) { this.objectName = objectName; }
    public Long getWaitDuration() { return waitDuration; }
    public void setWaitDuration(Long waitDuration) { this.waitDuration = waitDuration; }
    public String getSqlText() { return sqlText; }
    public void setSqlText(String sqlText) { this.sqlText = sqlText; }
    
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("requestSessionId", requestSessionId);
        json.put("resourceType", resourceType);
        json.put("resourceDescription", resourceDescription);
        json.put("requestMode", requestMode);
        json.put("requestStatus", requestStatus);
        json.put("databaseName", databaseName);
        json.put("objectName", objectName);
        json.put("waitDuration", waitDuration);
        json.put("sqlText", sqlText);
        return json;
    }
}
