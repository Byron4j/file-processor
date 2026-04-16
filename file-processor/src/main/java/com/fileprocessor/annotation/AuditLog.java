package com.fileprocessor.annotation;

import java.lang.annotation.*;

/**
 * 审计日志注解 - 用于标记需要记录审计日志的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    String action();
    String resourceType() default "";
    boolean logParams() default true;
}
