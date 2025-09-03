package com.jt.plugins.api.monitor;

import com.alibaba.fastjson2.JSONObject;
import com.jt.plugins.common.http.ExtensionRequestParam;
import com.jt.plugins.common.result.ResultMsg;
import org.pf4j.ExtensionPoint;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.api.monitor
 * @Author: 别来无恙qb
 * @CreateTime: 2025-08-12  14:04
 * @Description: 操作扩展点接口,相关扩展点实现类需实现此接口
 * @Version: 1.0
 */

/**
 * 操作扩展点接口
 */
public interface ActionExtension extends ExtensionPoint {
    /**
     * 获取扩展操作名称
     *
     * @return 操作名称
     */
    String name();

    /**
     * 获取扩展操作描述
     *
     * @return 操作描述
     */
    String description();

    /**
     * 执行操作逻辑
     *
     * @return 操作结果
     */
    <T> ResultMsg<T> execute(ExtensionRequestParam request);
}
