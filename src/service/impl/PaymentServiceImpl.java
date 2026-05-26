package service.impl;

import dao.BillDAO;
import dao.PaymentDAO;
import dao.impl.BillDAOImpl;
import dao.impl.PaymentDAOImpl;
import model.Bill;
import model.Payment;
import service.PaymentService;

import java.util.List;

/**
 * Business logic for payment recording.
 * Records payment and updates BILL.PaymentStatus to 'PAID'.
 */
public class PaymentServiceImpl implements PaymentService {

    private final PaymentDAO paymentDAO;
    private final BillDAO    billDAO;

    public PaymentServiceImpl() {
        this.paymentDAO = new PaymentDAOImpl();
        this.billDAO    = new BillDAOImpl();
    }

    public PaymentServiceImpl(PaymentDAO paymentDAO, BillDAO billDAO) {
        this.paymentDAO = paymentDAO;
        this.billDAO    = billDAO;
    }

    @Override
    public List<Payment> getRecentPayments(int limit) {
        return paymentDAO.getRecent(limit);
    }

    /**
     * Payment flow:
     *  1. Validate bill exists and is UNPAID.
     *  2. Insert PAYMENT row (amount, method, date = now).
     *  3. Update BILL.PaymentStatus = 'PAID'.
     */
    @Override
    public String recordPayment(Payment payment) {
        if (payment == null)             return "Thông tin thanh toán không hợp lệ!";
        if (payment.getBillId() <= 0)    return "Mã hóa đơn không hợp lệ!";
        if (payment.getAmount() <= 0)    return "Số tiền thanh toán phải lớn hơn 0!";
        if (payment.getPaymentMethod() == null || payment.getPaymentMethod().trim().isEmpty())
                                         return "Vui lòng chọn phương thức thanh toán!";

        Bill bill = billDAO.findById(payment.getBillId());
        if (bill == null)                return "Không tìm thấy hóa đơn!";
        if (bill.isPaid())               return "Hóa đơn " + bill.getBillCode() + " đã được thanh toán rồi!";

        boolean inserted = paymentDAO.insert(payment);
        if (!inserted)                   return "Lỗi lưu thông tin thanh toán!";

        boolean updated = billDAO.updatePaymentStatus(payment.getBillId(), "PAID");
        if (!updated)                    return "Lỗi cập nhật trạng thái hóa đơn!";

        return null; // success
    }

    @Override
    public double getTotalRevenue() {
        return paymentDAO.getTotalRevenue();
    }

    @Override
    public double getMonthlyRevenue(int month, int year) {
        return paymentDAO.getMonthlyRevenue(month, year);
    }

    @Override
    public double[] getRevenueLastNMonths(int n) {
        return paymentDAO.getRevenueLastNMonths(n);
    }
}
