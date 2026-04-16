package com.fileprocessor.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int requests() default 100;
    int windowMinutes() default 1;
    boolean strict() default false;
    String keyExpression() default "#request.remoteAddr";
}
