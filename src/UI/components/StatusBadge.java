package UI.components;

import java.awt.*;
import javax.swing.JLabel;

/**
 * Pill-shaped status badge with semantic color coding.
 */
public class StatusBadge extends JLabel {

    public enum Status {
        PAID,       // Đã thanh toán  — green
        UNPAID,     // Chưa thanh toán — red/pink
        WARNING,    // Cảnh báo        — orange
        ACTIVE,     // Hoạt động       — green
        LOCKED,     // Bị khóa         — gray
        ADMIN,      // Quản trị        — primary
        STAFF,      // Nhân viên       — blue
        NORMAL,     // Bình thường     — teal
        AI_HIGH,    // Nguy cơ cao     — red
        AI_MEDIUM,  // Nguy cơ TB      — orange
        AI_LOW      // Nguy cơ thấp    — green
    }

    private Color bgColor;
    private Color fgColor;
    private static final int ARC = 20;

    public StatusBadge(String text, Status status) {
        super(text, JLabel.CENTER);
        setFont(UIConstants.FONT_SMALL_BOLD);
        setOpaque(false);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 10, 3, 10));
        applyStatus(status);
    }

    private void applyStatus(Status status) {
        switch (status) {
            case PAID:
            case ACTIVE:
            case NORMAL:
            case AI_LOW:
                bgColor = UIConstants.SUCCESS_BG;
                fgColor = UIConstants.SUCCESS;
                break;
            case UNPAID:
            case AI_HIGH:
                bgColor = UIConstants.ERROR_BG;
                fgColor = UIConstants.ERROR;
                break;
            case WARNING:
            case AI_MEDIUM:
                bgColor = UIConstants.WARNING_BG;
                fgColor = UIConstants.WARNING;
                break;
            case LOCKED:
                bgColor = new Color(0xEEEEEE);
                fgColor = new Color(0x888888);
                break;
            case ADMIN:
                bgColor = new Color(UIConstants.PRIMARY.getRed(),
                                    UIConstants.PRIMARY.getGreen(),
                                    UIConstants.PRIMARY.getBlue(), 30);
                fgColor = UIConstants.PRIMARY_DARK;
                break;
            case STAFF:
                bgColor = UIConstants.INFO_BG;
                fgColor = UIConstants.INFO;
                break;
            default:
                bgColor = UIConstants.COLOR_BORDER_LIGHT;
                fgColor = UIConstants.COLOR_TEXT_SECONDARY;
        }
        setForeground(fgColor);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bgColor);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.width  = Math.max(d.width,  60);
        d.height = Math.max(d.height, 22);
        return d;
    }
}
