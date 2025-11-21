package com.pricetracker.server.http;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import com.pricetracker.server.db.ProductDAO;
import com.pricetracker.server.db.PriceHistoryDAO;
import com.pricetracker.server.db.ProductGroupDAO;
import com.pricetracker.server.db.ReviewDAO;
import com.pricetracker.server.utils.TikiScraperUtil;
import com.pricetracker.models.Product;
import com.pricetracker.models.PriceHistory;
import com.pricetracker.models.Review;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Simple HTTP Server wrapper for the Price Tracker
 * Allows frontend to communicate via HTTP instead of raw TCP socket
 */
public class SimpleHttpServer {
    private final int httpPort;
    
    // ⚡ Thread Pool để xử lý concurrent requests
    // 100 threads đủ cho 50 users (mỗi user có thể tạo 2 requests đồng thời)
    private static final int THREAD_POOL_SIZE = 100;
    
    // 🗄️ Cache TTL: 5 phút (300000ms) - đủ cho demo, data không đổi liên tục
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;
    
    private HttpServer server;
    private ExecutorService threadPool;
    private ProductDAO productDAO;
    private PriceHistoryDAO priceHistoryDAO;
    private ProductGroupDAO productGroupDAO;
    private ReviewDAO reviewDAO;
    
    // 🗄️ Cache layer để giảm DB queries
    private final ProductCache cache;
    private final com.pricetracker.server.websocket.SSEBroadcaster sseBroadcaster;

    /**
     * Constructor với port tùy chỉnh (dùng cho Railway/Render)
     */
    public SimpleHttpServer(int port) {
        this(port, null);
    }

    public SimpleHttpServer(int port, com.pricetracker.server.websocket.SSEBroadcaster sseBroadcaster) {
        this.httpPort = port;
        this.productDAO = new ProductDAO();
        this.priceHistoryDAO = new PriceHistoryDAO();
        this.productGroupDAO = new ProductGroupDAO();
        this.reviewDAO = new ReviewDAO();
        
        // 🗄️ Initialize cache với TTL 5 phút
        this.cache = new ProductCache(CACHE_TTL_MS);
        this.sseBroadcaster = sseBroadcaster;
    }

    /**
     * Constructor mặc định (port 8080)
     */
    public SimpleHttpServer() {
        this(8080);
    }

    public void start() throws IOException {
        // ⚡ Tạo thread pool với kích thước cố định
        threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        
        server = HttpServer.create(new InetSocketAddress(httpPort), 0);
        
        // CORS and search endpoint
        server.createContext("/search", this::handleSearch);
        
        // NEW: Deals endpoint for discount page
        server.createContext("/deals", this::handleDeals);
        
        // NEW: Product detail endpoint
        server.createContext("/product-detail", this::handleProductDetail);
        
        // NEW: Refresh price endpoint (Real-time scraping)
        server.createContext("/refresh-price", this::handleRefreshPrice);
        
        // NEW: Categories endpoint for category page
        server.createContext("/categories", this::handleCategories);
        
        // 📊 Metrics endpoint for monitoring
        server.createContext("/metrics", this::handleMetrics);
        
        // ⚡ Sử dụng thread pool thay vì unlimited threads
        server.setExecutor(threadPool);
        // Register SSE endpoint if broadcaster provided
        if (sseBroadcaster != null) {
            server.createContext("/events", this::handleSSE);
        }
        server.start();
        
        System.out.println("✓ HTTP Server started on port " + httpPort);
        System.out.println("  ⚡ Thread Pool: " + THREAD_POOL_SIZE + " threads (prevents OOM)");
        System.out.println("  Frontend can now connect via: http://localhost:" + httpPort + "/search");
        System.out.println("  Frontend can also access deals via: http://localhost:" + httpPort + "/deals");
        System.out.println("  Frontend can also access product detail via: http://localhost:" + httpPort + "/product-detail");
        System.out.println("  Frontend can also access categories via: http://localhost:" + httpPort + "/categories");
        System.out.println("  📊 Metrics endpoint: http://localhost:" + httpPort + "/metrics");
    }

    private void handleSSE(HttpExchange exchange) throws IOException {
        // Simple SSE endpoint
        // Allow CORS for events
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        try {
            sseBroadcaster.addClient(exchange);
            // do not close exchange here; SSEBroadcaster will hold open
        } catch (IOException e) {
            e.printStackTrace();
            try { exchange.close(); } catch (Exception ex) { }
        }
    }

