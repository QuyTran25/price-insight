package com.pricetracker.models;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Product - Lớp POJO đại diện cho thông tin sản phẩm
 * Dùng chung giữa Client và Server để truyền tải dữ liệu sản phẩm
 */
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int productId;
    private int groupId;
    private String name;
    private String brand;
    private String url;
    private String imageUrl;
    private String description;
    private String source;
    private boolean isFeatured;
    private Timestamp createdAt;
    
    // Constructor mặc định
    public Product() {
    }
    
    // Constructor đầy đủ
    public Product(int productId, int groupId, String name, String brand, String url,
                   String imageUrl, String description, String source, boolean isFeatured) {
        this.productId = productId;
        this.groupId = groupId;
        this.name = name;
        this.brand = brand;
        this.url = url;
        this.imageUrl = imageUrl;
        this.description = description;
        this.source = source;
        this.isFeatured = isFeatured;
    }
    
    // Getters and Setters
    public int getProductId() {
        return productId;
    }
    
    public void setProductId(int productId) {
        this.productId = productId;
    }
    
    public int getGroupId() {
        return groupId;
    }
    
    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getBrand() {
        return brand;
    }
    
    public void setBrand(String brand) {
        this.brand = brand;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public boolean isFeatured() {
        return isFeatured;
    }
    
    public void setFeatured(boolean featured) {
        isFeatured = featured;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "Product{" +
                "productId=" + productId +
                ", name='" + name + '\'' +
                ", brand='" + brand + '\'' +
                ", groupId=" + groupId +
                ", isFeatured=" + isFeatured +
                '}';
    }
}

