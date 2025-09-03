package com.jt.plugins.common.http;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.common.http
 * @Author: 别来无恙qb
 * @CreateTime: 2025-08-12  15:21
 * @Description: TODO
 * @Version: 1.0
 */

import com.jt.plugins.common.result.ResultMsg;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 插件通用HTTP请求客户端
 */
public class HttpClient {

    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private static final int DEFAULT_READ_TIMEOUT = 10000;

    /**
     * 发送GET请求
     *
     * @param url 请求URL
     * @return 响应结果
     */
    public static ResultMsg<String> get(String url) {
        return get(url, null, null);
    }

    /**
     * 发送GET请求
     *
     * @param url 请求URL
     * @param headers 请求头
     * @return 响应结果
     */
    public static ResultMsg<String> get(String url, Map<String, String> headers) {
        return get(url, headers, null);
    }

    /**
     * 发送GET请求
     *
     * @param url 请求URL
     * @param headers 请求头
     * @param params 请求参数
     * @return 响应结果
     */
    public static ResultMsg<String> get(String url, Map<String, String> headers, Map<String, String> params) {
        try {
            // 构建带参数的URL
            if (params != null && !params.isEmpty()) {
                StringBuilder urlBuilder = new StringBuilder(url);
                if (!url.contains("?")) {
                    urlBuilder.append("?");
                }

                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (urlBuilder.charAt(urlBuilder.length() - 1) != '?') {
                        urlBuilder.append("&");
                    }
                    urlBuilder.append(entry.getKey())
                            .append("=")
                            .append(entry.getValue());
                }
                url = urlBuilder.toString();
            }

            URL requestUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();

            // 设置请求方法和超时时间
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_READ_TIMEOUT);

            // 设置请求头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // 获取响应
            int responseCode = connection.getResponseCode();
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            if (responseCode >= 200 && responseCode < 300) {
                return ResultMsg.success(response.toString(), "请求成功", responseCode);
            } else {
                return ResultMsg.fail("请求失败: " + response.toString(), responseCode);
            }
        } catch (Exception e) {
            return ResultMsg.fail("请求异常: " + e.getMessage(), 500);
        }
    }

    /**
     * 发送POST请求
     *
     * @param url 请求URL
     * @param body 请求体
     * @return 响应结果
     */
    public static ResultMsg<String> post(String url, String body) {
        return post(url, body, null, "application/json");
    }

    /**
     * 发送POST请求
     *
     * @param url 请求URL
     * @param body 请求体
     * @param headers 请求头
     * @return 响应结果
     */
    public static ResultMsg<String> post(String url, String body, Map<String, String> headers) {
        return post(url, body, headers, "application/json");
    }

    /**
     * 发送POST请求
     *
     * @param url 请求URL
     * @param body 请求体
     * @param headers 请求头
     * @param contentType 内容类型
     * @return 响应结果
     */
    public static ResultMsg<String> post(String url, String body, Map<String, String> headers, String contentType) {
        try {
            URL requestUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();

            // 设置请求方法和属性
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_READ_TIMEOUT);
            connection.setDoOutput(true);

            // 设置请求头
            connection.setRequestProperty("Content-Type", contentType);
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // 发送请求体
            if (body != null && !body.isEmpty()) {
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = body.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            // 获取响应
            int responseCode = connection.getResponseCode();
            BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            if (responseCode >= 200 && responseCode < 300) {
                return ResultMsg.success(response.toString(), "请求成功", responseCode);
            } else {
                return ResultMsg.fail("请求失败: " + response.toString(), responseCode);
            }
        } catch (Exception e) {
            return ResultMsg.fail("请求异常: " + e.getMessage(), 500);
        }
    }
}
