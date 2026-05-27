package UI.components;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Rounded text field with focus glow ring.
 */
public class RoundedTextField extends JTextField {

    private boolean focused = false;
    private static final int RADIUS = UIConstants.INPUT_RADIUS;

    public RoundedTextField(int columns) {
        super(columns);
        init();
    }

    public RoundedTextField() {
        init();
    }

    private void init() {
        setOpaque(false);
        setFont(UIConstants.FONT_NORMAL);
        setForeground(UIConstants.COLOR_TEXT_PRIMARY);
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { focused = true;  repaint(); }
            @Override public void focusLost(FocusEvent e)   { focused = false; repaint(); }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.height = 38;
        return d;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        boolean editable = isEditable();

        // Glow
        if (focused && editable) {
            g2.setColor(UIConstants.INPUT_FOCUS_GLOW);
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, RADIUS + 2, RADIUS + 2);
        }

        // Background
        g2.setColor(editable ? UIConstants.INPUT_BG : new Color(0xECF0F0));
        g2.fillRoundRect(2, 2, getWidth() - 5, getHeight() - 5, RADIUS, RADIUS);

        // Border
        g2.setColor((focused && editable) ? UIConstants.INPUT_FOCUS : UIConstants.INPUT_BORDER);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, RADIUS, RADIUS);

        g2.dispose();
        super.paintComponent(g);
    }
}
