package com.quantalabs.jamusync.dao;

import com.quantalabs.jamusync.database.DatabaseManager;
import com.quantalabs.jamusync.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    /**
     * Get all active products.
     * @return List of active products.
     */
    public List<Product> getAllActiveProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE is_active = 1 ORDER BY name ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    /**
     * Get products that are at or below their low stock threshold.
     * @return List of low stock products.
     */
    public List<Product> getLowStockProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE is_active = 1 AND stock <= low_stock_threshold ORDER BY stock ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    /**
     * Get count of active products that are low in stock.
     * @return count.
     */
    public int getLowStockCount() {
        String sql = "SELECT COUNT(*) FROM products WHERE is_active = 1 AND stock <= low_stock_threshold";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Insert a new product (useful for seeding and testing).
     * @param product The product details.
     * @return True if insert succeeded, false otherwise.
     */
    public boolean insertProduct(Product product) {
        String sql = "INSERT INTO products (name, description, ingredients, health_benefits, price, cost, stock, low_stock_threshold, image_path, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getDescription());
            pstmt.setString(3, product.getIngredients());
            pstmt.setString(4, product.getHealthBenefits());
            pstmt.setDouble(5, product.getPrice());
            pstmt.setDouble(6, product.getCost());
            pstmt.setInt(7, product.getStock());
            pstmt.setInt(8, product.getLowStockThreshold());
            pstmt.setString(9, product.getImagePath());
            pstmt.setInt(10, product.isActive() ? 1 : 0);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all products (both active and inactive) for management.
     * @return List of all products.
     */
    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY name ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    /**
     * Update an existing product.
     * @param product The product details.
     * @return True if update succeeded, false otherwise.
     */
    public boolean updateProduct(Product product) {
        String sql = "UPDATE products SET name = ?, description = ?, ingredients = ?, health_benefits = ?, " +
                     "price = ?, cost = ?, stock = ?, low_stock_threshold = ?, image_path = ?, is_active = ? " +
                     "WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getDescription());
            pstmt.setString(3, product.getIngredients());
            pstmt.setString(4, product.getHealthBenefits());
            pstmt.setDouble(5, product.getPrice());
            pstmt.setDouble(6, product.getCost());
            pstmt.setInt(7, product.getStock());
            pstmt.setInt(8, product.getLowStockThreshold());
            pstmt.setString(9, product.getImagePath());
            pstmt.setInt(10, product.isActive() ? 1 : 0);
            pstmt.setInt(11, product.getId());
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deactivate a product by setting is_active = 0.
     * @param productId The product ID.
     * @return True if succeeded, false otherwise.
     */
    public boolean deactivateProduct(int productId) {
        String sql = "UPDATE products SET is_active = 0 WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if a product name is unique (excluding a specific ID).
     * @param name The product name to check.
     * @param excludeId The ID to exclude from query (0 if checking for a new product).
     * @return True if unique, false if name already exists.
     */
    public boolean isNameUnique(String name, int excludeId) {
        String sql = "SELECT COUNT(*) FROM products WHERE name = ? AND id != ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            pstmt.setInt(2, excludeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get a product by ID.
     * @param id Product identifier.
     * @return Product or null if not found.
     */
    public Product getProductById(int id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduct(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Update stock level for a product.
     * @param productId Product identifier.
     * @param newStock New stock quantity.
     * @return True if update succeeded.
     */
    public boolean updateStock(int productId, int newStock) {
        String sql = "UPDATE products SET stock = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, newStock);
            pstmt.setInt(2, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update stock within an existing database transaction.
     */
    public boolean updateStock(Connection conn, int productId, int newStock) throws SQLException {
        String sql = "UPDATE products SET stock = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newStock);
            pstmt.setInt(2, productId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Get product by ID within an existing connection (for transactional reads).
     */
    public Product getProductById(Connection conn, int id) throws SQLException {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduct(rs);
                }
            }
        }
        return null;
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        return new Product(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("ingredients"),
            rs.getString("health_benefits"),
            rs.getDouble("price"),
            rs.getDouble("cost"),
            rs.getInt("stock"),
            rs.getInt("low_stock_threshold"),
            rs.getString("image_path"),
            rs.getInt("is_active") == 1,
            rs.getString("created_at")
        );
    }
}
