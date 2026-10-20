package com.jt.plugins.extension;

import com.alibaba.fastjson.JSONObject;
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

public class ServiceConfigProviderImpl implements ServiceConfigProvider {
    // 插件日志记录器
    private static final PluginLogger logger = PluginLogger.getLogger("service-config-plugin");

    // 插件文件存储管理器
    private static final PluginFileStorage fileStorage = PluginFileStorage.getStorage("service-config-plugin");

    // 缓存方法映射，避免每次反射查找
    private static final Map<String, Method> ACTION_HANDLERS = new HashMap<>();

    // 在类加载时初始化方法映射
    static {
        Method[] methods = ServiceConfigProviderImpl.class.getDeclaredMethods();
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
     * 修改jvm配置
     */
}
