package UI.components;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Premium sidebar navigation button with icon, label, active indicator bar,
 * and smooth hover animation.
 */
public class SidebarButton extends JButton {

    private boolean selected    = false;
    private float   hoverAlpha  = 0f;
    private boolean entering    = false;
    private final Timer hoverTimer;

    private static final int ACTIVE_BAR_W  = 4;
    private static final int CORNER_RADIUS = 12;

    public SidebarButton(String iconChar, String label) {
        super();
        setLayout(new BorderLayout(0, 0));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH - 16, 46));
        setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH - 16, 46));

        // Icon
        JLabel iconLbl = new JLabel(iconChar);
        iconLbl.setFont(UIConstants.FONT_ICON_SIDEBAR);
        iconLbl.setForeground(UIConstants.COLOR_SIDEBAR_TEXT);
        iconLbl.setPreferredSize(new Dimension(40, 46));
        iconLbl.setHorizontalAlignment(SwingConstants.CENTER);
        iconLbl.setVerticalAlignment(SwingConstants.CENTER);

        // Label text
        JLabel textLbl = new JLabel(label);
        textLbl.setFont(UIConstants.FONT_NORMAL);
        textLbl.setForeground(UIConstants.COLOR_SIDEBAR_TEXT);

        add(iconLbl,  BorderLayout.WEST);
        add(textLbl,  BorderLayout.CENTER);

        // Smooth hover
        hoverTimer = new Timer(15, e -> {
            hoverAlpha = entering
                ? Math.min(hoverAlpha + 0.08f, 1f)
                : Math.max(hoverAlpha - 0.08f, 0f);
            repaint();
            if ((entering && hoverAlpha >= 1f) || (!entering && hoverAlpha <= 0f)) {
                ((Timer) e.getSource()).stop();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { entering = true;  hoverTimer.start(); }
            @Override public void mouseExited(MouseEvent e)  { entering = false; hoverTimer.start(); }
        });

        // Forward click to registered action listeners (already done by JButton)
    }

    /** Legacy constructor — use icon + label constructor for best results */
    public SidebarButton(String label) {
        this("\u25B6", label);
    }

    public void setSelectedStatus(boolean sel) {
        this.selected = sel;
        // Update child label colors
        for (Component c : getComponents()) {
            if (c instanceof JLabel) {
                ((JLabel) c).setForeground(sel ? Color.WHITE : UIConstants.COLOR_SIDEBAR_TEXT);
            }
        }
        repaint();
    }

    public boolean isSelectedStatus() { return selected; }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (selected) {
            // Active background
            g2.setColor(UIConstants.COLOR_SIDEBAR_ACTIVE_BG);
            g2.fillRoundRect(0, 0, w - 1, h - 1, CORNER_RADIUS, CORNER_RADIUS);

            // Active left bar
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 6, ACTIVE_BAR_W, h - 12, 4, 4);

        } else if (hoverAlpha > 0f) {
            // Hover overlay
            int alpha = (int)(hoverAlpha * 38);
            g2.setColor(new Color(255, 255, 255, alpha));
            g2.fillRoundRect(0, 0, w - 1, h - 1, CORNER_RADIUS, CORNER_RADIUS);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
