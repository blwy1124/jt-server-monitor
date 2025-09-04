package com.jt.plugins.controller;

import com.alibaba.fastjson2.JSONObject;
import com.jt.plugins.common.http.ExtensionRequestParam;
import com.jt.plugins.common.result.ResultMsg;
import com.jt.plugins.service.PluginManagerService;
import org.pf4j.PluginState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @author blwy_qb
 * @description 插件管理控制器
 */
@RestController
@RequestMapping("plugin")
public class PluginManagerController {

    private static final Logger logger = LoggerFactory.getLogger(PluginManagerController.class);

    @Autowired
    private PluginManagerService pluginManagerService;

    /**
     * 上传插件文件
     */
    @PostMapping("upload")
    public ResultMsg<JSONObject> uploadPlugin(@RequestParam("file") MultipartFile file) throws IOException {
        return pluginManagerService.uploadPlugin(file);
    }

    /**
     * 重新加载所有插件
     */
    @PostMapping("reload")
    public ResultMsg<JSONObject> reloadPlugins() {
        return pluginManagerService.reloadPlugins();
    }

    /**
     * 加载指定插件
     */
    @PostMapping("load")
    public ResultMsg<String> loadPlugin(@RequestBody ExtensionRequestParam request) {
        return pluginManagerService.loadPlugin(request);
    }

    /**
     * 卸载指定插件
     */
    @PostMapping("unload")
    public ResultMsg<JSONObject> unloadPlugin(@RequestBody ExtensionRequestParam request) {
        return pluginManagerService.unloadPlugin(request);
    }

    /**
     * 启动指定插件
     */
    @PostMapping("start")
    public ResultMsg<PluginState> startPlugin(@RequestBody ExtensionRequestParam request) {
        return pluginManagerService.startPlugin(request);
    }

    /**
     * 停止指定插件
     */
    @PostMapping("stop")
    public ResultMsg<PluginState> stopPlugin(@RequestBody ExtensionRequestParam request) {
        return pluginManagerService.stopPlugin(request);
    }

    /**
     * 禁用指定插件
     */
    @PostMapping("disable")
    public ResultMsg<JSONObject> disablePlugin(@RequestBody ExtensionRequestParam request) {
        return pluginManagerService.disablePlugin(request);
    }

    /**
     * 启用指定插件
     */
    @PostMapping("enable")
    public ResultMsg<JSONObject> enablePlugin(@RequestBody ExtensionRequestParam request) {
        return pluginManagerService.enablePlugin(request);
    }

    /**
     * 删除指定插件
     */
    @PostMapping("delete")
    public ResultMsg<JSONObject> deletePlugin(@RequestBody ExtensionRequestParam request) {
        return pluginManagerService.deletePlugin(request);
    }

    /**
     * 获取插件列表
     */
    @PostMapping("list")
    public ResultMsg<List<JSONObject>> listPlugin() {
        return pluginManagerService.getPlugins();
    }
}
