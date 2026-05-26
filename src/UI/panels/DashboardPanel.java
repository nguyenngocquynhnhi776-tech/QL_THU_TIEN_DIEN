package UI.panels;

import UI.ai.AIEngine;
import UI.charts.BarChartPanel;
import UI.charts.LineChartPanel;
import UI.components.*;
import model.Bill;
import model.Household;
import model.MeterReading;
import model.Payment;
import service.BillService;
import service.HouseholdService;
import service.MeterReadingService;
import service.PaymentService;
import service.impl.BillServiceImpl;
import service.impl.HouseholdServiceImpl;
import service.impl.MeterReadingServiceImpl;
import service.impl.PaymentServiceImpl;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * Dashboard — tổng quan hệ thống thực tế với thẻ thống kê, biểu đồ và AI insights từ database.
 * Hoàn toàn kết nối với SQL Server.
 */
public class DashboardPanel extends BasePanel {

    private final HouseholdService    householdService = new HouseholdServiceImpl();
    private final BillService         billService = new BillServiceImpl();
    private final PaymentService      paymentService = new PaymentServiceImpl();
    private final MeterReadingService readingService = new MeterReadingServiceImpl();

    public DashboardPanel() {
        super("Bảng điều khiển", "Tổng quan hệ thống quản lý thu tiền điện");

        JPanel root = new JPanel(new BorderLayout(0, UIConstants.SP_MD));
        root.setOpaque(false);

        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        // ====================================================================
        // 1. QUERY REAL STATISTICS FROM SQL SERVER
        // ====================================================================
        int totalHouseholds = householdService.getAll().size();
        
        List<Bill> currentMonthBills = billService.search(null, null, currentMonth, currentYear);
        int totalBillsGenerated = currentMonthBills.size();

        double monthlyRevenue = paymentService.getMonthlyRevenue(currentMonth, currentYear);

        int unpaidHouseholds = billService.search(null, "UNPAID", currentMonth, currentYear).size();

        // TRAILING 6 MONTHS VALUES (for charts & AI calculations)
        double[] trailingRevenue = new double[6];
        double[] trailingKwh = new double[6];
        String[] trailingMonths = new String[6];

        LocalDate targetDate = LocalDate.now().minusMonths(5);
        for (int i = 0; i < 6; i++) {
            int m = targetDate.getMonthValue();
            int y = targetDate.getYear();
            trailingMonths[i] = "T" + m;

            trailingRevenue[i] = paymentService.getMonthlyRevenue(m, y);

            List<MeterReading> mReadings = readingService.search(null, m, y);
            double sumKwh = 0;
            for (MeterReading r : mReadings) {
                sumKwh += r.getConsumption();
            }
            trailingKwh[i] = sumKwh;

            targetDate = targetDate.plusMonths(1);
        }

        // ====================================================================
        // 2. DYNAMIC AI INSIGHTS & ANOMALY SCANNER
        // ====================================================================
        List<AIEngine.AnomalyResult> anomalies = new ArrayList<>();
        List<MeterReading> allReadings = readingService.getAll();
        
        // Group readings by household
        Map<Integer, List<MeterReading>> readingsByHousehold = new HashMap<>();
        for (MeterReading r : allReadings) {
            readingsByHousehold.computeIfAbsent(r.getHouseholdId(), k -> new ArrayList<>()).add(r);
        }

        // Detect consumption anomalies (Spikes or Drops)
        for (Map.Entry<Integer, List<MeterReading>> entry : readingsByHousehold.entrySet()) {
            List<MeterReading> list = entry.getValue();
            if (list.size() < 2) continue; // need history

            // Sort ascending by period
            list.sort(Comparator.comparingInt(MeterReading::getYear).thenComparingInt(MeterReading::getMonth));

            // Extract historical consumption array
            double[] history = new double[list.size() - 1];
            for (int i = 0; i < list.size() - 1; i++) {
                history[i] = list.get(i).getConsumption();
            }

            MeterReading latest = list.get(list.size() - 1);
            double currentUsage = latest.getConsumption();

            AIEngine.AnomalyResult r = AIEngine.detectAnomaly(latest.getHouseholdCode(), history, currentUsage);
            if (r.type == AIEngine.AnomalyType.SPIKE || r.type == AIEngine.AnomalyType.DROP) {
                anomalies.add(r);
            }
        }

        int aiWarningsCount = anomalies.size();

        // ---- Stat cards row ----
        JPanel cardsRow = new JPanel(new GridLayout(1, 5, UIConstants.SP_MD, 0));
        cardsRow.setOpaque(false);
        cardsRow.setPreferredSize(new Dimension(0, 130));

        cardsRow.add(new StatsCard("🏠", "Tổng số hộ", String.format("%,d", totalHouseholds), "hộ đăng ký", true, UIConstants.PRIMARY));
        cardsRow.add(new StatsCard("🧾", "Hóa đơn kỳ này", String.format("%,d", totalBillsGenerated), String.format("Kỳ %02d/%04d", currentMonth, currentYear), true, UIConstants.SUCCESS));
        cardsRow.add(new StatsCard("💵", "Doanh thu tháng", String.format("%,.0f đ", monthlyRevenue), "tháng hiện tại", true, UIConstants.AI_PURPLE));
        cardsRow.add(new StatsCard("⚠️", "Chưa thanh toán", String.format("%,d hộ", unpaidHouseholds), "hộ tồn đọng", false, UIConstants.WARNING));
        
        Color aiColor = aiWarningsCount > 0 ? UIConstants.ERROR : UIConstants.SUCCESS;
        String aiTrend = aiWarningsCount > 0 ? "cần kiểm tra" : "hệ thống ổn định";
        cardsRow.add(new StatsCard("🧠", "Cảnh báo AI", String.format("%d hộ", aiWarningsCount), aiTrend, aiWarningsCount == 0, aiColor));

        // ---- Charts row ----
        JPanel chartsRow = new JPanel(new GridLayout(1, 2, UIConstants.SP_MD, 0));
        chartsRow.setOpaque(false);
        chartsRow.setPreferredSize(new Dimension(0, 270));

        // Revenue line chart
        GlassCard revenueCard = new GlassCard(UIConstants.CARD_RADIUS);
        revenueCard.setLayout(new BorderLayout());
        LineChartPanel lineChart = new LineChartPanel("Doanh thu 6 tháng gần nhất (VND)",
            trailingRevenue, trailingMonths, UIConstants.PRIMARY, "đ");
        revenueCard.add(lineChart, BorderLayout.CENTER);

        // Consumption bar chart
        GlassCard consumeCard = new GlassCard(UIConstants.CARD_RADIUS);
        consumeCard.setLayout(new BorderLayout());
        BarChartPanel barChart = new BarChartPanel("Sản lượng điện tiêu thụ (kWh)", trailingKwh, trailingMonths, UIConstants.SUCCESS);
        consumeCard.add(barChart, BorderLayout.CENTER);

        chartsRow.add(revenueCard);
        chartsRow.add(consumeCard);

        // ---- Bottom row: recent activity + AI insights ----
        JPanel bottomRow = new JPanel(new GridLayout(1, 2, UIConstants.SP_MD, 0));
        bottomRow.setOpaque(false);
        bottomRow.setPreferredSize(new Dimension(0, 250));

        // 3. Dynamic Recent activities from SQL Server
        GlassCard actCard = new GlassCard(UIConstants.CARD_RADIUS);
        actCard.setLayout(new BorderLayout());
        JLabel actTitle = new JLabel("Hoạt động gần đây (Thanh toán thực tế)");
        actTitle.setFont(UIConstants.FONT_SUBHEADER);
        actTitle.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
        actTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, UIConstants.SP_SM, 0));

        JPanel actList = new JPanel();
        actList.setOpaque(false);
        actList.setLayout(new BoxLayout(actList, BoxLayout.Y_AXIS));

        List<Payment> recentPayments = paymentService.getRecentPayments(5);
        if (recentPayments.isEmpty()) {
            JLabel emptyLbl = new JLabel("  Không có hoạt động thanh toán gần đây.");
            emptyLbl.setFont(UIConstants.FONT_SMALL);
            emptyLbl.setForeground(UIConstants.COLOR_TEXT_MUTED);
            actList.add(emptyLbl);
        } else {
            for (Payment p : recentPayments) {
                JPanel row = new JPanel(new BorderLayout());
                row.setOpaque(false);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
                row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.COLOR_DIVIDER));
                
                JLabel lbl = new JLabel("✔️  " + p.getOwnerName() + " đã TT " + String.format("%,.0fđ", p.getAmount()) + " (" + p.getPaymentMethod() + ")");
                lbl.setFont(UIConstants.FONT_SMALL);
                lbl.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
                
                String timeStr = p.getPaymentDate() != null ? p.getPaymentDate().toString().substring(5, 16) : "";
                JLabel time = new JLabel(timeStr);
                time.setFont(UIConstants.FONT_SMALL);
                time.setForeground(UIConstants.COLOR_TEXT_MUTED);
                
                row.add(lbl, BorderLayout.CENTER);
                row.add(time, BorderLayout.EAST);
                actList.add(row);
                actList.add(Box.createVerticalStrut(4));
            }
        }

        JPanel actWrap = new JPanel(new BorderLayout());
        actWrap.setOpaque(false);
        actWrap.add(actTitle, BorderLayout.NORTH);
        actWrap.add(actList,  BorderLayout.CENTER);
        actCard.add(actWrap, BorderLayout.CENTER);

        // 4. Dynamic AI Insights Column
        GlassCard aiCard = new GlassCard(UIConstants.CARD_RADIUS, UIConstants.AI_PURPLE);
        aiCard.setLayout(new BorderLayout(0, UIConstants.SP_SM));

        JLabel aiTitle = new JLabel("🧠  AI Phân tích & Dự báo");
        aiTitle.setFont(UIConstants.FONT_SUBHEADER);
        aiTitle.setForeground(UIConstants.AI_PURPLE);

        JPanel aiList = new JPanel();
        aiList.setOpaque(false);
        aiList.setLayout(new BoxLayout(aiList, BoxLayout.Y_AXIS));

        // Predict next month's revenue using Linear Regression
        double[] forecastData = AIEngine.linearRegressionForecast(trailingRevenue, 1);
        double predictedRev = forecastData.length > 0 ? forecastData[0] : 0;

        List<String> insights = new ArrayList<>();
        List<Color> insightColors = new ArrayList<>();

        // Add anomaly warning if found
        if (!anomalies.isEmpty()) {
            int shown = 0;
            for (AIEngine.AnomalyResult ar : anomalies) {
                if (shown >= 2) break;
                insights.add("⚠️  " + ar.householdId + ": " + ar.message);
                insightColors.add(UIConstants.WARNING);
                shown++;
            }
        } else {
            insights.add("✔️  Không phát hiện bất thường tiêu thụ điện kỳ này.");
            insightColors.add(UIConstants.SUCCESS);
        }

        // Add revenue forecast
        insights.add(String.format("🔮  Dự báo doanh thu tháng tới: %,.0f đ (Hồi quy AI)", predictedRev));
        insightColors.add(UIConstants.AI_PURPLE);

        // Add trailing consumption trend comparison
        double prevMonthKwh = trailingKwh[4];
        double currMonthKwh = trailingKwh[5];
        if (prevMonthKwh > 0) {
            String trend = AIEngine.trendLabel(prevMonthKwh, currMonthKwh);
            boolean isUp = AIEngine.isTrendUp(prevMonthKwh, currMonthKwh);
            insights.add(String.format("📊  Xu hướng tiêu thụ: %s so với tháng trước.", trend));
            insightColors.add(isUp ? UIConstants.ERROR : UIConstants.SUCCESS);
        }

        for (int i = 0; i < insights.size(); i++) {
            JLabel il = new JLabel("<html>" + insights.get(i) + "</html>");
            il.setFont(UIConstants.FONT_SMALL);
            il.setForeground(insightColors.get(i));
            il.setAlignmentX(LEFT_ALIGNMENT);
            il.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
            aiList.add(il);
        }

        JPanel aiWrap = new JPanel(new BorderLayout());
        aiWrap.setOpaque(false);
        aiWrap.add(aiTitle, BorderLayout.NORTH);
        aiWrap.add(aiList,  BorderLayout.CENTER);
        aiCard.add(aiWrap, BorderLayout.CENTER);

        bottomRow.add(actCard);
        bottomRow.add(aiCard);

        root.add(cardsRow,   BorderLayout.NORTH);
        root.add(chartsRow,  BorderLayout.CENTER);
        root.add(bottomRow,  BorderLayout.SOUTH);

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        contentArea.add(scrollPane, BorderLayout.CENTER);
    }
}
