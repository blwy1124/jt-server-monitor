package com.jt.plugins.service;

import com.alibaba.fastjson2.JSONObject;
import com.jt.plugins.common.constant.PluginConstants;
import com.jt.plugins.common.http.ExtensionRequestParam;
import com.jt.plugins.common.result.ResultMsg;
import com.jt.plugins.config.Pf4jManagerProperties;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.service
 * @Author: 别来无恙qb
 * @CreateTime: 2025-09-02  16:43
 * @Description: 插件管理服务类
 * @Version: 1.0
 */
@Service
public class PluginManagerService {

    private static final Logger logger = LoggerFactory.getLogger(PluginManagerService.class);

    private Pf4jManagerProperties pf4jManagerProperties;
    private PluginManager pluginManager;

    @Autowired
    public void setPf4jManagerProperties(Pf4jManagerProperties pf4jManagerProperties) {
        this.pf4jManagerProperties = pf4jManagerProperties;
    }

    @Autowired
    public void setPluginManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    /**
     * 上传插件文件
     * @param file 插件文件
     * @return 上传结果
     */
    public ResultMsg<JSONObject> uploadPlugin(MultipartFile file) {
        try {
            // 参数验证
            if (file == null || file.isEmpty()) {
                return ResultMsg.fail(PluginConstants.INVALID_PLUGIN_FILE);
            }

            // 文件大小检查
            if (file.getSize() > PluginConstants.MAX_FILE_SIZE) {
                return ResultMsg.fail("插件文件过大，最大支持100MB");
            }

            // 文件扩展名检查
            String originalFilename = file.getOriginalFilename();
            if (!StringUtils.hasText(originalFilename) ||
                    !originalFilename.toLowerCase().endsWith(PluginConstants.PLUGIN_FILE_EXTENSION)) {
                return ResultMsg.fail("只支持.jar格式的插件文件");
            }

            // 获取插件目录
            String pluginPath = pf4jManagerProperties.getPath();
            if (!StringUtils.hasText(pluginPath)) {
                return ResultMsg.fail("插件目录未配置");
            }

            // 检查文件是否已存在
            Path targetPath = Paths.get(pluginPath).resolve(originalFilename);
            if (Files.exists(targetPath)) {
                return ResultMsg.fail(PluginConstants.PLUGIN_FILE_EXISTS);
            }

            // 保存文件
            Files.copy(file.getInputStream(), targetPath);

            logger.info("插件文件上传成功: {}", originalFilename);
            return ResultMsg.successMsg("插件文件上传成功");
        } catch (Exception e) {
            logger.error("插件文件上传失败", e);
            return ResultMsg.fail("插件文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 重新加载所有插件
     * @return 重新加载结果
     */
    public ResultMsg<JSONObject> reloadPlugins() {
        try {
            pluginManager.unloadPlugins();
            pluginManager.loadPlugins();
            pluginManager.startPlugins();

            logger.info("所有插件重新加载成功");
            return ResultMsg.successMsg("所有插件重新加载成功");
        } catch (Exception e) {
            logger.error("重新加载插件失败", e);
            return ResultMsg.fail("重新加载插件失败: " + e.getMessage());
        }
    }

    /**
     * 加载指定插件
     * @param request 请求参数
     * @return 加载结果
     */
    public ResultMsg<String> loadPlugin(ExtensionRequestParam request) {
        try {
            String pluginId = request.getParameter("pluginId");
            if (!StringUtils.hasText(pluginId)) {
                return ResultMsg.fail("插件ID不能为空");
            }

            // 检查插件是否已加载
            PluginWrapper plugin = pluginManager.getPlugin(pluginId);
            if (plugin != null) {
                return ResultMsg.successMsg(PluginConstants.PLUGIN_ALREADY_LOADED);
            }

            // 查找插件文件
            String pluginPath = pf4jManagerProperties.getPath();
            Path pluginFilePath = Paths.get(pluginPath).resolve(pluginId + PluginConstants.PLUGIN_FILE_EXTENSION);

            if (!Files.exists(pluginFilePath)) {
                return ResultMsg.fail("插件文件不存在: " + pluginFilePath.toString());
            }

            // 加载插件
            String loadedPluginId = pluginManager.loadPlugin(pluginFilePath);
            if (loadedPluginId != null) {
                logger.info("插件加载成功: {}", pluginId);
                return ResultMsg.success(loadedPluginId, PluginConstants.PLUGIN_LOADED);
            } else {
                return ResultMsg.fail("插件加载失败");
            }
        } catch (Exception e) {
            logger.error("加载插件失败: {}", request.getParameter("pluginId"), e);
            return ResultMsg.fail("加载插件失败: " + e.getMessage());
        }
    }

    /**
     * 卸载指定插件
     * @param request 请求参数
     * @return 卸载结果
     */
    public ResultMsg<JSONObject> unloadPlugin(ExtensionRequestParam request) {
        try {
            String pluginId = request.getParameter("pluginId");
            if (!StringUtils.hasText(pluginId)) {
                return ResultMsg.fail("插件ID不能为空");
            }

            // 检查插件是否存在
            PluginWrapper plugin = pluginManager.getPlugin(pluginId);
            if (plugin == null) {
                return ResultMsg.fail(PluginConstants.PLUGIN_NOT_FOUND);
            }

            // 停止插件
            pluginManager.stopPlugin(pluginId);

            // 卸载插件
            boolean result = pluginManager.unloadPlugin(pluginId);
            if (result) {
                logger.info("插件卸载成功: {}", pluginId);
                return ResultMsg.successMsg(PluginConstants.PLUGIN_UNLOADED);
            } else {
                return ResultMsg.fail("插件卸载失败");
            }
        } catch (Exception e) {
            logger.error("卸载插件失败: {}", request.getParameter("pluginId"), e);
            return ResultMsg.fail("卸载插件失败: " + e.getMessage());
        }
    }

    /**
     * 启动指定插件
     * @param request 请求参数
     * @return 启动结果
     */
    public ResultMsg<PluginState> startPlugin(ExtensionRequestParam request) {
        try {
            String pluginId = request.getParameter("pluginId");
            if (!StringUtils.hasText(pluginId)) {
                return ResultMsg.fail("插件ID不能为空");
            }

            // 检查插件是否存在
            PluginWrapper plugin = pluginManager.getPlugin(pluginId);
            if (plugin == null) {
                return ResultMsg.fail(PluginConstants.PLUGIN_NOT_FOUND);
            }

            // 检查插件状态
            if (plugin.getPluginState() == PluginState.STARTED) {
                return ResultMsg.success(PluginState.STARTED, PluginConstants.PLUGIN_ALREADY_STARTED);
            }

            // 启动插件
            PluginState state = pluginManager.startPlugin(pluginId);
            if (state == PluginState.STARTED) {
                logger.info("插件启动成功: {}", pluginId);
                return ResultMsg.success(state, PluginConstants.PLUGIN_STARTED);
            } else {
                return ResultMsg.fail("插件启动失败，状态: " + state);
            }
        } catch (Exception e) {
            logger.error("启动插件失败: {}", request.getParameter("pluginId"), e);
            return ResultMsg.fail("启动插件失败: " + e.getMessage());
        }
    }

    /**
     * 停止指定插件
     * @param request 请求参数
     * @return 停止结果
     */
    public ResultMsg<PluginState> stopPlugin(ExtensionRequestParam request) {
        try {
            String pluginId = request.getParameter("pluginId");
            if (!StringUtils.hasText(pluginId)) {
                return ResultMsg.fail("插件ID不能为空");
            }

            // 检查插件是否存在
            PluginWrapper plugin = pluginManager.getPlugin(pluginId);
            if (plugin == null) {
                return ResultMsg.fail(PluginConstants.PLUGIN_NOT_FOUND);
            }

            // 停止插件
            PluginState state = pluginManager.stopPlugin(pluginId);
            logger.info("插件停止成功: {}", pluginId);
            return ResultMsg.success(state, PluginConstants.PLUGIN_STOPPED);
        } catch (Exception e) {
            logger.error("停止插件失败: {}", request.getParameter("pluginId"), e);
            return ResultMsg.fail("停止插件失败: " + e.getMessage());
        }
    }

    /**
     * 禁用指定插件
     * @param request 请求参数
     * @return 禁用结果
     */
    public ResultMsg<JSONObject> disablePlugin(ExtensionRequestParam request) {
        try {
            String pluginId = request.getParameter("pluginId");
            if (!StringUtils.hasText(pluginId)) {
                return ResultMsg.fail("插件ID不能为空");
            }

            // 检查插件是否存在
            PluginWrapper plugin = pluginManager.getPlugin(pluginId);
            if (plugin == null) {
                return ResultMsg.fail(PluginConstants.PLUGIN_NOT_FOUND);
            }

            // 停止插件
            pluginManager.stopPlugin(pluginId);

            // 禁用插件
            boolean result = pluginManager.disablePlugin(pluginId);
            if (result) {
                logger.info("插件禁用成功: {}", pluginId);
                return ResultMsg.successMsg(PluginConstants.PLUGIN_DISABLED);
            } else {
                return ResultMsg.fail("插件禁用失败");
            }
        } catch (Exception e) {
            logger.error("禁用插件失败: {}", request.getParameter("pluginId"), e);
            return ResultMsg.fail("禁用插件失败: " + e.getMessage());
        }
    }

    /**
     * 启用指定插件
     * @param request 请求参数
     * @return 启用结果
     */
    public ResultMsg<JSONObject> enablePlugin(ExtensionRequestParam request) {
        try {
            String pluginId = request.getParameter("pluginId");
            if (!StringUtils.hasText(pluginId)) {
                return ResultMsg.fail("插件ID不能为空");
            }

            // 检查插件是否存在
            PluginWrapper plugin = pluginManager.getPlugin(pluginId);
            if (plugin == null) {
                return ResultMsg.fail(PluginConstants.PLUGIN_NOT_FOUND);
            }

            // 启用插件
            boolean result = pluginManager.enablePlugin(pluginId);
            if (result) {
                logger.info("插件启用成功: {}", pluginId);
                return ResultMsg.successMsg(PluginConstants.PLUGIN_ENABLED);
            } else {
                return ResultMsg.fail("插件启用失败");
            }
        } catch (Exception e) {
            logger.error("启用插件失败: {}", request.getParameter("pluginId"), e);
            return ResultMsg.fail("启用插件失败: " + e.getMessage());
        }
    }

    /**
     * 删除指定插件
     * @param request 请求参数
     * @return 删除结果
     */
    public ResultMsg<JSONObject> deletePlugin(ExtensionRequestParam request) {
        try {
            String pluginId = request.getParameter("pluginId");
            if (!StringUtils.hasText(pluginId)) {
                return ResultMsg.fail("插件ID不能为空");
            }

            // 检查插件是否存在
            PluginWrapper plugin = pluginManager.getPlugin(pluginId);
            if (plugin == null) {
                if (plugin.getPluginPath() != null) {
                    Files.deleteIfExists(plugin.getPluginPath());
                    return ResultMsg.successMsg("插件删除成功");
                }
                return ResultMsg.fail(PluginConstants.PLUGIN_NOT_FOUND);
            }

            // 删除插件文件
            boolean result = pluginManager.deletePlugin(pluginId);
            if (result) {
                logger.info("插件删除成功: {}", pluginId);
                return ResultMsg.successMsg(PluginConstants.PLUGIN_DELETED);
            } else {
                return ResultMsg.fail("插件删除失败，请重新尝试");
            }
        } catch (Exception e) {
            logger.error("删除插件失败: {}", request.getParameter("pluginId"), e);
            return ResultMsg.fail("删除插件失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有插件列表
     * @return 插件列表
     */
    public ResultMsg<List<JSONObject>> getPlugins() {
        try {
            List<JSONObject> list = new ArrayList<>();
            List<PluginWrapper> plugins = pluginManager.getPlugins();

            for (PluginWrapper plugin : plugins) {
                JSONObject pluginInfo = new JSONObject()
                        .fluentPut("id", plugin.getPluginId())
                        .fluentPut("version", plugin.getDescriptor().getVersion())
                        .fluentPut("path", plugin.getPluginPath() != null ? plugin.getPluginPath().toString() : "")
                        .fluentPut("state", plugin.getPluginState().toString())
                        .fluentPut("class", plugin.getDescriptor().getPluginClass())
                        .fluentPut("provider", plugin.getDescriptor().getProvider());

                list.add(pluginInfo);
            }

            return ResultMsg.success(list);
        } catch (Exception e) {
            logger.error("获取插件列表失败", e);
            return ResultMsg.fail("获取插件列表失败: " + e.getMessage());
        }
    }
}
