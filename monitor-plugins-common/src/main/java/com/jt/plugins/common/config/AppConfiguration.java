package com.jt.plugins.common.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.jt.plugins.common.file.PluginFileStorage;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 应用配置管理类
 * 负责管理存储在BASE_STORAGE_PATH/config/appConfig.json中的应用配置
 */
@Component
public class AppConfiguration {

    private static final String CONFIG_FILE_NAME = "appConfig.json";
    private JSONObject config;
    private final Path configFilePath;

    public AppConfiguration() {
        // 初始化配置文件路径
        String baseStoragePath = PluginFileStorage.getBaseStoragePathStatic();
        this.configFilePath = Paths.get(baseStoragePath, "config", CONFIG_FILE_NAME);
    }

    @PostConstruct
    public void init() {
        loadConfig();
    }

    /**
     * 加载配置文件
     */
    private void loadConfig() {
        try {
            // 确保配置目录存在
            Files.createDirectories(configFilePath.getParent());

            // 如果配置文件存在，读取配置
            if (Files.exists(configFilePath)) {
                String content = new String(Files.readAllBytes(configFilePath));
                this.config = JSON.parseObject(content);
            } else {
                // 如果配置文件不存在，创建默认配置
                this.config = new JSONObject();
                saveConfig();
            }
        } catch (Exception e) {
            // 如果加载失败，使用空配置
            this.config = new JSONObject();
        }
    }

    /**
     * 保存配置到文件
     */
    private void saveConfig() {
        try {
            // 确保配置目录存在
            Files.createDirectories(configFilePath.getParent());

            // 写入配置文件
            Files.write(configFilePath, config.toJSONString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException("保存配置文件失败", e);
        }
    }

    /**
     * 获取仓库地址
     * @return 仓库地址
     */
    public String getRepositoryUrl() {
        return config.getString("repositoryUrl");
    }

    /**
     * 设置仓库地址
     * @param repositoryUrl 仓库地址
     */
    public void setRepositoryUrl(String repositoryUrl) {
        config.put("repositoryUrl", repositoryUrl);
        saveConfig();
    }

    /**
     * 获取配置文件路径
     * @return 配置文件路径
     */
    public Path getConfigFilePath() {
        return configFilePath;
    }
}
