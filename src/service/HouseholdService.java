package service;

import java.util.List;
import model.Household;

/**
 * Service interface for Household management business logic.
 */
public interface HouseholdService {

    Household getById(int id);
    Household getByCode(String code);
    List<Household> getAll();
    
    /**
     * Searches and filters households.
     */
    List<Household> search(String keyword, Integer areaId);

    /**
     * Generates a new HouseholdCode for the specified AreaID.
     * Uses sequence based on the Area's AreaCode.
     */
    String generateHouseholdCode(int areaId);

    /**
     * Validates and inserts a new Household.
     * Generates the code automatically based on AreaID.
     */
    String addHousehold(Household hh);

    /**
     * Validates and updates an existing Household.
     */
    String updateHousehold(Household hh);

    /**
     * Deactivates a Household (soft delete).
     */
    boolean deactivateHousehold(int id);
}
