package util;

import database.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL Query helper for generating filtered tabular reports.
 * Uses PreparedStatement, closes resources properly, and prevents memory leaks.
 */
public class ReportQueryHelper {

    /**
     * Fetch Household Report data.
     */
    public static List<Object[]> getHouseholdReport(Integer areaId, String status) {
        List<Object[]> data = new ArrayList<>();
        String sql = "SELECT h.HouseholdCode, h.OwnerName, h.Address, h.Phone, a.AreaName, h.Status, h.CreatedAt " +
                     "FROM HOUSEHOLD h " +
                     "LEFT JOIN AREA a ON h.AreaID = a.AreaID " +
                     "WHERE 1=1";
        
        List<Object> params = new ArrayList<>();
        if (areaId != null && areaId > 0) {
            sql += " AND h.AreaID = ?";
            params.add(areaId);
        }
        if (status != null && !status.equalsIgnoreCase("Tất cả") && !status.isEmpty()) {
            sql += " AND h.Status = ?";
            params.add(status.equalsIgnoreCase("Hoạt động") ? "ACTIVE" : "INACTIVE");
        }
        sql += " ORDER BY h.HouseholdCode ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String statDisp = "ACTIVE".equalsIgnoreCase(rs.getString("Status")) ? "Hoạt động" : "Tạm khóa";
                    Timestamp t = rs.getTimestamp("CreatedAt");
                    String dateStr = t != null ? t.toString().substring(0, 10) : "";
                    
                    data.add(new Object[]{
                        rs.getString("HouseholdCode"),
                        rs.getString("OwnerName"),
                        rs.getString("Address"),
                        rs.getString("Phone"),
                        rs.getString("AreaName"),
                        statDisp,
                        dateStr
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("ReportQueryHelper.getHouseholdReport error: " + e.getMessage());
        }
        return data;
    }

    /**
     * Fetch Billing Report data.
     */
    public static List<Object[]> getBillingReport(Integer areaId, String status, Integer month, Integer year) {
        List<Object[]> data = new ArrayList<>();
        String sql = "SELECT b.BillCode, h.HouseholdCode, h.OwnerName, mr.Month, mr.Year, mr.Consumption, b.TotalAmount, b.PaymentStatus, b.CreatedAt " +
                     "FROM BILL b " +
                     "JOIN HOUSEHOLD h ON b.HouseholdID = h.HouseholdID " +
                     "JOIN METER_READING mr ON b.ReadingID = mr.ReadingID " +
                     "WHERE 1=1";

        List<Object> params = new ArrayList<>();
        if (areaId != null && areaId > 0) {
            sql += " AND h.AreaID = ?";
            params.add(areaId);
        }
        if (status != null && !status.equalsIgnoreCase("Tất cả") && !status.isEmpty()) {
            sql += " AND b.PaymentStatus = ?";
            params.add(status.equalsIgnoreCase("Đã thanh toán") ? "PAID" : "UNPAID");
        }
        if (month != null) {
            sql += " AND mr.Month = ?";
            params.add(month);
        }
        if (year != null) {
            sql += " AND mr.Year = ?";
            params.add(year);
        }
        sql += " ORDER BY b.BillCode DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String statDisp = "PAID".equalsIgnoreCase(rs.getString("PaymentStatus")) ? "Đã thanh toán" : "Chưa thanh toán";
                    Timestamp t = rs.getTimestamp("CreatedAt");
                    String dateStr = t != null ? t.toString().substring(0, 10) : "";
                    
                    data.add(new Object[]{
                        rs.getString("BillCode"),
                        rs.getString("HouseholdCode"),
                        rs.getString("OwnerName"),
                        String.format("%02d/%04d", rs.getInt("Month"), rs.getInt("Year")),
                        rs.getDouble("Consumption"),
                        rs.getDouble("TotalAmount"),
                        statDisp,
                        dateStr
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("ReportQueryHelper.getBillingReport error: " + e.getMessage());
        }
        return data;
    }

    /**
     * Fetch Revenue Report data (grouped by period).
     */
    public static List<Object[]> getRevenueReport(Integer year) {
        List<Object[]> data = new ArrayList<>();
        String sql = "SELECT mr.Year, mr.Month, SUM(mr.Consumption) AS total_kwh, " +
                     "SUM(CASE WHEN b.PaymentStatus = 'PAID' THEN b.TotalAmount ELSE 0 END) AS total_paid, " +
                     "SUM(CASE WHEN b.PaymentStatus = 'UNPAID' THEN b.TotalAmount ELSE 0 END) AS total_unpaid, " +
                     "COUNT(b.BillID) AS total_bills " +
                     "FROM BILL b " +
                     "JOIN METER_READING mr ON b.ReadingID = mr.ReadingID " +
                     "WHERE 1=1";

        List<Object> params = new ArrayList<>();
        if (year != null) {
            sql += " AND mr.Year = ?";
            params.add(year);
        }
        sql += " GROUP BY mr.Year, mr.Month ORDER BY mr.Year DESC, mr.Month DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int totalBills = rs.getInt("total_bills");
                    double paid = rs.getDouble("total_paid");
                    double unpaid = rs.getDouble("total_unpaid");
                    double totalMoney = paid + unpaid;
                    String pct = totalMoney > 0 ? String.format("%.1f%%", (paid * 100) / totalMoney) : "0%";

                    data.add(new Object[]{
                        String.format("%02d/%04d", rs.getInt("Month"), rs.getInt("Year")),
                        rs.getDouble("total_kwh"),
                        paid,
                        unpaid,
                        (double) totalBills,
                        pct
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("ReportQueryHelper.getRevenueReport error: " + e.getMessage());
        }
        return data;
    }

    /**
     * Fetch Payment Report data (individual payment transactions).
     */
    public static List<Object[]> getPaymentReport(Integer areaId, String fromDate, String toDate) {
        List<Object[]> data = new ArrayList<>();
        String sql = "SELECT p.PaymentID, b.BillCode, h.OwnerName, a.AreaName, p.Amount, p.PaymentMethod, p.PaymentDate, p.Note " +
                     "FROM PAYMENT p " +
                     "JOIN BILL b ON p.BillID = b.BillID " +
                     "JOIN HOUSEHOLD h ON b.HouseholdID = h.HouseholdID " +
                     "LEFT JOIN AREA a ON h.AreaID = a.AreaID " +
                     "WHERE 1=1";

        List<Object> params = new ArrayList<>();
        if (areaId != null && areaId > 0) {
            sql += " AND h.AreaID = ?";
            params.add(areaId);
        }
        if (fromDate != null && !fromDate.trim().isEmpty()) {
            sql += " AND p.PaymentDate >= ?";
            params.add(Timestamp.valueOf(fromDate.trim() + " 00:00:00"));
        }
        if (toDate != null && !toDate.trim().isEmpty()) {
            sql += " AND p.PaymentDate <= ?";
            params.add(Timestamp.valueOf(toDate.trim() + " 23:59:59"));
        }
        sql += " ORDER BY p.PaymentDate DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp t = rs.getTimestamp("PaymentDate");
                    String dateStr = t != null ? t.toString().substring(0, 16) : "";
                    
                    data.add(new Object[]{
                        (double) rs.getInt("PaymentID"),
                        rs.getString("BillCode"),
                        rs.getString("OwnerName"),
                        rs.getString("AreaName"),
                        rs.getDouble("Amount"),
                        rs.getString("PaymentMethod"),
                        dateStr,
                        rs.getString("Note")
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("ReportQueryHelper.getPaymentReport error: " + e.getMessage());
        }
        return data;
    }

    /**
     * Fetch Area aggregate report data.
     */
    public static List<Object[]> getAreaReport(Integer month, Integer year) {
        List<Object[]> data = new ArrayList<>();
        String sql = "SELECT a.AreaCode, a.AreaName, a.Status, " +
                     "COUNT(DISTINCT h.HouseholdID) AS total_hhs, " +
                     "ISNULL(SUM(mr.Consumption), 0) AS total_kwh, " +
                     "ISNULL(SUM(b.TotalAmount), 0) AS total_revenue " +
                     "FROM AREA a " +
                     "LEFT JOIN HOUSEHOLD h ON a.AreaID = h.AreaID " +
                     "LEFT JOIN METER_READING mr ON h.HouseholdID = mr.HouseholdID";
        
        List<Object> params = new ArrayList<>();
        if (month != null && year != null) {
            sql += " AND mr.Month = ? AND mr.Year = ?";
            params.add(month);
            params.add(year);
        }
        
        sql += " LEFT JOIN BILL b ON mr.ReadingID = b.ReadingID " +
               "GROUP BY a.AreaID, a.AreaCode, a.AreaName, a.Status " +
               "ORDER BY a.AreaCode ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String statDisp = "ACTIVE".equalsIgnoreCase(rs.getString("Status")) ? "Hoạt động" : "Khóa";
                    data.add(new Object[]{
                        rs.getString("AreaCode"),
                        rs.getString("AreaName"),
                        (double) rs.getInt("total_hhs"),
                        rs.getDouble("total_kwh"),
                        rs.getDouble("total_revenue"),
                        statDisp
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("ReportQueryHelper.getAreaReport error: " + e.getMessage());
        }
        return data;
    }
}
