package UI.components;

import UI.theme.ThemeManager;
import java.awt.*;
import javax.swing.JPanel;

/**
 * Rounded panel with soft drop shadow and optional gradient fill.
 */
public class RoundedPanel extends JPanel {

    private int cornerRadius;
    private Color backgroundColor;
    private boolean drawShadow;

    public RoundedPanel(int radius, Color bgColor) {
        this(radius, bgColor, true);
    }

    public RoundedPanel(int radius) {
        this(radius, Color.WHITE, true);
    }

    public RoundedPanel(int radius, Color bgColor, boolean drawShadow) {
        this.cornerRadius   = radius;
        this.backgroundColor = bgColor;
        this.drawShadow     = drawShadow;
        setOpaque(false);
    }

    @Override
    public void setBackground(Color bg) {
        this.backgroundColor = bg;
        super.setBackground(bg);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int m = drawShadow ? 4 : 0;
        int yOff = drawShadow ? 2 : 0;
        int w = getWidth()  - m * 2 - (drawShadow ? 2 : 0);
        int h = getHeight() - m * 2 - (drawShadow ? 4 : 0);

        if (drawShadow) {
            ThemeManager.drawCardShadow(g2, m, m + yOff, w, h, cornerRadius);
        }

        Color bg = (backgroundColor != null) ? backgroundColor : getBackground();
        if (bg == null) bg = Color.WHITE;
        g2.setColor(bg);
        g2.fillRoundRect(m, m, w, h, cornerRadius, cornerRadius);

        g2.dispose();
        super.paintComponent(g);
    }
}
