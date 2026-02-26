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
}
