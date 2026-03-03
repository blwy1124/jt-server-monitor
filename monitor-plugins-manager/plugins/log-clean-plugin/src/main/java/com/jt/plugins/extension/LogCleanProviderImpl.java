package com.jt.plugins.extension;

import com.alibaba.fastjson.JSONObject;
import com.jt.plugins.utils.CacheManager;
import com.jt.plugins.utils.DeduplicationOrchestrator;
import com.jt.plugins.common.annotation.ActionHandler;
import com.jt.plugins.common.file.PluginFileStorage;
import com.jt.plugins.common.http.ExtensionRequestParam;
import com.jt.plugins.common.log.PluginLogger;
import com.jt.plugins.common.result.ResultMsg;
import com.jt.plugins.utils.FileProcessResult;
import com.jt.plugins.utils.FileProcessor;
import com.jt.plugins.utils.clean.FolderCleaner;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugin.extension
 * @Author: 别来无恙qb
 * @CreateTime: 2026-02-25  14:03
 * @Description: TODO
 * @Version: 1.0
 */
public class LogCleanProviderImpl implements LogCleanProvider {
    // 插件日志记录器
    private static final PluginLogger logger = PluginLogger.getLogger("log-clean-plugin");

    // 插件文件存储管理器
    private static final PluginFileStorage fileStorage = PluginFileStorage.getStorage("log-clean-plugin");

    // 缓存方法映射，避免每次反射查找
    private static final Map<String, Method> ACTION_HANDLERS = new HashMap<>();

    // 在类加载时初始化方法映射
    static {
        Method[] methods = LogCleanProviderImpl.class.getDeclaredMethods();
        for (Method method : methods) {
            ActionHandler annotation = method.getAnnotation(ActionHandler.class);
            if (annotation != null) {
                ACTION_HANDLERS.put(annotation.value(), method);
            }
        }
    }

    @Override
    public ResultMsg<JSONObject> execute(ExtensionRequestParam extensionRequestParam) {
        String targetAction = extensionRequestParam.getTargetAction();
        logger.info("开始执行日志监控操作: {}", targetAction);

        // 参数校验
        if (targetAction == null || targetAction.isEmpty()) {
            logger.warn("操作类型不能为空");
            return ResultMsg.fail("操作类型不能为空");
        }

        // 根据targetAction查找对应的处理方法
        Method handlerMethod = ACTION_HANDLERS.get(targetAction);

        if (handlerMethod != null) {
            try {
                // 调用对应的方法处理请求
                logger.info("调用处理方法: {}", handlerMethod.getName());
                ResultMsg<JSONObject> result = (ResultMsg<JSONObject>) handlerMethod.invoke(this, extensionRequestParam);
                logger.info("日志清理操作执行完成: {}", targetAction);
                return result;
            } catch (Exception e) {
                logger.error("执行日志清理操作失败: {}", targetAction, e);
                // 区分不同类型的异常并抛出相应的错误信息
                Throwable cause = e.getCause();
                if (cause != null) {
                    if (cause instanceof SecurityException) {
                        return ResultMsg.fail("权限不足，无法访问目标JVM进程：" + cause.getMessage());
                    } else if (cause instanceof IllegalArgumentException) {
                        return ResultMsg.fail("参数错误：" + cause.getMessage());
                    } else if (cause instanceof IllegalStateException) {
                        return ResultMsg.fail("状态错误：" + cause.getMessage());
                    } else {
                        return ResultMsg.fail("执行操作失败：" + cause.getMessage());
                    }
                }
                return ResultMsg.fail("执行操作失败：" + e.getMessage());
            }
        }

        logger.warn("不支持的操作类型: {}", targetAction);
        return ResultMsg.fail("不支持的操作类型：" + targetAction);
    }


    @ActionHandler("ping")
    private ResultMsg<JSONObject> handlePing(ExtensionRequestParam request) {
        logger.debug("处理ping请求");
        try {
            return ResultMsg.success(new JSONObject(), "Pong");
        } catch (Exception e) {
            logger.error("处理ping请求失败", e);
            return ResultMsg.fail("处理ping请求失败：" + e.getMessage());
        }
    }

