package UI.charts;

import UI.components.UIConstants;
import java.awt.*;
import javax.swing.JPanel;

/**
 * Custom Java2D Bar Chart — no external libraries required.
 * Renders vertical bars with rounded tops, grid lines, and value labels.
 */
public class BarChartPanel extends JPanel {

    private double[] data;
    private String[] labels;
    private String   title;
    private Color[]  barColors;

    private static final int PAD_TOP    = 40;
    private static final int PAD_LEFT   = 55;
    private static final int PAD_RIGHT  = 20;
    private static final int PAD_BOTTOM = 40;

    public BarChartPanel(String title, double[] data, String[] labels) {
        this.title  = title;
        this.data   = data;
        this.labels = labels;

        // Default colors cycle through palette
        Color[] defaults = {
            UIConstants.PRIMARY, UIConstants.SUCCESS, UIConstants.AI_PURPLE,
            UIConstants.WARNING, UIConstants.INFO
        };
        this.barColors = new Color[data.length];
        for (int i = 0; i < data.length; i++) barColors[i] = defaults[i % defaults.length];

        setOpaque(false);
        setMinimumSize(new Dimension(300, 200));
        setPreferredSize(new Dimension(400, 260));
    }

    public BarChartPanel(String title, double[] data, String[] labels, Color barColor) {
        this(title, data, labels);
        for (int i = 0; i < barColors.length; i++) barColors[i] = barColor;
    }

    public void setData(double[] data, String[] labels) {
        this.data   = data;
        this.labels = labels;
        if (barColors.length != data.length) {
            barColors = new Color[data.length];
            for (int i = 0; i < data.length; i++) barColors[i] = UIConstants.PRIMARY;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (data == null || data.length == 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int W = getWidth();
        int H = getHeight();

        // Title
        g2.setFont(UIConstants.FONT_SUBHEADER);
        g2.setColor(UIConstants.COLOR_TEXT_PRIMARY);
        g2.drawString(title, PAD_LEFT, 22);

        int chartW = W - PAD_LEFT - PAD_RIGHT;
        int chartH = H - PAD_TOP  - PAD_BOTTOM;
        if (chartW <= 0 || chartH <= 0) {
            g2.dispose();
            return;
        }

        // We want minY = 0 for bar charts
        double max = 0;
        for (double v : data) max = Math.max(max, v);
        if (max == 0) max = 1;

        double tempStep = max / 5.0;
        double magnitude = Math.pow(10, Math.floor(Math.log10(tempStep)));
        double normalized = tempStep / magnitude;
        double step;
        if (normalized < 1.5) step = 1 * magnitude;
        else if (normalized < 3.0) step = 2 * magnitude;
        else if (normalized < 7.0) step = 5 * magnitude;
        else step = 10 * magnitude;

        double minY = 0;
        double maxY = Math.ceil(max / step) * step;
        if (maxY == minY) {
            maxY += step;
        }
        int gridLines = (int) Math.round((maxY - minY) / step);

        // Horizontal grid lines
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                                     0, new float[]{4, 4}, 0));
        g2.setFont(UIConstants.FONT_SMALL);
        for (int i = 0; i <= gridLines; i++) {
            double frac = (double) i / gridLines;
            int y = PAD_TOP + chartH - (int)(frac * chartH);
            g2.setColor(UIConstants.COLOR_DIVIDER);
            g2.drawLine(PAD_LEFT, y, PAD_LEFT + chartW, y);
            g2.setColor(UIConstants.COLOR_TEXT_MUTED);
            double val = minY + frac * (maxY - minY);
            String yLbl = String.format("%.0f", val);
            g2.drawString(yLbl, PAD_LEFT - g2.getFontMetrics().stringWidth(yLbl) - 6, y + 4);
        }

        // Bars
        double slotW = (double) chartW / data.length;
        double barW  = slotW * 0.55;
        int arc = 8;

        for (int i = 0; i < data.length; i++) {
            int barH = (int)(((data[i] - minY) / (maxY - minY)) * chartH);
            int bx   = PAD_LEFT + (int)(i * slotW + (slotW - barW) / 2);
            int by   = PAD_TOP  + chartH - barH;

            if (barH > 0) {
                // Gradient bar fill
                GradientPaint gp = new GradientPaint(
                    bx, by, barColors[i],
                    bx, by + barH, new Color(barColors[i].getRed(),
                                              barColors[i].getGreen(),
                                              barColors[i].getBlue(), 120)
                );
                g2.setPaint(gp);
                g2.setStroke(new BasicStroke(1f));
                
                // Draw rounded top, flat bottom
                g2.fillRoundRect(bx, by, (int) barW, barH, arc, arc);
                if (barH > arc) {
                    g2.fillRect(bx, by + barH - arc, (int) barW, arc);
                }
            }

            // Value label above bar
            g2.setColor(UIConstants.COLOR_TEXT_SECONDARY);
            g2.setFont(UIConstants.FONT_SMALL_BOLD);
            String vLbl = String.format("%.0f", data[i]);
            int vlw = g2.getFontMetrics().stringWidth(vLbl);
            g2.drawString(vLbl, bx + (int)(barW / 2) - vlw / 2, by - 4);

            // X-axis label
            if (labels != null && i < labels.length) {
                g2.setColor(UIConstants.COLOR_TEXT_MUTED);
                g2.setFont(UIConstants.FONT_SMALL);
                String xl = labels[i];
                int xlw = g2.getFontMetrics().stringWidth(xl);
                g2.drawString(xl, bx + (int)(barW / 2) - xlw / 2, H - PAD_BOTTOM + 16);
            }
        }

        g2.dispose();
    }
}
