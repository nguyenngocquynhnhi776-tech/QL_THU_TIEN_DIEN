package dao;

import model.Bill;
import java.util.List;

/**
 * DAO interface for BILL table.
 */
public interface BillDAO {

    Bill findById(int id);

    Bill findByCode(String billCode);

    /** All bills, newest first. */
    List<Bill> getAll();

    /**
     * Search/filter bills.
     * @param keyword searches BillCode, HouseholdCode, OwnerName
     * @param paymentStatus "PAID", "UNPAID", or null for all
     * @param month filter month or null
     * @param year  filter year  or null
     */
    List<Bill> search(String keyword, String paymentStatus, Integer month, Integer year);

    boolean insert(Bill bill);

    boolean updatePaymentStatus(int billId, String newStatus);

    /**
     * Generates the next BillCode: HD + YYYYMM + 4-digit running number.
     * Example: HD2026050001
     */
    String getNextBillCode(int month, int year);

    /** Count of bills with a given paymentStatus. */
    int countByStatus(String paymentStatus);

    /** Sum of TotalAmount for bills with a given paymentStatus. */
    double sumTotalByStatus(String paymentStatus);

    /** Checks if a bill already exists for a given readingId. */
    boolean existsForReading(int readingId);

    /** Returns the bill for a given readingId, or null. */
    Bill findByReading(int readingId);

    /** Updates the TotalAmount (and optionally resets PaymentStatus) for an existing bill. */
    boolean updateAmount(int billId, double newTotal);
}
