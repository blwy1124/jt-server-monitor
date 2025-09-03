package com.jt.plugins.controller;

import com.alibaba.fastjson2.JSONObject;
import com.jt.plugins.common.result.ResultMsg;
import com.jt.plugins.config.Pf4jManagerProperties;
import com.jt.plugins.service.PluginManagerService;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author blwy_qb
 */
@RestController
@RequestMapping("plugin")
public class PluginManagerController {


    @Autowired
    private PluginManagerService pluginManagerService;

    @PostMapping("upload")
    public ResultMsg<JSONObject> uploadPlugin(@RequestParam("file") MultipartFile file) throws IOException {
        return pluginManagerService.uploadPlugin(file);
    }

    @GetMapping("reload")
    public ResultMsg<JSONObject> reloadPlugins() {
        return pluginManagerService.reloadPlugins();
    }

    @GetMapping("load")
    public ResultMsg<String> loadPlugin(@RequestParam() String pluginId) {
        return pluginManagerService.loadPlugin(pluginId);
    }

    @GetMapping("unload")
    public ResultMsg<JSONObject> unloadPlugin(@RequestParam() String pluginId) {
        return pluginManagerService.unloadPlugin(pluginId);
    }

    @GetMapping("start")
    public ResultMsg<PluginState> startPlugin(@RequestParam() String pluginId) {
        return pluginManagerService.startPlugin(pluginId);
    }

    @GetMapping("stop")
    public ResultMsg<PluginState> stopPlugin(@RequestParam() String pluginId) {
        return pluginManagerService.stopPlugin(pluginId);
    }

    @GetMapping("disable")
    public ResultMsg<JSONObject> disablePlugin(@RequestParam() String pluginId) {
        return pluginManagerService.disablePlugin(pluginId);
    }

    @GetMapping("enable")
    public ResultMsg<JSONObject> enablePlugin(@RequestParam() String pluginId) {
        return pluginManagerService.enablePlugin(pluginId);
    }

    @GetMapping("delete")
    public ResultMsg<JSONObject> deletePlugin(@RequestParam() String pluginId) {
        return pluginManagerService.deletePlugin(pluginId);
    }

    @GetMapping("list")
    public ResultMsg<List<JSONObject>> listPlugin() {
        return pluginManagerService.getPlugins();
    }

}
