package com.fileprocessor.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    int limit() default 100;
    int window() default 60;
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    String key() default "";
}