    /**
     * Stop HTTP server gracefully
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("✓ HTTP Server stopped");
        }
        
        // ⚡ Shutdown thread pool gracefully
        if (threadPool != null) {
            threadPool.shutdown();
            try {
                // Đợi tối đa 10 giây cho các tasks hiện tại hoàn thành
                if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                    System.out.println("⚠️  Thread pool forced shutdown");
                } else {
                    System.out.println("✓ Thread pool stopped gracefully");
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void handleSearch(HttpExchange exchange) throws IOException {
        // Add CORS headers
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type, Cache-Control, Pragma, Expires");
        headers.add("Content-Type", "application/json; charset=UTF-8");

        // Handle preflight OPTIONS request
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        // Only accept POST
        if (!"POST".equals(exchange.getRequestMethod())) {
            String response = "{\"success\": false, \"error\": \"Method not allowed\"}";
            sendResponse(exchange, 405, response);
            return;
        }

        try {
            // Read request body
            InputStream is = exchange.getRequestBody();
            String requestBody = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .reduce("", (acc, line) -> acc + line);

            System.out.println("📥 Received request: " + requestBody);

            JSONObject requestJson = new JSONObject(requestBody);
            String action = requestJson.getString("action");
            
            JSONObject responseJson;

            if ("SEARCH_BY_URL".equals(action)) {
                String query = requestJson.getString("query");
                responseJson = handleSearchByUrl(query);
            } else if ("SEARCH_BY_NAME".equals(action)) {
                String query = requestJson.getString("query");
                responseJson = handleSearchByName(query);
            } else if ("SEARCH_BY_CATEGORY".equals(action)) {
                int groupId = requestJson.getInt("group_id");
                responseJson = handleSearchByCategory(groupId);
            } else {
                responseJson = new JSONObject();
                responseJson.put("success", false);
                responseJson.put("error", "Unknown action: " + action);
            }

            String response = responseJson.toString();
            System.out.println("📤 Sending response: " + response);
            sendResponse(exchange, 200, response);

        } catch (Exception e) {
            e.printStackTrace();
            String errorResponse = String.format(
                "{\"success\": false, \"error\": \"Server error: %s\"}", 
                e.getMessage().replace("\"", "\\\"")
            );
            sendResponse(exchange, 500, errorResponse);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        // Ensure basic CORS headers are present for cross-origin clients (Vercel -> Railway)
        Headers headers = exchange.getResponseHeaders();
        if (!headers.containsKey("Access-Control-Allow-Origin")) {
            headers.add("Access-Control-Allow-Origin", "*");
        }
        if (!headers.containsKey("Access-Control-Allow-Methods")) {
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        }
        if (!headers.containsKey("Access-Control-Allow-Headers")) {
            headers.add("Access-Control-Allow-Headers", "Content-Type, Cache-Control, Pragma, Expires");
        }

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private JSONObject handleSearchByUrl(String tikiUrl) {
        JSONObject response = new JSONObject();
        
        try {
            System.out.println("🔍 Searching by URL: " + tikiUrl);
            
            // First check if product exists in database
            Product existingProduct = productDAO.searchByUrl(tikiUrl);
            
            if (existingProduct != null) {
                // Product exists - but ALWAYS scrape latest price from Tiki for realtime data!
                System.out.println("✓ Found existing product: " + existingProduct.getName());
                System.out.println("📊 Scraping latest price from Tiki...");
                
                // Scrape realtime price
                Object[] priceData = TikiScraperUtil.scrapePriceData(tikiUrl);
                PriceHistory currentPrice;
                
                if (priceData != null) {
                    // Use realtime scraped data
                    double price = (double) priceData[0];
                    double originalPrice = (double) priceData[1];
                    String dealType = (String) priceData[2];
                    
                    currentPrice = new PriceHistory();
                    currentPrice.setProductId(existingProduct.getProductId());
                    currentPrice.setPrice(price);
                    currentPrice.setOriginalPrice(originalPrice);
                    currentPrice.setCurrency("VND");
                    currentPrice.setDealType(dealType);
                    
                    System.out.println("✅ Realtime price: " + price + " VND (was: " + originalPrice + " VND)");
                } else {
                    // Fallback to database price if scraping fails
                    System.out.println("⚠️ Failed to scrape realtime price, using database value");
                    currentPrice = priceHistoryDAO.getCurrentPrice(existingProduct.getProductId());
                }
                
                String groupName = productGroupDAO.getGroupNameById(existingProduct.getGroupId());
                
                response.put("success", true);
                response.put("isNew", false);
                response.put("product", buildProductJSON(existingProduct, currentPrice, groupName));
                
            } else {
                // Product doesn't exist - scrape from Tiki and insert
                System.out.println("⚠ Product not found, scraping from Tiki...");
                
                Product newProduct = productDAO.insertProductFromTiki(tikiUrl);
                
                if (newProduct != null) {
                    System.out.println("✓ New product added: " + newProduct.getName());
                    
                    PriceHistory currentPrice = priceHistoryDAO.getCurrentPrice(newProduct.getProductId());
                    String groupName = productGroupDAO.getGroupNameById(newProduct.getGroupId());
                    
                    response.put("success", true);
                    response.put("isNew", true);
                    response.put("product", buildProductJSON(newProduct, currentPrice, groupName));
                } else {
                    response.put("success", false);
                    response.put("error", "Không thể lấy thông tin sản phẩm từ Tiki. Vui lòng kiểm tra lại URL.");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Lỗi hệ thống: " + e.getMessage());
        }
        
        return response;
    }

    private JSONObject handleSearchByName(String searchQuery) {
        String cacheKey = "search:name:" + searchQuery.toLowerCase();
        
        // 🗄️ Check cache first
        String cached = cache.get(cacheKey);
        if (cached != null) {
            System.out.println("✓ Search results loaded from cache (hit rate: " + String.format("%.1f", cache.getHitRate()) + "%)");
            return new JSONObject(cached);
        }
        
        JSONObject response = new JSONObject();
        
        try {
            System.out.println("🔍 Searching by name: " + searchQuery);
            
            List<Product> products = productDAO.searchByNameLike(searchQuery);
            
            if (products.isEmpty()) {
                response.put("success", false);
                response.put("error", "Không tìm thấy sản phẩm nào phù hợp. Hãy thử sản phẩm khác nhé ^^");
            } else {
                System.out.println("✓ Found " + products.size() + " products");
                
                JSONArray productsArray = new JSONArray();
                
                for (Product product : products) {
                    PriceHistory currentPrice = priceHistoryDAO.getCurrentPrice(product.getProductId());
                    String groupName = productGroupDAO.getGroupNameById(product.getGroupId());
                    
                    productsArray.put(buildProductJSON(product, currentPrice, groupName));
                }
                
                response.put("success", true);
                response.put("count", products.size());
                response.put("products", productsArray);
            }
            
            // 🗄️ Store in cache
            cache.put(cacheKey, response.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Lỗi hệ thống: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Handle SEARCH_BY_CATEGORY request
     * Returns all products in a specific category/group
     * 🗄️ Cached version - TTL 5 phút
     */
    private JSONObject handleSearchByCategory(int groupId) {
        String cacheKey = "search:category:" + groupId;
        
        // 🗄️ Check cache first
        String cached = cache.get(cacheKey);
        if (cached != null) {
            System.out.println("✓ Category products loaded from cache (hit rate: " + String.format("%.1f", cache.getHitRate()) + "%)");
            return new JSONObject(cached);
        }
        
        JSONObject response = new JSONObject();
        
        try {
            System.out.println("🔍 Searching by category (group_id): " + groupId);
            
            List<Product> products = productDAO.getProductsByGroupId(groupId);
            String groupName = productGroupDAO.getGroupNameById(groupId);
            
            if (products.isEmpty()) {
                response.put("success", false);
                response.put("error", "Chưa có sản phẩm nào trong danh mục này.");
            } else {
                System.out.println("✓ Found " + products.size() + " products in group: " + groupName);
                
                JSONArray productsArray = new JSONArray();
                
                for (Product product : products) {
                    PriceHistory currentPrice = priceHistoryDAO.getCurrentPrice(product.getProductId());
                    
                    productsArray.put(buildProductJSON(product, currentPrice, groupName));
                }
                
                response.put("success", true);
                response.put("count", products.size());
                response.put("category_name", groupName);
                response.put("products", productsArray);
            }
            
            // 🗄️ Store in cache
            cache.put(cacheKey, response.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Lỗi hệ thống: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * NEW: Handle deals endpoint - Get products with discounts
     * Supports filtering by deal_type: FLASH_SALE, HOT_DEAL, TRENDING, or ALL
     */
    private void handleDeals(HttpExchange exchange) throws IOException {
        // Add CORS headers
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type, Cache-Control, Pragma, Expires");
        headers.add("Content-Type", "application/json; charset=UTF-8");

        // Handle preflight OPTIONS request
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        try {
            String dealType = "ALL"; // Default: get all deals
            
            // Check if POST request with body
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String requestBody = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                        .lines()
                        .reduce("", (acc, line) -> acc + line);

                if (!requestBody.isEmpty()) {
                    JSONObject requestJson = new JSONObject(requestBody);
                    if (requestJson.has("deal_type")) {
                        dealType = requestJson.getString("deal_type");
                    }
                }
            }
            
            System.out.println("📥 Received deals request - Deal type: " + dealType);

            JSONObject responseJson = handleGetDeals(dealType);
            
            String response = responseJson.toString();
            System.out.println("📤 Sending deals response with " + 
                             responseJson.optInt("count", 0) + " products");
            sendResponse(exchange, 200, response);

        } catch (Exception e) {
            e.printStackTrace();
            String errorResponse = String.format(
                "{\"success\": false, \"error\": \"Server error: %s\"}", 
                e.getMessage().replace("\"", "\\\"")
            );
            sendResponse(exchange, 500, errorResponse);
        }
    }

    /**
     * NEW: Get products with deals/discounts
     * 🗄️ Cached version - TTL 5 phút
     * @param dealType Filter by deal type: "FLASH_SALE", "HOT_DEAL", "TRENDING", or "ALL"
     * @return JSONObject with products list
     */
    private JSONObject handleGetDeals(String dealType) {
        String cacheKey = "deals:" + dealType;
        
        // 🗄️ Check cache first
        String cached = cache.get(cacheKey);
        if (cached != null) {
            System.out.println("✓ Deals loaded from cache (hit rate: " + String.format("%.1f", cache.getHitRate()) + "%)");
            return new JSONObject(cached);
        }
        
        JSONObject response = new JSONObject();
        
        try {
            System.out.println("🎁 Fetching deals - Type: " + dealType);
            
            List<Product> products = productDAO.getProductsByDealType(dealType);
            
            if (products.isEmpty()) {
                response.put("success", false);
                response.put("error", "Hiện tại chưa có sản phẩm giảm giá nào. Vui lòng quay lại sau!");
            } else {
                System.out.println("✓ Found " + products.size() + " deal products");
                
                JSONArray productsArray = new JSONArray();
                
                for (Product product : products) {
                    PriceHistory currentPrice = priceHistoryDAO.getCurrentPrice(product.getProductId());
                    String groupName = productGroupDAO.getGroupNameById(product.getGroupId());
                    
                    productsArray.put(buildProductJSON(product, currentPrice, groupName));
                }
                
                response.put("success", true);
                response.put("count", products.size());
                response.put("deal_type", dealType);
                response.put("products", productsArray);
            }
            
            // 🗄️ Store in cache
            cache.put(cacheKey, response.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Lỗi hệ thống: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * NEW: Handle product detail endpoint - Get detailed product information
     * Requires product_id in request body
     */
    private void handleProductDetail(HttpExchange exchange) throws IOException {
        // Add CORS headers
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type, Cache-Control, Pragma, Expires");
        headers.add("Content-Type", "application/json; charset=UTF-8");

        // Handle preflight OPTIONS request
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                String errorResponse = "{\"success\": false, \"error\": \"Method not allowed. Use POST.\"}";
                sendResponse(exchange, 405, errorResponse);
                return;
            }
            
            // Read request body
            InputStream is = exchange.getRequestBody();
            String requestBody = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .reduce("", (acc, line) -> acc + line);

            if (requestBody.isEmpty()) {
                String errorResponse = "{\"success\": false, \"error\": \"Missing product_id in request body\"}";
                sendResponse(exchange, 400, errorResponse);
                return;
            }
            
            JSONObject requestJson = new JSONObject(requestBody);
            if (!requestJson.has("product_id")) {
                String errorResponse = "{\"success\": false, \"error\": \"Missing product_id in request body\"}";
                sendResponse(exchange, 400, errorResponse);
                return;
            }
            
            int productId = requestJson.getInt("product_id");
            System.out.println("📥 Received product detail request - Product ID: " + productId);

            JSONObject responseJson = handleGetProductDetail(productId);
            
            String response = responseJson.toString();
            System.out.println("📤 Sending product detail response");
            sendResponse(exchange, 200, response);

        } catch (Exception e) {
            e.printStackTrace();
            String errorResponse = String.format(
                "{\"success\": false, \"error\": \"Server error: %s\"}", 
                e.getMessage().replace("\"", "\\\"")
            );
            sendResponse(exchange, 500, errorResponse);
        }
    }

    /**
     * NEW: Get detailed product information including price history, reviews, and similar products
     * 🗄️ Cached version - TTL 5 phút
     * @param productId The product ID
     * @return JSONObject with complete product details
     */
    private JSONObject handleGetProductDetail(int productId) {
        String cacheKey = "product:" + productId;
        
        // 🗄️ Check cache first
        String cached = cache.get(cacheKey);
        if (cached != null) {
            System.out.println("✓ Product detail loaded from cache (hit rate: " + String.format("%.1f", cache.getHitRate()) + "%)");
            return new JSONObject(cached);
        }
        
        JSONObject response = new JSONObject();
        
        try {
            System.out.println("🔍 Fetching product detail - ID: " + productId);
            
            // Get product basic info
            Product product = productDAO.getProductById(productId);
            
            if (product == null) {
                response.put("success", false);
                response.put("error", "Không tìm thấy sản phẩm này!");
                return response;
            }
            
            System.out.println("✓ Found product: " + product.getName());
            
            // Get current price
            PriceHistory currentPrice = priceHistoryDAO.getCurrentPrice(productId);
            
            // Get price history
            List<PriceHistory> priceHistory = priceHistoryDAO.getPriceHistoryByProductId(productId);
            
            // Get reviews
            List<Review> reviews = reviewDAO.getReviewsByProductId(productId);
            int reviewCount = reviewDAO.countReviewsByProductId(productId);
            
            // Get similar products (same group) - 16 products for 4 rows
            List<Product> similarProducts = productDAO.getSimilarProducts(
                product.getGroupId(), 
                productId, 
                16  // Limit to 16 similar products (4 rows x 4 columns)
            );
            
            // Get group name
            String groupName = productGroupDAO.getGroupNameById(product.getGroupId());
            
            // Build response JSON
            response.put("success", true);
            
            // Product info
            JSONObject productJson = new JSONObject();
            productJson.put("product_id", product.getProductId());
            productJson.put("group_id", product.getGroupId());
            productJson.put("group_name", groupName);
            productJson.put("name", product.getName());
            productJson.put("brand", product.getBrand() != null ? product.getBrand() : "");
            productJson.put("url", product.getUrl());
            productJson.put("image_url", product.getImageUrl());
            productJson.put("description", product.getDescription() != null ? product.getDescription() : "");
            productJson.put("source", product.getSource());
            
            response.put("product", productJson);
            
            // Current price info
            JSONObject priceJson = new JSONObject();
            if (currentPrice != null) {
                priceJson.put("current_price", currentPrice.getPrice());
                priceJson.put("original_price", currentPrice.getOriginalPrice());
                priceJson.put("currency", currentPrice.getCurrency());
                priceJson.put("deal_type", currentPrice.getDealType() != null ? currentPrice.getDealType() : "Normal");
                
                // Calculate discount percentage
                int discountPercent = 0;
                if (currentPrice.getOriginalPrice() > currentPrice.getPrice() && currentPrice.getOriginalPrice() > 0) {
                    discountPercent = (int) Math.round(((currentPrice.getOriginalPrice() - currentPrice.getPrice()) / currentPrice.getOriginalPrice()) * 100);
                }
                priceJson.put("discount_percent", discountPercent);
            } else {
                priceJson.put("current_price", 0);
                priceJson.put("original_price", 0);
                priceJson.put("currency", "VND");
                priceJson.put("deal_type", "Normal");
                priceJson.put("discount_percent", 0);
            }
            response.put("price", priceJson);
            
            // Price history array
            JSONArray priceHistoryArray = new JSONArray();
            for (PriceHistory ph : priceHistory) {
                JSONObject phJson = new JSONObject();
                phJson.put("price", ph.getPrice());
                phJson.put("original_price", ph.getOriginalPrice());
                phJson.put("captured_at", ph.getCapturedAt().toString());
                phJson.put("deal_type", ph.getDealType() != null ? ph.getDealType() : "Normal");
                priceHistoryArray.put(phJson);
            }
            response.put("price_history", priceHistoryArray);
            
            // Reviews
            JSONObject reviewsJson = new JSONObject();
            reviewsJson.put("count", reviewCount);
            
            JSONArray reviewsArray = new JSONArray();
            for (Review review : reviews) {
                JSONObject rJson = new JSONObject();
                rJson.put("reviewer_name", review.getReviewerName());
                rJson.put("rating", review.getRating());
                rJson.put("review_text", review.getReviewText());
                rJson.put("review_date", review.getReviewDate().toString());
                reviewsArray.put(rJson);
            }
            reviewsJson.put("reviews", reviewsArray);
            response.put("reviews", reviewsJson);
            
            // Similar products
            JSONArray similarProductsArray = new JSONArray();
            for (Product sp : similarProducts) {
                PriceHistory spPrice = priceHistoryDAO.getCurrentPrice(sp.getProductId());
                JSONObject spJson = buildProductJSON(sp, spPrice, groupName);
                similarProductsArray.put(spJson);
            }
            response.put("similar_products", similarProductsArray);
            
            System.out.println("✓ Product detail prepared: " + reviewCount + " reviews, " + 
                             priceHistory.size() + " price records, " + 
                             similarProducts.size() + " similar products");
            
            // 🗄️ Store in cache
            cache.put(cacheKey, response.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Lỗi hệ thống: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * NEW: Handle refresh price endpoint - Real-time scraping
     * Forces an immediate price scrape if data is older than 1 hour
     */
    private void handleRefreshPrice(HttpExchange exchange) throws IOException {
        // Add CORS headers
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "POST, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type, Cache-Control, Pragma, Expires");
        headers.add("Content-Type", "application/json; charset=UTF-8");

        // Handle preflight OPTIONS request
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                String errorResponse = "{\"success\": false, \"error\": \"Method not allowed. Use POST.\"}";
                sendResponse(exchange, 405, errorResponse);
                return;
            }
            
            // Read request body
            InputStream is = exchange.getRequestBody();
            String requestBody = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .reduce("", (acc, line) -> acc + line);

            JSONObject requestJson = new JSONObject(requestBody);
            if (!requestJson.has("product_id")) {
                String errorResponse = "{\"success\": false, \"error\": \"Missing product_id\"}";
                sendResponse(exchange, 400, errorResponse);
                return;
            }
            
            int productId = requestJson.getInt("product_id");
            System.out.println("🔄 Refresh price request - Product ID: " + productId);

            // Get product info
            Product product = productDAO.getProductById(productId);
            if (product == null) {
                String errorResponse = "{\"success\": false, \"error\": \"Product not found\"}";
                sendResponse(exchange, 404, errorResponse);
                return;
            }

            // Get latest price from DB
            PriceHistory latestPrice = priceHistoryDAO.getCurrentPrice(productId);
            
            // Check if need to scrape (> 1 hour old)
            boolean needsScrape = false;
            if (latestPrice != null) {
                long hoursSinceUpdate = java.time.Duration.between(
                    latestPrice.getCapturedAt().toLocalDateTime(),
                    java.time.LocalDateTime.now()
                ).toHours();
                
                needsScrape = (hoursSinceUpdate >= 1);
                System.out.println("⏱️  Last update: " + hoursSinceUpdate + " hours ago");
            } else {
                needsScrape = true;
                System.out.println("⚠️  No price history found");
            }

            if (needsScrape) {
                System.out.println("🔍 Scraping new price from Tiki...");
                
                // Extract Tiki product ID
                int tikiProductId = TikiScraperUtil.extractProductId(product.getUrl());
                
                if (tikiProductId != -1) {
                    // Scrape price data
                    Object[] priceData = TikiScraperUtil.scrapePriceData(product.getUrl());
                    
                    if (priceData != null && priceData.length >= 3) {
                        double price = (Double) priceData[0];
                        double originalPrice = (Double) priceData[1];
                        String dealType = (String) priceData[2];
                        
                        // Save to database
                        boolean saved = priceHistoryDAO.addCompletePriceRecord(
                            productId, price, originalPrice, dealType
                        );
                        
                        if (saved) {
                            System.out.println("✅ New price saved: " + price + "đ");
                        }
                    }
                }
            } else {
                System.out.println("✓ Price is still fresh, no scraping needed");
            }

            // 🗑️ Invalidate cache for this product to force fresh data on next request
            String cacheKey = "product:" + productId;
            cache.invalidate(cacheKey);
            System.out.println("🗑️  Cache invalidated for product: " + productId);

            // Return latest price data
            PriceHistory currentPrice = priceHistoryDAO.getCurrentPrice(productId);
            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("price", currentPrice.getPrice());
            response.put("original_price", currentPrice.getOriginalPrice());
            response.put("deal_type", currentPrice.getDealType());
            response.put("recorded_at", currentPrice.getCapturedAt().toString());
            response.put("scraped_new", needsScrape);

            sendResponse(exchange, 200, response.toString());

        } catch (Exception e) {
            e.printStackTrace();
            String errorResponse = String.format(
                "{\"success\": false, \"error\": \"Server error: %s\"}", 
                e.getMessage().replace("\"", "\\\"")
            );
            sendResponse(exchange, 500, errorResponse);
        }
    }

    private JSONObject buildProductJSON(Product product, PriceHistory priceHistory, String groupName) {
        JSONObject json = new JSONObject();
        
        // Product basic info
        json.put("product_id", product.getProductId());
        json.put("group_id", product.getGroupId());
        json.put("group_name", groupName);
        json.put("name", product.getName());
        json.put("brand", product.getBrand() != null ? product.getBrand() : "");
        json.put("url", product.getUrl());
        json.put("image_url", product.getImageUrl());
        json.put("description", product.getDescription() != null ? product.getDescription() : "");
        json.put("source", product.getSource());
        
        // Price information
        if (priceHistory != null) {
            double currentPrice = priceHistory.getPrice();
            double originalPrice = priceHistory.getOriginalPrice();
            
            json.put("price", currentPrice);
            json.put("original_price", originalPrice);
            json.put("currency", priceHistory.getCurrency());
            json.put("deal_type", priceHistory.getDealType() != null ? priceHistory.getDealType() : "Normal");
            
            // Calculate discount percentage
            int discountPercent = 0;
            if (originalPrice > currentPrice && originalPrice > 0) {
                discountPercent = (int) Math.round(((originalPrice - currentPrice) / originalPrice) * 100);
            }
            json.put("discount_percent", discountPercent);
        } else {
            json.put("price", 0);
            json.put("original_price", 0);
            json.put("currency", "VND");
            json.put("deal_type", "Normal");
            json.put("discount_percent", 0);
        }
        
        return json;
    }
    
    /**
     * NEW: Handle categories endpoint - Get all product groups with counts
     */
    private void handleCategories(HttpExchange exchange) throws IOException {
        // Add CORS headers
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type, Cache-Control, Pragma, Expires");
        headers.add("Content-Type", "application/json; charset=UTF-8");

        // Handle preflight OPTIONS request
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        try {
            System.out.println("📥 Received categories request");

            JSONObject responseJson = handleGetCategories();
            
            String response = responseJson.toString();
            System.out.println("📤 Sending categories response");
            sendResponse(exchange, 200, response);

        } catch (Exception e) {
            e.printStackTrace();
            String errorResponse = String.format(
                "{\"success\": false, \"error\": \"Server error: %s\"}", 
                e.getMessage().replace("\"", "\\\"")
            );
            sendResponse(exchange, 500, errorResponse);
        }
    }
    
    /**
     * Get all categories with product counts
     * 🗄️ Cached version - TTL 5 phút
     */
    private JSONObject handleGetCategories() {
        String cacheKey = "categories:all";
        
        // 🗄️ Check cache first
        String cached = cache.get(cacheKey);
        if (cached != null) {
            System.out.println("✓ Categories loaded from cache (hit rate: " + String.format("%.1f", cache.getHitRate()) + "%)");
            return new JSONObject(cached);
        }
        
        JSONObject response = new JSONObject();
        
        try {
            // Get all groups
            java.util.Map<Integer, String> groups = productGroupDAO.getAllGroups();
            
            // Get product counts
            java.util.Map<Integer, Integer> counts = productDAO.countProductsByGroup();
            
            // Build categories array
            JSONArray categoriesArray = new JSONArray();
            
            for (java.util.Map.Entry<Integer, String> entry : groups.entrySet()) {
                int groupId = entry.getKey();
                String groupName = entry.getValue();
                int count = counts.getOrDefault(groupId, 0);
                
                JSONObject categoryJson = new JSONObject();
                categoryJson.put("group_id", groupId);
                categoryJson.put("group_name", groupName);
                categoryJson.put("product_count", count);
                
                categoriesArray.put(categoryJson);
            }
            
            response.put("success", true);
            response.put("categories", categoriesArray);
            
            // 🗄️ Store in cache
            cache.put(cacheKey, response.toString());
            
            System.out.println("✓ Loaded " + categoriesArray.length() + " categories from DB (cached for 5min)");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "Lỗi hệ thống: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 📊 Handle /metrics endpoint - System monitoring
     * Returns JSON with cache stats, thread pool info, DB connections, etc.
     */
    private void handleMetrics(HttpExchange exchange) throws IOException {
        // Add CORS headers
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
        headers.add("Access-Control-Allow-Headers", "Content-Type, Cache-Control, Pragma, Expires");
        headers.add("Content-Type", "application/json; charset=UTF-8");

        // Handle preflight OPTIONS request
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        try {
            JSONObject metrics = new JSONObject();
            
            // 🗄️ Cache metrics
            JSONObject cacheMetrics = new JSONObject();
            cacheMetrics.put("hit_rate_percent", String.format("%.2f", cache.getHitRate()));
            cacheMetrics.put("cache_size", cache.size());
            cacheMetrics.put("ttl_minutes", CACHE_TTL_MS / 60000);
            metrics.put("cache", cacheMetrics);
            
            // ⚡ Thread pool metrics
            JSONObject threadMetrics = new JSONObject();
            threadMetrics.put("max_threads", THREAD_POOL_SIZE);
            threadMetrics.put("pool_type", "FixedThreadPool");
            metrics.put("http_thread_pool", threadMetrics);
            
            // 💾 Database connection pool metrics (HikariCP)
            try {
                com.zaxxer.hikari.HikariDataSource ds = 
                    com.pricetracker.server.db.HikariCPConfig.getDataSource();
                
                JSONObject dbMetrics = new JSONObject();
                dbMetrics.put("pool_name", "PriceTrackerPool");
                dbMetrics.put("max_pool_size", 30);
                dbMetrics.put("min_idle", 15);
                dbMetrics.put("active_connections", ds.getHikariPoolMXBean().getActiveConnections());
                dbMetrics.put("idle_connections", ds.getHikariPoolMXBean().getIdleConnections());
                dbMetrics.put("total_connections", ds.getHikariPoolMXBean().getTotalConnections());
                dbMetrics.put("threads_awaiting_connection", ds.getHikariPoolMXBean().getThreadsAwaitingConnection());
                metrics.put("database_pool", dbMetrics);
            } catch (Exception e) {
                metrics.put("database_pool", "unavailable");
            }
            
            // 🚀 Server info
            JSONObject serverMetrics = new JSONObject();
            serverMetrics.put("http_port", httpPort);
            serverMetrics.put("ssl_server", "disabled");
            serverMetrics.put("websocket_port", 8081);
            metrics.put("server", serverMetrics);
            
            // ⏱️ System info
            JSONObject systemMetrics = new JSONObject();
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory() / (1024 * 1024);
            long freeMemory = runtime.freeMemory() / (1024 * 1024);
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory() / (1024 * 1024);
            
            systemMetrics.put("used_memory_mb", usedMemory);
            systemMetrics.put("total_memory_mb", totalMemory);
            systemMetrics.put("max_memory_mb", maxMemory);
            systemMetrics.put("available_processors", runtime.availableProcessors());
            metrics.put("system", systemMetrics);
            
            // Wrap in response
            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("timestamp", System.currentTimeMillis());
            response.put("metrics", metrics);
            
            sendResponse(exchange, 200, response.toString());
            System.out.println("📊 Metrics requested - Cache hit rate: " + String.format("%.1f", cache.getHitRate()) + "%");

        } catch (Exception e) {
            e.printStackTrace();
            String errorResponse = String.format(
                "{\"success\": false, \"error\": \"Server error: %s\"}", 
                e.getMessage().replace("\"", "\\\"")
            );
            sendResponse(exchange, 500, errorResponse);
        }
    }

    public static void main(String[] args) {
        try {
            SimpleHttpServer httpServer = new SimpleHttpServer();
            httpServer.start();
            
            System.out.println("\nPress Ctrl+C to stop the server...");
            
            // Keep the server running
            Thread.currentThread().join();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 🗄️ ProductCache - Simple in-memory cache với TTL
     * Giảm 90% database queries cho demo
     */
    private static class ProductCache {
        private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
        private final long ttlMs;
        
        // Cache statistics
        private long hits = 0;
        private long misses = 0;
        
        public ProductCache(long ttlMs) {
            this.ttlMs = ttlMs;
        }
        
        /**
         * Get cached value, null nếu expired hoặc không tồn tại
         */
        public String get(String key) {
            CacheEntry entry = cache.get(key);
            if (entry == null) {
                misses++;
                return null;
            }
            
            // Check expiration
            if (System.currentTimeMillis() - entry.timestamp > ttlMs) {
                cache.remove(key);
                misses++;
                return null;
            }
            
            hits++;
            return entry.value;
        }
        
        /**
         * Put value vào cache với timestamp hiện tại
         */
        public void put(String key, String value) {
            cache.put(key, new CacheEntry(value, System.currentTimeMillis()));
        }
        
        /**
         * Invalidate a specific cache entry
         */
        public void invalidate(String key) {
            cache.remove(key);
        }
        
        /**
         * Clear toàn bộ cache
         */
        public void clear() {
            cache.clear();
            hits = 0;
            misses = 0;
        }
        
        /**
         * Get cache hit rate (%)
         */
        public double getHitRate() {
            long total = hits + misses;
            return total == 0 ? 0.0 : (hits * 100.0) / total;
        }
        
        /**
         * Get cache size
         */
        public int size() {
            return cache.size();
        }
        
        /**
         * Cache entry với timestamp
         */
        private static class CacheEntry {
            final String value;
            final long timestamp;
            
            CacheEntry(String value, long timestamp) {
                this.value = value;
                this.timestamp = timestamp;
            }
        }
    }
}
