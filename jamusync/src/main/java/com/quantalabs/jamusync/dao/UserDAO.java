package com.quantalabs.jamusync.dao;

import com.quantalabs.jamusync.database.DatabaseManager;
import com.quantalabs.jamusync.model.User;
import com.quantalabs.jamusync.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    /**
     * Authenticate user with username and raw password.
     * @param username The username input.
     * @param rawPassword The raw password input.
     * @return The authenticated User object, or null if credentials are invalid or user is inactive.
     */
    public User authenticate(String username, String rawPassword) {
        String hash = PasswordUtil.hashPassword(rawPassword);
        if (hash == null) {
            return null;
        }

        String sql = "SELECT id, username, password_hash, role, is_active, created_at FROM users " +
                     "WHERE username = ? AND password_hash = ? AND is_active = 1";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, hash);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getInt("is_active") == 1,
                        rs.getString("created_at")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieve user by username.
     * @param username The username to look up.
     * @return The User object or null if not found.
     */
    public User getUserByUsername(String username) {
        String sql = "SELECT id, username, password_hash, role, is_active, created_at FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getInt("is_active") == 1,
                        rs.getString("created_at")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Add a new user to the database.
     * @param username The username of the new user.
     * @param rawPassword The raw password of the new user.
     * @param role The role (owner/staff).
     * @return True if insert succeeded, false otherwise.
     */
    public boolean insertUser(String username, String rawPassword, String role) {
        String hash = PasswordUtil.hashPassword(rawPassword);
        if (hash == null) {
            return false;
        }
        
        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, hash);
            pstmt.setString(3, role);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update active status of user (soft delete).
     * @param userId User identifier.
     * @param active The active status to set.
     * @return True if update succeeded, false otherwise.
     */
    public boolean setUserActive(int userId, boolean active) {
        String sql = "UPDATE users SET is_active = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, active ? 1 : 0);
            pstmt.setInt(2, userId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all staff users (both active and inactive).
     * @return List of staff users.
     */
    public List<User> getAllStaff() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, password_hash, role, is_active, created_at FROM users WHERE role = 'staff' ORDER BY username ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                users.add(new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("role"),
                    rs.getInt("is_active") == 1,
                    rs.getString("created_at")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Update an existing user.
     * @param id The user ID to update.
     * @param username The new username.
     * @param rawPassword The new raw password (if not null/empty, will be hashed and updated).
     * @param isActive The active status.
     * @return True if update succeeded, false otherwise.
     */
    public boolean updateUser(int id, String username, String rawPassword, boolean isActive) {
        boolean updatePassword = rawPassword != null && !rawPassword.trim().isEmpty();
        String sql;
        if (updatePassword) {
            sql = "UPDATE users SET username = ?, password_hash = ?, is_active = ? WHERE id = ?";
        } else {
            sql = "UPDATE users SET username = ?, is_active = ? WHERE id = ?";
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            if (updatePassword) {
                String hash = PasswordUtil.hashPassword(rawPassword);
                pstmt.setString(2, hash);
                pstmt.setInt(3, isActive ? 1 : 0);
                pstmt.setInt(4, id);
            } else {
                pstmt.setInt(2, isActive ? 1 : 0);
                pstmt.setInt(3, id);
            }
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if a username is unique (excluding a specific user ID).
     * @param username The username to check.
     * @param excludeId The ID to exclude (0 if checking for a new user).
     * @return True if unique, false otherwise.
     */
    public boolean isUsernameUnique(String username, int excludeId) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ? AND id != ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
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
}
