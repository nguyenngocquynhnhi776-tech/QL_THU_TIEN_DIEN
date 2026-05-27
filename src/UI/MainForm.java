package UI;

import UI.components.*;
import UI.panels.*;
import UI.theme.ThemeManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import session.UserSession;
import model.User;

/**
 * Electra Manager AI — Cửa sổ chính.
 * Layout: Sidebar (260px) | Topbar (60px) / Content (flex) / Statusbar (26px)
 */
public class MainForm extends JFrame {

    private static MainForm instance;
    private final CardLayout      cardLayout;
    private final JPanel          mainContent;
    private final ArrayList<SidebarButton> menuButtons = new ArrayList<>();
    private final java.util.Map<String, SidebarButton> sidebarButtons = new java.util.HashMap<>();
    private final String          username;
    private final JLabel          statusTimeLbl;
    private final JLabel          breadcrumbLbl;

    public static MainForm getInstance() {
        return instance;
    }

    // Panel keys
    private static final String[] CARDS = {
        "DASHBOARD","USERS","HOUSEHOLD","INDEX","BILLING","INVOICE","PAYMENT","STATS","AI","NOTIF","SYSTEM"
    };
    private static final String[] LABELS = {
        "Bảng điều khiển","Người dùng","Hộ gia đình","Chỉ số điện",
        "Tính tiền điện","Hóa đơn","Thanh toán","Thống kê","AI Phân tích","Thông báo","Hệ thống"
    };
    private static final String[] ICONS = {
        "\u25A3","\u263A","\u2302","\u2261","\u00A4","\u2630","\u2714","\u21D7","\u25C8","\u25CE","\u2699"
    };

    public MainForm() {
        this(
            UserSession.getInstance().getRole() == model.Role.ADMIN,
            UserSession.getInstance().getUsername() != null ? UserSession.getInstance().getUsername() : "admin"
        );
    }

