package com.jt.plugins.extension;

import com.alibaba.fastjson.JSONObject;
import com.jt.plugins.utils.DeduplicationOrchestrator;
import com.jt.plugins.common.annotation.ActionHandler;
import com.jt.plugins.common.file.PluginFileStorage;
import com.jt.plugins.common.http.ExtensionRequestParam;
import com.jt.plugins.common.log.PluginLogger;
import com.jt.plugins.common.result.ResultMsg;
import com.jt.plugins.utils.FileProcessResult;
import com.jt.plugins.utils.FileProcessor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
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
     * 注册下载令牌
     */
    private void registerDownloadToken(String token, String filePath, int expireSeconds) {
        // 这里可以使用Redis、内存缓存或其他存储方式
        // 示例使用简单的内存Map（生产环境建议使用Redis）
        java.util.Map<String, Object> tokenInfo = new java.util.HashMap<>();
        tokenInfo.put("filePath", filePath);
        tokenInfo.put("createTime", System.currentTimeMillis());
        tokenInfo.put("expireTime", System.currentTimeMillis() + (expireSeconds * 1000L));
        
        // 存储到缓存中（需要实现缓存管理器）
        // tokenCache.put(token, tokenInfo, expireSeconds);
    }
}
