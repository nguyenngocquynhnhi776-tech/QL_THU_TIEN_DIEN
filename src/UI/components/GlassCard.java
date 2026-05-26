package UI.components;

import UI.theme.ThemeManager;
import java.awt.*;
import javax.swing.JPanel;

/**
 * Glassmorphism card with soft shadow, rounded corners, and optional left accent bar.
 */
public class GlassCard extends JPanel {

    private final int radius;
    private final Color accentColor;

    public GlassCard() {
        this(UIConstants.CARD_RADIUS, null);
    }

    public GlassCard(int radius) {
        this(radius, null);
    }

    public GlassCard(int radius, Color accentColor) {
        this.radius      = radius;
        this.accentColor = accentColor;
        setOpaque(false);
        int leftPad = (accentColor != null) ? UIConstants.SP_MD + 6 : UIConstants.SP_MD;
        setBorder(javax.swing.BorderFactory.createEmptyBorder(
            UIConstants.SP_MD, leftPad, UIConstants.SP_MD, UIConstants.SP_MD));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int margin = 4;
        int x = margin;
        int y = margin + 2;
        int w = getWidth()  - margin * 2 - 2;
        int h = getHeight() - margin * 2 - 4;

        // Shadow
        ThemeManager.drawCardShadow(g2, x, y, w, h, radius);

        // Card background — near-opaque white
        g2.setColor(UIConstants.COLOR_CARD_BG);
        g2.fillRoundRect(x, y, w, h, radius, radius);

        // Accent bar on left edge
        if (accentColor != null) {
            g2.setColor(accentColor);
            g2.fillRoundRect(x, y, 5, h, 4, 4);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
