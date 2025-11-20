import com.pricetracker.security.AESUtil;
import com.pricetracker.security.KeyManager;
import javax.crypto.SecretKey;

/**
 * Test mã hóa AES
 * Chạy: javac -cp "bin;shared/src" test/TestEncryption.java
 *       java -cp "bin;shared/src;test" TestEncryption
 */
public class TestEncryption {
    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("         TEST MÃ HÓA AES-256/GCM");
        System.out.println("═══════════════════════════════════════════════════\n");
        
        try {
            // Kiểm tra key có được set chưa
            if (!KeyManager.isKeyConfigured()) {
                System.err.println("❌ Environment variable PRICE_TRACKER_KEY chưa được set!");
                System.err.println("\nChạy lệnh sau:");
                System.err.println("1. java tools.KeyGenerator");
                System.err.println("2. set PRICE_TRACKER_KEY=<key-từ-bước-1>");
                System.exit(1);
            }
            
            // Load key
            System.out.println("📌 Loading encryption key...");
            SecretKey key = KeyManager.getKey();
            System.out.println("✅ Key loaded\n");
            
            // Test 1: Mã hóa chuỗi tiếng Việt
            System.out.println("TEST 1: Mã hóa chuỗi tiếng Việt");
            System.out.println("───────────────────────────────────────────────────");
            String original = "Sản phẩm iPhone 15 Pro Max giá 30.000.000đ";
            System.out.println("Original:  " + original);
            
            String encrypted = AESUtil.encrypt(original, key);
            System.out.println("Encrypted: " + encrypted.substring(0, Math.min(60, encrypted.length())) + "...");
            
            String decrypted = AESUtil.decrypt(encrypted, key);
            System.out.println("Decrypted: " + decrypted);
            
            if (original.equals(decrypted)) {
                System.out.println("✅ PASS - Giải mã chính xác\n");
            } else {
                System.out.println("❌ FAIL - Giải mã sai!\n");
                System.exit(1);
            }
            
            // Test 2: Mã hóa JSON
            System.out.println("TEST 2: Mã hóa JSON data");
            System.out.println("───────────────────────────────────────────────────");
            String json = "{\"action\":\"SEARCH_PRODUCT\",\"keyword\":\"iPhone\",\"price\":20000000}";
            System.out.println("Original JSON:  " + json);
            
            String encryptedJson = AESUtil.encrypt(json, key);
            System.out.println("Encrypted JSON: " + encryptedJson.substring(0, Math.min(60, encryptedJson.length())) + "...");
            
            String decryptedJson = AESUtil.decrypt(encryptedJson, key);
            System.out.println("Decrypted JSON: " + decryptedJson);
            
            if (json.equals(decryptedJson)) {
                System.out.println("✅ PASS - JSON giải mã chính xác\n");
            } else {
                System.out.println("❌ FAIL - JSON giải mã sai!\n");
                System.exit(1);
            }
            
            // Test 3: IV unique (mỗi lần mã hóa khác nhau)
            System.out.println("TEST 3: IV Uniqueness");
            System.out.println("───────────────────────────────────────────────────");
            String msg = "Test message";
            String enc1 = AESUtil.encrypt(msg, key);
            String enc2 = AESUtil.encrypt(msg, key);
            
            System.out.println("Encrypt 1: " + enc1.substring(0, 40) + "...");
            System.out.println("Encrypt 2: " + enc2.substring(0, 40) + "...");
            
            if (!enc1.equals(enc2)) {
                System.out.println("✅ PASS - Mỗi lần mã hóa cho ra kết quả khác nhau (IV unique)\n");
            } else {
                System.out.println("❌ FAIL - Cùng plaintext cho ra cùng ciphertext!\n");
                System.exit(1);
            }
            
            // Test 4: Tampering detection
            System.out.println("TEST 4: Tampering Detection (GCM Authentication)");
            System.out.println("───────────────────────────────────────────────────");
            String original4 = "Important data";
            String encrypted4 = AESUtil.encrypt(original4, key);
            
            // Thử modify 1 ký tự trong ciphertext
            String tampered = encrypted4.substring(0, encrypted4.length() - 2) + "XX";
            
            try {
                String decrypted4 = AESUtil.decrypt(tampered, key);
                System.out.println("❌ FAIL - GCM không phát hiện tampering!");
                System.exit(1);
            } catch (Exception e) {
                System.out.println("✅ PASS - GCM phát hiện được tampering");
                System.out.println("   Error: " + e.getMessage() + "\n");
            }
            
            // Summary
            System.out.println("═══════════════════════════════════════════════════");
            System.out.println("           ✅ TẤT CẢ TESTS PASS!");
            System.out.println("═══════════════════════════════════════════════════");
            System.out.println("\n📊 Thống kê:");
            System.out.println("   • Algorithm: AES-256/GCM/NoPadding");
            System.out.println("   • IV Length: 12 bytes (unique mỗi lần)");
            System.out.println("   • Tag Length: 16 bytes (authentication)");
            System.out.println("   • Encoding: Base64");
            System.out.println("\n✅ Hệ thống mã hóa sẵn sàng sử dụng!");
            
        } catch (Exception e) {
            System.err.println("\n❌ LỖI: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
