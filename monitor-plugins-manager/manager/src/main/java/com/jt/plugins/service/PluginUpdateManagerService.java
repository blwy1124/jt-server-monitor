package com.jt.plugins.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jt.plugins.common.http.ExtensionRequestParam;
import com.jt.plugins.common.result.ResultMsg;
import com.jt.plugins.common.config.AppConfiguration;
import com.jt.plugins.config.Pf4jManagerProperties;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.service
 * @Author: 别来无恙qb
 * @CreateTime: 2025-09-03  09:58
 * @Description: 插件更新管理服务
 * @Version: 1.0
 */
@Service
public class PluginUpdateManagerService {

    private static final Logger logger = LoggerFactory.getLogger(PluginUpdateManagerService.class);

    @Autowired
    private PluginManager pluginManager;

    @Autowired
    private Pf4jManagerProperties pf4jManagerProperties;

    @Autowired
    private AppConfiguration appConfiguration;

    // 常量定义
    private static final int CONNECT_TIMEOUT = 10000; // 10秒连接超时
    private static final int READ_TIMEOUT = 60000;   // 60秒读取超时
    private static final int MAX_RETRY_ATTEMPTS = 3; // 最大重试次数
    private static final int BUFFER_SIZE = 8192;     // 缓冲区大小

    /**
     * 设置仓库地址
     * @param request 包含仓库URL的请求参数
     * @return 操作结果
     */
    public ResultMsg<String> setRepository(ExtensionRequestParam request) {
        try {
            String repositoryUrl = request.getParameter("repositoryUrl");

            if (repositoryUrl == null || repositoryUrl.isEmpty()) {
                logger.warn("设置仓库地址失败：仓库URL为空");
                return ResultMsg.fail("仓库URL不能为空");
            }

            // 更新应用配置中的仓库地址
            appConfiguration.setRepositoryUrl(repositoryUrl);
            logger.info("仓库地址设置成功：{}", repositoryUrl);

            return ResultMsg.successMsg("仓库地址设置成功");
        } catch (Exception e) {
            logger.error("设置仓库地址异常", e);
            return ResultMsg.fail("设置仓库地址异常: " + e.getMessage());
        }
    }

    /**
     * 获取当前使用的仓库地址
     * @return 仓库地址
     */
    private String getRepositoryUrl() {
        // 检查应用配置中的仓库地址
        String appConfigUrl = appConfiguration.getRepositoryUrl();
        if (appConfigUrl != null && !appConfigUrl.isEmpty()) {
            return appConfigUrl;
        }

        return null;
    }

    /**
     * 获取远程插件信息
     * @param pluginId 插件ID
     * @return 插件信息
     * @throws IOException IO异常
     */
    private JSONObject getRemotePluginInfo(String pluginId) throws IOException {
        String repositoryUrl = getRepositoryUrl();
        if (repositoryUrl == null || repositoryUrl.isEmpty()) {
            logger.warn("未配置插件仓库地址");
            throw new IOException("未配置插件仓库地址");
        }

        // 确保URL以/结尾
        if (!repositoryUrl.endsWith("/")) {
            repositoryUrl += "/";
        }

        // 构建元数据文件URL
        String metadataUrl = repositoryUrl + "plugins.json";
        logger.debug("正在获取远程插件信息，插件ID: {}, 元数据URL: {}", pluginId, metadataUrl);

        try {
            // 下载并解析元数据文件
            String metadataContent = downloadTextWithRetry(metadataUrl);
            JSONObject metadata = JSON.parseObject(metadataContent);

            JSONArray plugins = metadata.getJSONArray("plugins");
            if (plugins != null) {
                for (int i = 0; i < plugins.size(); i++) {
                    JSONObject pluginInfo = plugins.getJSONObject(i);
                    if (pluginId.equals(pluginInfo.getString("id"))) {
                        logger.debug("找到远程插件信息，插件ID: {}", pluginId);
                        return pluginInfo;
                    }
                }
            }

            logger.warn("远程仓库中未找到插件信息，插件ID: {}", pluginId);
            return null;
        } catch (Exception e) {
            logger.error("获取远程插件信息失败，插件ID: {}, URL: {}", pluginId, metadataUrl, e);
            throw new IOException("获取远程插件信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 带重试机制的文本下载方法
     * @param urlString URL地址
     * @return 文本内容
     * @throws IOException IO异常
     */
    private String downloadTextWithRetry(String urlString) throws IOException {
        IOException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                logger.debug("尝试下载文本 (第{}次尝试): {}", attempt, urlString);
                String result = downloadText(urlString);
                logger.debug("文本下载成功 (第{}次尝试): {}", attempt, urlString);
                return result;
            } catch (SocketTimeoutException e) {
                lastException = e;
                logger.warn("文本下载超时 (第{}次尝试): {}", attempt, urlString);
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    // 等待一段时间后重试
                    try {
                        Thread.sleep(1000 * attempt); // 递增等待时间
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("下载被中断", ie);
                    }
                }
            } catch (IOException e) {
                lastException = e;
                logger.warn("文本下载失败 (第{}次尝试): {}", attempt, urlString, e);
                // 对于其他IO异常，不重试直接抛出
                throw e;
            }
        }

        logger.error("文本下载最终失败，已重试{}次: {}", MAX_RETRY_ATTEMPTS, urlString);
        throw new IOException("下载失败，已重试" + MAX_RETRY_ATTEMPTS + "次", lastException);
    }

