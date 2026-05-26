package dao;

import java.util.List;
import model.Area;

/**
 * Data Access Object (DAO) interface for AREA table.
 */
public interface AreaDAO {

    /**
     * Finds an Area by its ID.
     */
    Area findById(int id);

    /**
     * Finds an Area by its Code.
     */
    Area findByCode(String code);

    /**
     * Returns a list of all areas in the system.
     */
    List<Area> getAll();

    /**
     * Returns a list of active areas in the system.
     */
    List<Area> getAllActive();

    /**
     * Inserts a new Area.
     */
    boolean insert(Area area);

    /**
     * Updates an existing Area.
     */
    boolean update(Area area);

    /**
     * Deactivates an Area (soft delete, sets Status = 'INACTIVE').
     */
    boolean deactivate(int areaId);
}
