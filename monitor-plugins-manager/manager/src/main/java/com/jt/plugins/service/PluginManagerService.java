package com.jt.plugins.service;

import com.alibaba.fastjson2.JSONObject;
import com.jt.plugins.common.result.ResultMsg;
import com.jt.plugins.config.Pf4jManagerProperties;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.service
 * @Author: 别来无恙qb
 * @CreateTime: 2025-09-02  16:43
 * @Description: TODO
 * @Version: 1.0
 */

@Service
public class PluginManagerService {
    private Pf4jManagerProperties pf4jManagerProperties;

    private PluginManager pluginManager;

    @Autowired
    public void setPf4jManagerProperties(Pf4jManagerProperties pf4jManagerProperties) {
        this.pf4jManagerProperties = pf4jManagerProperties;
    }

    @Autowired
    public void setPluginManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public ResultMsg<JSONObject> uploadPlugin(MultipartFile file) throws IOException {
        String path = pf4jManagerProperties.getPath();
        Files.copy(file.getInputStream(), Paths.get(path).resolve(file.getOriginalFilename()));
        return ResultMsg.success();
    }

    public ResultMsg<JSONObject> reloadPlugins() {
        pluginManager.unloadPlugins();
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        return ResultMsg.success();
    }

    public ResultMsg<String> loadPlugin(String pluginId) {
        PluginWrapper plugin = pluginManager.getPlugin(pluginId);
        if (plugin != null) {
            String loadPlugin = pluginManager.loadPlugin(plugin.getPluginPath());
            return ResultMsg.success(loadPlugin);
        } else {
            return ResultMsg.success("Plugin not found");
        }
    }

    public ResultMsg<JSONObject> unloadPlugin(String pluginId) {
        boolean unloadPlugin = pluginManager.unloadPlugin(pluginId);
        return unloadPlugin ? ResultMsg.success() : ResultMsg.fail("插件卸载失败");
    }
    public ResultMsg<PluginState> startPlugin(String pluginId) {
        return ResultMsg.success(pluginManager.startPlugin(pluginId));
    }

    public ResultMsg<PluginState> stopPlugin(String pluginId) {
        PluginState pluginState = pluginManager.stopPlugin(pluginId);
        return ResultMsg.success(pluginState);
    }

    public ResultMsg<JSONObject> disablePlugin(String pluginId) {
        boolean disablePlugin = pluginManager.disablePlugin(pluginId);
        return disablePlugin ? ResultMsg.success() : ResultMsg.fail("插件禁用失败");
    }

    public ResultMsg<JSONObject> enablePlugin(String pluginId) {
        boolean enablePlugin = pluginManager.enablePlugin(pluginId);
        return enablePlugin ? ResultMsg.success() : ResultMsg.fail("插件启用失败");
    }

    public ResultMsg<JSONObject> deletePlugin(String pluginId) {
        boolean deletePlugin = pluginManager.deletePlugin(pluginId);
        return deletePlugin ? ResultMsg.success() : ResultMsg.fail("插件删除失败");
    }

    public ResultMsg<List<JSONObject>> getPlugins() {
        ArrayList<JSONObject> list = new ArrayList<>();
        List<PluginWrapper> plugins = pluginManager.getPlugins();
        for (PluginWrapper plugin : plugins) {
            list.add(new JSONObject()
                    .fluentPut("id", plugin.getPluginId())
                    .fluentPut("version", plugin.getDescriptor().getVersion())
                    .fluentPut("path", plugin.getPluginPath().toString())
                    .fluentPut("state", plugin.getPluginState())
                    .fluentPut("class", plugin.getDescriptor().getPluginClass())
            );
        }
        return ResultMsg.success(list);
    }
}
