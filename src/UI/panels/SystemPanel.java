package UI.panels;

import UI.components.*;
import javax.swing.*;
import java.awt.*;

/**
 * Hệ thống — sao lưu, phục hồi, nhật ký hoạt động.
 */
public class SystemPanel extends BasePanel {

    public SystemPanel() {
        super("Hệ thống", "Sao lưu dữ liệu, phục hồi và theo dõi nhật ký hoạt động");

        JPanel root = new JPanel(new BorderLayout(0, UIConstants.SP_MD));
        root.setOpaque(false);

        // ---- Top: backup/restore actions ----
        JPanel actionRow = new JPanel(new GridLayout(1, 3, UIConstants.SP_MD, 0));
        actionRow.setOpaque(false);
        actionRow.setPreferredSize(new Dimension(0, 120));

        actionRow.add(actionCard("\u25BA", "Sao lưu dữ liệu",
            "Tạo bản sao lưu toàn bộ cơ sở dữ liệu", UIConstants.PRIMARY,
            e -> {
                boolean ok = UI.theme.ThemeManager.showConfirmDialog(this,
                    "Bắt đầu sao lưu toàn bộ dữ liệu hệ thống?", "Sao lưu dữ liệu");
                if (ok) ToastNotification.show(SwingUtilities.getWindowAncestor(this),
                    "Sao lưu dữ liệu thành công!", ToastNotification.Type.SUCCESS);
            }));

        actionRow.add(actionCard("\u25C6", "Phục hồi dữ liệu",
            "Khôi phục từ bản sao lưu đã chọn", UIConstants.WARNING,
            e -> {
                boolean ok = UI.theme.ThemeManager.showConfirmDialog(this,
                    "Phục hồi dữ liệu sẽ ghi đè dữ liệu hiện tại. Tiếp tục?",
                    "Phục hồi dữ liệu");
                if (ok) ToastNotification.show(SwingUtilities.getWindowAncestor(this),
                    "Đã phục hồi dữ liệu thành công!", ToastNotification.Type.SUCCESS);
            }));

        actionRow.add(actionCard("\u2714", "Xuất dữ liệu",
            "Xuất toàn bộ dữ liệu ra file Excel/CSV", UIConstants.SUCCESS,
            e -> ToastNotification.show(SwingUtilities.getWindowAncestor(this),
                "Đang xuất dữ liệu...", ToastNotification.Type.INFO)));

        // ---- System info card ----
        GlassCard infoCard = new GlassCard(UIConstants.CARD_RADIUS);
        infoCard.setLayout(new GridLayout(2, 3, UIConstants.SP_LG, UIConstants.SP_SM));
        infoCard.setPreferredSize(new Dimension(0, 90));

        String[][] sysInfo = {
            {"Phiên bản",      "Electra Manager AI v1.0"},
            {"Cơ sở dữ liệu", "SQL Server (Đã kết nối)"},
            {"Môi trường",     "Windows / JDK 11+"},
            {"Sao lưu cuối",   "18/05/2026 03:00"},
            {"Tổng người dùng","4 tài khoản"},
            {"Tổng số hộ",     "1.500 hộ"},
        };
        for (String[] info : sysInfo) {
            JPanel cell = new JPanel(new BorderLayout(0, 2));
            cell.setOpaque(false);
            JLabel key = new JLabel(info[0]);
            key.setFont(UIConstants.FONT_SMALL);
            key.setForeground(UIConstants.COLOR_TEXT_MUTED);
            JLabel val = new JLabel(info[1]);
            val.setFont(UIConstants.FONT_NORMAL_BOLD);
            val.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
            cell.add(key, BorderLayout.NORTH);
            cell.add(val, BorderLayout.CENTER);
            infoCard.add(cell);
        }

        // ---- Activity log table ----
        GlassCard logCard = new GlassCard(UIConstants.CARD_RADIUS);
        logCard.setLayout(new BorderLayout(0, UIConstants.SP_SM));

        JLabel logTitle = new JLabel("\u25B6  Nhật ký hoạt động hệ thống");
        logTitle.setFont(UIConstants.FONT_SUBHEADER);
        logTitle.setForeground(UIConstants.COLOR_TEXT_PRIMARY);

        ModernTable logTable = new ModernTable(
            new String[]{"Thời gian","Người dùng","Hành động","Đối tượng","Trạng thái"});
        logTable.setColumnWidths(150, 120, 180, 150, 90);

        Object[][] logs = {
            {"18/05/2026 09:45", "admin",  "Tạo hóa đơn hàng loạt", "Tháng 5/2026",  "Thành công"},
            {"18/05/2026 09:10", "admin",  "Đăng nhập hệ thống",    "admin",           "Thành công"},
            {"18/05/2026 08:45", "nv01",   "Nhập chỉ số điện",      "H-024",           "Thành công"},
            {"18/05/2026 08:30", "nv01",   "Đăng nhập hệ thống",    "nv01",            "Thành công"},
            {"17/05/2026 17:30", "nv02",   "Xác nhận thanh toán",   "INV-1480",        "Thành công"},
            {"17/05/2026 10:00", "nv03",   "Đăng nhập hệ thống",    "nv03",            "Thất bại"},
        };

        // Status badge in last column
        logTable.setColumnRenderer(4, (tbl, val, sel, foc, row, col) -> {
            String s = val == null ? "" : val.toString();
            StatusBadge.Status st = s.equals("Thành công") ? StatusBadge.Status.ACTIVE : StatusBadge.Status.LOCKED;
            return new StatusBadge(s, st);
        });
        for (Object[] r : logs) logTable.addRow(r);

        logCard.add(logTitle, BorderLayout.NORTH);
        logCard.add(logTable, BorderLayout.CENTER);

        JPanel topBlock = new JPanel(new BorderLayout(0, UIConstants.SP_MD));
        topBlock.setOpaque(false);
        topBlock.add(actionRow, BorderLayout.NORTH);
        topBlock.add(infoCard,  BorderLayout.CENTER);

        root.add(topBlock, BorderLayout.NORTH);
        root.add(logCard,  BorderLayout.CENTER);

        contentArea.add(root, BorderLayout.CENTER);
    }

    private GlassCard actionCard(String icon, String title, String desc, Color color,
                                  java.awt.event.ActionListener action) {
        GlassCard card = new GlassCard(UIConstants.CARD_RADIUS, color);
        card.setLayout(new BorderLayout(UIConstants.SP_SM, 0));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(title); t.setFont(UIConstants.FONT_SUBHEADER); t.setForeground(color);
        JLabel d = new JLabel(desc);  d.setFont(UIConstants.FONT_SMALL);     d.setForeground(UIConstants.COLOR_TEXT_MUTED);
        t.setAlignmentX(LEFT_ALIGNMENT);
        d.setAlignmentX(LEFT_ALIGNMENT);
        textPanel.add(t); textPanel.add(Box.createVerticalStrut(4)); textPanel.add(d);

        RoundedButton btn = new RoundedButton(title, UIConstants.BUTTON_RADIUS, color);
        btn.setPreferredSize(new Dimension(130, 32));
        btn.addActionListener(action);

        card.add(textPanel, BorderLayout.CENTER);
        card.add(btn, BorderLayout.SOUTH);
        return card;
    }
}
