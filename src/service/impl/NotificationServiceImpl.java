package service.impl;

import dao.NotificationDAO;
import dao.impl.NotificationDAOImpl;
import database.DatabaseConnection;
import model.Notification;
import service.NotificationService;
import session.UserSession;

import java.sql.*;
import java.util.Calendar;
import java.util.List;

/**
 * Implementation of NotificationService with robust database-driven audit engine.
 */
public class NotificationServiceImpl implements NotificationService {

    private final NotificationDAO dao = new NotificationDAOImpl();

    @Override
    public void addNotification(Notification notif) {
        if (notif.getCreatedBy() == null) {
            String currentUser = UserSession.getInstance().getUsername();
            notif.setCreatedBy(currentUser != null ? currentUser : "SYSTEM");
        }
        dao.insert(notif);
    }

    @Override
    public void addNotification(String title, String content, String type, String icon) {
        Notification notif = new Notification(title, content, type, icon);
        addNotification(notif);
    }

    @Override
    public void addNotification(String title, String content, String type, String icon, String relatedEntity, Integer relatedId) {
        Notification notif = new Notification(title, content, type, icon);
        notif.setRelatedEntity(relatedEntity);
        notif.setRelatedId(relatedId);
        addNotification(notif);
    }

    @Override
    public boolean deleteNotification(int notifId) {
        return dao.delete(notifId);
    }

    @Override
    public boolean clearAll() {
        return dao.deleteAll();
    }

    @Override
    public boolean markAsRead(int notifId) {
        return dao.markAsRead(notifId);
    }

    @Override
    public boolean markAllRead() {
        return dao.markAllAsRead();
    }

    @Override
    public List<Notification> getNotifications(String type, Boolean unreadOnly, int limit, int offset) {
        return dao.getFiltered(type, unreadOnly, limit, offset);
    }

    @Override
    public int getUnreadCount() {
        return dao.getUnreadCount();
    }

    @Override
    public void runSystemHealthAudit() {
        // Runs a background SQL audit to generate real alerts based on database records.
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) return;

            // Step 1: Clean up previous system audit notifications to prevent duplicate spamming
            try (PreparedStatement deletePs = conn.prepareStatement(
                    "DELETE FROM NOTIFICATION WHERE CreatedBy = 'SYSTEM_AUDIT'")) {
                deletePs.executeUpdate();
            }

            // Get current month and year
            Calendar cal = Calendar.getInstance();
            int month = cal.get(Calendar.MONTH) + 1;
            int year  = cal.get(Calendar.YEAR);

            // ─── AUDIT 1: Meter reading entries progress ──────────────────
            int totalHH = 0;
            int enteredReadings = 0;

