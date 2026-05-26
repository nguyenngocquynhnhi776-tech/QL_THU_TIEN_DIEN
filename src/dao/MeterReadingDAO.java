package dao;

import model.MeterReading;
import java.util.List;

/**
 * DAO interface for METER_READING table.
 */
public interface MeterReadingDAO {

    MeterReading findById(int id);

    /** All readings, newest first. */
    List<MeterReading> getAll();

    /**
     * Search by keyword (householdCode, ownerName) and/or period.
     * Pass null to skip a filter.
     */
    List<MeterReading> search(String keyword, Integer month, Integer year);

    boolean insert(MeterReading reading);

    /**
     * Returns an existing reading for the same household/month/year,
     * or null if none (used for duplicate check).
     */
    MeterReading findByHouseholdAndPeriod(int householdId, int month, int year);

    /**
     * Returns the most recent reading for a household (to pre-fill OldIndex).
     */
    MeterReading getLastReadingForHousehold(int householdId);
}
