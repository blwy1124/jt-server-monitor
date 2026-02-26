package com.jt.plugins;

import com.alibaba.fastjson.JSONObject;
import com.jt.plugins.extension.LogCleanProvider;
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
 * @author blwy_qb
 */
public class LogCleanPlugin extends SpringPlugin {

	public LogCleanPlugin(PluginWrapper wrapper) {
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
		if (applicationContext.containsBean("logCleanProvider")) {
			System.out.println("logCleanProvider bean已成功注册");
		} else {
			System.out.println("logCleanProvider bean注册失败");
		}

		return applicationContext;
	}

	@Extension
	public static class LogCleanExtension implements ActionExtension {

		private final LogCleanProvider logCleanProvider;

		@Autowired
		public LogCleanExtension(final LogCleanProvider logCleanProvider) {
			this.logCleanProvider = logCleanProvider;
		}


		@Override
		public String name() {
			return "LogClean";
		}

		@Override
		public String description() {
			return null;
		}

		@Override
		public ResultMsg<JSONObject> execute(ExtensionRequestParam extensionRequestParam) {
			return logCleanProvider.execute(extensionRequestParam);
		}

	}
}
