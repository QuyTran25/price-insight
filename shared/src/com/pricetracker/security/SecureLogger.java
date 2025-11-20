package com.pricetracker.security;

import java.util.logging.*;
import java.util.regex.*;

/**
 * SecureLogger - Logger tự động ẩn sensitive data
 * Tránh log key/password ra console
 */
public class SecureLogger {
    private static final Logger logger = Logger.getLogger("PriceTracker");
    
    // Pattern để phát hiện sensitive data
    private static final Pattern SENSITIVE_PATTERN = 
        Pattern.compile("(key|password|secret|token|auth)\\s*[:=]\\s*([^\\s,}]+)", 
                       Pattern.CASE_INSENSITIVE);
    
    static {
        // Configure logger
        logger.setLevel(Level.INFO);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFormatter(new SimpleFormatter());
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
    }
    
    /**
     * Log message (tự động ẩn sensitive data)
     */
    public static void log(Level level, String message) {
        String sanitized = sanitizeMessage(message);
        logger.log(level, sanitized);
    }
    
    public static void info(String message) {
        log(Level.INFO, message);
    }
    
    public static void warning(String message) {
        log(Level.WARNING, message);
    }
    
    public static void error(String message) {
        log(Level.SEVERE, message);
    }
    
    public static void error(String message, Throwable t) {
        log(Level.SEVERE, message);
        if (t != null) {
            logger.log(Level.SEVERE, "Exception: " + t.getClass().getName() + ": " + 
                      sanitizeMessage(t.getMessage()));
        }
    }
    
    /**
     * Ẩn sensitive data trong message
     */
    private static String sanitizeMessage(String message) {
        if (message == null) {
            return null;
        }
        
        // Thay thế key/password/token bằng ***
        Matcher matcher = SENSITIVE_PATTERN.matcher(message);
        return matcher.replaceAll("$1: ****");
    }
}
