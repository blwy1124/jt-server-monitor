package com.jt.plugins.extension;

import com.alibaba.fastjson.JSONObject;
import com.jt.plugins.sqlserver.SqlServerConnectionManager;
import com.jt.plugins.collector.SqlServerMetricsCollector;
import com.jt.plugins.model.SqlServerActivityMetrics;  // 添加这个导入
import com.jt.plugins.common.annotation.ActionHandler;
import com.jt.plugins.common.file.PluginFileStorage;
import com.jt.plugins.common.http.ExtensionRequestParam;
import com.jt.plugins.common.log.PluginLogger;
import com.jt.plugins.common.result.ResultMsg;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugin.extension
 * @Author: 别来无恙qb
 * @CreateTime: 2026-03-02  15:15
 * @Description: 数据库监控插件实现类
 * @Version: 1.0
 */

public class DbMonitorProviderImpl implements DbMonitorProvider {
    // 插件日志记录器
    private static final PluginLogger logger = PluginLogger.getLogger("db-monitor-plugin");

    // 插件文件存储管理器
    private static final PluginFileStorage fileStorage = PluginFileStorage.getStorage("db-monitor-plugin");

    // 缓存方法映射，避免每次反射查找
    private static final Map<String, Method> ACTION_HANDLERS = new HashMap<>();

    // 在类加载时初始化方法映射
    static {
        Method[] methods = DbMonitorProviderImpl.class.getDeclaredMethods();
        for (Method method : methods) {
            ActionHandler annotation = method.getAnnotation(ActionHandler.class);
            if (annotation != null) {
                ACTION_HANDLERS.put(annotation.value(), method);
            }
        }
    }

    @Override
    public ResultMsg<JSONObject> execute(ExtensionRequestParam extensionRequestParam) {
        String targetAction = extensionRequestParam.getTargetAction();
        logger.info("开始执行数据库监控操作: {}", targetAction);

        // 参数校验
        if (targetAction == null || targetAction.isEmpty()) {
            logger.warn("操作类型不能为空");
            return ResultMsg.fail("操作类型不能为空");
        }

        // 根据targetAction查找对应的处理方法
        Method handlerMethod = ACTION_HANDLERS.get(targetAction);

        if (handlerMethod != null) {
            try {
                // 调用对应的方法处理请求
                logger.info("调用处理方法: {}", handlerMethod.getName());
                ResultMsg<JSONObject> result = (ResultMsg<JSONObject>) handlerMethod.invoke(this, extensionRequestParam);
                logger.info("数据库监控操作执行完成: {}", targetAction);
                return result;
            } catch (Exception e) {
                logger.error("执行数据库监控操作失败: {}", targetAction, e);
                // 区分不同类型的异常并抛出相应的错误信息
                Throwable cause = e.getCause();
                if (cause != null) {
                    if (cause instanceof SecurityException) {
                        return ResultMsg.fail("权限不足：" + cause.getMessage());
                    } else if (cause instanceof IllegalArgumentException) {
                        return ResultMsg.fail("参数错误：" + cause.getMessage());
                    } else if (cause instanceof IllegalStateException) {
                        return ResultMsg.fail("状态错误：" + cause.getMessage());
                    } else {
                        return ResultMsg.fail("执行操作失败：" + cause.getMessage());
                    }
                }
                e.printStackTrace();
                return ResultMsg.fail("执行操作失败：" + e.getMessage());
            }
        }

        logger.warn("不支持的操作类型: {}", targetAction);
        return ResultMsg.fail("不支持的操作类型：" + targetAction);
    }

