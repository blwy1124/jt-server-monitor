package com.jt.plugins.utils.clean;

import com.jt.plugins.common.log.PluginLogger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.utils.clean
 * @Author: 别来无恙qb
 * @CreateTime: 2026-02-28  14:00
 * @Description: 文件夹清理工具类 - 提供高效的文件夹清理功能
 * @Version: 1.0
 */
public class FolderCleaner {

    private static final PluginLogger logger = PluginLogger.getLogger("log-clean-plugin");

    /**
     * 清理结果统计类
     */
    public static class CleanResult {
        private final long deletedFiles;
        private final long deletedFolders;
        private final long totalSize;
        private final long processingTime;

        public CleanResult(long deletedFiles, long deletedFolders, long totalSize, long processingTime) {
            this.deletedFiles = deletedFiles;
            this.deletedFolders = deletedFolders;
            this.totalSize = totalSize;
            this.processingTime = processingTime;
        }

        // Getters
        public long getDeletedFiles() { return deletedFiles; }
        public long getDeletedFolders() { return deletedFolders; }
        public long getTotalSize() { return totalSize; }
        public long getProcessingTime() { return processingTime; }
    }

    /**
     * 清理指定时间点之前创建或修改的文件
     * @param folderPath 文件夹路径
     * @param cutoffTime 截止时间戳（毫秒）- 此时间点之前的文件会被删除
     * @param recursive 是否递归清理子文件夹
     * @param checkModifiedTime 是否检查修改时间（true:检查修改时间，false:检查创建时间）
     * @return 清理结果
     * @throws IOException IO异常
     */
    public CleanResult cleanFolderBeforeTime(String folderPath, long cutoffTime, boolean recursive, boolean checkModifiedTime) throws IOException {
        String timeType = checkModifiedTime ? "修改时间" : "创建时间";
        logger.info("开始清理文件夹: {}, 截止{}: {}, 递归: {}",
                folderPath, timeType, cutoffTime, recursive);

        long startTimeMs = System.currentTimeMillis();
        AtomicLong deletedFiles = new AtomicLong(0);
        AtomicLong deletedFolders = new AtomicLong(0);
        AtomicLong totalSize = new AtomicLong(0);

        File folder = new File(folderPath);
        if (!folder.exists()) {
            throw new IOException("文件夹不存在: " + folderPath);
        }

        if (!folder.isDirectory()) {
            throw new IOException("路径不是文件夹: " + folderPath);
        }

        try {
            cleanDirectoryBeforeTime(folder.toPath(), cutoffTime, recursive, checkModifiedTime,
                    deletedFiles, deletedFolders, totalSize);
        } catch (Exception e) {
            logger.error("清理文件夹过程中发生异常: {}", folderPath, e);
            throw new IOException("清理文件夹失败: " + e.getMessage(), e);
        }

        long processingTime = System.currentTimeMillis() - startTimeMs;

        CleanResult result = new CleanResult(
                deletedFiles.get(),
                deletedFolders.get(),
                totalSize.get(),
                processingTime
        );

        logger.info("文件夹清理完成: {}, 删除文件: {}, 删除文件夹: {}, 总大小: {} bytes, 耗时: {} ms",
                folderPath, result.getDeletedFiles(), result.getDeletedFolders(),
                result.getTotalSize(), result.getProcessingTime());

        return result;
    }

    /**
     * 递归清理目录中截止时间之前的文件
     */
    private void cleanDirectoryBeforeTime(Path dirPath, long cutoffTime, boolean recursive, boolean checkModifiedTime,
                                          AtomicLong deletedFiles, AtomicLong deletedFolders, AtomicLong totalSize) throws IOException {
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            return;
        }

