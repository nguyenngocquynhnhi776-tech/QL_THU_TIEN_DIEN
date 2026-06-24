package UI.components;

import UI.theme.ThemeManager;
import java.awt.*;
import javax.swing.*;

/**
 * Premium statistics summary card.
 * Displays a Unicode icon, title, large value, and optional trend indicator.
 */
public class StatsCard extends JPanel {

    private final String icon;
    private final String titleText;
    private final String valueText;
    private final String trendText;
    private final boolean trendUp;
    private final Color  iconColor;
    private static final int RADIUS = UIConstants.CARD_RADIUS;

    /**
     * @param icon      Unicode character used as icon (e.g. "\u26A1")
     * @param titleText Short description label
     * @param valueText Main metric value
     * @param trendText Trend label (e.g. "+12%") — pass null to hide
     * @param trendUp   true = green arrow up, false = red arrow down
     * @param iconColor Color for the icon circle background
     */
    public StatsCard(String icon, String titleText, String valueText,
                     String trendText, boolean trendUp, Color iconColor) {
        this.icon      = icon;
        this.titleText = titleText;
        this.valueText = valueText;
        this.trendText = trendText;
        this.trendUp   = trendUp;
        this.iconColor = iconColor;

        setOpaque(false);
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder(22, 20, 18, 20));

        // Left content column
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel lTitle = new JLabel(titleText);
        lTitle.setFont(UIConstants.FONT_SMALL);
        lTitle.setForeground(UIConstants.COLOR_TEXT_MUTED);
        lTitle.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lValue = new JLabel(valueText);
        lValue.setFont(UIConstants.FONT_STAT_VALUE);
        lValue.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
        lValue.setAlignmentX(LEFT_ALIGNMENT);

        left.add(lTitle);
        left.add(Box.createVerticalStrut(6));
        left.add(lValue);

        if (trendText != null && !trendText.isEmpty()) {
            left.add(Box.createVerticalStrut(8));
            JLabel lTrend = new JLabel((trendUp ? "\u25B2 " : "\u25BC ") + trendText);
            lTrend.setFont(UIConstants.FONT_SMALL_BOLD);
            lTrend.setForeground(trendUp ? UIConstants.SUCCESS : UIConstants.ERROR);
            lTrend.setAlignmentX(LEFT_ALIGNMENT);
            left.add(lTrend);
        }

        // Right icon circle
        JPanel iconCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Tinted background
                Color bg = new Color(iconColor.getRed(), iconColor.getGreen(),
                                     iconColor.getBlue(), 30);
                g2.setColor(bg);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                // Icon
                g2.setColor(iconColor);
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth()  - fm.stringWidth(icon)) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2 - 1;
                g2.drawString(icon, tx, ty);
                g2.dispose();
            }
        };
        iconCircle.setOpaque(false);
        iconCircle.setPreferredSize(new Dimension(48, 48));

        add(left, BorderLayout.CENTER);
        JPanel rightWrap = new JPanel(new GridBagLayout());
        rightWrap.setOpaque(false);
        rightWrap.add(iconCircle);
        add(rightWrap, BorderLayout.EAST);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int m = 4;
        int w = getWidth()  - m * 2 - 2;
        int h = getHeight() - m * 2 - 4;

        ThemeManager.drawCardShadow(g2, m, m + 2, w, h, RADIUS);

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(m, m, w, h, RADIUS, RADIUS);

        // Thin top accent line
        g2.setColor(new Color(iconColor.getRed(), iconColor.getGreen(), iconColor.getBlue(), 100));
        g2.setStroke(new BasicStroke(3f));
        g2.drawLine(m + RADIUS, m + 1, m + w - RADIUS, m + 1);

        g2.dispose();
        super.paintComponent(g);
    }
}
