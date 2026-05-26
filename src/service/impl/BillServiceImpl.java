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

    private final BillDAO        billDAO;
    private final MeterReadingDAO readingDAO;
    private final PriceTierService priceTierService;

    public BillServiceImpl() {
        this.billDAO         = new BillDAOImpl();
        this.readingDAO      = new MeterReadingDAOImpl();
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
     * Steps:
     *  1. Load MeterReading from DB.
     *  2. Verify no bill already exists for this reading.
     *  3. Calculate total from tiered pricing (DB-sourced tiers).
     *  4. Generate BillCode: HD + YYYYMM + running number.
     *  5. Insert BILL row with PaymentStatus = 'UNPAID'.
     */
    @Override
    public String generateBill(int readingId) {
        MeterReading reading = readingDAO.findById(readingId);
        if (reading == null) {
            return "Không tìm thấy bản ghi chỉ số (ReadingID=" + readingId + ")!";
        }

        // Prevent duplicate bills
        if (billDAO.existsForReading(readingId)) {
            return "Hóa đơn cho chỉ số tháng " + reading.getPeriodDisplay() +
                   " của hộ " + reading.getHouseholdCode() + " đã được tạo rồi!";
        }

        // Calculate total from live pricing tiers
        double total = priceTierService.calculateTotal(reading.getConsumption());
        if (total <= 0 && reading.getConsumption() > 0) {
            return "Không tìm thấy bảng giá điện! Vui lòng kiểm tra cài đặt bậc giá.";
        }

        // Generate BillCode
        String billCode = billDAO.getNextBillCode(reading.getMonth(), reading.getYear());

        Bill bill = new Bill();
        bill.setBillCode(billCode);
        bill.setHouseholdId(reading.getHouseholdId());
        bill.setReadingId(readingId);
        bill.setTotalAmount(total);
        bill.setPaymentStatus("UNPAID");

        boolean ok = billDAO.insert(bill);
        return ok ? null : "Lỗi tạo hóa đơn vào cơ sở dữ liệu!";
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
