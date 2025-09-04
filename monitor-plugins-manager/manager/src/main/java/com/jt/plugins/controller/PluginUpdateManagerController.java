package com.jt.plugins.controller;

import com.alibaba.fastjson2.JSONObject;
import com.jt.plugins.common.http.ExtensionRequestParam;
import com.jt.plugins.common.result.ResultMsg;
import com.jt.plugins.service.PluginUpdateManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.controller
 * @Author: 别来无恙qb
 * @CreateTime: 2025-09-03  09:59
 * @Description: 插件更新管理控制器
 * @Version: 1.0
 */
@RestController
@RequestMapping("update")
public class PluginUpdateManagerController {

    @Autowired
    private PluginUpdateManagerService pluginUpdateManagerService;

    /**
     * 设置仓库地址
     * @param request 包含仓库ID和URL的请求参数
     * @return 操作结果
     */
    @PostMapping("repository")
    public ResultMsg<String> setRepository(@RequestBody ExtensionRequestParam request) {
        return pluginUpdateManagerService.setRepository(request);
    }

    /**
     * 检查插件更新
     * @param request 包含插件ID等信息的请求参数
     * @return 更新信息
     */
    @PostMapping("check")
    public ResultMsg<JSONObject> checkForUpdate(@RequestBody ExtensionRequestParam request) {
        return pluginUpdateManagerService.checkForUpdate(request);
    }

    /**
     * 更新或安装插件
     * 如果插件已存在则更新，否则安装新插件
     * @param request 包含插件ID等信息的请求参数
     * @return 操作结果
     */
    @PostMapping("update-or-install")
    public ResultMsg<String> updateOrInstallPlugin(@RequestBody ExtensionRequestParam request) {
        return pluginUpdateManagerService.updateOrInstallPlugin(request);
    }

    /**
     * 安装插件
     * @param request 包含插件ID等信息的请求参数
     * @return 操作结果
     */
    @PostMapping("install")
    public ResultMsg<String> installPlugin(@RequestBody ExtensionRequestParam request) {
        return pluginUpdateManagerService.installPlugin(request);
    }

    /**
     * 更新插件
     * @param request 包含插件ID等信息的请求参数
     * @return 操作结果
     */
    @PostMapping("update")
    public ResultMsg<String> updatePlugin(@RequestBody ExtensionRequestParam request) {
        return pluginUpdateManagerService.updatePlugin(request);
    }
}
