package dao.impl;

import dao.AreaDAO;
import database.DatabaseConnection;
import model.Area;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of AreaDAO interface using SQL Server JDBC.
 */
public class AreaDAOImpl implements AreaDAO {

    @Override
    public Area findById(int id) {
        String sql = "SELECT * FROM AREA WHERE AreaID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToArea(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("AreaDAOImpl.findById error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Area findByCode(String code) {
        String sql = "SELECT * FROM AREA WHERE AreaCode = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, code);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToArea(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("AreaDAOImpl.findByCode error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Area> getAll() {
        List<Area> list = new ArrayList<>();
        String sql = "SELECT * FROM AREA ORDER BY AreaCode";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                list.add(mapResultSetToArea(rs));
            }
        } catch (SQLException e) {
            System.err.println("AreaDAOImpl.getAll error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Area> getAllActive() {
        List<Area> list = new ArrayList<>();
        String sql = "SELECT * FROM AREA WHERE Status = 'ACTIVE' ORDER BY AreaCode";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                list.add(mapResultSetToArea(rs));
            }
        } catch (SQLException e) {
            System.err.println("AreaDAOImpl.getAllActive error: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public boolean insert(Area area) {
        String sql = "INSERT INTO AREA (AreaCode, AreaName, Status) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, area.getAreaCode());
            pstmt.setString(2, area.getAreaName());
            pstmt.setString(3, area.getStatus());
            
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("AreaDAOImpl.insert error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean update(Area area) {
        String sql = "UPDATE AREA SET AreaName = ?, Status = ? WHERE AreaID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, area.getAreaName());
            pstmt.setString(2, area.getStatus());
            pstmt.setInt(3, area.getAreaId());
            
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("AreaDAOImpl.update error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean deactivate(int areaId) {
        String sql = "UPDATE AREA SET Status = 'INACTIVE' WHERE AreaID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, areaId);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("AreaDAOImpl.deactivate error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private Area mapResultSetToArea(ResultSet rs) throws SQLException {
        Area area = new Area();
        area.setAreaId(rs.getInt("AreaID"));
        area.setAreaCode(rs.getString("AreaCode"));
        area.setAreaName(rs.getString("AreaName"));
        area.setStatus(rs.getString("Status"));
        area.setCreatedAt(rs.getTimestamp("CreatedAt"));
        return area;
    }
}
