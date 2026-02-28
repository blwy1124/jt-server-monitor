package com.jt.plugins.utils;

import com.jt.plugins.common.log.PluginLogger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.utils
 * @Author: 别来无恙qb
 * @CreateTime: 2026-02-26  15:21
 * @Description: TODO
 * @Version: 1.0
 */
/**
 * 文件处理器
 * 负责文件的读写操作
 */
public class FileProcessor {

    private static final PluginLogger logger = PluginLogger.getLogger("log-clean-plugin");

    /**
     * 读取文件所有行
     */
    public List<String> readFileLines(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        logger.debug("读取文件完成: {}, 行数: {}", filePath, lines.size());
        return lines;
    }

    /**
     * 写入文件行
     */
    public void writeFileLines(String filePath, List<String> lines) throws IOException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
        logger.debug("写入文件完成: {}, 行数: {}", filePath, lines.size());
    }

    /**
     * 生成输出文件路径
     */
    public String generateOutputPath(String inputPath, String outputDir) {
        File inputFile = new File(inputPath);
        String fileName = inputFile.getName();
        String fileNameWithoutExt = fileName;
        String fileExt = "";

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            fileNameWithoutExt = fileName.substring(0, lastDotIndex);
            fileExt = fileName.substring(lastDotIndex);
        }

        return outputDir + "/" + fileNameWithoutExt + "_deduplicated" + fileExt;
    }

    /**
     * 确保目录存在
     */
    public void ensureDirectoryExists(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
            logger.debug("创建目录: {}", dirPath);
        }
    }

    /**
     * 压缩文件夹
     * @param sourceDir 源文件夹路径
     * @param zipFilePath 目标zip文件路径
     * @return 压缩文件的绝对路径
     */
    public static String compressDirectory(String sourceDir, String zipFilePath) throws IOException {
        logger.info("开始压缩文件夹: {} -> {}", sourceDir, zipFilePath);

        Path sourcePath = Paths.get(sourceDir);
        if (!Files.exists(sourcePath)) {
            throw new IOException("源文件夹不存在: " + sourceDir);
        }

        if (!Files.isDirectory(sourcePath)) {
            throw new IOException("源路径不是文件夹: " + sourceDir);
        }

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // 计算相对路径
                    Path relativePath = sourcePath.relativize(file);
                    String entryName = relativePath.toString().replace('\\', '/');

                    // 添加文件到zip
                    ZipEntry zipEntry = new ZipEntry(entryName);
                    zos.putNextEntry(zipEntry);

                    // 写入文件内容
                    try (InputStream fis = Files.newInputStream(file)) {
                        byte[] buffer = new byte[8192];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                    }

                    zos.closeEntry();
                    logger.debug("已添加文件到压缩包: {}", entryName);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    // 为目录创建zip条目
                    Path relativePath = sourcePath.relativize(dir);
                    if (relativePath.toString().length() > 0) {
                        String entryName = relativePath.toString().replace('\\', '/') + "/";
                        zos.putNextEntry(new ZipEntry(entryName));
                        zos.closeEntry();
                        logger.debug("已添加目录到压缩包: {}", entryName);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        File zipFile = new File(zipFilePath);
        long fileSize = zipFile.length();
        logger.info("文件夹压缩完成: {}, 压缩包大小: {} bytes", zipFilePath, fileSize);

        return zipFilePath;
    }

    /**
     * 压缩单个文件
     * @param sourceFile 源文件路径
     * @param zipFilePath 目标zip文件路径
     * @return 压缩文件的绝对路径
     */
    public static String compressSingleFile(String sourceFile, String zipFilePath) throws IOException {
        logger.info("开始压缩文件: {} -> {}", sourceFile, zipFilePath);

        Path sourcePath = Paths.get(sourceFile);
        if (!Files.exists(sourcePath)) {
            throw new IOException("源文件不存在: " + sourceFile);
        }

        String fileName = sourcePath.getFileName().toString();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);

            try (InputStream fis = Files.newInputStream(sourcePath)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
            }

            zos.closeEntry();
        }

        File zipFile = new File(zipFilePath);
        long fileSize = zipFile.length();
        logger.info("文件压缩完成: {}, 压缩包大小: {} bytes", zipFilePath, fileSize);

        return zipFilePath;
    }
}
