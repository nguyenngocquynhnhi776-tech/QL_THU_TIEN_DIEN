package service;

import model.PriceTier;
import java.util.List;

/**
 * Service interface for electricity pricing tier management.
 */
public interface PriceTierService {

    List<PriceTier> getAllTiers();

    /**
     * Calculates total electricity bill amount from tiered pricing.
     * All tier data comes from the database.
     * @param consumptionKwh total kWh consumed
     * @return total amount in VND
     */
    double calculateTotal(double consumptionKwh);

    /** @return null on success, error string on failure */
    String addTier(PriceTier tier);

    /** @return null on success, error string on failure */
    String updateTier(PriceTier tier);

    boolean deleteTier(int priceId);
}
