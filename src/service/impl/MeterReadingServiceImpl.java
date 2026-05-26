package service.impl;

import dao.MeterReadingDAO;
import dao.impl.MeterReadingDAOImpl;
import model.MeterReading;
import service.MeterReadingService;

import java.util.List;

/**
 * Business logic for Meter Reading management.
 */
public class MeterReadingServiceImpl implements MeterReadingService {

    private final MeterReadingDAO dao;

    public MeterReadingServiceImpl() {
        this.dao = new MeterReadingDAOImpl();
    }

    public MeterReadingServiceImpl(MeterReadingDAO dao) {
        this.dao = dao;
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
        if (r == null)             return "Dữ liệu chỉ số không hợp lệ!";
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

        // Compute consumption
        r.setConsumption(r.getNewIndex() - r.getOldIndex());

        boolean ok = dao.insert(r);
        return ok ? null : "Lỗi lưu dữ liệu vào cơ sở dữ liệu!";
    }
}
