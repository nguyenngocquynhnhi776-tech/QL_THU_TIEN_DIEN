package service.impl;

import dao.BillDAO;
import dao.MeterReadingDAO;
import dao.impl.BillDAOImpl;
import dao.impl.MeterReadingDAOImpl;
import model.Bill;
import model.MeterReading;
import service.BillService;
import service.PriceTierService;

import java.util.List;

/**
 * Business logic for Bill generation.
 * Bill total is always computed from live PRICE_TIER data.
 * BillCode format: HD + YYYYMM + 4-digit running number (e.g. HD2026050001).
 */
public class BillServiceImpl implements BillService {

    private final BillDAO         billDAO;
    private final MeterReadingDAO readingDAO;
    private final PriceTierService priceTierService;

    public BillServiceImpl() {
        this.billDAO          = new BillDAOImpl();
        this.readingDAO       = new MeterReadingDAOImpl();
        this.priceTierService = new PriceTierServiceImpl();
    }

    public BillServiceImpl(BillDAO billDAO, MeterReadingDAO readingDAO,
                           PriceTierService priceTierService) {
        this.billDAO          = billDAO;
        this.readingDAO       = readingDAO;
        this.priceTierService = priceTierService;
    }

    @Override
    public Bill getById(int id) {
        return billDAO.findById(id);
    }

    @Override
    public Bill getByCode(String billCode) {
        if (billCode == null) return null;
        return billDAO.findByCode(billCode.trim().toUpperCase());
    }

    @Override
    public List<Bill> getAll() {
        return billDAO.getAll();
    }

    @Override
    public List<Bill> search(String keyword, String paymentStatus, Integer month, Integer year) {
        return billDAO.search(keyword, paymentStatus, month, year);
    }

    /**
     * Generates a bill from an existing METER_READING row.
     * Prevents duplicates — returns error string if bill already exists.
     */
    @Override
    public String generateBill(int readingId) {
        MeterReading reading = readingDAO.findById(readingId);
        if (reading == null) {
            return "Không tìm thấy bản ghi chỉ số (ReadingID=" + readingId + ")!";
        }

        if (billDAO.existsForReading(readingId)) {
            return "Hóa đơn cho chỉ số tháng " + reading.getPeriodDisplay()
                + " của hộ " + reading.getHouseholdCode() + " đã được tạo rồi!";
        }

        return doInsertBill(reading);
    }

    /**
     * If no bill exists for readingId, generates one.
     * If a bill already exists, recalculates and updates its amount.
     */
    @Override
    public String generateOrUpdateBill(int readingId) {
        MeterReading reading = readingDAO.findById(readingId);
        if (reading == null) {
            return "Không tìm thấy bản ghi chỉ số (ReadingID=" + readingId + ")!";
        }

        Bill existing = billDAO.findByReading(readingId);
        if (existing != null) {
            double newTotal = priceTierService.calculateTotal(reading.getConsumption());
            boolean ok = billDAO.updateAmount(existing.getBillId(), newTotal);
            System.out.println("[INFO] Bill updated for ReadingID=" + readingId
                + " BillCode=" + existing.getBillCode());
            return ok ? null : "Lỗi cập nhật hóa đơn!";
        }

        return doInsertBill(reading);
    }

    /** Shared insert logic used by generateBill and generateOrUpdateBill. */
    private String doInsertBill(MeterReading reading) {
        double total = priceTierService.calculateTotal(reading.getConsumption());
        if (total <= 0 && reading.getConsumption() > 0) {
            return "Không tìm thấy bảng giá điện! Vui lòng kiểm tra cài đặt bậc giá.";
        }

        String billCode = billDAO.getNextBillCode(reading.getMonth(), reading.getYear());

        Bill bill = new Bill();
        bill.setBillCode(billCode);
        bill.setHouseholdId(reading.getHouseholdId());
        bill.setReadingId(reading.getReadingId());
        bill.setTotalAmount(total);
        bill.setPaymentStatus("UNPAID");

        boolean ok = billDAO.insert(bill);
        if (ok) {
            System.out.println("[INFO] Bill generated. BillCode=" + billCode
                + " HouseholdID=" + reading.getHouseholdId()
                + " Period=" + reading.getPeriodDisplay()
                + " Total=" + String.format("%.0f", total));
            try {
                new NotificationServiceImpl().addNotification(
                    "Tạo hóa đơn thành công",
                    "Đã tạo hóa đơn " + billCode + " cho hộ " + reading.getHouseholdCode() + " với số tiền " + String.format("%,.0f đ", total) + " cho kỳ " + reading.getPeriodDisplay() + ".",
                    "success", "check-circle"
                );
            } catch (Exception e) {
                System.err.println("[WARN] failed to generate notification: " + e.getMessage());
            }
        }
        return ok ? null : "Lỗi tạo hóa đơn vào cơ sở dữ liệu!";
    }

    @Override
    public boolean payBill(int billId) {
        boolean ok = billDAO.updatePaymentStatus(billId, "PAID");
        if (ok) System.out.println("[INFO] Payment completed. BillID=" + billId);
        return ok;
    }

    @Override
    public int countUnpaid() {
        return billDAO.countByStatus("UNPAID");
    }

    @Override
    public int countPaid() {
        return billDAO.countByStatus("PAID");
    }

    @Override
    public double sumUnpaid() {
        return billDAO.sumTotalByStatus("UNPAID");
    }

    @Override
    public double sumPaid() {
        return billDAO.sumTotalByStatus("PAID");
    }
}
