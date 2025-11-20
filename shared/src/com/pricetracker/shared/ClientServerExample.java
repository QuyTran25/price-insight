package com.pricetracker.shared;

import com.pricetracker.models.*;
import java.util.*;

/**
 * ClientServerExample - Ví dụ minh họa cách sử dụng module nhập xuất object
 * trong giao tiếp Client-Server
 */
public class ClientServerExample {

    public static void main(String[] args) {
        System.out.println("=".repeat(70));
        System.out.println("    VÍ DỤ SỬ DỤNG MODULE TRONG CLIENT-SERVER");
        System.out.println("=".repeat(70));
        System.out.println();

        demonstrateClientSide();
        System.out.println();
        demonstrateServerSide();
    }

    /**
     * PHÍA CLIENT: Tạo và gửi request
     */
    private static void demonstrateClientSide() {
        System.out.println("👤 CLIENT SIDE - Tạo và gửi Request");
        System.out.println("-".repeat(70));

        try {
            // 1. Tạo request tìm kiếm sản phẩm
            System.out.println("\n[1] Tạo Request - SEARCH_PRODUCT");
            Request searchRequest = new Request(Request.Action.SEARCH_PRODUCT, "client-001");
            searchRequest.addParameter("query", "iPhone 15 Pro Max");
            searchRequest.addParameter("limit", 10);
            System.out.println("   Request: " + searchRequest);

            // 2. Serialize request
            byte[] requestData = SerializationUtil.serialize(searchRequest);
            System.out.println("   Serialized size: " + requestData.length + " bytes");
            System.out.println("   → Ready to send via socket/stream");

            // ===== GIẢ LẬP GỬI QUA MẠNG =====
            // outputStream.write(requestData);
            // outputStream.flush();

            System.out.println("\n[2] Tạo Request - GET_PRODUCT_DETAIL");
            Request detailRequest = new Request(Request.Action.GET_PRODUCT_DETAIL, "client-001");
            detailRequest.addParameter("product_id", 123);
            System.out.println("   Request: " + detailRequest);
            
            byte[] detailData = SerializationUtil.serialize(detailRequest);
            System.out.println("   Serialized size: " + detailData.length + " bytes");

            System.out.println("\n[3] Tạo Request - GET_PRICE_HISTORY");
            Request historyRequest = new Request(Request.Action.GET_PRICE_HISTORY, "client-001");
            historyRequest.addParameter("product_id", 123);
            historyRequest.addParameter("days", 30);
            System.out.println("   Request: " + historyRequest);

            System.out.println("\n✅ Client đã tạo và serialize 3 requests thành công!");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * PHÍA SERVER: Nhận request, xử lý và gửi response
     */
    private static void demonstrateServerSide() {
        System.out.println("🖥️  SERVER SIDE - Nhận Request và gửi Response");
        System.out.println("-".repeat(70));

        try {
            // ===== GIẢ LẬP NHẬN REQUEST TỪ CLIENT =====
            
            // 1. Giả sử server nhận được request SEARCH_PRODUCT
            System.out.println("\n[1] Server nhận Request - SEARCH_PRODUCT");
            Request receivedRequest = createMockSearchRequest();
            byte[] requestData = SerializationUtil.serialize(receivedRequest);
            
            // Server deserialize request
            Request request = SerializationUtil.deserialize(requestData, Request.class);
            System.out.println("   Received: " + request);
            
            // Xử lý request
            if (request.getAction() == Request.Action.SEARCH_PRODUCT) {
                String query = request.getStringParameter("query");
                Integer limit = request.getIntParameter("limit");
                
                System.out.println("   Processing search: query=\"" + query + "\", limit=" + limit);
                
                // Giả lập tìm kiếm trong database
                List<Product> products = createMockProducts();
                
                // Tạo response thành công
                Response response = Response.success("Tìm thấy " + products.size() + " sản phẩm", products);
                System.out.println("   Response: " + response);
                
                // Serialize response để gửi về client
                byte[] responseData = SerializationUtil.serialize(response);
                System.out.println("   Serialized response size: " + responseData.length + " bytes");
                System.out.println("   → Ready to send back to client");
                
                // ===== CLIENT NHẬN RESPONSE =====
                System.out.println("\n   [Client nhận response]");
                Response clientResponse = SerializationUtil.deserialize(responseData, Response.class);
                System.out.println("   Status: " + clientResponse.getStatus());
                System.out.println("   Message: " + clientResponse.getMessage());
                
                @SuppressWarnings("unchecked")
                List<Product> receivedProducts = (List<Product>) clientResponse.getData();
                System.out.println("   Products received: " + receivedProducts.size());
                for (Product p : receivedProducts) {
                    System.out.println("      - " + p.getName());
                }
            }

            // 2. Giả sử server nhận request GET_PRODUCT_DETAIL
            System.out.println("\n[2] Server nhận Request - GET_PRODUCT_DETAIL");
            Request detailRequest = new Request(Request.Action.GET_PRODUCT_DETAIL, "client-001");
            detailRequest.addParameter("product_id", 1);
            
            byte[] detailRequestData = SerializationUtil.serialize(detailRequest);
            Request receivedDetailRequest = SerializationUtil.deserialize(detailRequestData, Request.class);
            
            System.out.println("   Received: " + receivedDetailRequest);
            
            Integer productId = receivedDetailRequest.getIntParameter("product_id");
            System.out.println("   Processing: Get detail for product ID " + productId);
            
            // Giả lập lấy dữ liệu từ database
            ProductData productData = createMockProductData();
            
            // Tạo response với ProductData
            Response detailResponse = Response.success("Chi tiết sản phẩm", productData);
            byte[] detailResponseData = SerializationUtil.serialize(detailResponse);
            
            System.out.println("   Response created with ProductData");
            System.out.println("   Serialized size: " + detailResponseData.length + " bytes");
            
            // Client nhận và xử lý
            System.out.println("\n   [Client nhận ProductData]");
            Response clientDetailResponse = SerializationUtil.deserialize(detailResponseData, Response.class);
            ProductData receivedProductData = clientDetailResponse.getData(ProductData.class);
            
            System.out.println("   Product: " + receivedProductData.getProduct().getName());
            System.out.println("   Price History: " + receivedProductData.getPriceHistory().size() + " records");
            System.out.println("   Current Price: " + receivedProductData.getCurrentPrice().getPrice() + " VND");
            System.out.println("   Reviews: " + receivedProductData.getReviews().size() + " reviews");
            System.out.println("   Average Rating: " + receivedProductData.getAverageRating() + "/5");

            // 3. Ví dụ lỗi - Product not found
            System.out.println("\n[3] Server xử lý lỗi - Product Not Found");
            Response errorResponse = Response.notFound("Không tìm thấy sản phẩm với ID 999");
            byte[] errorData = SerializationUtil.serialize(errorResponse);
            
            Response clientErrorResponse = SerializationUtil.deserialize(errorData, Response.class);
            System.out.println("   Client nhận: " + clientErrorResponse.getStatus());
            System.out.println("   Message: " + clientErrorResponse.getMessage());
            
            if (clientErrorResponse.getStatus() == Response.Status.NOT_FOUND) {
                System.out.println("   → Client hiển thị thông báo lỗi cho user");
            }

            System.out.println("\n✅ Server đã xử lý và gửi 3 responses thành công!");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===== HELPER METHODS - Tạo mock data =====

    private static Request createMockSearchRequest() {
        Request request = new Request(Request.Action.SEARCH_PRODUCT, "client-001");
        request.addParameter("query", "Sony WH-1000XM5");
        request.addParameter("limit", 5);
        return request;
    }

    private static List<Product> createMockProducts() {
        List<Product> products = new ArrayList<>();
        
        Product p1 = new Product();
        p1.setProductId(1);
        p1.setName("Sony WH-1000XM5");
        p1.setBrand("Sony");
        p1.setGroupId(1);
        products.add(p1);
        
        Product p2 = new Product();
        p2.setProductId(2);
        p2.setName("Sony WH-1000XM4");
        p2.setBrand("Sony");
        p2.setGroupId(1);
        products.add(p2);
        
        return products;
    }

    private static ProductData createMockProductData() {
        // Product
        Product product = new Product();
        product.setProductId(1);
        product.setName("iPhone 15 Pro Max");
        product.setBrand("Apple");
        product.setGroupId(1);
        product.setDescription("iPhone 15 Pro Max - Titanium Design");

        // Price History
        List<PriceHistory> priceHistory = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            PriceHistory ph = new PriceHistory();
            ph.setPriceId(i + 1);
            ph.setProductId(1);
            ph.setPrice(34990000 - (i * 100000)); // Giá giảm dần
            ph.setOriginalPrice(39990000);
            ph.setCurrency("VND");
            ph.setDealType("NORMAL");
            ph.setCapturedAt(new java.sql.Timestamp(System.currentTimeMillis() - (i * 86400000L)));
            priceHistory.add(ph);
        }

        // Reviews
        List<Review> reviews = new ArrayList<>();
        reviews.add(new Review(1, "Nguyễn Văn A", 5, "Sản phẩm tuyệt vời!"));
        reviews.add(new Review(1, "Trần Thị B", 4, "Tốt, màn hình đẹp"));
        reviews.add(new Review(1, "Lê Văn C", 5, "Rất hài lòng, ship nhanh"));

        return new ProductData(product, priceHistory, reviews);
    }
}
