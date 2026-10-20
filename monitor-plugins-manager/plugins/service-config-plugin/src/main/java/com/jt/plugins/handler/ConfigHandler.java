package com.jt.plugins.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jt.plugins.common.http.ExtensionRequestParam;
import com.jt.plugins.common.result.ResultMsg;

import java.util.List;
import java.util.Map;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.handler
 * @Author: 别来无恙qb
 * @CreateTime: 2026-03-09  14:29
 * @Description: TODO
 * @Version: 1.0
 */

/**
 * 配置处理器接口
 */
public interface ConfigHandler {

    /**
     * 读取配置
     */
    ResultMsg<JSONObject> readConfig(ExtensionRequestParam request);

    /**
     * 修改配置
     */
    ResultMsg<JSONObject> modifyConfig(ExtensionRequestParam request);

    /**
     * 验证配置
     */
    ResultMsg<JSONObject> validateConfig(ExtensionRequestParam request);

    /**
     * 获取配置类型
     */
    String getConfigType();

    /**
     * 配置项解析结果
     */
    class ConfigItem {
        private String key;
        private String oldValue;
        private String newValue;

        public ConfigItem(String key, String oldValue, String newValue) {
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("key", key);
            json.put("oldValue", oldValue);
            json.put("newValue", newValue);
            return json;
        }
    }

    /**
     * 验证结果
     */
    class ValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<Map<String, Object>> warnings;
        private List<String> suggestions;

        public ValidationResult() {
            this.valid = true;
            this.errors = new java.util.ArrayList<>();
            this.warnings = new java.util.ArrayList<>();
            this.suggestions = new java.util.ArrayList<>();
        }

        public void addError(String error) {
            valid = false;
            errors.add(error);
        }

        public void addWarning(int line, String message, String suggestion) {
            Map<String, Object> warning = new java.util.HashMap<>();
            warning.put("line", line);
            warning.put("message", message);
            warning.put("suggestion", suggestion);
            warnings.add(warning);
        }

        public void addSuggestion(String suggestion) {
            suggestions.add(suggestion);
        }

        public JSONObject toJSON() {
            JSONObject result = new JSONObject();
            result.put("valid", valid);
            result.put("errors", errors);

            JSONArray warningsArray = new JSONArray();
            for (Map<String, Object> warning : warnings) {
                warningsArray.add(new JSONObject(warning));
            }
            result.put("warnings", warningsArray);
            result.put("suggestions", suggestions);

            return result;
        }
    }
}