    /**
     * 获取数据库活动监视器完整数据（汇总版）
     *
     * @param extensionRequestParam 请求参数（host, port, database, username, password）
     * @return 包含所有 SSMS 活动监视器指标的完整数据
     *
     * 返回数据结构说明：
     * {
     *   "totalProcesses": 13,              // 总进程数
     *   "runningProcesses": 1,             // 正在运行的进程数
     *   "suspendedProcesses": 0,           // 挂起等待的进程数
     *   "blockedProcesses": 0,             // 被阻塞的进程数
     *   "totalWaits": 1061,                // 总等待次数
     *   "averageWaitTime": 4772352.0,      // 平均等待时间 (毫秒)
     *   "batchRequestsPerSec": 0,          // 每秒批处理请求数
     *   "sqlCompilationsPerSec": 0,        // 每秒 SQL 编译次数
     *   "sqlRecompilationsPerSec": 0,      // 每秒 SQL 重新编译次数
     *   "bufferCacheHitRatio": 0.0,        // 缓冲缓存命中率 (%)
     *   "pageLifeExpectancy": 0,           // 页面预期生命周期 (秒)
     *   "memoryUsedMB": 128,               // SQL Server 已使用内存 (MB)
     *   "memoryAvailableMB": 540,          // 可用内存 (MB)
     *   "diskReadsPerSec": 740,            // 每秒磁盘读取次数
     *   "diskWritesPerSec": 6672,          // 每秒磁盘写入次数
     *   "cpuUsagePercent": 7.69,           // CPU 使用率 (%)
     *   "activeTransactions": 7,           // 活跃事务数
     *   "processDetails": [...],           // 进程详细信息列表（见 getProcessDetails 接口）
     *   "lockInfos": [...],                // 锁信息列表（见 getLockInformation 接口）
     *   "errorInfo": {                     // 错误信息对象
     *       "hasError": false,             // 是否有采集失败的项目
     *       "errorItems": "",              // 失败的项目名称
     *       "errorMessage": ""             // 错误描述
     *   }
     * }
     */
    @ActionHandler("getDbMetrics")
    public ResultMsg<JSONObject> getDbMetrics(ExtensionRequestParam extensionRequestParam) {
        logger.debug("开始获取 SQL Server 活动监视器数据");

        SqlServerConnectionManager connectionManager = null;
        try {
            // 获取数据库连接参数
            String host = extensionRequestParam.getParameter("host", "localhost");
            int port = Integer.parseInt(extensionRequestParam.getParameter("port", "1433"));
            String database = extensionRequestParam.getParameter("database", "master");
            String username = extensionRequestParam.getParameter("username");
            String password = extensionRequestParam.getParameter("password");
            int timeout = Integer.parseInt(extensionRequestParam.getParameter("timeout", "30"));

            // 参数验证
            if (username == null || username.isEmpty()) {
                return ResultMsg.fail("用户名不能为空");
            }
            if (password == null || password.isEmpty()) {
                return ResultMsg.fail("密码不能为空");
            }

            // 创建连接管理器
            connectionManager = new SqlServerConnectionManager(
                host, port, database, username, password, timeout);

            // 测试连接
            if (!connectionManager.testConnection()) {
                return ResultMsg.fail("无法连接到 SQL Server 数据库");
            }

            // 创建数据采集器并采集数据
            SqlServerMetricsCollector collector = new SqlServerMetricsCollector(connectionManager.getDataSource());
            SqlServerActivityMetrics metrics = collector.collectActivityMetrics();

            // 构建返回结果
            JSONObject resultData = metrics.toJSON();
            resultData.put("host", host);
            resultData.put("port", port);
            resultData.put("database", database);

            // 检查是否有采集失败的项目（值为 -1）
            StringBuilder errorItems = new StringBuilder();
            if (metrics.getTotalProcesses() == -1) {
                errorItems.append("进程信息 ");
            }
            if (metrics.getTotalWaits() == -1) {
                errorItems.append("等待统计 ");
            }
            if (metrics.getBatchRequestsPerSec() == -1) {
                errorItems.append("性能计数器 ");
            }
            if (metrics.getMemoryUsedMB() == -1) {
                errorItems.append("内存统计 ");
            }
            if (metrics.getDiskReadsPerSec() == -1) {
                errorItems.append("I/O 统计 ");
            }
            if (metrics.getCpuUsagePercent() == -1.0) {
                errorItems.append("CPU 使用率 ");
            }
            if (metrics.getActiveTransactions() == -1) {
                errorItems.append("活跃事务数 ");
            }

            // 将错误信息单独放在一个对象中
            JSONObject errorInfo = new JSONObject();
            if (errorItems.length() > 0) {
                errorInfo.put("hasError", true);
                errorInfo.put("errorItems", errorItems.toString().trim());
                errorInfo.put("errorMessage", "部分监控项采集失败（值为 -1），请查看日志获取详细信息");
                logger.warn("部分监控项采集失败：{}", errorItems.toString());
            } else {
                errorInfo.put("hasError", false);
            }

            // 添加到返回结果中
            resultData.put("errorInfo", errorInfo);

            String successMessage = String.format(
                "成功获取 SQL Server 活动监视器数据 - 进程数：%d, CPU 使用率：%.2f%%, 内存使用：%d MB",
                metrics.getTotalProcesses(), metrics.getCpuUsagePercent(), metrics.getMemoryUsedMB()
            );

            logger.info(successMessage);

            return ResultMsg.success(resultData, successMessage);

        } catch (NumberFormatException e) {
            logger.error("参数格式错误", e);
            return ResultMsg.fail("参数格式错误：" + e.getMessage());
        } catch (Exception e) {
            logger.error("获取数据库监控数据失败", e);
            return ResultMsg.fail("获取数据库监控数据失败：" + e.getMessage());
        } finally {
            // 清理资源
            if (connectionManager != null) {
                connectionManager.close();
            }
        }
    }

