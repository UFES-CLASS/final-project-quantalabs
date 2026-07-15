package com.quantalabs.jamusync.dao;

import com.quantalabs.jamusync.database.DatabaseManager;
import com.quantalabs.jamusync.model.TransactionItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransactionItemDAO {

    public boolean insertItem(TransactionItem item) {
        String sql = "INSERT INTO transaction_items (transaction_id, product_id, quantity, unit_price, subtotal, product_name) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            setItemParams(pstmt, item);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertItem(Connection conn, TransactionItem item) throws SQLException {
        String sql = "INSERT INTO transaction_items (transaction_id, product_id, quantity, unit_price, subtotal, product_name) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            setItemParams(pstmt, item);
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<TransactionItem> getItemsByTransactionId(int transactionId) {
        List<TransactionItem> items = new ArrayList<>();
        // Read the product name stored on the item at sale time. Only fall back to
        // the products table for legacy rows that predate the product_name column
        // (where it is still null). A LEFT JOIN ensures items whose product has
        // since been deleted are still returned.
        String sql = "SELECT ti.id, ti.transaction_id, ti.product_id, ti.quantity, ti.unit_price, ti.subtotal, " +
                     "COALESCE(ti.product_name, p.name) AS product_name FROM transaction_items ti " +
                     "LEFT JOIN products p ON ti.product_id = p.id WHERE ti.transaction_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public double getTotalCostByDateRange(String startDate, String endDate) {
        String sql = "SELECT SUM(ti.quantity * p.cost) FROM transaction_items ti " +
                     "JOIN products p ON ti.product_id = p.id " +
                     "JOIN transactions t ON ti.transaction_id = t.id " +
                     "WHERE t.status = 'Completed' AND date(t.created_at) >= date(?) AND date(t.created_at) <= date(?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    /**
     * Get the top 5 best selling products (by quantity) for completed sales
     * within a date range. Returns pairs of (product name, total quantity).
     */
    public List<Map.Entry<String, Integer>> getSalesByProduct(String startDate, String endDate) {
        List<Map.Entry<String, Integer>> results = new ArrayList<>();
        // Group by the name stored on each item (falling back to the products table
        // for legacy rows) so best-sellers stay correct even after a product is
        // deleted. LEFT JOIN keeps items whose product no longer exists.
        String sql = "SELECT COALESCE(ti.product_name, p.name) AS name, SUM(ti.quantity) AS total_qty " +
                     "FROM transaction_items ti " +
                     "JOIN transactions t ON ti.transaction_id = t.id " +
                     "LEFT JOIN products p ON ti.product_id = p.id " +
                     "WHERE t.status = 'Completed' " +
                     "AND date(t.created_at) >= date(?) AND date(t.created_at) <= date(?) " +
                     "GROUP BY name ORDER BY total_qty DESC LIMIT 5";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    int qty = rs.getInt("total_qty");
                    results.add(new AbstractMap.SimpleEntry<>(name, qty));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    private void setItemParams(PreparedStatement pstmt, TransactionItem item) throws SQLException {
        pstmt.setInt(1, item.getTransactionId());
        pstmt.setInt(2, item.getProductId());
        pstmt.setInt(3, item.getQuantity());
        pstmt.setDouble(4, item.getUnitPrice());
        pstmt.setDouble(5, item.getSubtotal());
        // Snapshot the product name at sale time so history survives deletion.
        pstmt.setString(6, item.getProductName());
    }

    private TransactionItem mapResultSet(ResultSet rs) throws SQLException {
        TransactionItem item = new TransactionItem();
        item.setId(rs.getInt("id"));
        item.setTransactionId(rs.getInt("transaction_id"));
        item.setProductId(rs.getInt("product_id"));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnitPrice(rs.getDouble("unit_price"));
        item.setSubtotal(rs.getDouble("subtotal"));
        item.setProductName(rs.getString("product_name"));
        return item;
    }
}
