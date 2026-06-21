package dao.impl;

import dao.PaymentDAO;
import database.DatabaseConnection;
import model.Payment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL Server implementation of PaymentDAO.
 */
public class PaymentDAOImpl implements PaymentDAO {

    @Override
    public List<Payment> findByBillId(int billId) {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT p.*, b.BillCode, h.HouseholdCode, h.OwnerName " +
                     "FROM PAYMENT p " +
                     "JOIN BILL b ON p.BillID = b.BillID " +
                     "JOIN HOUSEHOLD h ON b.HouseholdID = h.HouseholdID " +
                     "WHERE p.BillID = ? ORDER BY p.PaymentDate DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("PaymentDAOImpl.findByBillId: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Payment> getRecent(int limit) {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT TOP " + limit + " p.*, b.BillCode, h.HouseholdCode, h.OwnerName " +
                     "FROM PAYMENT p " +
                     "JOIN BILL b ON p.BillID = b.BillID " +
                     "JOIN HOUSEHOLD h ON b.HouseholdID = h.HouseholdID " +
                     "ORDER BY p.PaymentDate DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("PaymentDAOImpl.getRecent: " + e.getMessage());
        }
        return list;
    }

    @Override
    public boolean insert(Payment payment) {
        String sql = "INSERT INTO PAYMENT (BillID, Amount, PaymentMethod, PaymentDate, Note) " +
                     "VALUES (?, ?, ?, GETDATE(), ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, payment.getBillId());
            ps.setDouble(2, payment.getAmount());
            ps.setString(3, payment.getPaymentMethod());
            ps.setString(4, payment.getNote() != null ? payment.getNote() : "");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("PaymentDAOImpl.insert: " + e.getMessage());
        }
        return false;
    }

    @Override
    public double getTotalRevenue() {
        String sql = "SELECT ISNULL(SUM(Amount), 0) FROM PAYMENT";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            System.err.println("PaymentDAOImpl.getTotalRevenue: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public double getMonthlyRevenue(int month, int year) {
        String sql = "SELECT ISNULL(SUM(p.Amount), 0) FROM PAYMENT p " +
                     "JOIN BILL b ON p.BillID = b.BillID " +
                     "JOIN METER_READING mr ON b.ReadingID = mr.ReadingID " +
                     "WHERE mr.Month = ? AND mr.Year = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, month);
            ps.setInt(2, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("PaymentDAOImpl.getMonthlyRevenue: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public double[] getRevenueLastNMonths(int n) {
        double[] result = new double[n];
        // Tính từ tháng hiện tại lùi về N tháng (ví dụ n=6)
        String sql = "SELECT TOP " + n + " mr.Year AS yr, mr.Month AS mo, " +
                     "ISNULL(SUM(p.Amount), 0) AS total " +
                     "FROM PAYMENT p " +
                     "JOIN BILL b ON p.BillID = b.BillID " +
                     "JOIN METER_READING mr ON b.ReadingID = mr.ReadingID " +
                     "GROUP BY mr.Year, mr.Month " +
                     "ORDER BY yr DESC, mo DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int idx = n - 1;
            while (rs.next() && idx >= 0) {
                // Kết quả được ORDER BY DESC nên lưu từ cuối mảng lên đầu
                result[idx--] = rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.err.println("PaymentDAOImpl.getRevenueLastNMonths: " + e.getMessage());
        }
        return result;
    }

    private Payment map(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getInt("PaymentID"));
        p.setBillId(rs.getInt("BillID"));
        p.setAmount(rs.getDouble("Amount"));
        p.setPaymentMethod(rs.getString("PaymentMethod"));
        p.setPaymentDate(rs.getTimestamp("PaymentDate"));
        p.setNote(rs.getString("Note"));
        p.setBillCode(rs.getString("BillCode"));
        p.setHouseholdCode(rs.getString("HouseholdCode"));
        p.setOwnerName(rs.getString("OwnerName"));
        return p;
    }
}
