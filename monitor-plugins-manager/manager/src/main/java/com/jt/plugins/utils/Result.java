package com.jt.plugins.utils;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

/**
 * @ Author：别来无恙qb
 * @ Date：2024-08-17-13:59
 * @ Version：1.0
 * @ Description：返回值统一处理
 */
@Data
public class Result<T> implements Serializable {

    /**
     * 是否成功, 默认false
     */
    private Boolean state = false;
    private int code = HttpStatus.OK.value();

    private String msg;

    private T data;


    public static <T> Result<T> success() {
        return success(null);
    }


    public static <T> Result<T> failure() {
        return failure(HttpStatus.BAD_REQUEST.value(),HttpStatus.BAD_REQUEST.getReasonPhrase());
    }

    public static <T> Result<T> success(T data) {
        return new Result<T>(true, HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(), data);
    }
    public static <T> Result<T> success(String msg) {
        return new Result<T>(true, HttpStatus.OK.value(), msg, null);
    }
    public static <T> Result<T> success(String msg, T data) {
        return new Result<T>(true, HttpStatus.OK.value(), msg, data);
    }

    public static <T> Result<T> failure(int errorCode, String errorMsg) {
        return failure(errorCode, errorMsg, null);
    }

    public static <T> Result<T> failure(int code, String errorMsg, T data) {
        return new Result<T>(false, code, errorMsg, data);
    }
    public Result(boolean state, int code, String msg, T data) {
        this.state = state;
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

}
