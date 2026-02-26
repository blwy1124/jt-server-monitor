package com.jt.plugins.utils;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.utils
 * @Author: 别来无恙qb
 * @CreateTime: 2026-02-26  11:02
 * @Description: TODO
 * @Version: 1.0
 */

public class FileProcessResult {
    private String sourceFile;
    private String outputFile;
    private int originalLineCount;
    private int cleanedLineCount;
    private int removedLineCount;
    private long processingTime;

    public FileProcessResult(String sourceFile, String outputFile,
                             int originalLineCount, int cleanedLineCount,
                             int removedLineCount, long processingTime) {
        this.sourceFile = sourceFile;
        this.outputFile = outputFile;
        this.originalLineCount = originalLineCount;
        this.cleanedLineCount = cleanedLineCount;
        this.removedLineCount = removedLineCount;
        this.processingTime = processingTime;
    }

    // getter方法
    public String getSourceFile() { return sourceFile; }
    public String getOutputFile() { return outputFile; }
    public int getOriginalLineCount() { return originalLineCount; }
    public int getCleanedLineCount() { return cleanedLineCount; }
    public int getRemovedLineCount() { return removedLineCount; }
    public long getProcessingTime() { return processingTime; }
}
