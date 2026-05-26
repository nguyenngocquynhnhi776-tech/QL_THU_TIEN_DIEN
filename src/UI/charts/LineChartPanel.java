package UI.charts;

import UI.components.UIConstants;
import UI.theme.ThemeManager;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.JPanel;

/**
 * Custom Java2D Line Chart — no external libraries required.
 * Displays one or more data series as smooth Bezier curves.
 */
public class LineChartPanel extends JPanel {

    private double[] data;
    private String[] labels;
    private String   title;
    private Color    lineColor;
    private String   valueUnit;

    private static final int PAD_TOP    = 40;
    private static final int PAD_LEFT   = 60;
    private static final int PAD_RIGHT  = 20;
    private static final int PAD_BOTTOM = 40;

    public LineChartPanel(String title, double[] data, String[] labels, Color lineColor, String valueUnit) {
        this.title     = title;
        this.data      = data;
        this.labels    = labels;
        this.lineColor = lineColor;
        this.valueUnit = valueUnit;
        setOpaque(false);
        setMinimumSize(new Dimension(300, 200));
        setPreferredSize(new Dimension(400, 260));
    }

    public LineChartPanel(String title, double[] data, String[] labels) {
        this(title, data, labels, UIConstants.PRIMARY, "");
    }

    /** Update data and repaint. */
    public void setData(double[] data, String[] labels) {
        this.data   = data;
        this.labels = labels;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (data == null || data.length == 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,       RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,  RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,          RenderingHints.VALUE_RENDER_QUALITY);

        int W = getWidth();
        int H = getHeight();

        // Title
        g2.setFont(UIConstants.FONT_SUBHEADER);
        g2.setColor(UIConstants.COLOR_TEXT_PRIMARY);
        g2.drawString(title, PAD_LEFT, 22);

        // Chart area
        int chartW = W - PAD_LEFT - PAD_RIGHT;
        int chartH = H - PAD_TOP  - PAD_BOTTOM;
        if (chartW <= 0 || chartH <= 0) {
            g2.dispose();
            return;
        }

        // Find min, max, range, step, minY, maxY, gridLines
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double v : data) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        if (min == max) {
            min = min * 0.9;
            max = max * 1.1;
        }
        if (min == 0 && max == 0) {
            max = 10;
        }

        double range = max - min;
        double tempStep = range / 4.0;
        double magnitude = Math.pow(10, Math.floor(Math.log10(tempStep)));
        double normalized = tempStep / magnitude;
        double step;
        if (normalized < 1.5) step = 1 * magnitude;
        else if (normalized < 3.0) step = 2 * magnitude;
        else if (normalized < 7.0) step = 5 * magnitude;
        else step = 10 * magnitude;

        double minY = Math.floor(min / step) * step;
        if (minY > 0 && minY < step * 2) {
            minY = 0;
        }
        double maxY = Math.ceil(max / step) * step;
        if (maxY == minY) {
            maxY += step;
        }
        int gridLines = (int) Math.round((maxY - minY) / step);

        // Grid lines
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                                     0, new float[]{4, 4}, 0));
        g2.setFont(UIConstants.FONT_SMALL);
        for (int i = 0; i <= gridLines; i++) {
            double frac = (double) i / gridLines;
            int y = PAD_TOP + chartH - (int)(frac * chartH);
            g2.setColor(UIConstants.COLOR_DIVIDER);
            g2.drawLine(PAD_LEFT, y, PAD_LEFT + chartW, y);
            // Y-axis labels
            g2.setColor(UIConstants.COLOR_TEXT_MUTED);
            double val = minY + frac * (maxY - minY);
            String yLabel = String.format("%.0f", val);
            g2.drawString(yLabel, PAD_LEFT - g2.getFontMetrics().stringWidth(yLabel) - 6, y + 4);
        }

        // X-axis labels
        double stepX = (data.length > 1) ? (double) chartW / (data.length - 1) : chartW;
        if (labels != null) {
            for (int i = 0; i < labels.length && i < data.length; i++) {
                int x = PAD_LEFT + (int)(i * stepX);
                g2.setColor(UIConstants.COLOR_TEXT_MUTED);
                String lbl = labels[i];
                int lw = g2.getFontMetrics().stringWidth(lbl);
                g2.drawString(lbl, x - lw / 2, H - PAD_BOTTOM + 16);
            }
        }

        // Build smooth curve path
        g2.setStroke(new BasicStroke(1f));
        GeneralPath fill = new GeneralPath();
        GeneralPath line = new GeneralPath();

        int[] px = new int[data.length];
        int[] py = new int[data.length];
        double rangeY = maxY - minY;
        for (int i = 0; i < data.length; i++) {
            px[i] = PAD_LEFT + (int)(i * stepX);
            py[i] = PAD_TOP  + chartH - (int)(((data[i] - minY) / rangeY) * chartH);
        }

        fill.moveTo(px[0], PAD_TOP + chartH);
        fill.lineTo(px[0], py[0]);
        line.moveTo(px[0], py[0]);

        for (int i = 1; i < data.length; i++) {
            int cx1 = px[i-1] + (int)((px[i] - px[i-1]) * 0.5);
            int cx2 = px[i]   - (int)((px[i] - px[i-1]) * 0.5);
            line.curveTo(cx1, py[i-1], cx2, py[i], px[i], py[i]);
            fill.curveTo(cx1, py[i-1], cx2, py[i], px[i], py[i]);
        }

        fill.lineTo(px[data.length-1], PAD_TOP + chartH);
        fill.closePath();

        // Fill gradient under the curve
        GradientPaint fillGrad = new GradientPaint(
            0, PAD_TOP, new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 60),
            0, PAD_TOP + chartH, new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 5)
        );
        g2.setPaint(fillGrad);
        g2.fill(fill);

        // Stroke the line
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(lineColor);
        g2.draw(line);

        // Data point dots
        for (int i = 0; i < data.length; i++) {
            g2.setColor(Color.WHITE);
            g2.fillOval(px[i] - 4, py[i] - 4, 9, 9);
            g2.setColor(lineColor);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(px[i] - 4, py[i] - 4, 9, 9);
        }

        // Data labels above dots
        g2.setFont(UIConstants.FONT_SMALL_BOLD);
        for (int i = 0; i < data.length; i++) {
            g2.setColor(UIConstants.COLOR_TEXT_SECONDARY);
            String vLbl = String.format("%.0f", data[i]);
            int vlw = g2.getFontMetrics().stringWidth(vLbl);
            g2.drawString(vLbl, px[i] - vlw / 2, py[i] - 8);
        }

        g2.dispose();
    }
}
