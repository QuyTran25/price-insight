package com.pricetracker.client.crypto;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * SSLClientManager - Quản lý SSL/TLS Context cho Client
 * 
 * Chức năng:
 * - Load truststore chứa server certificates
 * - Tạo SSLContext để verify server
 * - Cung cấp SSLSocketFactory để tạo secure connections
 * - Hỗ trợ trust all certificates cho development
 * 
 * Security: Client verify server certificate để tránh MITM attacks
 */
public class SSLClientManager {
    
    private SSLContext sslContext;
    private static final String PROTOCOL = "TLSv1.3";
    private static final String TRUSTSTORE_TYPE = "JKS";
    
    // Default paths (có thể override bằng system properties)
    private static final String DEFAULT_TRUSTSTORE_PATH = "client/certs/truststore.jks";
    private static final String DEFAULT_TRUSTSTORE_PASSWORD = "pricetracker123";
    
    private boolean trustAllCerts = false;
    
    /**
     * Constructor - Khởi tạo SSL Context với default settings
     */
    public SSLClientManager() throws Exception {
        this(
            System.getProperty("ssl.truststore.path", DEFAULT_TRUSTSTORE_PATH),
            System.getProperty("ssl.truststore.password", DEFAULT_TRUSTSTORE_PASSWORD),
            "true".equals(System.getProperty("ssl.trustAll", "false"))
        );
    }
    
    /**
     * Constructor với custom truststore
     * 
     * @param truststorePath Đường dẫn đến truststore
     * @param truststorePassword Password của truststore
     * @param trustAllCerts Trust tất cả certificates (chỉ dùng dev)
     */
    public SSLClientManager(String truststorePath, String truststorePassword, boolean trustAllCerts) 
            throws Exception {
        this.trustAllCerts = trustAllCerts;
        initializeSSLContext(truststorePath, truststorePassword);
    }
    
    /**
     * Khởi tạo SSL Context
     */
    private void initializeSSLContext(String truststorePath, String truststorePassword) 
            throws KeyStoreException, IOException, NoSuchAlgorithmException, 
                   CertificateException, KeyManagementException {
        
        TrustManager[] trustManagers;
        
        if (trustAllCerts) {
            // DEVELOPMENT ONLY - Trust all certificates
            System.err.println("⚠️  WARNING: Trusting ALL certificates (DEVELOPMENT MODE)");
            System.err.println("⚠️  DO NOT USE IN PRODUCTION!");
            
            trustManagers = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
            };
        } else {
            // PRODUCTION - Verify server certificate
            try {
                // 1. Load Truststore
                KeyStore trustStore = KeyStore.getInstance(TRUSTSTORE_TYPE);
                try (FileInputStream fis = new FileInputStream(truststorePath)) {
                    trustStore.load(fis, truststorePassword.toCharArray());
                }
                
                // 2. Khởi tạo TrustManagerFactory
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm()
                );
                tmf.init(trustStore);
                trustManagers = tmf.getTrustManagers();
                
                System.out.println("✓ Loaded truststore from: " + truststorePath);
            } catch (IOException e) {
                System.err.println("⚠️  Cannot load truststore: " + e.getMessage());
                System.err.println("⚠️  Falling back to system default trust anchors");
                
                // Fallback to system trust anchors
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm()
                );
                tmf.init((KeyStore) null); // Use system default
                trustManagers = tmf.getTrustManagers();
            }
        }
        
        // 3. Tạo SSL Context
        try {
            sslContext = SSLContext.getInstance(PROTOCOL);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to TLSv1.2 if TLSv1.3 not available
            System.err.println("⚠️  TLSv1.3 không khả dụng, sử dụng TLSv1.2");
            sslContext = SSLContext.getInstance("TLSv1.2");
        }
        
        sslContext.init(null, trustManagers, new SecureRandom());
        
        System.out.println("✓ SSL Context đã được khởi tạo thành công");
        System.out.println("  - Protocol: " + sslContext.getProtocol());
        System.out.println("  - Trust mode: " + (trustAllCerts ? "TRUST ALL (DEV)" : "VERIFY (PROD)"));
    }
    
    /**
     * Lấy SSLSocketFactory để tạo SSL Socket
     */
    public SSLSocketFactory getSocketFactory() {
        return sslContext.getSocketFactory();
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
        
        // Chỉ cho phép các cipher suites mạnh
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
    }
    
    /**
     * Hiển thị thông tin SSL configuration
     */
    public void printSSLInfo(SSLSocket socket) {
        System.out.println("\n=== SSL/TLS Client Configuration ===");
        System.out.println("Connected to: " + socket.getInetAddress().getHostName());
        System.out.println("Port: " + socket.getPort());
        
        // Show session info after handshake
        SSLSession session = socket.getSession();
        System.out.println("\nNegotiated Protocol: " + session.getProtocol());
        System.out.println("Cipher Suite: " + session.getCipherSuite());
        
        try {
            System.out.println("\nServer Certificate:");
            java.security.cert.Certificate[] serverCerts = session.getPeerCertificates();
            if (serverCerts.length > 0 && serverCerts[0] instanceof java.security.cert.X509Certificate) {
                java.security.cert.X509Certificate x509 = 
                    (java.security.cert.X509Certificate) serverCerts[0];
                System.out.println("  Subject: " + x509.getSubjectDN());
                System.out.println("  Issuer: " + x509.getIssuerDN());
                System.out.println("  Valid: " + x509.getNotBefore() + " to " + x509.getNotAfter());
            }
        } catch (Exception e) {
            System.err.println("  Cannot retrieve certificate: " + e.getMessage());
        }
        
        System.out.println("====================================\n");
    }
}
