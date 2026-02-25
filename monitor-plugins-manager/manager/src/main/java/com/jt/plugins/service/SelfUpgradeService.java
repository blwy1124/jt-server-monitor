package com.jt.plugins.service;

import com.alibaba.fastjson.JSONObject;
import com.jt.plugins.common.config.AppConfiguration;
import com.jt.plugins.common.http.ExtensionRequestParam;
import com.jt.plugins.common.result.ResultMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * 应用自升级服务
 * 实现应用自己升级自己的功能
 */
@Service
public class SelfUpgradeService {

    private static final Logger logger = LoggerFactory.getLogger(SelfUpgradeService.class);

    @Autowired
    private AppConfiguration appConfiguration;

    // 升级状态标志
    private final AtomicBoolean isUpgrading = new AtomicBoolean(false);

    /**
     * 定时检查更新（每天凌晨2点检查）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledCheckForUpdates() {
        try {
            if (isUpgrading.get()) {
                logger.info("正在进行升级，跳过定时检查");
                return;
            }

            logger.info("开始定时检查应用更新");
            performSelfUpgrade(true);
        } catch (Exception e) {
            logger.error("定时检查更新失败", e);
        }
    }

    /**
     * 手动触发自升级
     */
    public ResultMsg<String> manualSelfUpgrade(ExtensionRequestParam request) {
        try {
            boolean autoConfirm = Boolean.parseBoolean(request.getParameter("autoConfirm", "false"));
            String result = performSelfUpgrade(autoConfirm);
            return ResultMsg.success(result);
        } catch (Exception e) {
            logger.error("手动自升级失败", e);
            return ResultMsg.fail("自升级失败: " + e.getMessage());
        }
    }

    /**
     * 执行自升级
     * @param autoConfirm 是否自动确认升级
     * @return 升级结果
     */
    private String performSelfUpgrade(boolean autoConfirm) {
        if (!isUpgrading.compareAndSet(false, true)) {
            return "升级已在进行中";
        }

        try {
            logger.info("开始执行自升级流程...");

            // 1. 获取当前应用信息
            String currentVersion = getCurrentAppVersion();
            String currentJarPath = getCurrentJarPath();
            logger.info("当前版本: {}, 当前路径: {}", currentVersion, currentJarPath);

            // 2. 检查是否有新版本
            JSONObject remoteAppInfo = getRemoteAppInfo();
            if (remoteAppInfo == null) {
                isUpgrading.set(false);
                return "无法获取远程应用信息";
            }

            String remoteVersion = remoteAppInfo.getString("version");
            logger.info("远程版本: {}", remoteVersion);

            // 3. 比较版本
            int versionComparison = compareVersions(remoteVersion, currentVersion);
            if (versionComparison <= 0) {
                isUpgrading.set(false);
                return "当前已是最新版本 (v" + currentVersion + ")";
            }

            // 4. 如果不是自动确认，只返回信息
            if (!autoConfirm) {
                isUpgrading.set(false);
                return "发现新版本 v" + remoteVersion + "，当前版本 v" + currentVersion + "。确认升级请设置 autoConfirm=true";
            }

            logger.info("发现新版本，开始升级流程...");

            // 5. 下载新版本
            String downloadUrl = remoteAppInfo.getString("downloadUrl");
            String expectedSha512 = remoteAppInfo.getString("sha512");

            if (downloadUrl == null || downloadUrl.isEmpty()) {
                // 使用默认URL格式
                String repositoryUrl = appConfiguration.getRepositoryUrl();
                if (repositoryUrl != null && !repositoryUrl.isEmpty()) {
                    if (!repositoryUrl.endsWith("/")) {
                        repositoryUrl += "/";
                    }
                    downloadUrl = repositoryUrl + "app-" + remoteVersion + ".jar";
                }
            }

            if (downloadUrl == null || downloadUrl.isEmpty()) {
                isUpgrading.set(false);
                throw new Exception("未配置下载URL");
            }

            logger.info("开始下载新版本: {}", downloadUrl);
            Path tempNewAppPath = downloadNewVersion(downloadUrl);

            // 6. 验证文件完整性
            if (expectedSha512 != null && !expectedSha512.isEmpty()) {
                logger.info("验证文件完整性...");
                String actualSha512 = calculateSHA512(tempNewAppPath);
                if (!expectedSha512.equalsIgnoreCase(actualSha512)) {
                    Files.deleteIfExists(tempNewAppPath);
                    isUpgrading.set(false);
                    throw new Exception("新版本文件校验失败");
                }
                logger.info("文件校验通过");
            }

            // 7. 备份当前版本
            logger.info("备份当前版本...");
            backupCurrentVersion(currentJarPath, remoteVersion);

            // 8. 替换为新版本
            logger.info("替换为新版本...");
            replaceWithNewVersion(currentJarPath, tempNewAppPath);

            // 9. 创建重启脚本
            logger.info("创建重启脚本...");
            createRestartScript(currentJarPath);

            // 10. 执行重启
            logger.info("执行重启，应用将升级到版本: {}", remoteVersion);
            executeRestart();

            return "升级成功，应用将重启到新版本 v" + remoteVersion;

        } catch (Exception e) {
            logger.error("自升级失败", e);
            isUpgrading.set(false);
            return "升级失败: " + e.getMessage();
        }
    }

