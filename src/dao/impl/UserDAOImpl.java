package dao.impl;

import dao.UserDAO;
import database.DatabaseConnection;
import model.User;
import util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of UserDAO interface using JDBC and DatabaseConnection.
 */
public class UserDAOImpl implements UserDAO {

    @Override
    public User findByUsername(String username) {
        String sql = "SELECT * FROM USERS WHERE Username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDAOImpl.findByUsername error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public User findById(int id) {
        String sql = "SELECT * FROM USERS WHERE UserID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDAOImpl.findById error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean insert(User user) {
        String sql = "INSERT INTO USERS (Username, PasswordHash, FullName, Role, Status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getFullName());
            pstmt.setString(4, user.getRole());
            pstmt.setString(5, user.getStatus());
            
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("UserDAOImpl.insert error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean update(User user) {
        boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().trim().isEmpty();
        String sql;
        if (hasPassword) {
            sql = "UPDATE USERS SET FullName = ?, Role = ?, Status = ?, PasswordHash = ? WHERE UserID = ?";
        } else {
            sql = "UPDATE USERS SET FullName = ?, Role = ?, Status = ? WHERE UserID = ?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getFullName());
            pstmt.setString(2, user.getRole());
            pstmt.setString(3, user.getStatus());
            
            if (hasPassword) {
                pstmt.setString(4, user.getPasswordHash());
                pstmt.setInt(5, user.getUserId());
            } else {
                pstmt.setInt(4, user.getUserId());
            }
            
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("UserDAOImpl.update error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean softDelete(int userId) {
        // Soft delete sets status to 'INACTIVE'
        String sql = "UPDATE USERS SET Status = 'INACTIVE' WHERE UserID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("UserDAOImpl.softDelete error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<User> getAll() {
        List<User> list = new ArrayList<>();
        // Fetch all users except those soft-deleted (INACTIVE)
        String sql = "SELECT * FROM USERS WHERE Status != 'INACTIVE' ORDER BY UserID";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                list.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("UserDAOImpl.getAll error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public User authenticate(String username, String password) {
        User user = findByUsername(username);
        if (user != null) {
            // Verify password using BCrypt
            if (PasswordUtil.checkPassword(password, user.getPasswordHash())) {
                // Return user if ACTIVE or otherwise verified
                if ("ACTIVE".equalsIgnoreCase(user.getStatus())) {
                    return user;
                } else {
                    System.out.println("UserDAOImpl: Account is locked or inactive (" + user.getStatus() + ")");
                }
            }
        }
        return null;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("UserID"));
        user.setUsername(rs.getString("Username"));
        user.setPasswordHash(rs.getString("PasswordHash"));
        user.setFullName(rs.getString("FullName"));
        user.setRole(rs.getString("Role"));
        user.setStatus(rs.getString("Status"));
        user.setCreatedAt(rs.getTimestamp("CreatedAt"));
        return user;
    }
}
