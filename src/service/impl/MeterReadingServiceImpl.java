package service.impl;

import dao.MeterReadingDAO;
import dao.impl.MeterReadingDAOImpl;
import model.MeterReading;
import service.BillService;
import service.MeterReadingService;

import java.util.List;

/**
 * Business logic for Meter Reading management.
 */
public class MeterReadingServiceImpl implements MeterReadingService {

    private final MeterReadingDAO dao;
    private final BillService     billService;

    public MeterReadingServiceImpl() {
        this.dao         = new MeterReadingDAOImpl();
        this.billService = new BillServiceImpl();
    }

    public MeterReadingServiceImpl(MeterReadingDAO dao) {
        this.dao         = dao;
        this.billService = new BillServiceImpl();
    }

    @Override
    public MeterReading getById(int id) {
        return dao.findById(id);
    }

    @Override
    public List<MeterReading> getAll() {
        return dao.getAll();
    }

    @Override
    public List<MeterReading> search(String keyword, Integer month, Integer year) {
        return dao.search(keyword, month, year);
    }

    @Override
    public MeterReading getLastReading(int householdId) {
        return dao.getLastReadingForHousehold(householdId);
    }

    @Override
    public String addReading(MeterReading r) {
        if (r == null)              return "Dữ liệu chỉ số không hợp lệ!";
        if (r.getHouseholdId() <= 0) return "Vui lòng chọn hộ gia đình!";
        if (r.getMonth() < 1 || r.getMonth() > 12) return "Tháng không hợp lệ (1–12)!";
        if (r.getYear() < 2000)    return "Năm không hợp lệ!";
        if (r.getNewIndex() < 0 || r.getOldIndex() < 0)
                                   return "Chỉ số không được âm!";
        if (r.getNewIndex() < r.getOldIndex())
                                   return "Chỉ số mới phải lớn hơn hoặc bằng chỉ số cũ!";

        // Duplicate check: same household, same month/year
        MeterReading dup = dao.findByHouseholdAndPeriod(
                r.getHouseholdId(), r.getMonth(), r.getYear());
        if (dup != null) {
            return String.format(
                "Hộ này đã có chỉ số tháng %02d/%04d! (ReadingID=%d)",
                r.getMonth(), r.getYear(), dup.getReadingId());
        }

        r.setConsumption(r.getNewIndex() - r.getOldIndex());
        boolean ok = dao.insert(r);
        return ok ? null : "Lỗi lưu dữ liệu vào cơ sở dữ liệu!";
    }

    /**
     * Full workflow: validate → insert reading → auto-generate bill.
     * Returns DUPLICATE status if a reading already exists for that period
     * so the UI can prompt the user to confirm an update.
     */
    @Override
    public SaveResult addReadingAndGenerateBill(MeterReading r) {
        // --- validation ---
        if (r == null)
            return new SaveResult(SaveResult.Status.ERROR, "Dữ liệu chỉ số không hợp lệ!", -1);
        if (r.getHouseholdId() <= 0)
            return new SaveResult(SaveResult.Status.ERROR, "Vui lòng chọn hộ gia đình!", -1);
        if (r.getMonth() < 1 || r.getMonth() > 12)
            return new SaveResult(SaveResult.Status.ERROR, "Tháng không hợp lệ (1–12)!", -1);
        if (r.getYear() < 2000)
            return new SaveResult(SaveResult.Status.ERROR, "Năm không hợp lệ!", -1);
        if (r.getNewIndex() < 0 || r.getOldIndex() < 0)
            return new SaveResult(SaveResult.Status.ERROR, "Chỉ số không được âm!", -1);
        if (r.getNewIndex() <= r.getOldIndex())
            return new SaveResult(SaveResult.Status.ERROR,
                "Chỉ số mới phải lớn hơn chỉ số cũ!", -1);

        r.setConsumption(r.getNewIndex() - r.getOldIndex());

        // --- duplicate check ---
        MeterReading dup = dao.findByHouseholdAndPeriod(
                r.getHouseholdId(), r.getMonth(), r.getYear());
        if (dup != null) {
            return new SaveResult(SaveResult.Status.DUPLICATE,
                String.format("Hộ này đã có chỉ số tháng %02d/%04d. Bạn có muốn cập nhật không?",
                    r.getMonth(), r.getYear()),
                dup.getReadingId());
        }

        // --- insert ---
        int newId = dao.insertAndGetId(r);
        if (newId < 0) {
            return new SaveResult(SaveResult.Status.ERROR,
                "Lỗi lưu chỉ số vào cơ sở dữ liệu!", -1);
        }

        // --- generate bill ---
        String billErr = billService.generateOrUpdateBill(newId);
        if (billErr != null) {
            // Reading saved OK but bill failed — still report success with warning
            System.err.println("[WARN] Bill generation failed: " + billErr);
            return new SaveResult(SaveResult.Status.SUCCESS,
                "Đã lưu chỉ số điện nhưng tạo hóa đơn thất bại: " + billErr, newId);
        }

        System.out.println("[INFO] Meter reading saved and bill generated successfully. ReadingID=" + newId);
        return new SaveResult(SaveResult.Status.SUCCESS,
            "Đã lưu chỉ số điện và tạo hóa đơn thành công.", newId);
    }

    /**
     * Update an existing reading row and regenerate/update its bill.
     */
    @Override
    public SaveResult updateReadingAndBill(int existingReadingId, MeterReading newData) {
        // Find existing
        MeterReading existing = dao.findById(existingReadingId);
        if (existing == null) {
            return new SaveResult(SaveResult.Status.ERROR,
                "Không tìm thấy chỉ số cần cập nhật (ReadingID=" + existingReadingId + ")!", -1);
        }

        if (newData.getNewIndex() <= newData.getOldIndex()) {
            return new SaveResult(SaveResult.Status.ERROR,
                "Chỉ số mới phải lớn hơn chỉ số cũ!", -1);
        }

        // Update the existing row
        existing.setOldIndex(newData.getOldIndex());
        existing.setNewIndex(newData.getNewIndex());
        existing.setConsumption(newData.getNewIndex() - newData.getOldIndex());

        boolean ok = dao.update(existing);
        if (!ok) {
            return new SaveResult(SaveResult.Status.ERROR,
                "Lỗi cập nhật chỉ số vào cơ sở dữ liệu!", -1);
        }

        // Update or create bill
        String billErr = billService.generateOrUpdateBill(existingReadingId);
        if (billErr != null) {
            System.err.println("[WARN] Bill update failed: " + billErr);
            return new SaveResult(SaveResult.Status.SUCCESS,
                "Đã cập nhật chỉ số nhưng cập nhật hóa đơn thất bại: " + billErr,
                existingReadingId);
        }

        System.out.println("[INFO] Meter reading updated and bill regenerated. ReadingID=" + existingReadingId);
        return new SaveResult(SaveResult.Status.SUCCESS,
            "Đã cập nhật chỉ số điện và hóa đơn thành công.", existingReadingId);
    }
}
