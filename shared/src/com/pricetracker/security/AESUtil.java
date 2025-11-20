package com.pricetracker.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AESUtil - Tiện ích mã hóa/giải mã AES-GCM
 * Sử dụng AES/GCM/NoPadding để vừa mã hóa vừa xác thực
 */
public class AESUtil {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;  // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits
    
    /**
     * Mã hóa chuỗi text
     * 
     * @param plaintext Chuỗi cần mã hóa
     * @param key SecretKey dùng để mã hóa
     * @return Chuỗi đã mã hóa (Base64)
     */
    public static String encrypt(String plaintext, SecretKey key) throws Exception {
        // Tạo IV (Initialization Vector) ngẫu nhiên
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        
        // Khởi tạo cipher
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        
        // Mã hóa
        byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));
        
        // Ghép IV + encrypted data (IV không cần bí mật)
        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        
        // Encode sang Base64 để dễ truyền tải
        return Base64.getEncoder().encodeToString(result);
    }
    
    /**
     * Giải mã chuỗi đã mã hóa
     * 
     * @param ciphertext Chuỗi đã mã hóa (Base64)
     * @param key SecretKey dùng để giải mã
     * @return Chuỗi gốc
     */
    public static String decrypt(String ciphertext, SecretKey key) throws Exception {
        // Decode từ Base64
        byte[] decoded = Base64.getDecoder().decode(ciphertext);
        
        // Tách IV và encrypted data
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] encrypted = new byte[decoded.length - GCM_IV_LENGTH];
        System.arraycopy(decoded, 0, iv, 0, iv.length);
        System.arraycopy(decoded, iv.length, encrypted, 0, encrypted.length);
        
        // Khởi tạo cipher
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        
        // Giải mã
        byte[] decrypted = cipher.doFinal(encrypted);
        
        return new String(decrypted, "UTF-8");
    }
    
    /**
     * Test helper - Kiểm tra encrypt/decrypt hoạt động đúng
     */
    public static boolean testEncryption(SecretKey key) {
        try {
            String original = "Test message: Mã hóa AES-256/GCM";
            String encrypted = encrypt(original, key);
            String decrypted = decrypt(encrypted, key);
            return original.equals(decrypted);
        } catch (Exception e) {
            return false;
        }
    }
}
