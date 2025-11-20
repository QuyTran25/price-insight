package tools;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Tool sinh AES-256 key
 * Chạy 1 lần khi setup: javac tools/AESKeyGenerator.java && java tools.AESKeyGenerator
 */
public class AESKeyGenerator {
    public static void main(String[] args) {
        try {
            System.out.println("🔐 Generating AES-256 Key...\n");
            
            // Sinh key AES-256 bằng SecureRandom
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256, SecureRandom.getInstanceStrong());
            SecretKey key = keyGen.generateKey();
            
            // Convert sang Base64
            String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
            
            System.out.println("✅ Key đã được tạo thành công!\n");
            System.out.println("═══════════════════════════════════════════════════════════════");
            System.out.println(base64Key);
            System.out.println("═══════════════════════════════════════════════════════════════");
            
            System.out.println("\n📋 Hướng dẫn sử dụng:");
            System.out.println("1. Copy key ở trên");
            System.out.println("2. Set environment variable:");
            System.out.println("\n   Windows CMD:");
            System.out.println("   set PRICE_TRACKER_KEY=" + base64Key);
            System.out.println("\n   Windows PowerShell:");
            System.out.println("   $env:PRICE_TRACKER_KEY=\"" + base64Key + "\"");
            System.out.println("\n   Linux/Mac:");
            System.out.println("   export PRICE_TRACKER_KEY=" + base64Key);
            
            System.out.println("\n⚠️ LƯU Ý QUAN TRỌNG:");
            System.out.println("   • KHÔNG commit key này vào Git");
            System.out.println("   • Server và Client phải dùng CÙNG key");
            System.out.println("   • Chia sẻ key cho team qua kênh riêng tư");
            
            System.out.println("\n📊 Thông số kỹ thuật:");
            System.out.println("   Algorithm: AES");
            System.out.println("   Key Size: 256 bits");
            System.out.println("   Entropy: High (SecureRandom)");
            
        } catch (Exception e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
