package com.pricetracker.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ProductData - Lớp wrapper chứa toàn bộ thông tin của một sản phẩm
 * Bao gồm: thông tin sản phẩm, lịch sử giá, và danh sách review
 * Dùng để truyền tải dữ liệu tổng hợp từ Server về Client
 */
public class ProductData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Product product;
    private List<PriceHistory> priceHistory;
    private List<Review> reviews;
    private PriceHistory currentPrice;
    private double averageRating;
    private int totalReviews;
    
    /**
     * Constructor mặc định
     */
    public ProductData() {
        this.priceHistory = new ArrayList<>();
        this.reviews = new ArrayList<>();
    }
    
    /**
     * Constructor với product
     */
    public ProductData(Product product) {
        this();
        this.product = product;
    }
    
    /**
     * Constructor đầy đủ
     */
    public ProductData(Product product, List<PriceHistory> priceHistory, List<Review> reviews) {
        this.product = product;
        this.priceHistory = priceHistory != null ? priceHistory : new ArrayList<>();
        this.reviews = reviews != null ? reviews : new ArrayList<>();
        calculateStatistics();
    }
    
    // Getters and Setters
    
    public Product getProduct() {
        return product;
    }
    
    public void setProduct(Product product) {
        this.product = product;
    }
    
    public List<PriceHistory> getPriceHistory() {
        return priceHistory;
    }
    
    public void setPriceHistory(List<PriceHistory> priceHistory) {
        this.priceHistory = priceHistory;
        findCurrentPrice();
    }
    
    public List<Review> getReviews() {
        return reviews;
    }
    
    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
        calculateAverageRating();
    }
    
    public PriceHistory getCurrentPrice() {
        if (currentPrice == null) {
            findCurrentPrice();
        }
        return currentPrice;
    }
    
    public void setCurrentPrice(PriceHistory currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public double getAverageRating() {
        return averageRating;
    }
    
    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }
    
    public int getTotalReviews() {
        return totalReviews;
    }
    
    public void setTotalReviews(int totalReviews) {
        this.totalReviews = totalReviews;
    }
    
    // Helper methods
    
    /**
     * Thêm một bản ghi giá vào lịch sử
     */
    public void addPriceHistory(PriceHistory price) {
        if (price != null) {
            this.priceHistory.add(price);
            findCurrentPrice();
        }
    }
    
    /**
     * Thêm một review
     */
    public void addReview(Review review) {
        if (review != null) {
            this.reviews.add(review);
            calculateAverageRating();
        }
    }
    
    /**
     * Tìm giá hiện tại (mới nhất) từ lịch sử giá
     */
    private void findCurrentPrice() {
        if (priceHistory != null && !priceHistory.isEmpty()) {
            currentPrice = priceHistory.get(priceHistory.size() - 1);
            
            // Tìm giá có timestamp mới nhất
            for (PriceHistory ph : priceHistory) {
                if (ph.getCapturedAt() != null && currentPrice.getCapturedAt() != null) {
                    if (ph.getCapturedAt().after(currentPrice.getCapturedAt())) {
                        currentPrice = ph;
                    }
                }
            }
        }
    }
    
    /**
     * Tính điểm đánh giá trung bình từ danh sách review
     */
    private void calculateAverageRating() {
        if (reviews != null && !reviews.isEmpty()) {
            double sum = 0;
            for (Review review : reviews) {
                sum += review.getRating();
            }
            this.averageRating = sum / reviews.size();
            this.totalReviews = reviews.size();
        } else {
            this.averageRating = 0;
            this.totalReviews = 0;
        }
    }
    
    /**
     * Tính toán tất cả các thống kê
     */
    public void calculateStatistics() {
        findCurrentPrice();
        calculateAverageRating();
    }
    
    /**
     * Lấy giá thấp nhất trong lịch sử
     */
    public Double getLowestPrice() {
        if (priceHistory == null || priceHistory.isEmpty()) {
            return null;
        }
        
        double lowest = Double.MAX_VALUE;
        for (PriceHistory ph : priceHistory) {
            if (ph.getPrice() < lowest) {
                lowest = ph.getPrice();
            }
        }
        return lowest;
    }
    
    /**
     * Lấy giá cao nhất trong lịch sử
     */
    public Double getHighestPrice() {
        if (priceHistory == null || priceHistory.isEmpty()) {
            return null;
        }
        
        double highest = Double.MIN_VALUE;
        for (PriceHistory ph : priceHistory) {
            if (ph.getPrice() > highest) {
                highest = ph.getPrice();
            }
        }
        return highest;
    }
    
    /**
     * Tính phần trăm thay đổi giá so với giá gốc
     */
    public Double getPriceChangePercent() {
        if (currentPrice == null) {
            return null;
        }
        
        double current = currentPrice.getPrice();
        double original = currentPrice.getOriginalPrice();
        
        if (original <= 0) {
            return null;
        }
        
        return ((current - original) / original) * 100;
    }
    
    @Override
    public String toString() {
        return "ProductData{" +
                "product=" + (product != null ? product.getName() : "null") +
                ", priceHistoryCount=" + (priceHistory != null ? priceHistory.size() : 0) +
                ", reviewsCount=" + (reviews != null ? reviews.size() : 0) +
                ", currentPrice=" + (currentPrice != null ? currentPrice.getPrice() : "null") +
                ", averageRating=" + averageRating +
                '}';
    }
}
