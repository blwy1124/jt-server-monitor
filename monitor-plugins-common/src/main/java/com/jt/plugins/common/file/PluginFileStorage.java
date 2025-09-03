package com.jt.plugins.common.file;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.common.file
 * @Author: 别来无恙qb
 * @CreateTime: 2025-09-02  15:29
 * @Description: 插件文件存储管理工具类
 * 提供统一的文件存储路径管理，每个插件有独立的存储目录
 * @Version: 1.0
 */

public class PluginFileStorage {

    private static final ConcurrentHashMap<String, PluginFileStorage> storageCache = new ConcurrentHashMap<>();
    private static final String BASE_STORAGE_PATH = getBaseStoragePath();

    private final String pluginName;
    private final File pluginStorageDir;

    private PluginFileStorage(String pluginName) {
        this.pluginName = pluginName;
        this.pluginStorageDir = new File(BASE_STORAGE_PATH, pluginName);
        // 确保插件存储目录存在
        if (!pluginStorageDir.exists()) {
            pluginStorageDir.mkdirs();
        }
    }

    /**
     * 获取插件文件存储管理器
     * @param pluginName 插件名称
     * @return PluginFileStorage实例
     */
    public static PluginFileStorage getStorage(String pluginName) {
        return storageCache.computeIfAbsent(pluginName, PluginFileStorage::new);
    }

    /**
     * 获取基础存储路径
     * 优先级：当前目录
     */
    private static String getBaseStoragePath() {
        String storagePath = null;

        // 首先尝试使用系统临时目录
//        try {
//            String tempDir = System.getProperty("java.io.tmpdir");
//            if (tempDir != null && !tempDir.isEmpty()) {
//                storagePath = new File(tempDir, "jt-monitor-storage").getAbsolutePath();
//            }
//        } catch (Exception e) {
//            // 忽略异常，尝试其他位置
//        }
//
//        // 如果临时目录不可用，尝试用户主目录
//        if (storagePath == null) {
//            try {
//                String userHome = System.getProperty("user.home");
//                if (userHome != null && !userHome.isEmpty()) {
//                    storagePath = new File(userHome, "jt-monitor-storage").getAbsolutePath();
//                }
//            } catch (Exception e) {
//                // 忽略异常，使用当前目录
//            }
//        }

        // 如果以上都不可用，使用当前目录
        if (storagePath == null) {
            storagePath = System.getProperty("user.dir") + File.separator + "storage";
        }

        // 确保存储根目录存在
        File storageDir = new File(storagePath);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        return storagePath;
    }

    /**
     * 获取插件存储目录
     * @return 插件存储目录File对象
     */
    public File getPluginStorageDir() {
        return pluginStorageDir;
    }

    /**
     * 获取插件存储目录路径
     * @return 插件存储目录路径
     */
    public String getPluginStoragePath() {
        return pluginStorageDir.getAbsolutePath();
    }

    /**
     * 在插件存储目录下创建文件
     * @param fileName 文件名
     * @return 文件File对象
     */
    public File createFile(String fileName) {
        return new File(pluginStorageDir, fileName);
    }

    /**
     * 在插件存储目录下创建子目录
     * @param dirName 目录名
     * @return 目录File对象
     */
    public File createDirectory(String dirName) {
        File dir = new File(pluginStorageDir, dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * 获取插件存储目录下的文件
     * @param fileName 文件名
     * @return 文件File对象
     */
    public File getFile(String fileName) {
        return new File(pluginStorageDir, fileName);
    }

    /**
     * 检查文件是否存在
     * @param fileName 文件名
     * @return 文件是否存在
     */
    public boolean exists(String fileName) {
        return getFile(fileName).exists();
    }

    /**
     * 删除文件
     * @param fileName 文件名
     * @return 是否删除成功
     */
    public boolean deleteFile(String fileName) {
        return getFile(fileName).delete();
    }

    /**
     * 获取基础存储路径
     * @return 基础存储路径
     */
    public static String getBaseStoragePathStatic() {
        return BASE_STORAGE_PATH;
    }
}

