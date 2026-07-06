package com.quantalabs.jamusync.dao;

import com.quantalabs.jamusync.database.DatabaseManager;
import com.quantalabs.jamusync.model.Voucher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VoucherDAO {

    public List<Voucher> getAllVouchers() {
        List<Voucher> vouchers = new ArrayList<>();
        String sql = "SELECT * FROM vouchers ORDER BY created_at DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                vouchers.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return vouchers;
    }

    public Voucher getById(int id) {
        String sql = "SELECT * FROM vouchers WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Voucher getById(Connection conn, int id) throws SQLException {
        String sql = "SELECT * FROM vouchers WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    public Voucher getByCode(String code) {
        String sql = "SELECT * FROM vouchers WHERE code = ? COLLATE NOCASE";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, code.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean insertVoucher(Voucher voucher) {
        String sql = "INSERT INTO vouchers (code, discount_type, discount_value, usage_limit, usage_count, expiry_date, is_active) " +
                     "VALUES (?, ?, ?, ?, 0, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            setVoucherParams(pstmt, voucher);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateVoucher(Voucher voucher) {
        String sql = "UPDATE vouchers SET code = ?, discount_type = ?, discount_value = ?, usage_limit = ?, " +
                     "expiry_date = ?, is_active = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, voucher.getCode());
            pstmt.setString(2, voucher.getDiscountType());
            pstmt.setDouble(3, voucher.getDiscountValue());
            pstmt.setInt(4, voucher.getUsageLimit());
            pstmt.setString(5, voucher.getExpiryDate());
            pstmt.setInt(6, voucher.isActive() ? 1 : 0);
            pstmt.setInt(7, voucher.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deactivateVoucher(int id) {
        String sql = "UPDATE vouchers SET is_active = 0 WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isCodeUnique(String code, int excludeId) {
        String sql = "SELECT COUNT(*) FROM vouchers WHERE code = ? COLLATE NOCASE AND id != ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, code.trim());
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

    public boolean isValid(Voucher voucher) {
        if (voucher == null || !voucher.isActive()) {
            return false;
        }
        if (voucher.getUsageCount() >= voucher.getUsageLimit()) {
            return false;
        }
        try {
            LocalDate expiry = LocalDate.parse(voucher.getExpiryDate());
            return !expiry.isBefore(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean incrementUsage(Connection conn, int voucherId) throws SQLException {
        String sql = "UPDATE vouchers SET usage_count = usage_count + 1 WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, voucherId);
            return pstmt.executeUpdate() > 0;
        }
    }

    private void setVoucherParams(PreparedStatement pstmt, Voucher voucher) throws SQLException {
        pstmt.setString(1, voucher.getCode());
        pstmt.setString(2, voucher.getDiscountType());
        pstmt.setDouble(3, voucher.getDiscountValue());
        pstmt.setInt(4, voucher.getUsageLimit());
        pstmt.setString(5, voucher.getExpiryDate());
        pstmt.setInt(6, voucher.isActive() ? 1 : 0);
    }

    private Voucher mapResultSet(ResultSet rs) throws SQLException {
        Voucher voucher = new Voucher();
        voucher.setId(rs.getInt("id"));
        voucher.setCode(rs.getString("code"));
        voucher.setDiscountType(rs.getString("discount_type"));
        voucher.setDiscountValue(rs.getDouble("discount_value"));
        voucher.setUsageLimit(rs.getInt("usage_limit"));
        voucher.setUsageCount(rs.getInt("usage_count"));
        voucher.setExpiryDate(rs.getString("expiry_date"));
        voucher.setActive(rs.getInt("is_active") == 1);
        voucher.setCreatedAt(rs.getString("created_at"));
        return voucher;
    }
}
