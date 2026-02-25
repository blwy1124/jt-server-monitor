package com.jt.plugins.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jt.plugins.api.OperationButtonExtension;
import com.jt.plugins.api.monitor.ActionExtension;
import com.jt.plugins.common.http.ExtensionRequestParam;
import com.jt.plugins.common.result.ResultMsg;
import com.jt.plugins.service.ExtensionManagerService;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author blwy_qb
 */
@RestController
@RequestMapping("ext")
public class ExtensionManagerController {

    @Autowired
    private ExtensionManagerService extensionManagerService;

    private PluginManager pluginManager;

    @Autowired
    public void setPluginManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    /**
     * 获取插件列表
     * @param extensionClass 插件接口顶级类名
     * @return
     * @throws ClassNotFoundException
     */
    @GetMapping("list")
    public JSONArray extensions(@RequestParam String extensionClass) throws ClassNotFoundException {
        List extensions = pluginManager.getExtensions(Class.forName(extensionClass));
        JSONArray result = new JSONArray();
        for (Object extension : extensions) {
            if (extension instanceof OperationButtonExtension) {
                PluginWrapper plugin = pluginManager.whichPlugin(extension.getClass());
                result.add(new JSONObject().fluentPut("class", extension.getClass().getName())
                        .fluentPut("name", ((OperationButtonExtension) extension).name()).fluentPut("plugin", new JSONObject()
                                .fluentPut("id", plugin.getPluginId())
                                .fluentPut("version", plugin.getDescriptor().getVersion())
                                .fluentPut("path", plugin.getPluginPath().toString())
                                .fluentPut("state", plugin.getPluginState())
                                .fluentPut("class", plugin.getDescriptor().getPluginClass())));
            }
        }
        return result;
    }

    /**
     *  插件点击  框架接口 目前未使用 未来删除
     * @param extensionClass
     * @param name
     * @return
     * @throws ClassNotFoundException
     */
    @GetMapping("click")
    public String click(@RequestParam String extensionClass, @RequestParam String name) throws ClassNotFoundException {
        List extensions = pluginManager.getExtensions(Class.forName(extensionClass));
        for (Object extension : extensions) {
            if (extension instanceof OperationButtonExtension && ((OperationButtonExtension) extension).name().equals(name)) {
                return ((OperationButtonExtension) extension).onClick();
            }
        }
        return "Extension not found or unavailable";
    }

    /**
     * 指定插件进行操作
     * @param extensionRequestParam
     * @return 操作结果
     * @throws ClassNotFoundException
     */
    @RequestMapping(value = "/action", method = RequestMethod.POST)
    public ResultMsg<JSONObject> action(@RequestBody ExtensionRequestParam extensionRequestParam) throws ClassNotFoundException {
        return extensionManagerService.action(extensionRequestParam);
    }
}
