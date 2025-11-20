package com.pricetracker.server;

import com.pricetracker.server.core.PriceTrackerServer;

/**
 * Main - Điểm khởi động ứng dụng Server
 * Khởi tạo và chạy PriceTrackerServer
 */
public class Main {
    
    // Cổng mặc định cho server
    private static final int DEFAULT_PORT = 8888;
    
    public static void main(String[] args) {
        // Lấy port từ tham số dòng lệnh hoặc dùng port mặc định
        int port = DEFAULT_PORT;
        
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Port không hợp lệ. Sử dụng port mặc định: " + DEFAULT_PORT);
            }
        }
        
        // Hiển thị thông tin khởi động
        System.out.println("===========================================");
        System.out.println("  PRICE TRACKER SERVER - HỆ THỐNG THEO DÕI GIÁ");
        System.out.println("===========================================");
        System.out.println("Server đang khởi động trên port: " + port);
        System.out.println("Thời gian: " + new java.util.Date());
        System.out.println("===========================================\n");
        
        // Khởi tạo và chạy server
        PriceTrackerServer server = new PriceTrackerServer(port);
        server.start();
    }
}
