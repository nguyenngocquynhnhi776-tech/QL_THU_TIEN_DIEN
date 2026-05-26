package dao.impl;

import dao.HouseholdDAO;
import database.DatabaseConnection;
import model.Household;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of HouseholdDAO interface using SQL Server JDBC.
 */
public class HouseholdDAOImpl implements HouseholdDAO {

    @Override
    public Household findById(int id) {
        String sql = "SELECT h.*, a.AreaCode, a.AreaName "
                   + "FROM HOUSEHOLD h "
                   + "LEFT JOIN AREA a ON h.AreaID = a.AreaID "
                   + "WHERE h.HouseholdID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToHousehold(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("HouseholdDAOImpl.findById error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Household findByCode(String code) {
        String sql = "SELECT h.*, a.AreaCode, a.AreaName "
                   + "FROM HOUSEHOLD h "
                   + "LEFT JOIN AREA a ON h.AreaID = a.AreaID "
                   + "WHERE h.HouseholdCode = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, code);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToHousehold(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("HouseholdDAOImpl.findByCode error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Household> getAll() {
        List<Household> list = new ArrayList<>();
        String sql = "SELECT h.*, a.AreaCode, a.AreaName "
                   + "FROM HOUSEHOLD h "
                   + "LEFT JOIN AREA a ON h.AreaID = a.AreaID "
                   + "WHERE h.Status != 'INACTIVE' "
                   + "ORDER BY h.HouseholdCode";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                list.add(mapResultSetToHousehold(rs));
            }
        } catch (SQLException e) {
            System.err.println("HouseholdDAOImpl.getAll error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Household> search(String keyword, Integer areaId) {
        List<Household> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT h.*, a.AreaCode, a.AreaName " +
            "FROM HOUSEHOLD h " +
            "LEFT JOIN AREA a ON h.AreaID = a.AreaID " +
            "WHERE h.Status != 'INACTIVE'"
        );
        
        List<Object> params = new ArrayList<>();
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            String pattern = "%" + keyword.trim() + "%";
            sql.append(" AND (h.HouseholdCode LIKE ? OR h.OwnerName LIKE ? OR h.Phone LIKE ? OR h.Address LIKE ? OR a.AreaCode LIKE ? OR a.AreaName LIKE ?)");
            for (int i = 0; i < 6; i++) {
                params.add(pattern);
            }
        }
        
        if (areaId != null) {
            sql.append(" AND h.AreaID = ?");
            params.add(areaId);
        }
        
        sql.append(" ORDER BY h.HouseholdCode");
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer) {
                    pstmt.setInt(i + 1, (Integer) p);
                } else if (p instanceof String) {
                    pstmt.setString(i + 1, (String) p);
                }
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToHousehold(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("HouseholdDAOImpl.search error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return list;
    }

    @Override
    public boolean insert(Household hh) {
        String sql = "INSERT INTO HOUSEHOLD (HouseholdCode, OwnerName, Address, Phone, AreaID, Status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, hh.getHouseholdCode());
            pstmt.setString(2, hh.getOwnerName());
            pstmt.setString(3, hh.getAddress());
            pstmt.setString(4, hh.getPhone());
            pstmt.setInt(5, hh.getAreaId());
            pstmt.setString(6, hh.getStatus());
            
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("HouseholdDAOImpl.insert error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean update(Household hh) {
        String sql = "UPDATE HOUSEHOLD SET OwnerName = ?, Address = ?, Phone = ?, AreaID = ?, Status = ? WHERE HouseholdID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, hh.getOwnerName());
            pstmt.setString(2, hh.getAddress());
            pstmt.setString(3, hh.getPhone());
            pstmt.setInt(4, hh.getAreaId());
            pstmt.setString(5, hh.getStatus());
            pstmt.setInt(6, hh.getHouseholdId());
            
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("HouseholdDAOImpl.update error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean deactivate(int hhId) {
        String sql = "UPDATE HOUSEHOLD SET Status = 'INACTIVE' WHERE HouseholdID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, hhId);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("HouseholdDAOImpl.deactivate error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String getHighestCodeForArea(String areaCode) {
        String sql = "SELECT TOP 1 HouseholdCode FROM HOUSEHOLD WHERE HouseholdCode LIKE ? ORDER BY HouseholdCode DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, areaCode + "-%");
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("HouseholdCode");
                }
            }
        } catch (SQLException e) {
            System.err.println("HouseholdDAOImpl.getHighestCodeForArea error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private Household mapResultSetToHousehold(ResultSet rs) throws SQLException {
        Household hh = new Household();
        hh.setHouseholdId(rs.getInt("HouseholdID"));
        hh.setHouseholdCode(rs.getString("HouseholdCode"));
        hh.setOwnerName(rs.getString("OwnerName"));
        hh.setAddress(rs.getString("Address"));
        hh.setPhone(rs.getString("Phone"));
        hh.setAreaId(rs.getInt("AreaID"));
        hh.setStatus(rs.getString("Status"));
        hh.setCreatedAt(rs.getTimestamp("CreatedAt"));
        
        // Map extra joined fields
        hh.setAreaCode(rs.getString("AreaCode"));
        hh.setAreaName(rs.getString("AreaName"));
        return hh;
    }
}
