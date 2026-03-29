package util;

import java.awt.*;

/**
 * Shared colors, fonts, and dimensions used across all views.
 * Centralizing these ensures a consistent look-and-feel.
 */
public class UIConstants {

    // ---- Colors ----
    public static final Color BG_DARK       = new Color(15,  20,  40);
    public static final Color BG_PANEL      = new Color(22,  30,  58);
    public static final Color ACCENT_BLUE   = new Color(64, 132, 255);
    public static final Color ACCENT_PURPLE = new Color(130, 80, 220);
    public static final Color ACCENT_CYAN   = new Color(0,  210, 220);
    public static final Color CARD_BACK     = new Color(30,  50, 100);
    public static final Color CARD_FACE     = new Color(240, 245, 255);
    public static final Color CARD_MATCHED  = new Color(40, 200, 120);
    public static final Color TEXT_PRIMARY  = new Color(220, 230, 255);
    public static final Color TEXT_MUTED    = new Color(130, 145, 180);
    public static final Color BUTTON_BG     = new Color(64, 132, 255);
    public static final Color BUTTON_HOVER  = new Color(90, 160, 255);
    public static final Color ERROR_RED     = new Color(255,  80,  80);
    public static final Color SUCCESS_GREEN = new Color(60, 200, 120);

    // ---- Fonts ----
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 32);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN, 15);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_CARD    = new Font("Segoe UI Emoji", Font.BOLD, 26);
    public static final Font FONT_BUTTON  = new Font("Segoe UI", Font.BOLD, 14);

    // ---- Dimensions ----
    public static final int CARD_MIN_SIZE = 60;
    public static final int CARD_MAX_SIZE = 100;
    public static final int WINDOW_WIDTH  = 960;
    public static final int WINDOW_HEIGHT = 680;

    private UIConstants() {} // Utility class — no instantiation
}
