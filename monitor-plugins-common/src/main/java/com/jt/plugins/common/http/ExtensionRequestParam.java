package com.jt.plugins.common.http;

import com.alibaba.fastjson2.JSONObject;
import java.util.Map;
import java.util.Objects;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.common.http
 * @Author: 别来无恙qb
 * @CreateTime: 2025-08-29  11:26
 * @Description: TODO
 * @Version: 1.0
 */

public class ExtensionRequestParam {
    private  String extensionClass; // 扩展点接口类名
    private String extensionName;      // 扩展名称
    private String targetAction;          // 目标操作名称
    private JSONObject parameters; // 其他参数

    // 构造函数
    public ExtensionRequestParam() {
        this.parameters = new JSONObject();
    }
    // Getters and Setters
    public String getExtensionClass() {
        return extensionClass;
    }

    public void setExtensionClass(String extensionClass) {
        this.extensionClass = extensionClass;
    }
    public String getExtensionName() {
        return extensionName;
    }

    public void setExtensionName(String extensionName) {
        this.extensionName = extensionName;
    }

    public String getTargetAction() {
        return targetAction;
    }

    public void setTargetAction(String targetAction) {
        this.targetAction = targetAction;
    }

    public JSONObject getParameters() {
        return parameters;
    }

    public void setParameters(JSONObject parameters) {
        this.parameters = parameters;
    }

    public void addParameter(String key, Object value) {
        this.parameters.put(key, value);
    }
    public void addParameter(String key, String value) {
        this.parameters.put(key, value);
    }

    public Object getObjectParameter(String key) {
        return this.parameters.get(key);
    }

    public String getParameter(String key) {
        return this.parameters.getString(key);
    }

    public String getParameter(String key, String defaultValue) {
        return this.parameters.containsKey( key) ? this.parameters.getString(key) : defaultValue;
    }
}
