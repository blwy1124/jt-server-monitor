package com.jt.plugins.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/download")
public class FileDownloadController {
    
    // 令牌缓存（生产环境应使用Redis等）
    private static final Map<String, Map<String, Object>> TOKEN_CACHE = new ConcurrentHashMap<>();
    
    @GetMapping("/{token}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String token, HttpServletRequest request) {
        try {
            // 验证令牌
            Map<String, Object> tokenInfo = TOKEN_CACHE.get(token);
            if (tokenInfo == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查是否过期
            Long expireTime = (Long) tokenInfo.get("expireTime");
            if (System.currentTimeMillis() > expireTime) {
                TOKEN_CACHE.remove(token);
                return ResponseEntity.notFound().build();
            }
            
            String filePath = (String) tokenInfo.get("filePath");
            File file = new File(filePath);
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"" + file.getName() + "\"");
            headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, 
                       HttpHeaders.CONTENT_DISPOSITION);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 清理过期令牌的方法（可以定时执行）
    public void cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        TOKEN_CACHE.entrySet().removeIf(entry -> {
            Long expireTime = (Long) entry.getValue().get("expireTime");
            return currentTime > expireTime;
        });
    }
}
