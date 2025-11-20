package com.pricetracker.server.db;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Data Access Object for product_group table
 * Handles retrieval of group names and group information
 */
public class ProductGroupDAO {
    
    /**
     * Get group name by group_id
     * @param groupId The group ID
     * @return Group name, or "Sản phẩm mới" if not found
     */
    public String getGroupNameById(int groupId) {
        String sql = "SELECT group_name FROM product_group WHERE group_id = ?";
        
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("group_name");
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting group name: " + e.getMessage());
        }
        
        return "Sản phẩm mới"; // Default fallback
    }
    
    /**
     * Get all product groups as a map (group_id -> group_name)
     * @return Map of group_id to group_name
     */
    public Map<Integer, String> getAllGroups() {
        Map<Integer, String> groups = new HashMap<>();
        String sql = "SELECT group_id, group_name FROM product_group";
        
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                groups.put(rs.getInt("group_id"), rs.getString("group_name"));
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting all groups: " + e.getMessage());
        }
        
        return groups;
    }
    
    /**
     * Get group_id by group name (case-insensitive)
     * @param groupName The group name to search for
     * @return group_id if found, or 9 (Sản phẩm mới) if not found
     */
    public int getGroupIdByName(String groupName) {
        String sql = "SELECT group_id FROM product_group WHERE LOWER(group_name) = LOWER(?)";
        
        try (Connection conn = DatabaseConnectionManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, groupName);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("group_id");
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting group_id by name: " + e.getMessage());
        }
        
        return 9; // Default to "Sản phẩm mới" (group 9)
    }
}
