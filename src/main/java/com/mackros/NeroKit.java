package com.mackros;

import java.util.Locale;

/** Общие детали пультов: стена с кнопками, таблички «что и как», информационные доски из text_display. */
final class NeroKit {
    static String ph(char axis, int n) { return "{" + axis + (n >= 0 ? "+" : "-") + Math.abs(n) + "}"; }

    static String esc(String s) { return s.replace("\\", "/").replace("\"", "”").replace("'", "’"); }

    private static String f(double v) { return String.format(Locale.ROOT, "%.2f", v); }

    /** Настенная табличка под кнопкой: 4 строки до 15 символов. */
    static String sign(int x, int y, int z, String a, String b, String c, String d) {
        return "setblock " + ph('x', x) + " " + ph('y', y) + " " + ph('z', z) + " minecraft:oak_wall_sign[facing=north]{front_text:{messages:['{\"text\":\"" + esc(a) + "\"}','{\"text\":\"" + esc(b)
                + "\"}','{\"text\":\"" + esc(c) + "\"}','{\"text\":\"" + esc(d) + "\"}']}}";
    }

    /** Доска над пультом: строки текста друг под другом (первая золотая). */
    static void board(NeroArchetypes.B b, int cx, int cy, int cz, String... lines) {
        for (int i = 0; i < lines.length; i++)
            b.setup("execute positioned " + ph('x', cx) + " " + ph('y', cy) + " " + ph('z', cz) + " run summon minecraft:text_display ~ ~" + f(-0.3 * i) + " ~ {text:'{\"text\":\"" + esc(lines[i]) + "\",\"color\":\""
                    + (i == 0 ? "gold" : "white") + "\"}',billboard:\"center\",Tags:[\"{tag}\"]}");
    }

    /** Стена пульта: n кнопок на x = 1, 3, 5...; индикатор-лампа справа. Игрок стоит перед стеной (z меньше 0). */
    static void console(NeroArchetypes.B b, int n) {
        int w = 2 * n + 2;
        b.part("Пульт управления");
        b.box(0, -1, -3, w, -1, 6, "smooth_stone");
        b.box(0, 0, 0, w, 3, 0, "black_concrete");
        b.box(0, 4, 0, w, 4, 0, "iron_block");
        for (int i = 0; i < n; i++) b.set(1 + 2 * i, 1, -1, "stone_button[face=wall,facing=north]");
        b.set(2 * n + 1, 2, 0, "red_concrete");
    }

    /** Команда-проверка нажатия i-й кнопки (кнопки опрашиваются командными блоками: так не нужен командный блок у каждой кнопки). */
    static String poll(int i) { return "execute if block " + ph('x', 1 + 2 * i) + " {y+1} {z-1} minecraft:stone_button[powered=true] run "; }

    private NeroKit() { }
}
