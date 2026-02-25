package com.jt.plugin;

import com.alibaba.fastjson.JSONObject;
import com.jt.plugin.extension.LogCleanProvider;
import com.jt.plugin.extension.LogCleanProviderImpl;
import com.jt.plugins.common.http.ExtensionRequestParam;
import com.jt.plugins.common.result.ResultMsg;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.pf4j.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class LogCleanPluginApplicationTests {
    @Autowired
    private PluginManager pluginManager;

    @Test
    public void testPingFunction() {
        // 直接调用插件方法进行测试
        LogCleanProvider provider = new LogCleanProviderImpl();

        ExtensionRequestParam request = new ExtensionRequestParam();
        request.setTargetAction("ping");

        ResultMsg<JSONObject> result = provider.execute(request);

        System.out.println("Result: " + result);
        Assertions.assertTrue(result.getState() == 1);
    }

    @Test
    public void testThroughPluginManager() {
        // 通过插件管理器调用
        List<LogCleanProvider> providers = pluginManager.getExtensions(LogCleanProvider.class);

        if (!providers.isEmpty()) {
            LogCleanProvider provider = providers.get(0);

            ExtensionRequestParam request = new ExtensionRequestParam();
            request.setTargetAction("ping");

            ResultMsg<JSONObject> result = provider.execute(request);

            System.out.println("Plugin Result: " + result);
            Assertions.assertTrue(result.getState() == 1);
        }
    }
}
