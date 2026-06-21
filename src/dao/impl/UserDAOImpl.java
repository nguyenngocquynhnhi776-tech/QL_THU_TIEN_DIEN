package dao.impl;

import dao.UserDAO;
import database.DatabaseConnection;
import model.User;
import util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of UserDAO interface using JDBC and DatabaseConnection.
 * Audit logs SQL execution, connection status, and transaction states.
 */
public class UserDAOImpl implements UserDAO {

    @Override
    public User findByUsername(String username) {
        String sql = "SELECT * FROM USERS WHERE Username = ?";
        System.out.println("[SQL INFO] Executing Query: " + sql + " | Parameter Username = " + username);
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                throw new SQLException("Failed to establish DB connection");
            }
            System.out.println("[SQL INFO] Connection Status: Open, AutoCommit = " + conn.getAutoCommit());
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("[SQL INFO] User found with username: " + username);
                        return mapResultSetToUser(rs);
                    }
                }
            }
            System.out.println("[SQL INFO] No user found with username: " + username);
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] UserDAOImpl.findByUsername failed: " + e.getMessage());
            throw new RuntimeException("Lỗi CSDL khi tìm kiếm tên đăng nhập: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public User findById(int id) {
        String sql = "SELECT * FROM USERS WHERE UserID = ?";
        System.out.println("[SQL INFO] Executing Query: " + sql + " | Parameter UserID = " + id);
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                throw new SQLException("Failed to establish DB connection");
            }
            System.out.println("[SQL INFO] Connection Status: Open, AutoCommit = " + conn.getAutoCommit());
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("[SQL INFO] User found with ID: " + id);
                        return mapResultSetToUser(rs);
                    }
                }
            }
            System.out.println("[SQL INFO] No user found with ID: " + id);
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] UserDAOImpl.findById failed: " + e.getMessage());
            throw new RuntimeException("Lỗi CSDL khi tìm kiếm ID người dùng: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean insert(User user) {
        String sql = "INSERT INTO USERS (Username, PasswordHash, FullName, Role, Status) VALUES (?, ?, ?, ?, ?)";
        System.out.println("[SQL INFO] Executing Query: " + sql 
            + " | Parameters: Username=" + user.getUsername() 
            + ", FullName=" + user.getFullName() 
            + ", Role=" + user.getRole() 
            + ", Status=" + user.getStatus());
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                throw new SQLException("Failed to establish DB connection");
            }
            System.out.println("[SQL INFO] Connection Status: Open, AutoCommit = " + conn.getAutoCommit());
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, user.getUsername());
                pstmt.setString(2, user.getPasswordHash());
                pstmt.setString(3, user.getFullName());
                pstmt.setString(4, user.getRole());
                pstmt.setString(5, user.getStatus());

                int rows = pstmt.executeUpdate();
                System.out.println("[SQL INFO] Query executed successfully. Rows affected = " + rows);
                return rows > 0;
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] UserDAOImpl.insert failed: " + e.getMessage());
            throw new RuntimeException("Lỗi CSDL khi thêm người dùng: " + e.getMessage(), e);
        }
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

        System.out.println("[SQL INFO] Executing Query: " + sql 
            + " | Parameters: UserID=" + user.getUserId()
            + ", FullName=" + user.getFullName() 
            + ", Role=" + user.getRole() 
            + ", Status=" + user.getStatus()
            + ", UpdatePassword=" + hasPassword);
            
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                throw new SQLException("Failed to establish DB connection");
            }
            System.out.println("[SQL INFO] Connection Status: Open, AutoCommit = " + conn.getAutoCommit());
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
                System.out.println("[SQL INFO] Query executed successfully. Rows affected = " + rows);
                return rows > 0;
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] UserDAOImpl.update failed: " + e.getMessage());
            throw new RuntimeException("Lỗi CSDL khi cập nhật người dùng: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int userId) {
        String sql = "DELETE FROM USERS WHERE UserID = ?";
        System.out.println("[SQL INFO] Executing Query: " + sql + " | Parameter UserID = " + userId);
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                throw new SQLException("Failed to establish DB connection");
            }
            System.out.println("[SQL INFO] Connection Status: Open, AutoCommit = " + conn.getAutoCommit());
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, userId);
                int rows = pstmt.executeUpdate();
                System.out.println("[SQL INFO] Query executed successfully. Rows affected = " + rows);
                return rows > 0;
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] UserDAOImpl.delete failed: " + e.getMessage());
            throw new RuntimeException("Lỗi CSDL khi xóa người dùng: " + e.getMessage(), e);
        }
    }

    @Override
    public List<User> getAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM USERS ORDER BY UserID";
        System.out.println("[SQL INFO] Executing Query: " + sql);
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                throw new SQLException("Failed to establish DB connection");
            }
            System.out.println("[SQL INFO] Connection Status: Open, AutoCommit = " + conn.getAutoCommit());
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                int count = 0;
                while (rs.next()) {
                    list.add(mapResultSetToUser(rs));
                    count++;
                }
                System.out.println("[SQL INFO] Query executed successfully. Loaded " + count + " users.");
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] UserDAOImpl.getAll failed: " + e.getMessage());
            throw new RuntimeException("Lỗi CSDL khi lấy danh sách người dùng: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public User authenticate(String username, String password) {
        System.out.println("[SQL INFO] Authenticating user: " + username);
        User user = findByUsername(username);
        if (user != null) {
            if (PasswordUtil.checkPassword(password, user.getPasswordHash())) {
                if ("ACTIVE".equalsIgnoreCase(user.getStatus())) {
                    System.out.println("[SQL INFO] Authentication successful for user: " + username);
                    return user;
                } else {
                    System.out.println("[SQL INFO] Authentication failed: Account is locked or inactive (" + user.getStatus() + ")");
                }
            } else {
                System.out.println("[SQL INFO] Authentication failed: Incorrect password for user: " + username);
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
