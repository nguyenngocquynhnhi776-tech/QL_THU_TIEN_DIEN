package service.impl;

import dao.AreaDAO;
import dao.impl.AreaDAOImpl;
import model.Area;
import service.AreaService;

import java.util.List;

/**
 * Implementation of AreaService interface.
 */
public class AreaServiceImpl implements AreaService {

    private final AreaDAO areaDAO;

    public AreaServiceImpl() {
        this.areaDAO = new AreaDAOImpl();
    }

    public AreaServiceImpl(AreaDAO areaDAO) {
        this.areaDAO = areaDAO;
    }

    @Override
    public Area getById(int id) {
        return areaDAO.findById(id);
    }

    @Override
    public Area getByCode(String code) {
        if (code == null) return null;
        return areaDAO.findByCode(code.trim());
    }

    @Override
    public List<Area> getAll() {
        return areaDAO.getAll();
    }

    @Override
    public List<Area> getAllActive() {
        return areaDAO.getAllActive();
    }

    @Override
    public String addArea(Area area) {
        if (area == null) {
            return "Dữ liệu khu vực không hợp lệ!";
        }
        if (area.getAreaCode() == null || area.getAreaCode().trim().isEmpty()) {
            return "Mã khu vực không được để trống!";
        }
        if (area.getAreaName() == null || area.getAreaName().trim().isEmpty()) {
            return "Tên khu vực không được để trống!";
        }

        String cleanCode = area.getAreaCode().trim().toUpperCase();
        area.setAreaCode(cleanCode);
        area.setAreaName(area.getAreaName().trim());

        // Check duplicate area code
        Area existing = areaDAO.findByCode(cleanCode);
        if (existing != null) {
            return "Mã khu vực '" + cleanCode + "' đã tồn tại trên hệ thống!";
        }

        if (area.getStatus() == null) {
            area.setStatus("ACTIVE");
        }

        boolean success = areaDAO.insert(area);
        return success ? null : "Lỗi lưu trữ dữ liệu vào CSDL!";
    }

    @Override
    public String updateArea(Area area) {
        if (area == null) {
            return "Dữ liệu khu vực không hợp lệ!";
        }
        if (area.getAreaName() == null || area.getAreaName().trim().isEmpty()) {
            return "Tên khu vực không được để trống!";
        }

        area.setAreaName(area.getAreaName().trim());

        boolean success = areaDAO.update(area);
        return success ? null : "Lỗi cập nhật dữ liệu vào CSDL!";
    }

    @Override
    public boolean deactivateArea(int id) {
        return areaDAO.deactivate(id);
    }
}
