package model;

import java.sql.Timestamp;

/**
 * Domain model class representing the HOUSEHOLD database table.
 */
public class Household {

    private int       householdId;
    private String    householdCode;
    private String    ownerName;
    private String    address;
    private String    phone;
    private int       areaId;
    private String    status;
    private Timestamp createdAt;

    // Display-only fields (joined from AREA)
    private String    areaCode;
    private String    areaName;

    // Constructors
    public Household() {}

    public Household(int householdId, String householdCode, String ownerName, String address, 
                     String phone, int areaId, String status, Timestamp createdAt) {
        this.householdId   = householdId;
        this.householdCode = householdCode;
        this.ownerName     = ownerName;
        this.address       = address;
        this.phone         = phone;
        this.areaId        = areaId;
        this.status        = status;
        this.createdAt     = createdAt;
    }

    public Household(String householdCode, String ownerName, String address, String phone, int areaId, String status) {
        this.householdCode = householdCode;
        this.ownerName     = ownerName;
        this.address       = address;
        this.phone         = phone;
        this.areaId        = areaId;
        this.status        = status;
    }

    // Getters and Setters
    public int getHouseholdId() {
        return householdId;
    }

    public void setHouseholdId(int householdId) {
        this.householdId = householdId;
    }

    public String getHouseholdCode() {
        return householdCode;
    }

    public void setHouseholdCode(String householdCode) {
        this.householdCode = householdCode;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getAreaId() {
        return areaId;
    }

    public void setAreaId(int areaId) {
        this.areaId = areaId;
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

    // Helpers
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
        return ownerName + " (" + householdCode + ")";
    }
}
