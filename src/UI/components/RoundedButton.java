package UI.components;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Premium rounded button with smooth hover/press color transition (via Timer).
 */
public class RoundedButton extends JButton {

    private final int radius;
    private final Color defaultBg;
    private final Color hoverBg;
    private final Color pressBg;
    private Color currentBg;

    private final Timer hoverTimer;
    private float hoverProgress = 0f; // 0.0 → 1.0
    private boolean entering = false;

    public RoundedButton(String text, int radius, Color bgColor) {
        super(text);
        this.radius    = radius;
        this.defaultBg = bgColor;
        this.hoverBg   = bgColor.darker();
        this.pressBg   = bgColor.darker().darker();
        this.currentBg = bgColor;

        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setForeground(Color.WHITE);
        setFont(UIConstants.FONT_NORMAL_BOLD);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Smooth color transition
        hoverTimer = new Timer(12, e -> {
            hoverProgress = entering
                ? Math.min(hoverProgress + 0.10f, 1f)
                : Math.max(hoverProgress - 0.10f, 0f);
            currentBg = blend(defaultBg, hoverBg, hoverProgress);
            repaint();
            if ((entering && hoverProgress >= 1f) || (!entering && hoverProgress <= 0f)) {
                ((Timer) e.getSource()).stop();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { entering = true;  hoverTimer.start(); }
            @Override public void mouseExited(MouseEvent e)  { entering = false; hoverTimer.start(); }
            @Override public void mousePressed(MouseEvent e) { currentBg = pressBg; repaint(); }
            @Override public void mouseReleased(MouseEvent e){ currentBg = blend(defaultBg, hoverBg, hoverProgress); repaint(); }
        });
    }

    private Color blend(Color a, Color b, float ratio) {
        int r = (int)(a.getRed()   * (1-ratio) + b.getRed()   * ratio);
        int g = (int)(a.getGreen() * (1-ratio) + b.getGreen() * ratio);
        int bl= (int)(a.getBlue()  * (1-ratio) + b.getBlue()  * ratio);
        return new Color(
            Math.max(0, Math.min(255, r)),
            Math.max(0, Math.min(255, g)),
            Math.max(0, Math.min(255, bl))
        );
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(currentBg);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        g2.dispose();
        super.paintComponent(g);
    }
}
