package com.pricetracker.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Request - Lớp đại diện cho yêu cầu từ Client gửi đến Server
 * Sử dụng để đóng gói các tham số và loại action
 */
public class Request implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * Các loại action có thể thực hiện
     */
    public enum Action {
        SEARCH_PRODUCT,          // Tìm kiếm sản phẩm
        GET_PRODUCT_DETAIL,      // Lấy chi tiết sản phẩm
        GET_PRICE_HISTORY,       // Lấy lịch sử giá
        GET_REVIEWS,             // Lấy danh sách review
        SEARCH_SUGGEST,          // Gợi ý tìm kiếm
        PING,                    // Kiểm tra kết nối
        DISCONNECT               // Ngắt kết nối
    }
    
    private Action action;
    private Map<String, Object> parameters;
    private String clientId;
    private long timestamp;
    
    /**
     * Constructor mặc định
     */
    public Request() {
        this.parameters = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Constructor với action
     */
    public Request(Action action) {
        this();
        this.action = action;
    }
    
    /**
     * Constructor đầy đủ
     */
    public Request(Action action, String clientId) {
        this(action);
        this.clientId = clientId;
    }
    
    // Getters and Setters
    public Action getAction() {
        return action;
    }
    
    public void setAction(Action action) {
        this.action = action;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Thêm một parameter vào request
     */
    public Request addParameter(String key, Object value) {
        this.parameters.put(key, value);
        return this;
    }
    
    /**
     * Lấy parameter theo key
     */
    public Object getParameter(String key) {
        return this.parameters.get(key);
    }
    
    /**
     * Lấy parameter với kiểu dữ liệu cụ thể
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> type) {
        Object value = this.parameters.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Lấy parameter dạng String
     */
    public String getStringParameter(String key) {
        Object value = this.parameters.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Lấy parameter dạng Integer
     */
    public Integer getIntParameter(String key) {
        Object value = this.parameters.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * Kiểm tra xem có parameter hay không
     */
    public boolean hasParameter(String key) {
        return this.parameters.containsKey(key);
    }
    
    @Override
    public String toString() {
        return "Request{" +
                "action=" + action +
                ", clientId='" + clientId + '\'' +
                ", parameters=" + parameters +
                ", timestamp=" + timestamp +
                '}';
    }
}