    /**
     * 获取当前应用版本
     */
    public String getCurrentAppVersion() {
        try {
            // 方法1: 从MANIFEST.MF获取版本（Maven打包时会自动包含）
            String jarPath = getCurrentJarPath();
            if (jarPath != null && !jarPath.isEmpty()) {
                try (JarFile jarFile = new JarFile(jarPath)) {
                    Manifest manifest = jarFile.getManifest();
                    if (manifest != null) {
                        Attributes attributes = manifest.getMainAttributes();
                        // Maven打包时会将版本信息放在Implementation-Version属性中
                        String version = attributes.getValue("Implementation-Version");
                        if (version != null && !version.isEmpty()) {
                            logger.info("从MANIFEST.MF获取版本: {}", version);
                            return version;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("从MANIFEST.MF读取版本失败", e);
                }
            }

            // 方法2: 从系统属性获取（启动时通过-D参数设置）
            String version = System.getProperty("app.version");
            if (version != null && !version.isEmpty()) {
                logger.info("从系统属性获取版本: {}", version);
                return version;
            }

            // 方法3: 从环境变量获取
            version = System.getenv("APP_VERSION");
            if (version != null && !version.isEmpty()) {
                logger.info("从环境变量获取版本: {}", version);
                return version;
            }

            // 方法4: 从pom.properties文件获取（如果存在）
            version = getVersionFromPomProperties();
            if (version != null && !version.isEmpty()) {
                logger.info("从pom.properties获取版本: {}", version);
                return version;
            }

            // 方法5: 默认版本
            logger.warn("无法获取版本信息，使用默认版本");
            return "1.0.0";
        } catch (Exception e) {
            logger.error("获取当前版本失败", e);
            return "1.0.0";
        }
    }

    /**
     * 从pom.properties文件获取版本
     */
    private String getVersionFromPomProperties() {
        try {
            // 尝试从classpath加载pom.properties
            Properties props = new Properties();
            try (InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("META-INF/maven/com.jt.plugins/jt-server-monitor/pom.properties")) {
                if (is != null) {
                    props.load(is);
                    return props.getProperty("version");
                }
            }
        } catch (Exception e) {
            logger.warn("从pom.properties读取版本失败", e);
        }
        return null;
    }

    /**
     * 获取当前jar文件路径
     */
    private String getCurrentJarPath() {
        try {
            return new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        } catch (Exception e) {
            logger.warn("获取当前jar路径失败", e);
            return System.getProperty("user.dir") + "/hisServiceMonitor.jar";
        }
    }

    /**
     * 下载新版本应用
     */
    private Path downloadNewVersion(String downloadUrl) throws Exception {
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path tempNewAppPath = tempDir.resolve("app-new-" + System.currentTimeMillis() + ".jar");

        logger.info("下载到临时文件: {}", tempNewAppPath.toString());

        URL url = new URL(downloadUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(300000); // 5分钟超时

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("下载失败，HTTP响应码: " + responseCode);
        }

        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(tempNewAppPath.toFile())) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }

            logger.info("下载完成，文件大小: {} bytes", totalBytes);
        } catch (Exception e) {
            Files.deleteIfExists(tempNewAppPath);
            throw e;
        }

