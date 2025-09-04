package com.jt.plugins.common.constant;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.common.constant
 * @Author: 别来无恙qb
 * @CreateTime: 2025-09-04  14:03
 * @Description: 插件管理常量类
 * @Version: 1.0
 */

public class PluginConstants {
    // 插件状态
    public static final String PLUGIN_STATE_STARTED = "STARTED";
    public static final String PLUGIN_STATE_STOPPED = "STOPPED";
    public static final String PLUGIN_STATE_DISABLED = "DISABLED";
    public static final String PLUGIN_STATE_CREATED = "CREATED";

    // 插件操作结果
    public static final String PLUGIN_LOADED = "插件加载成功";
    public static final String PLUGIN_UNLOADED = "插件卸载成功";
    public static final String PLUGIN_STARTED = "插件启动成功";
    public static final String PLUGIN_STOPPED = "插件停止成功";
    public static final String PLUGIN_ENABLED = "插件启用成功";
    public static final String PLUGIN_DISABLED = "插件禁用成功";
    public static final String PLUGIN_DELETED = "插件删除成功";

    // 错误信息
    public static final String PLUGIN_NOT_FOUND = "插件未找到";
    public static final String PLUGIN_ALREADY_LOADED = "插件已加载";
    public static final String PLUGIN_ALREADY_STARTED = "插件已启动";
    public static final String PLUGIN_NOT_LOADED = "插件未加载";
    public static final String PLUGIN_FILE_EXISTS = "插件文件已存在";
    public static final String INVALID_PLUGIN_FILE = "无效的插件文件";

    // 文件相关
    public static final String PLUGIN_FILE_EXTENSION = ".jar";
    public static final int MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
}
