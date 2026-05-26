package service.impl;

import dao.PriceTierDAO;
import dao.impl.PriceTierDAOImpl;
import model.PriceTier;
import service.PriceTierService;

import java.util.List;

/**
 * Business logic for electricity pricing tiers.
 * All tier data comes from the PRICE_TIER table in SQL Server.
 */
public class PriceTierServiceImpl implements PriceTierService {

    private final PriceTierDAO dao;

    public PriceTierServiceImpl() {
        this.dao = new PriceTierDAOImpl();
    }

    public PriceTierServiceImpl(PriceTierDAO dao) {
        this.dao = dao;
    }

    @Override
    public List<PriceTier> getAllTiers() {
        return dao.getAll();
    }

    /**
     * Applies tiered pricing from the database.
     *
     * Example with EVN tiers (kWh ranges):
     *   Tier 1: 0–50    → 1806 đ/kWh
     *   Tier 2: 51–100  → 1866 đ/kWh
     *   Tier 3: 101–200 → 2167 đ/kWh
     *   Tier 4: 201–300 → 2729 đ/kWh
     *   Tier 5: 301–400 → 3050 đ/kWh
     *   Tier 6: 401+    → 3151 đ/kWh  (LevelTo stored as -1)
     *
     * For each tier, consumption in that band = min(newIndex, tierEnd) - tierStart
     */
    @Override
    public double calculateTotal(double consumptionKwh) {
        if (consumptionKwh <= 0) return 0;

        List<PriceTier> tiers = dao.getAll();
        if (tiers.isEmpty()) return 0;

        double total     = 0;
        double remaining = consumptionKwh;

        for (PriceTier tier : tiers) {
            if (remaining <= 0) break;

            int from = tier.getLevelFrom();
            int to   = tier.getLevelTo();  // -1 means unlimited

            double bandSize;
            if (to < 0) {
                // Unlimited (last tier)
                bandSize = remaining;
            } else {
                bandSize = Math.min(remaining, to - from + 1);
            }

            if (bandSize > 0) {
                total     += bandSize * tier.getUnitPrice();
                remaining -= bandSize;
            }
        }
        return total;
    }

    @Override
    public String addTier(PriceTier tier) {
        if (tier == null)               return "Dữ liệu bậc giá không hợp lệ!";
        if (tier.getLevelFrom() < 0)    return "Mức từ không được âm!";
        if (tier.getUnitPrice() <= 0)   return "Đơn giá phải lớn hơn 0!";
        boolean ok = dao.insert(tier);
        return ok ? null : "Lỗi thêm bậc giá!";
    }

    @Override
    public String updateTier(PriceTier tier) {
        if (tier == null)               return "Dữ liệu bậc giá không hợp lệ!";
        if (tier.getUnitPrice() <= 0)   return "Đơn giá phải lớn hơn 0!";
        boolean ok = dao.update(tier);
        return ok ? null : "Lỗi cập nhật bậc giá!";
    }

    @Override
    public boolean deleteTier(int priceId) {
        return dao.delete(priceId);
    }
}
