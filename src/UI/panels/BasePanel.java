package UI.panels;

import UI.components.UIConstants;
import UI.theme.ThemeManager;
import javax.swing.*;
import java.awt.*;

/**
 * Base panel for all content screens.
 * Provides: gradient background, page title + subtitle row, and a content area.
 */
public class BasePanel extends JPanel {

    protected final JPanel contentArea;

    public BasePanel(String title) {
        this(title, null);
    }

    public BasePanel(String title, String subtitle) {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        // Header row
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(
            UIConstants.SP_LG, UIConstants.SP_LG, UIConstants.SP_SM, UIConstants.SP_LG));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UIConstants.FONT_TITLE);
        titleLbl.setForeground(UIConstants.COLOR_TEXT_PRIMARY);
        header.add(titleLbl, BorderLayout.NORTH);

        if (subtitle != null) {
            JLabel subLbl = new JLabel(subtitle);
            subLbl.setFont(UIConstants.FONT_SMALL);
            subLbl.setForeground(UIConstants.COLOR_TEXT_MUTED);
            header.add(subLbl, BorderLayout.CENTER);
        }

        // Divider
        JSeparator sep = new JSeparator();
        sep.setForeground(UIConstants.COLOR_DIVIDER);

        // Content wrapper
        contentArea = new JPanel(new BorderLayout(0, 0));
        contentArea.setOpaque(false);
        contentArea.setBorder(BorderFactory.createEmptyBorder(
            UIConstants.SP_MD, UIConstants.SP_LG, UIConstants.SP_LG, UIConstants.SP_LG));

        add(header,      BorderLayout.NORTH);
        add(contentArea, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        ThemeManager.paintGradientBackground(g2, getWidth(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }

    /**
     * Subclasses add their widgets to contentArea.
     */
    protected JPanel getContentArea() { return contentArea; }
}
