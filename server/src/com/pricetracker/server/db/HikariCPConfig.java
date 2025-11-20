package com.pricetracker.server.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * HikariCPConfig - Cấu hình HikariCP Connection Pool
 * HikariCP là connection pool nhanh nhất và được sử dụng rộng rãi nhất
 * 
 * Features:
 * - Auto connection recovery
 * - Connection leak detection
 * - Performance monitoring
 * - Configurable pool size
 */
public class HikariCPConfig {
    private static HikariDataSource dataSource;

    /**
     * Khởi tạo HikariCP với cấu hình tối ưu
     */
    public static void initialize() {
        if (dataSource != null && !dataSource.isClosed()) {
            return; // Đã khởi tạo rồi
        }

        try {
            HikariConfig config = new HikariConfig();
            
            // === Database Connection Settings ===
            // Ưu tiên 1: DATABASE_URL từ Railway/Render
            // Ưu tiên 2: DB_HOST, DB_USER, DB_PASSWORD riêng lẻ
            // Ưu tiên 3: Local defaults
            String databaseUrl = getDatabaseUrl();
            String dbUser = getEnvOrProperty("DB_USER", "db.user", "root");
            String dbPassword = getEnvOrProperty("DB_PASSWORD", "db.password", "");
            
            config.setJdbcUrl(databaseUrl);
            config.setUsername(dbUser);
            config.setPassword(dbPassword);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            // === Pool Size Configuration ===
            // Maximum connections trong pool
            // ⚡ Tăng từ 10 → 30 để support 50 concurrent users
            // Formula: (50 users / 2) + buffer = ~30 connections
            config.setMaximumPoolSize(Integer.parseInt(getProperty("db.pool.maxSize", "30")));
            
            // Minimum idle connections (connections sẵn sàng chờ)
            // ⚡ Tăng từ 5 → 15 để có đủ connections sẵn sàng
            config.setMinimumIdle(Integer.parseInt(getProperty("db.pool.minIdle", "15")));

            // === Connection Timeout Settings ===
            // Thời gian chờ để lấy connection từ pool (milliseconds)
            config.setConnectionTimeout(30000); // 30 seconds
            
            // Thời gian tối đa một connection có thể idle (milliseconds)
            config.setIdleTimeout(600000); // 10 minutes
            
            // Thời gian tối đa một connection tồn tại (milliseconds)
            config.setMaxLifetime(1800000); // 30 minutes

            // === Performance Tuning ===
            // Connection test query
            config.setConnectionTestQuery("SELECT 1");
            
            // Tên pool để dễ identify trong logs
            config.setPoolName("PriceTrackerPool");
            
            // Auto-commit (recommended: true)
            config.setAutoCommit(true);

            // === Leak Detection ===
            // Cảnh báo khi connection bị leak (không trả lại pool)
            config.setLeakDetectionThreshold(60000); // 60 seconds

            // === MySQL Specific Optimizations ===
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");

            dataSource = new HikariDataSource(config);
            System.out.println("✓ HikariCP Connection Pool initialized successfully");
            System.out.println("  ├─ Pool Name: " + config.getPoolName());
            System.out.println("  ├─ Max Pool Size: " + config.getMaximumPoolSize());
            System.out.println("  ├─ Min Idle: " + config.getMinimumIdle());
            System.out.println("  └─ Database: " + config.getJdbcUrl());
            
        } catch (Exception e) {
            System.err.println("✗ Failed to initialize HikariCP: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Cannot initialize database connection pool", e);
        }
    }

    /**
     * Lấy DataSource để get connections
     */
    public static HikariDataSource getDataSource() {
        if (dataSource == null || dataSource.isClosed()) {
            initialize();
        }
        return dataSource;
    }

    /**
     * Đóng connection pool (gọi khi shutdown server)
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("✓ HikariCP Connection Pool closed");
        }
    }

    /**
     * Lấy thông tin monitoring của pool
     */
    public static void printPoolStats() {
        if (dataSource != null && !dataSource.isClosed()) {
            System.out.println("\n=== HikariCP Pool Statistics ===");
            System.out.println("Active Connections: " + dataSource.getHikariPoolMXBean().getActiveConnections());
            System.out.println("Idle Connections: " + dataSource.getHikariPoolMXBean().getIdleConnections());
            System.out.println("Total Connections: " + dataSource.getHikariPoolMXBean().getTotalConnections());
            System.out.println("Threads Awaiting: " + dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
            System.out.println("================================\n");
        }
    }

    /**
     * Lấy database URL từ environment hoặc fallback to local
     */
    private static String getDatabaseUrl() {
        // Railway/Render format: mysql://user:pass@host:port/dbname
        String databaseUrl = System.getenv("DATABASE_URL");
        
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            // Chuyển đổi mysql:// thành jdbc:mysql://
            if (databaseUrl.startsWith("mysql://")) {
                databaseUrl = "jdbc:" + databaseUrl;
            }
            // Thêm timezone và SSL params nếu chưa có
            if (!databaseUrl.contains("serverTimezone")) {
                String separator = databaseUrl.contains("?") ? "&" : "?";
                databaseUrl += separator + "serverTimezone=UTC&useSSL=true&requireSSL=true";
            }
            System.out.println("✓ Using DATABASE_URL from environment");
            return databaseUrl;
        }
        
        // Fallback: Đọc từ DB_HOST, DB_PORT, DB_NAME riêng lẻ
        String host = getEnvOrProperty("DB_HOST", "db.host", "localhost");
        String port = getEnvOrProperty("DB_PORT", "db.port", "3306");
        String dbName = getEnvOrProperty("DB_NAME", "db.name", "price_insight");
        
        String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC&useSSL=false", 
                                       host, port, dbName);
        System.out.println("✓ Using local database: " + jdbcUrl);
        return jdbcUrl;
    }

    /**
     * Helper method để đọc từ env var trước, rồi mới tới system property và config file
     */
    private static String getEnvOrProperty(String envKey, String propKey, String defaultValue) {
        // Ưu tiên 1: Environment variable (Railway/Render)
        String value = System.getenv(envKey);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        
        // Ưu tiên 2: System property
        value = System.getProperty(propKey);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        // Ưu tiên 3: config.ini file
        try {
            Properties props = new Properties();
            props.load(new FileInputStream("config.ini"));
            value = props.getProperty(propKey);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        } catch (IOException e) {
            // Không có config file, dùng default
        }

        return defaultValue;
    }
    
    /**
     * Helper method để đọc config từ file hoặc dùng default (kept for backward compatibility)
     */
    private static String getProperty(String key, String defaultValue) {
        return getEnvOrProperty(key.toUpperCase().replace(".", "_"), key, defaultValue);
    }

    /**
     * Test connection pool
     */
    public static boolean testConnection() {
        try {
            java.sql.Connection conn = getDataSource().getConnection();
            boolean isValid = conn.isValid(5); // timeout 5 seconds
            conn.close(); // Trả lại pool
            return isValid;
        } catch (Exception e) {
            System.err.println("✗ Connection test failed: " + e.getMessage());
            return false;
        }
    }
}
