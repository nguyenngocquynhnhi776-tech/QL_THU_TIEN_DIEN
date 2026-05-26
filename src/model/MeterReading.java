package model;

import java.sql.Timestamp;

/**
 * Domain model for METER_READING table.
 */
public class MeterReading {

    private int       readingId;
    private int       householdId;
    private int       month;
    private int       year;
    private double    oldIndex;
    private double    newIndex;
    private double    consumption;  // newIndex - oldIndex, stored for convenience
    private Timestamp createdAt;

    // Join fields (from HOUSEHOLD)
    private String householdCode;
    private String ownerName;
    private String areaName;

    public MeterReading() {}

    public MeterReading(int householdId, int month, int year,
                        double oldIndex, double newIndex) {
        this.householdId = householdId;
        this.month       = month;
        this.year        = year;
        this.oldIndex    = oldIndex;
        this.newIndex    = newIndex;
        this.consumption = newIndex - oldIndex;
    }

    // --- Getters / Setters ---

    public int getReadingId()            { return readingId; }
    public void setReadingId(int v)      { this.readingId = v; }

    public int getHouseholdId()          { return householdId; }
    public void setHouseholdId(int v)    { this.householdId = v; }

    public int getMonth()                { return month; }
    public void setMonth(int v)          { this.month = v; }

    public int getYear()                 { return year; }
    public void setYear(int v)           { this.year = v; }

    public double getOldIndex()          { return oldIndex; }
    public void setOldIndex(double v)    { this.oldIndex = v; }

    public double getNewIndex()          { return newIndex; }
    public void setNewIndex(double v)    { this.newIndex = v; }

    public double getConsumption()       { return consumption; }
    public void setConsumption(double v) { this.consumption = v; }

    public Timestamp getCreatedAt()      { return createdAt; }
    public void setCreatedAt(Timestamp v){ this.createdAt = v; }

    public String getHouseholdCode()          { return householdCode; }
    public void setHouseholdCode(String v)    { this.householdCode = v; }

    public String getOwnerName()              { return ownerName; }
    public void setOwnerName(String v)        { this.ownerName = v; }

    public String getAreaName()               { return areaName; }
    public void setAreaName(String v)         { this.areaName = v; }

    /** Returns "MM/YYYY" display string */
    public String getPeriodDisplay() {
        return String.format("%02d/%04d", month, year);
    }
}
