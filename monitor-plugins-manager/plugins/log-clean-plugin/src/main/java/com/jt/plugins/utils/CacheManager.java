package com.jt.plugins.utils;

import com.jt.plugins.common.log.PluginLogger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.utils
 * @Author: 别来无恙qb
 * @CreateTime: 2026-02-28  15:30
 * @Description: 通用缓存管理器 - 支持过期时间和自动清理
 * @Version: 1.0
 */
public class CacheManager {
    
    private static final PluginLogger logger = PluginLogger.getLogger("log-clean-plugin");
    
    // 缓存存储
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    
    // 定时清理任务执行器
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // 默认清理间隔（分钟）
    private static final int DEFAULT_CLEANUP_INTERVAL = 5;
    
    /**
     * 缓存条目内部类
     */
    private static class CacheEntry {
        private final Object value;
        private final long createTime;
        private final long expireTime;
        
        public CacheEntry(Object value, long expireTime) {
            this.value = value;
            this.createTime = System.currentTimeMillis();
            this.expireTime = expireTime;
        }
        
        public Object getValue() { return value; }
        public long getCreateTime() { return createTime; }
        public long getExpireTime() { return expireTime; }
        public boolean isExpired() { return System.currentTimeMillis() > expireTime; }
    }
    
    /**
     * 单例实例
     */
    private static class SingletonHolder {
        private static final CacheManager INSTANCE = new CacheManager();
    }
    
    public static CacheManager getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    /**
     * 私有构造函数
     */
    private CacheManager() {
        // 启动定时清理任务
        startCleanupTask();
        logger.info("缓存管理器初始化完成，清理间隔: {}分钟", DEFAULT_CLEANUP_INTERVAL);
    }
    
    /**
     * 启动定时清理任务
     */
    private void startCleanupTask() {
        scheduler.scheduleAtFixedRate(this::cleanupExpiredEntries, 
                                    DEFAULT_CLEANUP_INTERVAL, 
                                    DEFAULT_CLEANUP_INTERVAL, 
                                    TimeUnit.MINUTES);
    }
    
    /**
     * 存储键值对到缓存
     * @param key 键
     * @param value 值
     * @param expireSeconds 过期时间（秒），<=0表示永不过期
     */
    public void put(String key, Object value, int expireSeconds) {
        if (key == null || value == null) {
            logger.warn("缓存键或值不能为空");
            return;
        }
        
        long expireTime = expireSeconds <= 0 ? Long.MAX_VALUE : 
                         System.currentTimeMillis() + (expireSeconds * 1000L);
        
        cache.put(key, new CacheEntry(value, expireTime));
        logger.debug("缓存存储成功: key={}, expireSeconds={}", key, expireSeconds);
    }
    
    /**
     * 获取缓存值
     * @param key 键
     * @return 缓存值，如果不存在或已过期则返回null
     */
    public Object get(String key) {
        if (key == null) {
            return null;
        }
        
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        
        if (entry.isExpired()) {
            cache.remove(key);
            logger.debug("缓存条目已过期并被移除: {}", key);
            return null;
        }
        
        return entry.getValue();
    }
    
    /**
     * 检查键是否存在且未过期
     * @param key 键
     * @return 是否存在
     */
    public boolean containsKey(String key) {
        return get(key) != null;
    }
    
    /**
     * 删除指定键的缓存
     * @param key 键
     * @return 是否删除成功
     */
    public boolean remove(String key) {
        if (key == null) {
            return false;
        }
        
        CacheEntry removed = cache.remove(key);
        boolean success = removed != null;
        if (success) {
            logger.debug("缓存条目删除成功: {}", key);
        }
        return success;
    }
    
    /**
     * 获取缓存大小
     * @return 当前缓存条目数量
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * 清理所有缓存
     */
    public void clear() {
        int size = cache.size();
        cache.clear();
        logger.info("缓存已清空，清理条目数: {}", size);
    }
    
    /**
     * 清理过期的缓存条目
     */
    public void cleanupExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;
        
        // 创建副本避免并发修改异常
        Map<String, CacheEntry> copy = new ConcurrentHashMap<>(cache);
        
        for (Map.Entry<String, CacheEntry> entry : copy.entrySet()) {
            if (entry.getValue().isExpired()) {
                cache.remove(entry.getKey());
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            logger.info("定时清理过期缓存条目: {}个", removedCount);
        }
    }
    
    /**
     * 获取缓存统计信息
     * @return 统计信息JSON对象
     */
    public com.alibaba.fastjson.JSONObject getStats() {
        com.alibaba.fastjson.JSONObject stats = new com.alibaba.fastjson.JSONObject();
        stats.put("totalEntries", cache.size());
        stats.put("cleanupIntervalMinutes", DEFAULT_CLEANUP_INTERVAL);
        
        // 统计过期条目
        long expiredCount = cache.values().stream()
            .filter(CacheEntry::isExpired)
            .count();
        stats.put("expiredEntries", expiredCount);
        
        return stats;
    }
    
    /**
     * 关闭缓存管理器
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        clear();
        logger.info("缓存管理器已关闭");
    }
    
    /**
     * 下载令牌专用缓存操作
     */
    public static class DownloadTokenCache {
        private static final String TOKEN_PREFIX = "download_token_";
        private static final CacheManager cacheManager = CacheManager.getInstance();
        
        /**
         * 存储下载令牌
         * @param token 令牌
         * @param filePath 文件路径
         * @param expireSeconds 过期时间（秒）
         */
        public static void storeToken(String token, String filePath, int expireSeconds) {
            if (token == null || filePath == null) {
                logger.warn("令牌或文件路径不能为空");
                return;
            }
            
            com.alibaba.fastjson.JSONObject tokenInfo = new com.alibaba.fastjson.JSONObject();
            tokenInfo.put("filePath", filePath);
            tokenInfo.put("createTime", System.currentTimeMillis());
            tokenInfo.put("expireTime", System.currentTimeMillis() + (expireSeconds * 1000L));
            
            String cacheKey = TOKEN_PREFIX + token;
            cacheManager.put(cacheKey, tokenInfo, expireSeconds);
            logger.info("下载令牌已存储: token={}, filePath={}, expireSeconds={}", 
                       token, filePath, expireSeconds);
        }
        
        /**
         * 获取下载令牌信息
         * @param token 令牌
         * @return 令牌信息，如果不存在或已过期则返回null
         */
        public static com.alibaba.fastjson.JSONObject getTokenInfo(String token) {
            if (token == null) {
                return null;
            }
            
            String cacheKey = TOKEN_PREFIX + token;
            Object cached = cacheManager.get(cacheKey);
            
            if (cached instanceof com.alibaba.fastjson.JSONObject) {
                return (com.alibaba.fastjson.JSONObject) cached;
            }
            
            return null;
        }
        
        /**
         * 验证令牌是否有效
         * @param token 令牌
         * @return 是否有效
         */
        public static boolean isValidToken(String token) {
            com.alibaba.fastjson.JSONObject tokenInfo = getTokenInfo(token);
            if (tokenInfo == null) {
                return false;
            }
            
            Long expireTime = tokenInfo.getLong("expireTime");
            return expireTime != null && System.currentTimeMillis() <= expireTime;
        }
        
        /**
         * 删除令牌
         * @param token 令牌
         * @return 是否删除成功
         */
        public static boolean removeToken(String token) {
            if (token == null) {
                return false;
            }
            
            String cacheKey = TOKEN_PREFIX + token;
            boolean removed = cacheManager.remove(cacheKey);
            if (removed) {
                logger.debug("下载令牌已删除: {}", token);
            }
            return removed;
        }
    }
}
