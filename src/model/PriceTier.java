package model;

/**
 * Domain model for PRICE_TIER table (tiered electricity pricing).
 */
public class PriceTier {

    private int    priceId;
    private int    levelFrom;   // kWh start (inclusive)
    private int    levelTo;     // kWh end   (inclusive, -1 = unlimited)
    private double unitPrice;   // VND per kWh

    public PriceTier() {}

    public PriceTier(int priceId, int levelFrom, int levelTo, double unitPrice) {
        this.priceId   = priceId;
        this.levelFrom = levelFrom;
        this.levelTo   = levelTo;
        this.unitPrice = unitPrice;
    }

    public PriceTier(int levelFrom, int levelTo, double unitPrice) {
        this.levelFrom = levelFrom;
        this.levelTo   = levelTo;
        this.unitPrice = unitPrice;
    }

    // --- Getters / Setters ---

    public int getPriceId()             { return priceId; }
    public void setPriceId(int v)       { this.priceId = v; }

    public int getLevelFrom()           { return levelFrom; }
    public void setLevelFrom(int v)     { this.levelFrom = v; }

    public int getLevelTo()             { return levelTo; }
    public void setLevelTo(int v)       { this.levelTo = v; }

    public double getUnitPrice()        { return unitPrice; }
    public void setUnitPrice(double v)  { this.unitPrice = v; }

    /** Display: "0 – 50 kWh" or "401+ kWh" */
    public String getRangeDisplay() {
        if (levelTo < 0) return levelFrom + "+ kWh";
        return levelFrom + " – " + levelTo + " kWh";
    }

    /** Formatted price: "1.806 đ/kWh" */
    public String getPriceDisplay() {
        return String.format("%,.0f đ/kWh", unitPrice);
    }
}
