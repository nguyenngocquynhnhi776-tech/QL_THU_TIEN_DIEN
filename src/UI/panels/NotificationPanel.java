package UI.panels;

import UI.components.*;
import javax.swing.*;
import java.awt.*;

/**
 * Thông báo — danh sách nhắc nhở thanh toán và cảnh báo hệ thống.
 */
public class NotificationPanel extends BasePanel {

    public NotificationPanel() {
        super("Thông báo", "Nhắc nhở thanh toán và cảnh báo từ hệ thống");

        JPanel root = new JPanel(new BorderLayout(0, UIConstants.SP_MD));
        root.setOpaque(false);

        // ---- Toolbar ----
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, UIConstants.SP_MD, 0));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIConstants.SP_SM, 0));
        btnRow.setOpaque(false);

        RoundedButton sendReminder = new RoundedButton("Gửi nhắc nhở thanh toán", UIConstants.BUTTON_RADIUS, UIConstants.PRIMARY);
        sendReminder.setPreferredSize(new Dimension(220, 36));
        sendReminder.addActionListener(e -> {
            boolean ok = UI.theme.ThemeManager.showConfirmDialog(this,
                "Gửi SMS nhắc nhở thanh toán cho tất cả 255 hộ chưa thanh toán?",
                "Xác nhận gửi thông báo");
            if (ok) ToastNotification.show(SwingUtilities.getWindowAncestor(this),
                "Đã gửi nhắc nhở cho 255 hộ!", ToastNotification.Type.SUCCESS);
        });

        RoundedButton markAllRead = new RoundedButton("Đánh dấu đã đọc", UIConstants.BUTTON_RADIUS, UIConstants.COLOR_TEXT_MUTED);
        markAllRead.setPreferredSize(new Dimension(160, 36));

        btnRow.add(markAllRead); btnRow.add(sendReminder);
        toolbar.add(btnRow, BorderLayout.EAST);

        // ---- Notification list ----
        GlassCard listCard = new GlassCard(UIConstants.CARD_RADIUS);
        listCard.setLayout(new BorderLayout());

        JPanel notifList = new JPanel();
        notifList.setOpaque(false);
        notifList.setLayout(new BoxLayout(notifList, BoxLayout.Y_AXIS));

        Object[][] notifs = {
            {"\u26A0", "Cảnh báo",    "H-003 tiêu thụ tăng 160% — có thể có sự cố rò điện!", "Vừa xong",   UIConstants.ERROR},
            {"\u26A0", "Nhắc nhở",    "255 hộ chưa thanh toán hóa đơn tháng 5/2026.",         "5 phút",     UIConstants.WARNING},
            {"\u25C6", "AI Cảnh báo", "5 hộ có nguy cơ nợ tiền cao dựa trên lịch sử.",         "12 phút",    UIConstants.AI_PURPLE},
            {"\u2714", "Hệ thống",    "Tạo hóa đơn tháng 5/2026 cho 1.500 hộ thành công.",     "1 giờ",      UIConstants.SUCCESS},
            {"\u26A0", "Nhắc nhở",    "Hộ H-006 nợ tiền 4 tháng liên tiếp (1.250.000 đ).",     "2 giờ",      UIConstants.ERROR},
            {"\u25B6", "Hệ thống",    "Sao lưu dữ liệu tự động hoàn tất lúc 03:00.",            "Hôm qua",    UIConstants.PRIMARY},
            {"\u25B6", "Hệ thống",    "Nhân viên nv03 đăng nhập từ thiết bị lạ.",               "Hôm qua",    UIConstants.WARNING},
        };

        for (Object[] n : notifs) {
            String icon   = (String) n[0];
            String type   = (String) n[1];
            String msg    = (String) n[2];
            String time   = (String) n[3];
            Color  color  = (Color)  n[4];

            JPanel item = new JPanel(new BorderLayout(UIConstants.SP_MD, 0));
            item.setOpaque(false);
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            item.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.COLOR_DIVIDER));

            // Icon circle
            JPanel iconCircle = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
                    g2.fillOval(0, 0, getWidth()-1, getHeight()-1);
                    g2.setColor(color);
                    g2.setFont(UIConstants.FONT_SMALL_BOLD);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(icon,
                        (getWidth() - fm.stringWidth(icon)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2.dispose();
                }
            };
            iconCircle.setOpaque(false);
            iconCircle.setPreferredSize(new Dimension(38, 38));

            // Text
            JPanel textPanel = new JPanel(new BorderLayout(0, 2));
            textPanel.setOpaque(false);
            JLabel typeLbl = new JLabel(type);
            typeLbl.setFont(UIConstants.FONT_SMALL_BOLD); typeLbl.setForeground(color);
            JLabel msgLbl = new JLabel(msg);
            msgLbl.setFont(UIConstants.FONT_SMALL); msgLbl.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
            textPanel.add(typeLbl, BorderLayout.NORTH);
            textPanel.add(msgLbl,  BorderLayout.CENTER);

            JLabel timeLbl = new JLabel(time);
            timeLbl.setFont(UIConstants.FONT_SMALL); timeLbl.setForeground(UIConstants.COLOR_TEXT_MUTED);

            item.add(iconCircle, BorderLayout.WEST);
            item.add(textPanel,  BorderLayout.CENTER);
            item.add(timeLbl,    BorderLayout.EAST);
            item.setBorder(BorderFactory.createEmptyBorder(UIConstants.SP_SM, 0, UIConstants.SP_SM, 0));

            notifList.add(item);
        }

        JScrollPane scroll = new JScrollPane(notifList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        listCard.add(scroll, BorderLayout.CENTER);

        root.add(toolbar,  BorderLayout.NORTH);
        root.add(listCard, BorderLayout.CENTER);

        contentArea.add(root, BorderLayout.CENTER);
    }
}
