package com.quantalabs.jamusync.dao;

import com.quantalabs.jamusync.database.DatabaseManager;
import com.quantalabs.jamusync.model.StockMovement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StockMovementDAO {

    public boolean insertMovement(StockMovement movement) {
        String sql = "INSERT INTO stock_movements (product_id, type, quantity, note, performed_by) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, movement.getProductId());
            pstmt.setString(2, movement.getType());
            pstmt.setInt(3, movement.getQuantity());
            pstmt.setString(4, movement.getNote());
            pstmt.setInt(5, movement.getPerformedBy());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean insertMovement(Connection conn, StockMovement movement) throws SQLException {
        String sql = "INSERT INTO stock_movements (product_id, type, quantity, note, performed_by) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, movement.getProductId());
            pstmt.setString(2, movement.getType());
            pstmt.setInt(3, movement.getQuantity());
            pstmt.setString(4, movement.getNote());
            pstmt.setInt(5, movement.getPerformedBy());
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<StockMovement> getAllMovements() {
        List<StockMovement> movements = new ArrayList<>();
        String sql = "SELECT sm.*, p.name AS product_name, u.username AS performed_by_username " +
                       "FROM stock_movements sm " +
                       "JOIN products p ON sm.product_id = p.id " +
                       "LEFT JOIN users u ON sm.performed_by = u.id " +
                       "ORDER BY sm.created_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                movements.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movements;
    }

    public List<StockMovement> getMovementsByProduct(int productId) {
        List<StockMovement> movements = new ArrayList<>();
        String sql = "SELECT sm.*, p.name AS product_name, u.username AS performed_by_username " +
                       "FROM stock_movements sm " +
                       "JOIN products p ON sm.product_id = p.id " +
                       "LEFT JOIN users u ON sm.performed_by = u.id " +
                       "WHERE sm.product_id = ? ORDER BY sm.created_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    movements.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return movements;
    }

    private StockMovement mapResultSet(ResultSet rs) throws SQLException {
        StockMovement movement = new StockMovement();
        movement.setId(rs.getInt("id"));
        movement.setProductId(rs.getInt("product_id"));
        movement.setType(rs.getString("type"));
        movement.setQuantity(rs.getInt("quantity"));
        movement.setNote(rs.getString("note"));
        movement.setPerformedBy(rs.getInt("performed_by"));
        movement.setCreatedAt(rs.getString("created_at"));
        movement.setProductName(rs.getString("product_name"));
        movement.setPerformedByUsername(rs.getString("performed_by_username"));
        return movement;
    }
}
