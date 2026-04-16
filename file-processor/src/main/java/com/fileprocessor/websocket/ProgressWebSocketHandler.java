package com.fileprocessor.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket处理器 - 用于上传进度通知
 */
@Component
public class ProgressWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ProgressWebSocketHandler.class);

    // 存储所有会话: userId -> session
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 反向映射: sessionId -> userId
    private final Map<String, String> sessionToUser = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = extractUserId(session);
        sessions.put(userId, session);
        sessionToUser.put(session.getId(), userId);

        log.info("WebSocket connected: userId={}, sessionId={}", userId, session.getId());

        // 发送连接成功消息
        sendMessage(session, new WebSocketMessage("connected", "Connected to progress notification service", null));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received message: {}", payload);

        try {
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String action = (String) data.get("action");

            switch (action) {
                case "subscribe":
                    handleSubscribe(session, data);
                    break;
                case "unsubscribe":
                    handleUnsubscribe(session, data);
                    break;
                case "ping":
                    sendMessage(session, new WebSocketMessage("pong", "pong", null));
                    break;
                default:
                    sendMessage(session, new WebSocketMessage("error", "Unknown action: " + action, null));
            }
        } catch (Exception e) {
            log.error("Failed to handle message", e);
            sendMessage(session, new WebSocketMessage("error", e.getMessage(), null));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        String userId = sessionToUser.remove(sessionId);
        if (userId != null) {
            sessions.remove(userId);
        }

        log.info("WebSocket disconnected: sessionId={}, status={}", sessionId, status);
    }

    /**
     * 发送进度通知给指定用户
     */
    public void sendProgress(String userId, String taskId, int progress, String status, String message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            ProgressData data = new ProgressData(taskId, progress, status, message);
            WebSocketMessage wsMessage = new WebSocketMessage("progress", "Upload progress", data);
            sendMessage(session, wsMessage);
        }
    }

    /**
     * 广播消息给所有连接
     */
    public void broadcast(String type, String message, Object data) {
        WebSocketMessage wsMessage = new WebSocketMessage(type, message, data);
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                sendMessage(session, wsMessage);
            }
        });
    }

    /**
     * 获取连接数
     */
    public int getConnectionCount() {
        return sessions.size();
    }

    private void handleSubscribe(WebSocketSession session, Map<String, Object> data) {
        String taskId = (String) data.get("taskId");
        log.info("User {} subscribed to task {}", extractUserId(session), taskId);

        // 将taskId关联到session
        session.getAttributes().put("subscribedTask", taskId);

        sendMessage(session, new WebSocketMessage("subscribed", "Subscribed to " + taskId, Map.of("taskId", taskId)));
    }

    private void handleUnsubscribe(WebSocketSession session, Map<String, Object> data) {
        String taskId = (String) data.get("taskId");
        log.info("User {} unsubscribed from task {}", extractUserId(session), taskId);

        session.getAttributes().remove("subscribedTask");

        sendMessage(session, new WebSocketMessage("unsubscribed", "Unsubscribed from " + taskId, Map.of("taskId", taskId)));
    }

    private void sendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Failed to send WebSocket message", e);
        }
    }

    private String extractUserId(WebSocketSession session) {
        // 从URL参数或token中提取用户ID
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                if (pair.startsWith("userId=")) {
                    return pair.substring(7);
                }
            }
        }

        // 从session attributes获取（如果已通过认证）
        Object userId = session.getAttributes().get("userId");
        if (userId != null) {
            return userId.toString();
        }

        // 默认使用session ID作为标识
        return session.getId();
    }

    // ==================== 内部消息类 ====================

    public static class WebSocketMessage {
        private String type;
        private String message;
        private Object data;
        private long timestamp;

        public WebSocketMessage(String type, String message, Object data) {
            this.type = type;
            this.message = message;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    public static class ProgressData {
        private String taskId;
        private int progress;
        private String status;
        private String message;
        private long timestamp;

        public ProgressData(String taskId, int progress, String status, String message) {
            this.taskId = taskId;
            this.progress = progress;
            this.status = status;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public int getProgress() {
            return progress;
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
