package com.pricetracker.security;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * KeyManager - Quản lý khóa mã hóa
 * Đọc key từ environment variable để không lưu trong code
 */
public class KeyManager {
    private static final String ENV_KEY_NAME = "PRICE_TRACKER_KEY";
    private static SecretKey cachedKey = null;
    
    /**
     * Lấy key từ environment variable
     * Key phải được set trước khi chạy: set PRICE_TRACKER_KEY=<your-key>
     */
    public static SecretKey getKey() {
        if (cachedKey != null) {
            return cachedKey;
        }
        
        String base64Key = System.getenv(ENV_KEY_NAME);
        
        if (base64Key == null || base64Key.isEmpty()) {
            throw new RuntimeException(
                "❌ Thiếu encryption key!\n\n" +
                "Cách khắc phục:\n" +
                "1. Chạy: java tools.KeyGenerator\n" +
                "2. Copy key hiển thị ra\n" +
                "3. Set environment variable:\n" +
                "   Windows: set " + ENV_KEY_NAME + "=<key>\n" +
                "   Linux/Mac: export " + ENV_KEY_NAME + "=<key>\n" +
                "4. Chạy lại server/client\n"
            );
        }
        
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            
            if (keyBytes.length != 32) { // 256-bit
                throw new RuntimeException(
                    "❌ Key không hợp lệ! Phải là 256-bit (32 bytes)\n" +
                    "Chạy lại KeyGenerator để tạo key mới."
                );
            }
            
            cachedKey = new SecretKeySpec(keyBytes, "AES");
            System.out.println("✅ Encryption key loaded successfully");
            return cachedKey;
            
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                "❌ Key format không hợp lệ! Phải là Base64\n" +
                "Chạy KeyGenerator để tạo key mới.", e
            );
        }
    }
    
    /**
     * Kiểm tra key có được cấu hình chưa
     */
    public static boolean isKeyConfigured() {
        String key = System.getenv(ENV_KEY_NAME);
        return key != null && !key.isEmpty();
    }
    
    /**
     * Xóa cached key (dùng khi cần reload)
     */
    public static void clearCache() {
        cachedKey = null;
    }
}
