package model;

import java.sql.Timestamp;

/**
 * Domain model class representing the AREA database table.
 */
public class Area {

    private int       areaId;
    private String    areaCode;
    private String    areaName;
    private String    status;
    private Timestamp createdAt;

    // Constructors
    public Area() {}

    public Area(int areaId, String areaCode, String areaName, String status, Timestamp createdAt) {
        this.areaId    = areaId;
        this.areaCode  = areaCode;
        this.areaName  = areaName;
        this.status    = status;
        this.createdAt = createdAt;
    }

    public Area(String areaCode, String areaName, String status) {
        this.areaCode = areaCode;
        this.areaName = areaName;
        this.status   = status;
    }

    // Getters and Setters
    public int getAreaId() {
        return areaId;
    }

    public void setAreaId(int areaId) {
        this.areaId = areaId;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // Helper for table renderers
    public String getStatusDisplay() {
        if (status == null) return "Không xác định";
        switch (status.toUpperCase()) {
            case "ACTIVE":   return "Hoạt động";
            case "INACTIVE": return "Không hoạt động";
            default:         return status;
        }
    }

    @Override
    public String toString() {
        return areaName + " (" + areaCode + ")";
    }
}
