package com.pricetracker.server.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.pricetracker.models.Review;

/**
 * ReviewDAO - Lớp truy vấn bảng 'review'
 * Phụ trách lấy, thêm và đếm đánh giá theo sản phẩm
 */
public class ReviewDAO {

    /**
     * Lấy danh sách review theo product_id (mới nhất trước)
     */
    public List<Review> getReviewsByProductId(int productId) {
        List<Review> list = new ArrayList<>();
        String sql = "SELECT * FROM review WHERE product_id = ? ORDER BY review_date DESC";

        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Review r = new Review();
                r.setReviewId(rs.getInt("review_id"));
                r.setProductId(rs.getInt("product_id"));
                r.setReviewerName(rs.getString("reviewer_name"));
                r.setRating(rs.getInt("rating"));
                r.setReviewText(rs.getString("review_text"));
                r.setReviewDate(rs.getTimestamp("review_date"));
                list.add(r);
            }

        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi truy vấn bảng review");
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Thêm một review mới vào database
     */
    public boolean addReview(Review review) {
        String sql = "INSERT INTO review (product_id, reviewer_name, rating, review_text, review_date) " +
                     "VALUES (?, ?, ?, ?, NOW())";

        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, review.getProductId());
            stmt.setString(2, review.getReviewerName());
            stmt.setInt(3, review.getRating());
            stmt.setString(4, review.getReviewText());

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi thêm review mới");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Đếm số lượng review của một sản phẩm
     */
    public int countReviewsByProductId(int productId) {
        String sql = "SELECT COUNT(*) FROM review WHERE product_id = ?";
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi đếm review");
            e.printStackTrace();
        }
        return 0;
    }
}
