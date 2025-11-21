package com.pricetracker.server.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class PriceWebSocketHandler extends TextWebSocketHandler {

    // Lưu các session kết nối (Thread-safe)
    private static final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());
    
    // Thread pool cho broadcast (Giữ nguyên logic của bạn)
    private static final int BROADCAST_THREADS = 50;
    private final ExecutorService broadcastExecutor = Executors.newFixedThreadPool(BROADCAST_THREADS);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("[WebSocket] Client connected: " + session.getRemoteAddress());
        
        // Gửi welcome message
        session.sendMessage(new TextMessage("{\"type\":\"connected\",\"message\":\"Welcome to Price Tracker WebSocket!\"}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("[WebSocket] Client disconnected. Remaining: " + sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Xử lý tin nhắn từ Client gửi lên (nếu cần)
        System.out.println("[WebSocket] Received: " + message.getPayload());
    }

    /**
     * ⚡ Broadcast message tới tất cả clients (Async)
     */
    public void broadcast(String jsonData) {
        if (sessions.isEmpty()) return;

        Set<WebSocketSession> activeSessions;
        synchronized (sessions) {
            activeSessions = new HashSet<>(sessions);
        }

        for (WebSocketSession session : activeSessions) {
            CompletableFuture.runAsync(() -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(jsonData));
                    }
                } catch (IOException e) {
                    // Ignore error when sending fails
                }
            }, broadcastExecutor);
        }
    }
}