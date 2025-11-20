package com.pricetracker.models;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * PriceData - Lớp POJO đại diện cho dữ liệu giá sản phẩm theo thời gian
 * Dùng để lưu trữ lịch sử biến động giá của sản phẩm
 */
public class PriceData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int historyId;
    private int productId;
    private double price;
    private Timestamp recordedAt;
    
    // Constructor mặc định
    public PriceData() {
    }
    
    // Constructor đầy đủ
    public PriceData(int historyId, int productId, double price, Timestamp recordedAt) {
        this.historyId = historyId;
        this.productId = productId;
        this.price = price;
        this.recordedAt = recordedAt;
    }
    
    // Constructor không có historyId (để insert)
    public PriceData(int productId, double price) {
        this.productId = productId;
        this.price = price;
    }
    
    // Getters and Setters
    public int getHistoryId() {
        return historyId;
    }
    
    public void setHistoryId(int historyId) {
        this.historyId = historyId;
    }
    
    public int getProductId() {
        return productId;
    }
    
    public void setProductId(int productId) {
        this.productId = productId;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public Timestamp getRecordedAt() {
        return recordedAt;
    }
    
    public void setRecordedAt(Timestamp recordedAt) {
        this.recordedAt = recordedAt;
    }
    
    @Override
    public String toString() {
        return "PriceData{" +
                "historyId=" + historyId +
                ", productId=" + productId +
                ", price=" + price +
                ", recordedAt=" + recordedAt +
                '}';
    }
}
