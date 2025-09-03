package com.jt.plugins.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @BelongsProject: jt-server-monitor
 * @BelongsPackage: com.jt.plugins.common.annotation
 * @Author: 别来无恙qb
 * @CreateTime: 2025-08-29  14:46
 * @Description: 处理方法注解，标记扩展中实际操作方法调用点
 * @Version: 1.0
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ActionHandler {
    String value();
}
