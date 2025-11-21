package com.pricetracker.server.websocket;

/**
 * Generic broadcaster interface for real-time messages.
 * Implementations: WebSocket-based or SSE-based broadcasters.
 */
public interface Broadcaster {
    void broadcast(String message);
    int getClientCount();
}
