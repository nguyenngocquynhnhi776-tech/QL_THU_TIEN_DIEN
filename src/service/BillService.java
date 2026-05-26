package service;

import model.Bill;
import java.util.List;

/**
 * Service interface for Bill generation and management.
 */
public interface BillService {

    Bill getById(int id);

    Bill getByCode(String billCode);

    List<Bill> getAll();

    List<Bill> search(String keyword, String paymentStatus, Integer month, Integer year);

    /**
     * Generates a bill from an existing MeterReading.
     * Uses live PRICE_TIER data from the database to compute total.
     * BillCode format: HD + YYYYMM + 4-digit running number (e.g. HD2026050001).
     * @return null on success, error string on failure
     */
    String generateBill(int readingId);

    int countUnpaid();

    int countPaid();

    double sumUnpaid();

    double sumPaid();
}
