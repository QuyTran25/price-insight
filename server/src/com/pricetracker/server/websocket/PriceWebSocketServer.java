package com.pricetracker.server.websocket;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket Server để push real-time price updates tới frontend clients
 * Port: 8081
 */
public class PriceWebSocketServer extends WebSocketServer implements com.pricetracker.server.websocket.Broadcaster {
    
    private final int wsPort;
    
    // ⚡ Thread pool cho async broadcast (50 threads cho 50 clients)
    private static final int BROADCAST_THREADS = 50;
    private final ExecutorService broadcastExecutor;
    
    // Lưu tất cả connected clients (thread-safe)
    private final Set<WebSocket> clients = Collections.synchronizedSet(new HashSet<>());
    
    /**
     * Constructor với port tùy chỉnh (dùng cho Railway/Render)
     */
    public PriceWebSocketServer(int port) {
        super(new InetSocketAddress(port));
        this.wsPort = port;
        setReuseAddr(true); // Cho phép restart nhanh
        
        // ⚡ Khởi tạo thread pool cho async broadcast
        this.broadcastExecutor = Executors.newFixedThreadPool(BROADCAST_THREADS);
    }
    
    /**
     * Constructor mặc định (port 8081)
     */
    public PriceWebSocketServer() {
        this(8081);
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        clients.add(conn);
        String clientInfo = conn.getRemoteSocketAddress().toString();
        System.out.println("[WebSocket] Client connected: " + clientInfo + " (Total: " + clients.size() + ")");
        
        // Gửi welcome message
        conn.send("{\"type\":\"connected\",\"message\":\"Welcome to Price Tracker WebSocket!\"}");
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clients.remove(conn);
        String clientInfo = conn.getRemoteSocketAddress().toString();
        System.out.println("[WebSocket] Client disconnected: " + clientInfo + " (Remaining: " + clients.size() + ")");
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        // Xử lý message từ client (nếu cần)
        System.out.println("[WebSocket] Received from " + conn.getRemoteSocketAddress() + ": " + message);
        
        // Echo back (có thể mở rộng xử lý subscribe/unsubscribe)
        conn.send("{\"type\":\"echo\",\"data\":\"" + message + "\"}");
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[WebSocket] Error: " + ex.getMessage());
        ex.printStackTrace();
        
        if (conn != null) {
            clients.remove(conn);
        }
    }
    
    @Override
    public void onStart() {
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║  WebSocket Server Started on Port " + wsPort + "   ║");
        System.out.println("╚═══════════════════════════════════════════╝");
        setConnectionLostTimeout(100); // Ping clients every 100 seconds
    }
    
    /**
     * ⚡ Broadcast message tới TẤT CẢ connected clients - ASYNC PARALLEL
     * Thay vì gửi tuần tự (50 clients × 50ms = 2.5s),
     * gửi song song (max 200ms cho tất cả)
     * 
     * @param message JSON string để broadcast
     */
    public void broadcast(String message) {
        // Snapshot clients để tránh ConcurrentModificationException
        List<WebSocket> clientSnapshot;
        synchronized (clients) {
            clientSnapshot = new ArrayList<>(clients);
        }
        
        if (clientSnapshot.isEmpty()) {
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        // ⚡ Tạo CompletableFuture cho mỗi client
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (WebSocket client : clientSnapshot) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    if (client.isOpen()) {
                        client.send(message);
                        return true;
                    }
                    return false;
                } catch (Exception e) {
                    System.err.println("[WebSocket] Failed to send to client: " + e.getMessage());
                    return false;
                }
            }, broadcastExecutor);
            
            futures.add(future);
        }
        
        // ⚡ Đợi tất cả futures complete (non-blocking cho caller)
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenAccept(v -> {
                // Count results
                long successCount = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(success -> success)
                    .count();
                
                long failCount = futures.size() - successCount;
                long duration = System.currentTimeMillis() - startTime;
                
                System.out.println("[WebSocket] ⚡ Async broadcast to " + successCount + " clients in " + duration + "ms" +
                        (failCount > 0 ? " (" + failCount + " failed)" : ""));
            })
            .exceptionally(ex -> {
                System.err.println("[WebSocket] Broadcast error: " + ex.getMessage());
                return null;
            });
    }
    
    /**
     * Lấy số lượng clients đang connect
     */
    public int getClientCount() {
        return clients.size();
    }
    
    /**
     * Shutdown gracefully
     */
    public void shutdown() {
        try {
            System.out.println("[WebSocket] Shutting down... (" + clients.size() + " clients)");
            
            // Gửi disconnect message
            broadcast("{\"type\":\"server_shutdown\",\"message\":\"Server is shutting down\"}");
            
            // ⚡ Shutdown broadcast executor
            broadcastExecutor.shutdown();
            try {
                if (!broadcastExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    broadcastExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                broadcastExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            // Đóng tất cả connections
            synchronized (clients) {
                for (WebSocket client : clients) {
                    try {
                        client.close();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                clients.clear();
            }
            
            // Stop server
            stop(2000); // Timeout 2 seconds
            System.out.println("[WebSocket] Server stopped successfully");
            
        } catch (Exception e) {
            System.err.println("[WebSocket] Error during shutdown: " + e.getMessage());
        }
    }
}
