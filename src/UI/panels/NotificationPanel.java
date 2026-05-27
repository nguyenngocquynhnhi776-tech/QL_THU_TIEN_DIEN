package UI.panels;

import UI.components.*;
import UI.MainForm;
import UI.theme.ThemeManager;
import model.Notification;
import model.Role;
import service.NotificationService;
import service.impl.NotificationServiceImpl;
import session.UserSession;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Trang Thông Báo — Hoàn thiện logic & giao diện hệ thống Electra Manager.
 * Đồng bộ toàn diện với cơ sở dữ liệu thực, hỗ trợ:
 * - Lọc phân loại loại thông báo (Tất cả, Thành công, Cảnh báo, Lỗi, Hệ thống).
 * - Lọc chỉ xem thông báo chưa đọc.
 * - Chức năng "Quét hệ thống" (chỉ dành cho ADMIN) chạy ngầm SwingWorker.
 * - Đánh dấu tất cả đã đọc / Xóa các thông báo đã đọc ngầm.
 * - Danh sách cuộn mượt mà với hiệu ứng hover và giao diện tối giản, chuyên
 * nghiệp.
 */
public class NotificationPanel extends BasePanel {

    private final NotificationService notificationService = new NotificationServiceImpl();

    // Components
    private JComboBox<String> typeCombo;
    private JCheckBox unreadCheck;
    private RoundedButton markAllReadBtn;
    private RoundedButton clearReadBtn;
    private RoundedButton runAuditBtn;
    private JPanel notifListPanel;
    private JScrollPane scrollPane;
    private JLabel emptyLabel;

