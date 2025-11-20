package com.pricetracker.server.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * DatabaseConnectionManager - Quản lý kết nối database với HikariCP
 * 
 * UPGRADED: Sử dụng HikariCP Connection Pool thay vì single connection
 * 
 * Benefits:
 * - Connection pooling: Tái sử dụng connections, giảm overhead
 * - Auto reconnection: Tự động reconnect khi mất kết nối
 * - Performance: Nhanh hơn 10-100x so với tự code
 * - Monitoring: Built-in metrics để track pool health
 * - Thread-safe: An toàn khi nhiều threads truy cập đồng thời
 * 
 * Usage:
 * Connection conn = DatabaseConnectionManager.getConnection();
 * try {
 *     // Use connection
 * } finally {
 *     conn.close(); // Trả lại pool, KHÔNG đóng thật sự
 * }
 */
public class DatabaseConnectionManager {
    private static DatabaseConnectionManager instance;
    private static boolean initialized = false;

    // Private constructor cho Singleton
    private DatabaseConnectionManager() {
        if (!initialized) {
            initializePool();
            initialized = true;
        }
    }

    /**
     * Khởi tạo HikariCP pool
     */
    private void initializePool() {
        try {
            HikariCPConfig.initialize();
            System.out.println("✓ DatabaseConnectionManager initialized with HikariCP");
        } catch (Exception e) {
            System.err.println("✗ Failed to initialize connection pool!");
            e.printStackTrace();
            throw new RuntimeException("Cannot start server without database connection pool", e);
        }
    }

    /**
     * Lấy instance của DatabaseConnectionManager (Singleton)
     */
    public static synchronized DatabaseConnectionManager getInstance() {
        if (instance == null) {
            instance = new DatabaseConnectionManager();
        }
        return instance;
    }

    /**
     * Lấy connection từ pool
     * 
     * IMPORTANT: Phải close() connection sau khi dùng xong để trả lại pool!
     * Recommend dùng try-with-resources:
     * 
     * try (Connection conn = DatabaseConnectionManager.getConnection()) {
     *     // Your code here
     * }
     */
    public static Connection getConnection() throws SQLException {
        try {
            Connection conn = HikariCPConfig.getDataSource().getConnection();
            
            // Log để debug (có thể tắt trong production)
            if (System.getProperty("db.debug", "false").equals("true")) {
                System.out.println("✓ Connection acquired from pool");
            }
            
            return conn;
        } catch (SQLException e) {
            System.err.println("✗ Failed to get connection from pool: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Đóng connection pool (gọi khi shutdown server)
     * Không gọi method này trong business logic!
     */
    public void shutdown() {
        HikariCPConfig.shutdown();
        System.out.println("✓ DatabaseConnectionManager shut down");
    }

    /**
     * Test kết nối database
     */
    public boolean testConnection() {
        return HikariCPConfig.testConnection();
    }

    /**
     * In thống kê pool (dùng để monitoring)
     */
    public void printPoolStats() {
        HikariCPConfig.printPoolStats();
    }

    /**
     * Legacy method để tương thích với code cũ
     * @deprecated Dùng shutdown() thay thế
     */
    @Deprecated
    public void closeConnection() {
        System.out.println("⚠ closeConnection() is deprecated. Use shutdown() for pool cleanup.");
        System.out.println("⚠ Individual connections will be returned to pool automatically when closed.");
    }
}
