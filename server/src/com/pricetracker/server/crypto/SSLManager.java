package com.pricetracker.server.crypto;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * SSLManager - Quản lý SSL/TLS Context cho Server
 * 
 * Chức năng:
 * - Load keystore chứa private key và certificate
 * - Tạo SSLContext với các cipher suites mạnh
 * - Cung cấp SSLServerSocketFactory để tạo secure connections
 * 
 * Security: Sử dụng TLSv1.3 (hoặc TLSv1.2 nếu không có)
 */
public class SSLManager {
    
    private SSLContext sslContext;
    private static final String PROTOCOL = "TLSv1.3";
    private static final String KEYSTORE_TYPE = "JKS";
    
    // Default paths (có thể override bằng system properties)
    private static final String DEFAULT_KEYSTORE_PATH = "certs/server.keystore";
    private static final String DEFAULT_KEYSTORE_PASSWORD = "pricetracker123";
    
    /**
     * Constructor - Khởi tạo SSL Context với default settings
     */
    public SSLManager() throws Exception {
        this(
            System.getProperty("ssl.keystore.path", DEFAULT_KEYSTORE_PATH),
            System.getProperty("ssl.keystore.password", DEFAULT_KEYSTORE_PASSWORD)
        );
    }
    
    /**
     * Constructor với custom keystore path và password
     * 
     * @param keystorePath Đường dẫn đến file keystore
     * @param keystorePassword Password của keystore
     */
    public SSLManager(String keystorePath, String keystorePassword) throws Exception {
        initializeSSLContext(keystorePath, keystorePassword);
    }
    
    /**
     * Khởi tạo SSL Context
     */
    private void initializeSSLContext(String keystorePath, String keystorePassword) 
            throws KeyStoreException, IOException, NoSuchAlgorithmException, 
                   CertificateException, UnrecoverableKeyException, KeyManagementException {
        
        // 1. Load Keystore
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, keystorePassword.toCharArray());
        }
        
        // 2. Khởi tạo KeyManagerFactory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm()
        );
        kmf.init(keyStore, keystorePassword.toCharArray());
        
        // 3. Khởi tạo TrustManagerFactory (cho mutual TLS nếu cần)
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        );
        tmf.init(keyStore);
        
        // 4. Tạo SSL Context
        try {
            sslContext = SSLContext.getInstance(PROTOCOL);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to TLSv1.2 if TLSv1.3 not available
            System.err.println("⚠️  TLSv1.3 không khả dụng, sử dụng TLSv1.2");
            sslContext = SSLContext.getInstance("TLSv1.2");
        }
        
        sslContext.init(
            kmf.getKeyManagers(),
            tmf.getTrustManagers(),
            new SecureRandom()
        );
        
        System.out.println("✓ SSL Context đã được khởi tạo thành công");
        System.out.println("  - Protocol: " + sslContext.getProtocol());
        System.out.println("  - Keystore: " + keystorePath);
    }
    
    /**
     * Lấy SSLServerSocketFactory để tạo SSL ServerSocket
     */
    public SSLServerSocketFactory getServerSocketFactory() {
        return sslContext.getServerSocketFactory();
    }
    
    /**
     * Lấy SSLContext (nếu cần custom configuration)
     */
    public SSLContext getSSLContext() {
        return sslContext;
    }
    
    /**
     * Config SSL Socket với các tham số bảo mật mạnh
     * 
     * @param sslSocket SSL Socket cần config
     */
    public static void configureSSLSocket(SSLSocket sslSocket) {
        // Chỉ cho phép các protocol mạnh
        sslSocket.setEnabledProtocols(new String[] {
            "TLSv1.3",
            "TLSv1.2"
        });
        
        // Chỉ cho phép các cipher suites mạnh (không có NULL, EXPORT, DES, RC4)
        String[] strongCiphers = {
            "TLS_AES_256_GCM_SHA384",
            "TLS_AES_128_GCM_SHA256",
            "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256"
        };
        
        // Filter để chỉ giữ lại cipher suites được hỗ trợ
        java.util.Set<String> supportedCiphers = new java.util.HashSet<>(
            java.util.Arrays.asList(sslSocket.getSupportedCipherSuites())
        );
        
        java.util.List<String> enabledCiphers = new java.util.ArrayList<>();
        for (String cipher : strongCiphers) {
            if (supportedCiphers.contains(cipher)) {
                enabledCiphers.add(cipher);
            }
        }
        
        if (!enabledCiphers.isEmpty()) {
            sslSocket.setEnabledCipherSuites(
                enabledCiphers.toArray(new String[0])
            );
        }
        
        // Yêu cầu client authentication (optional - tắt nếu không cần)
        // sslSocket.setNeedClientAuth(true);
    }
    
    /**
     * Hiển thị thông tin SSL configuration
     */
    public void printSSLInfo(SSLServerSocket serverSocket) {
        System.out.println("\n=== SSL/TLS Configuration ===");
        System.out.println("Enabled Protocols:");
        for (String protocol : serverSocket.getEnabledProtocols()) {
            System.out.println("  - " + protocol);
        }
        
        System.out.println("\nEnabled Cipher Suites:");
        String[] ciphers = serverSocket.getEnabledCipherSuites();
        int count = Math.min(ciphers.length, 10); // Chỉ hiển thị 10 cái đầu
        for (int i = 0; i < count; i++) {
            System.out.println("  - " + ciphers[i]);
        }
        if (ciphers.length > 10) {
            System.out.println("  ... và " + (ciphers.length - 10) + " cipher suites khác");
        }
        System.out.println("=============================\n");
    }
}