    public NotificationPanel() {
        super("Thông báo", "Nhắc nhở thanh toán và cảnh báo hệ thống từ cơ sở dữ liệu");
        buildUI();
        loadNotifications();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, UIConstants.SP_MD));
        root.setOpaque(false);

        // ── 1. Top Toolbar: filters and actions ──────────────────────────────
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, UIConstants.SP_SM, 0));

        // Filter group (Left side)
        JPanel filterGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, UIConstants.SP_SM, 0));
        filterGroup.setOpaque(false);

        JLabel filterLbl = new JLabel("Lọc theo:");
        filterLbl.setFont(UIConstants.FONT_SMALL_BOLD);
        filterLbl.setForeground(UIConstants.COLOR_TEXT_PRIMARY);

        typeCombo = new JComboBox<>(new String[] {
                "Tất cả", "Thành công", "Cảnh báo", "Lỗi", "Hệ thống"
        });
        typeCombo.setFont(UIConstants.FONT_SMALL);
        typeCombo.setPreferredSize(new Dimension(130, 36));
        typeCombo.addActionListener(e -> loadNotifications());

        unreadCheck = new JCheckBox("Chỉ hiển thị chưa đọc");
        unreadCheck.setFont(UIConstants.FONT_SMALL);
        unreadCheck.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
        unreadCheck.setOpaque(false);
        unreadCheck.addActionListener(e -> loadNotifications());

        filterGroup.add(filterLbl);
        filterGroup.add(typeCombo);
        filterGroup.add(unreadCheck);

        // Action group (Right side)
        JPanel actionGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIConstants.SP_SM, 0));
        actionGroup.setOpaque(false);

        runAuditBtn = new RoundedButton("🔍  Quét hệ thống", UIConstants.BUTTON_RADIUS, UIConstants.PRIMARY);
        runAuditBtn.setFont(UIConstants.FONT_SMALL_BOLD);
        runAuditBtn.setPreferredSize(new Dimension(150, 36));
        // Only visible to ADMIN role
        runAuditBtn.setVisible(UserSession.getInstance().getRole() == Role.ADMIN);
        runAuditBtn.addActionListener(e -> triggerSystemAudit());

        markAllReadBtn = new RoundedButton("✓  Đọc tất cả", UIConstants.BUTTON_RADIUS, new Color(0x3E4F4F));
        markAllReadBtn.setFont(UIConstants.FONT_SMALL_BOLD);
        markAllReadBtn.setPreferredSize(new Dimension(130, 36));
        markAllReadBtn.addActionListener(e -> markAllAsRead());

        clearReadBtn = new RoundedButton("🗑  Xóa thông báo đã đọc", UIConstants.BUTTON_RADIUS, new Color(0xC0392B));
        clearReadBtn.setFont(UIConstants.FONT_SMALL_BOLD);
        clearReadBtn.setPreferredSize(new Dimension(200, 36));
        clearReadBtn.addActionListener(e -> clearReadNotifications());

        actionGroup.add(runAuditBtn);
        actionGroup.add(markAllReadBtn);
        actionGroup.add(clearReadBtn);

        toolbar.add(filterGroup, BorderLayout.WEST);
        toolbar.add(actionGroup, BorderLayout.EAST);

        // ── 2. Card holding the scrollable notification list ────────────────
        GlassCard listCard = new GlassCard(UIConstants.CARD_RADIUS);
        listCard.setLayout(new BorderLayout());

        notifListPanel = new JPanel();
        notifListPanel.setOpaque(false);
        notifListPanel.setLayout(new BoxLayout(notifListPanel, BoxLayout.Y_AXIS));

        // Empty state label
        emptyLabel = new JLabel("🔔  Không có thông báo nào phù hợp.", SwingConstants.CENTER);
        emptyLabel.setFont(UIConstants.FONT_HEADER);
        emptyLabel.setForeground(UIConstants.COLOR_TEXT_MUTED);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(60, 20, 60, 20));

        scrollPane = new JScrollPane(notifListPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        listCard.add(scrollPane, BorderLayout.CENTER);

        root.add(toolbar, BorderLayout.NORTH);
        root.add(listCard, BorderLayout.CENTER);

        contentArea.add(root, BorderLayout.CENTER);
    }

    // =========================================================================
    // DATABASE / ASYNC OPERATIONS
    // =========================================================================

    public void loadNotifications() {
        String vnType = (String) typeCombo.getSelectedItem();
        final String type;
        if ("Thành công".equals(vnType))
            type = "success";
        else if ("Cảnh báo".equals(vnType))
            type = "warning";
        else if ("Lỗi".equals(vnType))
            type = "error";
        else if ("Hệ thống".equals(vnType))
            type = "info";
        else
            type = null;

        final boolean unreadOnly = unreadCheck.isSelected();

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<List<Notification>, Void>() {
            @Override
            protected List<Notification> doInBackground() {
                // Get maximum of 200 notifications
                return notificationService.getNotifications(type, unreadOnly, 200, 0);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    List<Notification> list = get();
                    populateNotificationList(list);
                } catch (Exception ex) {
                    System.err.println("[ERROR] Failed to load notifications: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void populateNotificationList(List<Notification> notifications) {
        notifListPanel.removeAll();

        if (notifications.isEmpty()) {
            notifListPanel.setLayout(new BorderLayout());
            notifListPanel.add(emptyLabel, BorderLayout.CENTER);
        } else {
            notifListPanel.setLayout(new BoxLayout(notifListPanel, BoxLayout.Y_AXIS));
            for (Notification notif : notifications) {
                notifListPanel.add(createNotificationCard(notif));
            }
        }

        notifListPanel.revalidate();
        notifListPanel.repaint();

        // Update the badge count in sidebar
        if (MainForm.getInstance() != null) {
            MainForm.getInstance().updateNotificationBadge();
        }
    }

    private JPanel createNotificationCard(Notification notif) {
        // Resolve type colors & icon character
        Color bgTheme;
        Color fgTheme;
        String iconChar;

        String rawType = notif.getType() != null ? notif.getType().toLowerCase() : "info";
        switch (rawType) {
            case "success":
                fgTheme = UIConstants.SUCCESS;
                bgTheme = UIConstants.SUCCESS_BG;
                iconChar = "\u2714"; // checkmark
                break;
            case "warning":
                fgTheme = UIConstants.WARNING;
                bgTheme = UIConstants.WARNING_BG;
                iconChar = "\u26A0"; // warning sign
                break;
            case "error":
                fgTheme = UIConstants.ERROR;
                bgTheme = UIConstants.ERROR_BG;
                iconChar = "\u2718"; // x mark
                break;
            case "info":
            default:
                fgTheme = UIConstants.INFO;
                bgTheme = UIConstants.INFO_BG;
                iconChar = "\u2139"; // info sign
                break;
        }

        // ── Card wrapper: dùng BorderLayout để kiểm soát kích thước chặt chẽ ──
        // Chiều cao cố định ~90px, padding gọn 10px top/bottom
        final int CARD_H = 60;
        final int ICON_SIZE = 40;
        final int CARD_PADDING_V = 10;
        final int CARD_PADDING_H = UIConstants.SP_MD;

        // Entire notification card panel
        JPanel card = new JPanel(new BorderLayout(UIConstants.SP_SM + 4, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                float alpha = notif.isRead() ? 0.50f : 1.0f;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

                // Base white background — nhẹ, phẳng
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Border mềm, nhạt hơn
                g2.setColor(new Color(0xE8EDED));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        // Chiều cao cố định ~90px để hiển thị được 5-6 thông báo trên màn hình
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, CARD_H));
        card.setPreferredSize(new Dimension(800, CARD_H));
        card.setMinimumSize(new Dimension(400, CARD_H));
        card.setBorder(BorderFactory.createEmptyBorder(CARD_PADDING_V, CARD_PADDING_H, CARD_PADDING_V, CARD_PADDING_H));

        // ── Icon circle: tròn hoàn chỉnh 40x40, căn giữa dọc ──
        JPanel iconWrapper = new JPanel(new GridBagLayout()); // GridBagLayout căn giữa icon theo chiều dọc
        iconWrapper.setOpaque(false);
        iconWrapper.setPreferredSize(new Dimension(ICON_SIZE + 4, CARD_H - CARD_PADDING_V * 2));

        JPanel iconCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Vẽ hình tròn hoàn chỉnh — dùng min(w,h) để đảm bảo tròn
                int size = Math.min(getWidth(), getHeight());
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                g2.setColor(bgTheme);
                g2.fillOval(x, y, size, size);

                // Icon symbol căn giữa trong hình tròn
                g2.setColor(fgTheme);
                g2.setFont(new Font("Segoe UI Symbol", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                int tx = x + (size - fm.stringWidth(iconChar)) / 2;
                int ty = y + (size + fm.getAscent() - fm.getDescent()) / 2 - 1;
                g2.drawString(iconChar, tx, ty);
                g2.dispose();
            }
        };
        iconCircle.setOpaque(false);
        // Kích thước tròn cố định — width = height = ICON_SIZE
        iconCircle.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
        iconCircle.setMinimumSize(new Dimension(ICON_SIZE, ICON_SIZE));
        iconCircle.setMaximumSize(new Dimension(ICON_SIZE, ICON_SIZE));

        iconWrapper.add(iconCircle); // GridBagLayout mặc định căn giữa

        // ── Center Panel: Title + Content, căn giữa dọc ──
        JPanel textPanel = new JPanel(new GridBagLayout());
        textPanel.setOpaque(false);

        JPanel textInner = new JPanel(new BorderLayout(0, 2));
        textInner.setOpaque(false);

        JLabel titleLbl = new JLabel(notif.getTitle());
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(notif.isRead() ? UIConstants.COLOR_TEXT_SECONDARY : UIConstants.COLOR_TEXT_PRIMARY);

        JLabel contentLbl = new JLabel(notif.getContent());
        contentLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        contentLbl.setForeground(notif.isRead() ? UIConstants.COLOR_TEXT_MUTED : UIConstants.COLOR_TEXT_SECONDARY);

        textInner.add(titleLbl, BorderLayout.NORTH);
        textInner.add(contentLbl, BorderLayout.CENTER);

        GridBagConstraints tgc = new GridBagConstraints();
        tgc.fill = GridBagConstraints.HORIZONTAL;
        tgc.weightx = 1.0;
        tgc.anchor = GridBagConstraints.CENTER;
        textPanel.add(textInner, tgc);

        // ── Right Panel: Timestamp + Xóa — stack dọc, căn giữa theo chiều dọc card ──
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(130, CARD_H - CARD_PADDING_V * 2));

        String timeStr = "";
        if (notif.getCreatedAt() != null) {
            timeStr = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(notif.getCreatedAt());
        }
        JLabel timeLbl = new JLabel(timeStr, SwingConstants.RIGHT);
        timeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        // Timestamp không quá nhạt — dùng COLOR_TEXT_SECONDARY thay vì MUTED
        timeLbl.setForeground(UIConstants.COLOR_TEXT_SECONDARY);

        // Delete button
        JButton deleteCardBtn = new JButton("Xóa");
        deleteCardBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        deleteCardBtn.setForeground(new Color(0xC0392B));
        deleteCardBtn.setBorderPainted(false);
        deleteCardBtn.setContentAreaFilled(false);
        deleteCardBtn.setFocusPainted(false);
        deleteCardBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteCardBtn.setMargin(new Insets(0, 0, 0, 0));
        deleteCardBtn.addActionListener(e -> deleteSingleNotification(notif.getNotificationId()));

        GridBagConstraints rgc = new GridBagConstraints();
        rgc.gridx = 0;
        rgc.gridy = 0;
        rgc.anchor = GridBagConstraints.CENTER;
        rgc.insets = new Insets(0, 0, 4, 0);
        rightPanel.add(timeLbl, rgc);

        rgc.gridy = 1;
        rgc.insets = new Insets(0, 0, 0, 0);
        rightPanel.add(deleteCardBtn, rgc);

        card.add(iconWrapper, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);
        card.add(rightPanel, BorderLayout.EAST);

        // Card interaction: click to mark read (if unread)
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!notif.isRead()) {
                    markSingleAsRead(notif.getNotificationId());
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                card.setBackground(new Color(245, 248, 250));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setCursor(Cursor.getDefaultCursor());
                card.setBackground(null);
            }
        });

        // Container bao ngoài với margin bottom nhỏ hơn (4px thay vì 8px)
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        container.add(card, BorderLayout.CENTER);
        container.setBorder(BorderFactory.createEmptyBorder(0, 0, UIConstants.SP_XS, 0));

        return container;
    }

    private void markSingleAsRead(int notifId) {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return notificationService.markAsRead(notifId);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        loadNotifications();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void deleteSingleNotification(int notifId) {
        boolean ok = ThemeManager.showConfirmDialog(this,
                "Bạn có chắc muốn xóa thông báo này?",
                "Xác nhận xóa");
        if (!ok)
            return;

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return notificationService.deleteNotification(notifId);
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    if (get()) {
                        ToastNotification.show(SwingUtilities.getWindowAncestor(NotificationPanel.this),
                                "Đã xóa thông báo.", ToastNotification.Type.SUCCESS);
                        loadNotifications();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void markAllAsRead() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return notificationService.markAllRead();
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    if (get()) {
                        ToastNotification.show(SwingUtilities.getWindowAncestor(NotificationPanel.this),
                                "Đã đánh dấu tất cả là đã đọc.", ToastNotification.Type.SUCCESS);
                        loadNotifications();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void clearReadNotifications() {
        boolean ok = ThemeManager.showConfirmDialog(this,
                "Bạn có chắc muốn xóa tất cả thông báo đã đọc?",
                "Xác nhận xóa");
        if (!ok)
            return;

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                // Delete all notifications that are marked as read (IsRead = 1)
                // Since NotificationService doesn't have a direct clearRead() method, we can do
                // it via a quick SQL query or custom impl.
                // Let's implement it inside NotificationService/DAO or do a custom SQL delete
                // here.
                // We'll write a clean helper in the service or execute it via database.
                // To keep clean service separation, let's write the query here safely.
                java.sql.Connection conn = null;
                try {
                    conn = database.DatabaseConnection.getConnection();
                    if (conn != null) {
                        try (java.sql.PreparedStatement ps = conn.prepareStatement(
                                "DELETE FROM NOTIFICATION WHERE IsRead = 1")) {
                            ps.executeUpdate();
                            return true;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    if (conn != null) {
                        try {
                            conn.close();
                        } catch (Exception ex) {
                        }
                    }
                }
                return false;
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    if (get()) {
                        ToastNotification.show(SwingUtilities.getWindowAncestor(NotificationPanel.this),
                                "Đã xóa toàn bộ thông báo đã đọc.", ToastNotification.Type.SUCCESS);
                        loadNotifications();
                    } else {
                        ToastNotification.show(SwingUtilities.getWindowAncestor(NotificationPanel.this),
                                "Không tìm thấy thông báo đã đọc nào để xóa.", ToastNotification.Type.INFO);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void triggerSystemAudit() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        runAuditBtn.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                notificationService.runSystemHealthAudit();
                return null;
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                runAuditBtn.setEnabled(true);
                ToastNotification.show(SwingUtilities.getWindowAncestor(NotificationPanel.this),
                        "Quét kiểm tra hệ thống hoàn tất!", ToastNotification.Type.SUCCESS);
                loadNotifications();
            }
        }.execute();
    }
}
