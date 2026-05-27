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
        infoCard.setPreferredSize(new Dimension(0, 100));

        // Các JLabel cần cập nhật từ DB
        JLabel totalUserVal = new JLabel("Đang tải...");
        JLabel totalHHVal   = new JLabel("Đang tải...");

        String[][] sysInfo = {
            {"Phiên bản",       "Electra Manager AI v1.0"},
            {"Cơ sở dữ liệu",  "SQL Server (Đã kết nối)"},
            {"Môi trường",      "Windows / JDK 11+"},
            {"Sao lưu cuối",    "18/05/2026 03:00"},
            {"Tổng người dùng", null},   // null = dùng JLabel động
            {"Tổng số hộ",      null},   // null = dùng JLabel động
        };

        int dynIdx = 0;
        for (String[] info : sysInfo) {
            JPanel cell = new JPanel(new BorderLayout(0, 2));
            cell.setOpaque(false);
            JLabel key = new JLabel(info[0]);
            key.setFont(UIConstants.FONT_SMALL);
            key.setForeground(UIConstants.COLOR_TEXT_MUTED);
            cell.add(key, BorderLayout.NORTH);

            if (info[1] != null) {
                // Giá trị tĩnh
                JLabel val = new JLabel(info[1]);
                val.setFont(UIConstants.FONT_NORMAL_BOLD);
                val.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
                cell.add(val, BorderLayout.CENTER);
            } else {
                // Giá trị động từ DB
                JLabel dynVal = (dynIdx == 0) ? totalUserVal : totalHHVal;
                dynVal.setFont(UIConstants.FONT_NORMAL_BOLD);
                dynVal.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
                cell.add(dynVal, BorderLayout.CENTER);
                dynIdx++;
            }
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

        logTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, UIConstants.SP_SM, 0));

        // Bọc bảng trong JScrollPane để không bị che khuất và có thể cuộn
        JScrollPane logScroll = new JScrollPane(logTable);
        logScroll.setBorder(BorderFactory.createEmptyBorder());
        logScroll.setOpaque(false);
        logScroll.getViewport().setOpaque(false);
        logScroll.getVerticalScrollBar().setUnitIncrement(16);

        logCard.add(logTitle,  BorderLayout.NORTH);
        logCard.add(logScroll, BorderLayout.CENTER);

        JPanel topBlock = new JPanel(new BorderLayout(0, UIConstants.SP_MD));
        topBlock.setOpaque(false);
        topBlock.add(actionRow, BorderLayout.NORTH);
        topBlock.add(infoCard,  BorderLayout.CENTER);

        root.add(topBlock, BorderLayout.NORTH);
        root.add(logCard,  BorderLayout.CENTER);

        contentArea.add(root, BorderLayout.CENTER);

        // ---- Load số liệu thực từ DB ngầm (SwingWorker) ----
        new SwingWorker<int[], Void>() {
            @Override
            protected int[] doInBackground() {
                int totalHH   = 0;
                int totalUser = 0;
                try {
                    service.HouseholdService hhSvc = new service.impl.HouseholdServiceImpl();
                    totalHH = hhSvc.getAll().size();
                } catch (Exception ex) {
                    System.err.println("[WARN] Không lấy được tổng số hộ: " + ex.getMessage());
                }
                try {
                    dao.UserDAO userDao = new dao.impl.UserDAOImpl();
                    totalUser = userDao.getAll().size();
                } catch (Exception ex) {
                    System.err.println("[WARN] Không lấy được tổng người dùng: " + ex.getMessage());
                }
                return new int[]{totalUser, totalHH};
            }

            @Override
            protected void done() {
                try {
                    int[] result = get();
                    totalUserVal.setText(result[0] + " tài khoản");
                    totalHHVal.setText(String.format("%,d hộ", result[1]).replace(",", "."));
                } catch (Exception ex) {
                    totalUserVal.setText("N/A");
                    totalHHVal.setText("N/A");
                }
            }
        }.execute();
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
