package UI.components;

import java.awt.*;

/**
 * Electra Manager AI — Centralized Design Token Registry
 * All colors, fonts, spacing, and dimension constants live here.
 */
public class UIConstants {

    // =====================================================================
    // PRIMARY BRAND COLORS
    // =====================================================================
    public static final Color PRIMARY        = new Color(0x5D6B6B);
    public static final Color PRIMARY_DARK   = new Color(0x4A5858);
    public static final Color PRIMARY_LIGHT  = new Color(0x7A8F8F);
    public static final Color PRIMARY_HOVER  = new Color(0x4E5D5D);

    // =====================================================================
    // BACKGROUND PALETTE
    // =====================================================================
    public static final Color BG_BLUSH_PINK    = new Color(0xF7CBCA);
    public static final Color BG_MUTED_ROSE    = new Color(0xEDD3D3);
    public static final Color BG_LAVENDER_MIST = new Color(0xEFE2F7);
    public static final Color BG_ICE_BLUE      = new Color(0xDDEFF6);
    public static final Color BG_TEAL_LIGHT    = new Color(0xD3E5E6);

    // Main gradient (used for content area background)
    public static final Color BG_GRADIENT_START = new Color(0xDDEFF6);
    public static final Color BG_GRADIENT_END   = new Color(0xEFE2F7);

    // =====================================================================
    // SEMANTIC COLORS
    // =====================================================================
    public static final Color SUCCESS     = new Color(0x3D9970);
    public static final Color SUCCESS_BG  = new Color(0xE6F4EE);
    public static final Color WARNING     = new Color(0xE67E22);
    public static final Color WARNING_BG  = new Color(0xFEF3C7);
    public static final Color ERROR       = new Color(0xC0392B);
    public static final Color ERROR_BG    = new Color(0xFEE2E2);
    public static final Color INFO        = new Color(0x2980B9);
    public static final Color INFO_BG     = new Color(0xEFF6FF);
    public static final Color AI_PURPLE   = new Color(0x8E44AD);
    public static final Color AI_PURPLE_BG = new Color(0xF3E8FF);

    // =====================================================================
    // NEUTRAL / TEXT COLORS
    // =====================================================================
    public static final Color WHITE              = Color.WHITE;
    public static final Color COLOR_WHITE        = Color.WHITE;
    public static final Color COLOR_TEXT_PRIMARY = new Color(0x1E2D2D);
    public static final Color COLOR_TEXT_SECONDARY = new Color(0x5D6B6B);
    public static final Color COLOR_TEXT_MUTED   = new Color(0x9EADAD);
    public static final Color COLOR_BORDER       = new Color(0xDDE4E4);
    public static final Color COLOR_BORDER_LIGHT = new Color(0xEFF2F2);
    public static final Color COLOR_DIVIDER      = new Color(0xEBEEEE);

    // =====================================================================
    // COMPONENT-SPECIFIC COLORS
    // =====================================================================
    // Card
    public static final Color COLOR_CARD_BG      = new Color(255, 255, 255, 235);
    public static final Color COLOR_CARD_SHADOW   = new Color(93, 107, 107, 35);

    // Sidebar
    public static final Color COLOR_SIDEBAR_BG        = new Color(0x5D6B6B);
    public static final Color COLOR_SIDEBAR_BG_DARK   = new Color(0x3E4F4F);
    public static final Color COLOR_SIDEBAR_TEXT      = new Color(0xD4E0E0);
    public static final Color COLOR_SIDEBAR_ACTIVE_BG = new Color(255, 255, 255, 28);
    public static final Color COLOR_SIDEBAR_HOVER_BG  = new Color(255, 255, 255, 15);

    // Table
    public static final Color TABLE_HEADER_BG   = new Color(0x5D6B6B);
    public static final Color TABLE_HEADER_FG   = Color.WHITE;
    public static final Color TABLE_ROW_EVEN    = Color.WHITE;
    public static final Color TABLE_ROW_ODD     = new Color(0xF4F9FB);
    public static final Color TABLE_ROW_HOVER   = new Color(0xE8F3F8);
    public static final Color TABLE_SELECTION   = new Color(0xD0E8F2);
    public static final Color TABLE_GRID        = new Color(0xECF0F0);

    // Input fields
    public static final Color INPUT_BG          = new Color(0xF8FBFB);
    public static final Color INPUT_BORDER      = new Color(0xCDD8D8);
    public static final Color INPUT_FOCUS       = new Color(0x5D6B6B);
    public static final Color INPUT_FOCUS_GLOW  = new Color(93, 107, 107, 60);

    // Topbar
    public static final Color TOPBAR_BG         = new Color(255, 255, 255, 245);
    public static final Color STATUS_BAR_BG     = new Color(0x5D6B6B);

    // =====================================================================
    // TYPOGRAPHY
    // =====================================================================
    public static final Font FONT_DISPLAY      = new Font("Segoe UI", Font.BOLD, 30);
    public static final Font FONT_TITLE        = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_HEADER       = new Font("Segoe UI", Font.BOLD, 17);
    public static final Font FONT_SUBHEADER    = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_NORMAL       = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_NORMAL_BOLD  = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL        = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_SMALL_BOLD   = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_MONO         = new Font("Consolas", Font.PLAIN, 12);
    public static final Font FONT_ICON_SIDEBAR = new Font("Dialog", Font.PLAIN, 16);
    public static final Font FONT_ICON_EMOJI   = new Font("Segoe UI Emoji", Font.PLAIN, 18);
    public static final Font FONT_ICON_SYMBOL  = new Font("Dialog", Font.PLAIN, 14);
    public static final Font FONT_STAT_VALUE   = new Font("Segoe UI", Font.BOLD, 24);

    // =====================================================================
    // DIMENSIONS & SPACING
    // =====================================================================
    public static final int SIDEBAR_WIDTH           = 260;
    public static final int SIDEBAR_COLLAPSED_WIDTH = 72;
    public static final int TOPBAR_HEIGHT           = 60;
    public static final int STATUSBAR_HEIGHT        = 26;
    public static final int CARD_RADIUS             = 18;
    public static final int BUTTON_RADIUS           = 10;
    public static final int INPUT_RADIUS            = 9;
    public static final int BADGE_RADIUS            = 20;

    public static final int SP_XS  = 4;
    public static final int SP_SM  = 8;
    public static final int SP_MD  = 16;
    public static final int SP_LG  = 24;
    public static final int SP_XL  = 32;
    public static final int SP_2XL = 48;

    // =====================================================================
    // LEGACY ALIASES (backward compatibility)
    // =====================================================================
    public static final Color PRIMARY_COLOR        = PRIMARY;
    public static final Color SECONDARY_COLOR      = SUCCESS;
    public static final Color COLOR_WARNING_PINK   = BG_BLUSH_PINK;
    public static final Color COLOR_ERROR_BG       = BG_MUTED_ROSE;
    public static final Color COLOR_AI_PURPLE      = BG_LAVENDER_MIST;
    public static final Color COLOR_INFO_TEAL      = BG_TEAL_LIGHT;
    public static final Color COLOR_APP_BG         = BG_ICE_BLUE;

    private UIConstants() {}
}
