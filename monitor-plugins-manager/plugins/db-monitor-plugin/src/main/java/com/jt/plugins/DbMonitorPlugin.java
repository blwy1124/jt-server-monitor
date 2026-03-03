package com.jt.plugins;

import com.alibaba.fastjson.JSONObject;
import com.jt.plugins.extension.DbMonitorProvider;
import com.jt.plugins.extension.SpringConfiguration;
import com.jt.plugins.api.monitor.ActionExtension;
import com.jt.plugins.common.http.ExtensionRequestParam;
import com.jt.plugins.common.result.ResultMsg;
import com.jt.plugins.core.SpringPlugin;
import org.pf4j.Extension;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugin
 * @Author: 别来无恙qb
 * @CreateTime: 2026-03-02  15:00
 * @Description: TODO
 * @Version: 1.0
 */

public class DbMonitorPlugin extends SpringPlugin {

    public DbMonitorPlugin(PluginWrapper wrapper) {
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
        if (applicationContext.containsBean("dbMonitorProvider")) {
            System.out.println("bbMonitorProvider bean已成功注册");
        } else {
            System.out.println("bbMonitorProvider bean注册失败");
        }

        return applicationContext;
    }

    @Extension
    public static class DbMonitorExtension implements ActionExtension {

        private final DbMonitorProvider dbMonitorProvider;

        @Autowired
        public DbMonitorExtension(final DbMonitorProvider dbMonitorProvider) {
            this.dbMonitorProvider = dbMonitorProvider;
        }


        @Override
        public String name() {
            return "dbMonitor";
        }

        @Override
        public String description() {
            return "SQL Server数据库活动监视器监控插件";
        }

        @Override
        public ResultMsg<JSONObject> execute(ExtensionRequestParam extensionRequestParam) {
            return dbMonitorProvider.execute(extensionRequestParam);
		}

	}
}
