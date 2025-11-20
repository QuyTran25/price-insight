package com.pricetracker.client.net;

import com.pricetracker.client.crypto.SSLClientManager;
import com.pricetracker.security.AESUtil;
import com.pricetracker.security.KeyManager;

import javax.crypto.SecretKey;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * NetworkClient - Lớp xử lý kết nối và giao tiếp với Server
 * Chịu trách nhiệm gửi/nhận dữ liệu và mã hóa/giải mã thông tin
 * 
 * Hỗ trợ SSL/TLS cho secure communication
 */
public class NetworkClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private SecretKey encryptionKey;
    
    private String host;
    private int port;
    
    // SSL Manager
    private SSLClientManager sslManager;
    
    // Enable/Disable SSL (có thể config via system property)
    private final boolean enableSSL;
    
    /**
     * Constructor
     * @param host Địa chỉ server
     * @param port Cổng server
     */
    public NetworkClient(String host, int port) {
        this(host, port, !"false".equals(System.getProperty("ssl.enabled", "true")));
    }
    
    /**
     * Constructor với SSL option
     * @param host Địa chỉ server
     * @param port Cổng server
     * @param enableSSL Bật/tắt SSL
     */
    public NetworkClient(String host, int port, boolean enableSSL) {
        this.host = host;
        this.port = port;
        this.enableSSL = enableSSL;
        
        // Load encryption key
        try {
            this.encryptionKey = KeyManager.getKey();
            System.out.println("🔐 AES Encryption enabled");
        } catch (Exception e) {
            System.err.println("⚠️ AES Encryption disabled: " + e.getMessage());
            this.encryptionKey = null;
        }
    }
    
    /**
     * Kết nối đến server
     */
    public void connect() throws Exception {
        if (enableSSL) {
            System.out.println("🔒 Connecting with SSL/TLS...");
            try {
                // Khởi tạo SSL Manager
                sslManager = new SSLClientManager();
                
                // Tạo SSL Socket
                socket = sslManager.getSocketFactory().createSocket(host, port);
                
                // Config SSL parameters
                if (socket instanceof SSLSocket) {
                    SSLSocket sslSocket = (SSLSocket) socket;
                    SSLClientManager.configureSSLSocket(sslSocket);
                    
                    // Bắt đầu SSL handshake
                    sslSocket.startHandshake();
                    
                    // Hiển thị thông tin SSL
                    sslManager.printSSLInfo(sslSocket);
                }
                
                System.out.println("✅ SSL/TLS connection established");
            } catch (Exception e) {
                System.err.println("✗ SSL connection failed: " + e.getMessage());
                System.err.println("⚠️  Fallback to non-SSL connection...");
                socket = new Socket(host, port);
            }
        } else {
            System.out.println("⚠️  Connecting without SSL (not recommended)");
            socket = new Socket(host, port);
        }
        
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("✅ Connected to server: " + host + ":" + port);
    }
    
    /**
     * Gửi request và nhận response
     * @param request Request cần gửi
     * @return Response từ server
     */
    public String sendRequest(String request) throws Exception {
        if (socket == null || socket.isClosed()) {
            throw new IOException("Not connected to server");
        }
        
        // Mã hóa request
        String requestToSend;
        if (encryptionKey != null) {
            requestToSend = AESUtil.encrypt(request, encryptionKey);
            System.out.println("🔒 Request encrypted");
        } else {
            requestToSend = request;
        }
        
        // Gửi
        out.println(requestToSend);
        System.out.println("📤 Request sent: " + request);
        
        // Nhận response
        String encryptedResponse = in.readLine();
        if (encryptedResponse == null) {
            throw new IOException("Server closed connection");
        }
        
        // Giải mã response
        String response;
        if (encryptionKey != null) {
            response = AESUtil.decrypt(encryptedResponse, encryptionKey);
            System.out.println("🔓 Response decrypted");
        } else {
            response = encryptedResponse;
        }
        
        System.out.println("📥 Response received: " + response);
        return response;
    }
    
    /**
     * Đóng kết nối
     */
    public void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            System.out.println("✅ Connection closed");
        } catch (IOException e) {
            System.err.println("❌ Error closing connection: " + e.getMessage());
        }
    }
    
    /**
     * Kiểm tra kết nối
     */
    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }
}
