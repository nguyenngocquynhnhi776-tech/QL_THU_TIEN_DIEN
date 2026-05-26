package dao;

import model.Payment;
import java.util.List;

/**
 * DAO interface for PAYMENT table.
 */
public interface PaymentDAO {

    List<Payment> findByBillId(int billId);

    /** N most recent payments, joined with BILL and HOUSEHOLD. */
    List<Payment> getRecent(int limit);

    boolean insert(Payment payment);

    /** Total revenue: SUM of all payment amounts. */
    double getTotalRevenue();

    /** Revenue for a specific month/year. */
    double getMonthlyRevenue(int month, int year);

    /** Monthly revenue for the last N months, ordered oldest first. */
    double[] getRevenueLastNMonths(int n);
}