    /**
     * 获取进程详细信息列表
     *
     * @param extensionRequestParam 请求参数（host, port, database, username, password）
     * @return 当前所有活动进程的详细信息
     *
     * 返回数据结构说明：
     * {
     *   "processDetails": [                  // 进程详情数组
     *     {
     *       "sessionId": 52,                 // 会话 ID
     *       "loginName": "sa",               // 登录用户名
     *       "hostName": "WEB-SERVER-01",     // 客户端主机名
     *       "programName": "Java Application", // 应用程序名称
     *       "databaseName": "TestDB",        // 数据库名称
     *       "status": "running",             // 状态 (running/sleeping)
     *       "command": "SELECT",             // 命令类型
     *       "cpuTime": 1250,                 // CPU 耗时 (毫秒)
     *       "logicalReads": 4500,            // 逻辑读取次数
     *       "writes": 350,                  // 写入次数
     *       "elapsedTime": 5000,            // 执行总耗时 (毫秒)
     *       "lastSqlText": "SELECT * FROM...", // 正在执行的 SQL 语句
     *       "blockingSessionId": null,       // 阻塞它的会话 ID（如果有）
     *       "waitType": null,                // 等待类型
     *       "waitTime": 0,                   // 等待时间 (毫秒)
     *       "startTime": "2026-03-05..."     // 会话开始时间
     *     }
     *   ],
     *   "count": 15                          // 进程总数
     * }
     */
    @ActionHandler("getProcessDetails")
    public ResultMsg<JSONObject> getProcessDetails(ExtensionRequestParam extensionRequestParam) {
        logger.info("开始获取进程详细信息");

        SqlServerConnectionManager connectionManager = null;
        try {
            // 获取连接参数
            String host = extensionRequestParam.getParameter("host", "localhost");
            int port = Integer.parseInt(extensionRequestParam.getParameter("port", "1433"));
            String database = extensionRequestParam.getParameter("database", "master");
            String username = extensionRequestParam.getParameter("username");
            String password = extensionRequestParam.getParameter("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                return ResultMsg.fail("用户名和密码不能为空");
            }

            connectionManager = new SqlServerConnectionManager(host, port, database, username, password, 30);

            if (!connectionManager.testConnection()) {
                return ResultMsg.fail("无法连接到数据库");
            }

            SqlServerMetricsCollector collector = new SqlServerMetricsCollector(connectionManager.getDataSource());
            List<Map<String, Object>> processDetails = collector.getProcessDetailsDirectly();

            JSONObject resultData = new JSONObject();
            resultData.put("processDetails", processDetails);
            resultData.put("count", processDetails.size());

            return ResultMsg.success(resultData, "成功获取进程详细信息，共 " + processDetails.size() + " 条");

        } catch (Exception e) {
            logger.error("获取进程详细信息失败", e);
            return ResultMsg.fail("获取进程详细信息失败：" + e.getMessage());
        } finally {
            if (connectionManager != null) {
                connectionManager.close();
            }
        }
    }

