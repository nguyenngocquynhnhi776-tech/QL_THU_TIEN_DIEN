package UI.theme;

import UI.components.UIConstants;
import javax.swing.*;
import java.awt.*;

/**
 * Electra Manager AI — ThemeManager
 * Centralized utility for painting gradients, shadows, and applying global defaults.
 */
public final class ThemeManager {

    private ThemeManager() {}

    /** Paint the standard diagonal gradient background for content panels. */
    public static void paintGradientBackground(Graphics2D g2, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        GradientPaint gp = new GradientPaint(
            0, 0,            UIConstants.BG_GRADIENT_START,
            width, height,   UIConstants.BG_GRADIENT_END
        );
        g2.setPaint(gp);
        g2.fillRect(0, 0, width, height);
    }

    /** Paint the sidebar vertical gradient (dark top to darker bottom). */
    public static void paintSidebarBackground(Graphics2D g2, int width, int height) {
        GradientPaint gp = new GradientPaint(
            0, 0,       UIConstants.COLOR_SIDEBAR_BG,
            0, height,  UIConstants.COLOR_SIDEBAR_BG_DARK
        );
        g2.setPaint(gp);
        g2.fillRect(0, 0, width, height);
    }

    /**
     * Draw a soft multi-layer drop shadow under a rounded rectangle.
     * @param g2       Graphics2D context
     * @param x        left of the card
     * @param y        top of the card
     * @param w        width of the card
     * @param h        height of the card
     * @param radius   corner arc radius
     */
    public static void drawCardShadow(Graphics2D g2, int x, int y, int w, int h, int radius) {
        int layers = 6;
        for (int i = layers; i >= 1; i--) {
            int alpha = (int)(255 * (0.025f * i));
            g2.setColor(new Color(0, 0, 0, Math.min(alpha, 80)));
            g2.fillRoundRect(x + i, y + i + 1, w, h, radius + 2, radius + 2);
        }
    }

    /**
     * Apply sensible global UI defaults (fonts, focus colors, etc.)
     * Call once at application startup.
     */
    public static void applyGlobalDefaults() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        UIManager.put("OptionPane.messageFont",   UIConstants.FONT_NORMAL);
        UIManager.put("OptionPane.buttonFont",    UIConstants.FONT_NORMAL_BOLD);
        UIManager.put("Label.font",               UIConstants.FONT_NORMAL);
        UIManager.put("Button.font",              UIConstants.FONT_NORMAL_BOLD);
        UIManager.put("TextField.font",           UIConstants.FONT_NORMAL);
        UIManager.put("PasswordField.font",       UIConstants.FONT_NORMAL);
        UIManager.put("ComboBox.font",            UIConstants.FONT_NORMAL);
        UIManager.put("Table.font",               UIConstants.FONT_NORMAL);
        UIManager.put("TableHeader.font",         UIConstants.FONT_NORMAL_BOLD);
        UIManager.put("ScrollBar.width",          8);
        UIManager.put("ScrollBar.thumbDarkShadow", UIConstants.COLOR_BORDER);
        UIManager.put("ScrollBar.thumb",          UIConstants.COLOR_BORDER);
        UIManager.put("ScrollBar.track",          UIConstants.COLOR_BORDER_LIGHT);
        UIManager.put("Focus.color",              UIConstants.PRIMARY);
        UIManager.put("TitledBorder.font",        UIConstants.FONT_NORMAL_BOLD);
        UIManager.put("TitledBorder.titleColor",  UIConstants.COLOR_TEXT_SECONDARY);
    }

    /**
     * Show a standard styled confirm dialog.
     * @return true if user confirmed
     */
    public static boolean showConfirmDialog(Component parent, String message, String title) {
        int result = JOptionPane.showConfirmDialog(
            parent, message, title,
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }

    /** Show a styled info dialog. */
    public static void showInfoDialog(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /** Show a styled error dialog. */
    public static void showErrorDialog(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
