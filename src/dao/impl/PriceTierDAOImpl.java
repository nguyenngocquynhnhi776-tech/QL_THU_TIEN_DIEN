package dao.impl;

import dao.PriceTierDAO;
import database.DatabaseConnection;
import model.PriceTier;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL Server implementation of PriceTierDAO.
 */
public class PriceTierDAOImpl implements PriceTierDAO {

    @Override
    public List<PriceTier> getAll() {
        List<PriceTier> list = new ArrayList<>();
        String sql = "SELECT * FROM PRICE_TIER ORDER BY LevelFrom ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("PriceTierDAOImpl.getAll: " + e.getMessage());
        }
        return list;
    }

    @Override
    public boolean insert(PriceTier tier) {
        String sql = "INSERT INTO PRICE_TIER (LevelFrom, LevelTo, UnitPrice) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tier.getLevelFrom());
            ps.setInt(2, tier.getLevelTo());
            ps.setDouble(3, tier.getUnitPrice());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("PriceTierDAOImpl.insert: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean update(PriceTier tier) {
        String sql = "UPDATE PRICE_TIER SET LevelFrom = ?, LevelTo = ?, UnitPrice = ? WHERE PriceID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tier.getLevelFrom());
            ps.setInt(2, tier.getLevelTo());
            ps.setDouble(3, tier.getUnitPrice());
            ps.setInt(4, tier.getPriceId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("PriceTierDAOImpl.update: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean delete(int priceId) {
        String sql = "DELETE FROM PRICE_TIER WHERE PriceID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, priceId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("PriceTierDAOImpl.delete: " + e.getMessage());
        }
        return false;
    }

    private PriceTier map(ResultSet rs) throws SQLException {
        PriceTier t = new PriceTier();
        t.setPriceId(rs.getInt("PriceID"));
        t.setLevelFrom(rs.getInt("LevelFrom"));
        t.setLevelTo(rs.getInt("LevelTo"));
        t.setUnitPrice(rs.getDouble("UnitPrice"));
        return t;
    }
}
