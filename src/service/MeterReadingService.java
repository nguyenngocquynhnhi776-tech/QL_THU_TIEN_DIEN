package service;

import model.MeterReading;
import java.util.List;

/**
 * Service interface for Meter Reading business logic.
 */
public interface MeterReadingService {

    MeterReading getById(int id);

    List<MeterReading> getAll();

    List<MeterReading> search(String keyword, Integer month, Integer year);

    /**
     * Returns the last reading for a household (to pre-fill OldIndex).
     */
    MeterReading getLastReading(int householdId);

    /**
     * Validates and saves a new meter reading.
     * @return null on success, error message string on failure.
     */
    String addReading(MeterReading reading);
}