    /**
     * 处理日志内容去重清洗请求
     * @param request
     * // dedupStrategy可选: basic, advanced, semantic
     * @return
     * 日志格式：**********************************【WEB系统】**********************************
     * 【日志开始：2026-01-14 17:28:07】  在函数
     *    javax.naming.NameNotFoundException: ms_message -- service jboss.naming.context.java.ms_message
     * 【日志结束：2026-01-14 17:28:07】
     * **********************************【WEB系统】**********************************
     * 【日志开始：2026-01-14 17:28:07】  在函数
     *    javax.naming.NameNotFoundException: ms_message -- service jboss.naming.context.java.ms_message
     * 【日志结束：2026-01-14 17:28:07】
     * **********************************【WEB系统】**********************************
     * 【日志开始：2026-01-14 17:30:13】  在函数
     *    javax.naming.NameNotFoundException: ms_message -- service jboss.naming.context.java.ms_message
     * 【日志结束：2026-01-14 17:30:13】
     * **********************************【WEB系统】**********************************
     * 【日志开始：2026-01-14 17:30:13】  在函数
     *    javax.naming.NameNotFoundException: ms_message -- service jboss.naming.context.java.ms_message
     * 【日志结束：2026-01-14 17:30:13】
     */
    @ActionHandler("contentWash")
    public ResultMsg<JSONObject> handleContentWash(ExtensionRequestParam request) {
        logger.debug("处理日志内容清洗请求");
        
        try {
            // 获取请求参数
            String sourceFilePaths = request.getParameter("sourceFilePaths");
            String outputDir = request.getParameter("outputDir");
            String timeWindowParam = request.getParameter("timeWindowSeconds");
            int timeWindowSeconds = timeWindowParam != null ? Integer.parseInt(timeWindowParam) : -1;
            boolean separateFiles = Boolean.parseBoolean(request.getParameter("separateFiles", "true"));
            boolean returnCompressedFile = Boolean.parseBoolean(request.getParameter("returnCompressedFile", "false"));
            // 新增参数：文件访问令牌
            boolean returnDownloadToken = Boolean.parseBoolean(request.getParameter("returnDownloadToken", "false"));
            
            // 参数校验
            if (sourceFilePaths == null || sourceFilePaths.isEmpty()) {
                return ResultMsg.fail("源文件路径不能为空");
            }
            
            // 解析文件路径列表
            List<String> filePaths = Arrays.asList(sourceFilePaths.split(","));
            filePaths.replaceAll(String::trim);
            
            // 验证文件是否存在
            for (String filePath : filePaths) {
                if (!new File(filePath).exists()) {
                    return ResultMsg.fail("文件不存在: " + filePath);
                }
            }
            
            // 设置默认输出目录
            if (outputDir == null || outputDir.isEmpty()) {
                outputDir = fileStorage.getPluginStoragePath() + "/deduplicated_logs";
            }
            
            // 执行智能去重
            DeduplicationOrchestrator orchestrator = new DeduplicationOrchestrator();
            List<FileProcessResult> results = orchestrator.executeDeduplication(
                filePaths, outputDir, timeWindowSeconds, separateFiles);
            
            JSONObject resultData = new JSONObject();
            resultData.put("outputDir", outputDir);
            resultData.put("fileResults", results);
            resultData.put("totalFiles", filePaths.size());
            resultData.put("timeWindowSeconds", timeWindowSeconds);
            resultData.put("separateFiles", separateFiles);
            resultData.put("returnCompressedFile", returnCompressedFile);
            resultData.put("returnDownloadToken", returnDownloadToken);
            
            String successMessage;
            
            // 如果需要返回下载令牌而不是文件内容
            if (returnDownloadToken) {
                String zipFileName = "deduplicated_logs_" + System.currentTimeMillis() + ".zip";
                String zipFilePath = fileStorage.getPluginStoragePath() + "/" + zipFileName;
                
                try {
                    // 压缩整个输出目录
                    String compressedFile = FileProcessor.compressDirectory(outputDir, zipFilePath);
                    File zipFile = new File(compressedFile);
                    
                    // 生成下载令牌
                    String downloadToken = generateDownloadToken(zipFilePath);
                    
                    // 注册下载令牌（可以存储在缓存中，设置过期时间）
                    registerDownloadToken(downloadToken, zipFilePath, 3600); // 1小时过期
                    
                    resultData.put("downloadToken", downloadToken);
                    resultData.put("downloadUrl", "/api/download/" + downloadToken);
                    resultData.put("compressedFileName", zipFileName);
                    resultData.put("compressedFileSize", zipFile.length());
                    resultData.put("contentType", "application/zip");
                    resultData.put("expiresIn", 3600); // 过期时间（秒）
                    
                    successMessage = String.format("日志去重完成，可通过令牌下载压缩包，处理了%d个文件，压缩包大小: %d bytes", 
                        filePaths.size(), zipFile.length());
                    logger.info("日志内容清洗完成: {}", successMessage);
                    
                } catch (Exception e) {
                    logger.error("生成下载令牌失败", e);
                    successMessage = String.format("日志去重完成但生成下载链接失败: %s", e.getMessage());
                }
            }
            // 如果需要直接返回文件内容（小文件）
            else if (returnCompressedFile) {
                String zipFileName = "deduplicated_logs_" + System.currentTimeMillis() + ".zip";
                String zipFilePath = fileStorage.getPluginStoragePath() + "/" + zipFileName;
                
                try {
                    String compressedFile = FileProcessor.compressDirectory(outputDir, zipFilePath);
                    File zipFile = new File(compressedFile);
                    
                    // 检查文件大小，如果超过阈值则建议使用令牌方式
                    long fileSize = zipFile.length();
                    long maxSize = 50 * 1024 * 1024; // 50MB阈值
                    
                    if (fileSize > maxSize) {
                        // 文件太大，建议使用令牌下载
                        String downloadToken = generateDownloadToken(zipFilePath);
                        registerDownloadToken(downloadToken, zipFilePath, 3600);
                        
                        resultData.put("warning", "文件较大(" + (fileSize/1024/1024) + "MB)，建议使用下载令牌方式");
                        resultData.put("downloadToken", downloadToken);
                        resultData.put("downloadUrl", "/api/download/" + downloadToken);
                        resultData.put("compressedFileName", zipFileName);
                        resultData.put("compressedFileSize", fileSize);
                        resultData.put("contentType", "application/zip");
                        
                        successMessage = String.format("日志去重完成，文件较大建议使用令牌下载，压缩包大小: %d bytes", fileSize);
                    } else {
                        // 小文件，直接返回内容
                        byte[] fileBytes = Files.readAllBytes(zipFile.toPath());
                        String base64Content = java.util.Base64.getEncoder().encodeToString(fileBytes);
                        
                        resultData.put("compressedFileContent", base64Content);
                        resultData.put("compressedFileName", zipFileName);
                        resultData.put("compressedFileSize", fileSize);
                        resultData.put("contentType", "application/zip");
                        
                        successMessage = String.format("日志去重完成并返回压缩包，压缩包大小: %d bytes", fileSize);
                    }
                    
                    logger.info("日志内容清洗完成: {}", successMessage);
                    
                } catch (IOException e) {
                    logger.error("处理压缩包失败", e);
                    successMessage = String.format("日志去重完成但处理压缩包失败: %s", e.getMessage());
                }
            } else {
                successMessage = String.format("日志去重完成，处理了%d个文件，输出目录: %s", 
                    filePaths.size(), outputDir);
                logger.info("日志内容清洗完成: {}", successMessage);
            }
            
            return ResultMsg.success(resultData, successMessage);
            
        } catch (Exception e) {
            logger.error("处理日志内容清洗失败", e);
            return ResultMsg.fail("日志内容清洗失败：" + e.getMessage());
        }
    }

