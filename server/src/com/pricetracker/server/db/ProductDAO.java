package com.pricetracker.server.db;

import com.pricetracker.models.Product;
import com.pricetracker.server.utils.TikiScraperUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM product LIMIT 10";

        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Product p = new Product();
                p.setProductId(rs.getInt("product_id"));
                p.setName(rs.getString("name"));
                p.setBrand(rs.getString("brand"));
                p.setUrl(rs.getString("url"));
                p.setImageUrl(rs.getString("image_url"));
                list.add(p);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
    
    /**
     * Get product by ID
     * @param productId The product ID
     * @return Product if found, null otherwise
     */
    public Product getProductById(int productId) {
        String sql = "SELECT * FROM product WHERE product_id = ?";
        
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToProduct(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting product by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Search product by exact Tiki URL
     * @param tikiUrl The Tiki product URL
     * @return Product if found, null otherwise
     */
    public Product searchByUrl(String tikiUrl) {
        String sql = "SELECT * FROM product WHERE url = ?";
        
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, tikiUrl);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToProduct(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error searching by URL: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get similar products by group_id (for "Similar Products" section)
     * @param groupId The product group ID
     * @param excludeProductId Product ID to exclude (the current product)
     * @param limit Maximum number of similar products to return
     * @return List of similar products
     */
    public List<Product> getSimilarProducts(int groupId, int excludeProductId, int limit) {
        List<Product> results = new ArrayList<>();
        
        String sql = "SELECT * FROM product " +
                     "WHERE group_id = ? AND product_id != ? " +
                     "ORDER BY product_id DESC " +
                     "LIMIT ?";
        
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, groupId);
            stmt.setInt(2, excludeProductId);
            stmt.setInt(3, limit);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                results.add(mapResultSetToProduct(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting similar products: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Search products by name (LIKE search)
     * Case-insensitive search using LOWER() function
     * @param keyword Search keyword
     * @return List of matching products
     */
    public List<Product> searchByNameLike(String keyword) {
        List<Product> results = new ArrayList<>();
        
        System.out.println("🔍 Searching for keyword: " + keyword);
        
        // Use LOWER() for case-insensitive search
        // This will match "Samsung", "samsung", "SAMSUNG" all the same
        String sql = "SELECT DISTINCT p.* FROM product p " +
                     "LEFT JOIN product_group pg ON p.group_id = pg.group_id " +
                     "WHERE " +
                     "LOWER(p.name) LIKE LOWER(?) OR " +
                     "LOWER(p.brand) LIKE LOWER(?) OR " +
                     "LOWER(pg.group_name) LIKE LOWER(?) " +
                     "LIMIT 50";
        
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String pattern = "%" + keyword + "%";
            System.out.println("📝 Search pattern: " + pattern);
            
            stmt.setString(1, pattern);  // Search in product name
            stmt.setString(2, pattern);  // Search in brand
            stmt.setString(3, pattern);  // Search in group name
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                results.add(mapResultSetToProduct(rs));
            }
            
            System.out.println("✅ Found " + results.size() + " products");
            
        } catch (SQLException e) {
            System.err.println("Error searching by name: " + e.getMessage());
            e.printStackTrace();
        }
        
        return results;
    }
    
    /**
     * Insert product from Tiki scraping
     * First scrapes the product data, then inserts into database
     * @param tikiUrl The Tiki product URL
     * @return The inserted Product with product_id, or null if failed
     */
    public Product insertProductFromTiki(String tikiUrl) {
        // Scrape product data from Tiki
        Product product = TikiScraperUtil.scrapeProductFromUrl(tikiUrl);
        if (product == null) {
            System.err.println("Failed to scrape product from Tiki");
            return null;
        }
        
        // IMPORTANT: Force group_id = 9 for user-added products
        product.setGroupId(9);
        System.out.println("🆕 User-added product, setting group_id = 9 (Sản phẩm mới)");
        
        // Insert into database
        String sql = "INSERT INTO product (group_id, name, brand, url, image_url, description, source) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, product.getGroupId());
            stmt.setString(2, product.getName());
            stmt.setString(3, product.getBrand());
            stmt.setString(4, product.getUrl());
            stmt.setString(5, product.getImageUrl());
            stmt.setString(6, product.getDescription());
            stmt.setString(7, product.getSource());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    product.setProductId(generatedKeys.getInt(1));
                    System.out.println("✅ Inserted new product: " + product.getName() + " (ID: " + product.getProductId() + ")");
                    
                    // Also scrape and insert initial price data
                    insertInitialPriceData(product.getProductId(), tikiUrl);
                    
                    return product;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error inserting product: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Insert initial price data after adding new product
     */
    private void insertInitialPriceData(int productId, String tikiUrl) {
        Object[] priceData = TikiScraperUtil.scrapePriceData(tikiUrl);
        if (priceData == null) {
            return;
        }
        
        double price = (double) priceData[0];
        double originalPrice = (double) priceData[1];
        String dealType = (String) priceData[2];
        
        String sql = "INSERT INTO price_history (product_id, price, original_price, currency, deal_type) " +
                     "VALUES (?, ?, ?, 'VND', ?)";
        
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, productId);
            stmt.setDouble(2, price);
            stmt.setDouble(3, originalPrice);
            stmt.setString(4, dealType);
            
            stmt.executeUpdate();
            System.out.println("✅ Inserted initial price: " + price + " VND");
            
        } catch (SQLException e) {
            System.err.println("Error inserting initial price: " + e.getMessage());
        }
    }
    
    /**
     * Map ResultSet to Product object
     */
    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getInt("product_id"));
        p.setGroupId(rs.getInt("group_id"));
        p.setName(rs.getString("name"));
        p.setBrand(rs.getString("brand"));
        p.setUrl(rs.getString("url"));
        p.setImageUrl(rs.getString("image_url"));
        p.setDescription(rs.getString("description"));
        p.setSource(rs.getString("source"));
        p.setFeatured(rs.getBoolean("is_featured"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        return p;
    }
    
    /**
     * Get products by deal type 
     * Đơn giản: Chỉ query dựa trên deal_type có sẵn trong price_history
     * (deal_type đã được Python scraper xác định khi cào dữ liệu)
     * 
     * @param dealType "FLASH_SALE", "HOT_DEAL", "TRENDING", or "ALL" for all deals
     * @return List of products with the specified deal type
     */
    public List<Product> getProductsByDealType(String dealType) {
        List<Product> results = new ArrayList<>();
        String sql;
        
        if ("ALL".equals(dealType)) {
            // ALL: Lấy bản ghi mới nhất có deal-type hợp lệ cho mỗi sản phẩm
            sql = "SELECT p.* FROM product p " +
                  "INNER JOIN ( " +
                  "  SELECT ph1.* FROM price_history ph1 " +
                  "  INNER JOIN ( " +
                  "    SELECT product_id, MAX(price_id) AS max_price_id " +
                  "    FROM price_history WHERE deal_type IN ('FLASH_SALE', 'HOT_DEAL', 'TRENDING') AND original_price > price " +
                  "    GROUP BY product_id " +
                  "  ) ph2 ON ph1.product_id = ph2.product_id AND ph1.price_id = ph2.max_price_id " +
                  ") ph ON p.product_id = ph.product_id " +
                  "ORDER BY ((ph.original_price - ph.price) / ph.original_price) DESC " +
                  "LIMIT 200";
        } else if ("TRENDING".equals(dealType)) {
            // TRENDING: Lấy bản ghi mới nhất có deal_type TRENDING cho mỗi sản phẩm
            sql = "SELECT p.* FROM product p " +
                  "INNER JOIN ( " +
                  "  SELECT ph1.* FROM price_history ph1 " +
                  "  INNER JOIN ( " +
                  "    SELECT product_id, MAX(price_id) AS max_price_id " +
                  "    FROM price_history WHERE deal_type = 'TRENDING' AND original_price > price " +
                  "    GROUP BY product_id " +
                  "  ) ph2 ON ph1.product_id = ph2.product_id AND ph1.price_id = ph2.max_price_id " +
                  ") ph ON p.product_id = ph.product_id " +
                  "ORDER BY ((ph.original_price - ph.price) / ph.original_price) DESC " +
                  "LIMIT 20";
        } else {
            // FLASH_SALE hoặc HOT_DEAL: Lấy bản ghi mới nhất có deal_type tương ứng cho mỗi sản phẩm
            sql = "SELECT p.* FROM product p " +
                  "INNER JOIN ( " +
                  "  SELECT ph1.* FROM price_history ph1 " +
                  "  INNER JOIN ( " +
                  "    SELECT product_id, MAX(price_id) AS max_price_id " +
                  "    FROM price_history WHERE deal_type = ? AND original_price > price " +
                  "    GROUP BY product_id " +
                  "  ) ph2 ON ph1.product_id = ph2.product_id AND ph1.price_id = ph2.max_price_id " +
                  ") ph ON p.product_id = ph.product_id " +
                  "ORDER BY ((ph.original_price - ph.price) / ph.original_price) DESC " +
                  "LIMIT 100";
        }
        
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set parameter nếu không phải ALL hoặc TRENDING
            if (!"ALL".equals(dealType) && !"TRENDING".equals(dealType)) {
                stmt.setString(1, dealType);
            }
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                results.add(mapResultSetToProduct(rs));
            }
            
            System.out.println("✓ Found " + results.size() + " products with deal type: " + dealType);
            
        } catch (SQLException e) {
            System.err.println("Error getting products by deal type: " + e.getMessage());
            e.printStackTrace();
        }
        
        return results; 
    }
    
    /**
     * Get products by group_id (for category filtering)
     * @param groupId The product group ID
     * @return List of products in that group
     */
    public List<Product> getProductsByGroupId(int groupId) {
        List<Product> results = new ArrayList<>();
        String sql = "SELECT * FROM product WHERE group_id = ? ORDER BY product_id DESC LIMIT 100";
        
        try (Connection conn = DatabaseConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                results.add(mapResultSetToProduct(rs));
            }
            
            System.out.println("✓ Found " + results.size() + " products in group " + groupId);
            
        } catch (SQLException e) {
            System.err.println("Error getting products by group: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Count products in each group
     * @return Map of group_id to product count
     */
    public java.util.Map<Integer, Integer> countProductsByGroup() {
        java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
        String sql = "SELECT group_id, COUNT(*) as count FROM product GROUP BY group_id";
        
        try (Connection conn = DatabaseConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                counts.put(rs.getInt("group_id"), rs.getInt("count"));
            }
            
        } catch (SQLException e) {
            System.err.println("Error counting products by group: " + e.getMessage());
        }
        
        return counts;
    }
}

