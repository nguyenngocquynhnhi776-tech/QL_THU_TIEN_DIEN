package dao;

import java.util.List;
import model.Household;

/**
 * Data Access Object (DAO) interface for HOUSEHOLD table.
 */
public interface HouseholdDAO {

    /**
     * Finds a Household by its ID.
     */
    Household findById(int id);

    /**
     * Finds a Household by its Code.
     */
    Household findByCode(String code);

    /**
     * Returns a list of all active or locked households.
     * Excludes soft-deleted/INACTIVE households.
     */
    List<Household> getAll();

    /**
     * Searches and filters households.
     *
     * @param keyword the search term (checks HouseholdCode, OwnerName, Phone)
     * @param areaId filter by AreaID (null for no filter)
     * @return List of matched Households
     */
    List<Household> search(String keyword, Integer areaId);

    /**
     * Inserts a new Household.
     */
    boolean insert(Household hh);

    /**
     * Updates an existing Household.
     */
    boolean update(Household hh);

    /**
     * Deactivates a Household (soft delete, sets Status = 'INACTIVE').
     */
    boolean deactivate(int hhId);

    /**
     * Finds the highest HouseholdCode matching the area code prefix (e.g. 'KV01-%').
     *
     * @param areaCode the area code prefix to match
     * @return the highest matched code, or null if none
     */
    String getHighestCodeForArea(String areaCode);
}
