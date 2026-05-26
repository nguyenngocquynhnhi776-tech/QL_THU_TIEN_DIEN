package service.impl;

import dao.AreaDAO;
import dao.HouseholdDAO;
import dao.impl.AreaDAOImpl;
import dao.impl.HouseholdDAOImpl;
import model.Area;
import model.Household;
import service.HouseholdService;

import java.util.List;

/**
 * Implementation of HouseholdService.
 * Handles business logic and automatic HouseholdCode generation.
 */
public class HouseholdServiceImpl implements HouseholdService {

    private final HouseholdDAO householdDAO;
    private final AreaDAO areaDAO;

    public HouseholdServiceImpl() {
        this.householdDAO = new HouseholdDAOImpl();
        this.areaDAO      = new AreaDAOImpl();
    }

    public HouseholdServiceImpl(HouseholdDAO householdDAO, AreaDAO areaDAO) {
        this.householdDAO = householdDAO;
        this.areaDAO      = areaDAO;
    }

    @Override
    public Household getById(int id) {
        return householdDAO.findById(id);
    }

    @Override
    public Household getByCode(String code) {
        if (code == null) return null;
        return householdDAO.findByCode(code.trim());
    }

    @Override
    public List<Household> getAll() {
        return householdDAO.getAll();
    }

    @Override
    public List<Household> search(String keyword, Integer areaId) {
        return householdDAO.search(keyword, areaId);
    }

    /**
     * Generates the next HouseholdCode for a given area.
     * Format: {AreaCode}-{4-digit sequence}  e.g. KV01-0001
     */
    @Override
    public String generateHouseholdCode(int areaId) {
        Area area = areaDAO.findById(areaId);
        if (area == null) return null;

        String areaCode = area.getAreaCode();
        String highest  = householdDAO.getHighestCodeForArea(areaCode);

        int nextSeq = 1;
        if (highest != null) {
            // e.g. "KV01-0042" → split on last '-' → "0042" → 42
            int dashIdx = highest.lastIndexOf('-');
            if (dashIdx >= 0 && dashIdx < highest.length() - 1) {
                try {
                    nextSeq = Integer.parseInt(highest.substring(dashIdx + 1)) + 1;
                } catch (NumberFormatException ignored) {
                    nextSeq = 1;
                }
            }
        }

        return String.format("%s-%04d", areaCode, nextSeq);
    }

    @Override
    public String addHousehold(Household hh) {
        if (hh == null) {
            return "Dữ liệu hộ gia đình không hợp lệ!";
        }
        if (hh.getOwnerName() == null || hh.getOwnerName().trim().isEmpty()) {
            return "Tên chủ hộ không được để trống!";
        }
        if (hh.getAreaId() <= 0) {
            return "Vui lòng chọn khu vực!";
        }

        // Validate area exists
        Area area = areaDAO.findById(hh.getAreaId());
        if (area == null) {
            return "Khu vực không tồn tại!";
        }
        if (!"ACTIVE".equalsIgnoreCase(area.getStatus())) {
            return "Khu vực đã ngừng hoạt động, không thể thêm hộ!";
        }

        // Auto-generate household code
        String code = generateHouseholdCode(hh.getAreaId());
        if (code == null) {
            return "Không thể tạo mã hộ tự động!";
        }
        hh.setHouseholdCode(code);
        hh.setOwnerName(hh.getOwnerName().trim());

        if (hh.getAddress() != null) {
            hh.setAddress(hh.getAddress().trim());
        }
        if (hh.getPhone() != null) {
            hh.setPhone(hh.getPhone().trim());
        }
        if (hh.getStatus() == null || hh.getStatus().isEmpty()) {
            hh.setStatus("ACTIVE");
        }

        boolean success = householdDAO.insert(hh);
        return success ? null : "Lỗi lưu dữ liệu vào cơ sở dữ liệu!";
    }

    @Override
    public String updateHousehold(Household hh) {
        if (hh == null) {
            return "Dữ liệu hộ gia đình không hợp lệ!";
        }
        if (hh.getOwnerName() == null || hh.getOwnerName().trim().isEmpty()) {
            return "Tên chủ hộ không được để trống!";
        }
        if (hh.getAreaId() <= 0) {
            return "Vui lòng chọn khu vực!";
        }

        // Validate area still exists
        Area area = areaDAO.findById(hh.getAreaId());
        if (area == null) {
            return "Khu vực không tồn tại!";
        }

        hh.setOwnerName(hh.getOwnerName().trim());
        if (hh.getAddress() != null) {
            hh.setAddress(hh.getAddress().trim());
        }
        if (hh.getPhone() != null) {
            hh.setPhone(hh.getPhone().trim());
        }

        boolean success = householdDAO.update(hh);
        return success ? null : "Lỗi cập nhật dữ liệu!";
    }

    @Override
    public boolean deactivateHousehold(int id) {
        return householdDAO.deactivate(id);
    }
}
