package dao.impl;

import dao.NotificationDAO;
import database.DatabaseConnection;
import model.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of NotificationDAO interface using SQL Server JDBC.
 */
public class NotificationDAOImpl implements NotificationDAO {

    @Override
    public boolean insert(Notification notif) {
        String sql = "INSERT INTO NOTIFICATION (Title, Content, Type, Icon, IsRead, CreatedBy, TargetUser, RelatedEntity, RelatedID) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, notif.getTitle());
            pstmt.setString(2, notif.getContent());
            pstmt.setString(3, notif.getType());
            pstmt.setString(4, notif.getIcon());
            pstmt.setBoolean(5, notif.isRead());
            pstmt.setString(6, notif.getCreatedBy());
            pstmt.setString(7, notif.getTargetUser());
            pstmt.setString(8, notif.getRelatedEntity());
            if (notif.getRelatedId() != null) {
                pstmt.setInt(9, notif.getRelatedId());
            } else {
                pstmt.setNull(9, Types.INTEGER);
            }

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("NotificationDAOImpl.insert error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean delete(int notifId) {
        String sql = "DELETE FROM NOTIFICATION WHERE NotificationID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, notifId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("NotificationDAOImpl.delete error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean deleteAll() {
        String sql = "DELETE FROM NOTIFICATION";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("NotificationDAOImpl.deleteAll error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean markAsRead(int notifId) {
        String sql = "UPDATE NOTIFICATION SET IsRead = 1 WHERE NotificationID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, notifId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("NotificationDAOImpl.markAsRead error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean markAllAsRead() {
        String sql = "UPDATE NOTIFICATION SET IsRead = 1 WHERE IsRead = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("NotificationDAOImpl.markAllAsRead error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Notification> getFiltered(String type, Boolean unreadOnly, int limit, int offset) {
        List<Notification> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM NOTIFICATION WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (type != null && !type.equalsIgnoreCase("Tất cả") && !type.isBlank()) {
            sql.append(" AND Type = ?");
            params.add(type.toLowerCase());
        }

        if (unreadOnly != null && unreadOnly) {
            sql.append(" AND IsRead = 0");
        }

        sql.append(" ORDER BY CreatedAt DESC, NotificationID DESC");

        // Pagination for SQL Server
        sql.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(limit);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            for (Object param : params) {
                pstmt.setObject(idx++, param);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("NotificationDAOImpl.getFiltered error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public int getUnreadCount() {
        String sql = "SELECT COUNT(*) FROM NOTIFICATION WHERE IsRead = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("NotificationDAOImpl.getUnreadCount error: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    private Notification mapResultSet(ResultSet rs) throws SQLException {
        Notification notif = new Notification();
        notif.setNotificationId(rs.getInt("NotificationID"));
        notif.setTitle(rs.getString("Title"));
        notif.setContent(rs.getString("Content"));
        notif.setType(rs.getString("Type"));
        notif.setIcon(rs.getString("Icon"));
        notif.setRead(rs.getBoolean("IsRead"));
        notif.setCreatedAt(rs.getTimestamp("CreatedAt"));
        notif.setCreatedBy(rs.getString("CreatedBy"));
        notif.setTargetUser(rs.getString("TargetUser"));
        notif.setRelatedEntity(rs.getString("RelatedEntity"));
        int relId = rs.getInt("RelatedID");
        if (!rs.wasNull()) {
            notif.setRelatedId(relId);
        }
        return notif;
    }
}
