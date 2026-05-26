package UI.components;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Rounded search input field with a magnifier icon prefix and optional clear button.
 */
public class SearchField extends JPanel {

    private final JTextField field;
    private boolean isFocused = false;
    private static final int RADIUS = 22;

    public SearchField(String placeholder) {
        setOpaque(false);
        setLayout(new BorderLayout(4, 0));
        setPreferredSize(new Dimension(240, 38));

        // Icon label
        JLabel icon = new JLabel("\uD83D\uDD0D") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                    RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        icon.setForeground(UIConstants.COLOR_TEXT_MUTED);
        icon.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 4));

        // Text field
        field = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(UIConstants.COLOR_TEXT_MUTED);
                    g2.setFont(UIConstants.FONT_NORMAL);
                    Insets ins = getInsets();
                    g2.drawString(placeholder, ins.left, getHeight() / 2
                            + g2.getFontMetrics().getAscent() / 2 - 1);
                    g2.dispose();
                }
            }
        };
        field.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        field.setFont(UIConstants.FONT_NORMAL);
        field.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
        field.setOpaque(false);

        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { isFocused = true;  repaint(); }
            @Override public void focusLost(FocusEvent e)   { isFocused = false; repaint(); }
        });

        add(icon,  BorderLayout.WEST);
        add(field, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Outer glow when focused
        if (isFocused) {
            g2.setColor(UIConstants.INPUT_FOCUS_GLOW);
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, RADIUS, RADIUS);
        }

        // Background
        g2.setColor(UIConstants.INPUT_BG);
        g2.fillRoundRect(2, 2, getWidth() - 5, getHeight() - 5, RADIUS, RADIUS);

        // Border
        g2.setColor(isFocused ? UIConstants.INPUT_FOCUS : UIConstants.INPUT_BORDER);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, RADIUS, RADIUS);

        g2.dispose();
        super.paintComponent(g);
    }

    public String getText() { return field.getText(); }

    public void addSearchListener(ActionListener l) { field.addActionListener(l); }

    public JTextField getField() { return field; }
}