    /**
     * 获取锁信息列表
     *
     * @param extensionRequestParam 请求参数（host, port, database, username, password）
     * @return 当前所有活跃的锁信息
     *
     * 返回数据结构说明：
     * {
     *   "lockInfos": [                       // 锁信息数组
     *     {
     *       "requestSessionId": 53,          // 请求锁的会话 ID
     *       "resourceType": "OBJECT",        // 资源类型 (OBJECT/PAGE/KEY/RID/DATABASE)
     *       "resourceDescription": "...",    // 资源描述
     *       "requestMode": "X",              // 请求模式 (S=共享/U=更新/X=排他/Sch-S=架构稳定)
     *       "requestStatus": "GRANT",        // 状态 (GRANT=已授予/WAIT=等待中)
     *       "databaseName": "TestDB",        // 数据库名称
     *       "objectName": "Orders",          // 对象名称（表名/索引名）
     *       "waitDuration": 15000,           // 等待时长 (毫秒)
     *       "sqlText": "UPDATE Orders..."    // 导致锁的 SQL 语句
     *     }
     *   ],
     *   "count": 5                           // 锁的总数
     * }
     */
    @ActionHandler("getLockInformation")
    public ResultMsg<JSONObject> getLockInformation(ExtensionRequestParam extensionRequestParam) {
        logger.info("开始获取锁信息");

        SqlServerConnectionManager connectionManager = null;
        try {
            String host = extensionRequestParam.getParameter("host", "localhost");
            int port = Integer.parseInt(extensionRequestParam.getParameter("port", "1433"));
            String database = extensionRequestParam.getParameter("database", "master");
            String username = extensionRequestParam.getParameter("username");
            String password = extensionRequestParam.getParameter("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                return ResultMsg.fail("用户名和密码不能为空");
            }

            connectionManager = new SqlServerConnectionManager(host, port, database, username, password, 30);

            if (!connectionManager.testConnection()) {
                return ResultMsg.fail("无法连接到数据库");
            }

            SqlServerMetricsCollector collector = new SqlServerMetricsCollector(connectionManager.getDataSource());
            List<Map<String, Object>> lockInfos = collector.getLockInformationDirectly();

            JSONObject resultData = new JSONObject();
            resultData.put("lockInfos", lockInfos);
            resultData.put("count", lockInfos.size());

            return ResultMsg.success(resultData, "成功获取锁信息，共 " + lockInfos.size() + " 条");

        } catch (Exception e) {
            logger.error("获取锁信息失败", e);
            return ResultMsg.fail("获取锁信息失败：" + e.getMessage());
        } finally {
            if (connectionManager != null) {
                connectionManager.close();
            }
        }
    }

    /**
     * 获取阻塞链信息
     *
     * @param extensionRequestParam 请求参数（host, port, database, username, password）
     * @return 数据库阻塞链信息（用于排查死锁和阻塞问题）
     *
     * 返回数据结构说明：
     * {
     *   "blockingChain": [                   // 阻塞链数组
     *     {
     *       "sessionId": 54,                 // 被阻塞的会话 ID
     *       "blockingSessionId": 53,         // 阻塞它的会话 ID
     *       "waitType": "LCK_M_X",           // 等待类型
     *       "waitTime": 15000,               // 等待时间 (毫秒)
     *       "waitResource": "KEY:...",       // 等待的资源
     *       "level": 1,                      // 阻塞层级（0=最外层，越大表示阻塞越深）
     *       "loginName": "user1",            // 登录名
     *       "hostName": "SERVER-01",         // 主机名
     *       "programName": "SSMS",           // 程序名
     *       "databaseName": "TestDB",        // 数据库名
     *       "command": "UPDATE",             // 命令类型
     *       "sqlText": "UPDATE ..."          // SQL 语句
     *     }
     *   ],
     *   "count": 3                           // 阻塞链中的会话数
     * }
     */
    @ActionHandler("getBlockingChain")
    public ResultMsg<JSONObject> getBlockingChain(ExtensionRequestParam extensionRequestParam) {
        logger.info("开始获取阻塞链信息");

        SqlServerConnectionManager connectionManager = null;
        try {
            String host = extensionRequestParam.getParameter("host", "localhost");
            int port = Integer.parseInt(extensionRequestParam.getParameter("port", "1433"));
            String database = extensionRequestParam.getParameter("database", "master");
            String username = extensionRequestParam.getParameter("username");
            String password = extensionRequestParam.getParameter("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                return ResultMsg.fail("用户名和密码不能为空");
            }

            connectionManager = new SqlServerConnectionManager(host, port, database, username, password, 30);

            if (!connectionManager.testConnection()) {
                return ResultMsg.fail("无法连接到数据库");
            }

            SqlServerMetricsCollector collector = new SqlServerMetricsCollector(connectionManager.getDataSource());
            List<Map<String, Object>> blockingChain = collector.getBlockingChainDirectly();

            JSONObject resultData = new JSONObject();
            resultData.put("blockingChain", blockingChain);
            resultData.put("count", blockingChain.size());

            return ResultMsg.success(resultData, "成功获取阻塞链信息，共 " + blockingChain.size() + " 条");

        } catch (Exception e) {
            logger.error("获取阻塞链信息失败", e);
            return ResultMsg.fail("获取阻塞链信息失败：" + e.getMessage());
        } finally {
            if (connectionManager != null) {
                connectionManager.close();
            }
        }
    }

