package com.fileprocessor.aop;

import com.fileprocessor.annotation.RateLimit;
import com.fileprocessor.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        String key = generateKey(rateLimit);

        if (key == null) {
            return point.proceed();
        }

        String redisKey = "rate_limit:" + key;
        Long current = redisTemplate.opsForValue().increment(redisKey);

        if (current == 1) {
            redisTemplate.expire(redisKey, rateLimit.window(), rateLimit.timeUnit());
        }

        if (current != null && current > rateLimit.limit()) {
            log.warn("Rate limit exceeded for key: {}", key);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded. Try again later.");
        }

        return point.proceed();
    }

    private String generateKey(RateLimit rateLimit) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        StringBuilder key = new StringBuilder();

        // Method path
        key.append(request.getMethod()).append(":").append(request.getRequestURI());

        // Custom key or user identifier
        if (!rateLimit.key().isEmpty()) {
            key.append(":").append(rateLimit.key());
        } else {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal user = (UserPrincipal) auth.getPrincipal();
                key.append(":user:").append(user.getId());
            } else {
                key.append(":ip:").append(getClientIp(request));
            }
        }

        return key.toString();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip.split(",")[0].trim() : "";
    }
}
