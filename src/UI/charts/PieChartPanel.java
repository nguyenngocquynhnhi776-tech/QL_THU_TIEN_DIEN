package UI.charts;

import UI.components.UIConstants;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.JPanel;

/**
 * Custom Java2D Pie/Donut Chart — no external libraries required.
 * Renders slices with labels and a centre-hole donut style.
 */
public class PieChartPanel extends JPanel {

    private double[]  values;
    private String[]  labels;
    private Color[]   colors;
    private String    title;
    private String    centerLabel;

    private static final Color[] DEFAULT_COLORS = {
        UIConstants.PRIMARY,   UIConstants.SUCCESS,  UIConstants.WARNING,
        UIConstants.ERROR,     UIConstants.AI_PURPLE, UIConstants.INFO
    };

    public PieChartPanel(String title, double[] values, String[] labels) {
        this.title  = title;
        this.values = values;
        this.labels = labels;
        this.colors = new Color[values.length];
        for (int i = 0; i < values.length; i++) colors[i] = DEFAULT_COLORS[i % DEFAULT_COLORS.length];
        setOpaque(false);
        setMinimumSize(new Dimension(240, 240));
        setPreferredSize(new Dimension(240, 260));
    }

    public void setCenterLabel(String label) { this.centerLabel = label; }

    public void setData(double[] values, String[] labels) {
        this.values = values;
        this.labels = labels;
        this.colors = new Color[values.length];
        for (int i = 0; i < values.length; i++) colors[i] = DEFAULT_COLORS[i % DEFAULT_COLORS.length];
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (values == null || values.length == 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int W = getWidth();
        int H = getHeight();

        // Title
        g2.setFont(UIConstants.FONT_SUBHEADER);
        g2.setColor(UIConstants.COLOR_TEXT_PRIMARY);
        g2.drawString(title, 10, 22);

        // Legend height estimate
        int legendH = values.length * 22;
        int pieSize = Math.min(W - 20, H - 40 - legendH - 10);
        pieSize = Math.max(pieSize, 80);

        int cx = (W - pieSize) / 2;
        int cy = 36;

        // Slices
        double total = 0;
        for (double v : values) total += v;
        if (total == 0) total = 1;

        double startAngle = -90;
        for (int i = 0; i < values.length; i++) {
            double sweep = (values[i] / total) * 360.0;

            // Shadow
            g2.setColor(new Color(0, 0, 0, 20));
            g2.fillArc(cx + 2, cy + 4, pieSize, pieSize, (int) startAngle, (int) sweep);

            // Slice
            g2.setColor(colors[i]);
            g2.fillArc(cx, cy, pieSize, pieSize, (int) startAngle, (int) sweep);

            // Gap stroke
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawArc(cx, cy, pieSize, pieSize, (int) startAngle, (int) sweep);

            startAngle += sweep;
        }

        // Donut hole
        int holeSize = (int)(pieSize * 0.52);
        int hx = cx + (pieSize - holeSize) / 2;
        int hy = cy + (pieSize - holeSize) / 2;
        g2.setColor(Color.WHITE);
        g2.fillOval(hx, hy, holeSize, holeSize);

        // Center label
        if (centerLabel != null) {
            g2.setFont(UIConstants.FONT_NORMAL_BOLD);
            g2.setColor(UIConstants.COLOR_TEXT_SECONDARY);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(centerLabel,
                hx + (holeSize - fm.stringWidth(centerLabel)) / 2,
                hy + (holeSize + fm.getAscent() - fm.getDescent()) / 2);
        }

        // Legend
        int legendY = cy + pieSize + 16;
        g2.setFont(UIConstants.FONT_SMALL);
        for (int i = 0; i < values.length; i++) {
            int lx = 14;
            int ly = legendY + i * 22;
            g2.setColor(colors[i]);
            g2.fillRoundRect(lx, ly - 10, 14, 14, 4, 4);
            g2.setColor(UIConstants.COLOR_TEXT_PRIMARY);
            String pct = String.format("%.1f%%", (values[i] / total) * 100);
            String lbl = (labels != null && i < labels.length ? labels[i] : "Dữ liệu " + (i+1))
                         + "  " + pct;
            g2.drawString(lbl, lx + 20, ly);
        }

        g2.dispose();
    }
}
