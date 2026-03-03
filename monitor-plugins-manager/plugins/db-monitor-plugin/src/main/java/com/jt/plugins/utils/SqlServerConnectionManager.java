// 文件路径: D:\IdeaProject\jt-server-monitor\monitor-plugins-manager\plugins\db-monitor-plugin\src\main\java\com\jt\plugin\sqlserver\SqlServerConnectionManager.java

package com.jt.plugins.utils;

import com.jt.plugins.common.log.PluginLogger;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * SQL Server连接管理器（支持DataSource）
 */
public class SqlServerConnectionManager {
    
    private static final PluginLogger logger = PluginLogger.getLogger("db-monitor-plugin");
    
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private int connectionTimeout;
    private BasicDataSource dataSource;
    
    public SqlServerConnectionManager(String host, int port, String database, 
                                    String username, String password, int connectionTimeout) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.connectionTimeout = connectionTimeout;
        initializeDataSource();
    }
    
    /**
     * 初始化DataSource
     */
    private void initializeDataSource() {
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        dataSource.setUrl(String.format("jdbc:sqlserver://%s:%d;databaseName=%s;loginTimeout=%d;",
            host, port, database, connectionTimeout));
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setInitialSize(1);
        dataSource.setMaxTotal(10);
        dataSource.setMaxIdle(5);
        dataSource.setMinIdle(1);
        dataSource.setTestOnBorrow(true);
        dataSource.setValidationQuery("SELECT 1");
        
        logger.debug("DataSource初始化完成: {}:{}", host, port);
    }
    
    /**
     * 获取DataSource
     */
    public DataSource getDataSource() {
        return dataSource;
    }
    
    /**
     * 获取传统连接（向后兼容）
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    /**
     * 测试连接
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            logger.warn("SQL Server连接测试失败: {}:{}", host, port, e);
            return false;
        }
    }
    
    /**
     * 关闭DataSource
     */
    public void close() {
        if (dataSource != null) {
            try {
                dataSource.close();
                logger.debug("DataSource已关闭");
            } catch (SQLException e) {
                logger.warn("关闭DataSource时发生异常", e);
            }
        }
    }
    
    // Getters and Setters...
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }
}
