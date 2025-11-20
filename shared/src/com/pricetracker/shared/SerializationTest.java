package com.pricetracker.shared;

import com.pricetracker.models.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * SerializationTest - Class để test và minh họa cách sử dụng module nhập xuất object
 * Chạy file này để kiểm tra các chức năng serialize/deserialize
 */
public class SerializationTest {

    public static void main(String[] args) {
        System.out.println("=".repeat(70));
        System.out.println("    MODULE NHẬP XUẤT OBJECT - DEMO & TEST");
        System.out.println("=".repeat(70));
        System.out.println();

        // Test 1: Serialize/Deserialize Product
        testProduct();
        
        // Test 2: Serialize/Deserialize Request
        testRequest();
        
        // Test 3: Serialize/Deserialize Response
        testResponse();
        
        // Test 4: Serialize/Deserialize ProductData (complex object)
        testProductData();
        
        // Test 5: Deep Clone
        testDeepClone();
        
        // Test 6: File I/O
        testFileIO();
        
        // Test 7: Size calculation
        testSizeCalculation();
        
        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println("    HOÀN THÀNH TẤT CẢ CÁC TEST!");
        System.out.println("=".repeat(70));
    }

    /**
     * Test 1: Serialize và deserialize Product
     */
    private static void testProduct() {
        System.out.println("📦 TEST 1: Product Serialization");
        System.out.println("-".repeat(70));
        
        try {
            // Tạo product
            Product product = new Product();
            product.setProductId(1);
            product.setName("Sony WH-1000XM5");
            product.setBrand("Sony");
            product.setUrl("https://tiki.vn/sony-wh-1000xm5-p123456.html");
            product.setImageUrl("https://salt.tikicdn.com/cache/280x280/ts/product/abc.jpg");
            product.setGroupId(1);
            product.setSource("Tiki");
            
            System.out.println("   Original: " + product);
            
            // Serialize
            byte[] data = SerializationUtil.serialize(product);
            System.out.println("   ✓ Serialized: " + data.length + " bytes");
            
            // Deserialize
            Product deserialized = SerializationUtil.deserialize(data, Product.class);
            System.out.println("   ✓ Deserialized: " + deserialized);
            
            // Verify
            boolean isEqual = product.getProductId() == deserialized.getProductId() &&
                             product.getName().equals(deserialized.getName());
            System.out.println("   ✓ Verification: " + (isEqual ? "PASS ✅" : "FAIL ❌"));
            
        } catch (Exception e) {
            System.out.println("   ✗ ERROR: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Test 2: Serialize và deserialize Request
     */
    private static void testRequest() {
        System.out.println("📨 TEST 2: Request Serialization");
        System.out.println("-".repeat(70));
        
        try {
            // Tạo request
            Request request = new Request(Request.Action.SEARCH_PRODUCT, "client-001");
            request.addParameter("query", "Sony WH-1000XM5");
            request.addParameter("limit", 10);
            
            System.out.println("   Original: " + request);
            
            // Serialize
            byte[] data = SerializationUtil.serialize(request);
            System.out.println("   ✓ Serialized: " + data.length + " bytes");
            
            // Deserialize
            Request deserialized = SerializationUtil.deserialize(data, Request.class);
            System.out.println("   ✓ Deserialized: " + deserialized);
            
            // Verify
            String query = deserialized.getStringParameter("query");
            Integer limit = deserialized.getIntParameter("limit");
            boolean isEqual = "Sony WH-1000XM5".equals(query) && limit == 10;
            System.out.println("   ✓ Verification: " + (isEqual ? "PASS ✅" : "FAIL ❌"));
            
        } catch (Exception e) {
            System.out.println("   ✗ ERROR: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Test 3: Serialize và deserialize Response
     */
    private static void testResponse() {
        System.out.println("📬 TEST 3: Response Serialization");
        System.out.println("-".repeat(70));
        
        try {
            // Tạo response với data
            Product product = new Product();
            product.setProductId(1);
            product.setName("iPhone 15 Pro Max");
            
            Response response = Response.success("Tìm thấy sản phẩm", product);
            System.out.println("   Original: " + response);
            
            // Serialize
            byte[] data = SerializationUtil.serialize(response);
            System.out.println("   ✓ Serialized: " + data.length + " bytes");
            
            // Deserialize
            Response deserialized = SerializationUtil.deserialize(data, Response.class);
            System.out.println("   ✓ Deserialized: " + deserialized);
            
            // Verify
            Product deserializedProduct = deserialized.getData(Product.class);
            boolean isEqual = deserialized.isSuccess() && 
                             deserializedProduct.getName().equals("iPhone 15 Pro Max");
            System.out.println("   ✓ Verification: " + (isEqual ? "PASS ✅" : "FAIL ❌"));
            
        } catch (Exception e) {
            System.out.println("   ✗ ERROR: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Test 4: Serialize và deserialize ProductData (complex)
     */
    private static void testProductData() {
        System.out.println("📊 TEST 4: ProductData Serialization (Complex Object)");
        System.out.println("-".repeat(70));
        
        try {
            // Tạo product
            Product product = new Product();
            product.setProductId(1);
            product.setName("MacBook Pro M3");
            product.setBrand("Apple");
            
            // Tạo price history
            List<PriceHistory> priceHistory = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                PriceHistory ph = new PriceHistory();
                ph.setProductId(1);
                ph.setPrice(45000000 - (i * 500000));
                ph.setOriginalPrice(50000000);
                ph.setCurrency("VND");
                ph.setDealType("NORMAL");
                ph.setCapturedAt(new Timestamp(System.currentTimeMillis() - (i * 86400000L)));
                priceHistory.add(ph);
            }
            
            // Tạo reviews
            List<Review> reviews = new ArrayList<>();
            reviews.add(new Review(1, "Nguyễn Văn A", 5, "Sản phẩm tuyệt vời!"));
            reviews.add(new Review(1, "Trần Thị B", 4, "Tốt, nhưng hơi đắt"));
            
            // Tạo ProductData
            ProductData productData = new ProductData(product, priceHistory, reviews);
            
            System.out.println("   Original: " + productData);
            System.out.println("   - Price History: " + priceHistory.size() + " records");
            System.out.println("   - Reviews: " + reviews.size() + " records");
            System.out.println("   - Average Rating: " + productData.getAverageRating());
            
            // Serialize
            byte[] data = SerializationUtil.serialize(productData);
            System.out.println("   ✓ Serialized: " + data.length + " bytes");
            
            // Deserialize
            ProductData deserialized = SerializationUtil.deserialize(data, ProductData.class);
            System.out.println("   ✓ Deserialized: " + deserialized);
            System.out.println("   - Price History: " + deserialized.getPriceHistory().size() + " records");
            System.out.println("   - Reviews: " + deserialized.getReviews().size() + " records");
            
            // Verify
            boolean isEqual = deserialized.getProduct().getName().equals("MacBook Pro M3") &&
                             deserialized.getPriceHistory().size() == 5 &&
                             deserialized.getReviews().size() == 2;
            System.out.println("   ✓ Verification: " + (isEqual ? "PASS ✅" : "FAIL ❌"));
            
        } catch (Exception e) {
            System.out.println("   ✗ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println();
    }

    /**
     * Test 5: Deep Clone
     */
    private static void testDeepClone() {
        System.out.println("🔄 TEST 5: Deep Clone");
        System.out.println("-".repeat(70));
        
        try {
            Product original = new Product();
            original.setProductId(1);
            original.setName("Original Product");
            original.setBrand("Brand A");
            
            System.out.println("   Original: " + original);
            
            // Clone
            Product cloned = SerializationUtil.deepClone(original);
            System.out.println("   ✓ Cloned: " + cloned);
            
            // Modify original
            original.setName("Modified Product");
            
            // Verify clone is independent
            boolean isIndependent = cloned.getName().equals("Original Product") &&
                                   original.getName().equals("Modified Product");
            System.out.println("   ✓ Independence: " + (isIndependent ? "PASS ✅" : "FAIL ❌"));
            
        } catch (Exception e) {
            System.out.println("   ✗ ERROR: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Test 6: File I/O
     */
    private static void testFileIO() {
        System.out.println("💾 TEST 6: File I/O");
        System.out.println("-".repeat(70));
        
        try {
            String filePath = "test_product.ser";
            
            // Create product
            Product product = new Product();
            product.setProductId(999);
            product.setName("Test Product for File I/O");
            
            System.out.println("   Original: " + product);
            
            // Serialize to file
            SerializationUtil.serializeToFile(product, filePath);
            System.out.println("   ✓ Saved to file: " + filePath);
            
            // Deserialize from file
            Product loaded = SerializationUtil.deserializeFromFile(filePath, Product.class);
            System.out.println("   ✓ Loaded from file: " + loaded);
            
            // Verify
            boolean isEqual = loaded.getProductId() == 999 &&
                             loaded.getName().equals("Test Product for File I/O");
            System.out.println("   ✓ Verification: " + (isEqual ? "PASS ✅" : "FAIL ❌"));
            
            // Cleanup
            new java.io.File(filePath).delete();
            System.out.println("   ✓ Cleaned up test file");
            
        } catch (Exception e) {
            System.out.println("   ✗ ERROR: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Test 7: Size Calculation
     */
    private static void testSizeCalculation() {
        System.out.println("📏 TEST 7: Size Calculation");
        System.out.println("-".repeat(70));
        
        try {
            Product product = new Product();
            product.setProductId(1);
            product.setName("Test Product");
            
            long size = SerializationUtil.getSerializedSize(product);
            System.out.println("   Product size: " + size + " bytes");
            
            Request request = new Request(Request.Action.SEARCH_PRODUCT);
            request.addParameter("query", "test");
            
            long requestSize = SerializationUtil.getSerializedSize(request);
            System.out.println("   Request size: " + requestSize + " bytes");
            
            boolean isSerializable = SerializationUtil.isSerializable(product);
            System.out.println("   ✓ Is Serializable: " + (isSerializable ? "YES ✅" : "NO ❌"));
            
        } catch (Exception e) {
            System.out.println("   ✗ ERROR: " + e.getMessage());
        }
        
        System.out.println();
    }
}
