package com.jt.plugins.service;

import com.alibaba.fastjson2.JSONObject;
import com.jt.plugins.api.monitor.ActionExtension;
import com.jt.plugins.common.http.ExtensionRequestParam;
import com.jt.plugins.common.result.ResultMsg;
import org.pf4j.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.service
 * @Author: 别来无恙qb
 * @CreateTime: 2025-09-02  16:29
 * @Description: TODO
 * @Version: 1.0
 */

@Service
public class ExtensionManagerService {

    private PluginManager pluginManager;

    @Autowired
    public void setPluginManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }
    public ResultMsg<JSONObject> action(ExtensionRequestParam extensionRequestParam) throws ClassNotFoundException {
        String extensionName = extensionRequestParam.getExtensionName();
        String extensionClass = extensionRequestParam.getExtensionClass();
        List extensions = pluginManager.getExtensions(Class.forName(extensionClass));
        for (Object extension : extensions) {
            if (extension instanceof ActionExtension && ((ActionExtension) extension).name().equals(extensionName)) {
                return ((ActionExtension) extension).execute(extensionRequestParam);
            }
        }
        return ResultMsg.fail("Extension not found or unavailable(未找到扩展名或无法使用)");
    }

}
