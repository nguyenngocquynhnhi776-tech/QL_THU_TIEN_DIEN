package model;

import java.sql.Timestamp;

/**
 * Domain model for BILL table.
 */
public class Bill {

    private int       billId;
    private String    billCode;
    private int       householdId;
    private int       readingId;
    private double    totalAmount;
    private String    paymentStatus;  // UNPAID | PAID
    private Timestamp createdAt;

    // Join fields
    private String householdCode;
    private String ownerName;
    private String areaName;
    private int    month;
    private int    year;
    private double consumption;

    public Bill() {}

    // --- Getters / Setters ---

    public int getBillId()                  { return billId; }
    public void setBillId(int v)            { this.billId = v; }

    public String getBillCode()             { return billCode; }
    public void setBillCode(String v)       { this.billCode = v; }

    public int getHouseholdId()             { return householdId; }
    public void setHouseholdId(int v)       { this.householdId = v; }

    public int getReadingId()               { return readingId; }
    public void setReadingId(int v)         { this.readingId = v; }

    public double getTotalAmount()          { return totalAmount; }
    public void setTotalAmount(double v)    { this.totalAmount = v; }

    public String getPaymentStatus()        { return paymentStatus; }
    public void setPaymentStatus(String v)  { this.paymentStatus = v; }

    public Timestamp getCreatedAt()         { return createdAt; }
    public void setCreatedAt(Timestamp v)   { this.createdAt = v; }

    public String getHouseholdCode()        { return householdCode; }
    public void setHouseholdCode(String v)  { this.householdCode = v; }

    public String getOwnerName()            { return ownerName; }
    public void setOwnerName(String v)      { this.ownerName = v; }

    public String getAreaName()             { return areaName; }
    public void setAreaName(String v)       { this.areaName = v; }

    public int getMonth()                   { return month; }
    public void setMonth(int v)             { this.month = v; }

    public int getYear()                    { return year; }
    public void setYear(int v)              { this.year = v; }

    public double getConsumption()          { return consumption; }
    public void setConsumption(double v)    { this.consumption = v; }

    /** Returns "MM/YYYY" */
    public String getPeriodDisplay() {
        return String.format("%02d/%04d", month, year);
    }

    /** Returns formatted total */
    public String getTotalDisplay() {
        return String.format("%,.0f đ", totalAmount);
    }

    /** Returns display string for paymentStatus */
    public String getStatusDisplay() {
        if (paymentStatus == null) return "Chưa thanh toán";
        switch (paymentStatus.toUpperCase()) {
            case "PAID":   return "Đã thanh toán";
            case "UNPAID": return "Chưa thanh toán";
            default:       return paymentStatus;
        }
    }

    public boolean isPaid() {
        return "PAID".equalsIgnoreCase(paymentStatus);
    }
}
