package com.mackros;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalculatorScreen extends Screen {
    private String cur = "0", op = "", hist = "";
    private double acc = 0;
    private boolean fresh = true;

    private static final String[][] KEYS = {
            {"C", "DEL", "%", "/"},
            {"7", "8", "9", "*"},
            {"4", "5", "6", "-"},
            {"1", "2", "3", "+"},
            {"+/-", "0", ".", "="}
    };

    public CalculatorScreen() { super(Text.literal("Калькулятор")); }

    @Override
    protected void init() {
        int x0 = width / 2 - 82, y0 = Math.max(70, height / 2 - 40);
        for (int r = 0; r < KEYS.length; r++)
            for (int c = 0; c < 4; c++) {
                final String k = KEYS[r][c];
                addDrawableChild(ButtonWidget.builder(Text.literal(k), b -> press(k))
                        .dimensions(x0 + c * 42, y0 + r * 24, 38, 20).build());
            }
    }

    private static String fmt(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) return "Ошибка";
        if (d == Math.rint(d) && Math.abs(d) < 1e15) return String.valueOf((long) d);
        return new BigDecimal(d).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private double val() {
        try { return Double.parseDouble(cur); } catch (Exception e) { return 0; }
    }

    private static double calc(double a, String o, double b) {
        return switch (o) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> b == 0 ? Double.NaN : a / b;
            default -> b;
        };
    }

    private void press(String k) {
        if (cur.equals("Ошибка") && !k.equals("C")) { cur = "0"; fresh = true; op = ""; }
        switch (k) {
            case "C" -> { cur = "0"; acc = 0; op = ""; hist = ""; fresh = true; }
            case "DEL" -> { if (!fresh && cur.length() > 1) cur = cur.substring(0, cur.length() - 1); else { cur = "0"; fresh = true; } }
            case "+/-" -> { if (!cur.equals("0")) cur = cur.startsWith("-") ? cur.substring(1) : "-" + cur; }
            case "%" -> { cur = fmt(val() / 100.0); fresh = true; }
            case "." -> {
                if (fresh) { cur = "0."; fresh = false; }
                else if (!cur.contains(".")) cur += ".";
            }
            case "+", "-", "*", "/" -> {
                if (!op.isEmpty() && !fresh) { acc = calc(acc, op, val()); cur = fmt(acc); } else acc = val();
                op = k; fresh = true; hist = fmt(acc) + " " + k;
            }
            case "=" -> {
                if (!op.isEmpty()) {
                    double b = val();
                    hist = fmt(acc) + " " + op + " " + fmt(b) + " =";
                    cur = fmt(calc(acc, op, b));
                    op = ""; fresh = true;
                }
            }
            default -> {
                if (fresh) { cur = k; fresh = false; }
                else if (cur.length() < 14) cur = cur.equals("0") ? k : cur + k;
            }
        }
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if ("0123456789.+-*/=%".indexOf(c) >= 0) { press(String.valueOf(c)); return true; }
        if (c == 'c' || c == 'C') { press("C"); return true; }
        return super.charTyped(c, mods);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 257 || key == 335) { press("="); return true; }
        if (key == 259) { press("DEL"); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        int x0 = width / 2 - 82, y0 = Math.max(70, height / 2 - 40);
        ctx.fill(x0 - 6, y0 - 56, x0 + 170, y0 + 126, 0xE6101820);
        ctx.fill(x0, y0 - 46, x0 + 164, y0 - 6, 0xFF0B2A18);
        ctx.drawTextWithShadow(textRenderer, hist, x0 + 4, y0 - 42, 0x66AA77);
        var m = ctx.getMatrices();
        m.push();
        m.scale(2f, 2f, 1f);
        int tw = textRenderer.getWidth(cur);
        ctx.drawTextWithShadow(textRenderer, cur, (x0 + 160 - tw * 2) / 2, (y0 - 30) / 2, 0x55FF55);
        m.pop();
        ctx.drawCenteredTextWithShadow(textRenderer, "Калькулятор", width / 2, y0 - 54, 0x55FFFF);
        super.render(ctx, mx, my, d);
    }

    @Override
    public boolean shouldPause() { return false; }
}
