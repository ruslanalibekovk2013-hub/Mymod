package com.mackros;

import net.minecraft.client.gui.DrawContext;

/** Общий стиль экранов NERO: тёмная панель со скруглёнными углами и один акцентный цвет. */
public final class NeroUi {
    public static final int BG = 0xF00E141C, PANEL = 0xE0182230, PANEL2 = 0xF01C2A3C, LINE = 0xFF2C3E55, ACCENT = 0xFF39D0E0;
    public static final int TEXT = 0xFFE6EDF5, DIM = 0xFF8A99AD, OK = 0xFF55E08A, WARN = 0xFFFFB84D, ERR = 0xFFFF6B6B, USER = 0xFF7CC4FF;

    public static void round(DrawContext c, int x1, int y1, int x2, int y2, int col) {
        c.fill(x1 + 1, y1, x2 - 1, y2, col);
        c.fill(x1, y1 + 1, x2, y2 - 1, col);
    }

    public static void box(DrawContext c, int x1, int y1, int x2, int y2, int fill, int line) {
        round(c, x1, y1, x2, y2, line);
        round(c, x1 + 1, y1 + 1, x2 - 1, y2 - 1, fill);
    }

    public static String spinner() {
        String[] s = {"|", "/", "-", "\\"};
        return s[(int) ((System.currentTimeMillis() / 120) % 4)];
    }

    private NeroUi() { }
}
