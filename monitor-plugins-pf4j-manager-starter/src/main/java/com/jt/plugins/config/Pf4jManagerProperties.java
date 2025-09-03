package com.jt.plugins.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author blwy_qb
 */
@ConfigurationProperties(prefix = "spring.pf4j")
public class Pf4jManagerProperties {

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 插件目录，文件夹全路径
     */
    private String path;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
