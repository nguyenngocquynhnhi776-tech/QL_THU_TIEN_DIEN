package dao.impl;

import dao.MeterReadingDAO;
import database.DatabaseConnection;
import model.MeterReading;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL Server implementation of MeterReadingDAO.
 */
public class MeterReadingDAOImpl implements MeterReadingDAO {

    @Override
    public MeterReading findById(int id) {
        String sql = "SELECT mr.*, h.HouseholdCode, h.OwnerName, a.AreaName " +
                     "FROM METER_READING mr " +
                     "JOIN HOUSEHOLD h ON mr.HouseholdID = h.HouseholdID " +
                     "LEFT JOIN AREA a ON h.AreaID = a.AreaID " +
                     "WHERE mr.ReadingID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            System.err.println("MeterReadingDAOImpl.findById: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<MeterReading> getAll() {
        List<MeterReading> list = new ArrayList<>();
        String sql = "SELECT mr.*, h.HouseholdCode, h.OwnerName, a.AreaName " +
                     "FROM METER_READING mr " +
                     "JOIN HOUSEHOLD h ON mr.HouseholdID = h.HouseholdID " +
                     "LEFT JOIN AREA a ON h.AreaID = a.AreaID " +
                     "ORDER BY mr.Year DESC, mr.Month DESC, mr.ReadingID DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("MeterReadingDAOImpl.getAll: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<MeterReading> search(String keyword, Integer month, Integer year) {
        List<MeterReading> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT mr.*, h.HouseholdCode, h.OwnerName, a.AreaName " +
            "FROM METER_READING mr " +
            "JOIN HOUSEHOLD h ON mr.HouseholdID = h.HouseholdID " +
            "LEFT JOIN AREA a ON h.AreaID = a.AreaID WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String pat = "%" + keyword.trim() + "%";
            sql.append(" AND (h.HouseholdCode LIKE ? OR h.OwnerName LIKE ?)");
            params.add(pat); params.add(pat);
        }
        if (month != null) { sql.append(" AND mr.Month = ?"); params.add(month); }
        if (year  != null) { sql.append(" AND mr.Year = ?");  params.add(year);  }
        sql.append(" ORDER BY mr.Year DESC, mr.Month DESC, mr.ReadingID DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) setParam(ps, i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            System.err.println("MeterReadingDAOImpl.search: " + e.getMessage());
        }
        return list;
    }

    @Override
    public boolean insert(MeterReading r) {
        String sql = "INSERT INTO METER_READING (HouseholdID, Month, Year, OldIndex, NewIndex, Consumption) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, r.getHouseholdId());
            ps.setInt(2, r.getMonth());
            ps.setInt(3, r.getYear());
            ps.setDouble(4, r.getOldIndex());
            ps.setDouble(5, r.getNewIndex());
            ps.setDouble(6, r.getConsumption());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("MeterReadingDAOImpl.insert: " + e.getMessage());
        }
        return false;
    }

    @Override
    public MeterReading findByHouseholdAndPeriod(int householdId, int month, int year) {
        String sql = "SELECT mr.*, h.HouseholdCode, h.OwnerName, a.AreaName " +
                     "FROM METER_READING mr " +
                     "JOIN HOUSEHOLD h ON mr.HouseholdID = h.HouseholdID " +
                     "LEFT JOIN AREA a ON h.AreaID = a.AreaID " +
                     "WHERE mr.HouseholdID = ? AND mr.Month = ? AND mr.Year = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, householdId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            System.err.println("MeterReadingDAOImpl.findByHouseholdAndPeriod: " + e.getMessage());
        }
        return null;
    }

    @Override
    public MeterReading getLastReadingForHousehold(int householdId) {
        String sql = "SELECT TOP 1 mr.*, h.HouseholdCode, h.OwnerName, a.AreaName " +
                     "FROM METER_READING mr " +
                     "JOIN HOUSEHOLD h ON mr.HouseholdID = h.HouseholdID " +
                     "LEFT JOIN AREA a ON h.AreaID = a.AreaID " +
                     "WHERE mr.HouseholdID = ? " +
                     "ORDER BY mr.Year DESC, mr.Month DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, householdId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            System.err.println("MeterReadingDAOImpl.getLastReadingForHousehold: " + e.getMessage());
        }
        return null;
    }

    // ---- Helpers ----

    private MeterReading map(ResultSet rs) throws SQLException {
        MeterReading r = new MeterReading();
        r.setReadingId(rs.getInt("ReadingID"));
        r.setHouseholdId(rs.getInt("HouseholdID"));
        r.setMonth(rs.getInt("Month"));
        r.setYear(rs.getInt("Year"));
        r.setOldIndex(rs.getDouble("OldIndex"));
        r.setNewIndex(rs.getDouble("NewIndex"));
        r.setConsumption(rs.getDouble("Consumption"));
        r.setCreatedAt(rs.getTimestamp("CreatedAt"));
        r.setHouseholdCode(rs.getString("HouseholdCode"));
        r.setOwnerName(rs.getString("OwnerName"));
        r.setAreaName(rs.getString("AreaName"));
        return r;
    }

    private void setParam(PreparedStatement ps, int idx, Object val) throws SQLException {
        if (val instanceof Integer) ps.setInt(idx, (Integer) val);
        else if (val instanceof String) ps.setString(idx, (String) val);
    }
}
