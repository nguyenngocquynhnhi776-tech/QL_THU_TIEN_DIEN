package model;

import java.sql.Timestamp;

/**
 * Domain model for PAYMENT table.
 */
public class Payment {

    private int       paymentId;
    private int       billId;
    private double    amount;
    private String    paymentMethod;
    private Timestamp paymentDate;
    private String    note;

    // Join field
    private String billCode;
    private String ownerName;
    private String householdCode;

    public Payment() {}

    public Payment(int billId, double amount, String paymentMethod, String note) {
        this.billId        = billId;
        this.amount        = amount;
        this.paymentMethod = paymentMethod;
        this.note          = note;
    }

    // --- Getters / Setters ---

    public int getPaymentId()               { return paymentId; }
    public void setPaymentId(int v)         { this.paymentId = v; }

    public int getBillId()                  { return billId; }
    public void setBillId(int v)            { this.billId = v; }

    public double getAmount()               { return amount; }
    public void setAmount(double v)         { this.amount = v; }

    public String getPaymentMethod()        { return paymentMethod; }
    public void setPaymentMethod(String v)  { this.paymentMethod = v; }

    public Timestamp getPaymentDate()       { return paymentDate; }
    public void setPaymentDate(Timestamp v) { this.paymentDate = v; }

    public String getNote()                 { return note; }
    public void setNote(String v)           { this.note = v; }

    public String getBillCode()             { return billCode; }
    public void setBillCode(String v)       { this.billCode = v; }

    public String getOwnerName()            { return ownerName; }
    public void setOwnerName(String v)      { this.ownerName = v; }

    public String getHouseholdCode()        { return householdCode; }
    public void setHouseholdCode(String v)  { this.householdCode = v; }

    public String getAmountDisplay() {
        return String.format("%,.0f đ", amount);
    }

    public String getPaymentDateDisplay() {
        if (paymentDate == null) return "";
        return paymentDate.toString().substring(0, 10);
    }
}
