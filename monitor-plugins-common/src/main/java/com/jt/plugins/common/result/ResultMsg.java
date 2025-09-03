package com.jt.plugins.common.result;

import java.io.Serializable;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.common.result
 * @Author: 别来无恙qb
 * @CreateTime: 2025-08-12  15:05
 * @Description: 统一结果返回类，支持错误码
 * @Version: 1.0
 */
public class ResultMsg<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private T data; // 数据
    private String message = ""; // 消息
    private Integer state; // 1 成功 0 失败
    private Long timestamp; // 时间戳
    private String errorCode = ""; // 错误码

    public ResultMsg() {
        this.timestamp = System.currentTimeMillis();
    }

    public ResultMsg(T data) {
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public ResultMsg(T data, String message, Integer state) {
        this.data = data;
        this.message = message;
        this.state = state;
        this.timestamp = System.currentTimeMillis();
    }

    public ResultMsg(T data, String message, Integer state, Long timestamp) {
        this.data = data;
        this.message = message;
        this.state = state;
        this.timestamp = timestamp;
    }

    // 包含错误码的构造函数
    public ResultMsg(T data, String message, Integer state, String errorCode) {
        this.data = data;
        this.message = message;
        this.state = state;
        this.errorCode = errorCode;
        this.timestamp = System.currentTimeMillis();
    }

    // 包含所有字段的构造函数
    public ResultMsg(T data, String message, Integer state, String errorCode, Long timestamp) {
        this.data = data;
        this.message = message;
        this.state = state;
        this.errorCode = errorCode;
        this.timestamp = timestamp;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getState() {
        return state;
    }

    public void setState(Integer state) {
        this.state = state;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public static <T> ResultMsg<T> success() {
        ResultMsg<T> result = new ResultMsg<>();
        result.setState(1);
        result.setMessage("");
        return result;
    }

    public static <T> ResultMsg<T> success(T data) {
        ResultMsg<T> result = new ResultMsg<>();
        result.setData(data);
        result.setState(1);
        result.setMessage("");
        return result;
    }

    public static <T> ResultMsg<T> success(T data, String message) {
        ResultMsg<T> result = new ResultMsg<>();
        result.setData(data);
        result.setMessage(message);
        result.setState(1);
        return result;
    }

    public static <T> ResultMsg<T> success(T data, String message, Integer state) {
        ResultMsg<T> result = new ResultMsg<>();
        result.setData(data);
        result.setMessage(message);
        result.setState(state != null ? state : 1);
        return result;
    }

    // 带错误码的成功方法
    public static <T> ResultMsg<T> success(T data, String message, Integer state, String errorCode) {
        ResultMsg<T> result = new ResultMsg<>();
        result.setData(data);
        result.setMessage(message);
        result.setState(state != null ? state : 1);
        result.setErrorCode(errorCode);
        return result;
    }

    public static <T> ResultMsg<T> fail(String message) {
        ResultMsg<T> result = new ResultMsg<>();
        result.setData(null);
        result.setMessage(message);
        result.setState(0);
        return result;
    }

    public static <T> ResultMsg<T> fail(String message, Integer state) {
        ResultMsg<T> result = new ResultMsg<>();
        result.setData(null);
        result.setMessage(message);
        result.setState(state != null ? state : 0);
        return result;
    }

    // 带错误码的失败方法
    public static <T> ResultMsg<T> fail(String message, Integer state, String errorCode) {
        ResultMsg<T> result = new ResultMsg<>();
        result.setData(null);
        result.setMessage(message);
        result.setState(state != null ? state : 0);
        result.setErrorCode(errorCode);
        return result;
    }

    // 仅带错误码的失败方法
    public static <T> ResultMsg<T> fail(String errorCode, String message) {
        return fail(message, 0, errorCode);
    }
}
