package com.pricetracker.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Response - Lớp đại diện cho phản hồi từ Server gửi về Client
 * Chứa dữ liệu kết quả, trạng thái và thông báo lỗi (nếu có)
 */
public class Response implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * Trạng thái của response
     */
    public enum Status {
        SUCCESS,        // Thành công
        ERROR,          // Lỗi
        NOT_FOUND,      // Không tìm thấy
        INVALID_REQUEST // Yêu cầu không hợp lệ
    }
    
    private Status status;
    private String message;
    private Object data;
    private long timestamp;
    
    /**
     * Constructor mặc định
     */
    public Response() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Constructor với status
     */
    public Response(Status status) {
        this();
        this.status = status;
    }
    
    /**
     * Constructor với status và message
     */
    public Response(Status status, String message) {
        this(status);
        this.message = message;
    }
    
    /**
     * Constructor đầy đủ
     */
    public Response(Status status, String message, Object data) {
        this(status, message);
        this.data = data;
    }
    
    // Static factory methods để tạo response nhanh
    
    /**
     * Tạo response thành công
     */
    public static Response success() {
        return new Response(Status.SUCCESS, "Thành công");
    }
    
    /**
     * Tạo response thành công với data
     */
    public static Response success(Object data) {
        return new Response(Status.SUCCESS, "Thành công", data);
    }
    
    /**
     * Tạo response thành công với message và data
     */
    public static Response success(String message, Object data) {
        return new Response(Status.SUCCESS, message, data);
    }
    
    /**
     * Tạo response lỗi
     */
    public static Response error(String message) {
        return new Response(Status.ERROR, message);
    }
    
    /**
     * Tạo response không tìm thấy
     */
    public static Response notFound(String message) {
        return new Response(Status.NOT_FOUND, message);
    }
    
    /**
     * Tạo response yêu cầu không hợp lệ
     */
    public static Response invalidRequest(String message) {
        return new Response(Status.INVALID_REQUEST, message);
    }
    
    // Getters and Setters
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Lấy data với kiểu cụ thể
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(Class<T> type) {
        if (data != null && type.isInstance(data)) {
            return (T) data;
        }
        return null;
    }
    
    /**
     * Lấy data dạng List
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getDataAsList(Class<T> elementType) {
        if (data instanceof List) {
            return (List<T>) data;
        }
        return new ArrayList<>();
    }
    
    /**
     * Kiểm tra response có thành công không
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
    
    /**
     * Kiểm tra response có lỗi không
     */
    public boolean isError() {
        return status == Status.ERROR;
    }
    
    @Override
    public String toString() {
        return "Response{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", data=" + (data != null ? data.getClass().getSimpleName() : "null") +
                ", timestamp=" + timestamp +
                '}';
    }
}
