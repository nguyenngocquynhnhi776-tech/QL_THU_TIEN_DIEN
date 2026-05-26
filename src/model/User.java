package model;

import java.sql.Timestamp;

/**
 * User domain model — maps to the USERS table in SQL Server.
 */
public class User {

    private int       userId;
    private String    username;
    private String    passwordHash;
    private String    fullName;
    private String    role;
    private String    status;
    private Timestamp createdAt;

    // -------------------------------------------------------
    // Constructors
    // -------------------------------------------------------

    public User() {}

    /** Full constructor (used when reading from DB). */
    public User(int userId, String username, String passwordHash,
                String fullName, String role, String status, Timestamp createdAt) {
        this.userId       = userId;
        this.username     = username;
        this.passwordHash = passwordHash;
        this.fullName     = fullName;
        this.role         = role;
        this.status       = status;
        this.createdAt    = createdAt;
    }

    /** Convenience constructor for creating a new user (no ID yet). */
    public User(String username, String passwordHash, String fullName,
                String role, String status) {
        this.username     = username;
        this.passwordHash = passwordHash;
        this.fullName     = fullName;
        this.role         = role;
        this.status       = status;
    }

    // -------------------------------------------------------
    // Getters
    // -------------------------------------------------------

    public int       getUserId()       { return userId; }
    public String    getUsername()     { return username; }
    public String    getPasswordHash() { return passwordHash; }
    public String    getFullName()     { return fullName; }
    public String    getRole()        { return role; }
    public String    getStatus()      { return status; }
    public Timestamp getCreatedAt()   { return createdAt; }

    // -------------------------------------------------------
    // Setters
    // -------------------------------------------------------

    public void setUserId(int userId)             { this.userId = userId; }
    public void setUsername(String username)       { this.username = username; }
    public void setPasswordHash(String hash)       { this.passwordHash = hash; }
    public void setFullName(String fullName)       { this.fullName = fullName; }
    public void setRole(String role)               { this.role = role; }
    public void setStatus(String status)           { this.status = status; }
    public void setCreatedAt(Timestamp createdAt)  { this.createdAt = createdAt; }

    // -------------------------------------------------------
    // Utility
    // -------------------------------------------------------

    /** Returns a display-friendly role label in Vietnamese. */
    public String getRoleDisplay() {
        if (role == null) return "Không xác định";
        switch (role.toUpperCase()) {
            case "ADMIN":   return "Quản trị viên";
            case "MANAGER": return "Quản lý";
            case "STAFF":   return "Nhân viên";
            case "VIEWER":  return "Xem báo cáo";
            default:        return role;
        }
    }

    /** Returns a display-friendly status label in Vietnamese. */
    public String getStatusDisplay() {
        if (status == null) return "Không xác định";
        switch (status.toUpperCase()) {
            case "ACTIVE":   return "Hoạt động";
            case "LOCKED":   return "Bị khóa";
            case "INACTIVE": return "Đã xóa";
            default:         return status;
        }
    }

    @Override
    public String toString() {
        return "User{userId=" + userId
             + ", username='" + username + '\''
             + ", fullName='" + fullName + '\''
             + ", role='" + role + '\''
             + ", status='" + status + '\''
             + '}';
    }
}
