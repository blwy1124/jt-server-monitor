package com.jt.plugins.extension;

import com.alibaba.fastjson.JSONObject;
import com.jt.plugins.utils.SqlServerConnectionManager;
import com.jt.plugins.utils.SqlServerMetricsCollector;
import com.jt.plugins.model.SqlServerActivityMetrics;  // 添加这个导入
import com.jt.plugins.common.annotation.ActionHandler;
import com.jt.plugins.common.file.PluginFileStorage;
import com.jt.plugins.common.http.ExtensionRequestParam;
import com.jt.plugins.common.log.PluginLogger;
import com.jt.plugins.common.result.ResultMsg;

import java.lang.reflect.Method;
import java.util.HashMap;
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
     * 获取数据库 活动监视器中的 监控数据
     * @param extensionRequestParam
     * @return
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
}
