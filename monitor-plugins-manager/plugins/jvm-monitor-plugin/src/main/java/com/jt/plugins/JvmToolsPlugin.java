package com.jt.plugins;

import com.alibaba.fastjson2.JSONObject;
import com.jt.plugins.api.monitor.ActionExtension;
import com.jt.plugins.common.http.ExtensionRequestParam;
import com.jt.plugins.common.result.ResultMsg;
import com.jt.plugins.core.SpringPlugin;
import com.jt.plugins.extension.JvmToolsProvider;
import com.jt.plugins.extension.SpringConfiguration;
import org.pf4j.Extension;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Arrays;
import java.util.List;

/**
 * @author blwy_qb
 */
public class JvmToolsPlugin extends SpringPlugin {

    public JvmToolsPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }
    @Override
    protected ApplicationContext createApplicationContext() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
        // 设置插件类加载器
        applicationContext.setClassLoader(getWrapper().getPluginClassLoader());
        // 注册配置类
        applicationContext.register(SpringConfiguration.class);
        // 刷新上下文
        applicationContext.refresh();

        // 验证bean是否存在
        if (applicationContext.containsBean("jvmToolsProvider")) {
            System.out.println("JvmToolsProvider bean已成功注册");
        } else {
            System.out.println("JvmToolsProvider bean注册失败");
        }

        return applicationContext;
    }

    @Extension
    public static class JvmToolsExtension implements ActionExtension {

        private final JvmToolsProvider jvmToolsProvider;

        @Autowired
        public JvmToolsExtension(final JvmToolsProvider jvmToolsProvider) {
            this.jvmToolsProvider = jvmToolsProvider;
        }


        @Override
        public String name() {
            return "JvmMonitor";
        }

        @Override
        public String description() {
            return null;
        }

        @Override
        public ResultMsg<JSONObject> execute(ExtensionRequestParam extensionRequestParam) {
            return jvmToolsProvider.execute(extensionRequestParam);
        }

    }
}