    /**
     * 注册下载令牌
     */
    private void registerDownloadToken(String token, String filePath, int expireSeconds) {
        // 使用完善的缓存管理器存储下载令牌
        CacheManager.DownloadTokenCache.storeToken(token, filePath, expireSeconds);
    }
    
    /**
     * 生成下载令牌
     */
    private String generateDownloadToken(String filePath) {
        // 生成唯一令牌
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf(Math.random());
        String token = java.util.Base64.getUrlEncoder().encodeToString(
            (filePath + timestamp + random).getBytes());
        return token.replaceAll("[^a-zA-Z0-9]", ""); // 移除特殊字符
    }

    /**
     * 处理文件夹清理请求
     * @param request
     * @return
     */
    @ActionHandler("cleanFolder")
    public ResultMsg<JSONObject> handleCleanFolder(ExtensionRequestParam request) {
        logger.debug("处理文件夹清理请求");
        
        try {
            // 获取请求参数
            String folderPath = request.getParameter("folderPath");
            String cutoffTimeStr = request.getParameter("cutoffTime"); // 截止时间戳
            String daysAgoStr = request.getParameter("daysAgo"); // 多少天以前 daysAgo比cutoffTime优先级更高
            String cleanMode = request.getParameter("cleanMode", "beforeTime"); // beforeTime 或 entire
            boolean recursive = Boolean.parseBoolean(request.getParameter("recursive", "true")); // 是否递归清理子文件夹
            boolean checkModifiedTime = Boolean.parseBoolean(request.getParameter("checkModifiedTime", "true")); // true:检查修改时间，false:检查创建时间
            boolean returnDetails = Boolean.parseBoolean(request.getParameter("returnDetails", "true"));
            
            // 参数校验
            if (folderPath == null || folderPath.isEmpty()) {
                return ResultMsg.fail("文件夹路径不能为空");
            }
            
            // 验证文件夹是否存在
            File folder = new File(folderPath);
            if (!folder.exists()) {
                return ResultMsg.fail("文件夹不存在: " + folderPath);
            }
            
            if (!folder.isDirectory()) {
                return ResultMsg.fail("路径不是文件夹: " + folderPath);
            }
            
            // 执行清理操作
            FolderCleaner folderCleaner = new FolderCleaner();
            FolderCleaner.CleanResult cleanResult;
            
            if ("beforeTime".equals(cleanMode)) {
                // 清理截止时间之前的文件
                long cutoffTime;
                
                // 支持两种时间格式：时间戳或天数
                if (daysAgoStr != null && !daysAgoStr.isEmpty()) {
                    // 按天数计算截止时间
                    try {
                        int daysAgo = Integer.parseInt(daysAgoStr);
                        if (daysAgo <= 0) {
                            return ResultMsg.fail("daysAgo参数必须大于0");
                        }
                        cutoffTime = System.currentTimeMillis() - (daysAgo * 24L * 60 * 60 * 1000);
                        logger.info("使用天数模式：删除{}天以前的文件", daysAgo);
                    } catch (NumberFormatException e) {
                        return ResultMsg.fail("daysAgo参数格式错误，请提供有效的数字");
                    }
                } else if (cutoffTimeStr != null && !cutoffTimeStr.isEmpty()) {
                    // 使用时间戳模式
                    cutoffTime = Long.parseLong(cutoffTimeStr);
                    // 验证截止时间合理性（不能是未来时间）
                    if (cutoffTime > System.currentTimeMillis() + 60000) { // 允许1分钟的误差
                        return ResultMsg.fail("截止时间不能超过当前时间");
                    }
                    logger.info("使用时间戳模式：删除截止时间{}之前的文件", cutoffTime);
                } else {
                    return ResultMsg.fail("按时间清理时，必须提供cutoffTime或daysAgo参数");
                }
                
                String timeType = checkModifiedTime ? "修改时间" : "创建时间";
                logger.info("清理{}在{}之前创建/修改的文件: {}, 递归: {}", 
                           timeType, cutoffTime, folderPath, recursive);
                
                cleanResult = folderCleaner.cleanFolderBeforeTime(folderPath, cutoffTime, recursive, checkModifiedTime);
                
            } else if ("entire".equals(cleanMode)) {
                // 清空整个文件夹
                logger.info("清空整个文件夹: {}, 递归: {}", folderPath, recursive);
                cleanResult = folderCleaner.cleanEntireFolder(folderPath, recursive);
                
            } else {
                return ResultMsg.fail("不支持的清理模式: " + cleanMode + "，支持的模式: beforeTime, entire");
            }
            
            // 构建返回结果
            JSONObject resultData = new JSONObject();
            resultData.put("folderPath", folderPath);
            resultData.put("cleanMode", cleanMode);
            resultData.put("recursive", recursive);
            if ("beforeTime".equals(cleanMode)) {
                if (daysAgoStr != null && !daysAgoStr.isEmpty()) {
                    resultData.put("daysAgo", daysAgoStr);
                    resultData.put("calculatedCutoffTime", System.currentTimeMillis() - (Integer.parseInt(daysAgoStr) * 24L * 60 * 60 * 1000));
                } else {
                    resultData.put("cutoffTime", cutoffTimeStr);
                }
                resultData.put("checkModifiedTime", checkModifiedTime);
                resultData.put("timeType", checkModifiedTime ? "修改时间" : "创建时间");
            }
            resultData.put("deletedFiles", cleanResult.getDeletedFiles());
            resultData.put("deletedFolders", cleanResult.getDeletedFolders());
            resultData.put("totalSize", cleanResult.getTotalSize());
            resultData.put("processingTime", cleanResult.getProcessingTime());
            resultData.put("cleanTime", System.currentTimeMillis());
            
            // 如果需要返回详细信息
            if (returnDetails && cleanResult.getDeletedFiles() > 0) {
                String timeDesc = "";
                if ("beforeTime".equals(cleanMode)) {
                    String timeType = checkModifiedTime ? "修改时间" : "创建时间";
                    if (daysAgoStr != null && !daysAgoStr.isEmpty()) {
                        timeDesc = "（" + timeType + "在 " + daysAgoStr + " 天以前）";
                    } else {
                        timeDesc = "（" + timeType + "在 " + formatTimestamp(Long.parseLong(cutoffTimeStr)) + " 之前）";
                    }
                }
                resultData.put("details", "清理完成" + timeDesc + "，删除了 " + cleanResult.getDeletedFiles() + " 个文件和 " 
                              + cleanResult.getDeletedFolders() + " 个文件夹，总大小: " 
                              + formatFileSize(cleanResult.getTotalSize()));
            }
            
            String successMessage = String.format("文件夹清理完成，删除了%d个文件和%d个文件夹，总大小: %s，耗时: %d ms",
                cleanResult.getDeletedFiles(), cleanResult.getDeletedFolders(),
                formatFileSize(cleanResult.getTotalSize()), cleanResult.getProcessingTime());
            
            logger.info("文件夹清理操作完成: {}", successMessage);
            
            return ResultMsg.success(resultData, successMessage);
            
        } catch (NumberFormatException e) {
            logger.error("时间参数格式错误", e);
            return ResultMsg.fail("时间参数格式错误，请提供有效的时间戳或天数");
        } catch (Exception e) {
            logger.error("处理文件夹清理失败", e);
            return ResultMsg.fail("文件夹清理失败：" + e.getMessage());
        }
    }
    
    /**
     * 格式化文件大小显示
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 格式化时间戳显示
     */
    private String formatTimestamp(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        ZoneId zoneId = ZoneId.systemDefault();
       DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return ZonedDateTime.ofInstant(instant, zoneId).format(formatter);
    }
}
