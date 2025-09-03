package com.jt.plugins.execption;

import com.jt.plugins.common.result.ResultMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.common.exception
 * @Author: 别来无恙qb
 * @CreateTime: 2025-09-01  10:34
 * @Description: 全局异常处理器
 * @Version: 1.0
 */

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResultMsg<String> handleException(Exception e) {
        logger.error("系统异常: ", e);
        return ResultMsg.fail("系统异常，请联系管理员", 500);
    }

    /**
     * 处理自定义业务异常（如果有的话）
     */
    @ExceptionHandler(RuntimeException.class)
    public ResultMsg<String> handleRuntimeException(RuntimeException e) {
        logger.error("业务异常: ", e);
        return ResultMsg.fail(e.getMessage(), 500);
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResultMsg<String> handleNullPointerException(NullPointerException e) {
        logger.error("空指针异常: ", e);
        return ResultMsg.fail("数据不存在或为空", 500);
    }

    /**
     * 处理参数校验异常（如果使用了 validation）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResultMsg<String> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.error("参数校验异常: ", e);
        return ResultMsg.fail(e.getMessage(), 400);
    }
}