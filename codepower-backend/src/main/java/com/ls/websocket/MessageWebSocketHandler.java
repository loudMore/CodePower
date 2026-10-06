/**
 * WebSocket 消息处理器 —— 私信实时推送的核心。
 *
 * 连接建立流程：
 *   1. 前端 new WebSocket("ws://host/ws/messages?token=<JWT>")
 *   2. MessageWebSocketAuthInterceptor.beforeHandshake() 解析 JWT 验证身份
 *   3. afterConnectionEstablished() 把 userId → WebSocketSession 存入 ConcurrentHashMap
 *   4. 消息发送走 HTTP POST入库 → 事务提交后 pushPrivateMessage() 推给收发双方
 *   5. 同一用户多标签页各有一条 session，推送时遍历发送
 *
 * 消息收发：
 *   发送：HTTP POST /api/messages/send → 入库 → 事务回调 afterCommit() → WebSocket 推送
 *   推送 JSON: { type: "private_message", peerUserId, message: {...} }
 *   前端双通道兜底：WebSocket 实时推送 + HTTP 10 秒轮询
 */
package com.ls.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ls.domain.PrivateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 私信 WebSocket 推送器。单机部署下直接用内存会话表即可。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ConcurrentMap<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserId(session);
        if (userId == null) {
            closeQuietly(session, CloseStatus.NOT_ACCEPTABLE.withReason("unauthorized"));
            return;
        }

        userSessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
        sendJson(session, Map.of(
                "type", "connected",
                "userId", userId
        ));
        log.info("WebSocket connected: userId={}, session={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if ("ping".equalsIgnoreCase(payload)) {
            sendJson(session, Map.of("type", "pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("WebSocket transport error: session={}, error={}", session.getId(), exception.getMessage());
        removeSession(session);
        closeQuietly(session, CloseStatus.SERVER_ERROR);
    }

    public void pushPrivateMessage(PrivateMessage message) {
        if (message == null) {
            return;
        }
        sendToUser(message.getFromUserId(), buildPayload(message, message.getFromUserId()));
        if (!Objects.equals(message.getFromUserId(), message.getToUserId())) {
            sendToUser(message.getToUserId(), buildPayload(message, message.getToUserId()));
        }
    }

    public void sendToUser(Long userId, Object payload) {
        if (userId == null || payload == null) {
            return;
        }
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("WebSocket payload serialize failed: userId={}", userId, e);
            return;
        }

        for (WebSocketSession session : new ArrayList<>(sessions)) {
            if (session == null || !session.isOpen()) {
                removeSession(session);
                continue;
            }
            try {
                synchronized (session) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(json));
                    }
                }
            } catch (IOException e) {
                log.warn("WebSocket send failed: userId={}, session={}, error={}", userId, session.getId(), e.getMessage());
                removeSession(session);
                closeQuietly(session, CloseStatus.SERVER_ERROR);
            }
        }
    }

    private void sendJson(WebSocketSession session, Object payload) {
        if (session == null || payload == null || !session.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.warn("WebSocket send json failed: session={}, error={}", session.getId(), e.getMessage());
        }
    }

    /** 构造 WebSocket 推送 JSON。前端收到后根据 type 字段分发处理。 */
    // 推送 JSON 格式:
    // {
    //   "type": "private_message",
    //   "viewerUserId": 1,
    //   "peerUserId": 2,
    //   "message": {
    //     "id": 123,
    //     "fromUserId": 1,
    //     "toUserId": 2,
    //     "content": "你好，这道题怎么做？",
    //     "isRead": false,
    //     "createdAt": "2026-05-29T12:00:00"
    //   }
    // }
    private Map<String, Object> buildPayload(PrivateMessage message, Long viewerUserId) {
        Long peerUserId = Objects.equals(viewerUserId, message.getFromUserId())
                ? message.getToUserId()
                : message.getFromUserId();
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("id", message.getId());
        msg.put("fromUserId", message.getFromUserId());
        msg.put("toUserId", message.getToUserId());
        msg.put("content", message.getContent());
        msg.put("isRead", message.getIsRead());
        msg.put("createdAt", message.getCreatedAt());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "private_message");
        payload.put("viewerUserId", viewerUserId);
        payload.put("peerUserId", peerUserId);
        payload.put("message", msg);
        return payload;
    }

    private Long getUserId(WebSocketSession session) {
        Object value = session.getAttributes().get("userId");
        if (value instanceof Long userId) {
            return userId;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            try {
                return Long.valueOf(String.valueOf(value));
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }

    private void removeSession(WebSocketSession session) {
        if (session == null) {
            return;
        }
        Long userId = getUserId(session);
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                userSessions.remove(userId, sessions);
            }
        }
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.close(status);
        } catch (IOException ignore) {
            // 关闭连接失败不影响业务流程，直接忽略即可。
        }
    }
}