    /**
     * 从URL下载文本内容
     * @param urlString URL地址
     * @return 文本内容
     * @throws IOException IO异常
     */
    private String downloadText(String urlString) throws IOException {
        // 验证URL格式
        if (urlString == null || urlString.isEmpty()) {
            throw new IOException("URL不能为空");
        }

        URL url;
        try {
            url = new URL(urlString);
        } catch (Exception e) {
            logger.error("无效的URL格式: {}", urlString);
            throw new IOException("无效的URL格式: " + urlString, e);
        }

        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("User-Agent", "Plugin-Manager/1.0");

            // 检查响应码
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + responseCode + ": " + connection.getResponseMessage());
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                StringBuilder content = new StringBuilder();
                char[] buffer = new char[BUFFER_SIZE];
                int charsRead;

                while ((charsRead = reader.read(buffer)) != -1) {
                    content.append(buffer, 0, charsRead);
                }

                return content.toString();
            }
        } catch (SocketTimeoutException e) {
            throw new SocketTimeoutException("下载超时 (URL: " + urlString + "): " + e.getMessage());
        } catch (IOException e) {
            throw new IOException("下载失败 (URL: " + urlString + "): " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 检查插件更新
     * @param request 包含插件ID等信息的请求参数
     * @return 更新信息
     */
    public ResultMsg<JSONObject> checkForUpdate(ExtensionRequestParam request) {
        try {
            String pluginId = request.getParameter("pluginId");

            if (pluginId == null || pluginId.isEmpty()) {
                logger.warn("检查插件更新失败：插件ID为空");
                return ResultMsg.fail("插件ID不能为空");
            }

            // 获取本地插件信息
            PluginWrapper localPlugin = pluginManager.getPlugin(pluginId);
            if (localPlugin == null) {
                logger.warn("检查插件更新失败：本地未安装该插件，插件ID: {}", pluginId);
                return ResultMsg.fail("本地未安装该插件");
            }

            String localVersion = localPlugin.getDescriptor().getVersion();

            // 获取远程插件信息
            JSONObject remotePluginInfo = getRemotePluginInfo(pluginId);
            if (remotePluginInfo == null) {
                logger.warn("检查插件更新失败：远程仓库中未找到该插件，插件ID: {}", pluginId);
                return ResultMsg.fail("远程仓库中未找到该插件");
            }

            String remoteVersion = remotePluginInfo.getString("version");

            // 比较版本
            boolean hasUpdate = compareVersions(remoteVersion, localVersion) > 0;
            logger.info("插件更新检查完成，插件ID: {}, 本地版本: {}, 远程版本: {}, 有更新: {}",
                    pluginId, localVersion, remoteVersion, hasUpdate);

            JSONObject result = new JSONObject();
            result.put("pluginId", pluginId);
            result.put("localVersion", localVersion);
            result.put("remoteVersion", remoteVersion);
            result.put("hasUpdate", hasUpdate);
            result.put("updateAvailable", hasUpdate);

            return ResultMsg.success(result, hasUpdate ? "发现新版本" : "当前已是最新版本");
        } catch (Exception e) {
            logger.error("检查插件更新异常，插件ID: {}", request.getParameter("pluginId"), e);
            return ResultMsg.fail("检查更新异常: " + e.getMessage());
        }
    }

    /**
     * 更新或安装插件
     * @param request 包含插件ID等信息的请求参数
     * @return 操作结果
     */
    public ResultMsg<String> updateOrInstallPlugin(ExtensionRequestParam request) {
        try {
            String pluginId = request.getParameter("pluginId");

            if (pluginId == null || pluginId.isEmpty()) {
                logger.warn("更新或安装插件失败：插件ID为空");
                return ResultMsg.fail("插件ID不能为空");
            }

            logger.info("开始更新或安装插件: {}", pluginId);

            // 检查插件是否已安装
            PluginWrapper plugin = pluginManager.getPlugin(pluginId);

            // 如果已安装，检查是否有更新
            if (plugin != null) {
                try {
                    JSONObject remotePluginInfo = getRemotePluginInfo(pluginId);
                    if (remotePluginInfo != null) {
                        String remoteVersion = remotePluginInfo.getString("version");
                        String localVersion = plugin.getDescriptor().getVersion();

                        // 如果版本相同或更旧，则无需更新
                        if (compareVersions(remoteVersion, localVersion) <= 0) {
                            logger.info("插件已是最新版本，无需更新: {}", pluginId);
                            return ResultMsg.successMsg("当前已是最新版本，无需更新");
                        }
                    }
                } catch (Exception e) {
                    logger.warn("获取远程插件信息失败，继续执行更新流程: {}", pluginId, e);
                    // 如果无法获取远程信息，继续执行更新流程
                }
            }

            // 执行更新或安装
            ResultMsg<String> result = performUpdateOrInstall(pluginId, plugin);
            logger.info("插件更新或安装完成: {}, 结果: {}", pluginId, result.getMessage());
            return result;
        } catch (Exception e) {
            logger.error("更新或安装插件异常: {}", request.getParameter("pluginId"), e);
            return ResultMsg.fail("操作异常: " + e.getMessage());
        }
    }

    /**
     * 执行更新或安装操作
     * @param pluginId 插件ID
     * @param existingPlugin 已存在的插件（如果有的话）
     * @return 操作结果
     */
    private ResultMsg<String> performUpdateOrInstall(String pluginId, PluginWrapper existingPlugin) {
        Path tempPluginPath = null;
        try {
            logger.info("{}插件: {}", existingPlugin != null ? "更新" : "安装", pluginId);

            // 获取远程插件信息
            JSONObject remotePluginInfo = getRemotePluginInfo(pluginId);
            if (remotePluginInfo == null) {
                logger.warn("远程仓库中未找到插件: {}", pluginId);
                return ResultMsg.fail("远程仓库中未找到该插件");
            }

            String downloadUrl = remotePluginInfo.getString("jarUrl");
            String expectedSha512 = remotePluginInfo.getString("sha512");
            String remoteVersion = remotePluginInfo.getString("version");

            // 如果是更新操作，先验证版本
            if (existingPlugin != null) {
                String localVersion = existingPlugin.getDescriptor().getVersion();
                // 如果版本相同或更旧，则无需更新
                if (compareVersions(remoteVersion, localVersion) <= 0) {
                    logger.info("插件版本已是最新，无需更新: {}", pluginId);
                    return ResultMsg.successMsg("当前已是最新版本，无需更新");
                }
            }

            if (downloadUrl == null || downloadUrl.isEmpty()) {
                // 如果没有指定jarUrl，则使用默认命名规则
                String repositoryUrl = getRepositoryUrl();
                if (repositoryUrl == null || repositoryUrl.isEmpty()) {
                    logger.warn("未配置插件仓库地址");
                    return ResultMsg.fail("未配置插件仓库地址");
                }

                // 确保URL以/结尾
                if (!repositoryUrl.endsWith("/")) {
                    repositoryUrl += "/";
                }

                downloadUrl = repositoryUrl + pluginId + ".jar";
            }

            // 验证下载URL格式
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                logger.error("插件下载URL为空，插件ID: {}", pluginId);
                return ResultMsg.fail("插件下载URL配置错误");
            }

            // 创建临时文件路径
            String fileName = pluginId + ".jar";
            Path pluginDir = Paths.get(pf4jManagerProperties.getPath());

            // 确保插件目录存在
            if (!Files.exists(pluginDir)) {
                logger.info("创建插件目录: {}", pluginDir);
                Files.createDirectories(pluginDir);
            }

            tempPluginPath = pluginDir.resolve(fileName + ".tmp");
            logger.debug("创建临时文件路径: {}", tempPluginPath);

            // 下载文件
            logger.info("开始下载插件文件: {}", downloadUrl);
            downloadFileWithRetry(downloadUrl, tempPluginPath);
            logger.info("插件文件下载完成: {}", downloadUrl);

            // 验证SHA512校验和
            if (expectedSha512 != null && !expectedSha512.isEmpty()) {
                logger.info("验证插件文件完整性: {}", pluginId);
                String actualSha512 = calculateSHA512(tempPluginPath);
                if (!expectedSha512.equalsIgnoreCase(actualSha512)) {
                    // SHA512校验失败，删除临时文件
                    Files.deleteIfExists(tempPluginPath);
                    logger.warn("插件文件校验失败，可能文件已损坏: {}", pluginId);
                    return ResultMsg.fail("插件文件校验失败，可能文件已损坏");
                }
                logger.info("插件文件校验通过: {}", pluginId);
            }

            // 如果是更新操作，先停止并卸载现有插件，并将旧文件移动到临时目录
            if (existingPlugin != null) {
                logger.info("删除旧插件: {}", pluginId);
                try {
//                    pluginManager.unloadPlugin(pluginId);
                    pluginManager.deletePlugin(pluginId);
                } catch (Exception e) {
                    logger.warn("删除插件时出现异常: {}", pluginId, e);
                }

                // 将旧文件移动到临时目录
//                Path oldPluginPath = existingPlugin.getPluginPath();
//                if (oldPluginPath != null && Files.exists(oldPluginPath)) {
//                    Path backupPath = pluginDir.resolve(fileName + ".bak");
//                    logger.debug("将旧插件文件移动到临时位置: {} -> {}", oldPluginPath, backupPath);
//                    try {
//                        Files.move(oldPluginPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
//                        logger.debug("旧插件文件已移动到临时位置: {}", backupPath);
//                    } catch (Exception e) {
//                        logger.error("移动旧插件文件失败: {} -> {}", oldPluginPath, backupPath, e);
//                        // 如果移动失败，尝试删除临时文件
//                        Files.deleteIfExists(tempPluginPath);
//                        return ResultMsg.fail("无法移动旧插件文件: " + e.getMessage());
//                    }
//                }
            }

            // 将新文件移动到正式位置
            Path finalPluginPath = pluginDir.resolve(fileName);
            logger.debug("将新插件文件移动到正式位置: {} -> {}", tempPluginPath, finalPluginPath);
            try {
                Files.move(tempPluginPath, finalPluginPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                logger.error("移动新插件文件失败: {} -> {}", tempPluginPath, finalPluginPath, e);
                // 如果移动失败，尝试删除临时文件
                Files.deleteIfExists(tempPluginPath);
                return ResultMsg.fail("无法移动新插件文件到目标位置: " + e.getMessage());
            }

            // 加载并启动插件
            logger.info("加载插件: {}", pluginId);
            String loadedPluginId = pluginManager.loadPlugin(finalPluginPath);
            if (loadedPluginId != null) {
                logger.info("启动插件: {}", pluginId);
                try {
                    pluginManager.startPlugin(loadedPluginId);
                } catch (Exception e) {
                    logger.warn("启动插件时出现异常: {}", pluginId, e);
                }

                if (existingPlugin != null) {
                    logger.info("插件更新成功: {}", pluginId);
                    return ResultMsg.successMsg("插件更新成功，版本: " + remoteVersion);
                } else {
                    logger.info("插件安装成功: {}", pluginId);
                    return ResultMsg.successMsg("插件安装成功，版本: " + remoteVersion);
                }
            } else {
                // 插件加载失败，清理文件
                Files.deleteIfExists(finalPluginPath);
                logger.error("插件加载失败: {}", pluginId);
                return ResultMsg.fail("插件加载失败");
            }
        } catch (Exception e) {
            // 发生异常时，清理临时文件
            if (tempPluginPath != null) {
                try {
                    Files.deleteIfExists(tempPluginPath);
                } catch (IOException ioException) {
                    logger.warn("删除临时文件失败: {}", tempPluginPath, ioException);
                }
            }
            logger.error("{}插件失败: {}", existingPlugin != null ? "更新" : "安装", pluginId, e);
            return ResultMsg.fail("操作异常: " + e.getMessage());
        }
    }

    /**
     * 带重试机制的文件下载方法
     * @param urlString URL地址
     * @param destination 目标路径
     * @throws IOException IO异常
     */
    private void downloadFileWithRetry(String urlString, Path destination) throws IOException {
        IOException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                logger.debug("尝试下载文件 (第{}次尝试): {}", attempt, urlString);
                downloadFile(urlString, destination);
                logger.debug("文件下载成功 (第{}次尝试): {}", attempt, urlString);
                return; // 成功下载则返回
            } catch (SocketTimeoutException e) {
                lastException = e;
                logger.warn("文件下载超时 (第{}次尝试): {}", attempt, urlString);
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    // 等待一段时间后重试
                    try {
                        Thread.sleep(1000 * attempt); // 递增等待时间
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("下载被中断", ie);
                    }
                }
            } catch (IOException e) {
                lastException = e;
                logger.warn("文件下载失败 (第{}次尝试): {}", attempt, urlString, e);
                // 对于其他IO异常，不重试直接抛出
                throw e;
            }
        }

        logger.error("文件下载最终失败，已重试{}次: {}", MAX_RETRY_ATTEMPTS, urlString);
        throw new IOException("文件下载失败，已重试" + MAX_RETRY_ATTEMPTS + "次", lastException);
    }

    /**
     * 从URL下载文件
     * @param urlString URL地址
     * @param destination 目标路径
     * @throws IOException IO异常
     */
    private void downloadFile(String urlString, Path destination) throws IOException {
        // 验证URL格式
        if (urlString == null || urlString.isEmpty()) {
            throw new IOException("URL不能为空");
        }

        URL url;
        try {
            url = new URL(urlString);
        } catch (Exception e) {
            logger.error("无效的文件下载URL格式: {}", urlString);
            throw new IOException("无效的文件下载URL格式: " + urlString, e);
        }

        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("User-Agent", "Plugin-Manager/1.0");

            // 检查响应码
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + responseCode + ": " + connection.getResponseMessage());
            }

            // 确保目标目录存在
            Path parentDir = destination.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                logger.info("创建目录: {}", parentDir);
                Files.createDirectories(parentDir);
            }

            // 下载文件
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(destination.toFile())) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                long totalBytesRead = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
                logger.debug("文件下载完成，总字节数: {}", totalBytesRead);
            }
        } catch (SocketTimeoutException e) {
            throw new SocketTimeoutException("文件下载超时 (URL: " + urlString + "): " + e.getMessage());
        } catch (IOException e) {
            // 下载失败时删除临时文件
            Files.deleteIfExists(destination);
            throw new IOException("文件下载失败 (URL: " + urlString + "): " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 安装插件
     * @param request 包含插件ID等信息的请求参数
     * @return 是否安装成功
     */
    public ResultMsg<String> installPlugin(ExtensionRequestParam request) {
        try {
            String pluginId = request.getParameter("pluginId");

            if (pluginId == null || pluginId.isEmpty()) {
                logger.warn("安装插件失败：插件ID为空");
                return ResultMsg.fail("插件ID不能为空");
            }

            logger.info("开始安装插件: {}", pluginId);

            // 检查插件是否已安装
            if (pluginManager.getPlugin(pluginId) != null) {
                logger.warn("插件已存在，请使用更新功能: {}", pluginId);
                return ResultMsg.fail("插件已存在，请使用更新功能");
            }

            // 执行安装
            ResultMsg<String> result = performUpdateOrInstall(pluginId, null);
            logger.info("插件安装完成: {}, 结果: {}", pluginId, result.getMessage());
            return result;
        } catch (Exception e) {
            logger.error("安装插件异常: {}", request.getParameter("pluginId"), e);
            return ResultMsg.fail("安装异常: " + e.getMessage());
        }
    }

    /**
     * 更新插件
     * @param request 包含插件ID等信息的请求参数
     * @return 是否更新成功
     */
    public ResultMsg<String> updatePlugin(ExtensionRequestParam request) {
        try {
            String pluginId = request.getParameter("pluginId");

            if (pluginId == null || pluginId.isEmpty()) {
                logger.warn("更新插件失败：插件ID为空");
                return ResultMsg.fail("插件ID不能为空");
            }

            logger.info("开始更新插件: {}", pluginId);

            // 检查插件是否已安装
            PluginWrapper plugin = pluginManager.getPlugin(pluginId);
            if (plugin == null) {
                logger.warn("插件未安装，请使用安装功能: {}", pluginId);
                return ResultMsg.fail("插件未安装，请使用安装功能");
            }

            // 执行更新
            ResultMsg<String> result = performUpdateOrInstall(pluginId, plugin);
            logger.info("插件更新完成: {}, 结果: {}", pluginId, result.getMessage());
            return result;
        } catch (Exception e) {
            logger.error("更新插件异常: {}", request.getParameter("pluginId"), e);
            return ResultMsg.fail("更新异常: " + e.getMessage());
        }
    }

    /**
     * 计算文件的SHA512哈希值
     * @param filePath 文件路径
     * @return SHA512哈希值
     * @throws IOException IO异常
     */
    private String calculateSHA512(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new IOException("计算SHA512失败", e);
        }
    }

    /**
     * 比较两个版本号
     * @param version1 版本号1
     * @param version2 版本号2
     * @return 如果version1 > version2返回1，相等返回0，小于返回-1
     */
    private int compareVersions(String version1, String version2) {
        if (version1 == null || version2 == null) {
            return 0;
        }

        // 简单的版本号比较实现
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int v1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int v2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (v1 > v2) {
                return 1;
            } else if (v1 < v2) {
                return -1;
            }
        }

        return 0;
    }
}
