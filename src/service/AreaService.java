package service;

import java.util.List;
import model.Area;

/**
 * Service interface for Area management business logic.
 */
public interface AreaService {

    Area getById(int id);
    Area getByCode(String code);
    List<Area> getAll();
    List<Area> getAllActive();
    
    /**
     * Validates and inserts a new Area.
     * Checks:
     * - Duplicate AreaCode
     * - Empty AreaCode or AreaName
     */
    String addArea(Area area);

    /**
     * Validates and updates an existing Area.
     */
    String updateArea(Area area);

    /**
     * Deactivates an Area (Status = 'INACTIVE').
     */
    boolean deactivateArea(int id);
}
