package UI.panels;

import UI.ai.AIEngine;
import UI.charts.BarChartPanel;
import UI.charts.LineChartPanel;
import UI.charts.PieChartPanel;
import UI.components.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * AI Phân tích — phát hiện bất thường, dự báo, phân loại nguy cơ nợ.
 */
public class AIPanel extends BasePanel {

    public AIPanel() {
        super("AI Phân tích", "Phân tích thông minh: phát hiện bất thường, dự báo tiêu thụ, nguy cơ nợ");

        // ---- Run AI algorithms ----
        String[] ids = {"H001","H002","H003","H004","H005"};
        double[][] histories = {
            {140,150,145,160,155}, {70,75,68,80,72},  {200,210,195,205,198},
            {90,85,95,88,92},      {300,310,295,305,315}
        };
        double[] currents = {158, 74, 520, 91, 280};
        List<AIEngine.AnomalyResult> anomalies = AIEngine.detectAllAnomalies(ids, histories, currents);

        double[] histRevenue = {920, 1050, 980, 1100, 1180, 1250};
        double[] forecastMA  = AIEngine.movingAverageForecast(histRevenue, 3, 3);
        double[] forecastLR  = AIEngine.linearRegressionForecast(histRevenue, 3);

        // ---- Top row: anomaly list + AI stats ----
        JPanel topRow = new JPanel(new GridLayout(1, 2, UIConstants.SP_MD, 0));
        topRow.setOpaque(false);
        topRow.setPreferredSize(new Dimension(0, 220));

        // Anomaly card
        GlassCard anomalyCard = new GlassCard(UIConstants.CARD_RADIUS, UIConstants.WARNING);
        anomalyCard.setLayout(new BorderLayout(0, UIConstants.SP_SM));

        JLabel anomTitle = new JLabel("\u26A0  Phát hiện bất thường  (" + anomalies.size() + " hộ)");
        anomTitle.setFont(UIConstants.FONT_SUBHEADER);
        anomTitle.setForeground(UIConstants.WARNING);

        JPanel anomList = new JPanel();
        anomList.setOpaque(false);
        anomList.setLayout(new BoxLayout(anomList, BoxLayout.Y_AXIS));

        if (anomalies.isEmpty()) {
            JLabel ok = new JLabel("\u2714 Không phát hiện bất thường nào!");
            ok.setFont(UIConstants.FONT_NORMAL);
            ok.setForeground(UIConstants.SUCCESS);
            anomList.add(ok);
        } else {
            for (AIEngine.AnomalyResult r : anomalies) {
                JPanel row = new JPanel(new BorderLayout(UIConstants.SP_SM, 0));
                row.setOpaque(false);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.COLOR_DIVIDER));

                String icon = r.type == AIEngine.AnomalyType.SPIKE ? "\u25B2"
                            : r.type == AIEngine.AnomalyType.DROP  ? "\u25BC" : "\u26A0";
                StatusBadge.Status st = r.type == AIEngine.AnomalyType.SPIKE
                        ? StatusBadge.Status.WARNING : StatusBadge.Status.AI_HIGH;

                JPanel left = new JPanel(new BorderLayout(4, 0));
                left.setOpaque(false);
                left.add(new StatusBadge(icon + " " + r.householdId, st), BorderLayout.WEST);
                JLabel msg = new JLabel("<html><small>" + r.message + "</small></html>");
                msg.setFont(UIConstants.FONT_SMALL);
                msg.setForeground(UIConstants.COLOR_TEXT_SECONDARY);
                left.add(msg, BorderLayout.CENTER);

                row.add(left, BorderLayout.CENTER);
                anomList.add(row);
            }
        }

        JScrollPane anomScroll = new JScrollPane(anomList);
        anomScroll.setBorder(BorderFactory.createEmptyBorder());
        anomScroll.setOpaque(false);
        anomScroll.getViewport().setOpaque(false);

        anomalyCard.add(anomTitle,  BorderLayout.NORTH);
        anomalyCard.add(anomScroll, BorderLayout.CENTER);

        // Debt risk pie chart card
        GlassCard riskCard = new GlassCard(UIConstants.CARD_RADIUS, UIConstants.ERROR);
        riskCard.setLayout(new BorderLayout());
        PieChartPanel pie = new PieChartPanel("Phân loại nguy cơ nợ",
            new double[]{65, 25, 10},
            new String[]{"Nguy cơ thấp (65%)", "Nguy cơ TB (25%)", "Nguy cơ cao (10%)"});
        pie.setCenterLabel("1.500 hộ");
        // Colors are used by default PieChartPanel palette
        riskCard.add(pie, BorderLayout.CENTER);

        topRow.add(anomalyCard);
        topRow.add(riskCard);

        // ---- Bottom row: forecast charts ----
        JPanel bottomRow = new JPanel(new GridLayout(1, 2, UIConstants.SP_MD, 0));
        bottomRow.setOpaque(false);

        GlassCard maCard = new GlassCard(UIConstants.CARD_RADIUS);
        maCard.setLayout(new BorderLayout());
        String[] nextMonths = {"T7", "T8", "T9"};
        BarChartPanel maChart = new BarChartPanel(
            "Dự báo doanh thu - Trung bình động (triệu VNĐ)", forecastMA, nextMonths, UIConstants.AI_PURPLE);
        maCard.add(new JLabel("  Thuật toán: Moving Average (3 tháng)") {{
            setFont(UIConstants.FONT_SMALL); setForeground(UIConstants.COLOR_TEXT_MUTED);
        }}, BorderLayout.NORTH);
        maCard.add(maChart, BorderLayout.CENTER);

        GlassCard lrCard = new GlassCard(UIConstants.CARD_RADIUS);
        lrCard.setLayout(new BorderLayout());

        double[] allData = new double[histRevenue.length + forecastLR.length];
        System.arraycopy(histRevenue, 0, allData, 0, histRevenue.length);
        System.arraycopy(forecastLR, 0, allData, histRevenue.length, forecastLR.length);
        String[] allLabels = {"T1","T2","T3","T4","T5","T6","T7*","T8*","T9*"};
        LineChartPanel lrChart = new LineChartPanel(
            "Dự báo - Hồi quy tuyến tính (triệu VNĐ)", allData, allLabels, UIConstants.PRIMARY, "triệu");
        lrCard.add(new JLabel("  Thuật toán: Linear Regression") {{
            setFont(UIConstants.FONT_SMALL); setForeground(UIConstants.COLOR_TEXT_MUTED);
        }}, BorderLayout.NORTH);
        lrCard.add(lrChart, BorderLayout.CENTER);

        bottomRow.add(maCard);
        bottomRow.add(lrCard);

        JPanel root = new JPanel(new BorderLayout(0, UIConstants.SP_MD));
        root.setOpaque(false);
        root.add(topRow,    BorderLayout.NORTH);
        root.add(bottomRow, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        contentArea.add(scrollPane, BorderLayout.CENTER);
    }
}
