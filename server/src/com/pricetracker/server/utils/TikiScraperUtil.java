package com.pricetracker.server.utils;

import com.pricetracker.models.Product;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for scraping Tiki product data in real-time
 * Used when user searches for a new product not in database
 */
public class TikiScraperUtil {
    
    private static final String TIKI_API_BASE = "https://tiki.vn/api/v2/products/";
    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile("p(\\d+)\\.html");
    
    // Category mapping to group_id (based on 8 groups in database)
    private static final Map<String, Integer> CATEGORY_MAP = new HashMap<>();
    
    static {
        // Group 1: Điện tử
        CATEGORY_MAP.put("điện thoại", 1);
        CATEGORY_MAP.put("smartphone", 1);
        CATEGORY_MAP.put("laptop", 1);
        CATEGORY_MAP.put("máy tính", 1);
        CATEGORY_MAP.put("tablet", 1);
        CATEGORY_MAP.put("ipad", 1);
        CATEGORY_MAP.put("điện tử", 1);
        
        // Group 2: Điện gia dụng
        CATEGORY_MAP.put("điện gia dụng", 2);
        CATEGORY_MAP.put("tủ lạnh", 2);
        CATEGORY_MAP.put("máy giặt", 2);
        CATEGORY_MAP.put("lò vi sóng", 2);
        CATEGORY_MAP.put("quạt", 2);
        CATEGORY_MAP.put("nồi cơm", 2);
        
        // Group 3: Thời trang
        CATEGORY_MAP.put("thời trang", 3);
        CATEGORY_MAP.put("quần áo", 3);
        CATEGORY_MAP.put("giày", 3);
        CATEGORY_MAP.put("túi xách", 3);
        CATEGORY_MAP.put("phụ kiện thời trang", 3);
        
        // Group 4: Làm đẹp
        CATEGORY_MAP.put("làm đẹp", 4);
        CATEGORY_MAP.put("mỹ phẩm", 4);
        CATEGORY_MAP.put("chăm sóc da", 4);
        CATEGORY_MAP.put("nước hoa", 4);
        CATEGORY_MAP.put("son", 4);
        
        // Group 5: Sách
        CATEGORY_MAP.put("sách", 5);
        CATEGORY_MAP.put("nhà sách", 5);
        CATEGORY_MAP.put("văn học", 5);
        CATEGORY_MAP.put("tiểu thuyết", 5);
        
        // Group 6: Đồ chơi
        CATEGORY_MAP.put("đồ chơi", 6);
        CATEGORY_MAP.put("mẹ và bé", 6);
        CATEGORY_MAP.put("đồ chơi trẻ em", 6);
        
        // Group 7: Thể thao
        CATEGORY_MAP.put("thể thao", 7);
        CATEGORY_MAP.put("dụng cụ thể thao", 7);
        CATEGORY_MAP.put("gym", 7);
        CATEGORY_MAP.put("yoga", 7);
        
        // Group 8: Sản phẩm mới (default)
    }
    
    /**
     * Extract product ID from Tiki URL
     * @param tikiUrl URL like https://tiki.vn/...p12345.html
     * @return Product ID or -1 if invalid
     */
    public static int extractProductId(String tikiUrl) {
        Matcher matcher = PRODUCT_ID_PATTERN.matcher(tikiUrl);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return -1;
    }
    
    /**
     * Scrape product data from Tiki API
     * @param tikiUrl The Tiki product URL
     * @return Product object with scraped data, or null if failed
     */
    public static Product scrapeProductFromUrl(String tikiUrl) {
        int productId = extractProductId(tikiUrl);
        if (productId == -1) {
            System.err.println("Invalid Tiki URL: " + tikiUrl);
            return null;
        }
        
        try {
            String apiUrl = TIKI_API_BASE + productId;
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(10000); // 10 seconds timeout
            conn.setReadTimeout(10000);    // 10 seconds timeout
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Tiki API returned code: " + responseCode);
                return null;
            }
            
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            
            // Parse JSON response
            JSONObject json = new JSONObject(response.toString());
            
            Product product = new Product();
            product.setName(json.optString("name", "Unknown Product"));
            product.setBrand(json.optString("brand_name", ""));
            product.setUrl(tikiUrl);
            product.setImageUrl(json.optString("thumbnail_url", ""));
            product.setDescription(json.optString("short_description", ""));
            product.setSource("Tiki");
            
            // Map category to group_id
            String category = extractCategory(json);
            product.setGroupId(mapCategoryToGroupId(category));
            
            return product;
            
        } catch (Exception e) {
            System.err.println("Error scraping Tiki product: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Scrape price data from Tiki API
     * Returns array: [price, original_price, deal_type]
     * @param tikiUrl The Tiki product URL
     * @return double[] {price, original_price} and String deal_type
     */
    public static Object[] scrapePriceData(String tikiUrl) {
        int productId = extractProductId(tikiUrl);
        if (productId == -1) {
            return null;
        }
        
        try {
            String apiUrl = TIKI_API_BASE + productId;
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(10000); // 10 seconds timeout
            conn.setReadTimeout(10000);    // 10 seconds timeout
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return null;
            }
            
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            
            JSONObject json = new JSONObject(response.toString());
            
            double price = json.optDouble("price", 0.0);
            double originalPrice = json.optDouble("original_price", price);
            String dealType = extractDealType(json);
            
            return new Object[]{price, originalPrice, dealType};
            
        } catch (Exception e) {
            System.err.println("Error scraping price data: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract category from Tiki JSON response
     */
    private static String extractCategory(JSONObject json) {
        try {
            // Try to get from breadcrumbs or categories
            if (json.has("breadcrumbs")) {
                JSONArray breadcrumbs = json.getJSONArray("breadcrumbs");
                if (breadcrumbs.length() > 0) {
                    JSONObject firstCategory = breadcrumbs.getJSONObject(0);
                    return firstCategory.optString("name", "").toLowerCase();
                }
            }
            
            // Fallback to category field
            if (json.has("category")) {
                JSONObject category = json.getJSONObject("category");
                return category.optString("name", "").toLowerCase();
            }
            
        } catch (Exception e) {
            // Ignore parsing errors
        }
        
        return "";
    }
    
    /**
     * Extract deal type from badges
     */
    private static String extractDealType(JSONObject json) {
        try {
            if (json.has("badges")) {
                JSONArray badges = json.getJSONArray("badges");
                for (int i = 0; i < badges.length(); i++) {
                    JSONObject badge = badges.getJSONObject(i);
                    String code = badge.optString("code", "");
                    if (code.equals("flash_sale")) {
                        return "Flash Sale";
                    } else if (code.equals("deal_1")) {
                        return "Deal HOT";
                    } else if (code.equals("freeship")) {
                        return "Freeship";
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        
        return "Normal";
    }
    
    /**
     * Map category string to group_id
     * @param category Category name from Tiki
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
