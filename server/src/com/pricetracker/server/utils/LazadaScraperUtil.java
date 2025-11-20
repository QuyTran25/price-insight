package com.pricetracker.server.utils;

import com.pricetracker.models.Product;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for scraping Lazada product data in real-time
 * Tầng 2: Thu thập dữ liệu theo yêu cầu (On-Demand)
 * Used when user searches for a new product not in database
 */
public class LazadaScraperUtil {
    
    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile("-i(\\d+)-s(\\d+)\\.html");
    
    // Category mapping to group_id (based on 9 groups in database)
    private static final Map<String, Integer> CATEGORY_MAP = new HashMap<>();
    
    static {
        // Group 1: Điện tử
        CATEGORY_MAP.put("điện thoại", 1);
        CATEGORY_MAP.put("smartphone", 1);
        CATEGORY_MAP.put("laptop", 1);
        CATEGORY_MAP.put("máy tính", 1);
        CATEGORY_MAP.put("tablet", 1);
        CATEGORY_MAP.put("điện tử", 1);
        
        // Group 2: Điện gia dụng
        CATEGORY_MAP.put("điện gia dụng", 2);
        CATEGORY_MAP.put("gia dụng", 2);
        CATEGORY_MAP.put("tủ lạnh", 2);
        CATEGORY_MAP.put("máy giặt", 2);
        
        // Group 3: Thời trang
        CATEGORY_MAP.put("thời trang", 3);
        CATEGORY_MAP.put("quần áo", 3);
        CATEGORY_MAP.put("giày", 3);
        
        // Group 4: Làm đẹp
        CATEGORY_MAP.put("làm đẹp", 4);
        CATEGORY_MAP.put("mỹ phẩm", 4);
        
        // Group 5: Sách
        CATEGORY_MAP.put("sách", 5);
        
        // Group 6: Đồ chơi
        CATEGORY_MAP.put("đồ chơi", 6);
        CATEGORY_MAP.put("mẹ và bé", 6);
        
        // Group 7: Thể thao
        CATEGORY_MAP.put("thể thao", 7);
    }
    
    /**
     * Extract product ID from Lazada URL
     * @param lazadaUrl URL like https://www.lazada.vn/products/...-i12345-s67890.html
     * @return Product ID or -1 if invalid
     */
    public static int extractProductId(String lazadaUrl) {
        Matcher matcher = PRODUCT_ID_PATTERN.matcher(lazadaUrl);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return -1;
    }
    
