package com.jt.plugins.utils;

import com.jt.plugins.common.log.PluginLogger;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.utils
 * @Author: 别来无恙qb
 * @CreateTime: 2026-02-26  15:19
 * @Description: TODO
 * @Version: 1.0
 */

/**
 * 去重功能编排器
 * 负责协调各个去重组件完成智能去重任务
 */
public class DeduplicationOrchestrator {

    private static final PluginLogger logger = PluginLogger.getLogger("log-clean-plugin");

    private final SemanticAnalyzer semanticAnalyzer;
    private final TimeBasedDeduplicator timeBasedDeduplicator;
    private final MultiLineSegmentProcessor segmentProcessor;
    private final FileProcessor fileProcessor;
    // 添加缺失的成员变量
    private final WebSystemLogDeduplicator webSystemDeduplicator;
    private final SegmentLogDeduplicator segmentDeduplicator;

    public DeduplicationOrchestrator() {
        this.semanticAnalyzer = new SemanticAnalyzer();
        this.timeBasedDeduplicator = new TimeBasedDeduplicator();
        this.segmentProcessor = new MultiLineSegmentProcessor();
        this.fileProcessor = new FileProcessor();
        // 初始化新增的成员变量
        this.webSystemDeduplicator = new WebSystemLogDeduplicator();
        this.segmentDeduplicator = new SegmentLogDeduplicator();
    }

    /**
     * 执行智能去重
     * @param filePaths 输入文件路径列表
     * @param outputDir 输出目录
     * @param timeWindowSeconds 时间窗口（秒），-1表示全局去重
     * @param separateFiles 是否分别处理文件
     * @return 处理结果列表
     */
    public List<FileProcessResult> executeDeduplication(
            List<String> filePaths, 
            String outputDir, 
            int timeWindowSeconds, 
            boolean separateFiles) throws IOException {
    
        logger.info("开始执行智能去重，文件数: {}, 时间窗口: {}, 分别处理: {}", 
                   filePaths.size(), 
                   timeWindowSeconds == -1 ? "全局去重" : timeWindowSeconds + "秒", 
                   separateFiles);
    
        List<FileProcessResult> results = new ArrayList<>();
    
        if (separateFiles) {
            // 分别处理每个文件
            for (String filePath : filePaths) {
                FileProcessResult result = processSingleFile(filePath, outputDir, timeWindowSeconds);
                results.add(result);
            }
        } else {
            // 合并处理所有文件
            FileProcessResult result = processMergedFiles(filePaths, outputDir, timeWindowSeconds);
            results.add(result);
        }
    
        logger.info("去重处理完成，处理结果数: {}", results.size());
        return results;
    }

    /**
     * 处理单个文件
     */
    private FileProcessResult processSingleFile(String filePath, String outputDir, int timeWindowSeconds) throws IOException {
        logger.info("处理单个文件: {}, 时间窗口: {}", filePath, 
                   timeWindowSeconds == -1 ? "全局去重" : timeWindowSeconds + "秒");
    
        // 读取文件内容
        List<String> lines = fileProcessor.readFileLines(filePath);
        int originalCount = lines.size();
    
        // 智能去重处理
        List<String> deduplicatedLines = performIntelligentDeduplication(lines, timeWindowSeconds);
    
        // 生成输出文件路径
        String outputFilePath = fileProcessor.generateOutputPath(filePath, outputDir);
    
        // 写入结果文件
        fileProcessor.writeFileLines(outputFilePath, deduplicatedLines);
    
        return new FileProcessResult(
            filePath,
            outputFilePath,
            originalCount,
            deduplicatedLines.size(),
            originalCount - deduplicatedLines.size(),
            System.currentTimeMillis()
        );
    }

    /**
     * 合并处理多个文件
     */
    private FileProcessResult processMergedFiles(List<String> filePaths, String outputDir, int timeWindowSeconds) throws IOException {
        logger.info("合并处理 {} 个文件，时间窗口: {}", filePaths.size(), 
               timeWindowSeconds == -1 ? "全局去重" : timeWindowSeconds + "秒");
    
        // 合并所有文件内容
        List<String> mergedLines = new ArrayList<>();
        int totalOriginalCount = 0;
    
        for (String filePath : filePaths) {
            List<String> fileLines = fileProcessor.readFileLines(filePath);
            mergedLines.addAll(fileLines);
            totalOriginalCount += fileLines.size();
        }
    
        // 智能去重处理
        List<String> deduplicatedLines = performIntelligentDeduplication(mergedLines, timeWindowSeconds);
    
        // 生成合并输出文件路径
        String outputFilePath = outputDir + "/merged_deduplicated.log";
    
        // 写入结果文件
        fileProcessor.writeFileLines(outputFilePath, deduplicatedLines);
    
        return new FileProcessResult(
            "Merged Files",
            outputFilePath,
            totalOriginalCount,
            deduplicatedLines.size(),
            totalOriginalCount - deduplicatedLines.size(),
            System.currentTimeMillis()
        );
    }

    /**
     * 执行智能去重（核心逻辑）
     */
    private List<String> performIntelligentDeduplication(List<String> lines, int timeWindowSeconds) {
        // 检测日志格式类型
        MultiLineSegmentProcessor.LogFormatType formatType = segmentProcessor.detectLogFormat(lines);
        
        logger.info("检测到日志格式类型: {}, 时间窗口: {}", formatType, 
                   timeWindowSeconds == -1 ? "全局去重" : timeWindowSeconds + "秒");
        
        // 根据时间窗口参数选择处理方式
        if (timeWindowSeconds == -1) {
            // 全局去重模式
            return performGlobalDeduplication(lines, formatType);
        } else {
            // 时间窗口模式
            return performTimeWindowDeduplication(lines, formatType, timeWindowSeconds);
        }
    }

    /**
     * 全局去重（整个文件范围内去重）
     */
    private List<String> performGlobalDeduplication(List<String> lines, MultiLineSegmentProcessor.LogFormatType formatType) {
        logger.info("执行全局去重");
        
        switch (formatType) {
            case WEB_SYSTEM:
                return webSystemDeduplicator.deduplicateWebSystemLogsGlobal(lines);
            case SEGMENT_LOG:
                return segmentDeduplicator.deduplicateSegmentLogsGlobal(lines);
            case MULTI_LINE:
                return segmentProcessor.processGenericSegmentsGlobal(lines);
            default:
                return semanticAnalyzer.performGlobalSemanticDeduplication(lines);
        }
    }

    /**
     * 时间窗口去重
     */
    private List<String> performTimeWindowDeduplication(List<String> lines, 
                                                      MultiLineSegmentProcessor.LogFormatType formatType, 
                                                      int timeWindowSeconds) {
        logger.info("执行时间窗口去重，窗口: {}秒", timeWindowSeconds);
        
        switch (formatType) {
            case WEB_SYSTEM:
                return webSystemDeduplicator.deduplicateWebSystemLogs(lines, timeWindowSeconds);
            case SEGMENT_LOG:
                return segmentDeduplicator.deduplicateSegmentLogs(lines, timeWindowSeconds);
            case MULTI_LINE:
                return segmentProcessor.processSegments(lines, timeWindowSeconds);
            default:
                return semanticAnalyzer.performSemanticDeduplication(lines, timeWindowSeconds);
        }
    }
}
