package com.pricetracker.models;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Review - Lớp POJO đại diện cho đánh giá sản phẩm
 * Dùng để lưu trữ các bài review của khách hàng về sản phẩm
 */
public class Review implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int reviewId;
    private int productId;
    private String reviewerName;
    private int rating;
    private String reviewText;
    private Timestamp reviewDate;
    
    // Constructor mặc định
    public Review() {
    }
    
    // Constructor đầy đủ
    public Review(int reviewId, int productId, String reviewerName, int rating, 
                  String reviewText, Timestamp reviewDate) {
        this.reviewId = reviewId;
        this.productId = productId;
        this.reviewerName = reviewerName;
        this.rating = rating;
        this.reviewText = reviewText;
        this.reviewDate = reviewDate;
    }
    
    // Constructor không có reviewId (để insert)
    public Review(int productId, String reviewerName, int rating, String reviewText) {
        this.productId = productId;
        this.reviewerName = reviewerName;
        this.rating = rating;
        this.reviewText = reviewText;
    }
    
    // Getters and Setters
    public int getReviewId() {
        return reviewId;
    }
    
    public void setReviewId(int reviewId) {
        this.reviewId = reviewId;
    }
    
    public int getProductId() {
        return productId;
    }
    
    public void setProductId(int productId) {
        this.productId = productId;
    }
    
    public String getReviewerName() {
        return reviewerName;
    }
    
    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }
    
    public int getRating() {
        return rating;
    }
    
    public void setRating(int rating) {
        this.rating = rating;
    }
    
    public String getReviewText() {
        return reviewText;
    }
    
    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }
    
    public Timestamp getReviewDate() {
        return reviewDate;
    }
    
    public void setReviewDate(Timestamp reviewDate) {
        this.reviewDate = reviewDate;
    }
    
    @Override
    public String toString() {
        return "Review{" +
                "reviewId=" + reviewId +
                ", productId=" + productId +
                ", reviewerName='" + reviewerName + '\'' +
                ", rating=" + rating +
                ", reviewDate=" + reviewDate +
                '}';
    }
}
