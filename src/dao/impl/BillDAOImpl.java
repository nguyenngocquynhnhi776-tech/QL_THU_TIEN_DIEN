package dao.impl;

import dao.BillDAO;
import database.DatabaseConnection;
import model.Bill;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL Server implementation of BillDAO.
 */
public class BillDAOImpl implements BillDAO {

    @Override
    public Bill findById(int id) {
        String sql = buildSelectSQL() + " WHERE b.BillID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            System.err.println("BillDAOImpl.findById: " + e.getMessage());
        }
        return null;
    }

    @Override
    public Bill findByCode(String billCode) {
        String sql = buildSelectSQL() + " WHERE b.BillCode = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, billCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            System.err.println("BillDAOImpl.findByCode: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Bill> getAll() {
        List<Bill> list = new ArrayList<>();
        String sql = buildSelectSQL() + " ORDER BY b.CreatedAt DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("BillDAOImpl.getAll: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Bill> search(String keyword, String paymentStatus, Integer month, Integer year) {
        List<Bill> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(buildSelectSQL() + " WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String pat = "%" + keyword.trim() + "%";
            sql.append(" AND (b.BillCode LIKE ? OR h.HouseholdCode LIKE ? OR h.OwnerName LIKE ?)");
            params.add(pat); params.add(pat); params.add(pat);
        }
        if (paymentStatus != null && !paymentStatus.isEmpty()) {
            sql.append(" AND b.PaymentStatus = ?");
            params.add(paymentStatus);
        }
        if (month != null) { sql.append(" AND mr.Month = ?"); params.add(month); }
        if (year  != null) { sql.append(" AND mr.Year = ?");  params.add(year);  }
        sql.append(" ORDER BY b.CreatedAt DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) setParam(ps, i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("BillDAOImpl.search: " + e.getMessage());
        }
        return list;
    }

    @Override
    public boolean insert(Bill bill) {
        String sql = "INSERT INTO BILL (BillCode, HouseholdID, ReadingID, TotalAmount, PaymentStatus) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bill.getBillCode());
            ps.setInt(2, bill.getHouseholdId());
            ps.setInt(3, bill.getReadingId());
            ps.setDouble(4, bill.getTotalAmount());
            ps.setString(5, bill.getPaymentStatus() != null ? bill.getPaymentStatus() : "UNPAID");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("BillDAOImpl.insert: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean updatePaymentStatus(int billId, String newStatus) {
        String sql = "UPDATE BILL SET PaymentStatus = ? WHERE BillID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, billId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("BillDAOImpl.updatePaymentStatus: " + e.getMessage());
        }
        return false;
    }

    @Override
    public String getNextBillCode(int month, int year) {
        // Format: HD + YYYYMM + 4-digit running number
        String prefix = String.format("HD%04d%02d", year, month);
        String sql = "SELECT TOP 1 BillCode FROM BILL WHERE BillCode LIKE ? ORDER BY BillCode DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String last = rs.getString("BillCode");
                    // last = "HD2026050042" → suffix "0042" → seq 43
                    try {
                        int seq = Integer.parseInt(last.substring(prefix.length())) + 1;
                        return String.format("%s%04d", prefix, seq);
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (SQLException e) {
            System.err.println("BillDAOImpl.getNextBillCode: " + e.getMessage());
        }
        return String.format("%s%04d", prefix, 1);
    }

    @Override
    public int countByStatus(String paymentStatus) {
        String sql = "SELECT COUNT(*) FROM BILL WHERE PaymentStatus = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paymentStatus);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("BillDAOImpl.countByStatus: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public double sumTotalByStatus(String paymentStatus) {
        String sql = "SELECT ISNULL(SUM(TotalAmount), 0) FROM BILL WHERE PaymentStatus = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paymentStatus);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("BillDAOImpl.sumTotalByStatus: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean existsForReading(int readingId) {
        String sql = "SELECT COUNT(*) FROM BILL WHERE ReadingID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, readingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("BillDAOImpl.existsForReading: " + e.getMessage());
        }
        return false;
    }

    // ---- Helpers ----

    private String buildSelectSQL() {
        return "SELECT b.*, h.HouseholdCode, h.OwnerName, a.AreaName, " +
               "mr.Month, mr.Year, mr.Consumption " +
               "FROM BILL b " +
               "JOIN HOUSEHOLD h ON b.HouseholdID = h.HouseholdID " +
               "LEFT JOIN AREA a ON h.AreaID = a.AreaID " +
               "JOIN METER_READING mr ON b.ReadingID = mr.ReadingID";
    }

    private Bill map(ResultSet rs) throws SQLException {
        Bill b = new Bill();
        b.setBillId(rs.getInt("BillID"));
        b.setBillCode(rs.getString("BillCode"));
        b.setHouseholdId(rs.getInt("HouseholdID"));
        b.setReadingId(rs.getInt("ReadingID"));
        b.setTotalAmount(rs.getDouble("TotalAmount"));
        b.setPaymentStatus(rs.getString("PaymentStatus"));
        b.setCreatedAt(rs.getTimestamp("CreatedAt"));
        b.setHouseholdCode(rs.getString("HouseholdCode"));
        b.setOwnerName(rs.getString("OwnerName"));
        b.setAreaName(rs.getString("AreaName"));
        b.setMonth(rs.getInt("Month"));
        b.setYear(rs.getInt("Year"));
        b.setConsumption(rs.getDouble("Consumption"));
        return b;
    }

    private void setParam(PreparedStatement ps, int idx, Object val) throws SQLException {
        if (val instanceof Integer) ps.setInt(idx, (Integer) val);
        else if (val instanceof String) ps.setString(idx, (String) val);
    }
}
