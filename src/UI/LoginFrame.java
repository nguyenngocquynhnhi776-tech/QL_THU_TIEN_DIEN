package UI;

import UI.components.*;
import UI.theme.ThemeManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import service.AuthenticationService;
import service.impl.AuthenticationServiceImpl;
import session.UserSession;
import database.DatabaseInitializer;

/**
 * Electra Manager AI — Màn hình đăng nhập.
 * Thiết kế 2 cột: cột trái branding gradient, cột phải glassmorphism login card.
 */
public class LoginFrame extends JFrame {

    public LoginFrame() {
        setTitle("Electra Manager AI — Đăng nhập");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 780));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        ThemeManager.applyGlobalDefaults();

        // Root panel with gradient background
        JPanel root = new JPanel(new GridLayout(1, 2)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                ThemeManager.paintGradientBackground(g2, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setOpaque(false);

        // ============================================================
        // LEFT PANEL — Branding
        // ============================================================
        JPanel leftPanel = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Dark gradient
                GradientPaint gp = new GradientPaint(
                    0, 0, UIConstants.COLOR_SIDEBAR_BG,
                    getWidth(), getHeight(), UIConstants.COLOR_SIDEBAR_BG_DARK);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Decorative circles
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillOval(-80, -80, 400, 400);
                g2.setColor(new Color(255, 255, 255, 6));
                g2.fillOval(getWidth() - 200, getHeight() - 200, 500, 500);
                g2.fillOval(getWidth() / 2, 60, 250, 250);

                g2.dispose();
            }
        };
        leftPanel.setOpaque(false);

        JPanel brandContent = new JPanel();
        brandContent.setOpaque(false);
        brandContent.setLayout(new BoxLayout(brandContent, BoxLayout.Y_AXIS));

        // Lightning icon
        JLabel boltIcon = new JLabel("\u26A1") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Circle behind icon
                int size = Math.min(getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 20));
                g2.fillOval(0, 0, size - 1, size - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        boltIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        boltIcon.setForeground(new Color(0xFFEB3B));
        boltIcon.setPreferredSize(new Dimension(110, 110));
        boltIcon.setHorizontalAlignment(SwingConstants.CENTER);
        boltIcon.setAlignmentX(CENTER_ALIGNMENT);

        JLabel appName = new JLabel("Electra Manager AI");
        appName.setFont(UIConstants.FONT_DISPLAY);
        appName.setForeground(Color.WHITE);
        appName.setAlignmentX(CENTER_ALIGNMENT);

        JLabel tagline = new JLabel("Quản lý thu tiền điện thông minh");
        tagline.setFont(UIConstants.FONT_NORMAL);
        tagline.setForeground(new Color(255, 255, 255, 180));
        tagline.setAlignmentX(CENTER_ALIGNMENT);

        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(200, 1));
        sep.setForeground(new Color(255, 255, 255, 60));
        sep.setAlignmentX(CENTER_ALIGNMENT);

        // Features list
        String[] features = {"Phát hiện bất thường tự động (AI)", "Dự báo doanh thu thông minh", "Quản lý hóa đơn & thanh toán", "Báo cáo thống kê chi tiết"};
        JPanel featureList = new JPanel();
        featureList.setOpaque(false);
        featureList.setLayout(new BoxLayout(featureList, BoxLayout.Y_AXIS));
        featureList.setAlignmentX(CENTER_ALIGNMENT);
        for (String f : features) {
            JLabel fl = new JLabel("\u2714  " + f);
            fl.setFont(UIConstants.FONT_SMALL);
            fl.setForeground(new Color(255, 255, 255, 160));
            fl.setAlignmentX(CENTER_ALIGNMENT);
            featureList.add(fl);
            featureList.add(Box.createVerticalStrut(6));
        }

        brandContent.add(boltIcon);
        brandContent.add(Box.createVerticalStrut(UIConstants.SP_MD));
        brandContent.add(appName);
        brandContent.add(Box.createVerticalStrut(UIConstants.SP_SM));
        brandContent.add(tagline);
        brandContent.add(Box.createVerticalStrut(UIConstants.SP_LG));
        brandContent.add(sep);
        brandContent.add(Box.createVerticalStrut(UIConstants.SP_LG));
        brandContent.add(featureList);

        leftPanel.add(brandContent);

        // ============================================================
        // RIGHT PANEL — Login card
        // ============================================================
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setOpaque(false);

        // Login card (glassmorphism)
        JPanel loginCard = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.drawCardShadow(g2, 6, 8, getWidth() - 14, getHeight() - 16, 24);
                g2.setColor(new Color(255, 255, 255, 240));
                g2.fillRoundRect(6, 6, getWidth() - 14, getHeight() - 14, 24, 24);
                g2.dispose();
            }
        };
        loginCard.setOpaque(false);
        loginCard.setPreferredSize(new Dimension(420, 500));

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(8, 28, 8, 28);

        // Title
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 2; gc.insets = new Insets(32, 28, 4, 28);
        JLabel loginTitle = new JLabel("Chào mừng trở lại");
        loginTitle.setFont(UIConstants.FONT_TITLE);
        loginTitle.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
        loginCard.add(loginTitle, gc);

        gc.gridy = 1; gc.insets = new Insets(0, 28, 24, 28);
        JLabel loginSub = new JLabel("Vui lòng đăng nhập để tiếp tục");
        loginSub.setFont(UIConstants.FONT_SMALL);
        loginSub.setForeground(UIConstants.COLOR_TEXT_MUTED);
        loginCard.add(loginSub, gc);

        // Username
        gc.gridy = 2; gc.insets = new Insets(4, 28, 2, 28);
        JLabel userLbl = new JLabel("Tên đăng nhập");
        userLbl.setFont(UIConstants.FONT_NORMAL_BOLD);
        userLbl.setForeground(UIConstants.COLOR_TEXT_SECONDARY);
        loginCard.add(userLbl, gc);

        gc.gridy = 3; gc.insets = new Insets(0, 28, 12, 28);
        RoundedTextField userField = new RoundedTextField();
        userField.setPreferredSize(new Dimension(360, 42));
        loginCard.add(userField, gc);

        // Password
        gc.gridy = 4; gc.insets = new Insets(4, 28, 2, 28);
        JLabel passLbl = new JLabel("Mật khẩu");
        passLbl.setFont(UIConstants.FONT_NORMAL_BOLD);
        passLbl.setForeground(UIConstants.COLOR_TEXT_SECONDARY);
        loginCard.add(passLbl, gc);

        gc.gridy = 5; gc.insets = new Insets(0, 28, 12, 28);
        RoundedPasswordField passField = new RoundedPasswordField();
        passField.setPreferredSize(new Dimension(360, 42));
        loginCard.add(passField, gc);

        AuthenticationService authService = new AuthenticationServiceImpl();

        // Login button
        gc.gridy = 6; gc.insets = new Insets(12, 28, 10, 28);
        RoundedButton loginBtn = new RoundedButton("ĐĂNG NHẬP", UIConstants.BUTTON_RADIUS, UIConstants.PRIMARY);
        loginBtn.setPreferredSize(new Dimension(360, 46));
        loginBtn.setFont(UIConstants.FONT_NORMAL_BOLD);
        loginCard.add(loginBtn, gc);

        // Error label
        gc.gridy = 7; gc.insets = new Insets(0, 28, 20, 28);
        JLabel errLabel = new JLabel(" ");
        errLabel.setFont(UIConstants.FONT_SMALL);
        errLabel.setForeground(UIConstants.ERROR);
        errLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loginCard.add(errLabel, gc);

        // Login action
        ActionListener doLogin = e -> {
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword()).trim();
            if (user.isEmpty()) {
                errLabel.setText("Tên đăng nhập không được để trống!");
                return;
            }
            if (pass.isEmpty()) {
                errLabel.setText("Mật khẩu không được để trống!");
                return;
            }
            
            boolean success = authService.login(user, pass);
            if (success) {
                MainForm main = new MainForm();
                main.setVisible(true);
                dispose();
            } else {
                errLabel.setText("Sai tài khoản hoặc mật khẩu, hoặc tài khoản bị khóa!");
            }
        };

        loginBtn.addActionListener(doLogin);
        passField.addActionListener(doLogin);  // Enter key on password

        rightPanel.add(loginCard);

        root.add(leftPanel);
        root.add(rightPanel);

        setContentPane(root);
    }

    public static void main(String[] args) {
        // Initialize the database tables and admin seed
        DatabaseInitializer.initialize();

        SwingUtilities.invokeLater(() -> {
            ThemeManager.applyGlobalDefaults();
            new LoginFrame().setVisible(true);
        });
    }
}
