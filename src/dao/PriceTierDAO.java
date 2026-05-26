package dao;

import model.PriceTier;
import java.util.List;

/**
 * DAO interface for PRICE_TIER table.
 */
public interface PriceTierDAO {

    /** Returns all tiers ordered by LevelFrom ASC. */
    List<PriceTier> getAll();

    boolean insert(PriceTier tier);

    boolean update(PriceTier tier);

    boolean delete(int priceId);
}