        // 先处理文件（如果是递归模式）
        if (recursive) {
            Files.walk(dirPath)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            if (shouldDeleteFileBeforeTime(file, cutoffTime, checkModifiedTime)) {
                                long fileSize = getFileSize(file);
                                Files.delete(file);
                                deletedFiles.incrementAndGet();
                                totalSize.addAndGet(fileSize);
                                logger.debug("删除文件: {}, 大小: {} bytes", file, fileSize);
                            }
                        } catch (IOException e) {
                            logger.warn("删除文件失败: {}", file, e);
                        }
                    });
        } else {
            // 只清理顶层文件
            Files.list(dirPath)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            if (shouldDeleteFileBeforeTime(file, cutoffTime, checkModifiedTime)) {
                                long fileSize = getFileSize(file);
                                Files.delete(file);
                                deletedFiles.incrementAndGet();
                                totalSize.addAndGet(fileSize);
                                logger.debug("删除顶层文件: {}, 大小: {} bytes", file, fileSize);
                            }
                        } catch (IOException e) {
                            logger.warn("删除顶层文件失败: {}", file, e);
                        }
                    });
        }

        // 处理空文件夹（递归模式下）
        if (recursive) {
            // 重新遍历目录，删除空文件夹
            Files.walk(dirPath)
                    .filter(Files::isDirectory)
                    .sorted((p1, p2) -> p2.compareTo(p1)) // 逆序排列，先处理子目录
                    .forEach(dir -> {
                        try {
                            if (isDirectoryEmpty(dir) && !dir.equals(dirPath)) {
                                Files.delete(dir);
                                deletedFolders.incrementAndGet();
                                logger.debug("删除空文件夹: {}", dir);
                            }
                        } catch (IOException e) {
                            logger.warn("删除文件夹失败: {}", dir, e);
                        }
                    });
        }
    }

    /**
     * 判断文件是否应该删除（基于截止时间）
     * @param filePath 文件路径
     * @param cutoffTime 截止时间戳
     * @param checkModifiedTime true检查修改时间，false检查创建时间
     * @return 是否应该删除
     */
    private boolean shouldDeleteFileBeforeTime(Path filePath, long cutoffTime, boolean checkModifiedTime) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            long fileTime = checkModifiedTime ? attrs.lastModifiedTime().toMillis() : attrs.creationTime().toMillis();

            // 检查文件时间是否在截止时间之前
            return fileTime < cutoffTime;
        } catch (IOException e) {
            logger.warn("获取文件属性失败: {}", filePath, e);
            return false;
        }
    }

    /**
     * 获取文件大小
     */
    private long getFileSize(Path filePath) {
        try {
            return Files.size(filePath);
        } catch (IOException e) {
            logger.warn("获取文件大小失败: {}", filePath, e);
            return 0;
        }
    }

    /**
     * 判断目录是否为空
     */
    private boolean isDirectoryEmpty(Path dirPath) {
        try (java.util.stream.Stream<Path> entries = Files.list(dirPath)) {
            return !entries.findFirst().isPresent();
        } catch (IOException e) {
            logger.warn("检查目录是否为空失败: {}", dirPath, e);
            return false;
        }
    }

    /**
     * 清理整个文件夹（删除所有内容）
     * @param folderPath 文件夹路径
     * @param recursive 是否递归删除
     * @return 清理结果
     * @throws IOException IO异常
     */
    public CleanResult cleanEntireFolder(String folderPath, boolean recursive) throws IOException {
        logger.info("开始清空整个文件夹: {}, 递归: {}", folderPath, recursive);

        long startTimeMs = System.currentTimeMillis();
        AtomicLong deletedFiles = new AtomicLong(0);
        AtomicLong deletedFolders = new AtomicLong(0);
        AtomicLong totalSize = new AtomicLong(0);

        File folder = new File(folderPath);
        if (!folder.exists()) {
            throw new IOException("文件夹不存在: " + folderPath);
        }

        if (!folder.isDirectory()) {
            throw new IOException("路径不是文件夹: " + folderPath);
        }

        try {
            if (recursive) {
                // 递归删除所有内容
                Files.walk(folder.toPath())
                        .sorted((p1, p2) -> p2.compareTo(p1)) // 逆序排列
                        .forEach(path -> {
                            try {
                                if (Files.isRegularFile(path)) {
                                    long fileSize = getFileSize(path);
                                    Files.delete(path);
                                    deletedFiles.incrementAndGet();
                                    totalSize.addAndGet(fileSize);
                                } else if (Files.isDirectory(path) && !path.equals(folder.toPath())) {
                                    Files.delete(path);
                                    deletedFolders.incrementAndGet();
                                }
                            } catch (IOException e) {
                                logger.warn("删除路径失败: {}", path, e);
                            }
                        });
            } else {
                // 只删除顶层文件和空文件夹
                Files.list(folder.toPath())
                        .forEach(path -> {
                            try {
                                if (Files.isRegularFile(path)) {
                                    long fileSize = getFileSize(path);
                                    Files.delete(path);
                                    deletedFiles.incrementAndGet();
                                    totalSize.addAndGet(fileSize);
                                } else if (Files.isDirectory(path)) {
                                    if (isDirectoryEmpty(path)) {
                                        Files.delete(path);
                                        deletedFolders.incrementAndGet();
                                    }
                                }
                            } catch (IOException e) {
                                logger.warn("删除路径失败: {}", path, e);
                            }
                        });
            }
        } catch (Exception e) {
            logger.error("清空文件夹过程中发生异常: {}", folderPath, e);
            throw new IOException("清空文件夹失败: " + e.getMessage(), e);
        }

        long processingTime = System.currentTimeMillis() - startTimeMs;

        CleanResult result = new CleanResult(
                deletedFiles.get(),
                deletedFolders.get(),
                totalSize.get(),
                processingTime
        );

        logger.info("文件夹清空完成: {}, 删除文件: {}, 删除文件夹: {}, 总大小: {} bytes, 耗时: {} ms",
                folderPath, result.getDeletedFiles(), result.getDeletedFolders(),
                result.getTotalSize(), result.getProcessingTime());

        return result;
    }
}