    /**
     * 获取最耗资源的 SQL TOP 10
     *
     * @param extensionRequestParam 请求参数（host, port, database, username, password）
     * @return 消耗 CPU 资源最多的前 10 条 SQL 语句
     *
     * 返回数据结构说明：
     * {
     *   "topSqlList": [                      // SQL 列表数组
     *     {
     *       "execution_count": 1500,         // 执行次数
     *       "total_cpu_time": 850000,        // 总 CPU 时间 (毫秒)
     *       "total_elapsed_time": 1200000,   // 总耗时 (毫秒)
     *       "total_logical_reads": 450000,   // 总逻辑读取次数
     *       "total_logical_writes": 35000,   // 总写入次数
     *       "last_execution_time": "...",    // 最后执行时间
     *       "sql_text": "SELECT * FROM...",  // SQL 语句文本
     *       "database_name": "TestDB",       // 数据库名称
     *       "avg_cpu_time": 566,             // 平均 CPU 时间 (毫秒)
     *       "avg_elapsed_time": 800          // 平均耗时 (毫秒)
     *     }
     *   ],
     *   "count": 10                          // SQL 数量
     * }
     */
    @ActionHandler("getTopResourceSql")
    public ResultMsg<JSONObject> getTopResourceSql(ExtensionRequestParam extensionRequestParam) {
        logger.info("开始获取最耗资源的 SQL");

        SqlServerConnectionManager connectionManager = null;
        try {
            String host = extensionRequestParam.getParameter("host", "localhost");
            int port = Integer.parseInt(extensionRequestParam.getParameter("port", "1433"));
            String database = extensionRequestParam.getParameter("database", "master");
            String username = extensionRequestParam.getParameter("username");
            String password = extensionRequestParam.getParameter("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                return ResultMsg.fail("用户名和密码不能为空");
            }

            connectionManager = new SqlServerConnectionManager(host, port, database, username, password, 30);

            if (!connectionManager.testConnection()) {
                return ResultMsg.fail("无法连接到数据库");
            }

            SqlServerMetricsCollector collector = new SqlServerMetricsCollector(connectionManager.getDataSource());
            List<Map<String, Object>> topSqlList = collector.getTopResourceConsumingSqlDirectly();

            JSONObject resultData = new JSONObject();
            resultData.put("topSqlList", topSqlList);
            resultData.put("count", topSqlList.size());

            return ResultMsg.success(resultData, "成功获取最耗资源的 SQL TOP 10");

        } catch (Exception e) {
            logger.error("获取最耗资源的 SQL 失败", e);
            return ResultMsg.fail("获取最耗资源的 SQL 失败：" + e.getMessage());
        } finally {
            if (connectionManager != null) {
                connectionManager.close();
            }
        }
    }

