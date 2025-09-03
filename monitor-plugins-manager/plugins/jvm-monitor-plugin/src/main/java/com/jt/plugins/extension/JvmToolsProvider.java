package com.jt.plugins.extension;

import com.alibaba.fastjson2.JSONObject;
import com.jt.plugins.common.http.ExtensionRequestParam;
import com.jt.plugins.common.result.ResultMsg;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.extension
 * @Author: 别来无恙qb
 * @CreateTime: 2025-08-28  17:35
 * @Description: jvm工具提供接口
 * @Version: 1.0
 */

public interface JvmToolsProvider {
    ResultMsg<JSONObject> execute(ExtensionRequestParam extensionRequestParam);
}
