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

    /**
     * Saves a meter reading and automatically generates a bill.
     * If a reading already exists for the same household/month/year,
     * prompts via the returned message so the caller can decide to update.
     *
     * @return a SaveResult containing the outcome.
     */
    SaveResult addReadingAndGenerateBill(MeterReading reading);

    /**
     * Updates an existing duplicate reading and regenerates/updates its bill.
     * @param existingReadingId the ReadingID of the row to overwrite.
     */
    SaveResult updateReadingAndBill(int existingReadingId, MeterReading newData);

    /** Result object returned by addReadingAndGenerateBill / updateReadingAndBill. */
    class SaveResult {
        public enum Status { SUCCESS, DUPLICATE, ERROR }
        public final Status status;
        public final String message;    // error or info text
        public final int    readingId;  // -1 if not available

        public SaveResult(Status status, String message, int readingId) {
            this.status    = status;
            this.message   = message;
            this.readingId = readingId;
        }
    }
}
