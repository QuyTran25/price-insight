package com.pricetracker.server.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.pricetracker.models.PriceHistory;

/**
 * PriceHistoryDAO - Lớp truy vấn bảng 'price_history'
 * Đồng bộ với cấu trúc bảng thực tế trong MySQL
 */
public class PriceHistoryDAO {

    /**
     * Lấy danh sách lịch sử giá của 1 sản phẩm (sắp xếp theo thời gian tăng dần)
     */
    public List<PriceHistory> getPriceHistoryByProductId(int productId) {
        List<PriceHistory> list = new ArrayList<>();
        String sql = "SELECT * FROM price_history WHERE product_id = ? ORDER BY recorded_at ASC";

        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                PriceHistory ph = new PriceHistory();
                ph.setPriceId(rs.getInt("price_id"));
                ph.setProductId(rs.getInt("product_id"));
                ph.setPrice(rs.getDouble("price"));
                ph.setCapturedAt(rs.getTimestamp("recorded_at")); // vẫn dùng capturedAt trong object, nhưng đọc từ recorded_at
                list.add(ph);
            }

        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi truy vấn bảng price_history");
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Thêm một bản ghi giá mới cho sản phẩm
     */
    public boolean addPriceRecord(int productId, double price) {
        String sql = "INSERT INTO price_history (product_id, price, recorded_at, deal_type, currency) " +
                     "VALUES (?, ?, NOW(), 'NORMAL', 'VND')";

        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            stmt.setDouble(2, price);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi thêm bản ghi giá mới");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy giá mới nhất của sản phẩm
     */
    public Double getLatestPrice(int productId) {
        String sql = "SELECT price FROM price_history WHERE product_id = ? ORDER BY recorded_at DESC LIMIT 1";

        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("price");
            }

        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi lấy giá mới nhất");
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Get current price data (price, original_price, deal_type) of a product
     * Returns PriceHistory object with most recent data
     * @param productId The product ID
     * @return PriceHistory with current price data, or null if not found
     */
    public PriceHistory getCurrentPrice(int productId) {
        String sql = "SELECT * FROM price_history WHERE product_id = ? ORDER BY recorded_at DESC LIMIT 1";
        
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                PriceHistory ph = new PriceHistory();
                ph.setPriceId(rs.getInt("price_id"));
                ph.setProductId(rs.getInt("product_id"));
                ph.setPrice(rs.getDouble("price"));
                ph.setOriginalPrice(rs.getDouble("original_price"));
                ph.setCurrency(rs.getString("currency"));
                ph.setDealType(rs.getString("deal_type"));
                ph.setCapturedAt(rs.getTimestamp("recorded_at"));
                return ph;
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting current price: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Add complete price record with original_price and deal_type
     * Used for real-time scraping
     * @param productId Product ID
     * @param price Current price
     * @param originalPrice Original price (before discount)
     * @param dealType Deal type (NORMAL, FLASH_SALE, HOT_DEAL, etc.)
     * @return true if successful
     */
    public boolean addCompletePriceRecord(int productId, double price, double originalPrice, String dealType) {
        String sql = "INSERT INTO price_history (product_id, price, original_price, currency, deal_type, recorded_at) " +
                     "VALUES (?, ?, ?, 'VND', ?, NOW())";

        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            stmt.setDouble(2, price);
            stmt.setDouble(3, originalPrice);
            stmt.setString(4, dealType);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi thêm bản ghi giá đầy đủ: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
