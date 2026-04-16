package com.fileprocessor.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fileprocessor.config.RateLimitConfig;
import com.fileprocessor.dto.FileResponse;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RateLimitConfig rateLimitConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);

        if (rateLimit == null) {
            return true;
        }

        String key = getClientKey(request);
        RateLimiter limiter = rateLimit.strict()
                ? rateLimitConfig.createStrictLimiter()
                : rateLimitConfig.getLimiter(key);

        if (limiter.tryAcquire()) {
            return true;
        } else {
            sendErrorResponse(response, HttpStatus.TOO_MANY_REQUESTS,
                    "Rate limit exceeded. Please try again later.");
            return false;
        }
    }

    private String getClientKey(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ip = getClientIP(request);
        return ip + "_" + (userAgent != null ? userAgent.hashCode() : "unknown");
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        FileResponse errorResponse = FileResponse.builder()
                .success(false)
                .message(message)
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