    public MainForm(boolean isAdmin, String username) {
        instance = this;
        this.username = username;

        // Populating session for backward compatibility/testing
        if (!UserSession.getInstance().isLoggedIn()) {
            User testUser = new User();
            testUser.setUserId(999);
            testUser.setUsername(username);
            testUser.setFullName(username.equals("admin") ? "Nguyễn Quản Trị" : "Nhân viên");
            testUser.setRole(isAdmin ? "ADMIN" : "CASHIER");
            testUser.setStatus("ACTIVE");
            UserSession.getInstance().login(testUser);
        }

        ThemeManager.applyGlobalDefaults();
        setTitle("Electra Manager AI — Hệ thống Quản lý Thu tiền Điện");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1280, 800));
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // ======================================================
        // Root: BorderLayout
        // ======================================================
        JPanel rootPane = new JPanel(new BorderLayout(0, 0));
        rootPane.setBackground(UIConstants.BG_GRADIENT_START);

        // ---- Sidebar ----
        JPanel sidebar = buildSidebar();

        // ---- Right side (topbar + content + statusbar) ----
        JPanel rightSide = new JPanel(new BorderLayout(0, 0));
        rightSide.setOpaque(false);

        breadcrumbLbl = new JLabel("Bảng điều khiển");
        JPanel topbar = buildTopbar();

        // Content area (CardLayout)
        cardLayout  = new CardLayout();
        mainContent = new JPanel(cardLayout) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                ThemeManager.paintGradientBackground(g2, getWidth(), getHeight());
                g2.dispose();
            }
        };
        mainContent.setOpaque(false);

        // Panels will be loaded lazily on demand

        // Status bar
        statusTimeLbl = new JLabel();
        JPanel statusBar = buildStatusBar();

        rightSide.add(topbar,     BorderLayout.NORTH);
        rightSide.add(mainContent, BorderLayout.CENTER);
        rightSide.add(statusBar,  BorderLayout.SOUTH);

        rootPane.add(sidebar,   BorderLayout.WEST);
        rootPane.add(rightSide, BorderLayout.CENTER);

        setContentPane(rootPane);

        // Start clock
        Timer clock = new Timer(1000, e -> updateClock());
        clock.start();
        updateClock();
        updateNotificationBadge();

        // Select dashboard by default
        if (!menuButtons.isEmpty()) {
            menuButtons.get(0).doClick();
        }
    }

    /** Legacy constructor */
    public MainForm(boolean isAdmin) {
        this(isAdmin, isAdmin ? "admin" : "nhanvien");
    }

    // ======================================================
    // SIDEBAR
    // ======================================================
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.paintSidebarBackground(g2, getWidth(), getHeight());
                g2.dispose();
            }
        };
        sidebar.setLayout(new BorderLayout(0, 0));
        sidebar.setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 0));
        sidebar.setOpaque(false);

        // Logo area
        JPanel logoArea = new JPanel(new BorderLayout());
        logoArea.setOpaque(false);
        logoArea.setPreferredSize(new Dimension(0, UIConstants.TOPBAR_HEIGHT));
        logoArea.setBorder(BorderFactory.createEmptyBorder(0, UIConstants.SP_MD, 0, UIConstants.SP_MD));

        JLabel logoIcon = new JLabel("\u26A1 ");
        logoIcon.setFont(new Font("Segoe UI Emoji", Font.BOLD, 22));
        logoIcon.setForeground(new Color(0xFFEB3B));

        JLabel logoText = new JLabel("Electra Manager");
        logoText.setFont(UIConstants.FONT_SUBHEADER);
        logoText.setForeground(Color.WHITE);

        JPanel logoInner = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        logoInner.setOpaque(false);
        logoInner.add(logoIcon); logoInner.add(logoText);
        logoArea.add(logoInner, BorderLayout.CENTER);

        // Separator
        JSeparator logoSep = new JSeparator();
        logoSep.setForeground(new Color(255, 255, 255, 30));

        // Menu items
        JPanel menuPanel = new JPanel();
        menuPanel.setOpaque(false);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(UIConstants.SP_SM, UIConstants.SP_SM, UIConstants.SP_SM, UIConstants.SP_SM));

        for (int i = 0; i < CARDS.length; i++) {
            final String card  = CARDS[i];
            final String label = LABELS[i];

            // Use PermissionManager to filter sidebar menus
            if (!util.PermissionManager.getInstance().isMenuVisible(card)) continue;

            SidebarButton btn = new SidebarButton(ICONS[i], label);
            sidebarButtons.put(card, btn);
            final int idx = menuButtons.size();

            btn.addActionListener(e -> {
                // Deselect all
                menuButtons.forEach(b -> b.setSelectedStatus(false));
                btn.setSelectedStatus(true);
                showPanel(card, label);
            });

            menuButtons.add(btn);
            menuPanel.add(btn);
            menuPanel.add(Box.createVerticalStrut(2));
        }

        // Section divider label
        JLabel sectionLbl = new JLabel("  CHỨC NĂNG CHÍNH");
        sectionLbl.setFont(UIConstants.FONT_SMALL);
        sectionLbl.setForeground(new Color(255, 255, 255, 60));
        sectionLbl.setBorder(BorderFactory.createEmptyBorder(UIConstants.SP_SM, UIConstants.SP_MD, UIConstants.SP_SM, 0));

        JPanel menuWrapper = new JPanel(new BorderLayout());
        menuWrapper.setOpaque(false);
        menuWrapper.add(sectionLbl, BorderLayout.NORTH);
        menuWrapper.add(menuPanel,  BorderLayout.CENTER);

        // User profile card at bottom
        JPanel profileCard = new JPanel(new BorderLayout(UIConstants.SP_SM, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillRoundRect(8, 8, getWidth() - 16, getHeight() - 16, 12, 12);
                g2.dispose();
            }
        };
        profileCard.setOpaque(false);
        profileCard.setPreferredSize(new Dimension(0, 72));
        profileCard.setBorder(BorderFactory.createEmptyBorder(UIConstants.SP_SM, UIConstants.SP_MD, UIConstants.SP_SM, UIConstants.SP_MD));

        // Avatar circle
        JPanel avatar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xFFEB3B));
                g2.fillOval(0, 0, getWidth()-1, getHeight()-1);
                g2.setColor(UIConstants.COLOR_SIDEBAR_BG);
                g2.setFont(UIConstants.FONT_NORMAL_BOLD);
                String initial = username.isEmpty() ? "U" : String.valueOf(Character.toUpperCase(username.charAt(0)));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initial,
                    (getWidth() - fm.stringWidth(initial)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(38, 38));

        JPanel userInfo = new JPanel();
        userInfo.setOpaque(false);
        userInfo.setLayout(new BoxLayout(userInfo, BoxLayout.Y_AXIS));
        
        String displayUser = UserSession.getInstance().getFullName();
        if (displayUser == null || displayUser.trim().isEmpty()) {
            displayUser = username;
        }
        JLabel uname = new JLabel(displayUser);
        uname.setFont(UIConstants.FONT_NORMAL_BOLD);
        uname.setForeground(Color.WHITE);
        
        model.Role role = UserSession.getInstance().getRole();
        String displayRole = (role != null) ? role.getDisplayName() : "Nhân viên";
        JLabel urole = new JLabel(displayRole);
        urole.setFont(UIConstants.FONT_SMALL);
        urole.setForeground(new Color(255, 255, 255, 160));
        userInfo.add(uname); userInfo.add(urole);

        JButton logoutBtn = new JButton("\u2717") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 20));
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        logoutBtn.setFont(UIConstants.FONT_NORMAL_BOLD);
        logoutBtn.setForeground(new Color(255,100,100));
        logoutBtn.setContentAreaFilled(false); logoutBtn.setBorderPainted(false);
        logoutBtn.setFocusPainted(false); logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setToolTipText("Đăng xuất");
        logoutBtn.addActionListener(e -> {
            boolean ok = ThemeManager.showConfirmDialog(this, "Bạn có chắc muốn đăng xuất?", "Đăng xuất");
            if (ok) { 
                UserSession.getInstance().logout();
                new LoginFrame().setVisible(true); 
                dispose(); 
            }
        });

        profileCard.add(avatar,    BorderLayout.WEST);
        profileCard.add(userInfo,  BorderLayout.CENTER);
        profileCard.add(logoutBtn, BorderLayout.EAST);

        sidebar.add(logoArea,    BorderLayout.NORTH);
        sidebar.add(menuWrapper, BorderLayout.CENTER);
        sidebar.add(profileCard, BorderLayout.SOUTH);

        return sidebar;
    }

    // ======================================================
    // TOPBAR
    // ======================================================
    private JPanel buildTopbar() {
        JPanel topbar = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UIConstants.TOPBAR_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(UIConstants.COLOR_BORDER);
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.dispose();
            }
        };
        topbar.setOpaque(false);
        topbar.setPreferredSize(new Dimension(0, UIConstants.TOPBAR_HEIGHT));
        topbar.setBorder(BorderFactory.createEmptyBorder(0, UIConstants.SP_LG, 0, UIConstants.SP_LG));

        // Breadcrumb
        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT, UIConstants.SP_SM, 0));
        leftTop.setOpaque(false);
        JLabel homeIcon = new JLabel("\u2302");
        homeIcon.setFont(UIConstants.FONT_ICON_SIDEBAR);
        homeIcon.setForeground(UIConstants.COLOR_TEXT_MUTED);
        JLabel sep = new JLabel(" / ");
        sep.setFont(UIConstants.FONT_NORMAL);
        sep.setForeground(UIConstants.COLOR_TEXT_MUTED);
        breadcrumbLbl.setFont(UIConstants.FONT_NORMAL_BOLD);
        breadcrumbLbl.setForeground(UIConstants.PRIMARY);
        leftTop.add(homeIcon); leftTop.add(sep); leftTop.add(breadcrumbLbl);

        // Right side: search + notif + user
        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIConstants.SP_SM, 12));
        rightTop.setOpaque(false);

        SearchField topSearch = new SearchField("Tìm kiếm toàn hệ thống...");
        topSearch.setPreferredSize(new Dimension(260, 36));

        // Notification bell
        JButton bellBtn = iconButton("\uD83D\uDD14", UIConstants.COLOR_TEXT_SECONDARY);
        bellBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        bellBtn.addActionListener(e -> {
            menuButtons.forEach(b -> b.setSelectedStatus(false));
            int notifIdx = java.util.Arrays.asList(CARDS).indexOf("NOTIF");
            if (notifIdx >= 0) {
                menuButtons.get(notifIdx).setSelectedStatus(true);
                showPanel("NOTIF", LABELS[notifIdx]);
            }
        });
        bellBtn.setToolTipText("Thông báo");

        // User info
        JLabel userChip = new JLabel("  " + username + "  ");
        userChip.setFont(UIConstants.FONT_NORMAL_BOLD);
        userChip.setForeground(UIConstants.PRIMARY);
        userChip.setOpaque(true);
        userChip.setBackground(new Color(UIConstants.PRIMARY.getRed(),
            UIConstants.PRIMARY.getGreen(), UIConstants.PRIMARY.getBlue(), 18));
        userChip.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.COLOR_BORDER, 1, true),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        rightTop.add(topSearch); rightTop.add(bellBtn); rightTop.add(userChip);

        topbar.add(leftTop,  BorderLayout.WEST);
        topbar.add(rightTop, BorderLayout.EAST);
        return topbar;
    }

    // ======================================================
    // STATUS BAR
    // ======================================================
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UIConstants.STATUS_BAR_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, UIConstants.STATUSBAR_HEIGHT));
        bar.setBorder(BorderFactory.createEmptyBorder(0, UIConstants.SP_MD, 0, UIConstants.SP_MD));

        JLabel leftLbl = new JLabel("\u26A1  Electra Manager AI  |  Kết nối SQL Server: OK");
        leftLbl.setFont(UIConstants.FONT_SMALL);
        leftLbl.setForeground(new Color(255, 255, 255, 180));

        statusTimeLbl.setFont(UIConstants.FONT_SMALL);
        statusTimeLbl.setForeground(new Color(255, 255, 255, 180));

        bar.add(leftLbl,       BorderLayout.WEST);
        bar.add(statusTimeLbl, BorderLayout.EAST);
        return bar;
    }

    private void updateClock() {
        String time = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        statusTimeLbl.setText(time + "  ");
    }

    private JButton iconButton(String icon, Color fg) {
        JButton b = new JButton(icon);
        b.setFont(UIConstants.FONT_ICON_SIDEBAR);
        b.setForeground(fg);
        b.setContentAreaFilled(false); b.setBorderPainted(false);
        b.setFocusPainted(false); b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(36, 36));
        return b;
    }

    private final java.util.Map<String, JPanel> loadedPanels = new java.util.HashMap<>();

    private void showPanel(String card, String label) {
        // Dashboard is never cached — always show fresh stats
        if ("DASHBOARD".equals(card)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            SwingUtilities.invokeLater(() -> {
                JPanel p = createPanel(card);
                if (loadedPanels.containsKey(card)) {
                    mainContent.remove(loadedPanels.get(card));
                }
                loadedPanels.put(card, p);
                mainContent.add(p, card);
                cardLayout.show(mainContent, card);
                breadcrumbLbl.setText(label);
                setCursor(Cursor.getDefaultCursor());
            });
            return;
        }

        if (!loadedPanels.containsKey(card)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            SwingUtilities.invokeLater(() -> {
                JPanel p = createPanel(card);
                loadedPanels.put(card, p);
                mainContent.add(p, card);
                cardLayout.show(mainContent, card);
                breadcrumbLbl.setText(label);
                setCursor(Cursor.getDefaultCursor());
            });
        } else {
            // Refresh data in panel if it's already cached
            JPanel existing = loadedPanels.get(card);
            if (existing instanceof InvoicePanel) {
                ((InvoicePanel) existing).reloadBills();
                System.out.println("[INFO] Bills reloaded on INVOICE panel navigation.");
            } else if (existing instanceof BillingPanel) {
                ((BillingPanel) existing).reloadTable();
                System.out.println("[INFO] Billing panel reloaded on BILLING panel navigation.");
            } else if (existing instanceof ElectricityIndexPanel) {
                ((ElectricityIndexPanel) existing).refreshData();
                System.out.println("[INFO] Index panel refreshed on INDEX panel navigation.");
            } else if (existing instanceof PaymentPanel) {
                ((PaymentPanel) existing).refreshData();
                System.out.println("[INFO] Payment panel refreshed on PAYMENT panel navigation.");
            }
            cardLayout.show(mainContent, card);
            breadcrumbLbl.setText(label);
            updateNotificationBadge();
        }
    }

    public void updateNotificationBadge() {
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                try {
                    return new service.impl.NotificationServiceImpl().getUnreadCount();
                } catch (Exception ex) {
                    System.err.println("[WARN] Failed to get unread notifications count: " + ex.getMessage());
                    return 0;
                }
            }
            @Override
            protected void done() {
                try {
                    int count = get();
                    SidebarButton btn = sidebarButtons.get("NOTIF");
                    if (btn != null) {
                        if (count > 0) {
                            btn.setText("Thông báo (" + count + ")");
                        } else {
                            btn.setText("Thông báo");
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private JPanel createPanel(String card) {
        switch (card) {
            case "DASHBOARD": return new DashboardPanel();
            case "HOUSEHOLD": return new HouseholdPanel();
            case "INDEX":     return new ElectricityIndexPanel();
            case "BILLING":   return new BillingPanel();
            case "INVOICE":   return new InvoicePanel();
            case "PAYMENT":   return new PaymentPanel();
            case "STATS":     return new StatisticsPanel();
            case "AI":        return new AIPanel();
            case "NOTIF":     return new NotificationPanel();
            case "SYSTEM":    return new SystemPanel();
            case "USERS":     return new UserManagementPanel();
            default:          return new JPanel();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainForm(true, "admin").setVisible(true);
        });
    }
}