        return tempNewAppPath;
    }

    /**
     * 备份当前版本
     */
    private void backupCurrentVersion(String currentJarPath, String newVersion) throws Exception {
        Path currentJar = Paths.get(currentJarPath);
        Path backupDir = currentJar.getParent().resolve("backups");

        // 创建备份目录
        Files.createDirectories(backupDir);

        // 生成备份文件名
        String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path backupPath = backupDir.resolve("app-v" + getCurrentAppVersion() + "-" + timestamp + ".jar");

        // 复制当前版本到备份目录
        Files.copy(currentJar, backupPath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("当前版本已备份到: {}", backupPath.toString());
    }

    /**
     * 替换为新版本
     */
    private void replaceWithNewVersion(String currentJarPath, Path newAppPath) throws Exception {
        Path currentJar = Paths.get(currentJarPath);

        // 在Windows上，我们不能直接替换正在运行的jar文件
        // 所以我们先重命名为临时名称，然后替换
        Path tempCurrentJar = currentJar.getParent().resolve("app-current.tmp");

        // 先将当前jar重命名为临时名称
        if (Files.exists(currentJar)) {
            Files.move(currentJar, tempCurrentJar, StandardCopyOption.REPLACE_EXISTING);
        }

        // 将新版本移动到正确位置
        Files.move(newAppPath, currentJar, StandardCopyOption.REPLACE_EXISTING);

        // 删除临时文件
        Files.deleteIfExists(tempCurrentJar);

        logger.info("新版本已替换成功");
    }

    /**
     * 创建重启脚本
     */
    private void createRestartScript(String jarPath) throws Exception {
        Path appDir = Paths.get(jarPath).getParent();
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Windows批处理脚本
            Path batScriptPath = appDir.resolve("restart.bat");
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(batScriptPath))) {
                writer.println("@echo off");
                writer.println("chcp 65001 >nul"); // 设置UTF-8编码
                writer.println("echo 等待应用停止...");
                writer.println("timeout /t 5 /nobreak >nul");
                writer.println("echo 启动新版本应用...");
                writer.println("cd /d \"" + appDir.toString() + "\"");
                writer.println("java -jar \"" + jarPath + "\"");
            }
            logger.info("Windows重启脚本已创建: {}", batScriptPath.toString());
        } else {
            // Linux/Unix shell脚本
            Path shScriptPath = appDir.resolve("restart.sh");
            try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(shScriptPath))) {
                writer.println("#!/bin/bash");
                writer.println("echo '等待应用停止...'");
                writer.println("sleep 5");
                writer.println("echo '启动新版本应用...'");
                writer.println("cd \"" + appDir.toString() + "\"");
                writer.println("nohup java -jar \"" + jarPath + "\" > app.log 2>&1 &");
                writer.println("echo '应用已启动'");
            }

            // 给脚本添加执行权限
            try {
                Runtime.getRuntime().exec("chmod +x " + shScriptPath.toString());
            } catch (Exception e) {
                logger.warn("设置脚本执行权限失败", e);
            }
            logger.info("Linux重启脚本已创建: {}", shScriptPath.toString());
        }
    }

    /**
     * 执行重启
     */
    private void executeRestart() throws Exception {
        String jarPath = getCurrentJarPath();
        Path appDir = Paths.get(jarPath).getParent();
        String os = System.getProperty("os.name").toLowerCase();

        // 启动重启脚本并退出当前应用
        ProcessBuilder processBuilder = new ProcessBuilder();

        if (os.contains("win")) {
            Path batScriptPath = appDir.resolve("restart.bat");
            // 在Windows上使用cmd执行批处理文件
            processBuilder.command("cmd", "/c", "start", "/b", batScriptPath.toString());
        } else {
            Path shScriptPath = appDir.resolve("restart.sh");
            // 在Linux上直接执行shell脚本
            processBuilder.command("sh", shScriptPath.toString());
        }

        processBuilder.start();

        logger.info("重启脚本已启动，当前应用将退出");
        // 延迟退出，确保脚本启动
        Thread.sleep(2000);
        System.exit(0);
    }

    /**
     * 获取远程应用信息
     */
    private JSONObject getRemoteAppInfo() throws Exception {
        String repositoryUrl = appConfiguration.getRepositoryUrl();
        if (repositoryUrl == null || repositoryUrl.isEmpty()) {
            throw new Exception("未配置仓库地址");
        }

        if (!repositoryUrl.endsWith("/")) {
            repositoryUrl += "/";
        }

        String appInfoUrl = repositoryUrl + "app.json";
        logger.info("获取远程应用信息: {}", appInfoUrl);

        String appInfoContent = downloadText(appInfoUrl);
        return com.alibaba.fastjson.JSON.parseObject(appInfoContent);
    }

    /**
     * 从URL下载文本内容
     */
    private String downloadText(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            return content.toString();
        }
    }

    /**
     * 计算文件的SHA512哈希值
     */
    private String calculateSHA512(Path filePath) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
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
    }

    /**
     * 比较两个版本号
     */
    private int compareVersions(String version1, String version2) {
        if (version1 == null || version2 == null) {
            return 0;
        }

        String[] parts1 = version1.split("[.-]");
        String[] parts2 = version2.split("[.-]");

        int maxLength = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLength; i++) {
            int v1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int v2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

            if (v1 > v2) {
                return 1;
            } else if (v1 < v2) {
                return -1;
            }
        }

        return 0;
    }

    /**
     * 解析版本号部分
     */
    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 获取升级状态
     */
    public ResultMsg<JSONObject> getUpgradeStatus() {
        JSONObject status = new JSONObject();
        status.put("isUpgrading", isUpgrading.get());
        status.put("currentVersion", getCurrentAppVersion());
        status.put("lastCheckTime", System.currentTimeMillis());
        return ResultMsg.success(status);
    }

    /**
     * 获取备份列表
     */
    public ResultMsg<JSONObject> getBackupList() {
        try {
            String jarPath = getCurrentJarPath();
            Path backupDir = Paths.get(jarPath).getParent().resolve("backups");

            JSONObject result = new JSONObject();
            if (Files.exists(backupDir) && Files.isDirectory(backupDir)) {
                java.util.List<String> backups = new java.util.ArrayList<>();
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupDir, "*.jar")) {
                    for (Path backupFile : stream) {
                        backups.add(backupFile.getFileName().toString());
                    }
                }
                result.put("backups", backups);
            } else {
                result.put("backups", new java.util.ArrayList<>());
            }

            result.put("backupDir", backupDir.toString());
            return ResultMsg.success(result);
        } catch (Exception e) {
            return ResultMsg.fail("获取备份列表失败: " + e.getMessage());
        }
    }
}
