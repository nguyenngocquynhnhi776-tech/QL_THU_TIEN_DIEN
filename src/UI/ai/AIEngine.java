package UI.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * Electra Manager AI — Pure-Java AI Analytics Engine.
 * No external ML libraries required. All algorithms implemented from scratch.
 */
public class AIEngine {

    // =====================================================================
    // ANOMALY DETECTION
    // =====================================================================

    public enum AnomalyType { NONE, SPIKE, DROP, NEGATIVE }

    public static class AnomalyResult {
        public final String householdId;
        public final AnomalyType type;
        public final double currentUsage;
        public final double avgUsage;
        public final double changePercent;
        public final String message;

        public AnomalyResult(String hid, AnomalyType type, double curr, double avg, double pct, String msg) {
            this.householdId   = hid;
            this.type          = type;
            this.currentUsage  = curr;
            this.avgUsage      = avg;
            this.changePercent = pct;
            this.message       = msg;
        }
    }

    /**
     * Detect anomalies in electricity usage.
     * @param householdId  identifier for display
     * @param history      array of past usage values (oldest first)
     * @param current      current month's usage
     * @return AnomalyResult (type=NONE if normal)
     */
    public static AnomalyResult detectAnomaly(String householdId, double[] history, double current) {
        if (history == null || history.length == 0) {
            return new AnomalyResult(householdId, AnomalyType.NONE, current, 0, 0, "Chưa đủ dữ liệu lịch sử.");
        }

        if (current < 0) {
            return new AnomalyResult(householdId, AnomalyType.NEGATIVE, current, 0, 0,
                "Chỉ số âm — khả năng nhập sai dữ liệu!");
        }

        double avg = 0;
        for (double v : history) avg += v;
        avg /= history.length;

        if (avg == 0) avg = 1;
        double changePct = ((current - avg) / avg) * 100.0;

        if (changePct > 150) {
            return new AnomalyResult(householdId, AnomalyType.SPIKE, current, avg, changePct,
                String.format("Tiêu thụ tăng %.0f%% so với trung bình (%.0f kWh → %.0f kWh).",
                              changePct, avg, current));
        } else if (changePct < -50) {
            return new AnomalyResult(householdId, AnomalyType.DROP, current, avg, changePct,
                String.format("Tiêu thụ giảm %.0f%% so với trung bình (%.0f kWh → %.0f kWh).",
                              Math.abs(changePct), avg, current));
        }

        return new AnomalyResult(householdId, AnomalyType.NONE, current, avg, changePct, "Bình thường.");
    }

    /** Batch anomaly detection across multiple households. */
    public static List<AnomalyResult> detectAllAnomalies(String[] ids, double[][] histories, double[] currents) {
        List<AnomalyResult> results = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            AnomalyResult r = detectAnomaly(ids[i], histories[i], currents[i]);
            if (r.type != AnomalyType.NONE) results.add(r);
        }
        return results;
    }

    // =====================================================================
    // FORECASTING — MOVING AVERAGE
    // =====================================================================

    /**
     * Compute a simple moving average forecast for the next N periods.
     * @param data   historical data array
     * @param window window size for the average
     * @param periods number of future periods to forecast
     * @return array of forecasted values
     */
    public static double[] movingAverageForecast(double[] data, int window, int periods) {
        if (data == null || data.length == 0) return new double[periods];
        int len = data.length;
        double[] forecast = new double[periods];
        double[] extended = new double[len + periods];
        System.arraycopy(data, 0, extended, 0, len);

        for (int i = 0; i < periods; i++) {
            int start = len + i - Math.min(window, len + i);
            double sum = 0;
            int cnt = 0;
            for (int j = start; j < len + i; j++) { sum += extended[j]; cnt++; }
            extended[len + i] = (cnt > 0) ? sum / cnt : 0;
            forecast[i] = extended[len + i];
        }
        return forecast;
    }

    // =====================================================================
    // FORECASTING — LINEAR REGRESSION
    // =====================================================================

    /**
     * Forecast future values using simple linear regression (least-squares).
     * @param data    historical data
     * @param periods number of future periods to forecast
     * @return forecasted values array
     */
    public static double[] linearRegressionForecast(double[] data, int periods) {
        if (data == null || data.length < 2) return new double[periods];
        int n = data.length;

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX  += i;
            sumY  += data[i];
            sumXY += i * data[i];
            sumX2 += (double) i * i;
        }
        double slope     = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        double[] forecast = new double[periods];
        for (int i = 0; i < periods; i++) {
            forecast[i] = Math.max(0, intercept + slope * (n + i));
        }
        return forecast;
    }

    // =====================================================================
    // DEBT RISK CLASSIFICATION
    // =====================================================================

    public enum DebtRisk { LOW, MEDIUM, HIGH }

    /**
     * Classify debt risk based on payment delay history.
     * @param latePayments  number of times this household paid late historically
     * @param maxDaysLate   maximum days late on a single payment
     * @return DebtRisk level
     */
    public static DebtRisk classifyDebtRisk(int latePayments, int maxDaysLate) {
        if (latePayments == 0 && maxDaysLate == 0) return DebtRisk.LOW;
        if (latePayments >= 3 || maxDaysLate >= 30)  return DebtRisk.HIGH;
        if (latePayments >= 1 || maxDaysLate >= 10)  return DebtRisk.MEDIUM;
        return DebtRisk.LOW;
    }

    public static String debtRiskLabel(DebtRisk risk) {
        switch (risk) {
            case HIGH:   return "Nguy cơ cao";
            case MEDIUM: return "Nguy cơ trung bình";
            default:     return "Nguy cơ thấp";
        }
    }

    // =====================================================================
    // TREND ANALYSIS
    // =====================================================================

    /**
     * Calculate month-over-month percentage change.
     * @param previous previous period value
     * @param current  current period value
     * @return percentage change (positive = increase, negative = decrease)
     */
    public static double trendPercent(double previous, double current) {
        if (previous == 0) return 0;
        return ((current - previous) / previous) * 100.0;
    }

    /** Return a formatted trend string like "+12.3%" or "-5.1%". */
    public static String trendLabel(double previous, double current) {
        double pct = trendPercent(previous, current);
        return String.format("%+.1f%%", pct);
    }

    /** True if the trend is upward. */
    public static boolean isTrendUp(double previous, double current) {
        return current >= previous;
    }
}