            try (Statement stmt = conn.createStatement()) {
                // Active households
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM HOUSEHOLD WHERE Status = 'ACTIVE'")) {
                    if (rs.next()) totalHH = rs.getInt(1);
                }
                // Entered readings
                String enteredSql = "SELECT COUNT(*) FROM METER_READING WHERE Month = " + month + " AND Year = " + year;
                try (ResultSet rs = stmt.executeQuery(enteredSql)) {
                    if (rs.next()) enteredReadings = rs.getInt(1);
                }
            }

            if (enteredReadings > 0) {
                insertAudit(conn, "Chỉ số điện tháng " + month + "/" + year,
                        "Đã nhập chỉ số điện cho " + enteredReadings + " hộ gia đình thành công.",
                        "success", "check-circle");
            }

            int missingReadings = totalHH - enteredReadings;
            if (missingReadings > 0) {
                insertAudit(conn, "Chỉ số điện chưa nhập",
                        "Còn " + missingReadings + " hộ chưa nhập chỉ số điện tháng " + month + "/" + year + ".",
                        "warning", "alert-triangle");
            }

            // ─── AUDIT 2: Mismatched indexes (new_index < old_index) ──────
            String mismatchSql = "SELECT h.HouseholdCode, h.OwnerName, r.Month, r.Year, r.OldIndex, r.NewIndex "
                               + "FROM METER_READING r "
                               + "JOIN HOUSEHOLD h ON r.HouseholdID = h.HouseholdID "
                               + "WHERE r.NewIndex < r.OldIndex";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(mismatchSql)) {
                while (rs.next()) {
                    String code = rs.getString("HouseholdCode");
                    String name = rs.getString("OwnerName");
                    int m = rs.getInt("Month");
                    int y = rs.getInt("Year");
                    double oldIdx = rs.getDouble("OldIndex");
                    double newIdx = rs.getDouble("NewIndex");

                    insertAudit(conn, "Chỉ số điện bất thường",
                            "Hộ " + name + " (" + code + ") có chỉ số mới (" + String.format("%.0f", newIdx)
                            + ") nhỏ hơn chỉ số cũ (" + String.format("%.0f", oldIdx) + ") trong tháng " + m + "/" + y + ".",
                            "error", "x-circle");
                }
            }

            // ─── AUDIT 3: Consumption Surge (> 1.5x previous month) ────────
            String surgeSql = "SELECT h.HouseholdCode, h.OwnerName, r1.Month, r1.Year, r1.Consumption AS curr_c, r2.Consumption AS prev_c "
                            + "FROM METER_READING r1 "
                            + "JOIN HOUSEHOLD h ON r1.HouseholdID = h.HouseholdID "
                            + "JOIN METER_READING r2 ON r1.HouseholdID = r2.HouseholdID "
                            + "  AND ((r1.Month = 1 AND r2.Month = 12 AND r2.Year = r1.Year - 1) "
                            + "       OR (r1.Month > 1 AND r2.Month = r1.Month - 1 AND r2.Year = r1.Year)) "
                            + "WHERE r1.Consumption > r2.Consumption * 1.5 AND r2.Consumption > 0";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(surgeSql)) {
                while (rs.next()) {
                    String code = rs.getString("HouseholdCode");
                    String name = rs.getString("OwnerName");
                    double currC = rs.getDouble("curr_c");
                    double prevC = rs.getDouble("prev_c");
                    double percent = ((currC - prevC) / prevC) * 100;

                    insertAudit(conn, "Chỉ số điện tăng bất thường",
                            "Hộ " + name + " (" + code + ") tiêu thụ " + String.format("%.0f", currC)
                            + " kWh, tăng " + String.format("%.0f", percent) + "% so với tháng trước (" + String.format("%.0f", prevC) + " kWh).",
                            "warning", "alert-triangle");
                }
            }

            // ─── AUDIT 4: Unpaid Invoices count ───────────────────────────
            int unpaidBills = 0;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM BILL WHERE PaymentStatus = 'UNPAID'")) {
                if (rs.next()) unpaidBills = rs.getInt(1);
            }

            if (unpaidBills > 0) {
                insertAudit(conn, "Hóa đơn chưa thanh toán",
                        unpaidBills + " hộ gia đình chưa thanh toán hóa đơn tiền điện.",
                        "warning", "alert-triangle");
            }

            // ─── AUDIT 5: Multi-month debts (3+ consecutive months unpaid) 
            String debtSql = "SELECT h.HouseholdCode, h.OwnerName, COUNT(b.BillID) AS unpaid_months "
                           + "FROM BILL b "
                           + "JOIN HOUSEHOLD h ON b.HouseholdID = h.HouseholdID "
                           + "WHERE b.PaymentStatus = 'UNPAID' "
                           + "GROUP BY h.HouseholdCode, h.OwnerName "
                           + "HAVING COUNT(b.BillID) >= 3";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(debtSql)) {
                while (rs.next()) {
                    String code = rs.getString("HouseholdCode");
                    String name = rs.getString("OwnerName");
                    int months = rs.getInt("unpaid_months");

                    insertAudit(conn, "Nợ tiền điện nhiều tháng",
                            "Hộ " + name + " (" + code + ") nợ tiền điện " + months + " kỳ liên tiếp.",
                            "warning", "alert-triangle");
                }
            }

        } catch (SQLException e) {
            System.err.println("NotificationServiceImpl.runSystemHealthAudit error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    private void insertAudit(Connection conn, String title, String content, String type, String icon) throws SQLException {
        String sql = "INSERT INTO NOTIFICATION (Title, Content, Type, Icon, IsRead, CreatedBy) "
                   + "VALUES (?, ?, ?, ?, 0, 'SYSTEM_AUDIT')";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, content);
            pstmt.setString(3, type);
            pstmt.setString(4, icon);
            pstmt.executeUpdate();
        }
    }
}