    /**
     * Scrape product data from Lazada
     * Parse HTML to extract price using regex (similar to Python approach)
     * 
     * @param lazadaUrl The Lazada product URL
     * @return Product object with scraped data, or null if failed
     */
    public static Product scrapeProductFromUrl(String lazadaUrl) {
        int productId = extractProductId(lazadaUrl);
        if (productId == -1) {
            System.err.println("Invalid Lazada URL: " + lazadaUrl);
            return null;
        }
        
        try {
            URL url = new URL(lazadaUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Referer", "https://www.lazada.vn/");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Lazada returned code: " + responseCode);
                return null;
            }
            
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            
            String html = response.toString();
            
            Product product = new Product();
            
            // Extract product name from title tag
            String name = extractBetween(html, "<title>", "</title>");
            if (name != null) {
                // Clean up title (remove "| Lazada.vn" suffix)
                name = name.replaceAll("\\s*\\|.*", "").trim();
                product.setName(name);
            } else {
                product.setName("Sản phẩm Lazada #" + productId);
            }
            
            // Extract image URL from meta og:image
            // Try multiple patterns for robustness
            String imageUrl = extractBetween(html, "property=\"og:image\" content=\"", "\"");
            if (imageUrl == null) {
                imageUrl = extractBetween(html, "property='og:image' content='", "'");
            }
            if (imageUrl == null) {
                // Try without quotes
                imageUrl = extractBetween(html, "property=og:image content=", " ");
            }
            product.setImageUrl(imageUrl != null ? imageUrl : "");
            
            // Extract description from meta description
            String description = extractBetween(html, "name=\"description\" content=\"", "\"");
            product.setDescription(description != null ? description : "");
            
            product.setUrl(lazadaUrl);
            product.setSource("Lazada");
            product.setBrand(""); // Brand extraction is complex for Lazada
            
            // Default to group 9 (Sản phẩm mới) for user-added products
            product.setGroupId(9);
            
            return product;
            
        } catch (Exception e) {
            System.err.println("Error scraping Lazada product: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Scrape price data from Lazada
     * Parse HTML using regex to find price (similar to Python method)
     * Returns array: [price, original_price, deal_type]
     * 
     * @param lazadaUrl The Lazada product URL
     * @return Object[] {price, original_price, deal_type} or null if failed
     */
    public static Object[] scrapePriceData(String lazadaUrl) {
        int productId = extractProductId(lazadaUrl);
        if (productId == -1) {
            return null;
        }
        
        try {
            URL url = new URL(lazadaUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Referer", "https://www.lazada.vn/");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return null;
            }
            
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            
            String html = response.toString();
            
            // Extract prices using regex (find ₫ symbol)
            Pattern pricePattern1 = Pattern.compile("₫\\s*([0-9,\\.]+)");
            Pattern pricePattern2 = Pattern.compile("([0-9,\\.]+)\\s*₫");
            
            java.util.Set<Double> allPrices = new java.util.HashSet<>();
            
            // Try pattern 1
            Matcher matcher1 = pricePattern1.matcher(html);
            while (matcher1.find()) {
                try {
                    String priceStr = matcher1.group(1).replaceAll("[,.]", "");
                    double price = Double.parseDouble(priceStr);
                    if (price > 1000 && price < 1000000000) {
                        allPrices.add(price);
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid numbers
                }
            }
            
            // Try pattern 2
            Matcher matcher2 = pricePattern2.matcher(html);
            while (matcher2.find()) {
                try {
                    String priceStr = matcher2.group(1).replaceAll("[,.]", "");
                    double price = Double.parseDouble(priceStr);
                    if (price > 1000 && price < 1000000000) {
                        allPrices.add(price);
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid numbers
                }
            }
            
            if (allPrices.isEmpty()) {
                System.err.println("Could not extract price from Lazada page");
                return null;
            }
            
            // Sort prices
            java.util.List<Double> sortedPrices = new java.util.ArrayList<>(allPrices);
            java.util.Collections.sort(sortedPrices);
            
            // Lowest price is current price, highest is original price
            double currentPrice = sortedPrices.get(0);
            double originalPrice = sortedPrices.size() > 1 ? sortedPrices.get(sortedPrices.size() - 1) : currentPrice;
            
            // Determine deal_type based on discount
            String dealType = "Normal";
            if (originalPrice > currentPrice) {
                double discount = ((originalPrice - currentPrice) / originalPrice) * 100;
                if (discount >= 50) {
                    dealType = "Flash Sale";
                } else if (discount >= 30) {
                    dealType = "Deal HOT";
                } else if (discount >= 10) {
                    dealType = "Trending";
                }
            }
            
            return new Object[]{currentPrice, originalPrice, dealType};
            
        } catch (Exception e) {
            System.err.println("Error scraping Lazada price data: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Helper method to extract text between two strings
     */
    private static String extractBetween(String text, String start, String end) {
        int startIdx = text.indexOf(start);
        if (startIdx == -1) return null;
        
        startIdx += start.length();
        int endIdx = text.indexOf(end, startIdx);
        if (endIdx == -1) return null;
        
        return text.substring(startIdx, endIdx);
    }
    
    /**
     * Map category string to group_id
     * @param category Category name from Lazada
     * @return group_id (1-8 for matched categories, 9 for "Sản phẩm mới")
     */
    public static int mapCategoryToGroupId(String category) {
        if (category == null || category.isEmpty()) {
            return 9; // Sản phẩm mới (group 9)
        }
        
        String lowerCategory = category.toLowerCase();
        
        // Check each keyword in CATEGORY_MAP
        for (Map.Entry<String, Integer> entry : CATEGORY_MAP.entrySet()) {
            if (lowerCategory.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return 9; // Default: Sản phẩm mới (group 9)
    }
}
