package service;

import model.Payment;
import java.util.List;

/**
 * Service interface for Payment recording and revenue reporting.
 */
public interface PaymentService {

    List<Payment> getRecentPayments(int limit);

    /**
     * Records a payment for a bill:
     * 1. Inserts a PAYMENT row.
     * 2. Updates BILL.PaymentStatus = 'PAID'.
     * @return null on success, error string on failure
     */
    String recordPayment(Payment payment);

    double getTotalRevenue();

    double getMonthlyRevenue(int month, int year);

    /** Monthly revenue for the last N months, oldest first. */
    double[] getRevenueLastNMonths(int n);
}