    /**
     * 获取按程序和 SQL 分组的连接统计
     *
     * @param extensionRequestParam 请求参数（host, port, database, username, password）
     * @return 按 program_name 和 sql_text 分组的连接使用统计
     *
     * 返回数据结构说明：
     * {
     *   "groupedStats": [                    // 分组统计数组
     *     {
     *       "program_name": "Java App",      // 应用程序名称
     *       "sql_text": "SELECT * FROM...",  // SQL 语句（前 200 字符）
     *       "database_name": "TestDB",       // 数据库名称
     *       "connection_count": 5,           // 该组合的连接数
     *       "running_count": 2,              // 运行中的连接数
     *       "sleeping_count": 3,             // 休眠中的连接数
     *       "active_request_count": 2,       // 有活跃请求的连接数
     *       "avg_cpu_time": 1250.5,          // 平均 CPU 时间
     *       "total_cpu_time": 6252,          // 总 CPU 时间
     *       "avg_reads": 450.2,              // 平均逻辑读取
     *       "total_reads": 2251,             // 总逻辑读取
     *       "avg_writes": 120.5,             // 平均写入次数
     *       "total_writes": 602,             // 总写入次数
     *       "max_cpu_time": 8500,            // 最大 CPU 时间
     *       "max_elapsed_time": 15000,       // 最大执行时间
     *       "first_login_time": "...",       // 最早登录时间
     *       "last_request_time": "..."       // 最后请求时间
     *     }
     *   ],
     *   "groupCount": 2,                     // 不同的程序/SQL 组合数
     *   "summary": {                         // 汇总统计
     *       "totalConnections": 7,           // 总连接数
     *       "totalRunning": 2,               // 总运行数
     *       "totalSleeping": 5,              // 总休眠数
     *       "totalCpuTime": 7252             // 总 CPU 时间
     *   }
     * }
     */
    @ActionHandler("getConnectionStatsByProgram")
    public ResultMsg<JSONObject> getConnectionStatsByProgram(ExtensionRequestParam extensionRequestParam) {
        logger.info("开始获取按程序和 SQL 分组的连接统计");
        
        SqlServerConnectionManager connectionManager = null;
        try {
            String host = extensionRequestParam.getParameter("host", "localhost");
            int port = Integer.parseInt(extensionRequestParam.getParameter("port", "1433"));
            String database = extensionRequestParam.getParameter("database", "master");
            String username = extensionRequestParam.getParameter("username");
            String password = extensionRequestParam.getParameter("password");
            
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                return ResultMsg.fail("用户名和密码不能为空");
            }
            
            connectionManager = new SqlServerConnectionManager(host, port, database, username, password, 30);
            
            if (!connectionManager.testConnection()) {
                return ResultMsg.fail("无法连接到数据库");
            }
            
            SqlServerMetricsCollector collector = new SqlServerMetricsCollector(connectionManager.getDataSource());
            List<Map<String, Object>> groupedStats = collector.getConnectionsGroupedByProgramAndSqlDirectly();
            
            JSONObject resultData = new JSONObject();
            resultData.put("groupedStats", groupedStats);
            resultData.put("groupCount", groupedStats.size());
            
            // 计算总计
            int totalConnections = 0;
            int totalRunning = 0;
            int totalSleeping = 0;
            long totalCpuTime = 0;
            
            for (Map<String, Object> group : groupedStats) {
                totalConnections += getIntValue(group, "connection_count");
                totalRunning += getIntValue(group, "running_count");
                totalSleeping += getIntValue(group, "sleeping_count");
                totalCpuTime += getLongValue(group, "total_cpu_time");
            }
            
            JSONObject summary = new JSONObject();
            summary.put("totalConnections", totalConnections);
            summary.put("totalRunning", totalRunning);
            summary.put("totalSleeping", totalSleeping);
            summary.put("totalCpuTime", totalCpuTime);
            
            resultData.put("summary", summary);
            
            return ResultMsg.success(resultData, 
                "成功获取连接分组统计，共 " + groupedStats.size() + " 个不同的程序/SQL 组合");
            
        } catch (Exception e) {
            logger.error("获取连接分组统计失败", e);
            return ResultMsg.fail("获取连接分组统计失败：" + e.getMessage());
        } finally {
            if (connectionManager != null) {
                connectionManager.close();
            }
        }
    }
    
    /**
     * 获取整数值的工具方法
     */
    private int getIntValue(Map<String, Object> map, String key) {
        if (map == null) return 0;
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }
    
    /**
     * 获取长整型值的工具方法
     */
    private long getLongValue(Map<String, Object> map, String key) {
        if (map == null) return 0L;
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }
}
