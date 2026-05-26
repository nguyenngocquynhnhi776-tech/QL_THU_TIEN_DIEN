package UI.components;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Floating toast notification that appears at bottom-right and fades out automatically.
 */
public class ToastNotification extends JWindow {

    public enum Type { SUCCESS, ERROR, WARNING, INFO }

    private float alpha = 0f;
    private final Timer fadeIn;
    private final Timer fadeOut;
    private Timer hold;

    public ToastNotification(Window owner, String message, Type type) {
        super(owner);

        Color bgColor;
        Color fgColor;
        String prefix;
        switch (type) {
            case SUCCESS: bgColor = UIConstants.SUCCESS;  fgColor = Color.WHITE; prefix = "\u2713 "; break;
            case ERROR:   bgColor = UIConstants.ERROR;    fgColor = Color.WHITE; prefix = "\u2715 "; break;
            case WARNING: bgColor = UIConstants.WARNING;  fgColor = Color.WHITE; prefix = "\u26A0 "; break;
            default:      bgColor = UIConstants.PRIMARY;  fgColor = Color.WHITE; prefix = "\u2139 "; break;
        }

        JPanel panel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel lbl = new JLabel(prefix + message);
        lbl.setFont(UIConstants.FONT_NORMAL_BOLD);
        lbl.setForeground(fgColor);
        panel.add(lbl, BorderLayout.CENTER);

        setContentPane(panel);
        setSize(360, 52);

        // Position bottom-right (relative to owner)
        if (owner != null) {
            int ox = owner.getX();
            int oy = owner.getY();
            int ow = owner.getWidth();
            int oh = owner.getHeight();
            setLocation(ox + ow - getWidth() - 24, oy + oh - getHeight() - 36);
        }

        // Enable per-pixel transparency
        try { setOpacity(0f); } catch (Exception ignored) {}

        // Fade in
        fadeIn = new Timer(20, null);
        fadeIn.addActionListener(e -> {
            alpha = Math.min(alpha + 0.07f, 1f);
            try { setOpacity(alpha); } catch (Exception ignored) {}
            if (alpha >= 1f) fadeIn.stop();
        });

        // Fade out
        fadeOut = new Timer(20, null);
        fadeOut.addActionListener(e -> {
            alpha = Math.max(alpha - 0.05f, 0f);
            try { setOpacity(alpha); } catch (Exception ignored) {}
            if (alpha <= 0f) { fadeOut.stop(); dispose(); }
        });

        // Hold delay before fade-out
        hold = new Timer(2500, e -> { hold.stop(); fadeOut.start(); });
        hold.setRepeats(false);
    }

    /** Show the toast and begin its lifecycle. */
    public void showToast() {
        setVisible(true);
        fadeIn.start();
        hold.start();
    }

    /**
     * Convenience factory method.
     * @param owner   parent window
     * @param message message text in Vietnamese
     * @param type    toast type
     */
    public static void show(Window owner, String message, Type type) {
        SwingUtilities.invokeLater(() -> new ToastNotification(owner, message, type).showToast());
    }
}
