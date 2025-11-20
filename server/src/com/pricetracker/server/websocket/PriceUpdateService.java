package com.pricetracker.server.websocket;

import com.pricetracker.server.db.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service để monitor database changes và broadcast qua WebSocket
 * Check database mỗi 30 giây
 */
public class PriceUpdateService {
    
    private final PriceWebSocketServer webSocketServer;
    private final ScheduledExecutorService scheduler;
    private Timestamp lastCheckTime;
    
    // Check interval: 30 seconds
    private static final int CHECK_INTERVAL_SECONDS = 30;
    
    public PriceUpdateService(PriceWebSocketServer webSocketServer) {
        this.webSocketServer = webSocketServer;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.lastCheckTime = new Timestamp(System.currentTimeMillis());
    }
    
    /**
     * Start monitoring database
     */
    public void start() {
        System.out.println("[PriceUpdate] Service started - checking every " + CHECK_INTERVAL_SECONDS + " seconds");
        
        // Schedule task mỗi 30 giây
        scheduler.scheduleAtFixedRate(
            this::checkForUpdates,
            10, // Initial delay 10s (đợi server start xong)
            CHECK_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
    }
    
    /**
     * Stop monitoring
     */
    public void stop() {
        System.out.println("[PriceUpdate] Stopping service...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            System.out.println("[PriceUpdate] Service stopped successfully");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Check database cho price updates từ lần check cuối
     */
    private void checkForUpdates() {
        try {
            // Chỉ check khi có clients connected
            if (webSocketServer.getClientCount() == 0) {
                return;
            }
            
            List<PriceUpdate> updates = queryPriceUpdates();
            
            if (!updates.isEmpty()) {
                System.out.println("[PriceUpdate] Found " + updates.size() + " price changes");
                
                // Broadcast từng update
                for (PriceUpdate update : updates) {
                    String json = createUpdateMessage(update);
                    webSocketServer.broadcast(json);
                }
                
                // Update last check time
                lastCheckTime = new Timestamp(System.currentTimeMillis());
            }
            
        } catch (Exception e) {
            System.err.println("[PriceUpdate] Error checking updates: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Query database cho sản phẩm có price thay đổi
     */
    private List<PriceUpdate> queryPriceUpdates() {
        List<PriceUpdate> updates = new ArrayList<>();
        
        // Query: Lấy sản phẩm có recorded_at mới hơn lastCheckTime
        // recorded_at là cột timestamp trong bảng price_history
        String sql = "SELECT p.product_id, p.name as product_name, p.image_url, " +
                     "       ph.price, ph.original_price, " +
                     "       ROUND(((ph.original_price - ph.price) / ph.original_price) * 100) as discount_percent, " +
                     "       ph.recorded_at " +
                     "FROM product p " +
                     "INNER JOIN price_history ph ON p.product_id = ph.product_id " +
                     "WHERE ph.recorded_at > ? " +
                     "ORDER BY ph.recorded_at DESC " +
                     "LIMIT 50"; // Giới hạn 50 updates mỗi lần
        
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setTimestamp(1, lastCheckTime);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    PriceUpdate update = new PriceUpdate();
                    update.productId = rs.getString("product_id");
                    update.productName = rs.getString("product_name");
                    update.imageUrl = rs.getString("image_url");
                    update.currentPrice = rs.getDouble("price");
                    update.originalPrice = rs.getDouble("original_price");
                    update.discountPercent = rs.getInt("discount_percent");
                    update.updatedAt = rs.getTimestamp("recorded_at");
                    
                    updates.add(update);
                }
            }
            
        } catch (Exception e) {
            System.err.println("[PriceUpdate] Database query error: " + e.getMessage());
        }
        
        return updates;
    }
    
    /**
     * Tạo JSON message cho price update (manual JSON building)
     */
    private String createUpdateMessage(PriceUpdate update) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"type\":\"price_update\",");
        json.append("\"product_id\":\"").append(escapeJson(update.productId)).append("\",");
        json.append("\"product_name\":\"").append(escapeJson(update.productName)).append("\",");
        json.append("\"image_url\":\"").append(escapeJson(update.imageUrl)).append("\",");
        json.append("\"current_price\":").append(update.currentPrice).append(",");
        json.append("\"original_price\":").append(update.originalPrice).append(",");
        json.append("\"discount_percent\":").append(update.discountPercent).append(",");
        json.append("\"updated_at\":\"").append(escapeJson(update.updatedAt.toString())).append("\",");
        json.append("\"timestamp\":").append(System.currentTimeMillis());
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Escape JSON special characters
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Inner class cho price update data
     */
    private static class PriceUpdate {
        String productId;
        String productName;
        String imageUrl;
        double currentPrice;
        double originalPrice;
        int discountPercent;
        Timestamp updatedAt;
    }
}
