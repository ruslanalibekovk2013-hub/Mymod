package com.mackros;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Config {
    public static int reach = 128, previewLimit = 2500, undoLimit = 50, cmdRate = 12, colorMode = 1, fillMode = 0;
    public static long maxVolume = 3_000_000L;
    public static boolean showHud = true, showOutlines = true, relMove = true, autoSelect = false, hudCompact = false;
    public static int accent = 0;
    public static final int[] ACCENTS = {0x00C8E0, 0x55FF55, 0xFFAA00, 0xFF5555, 0xFF55FF, 0xAAAAFF};
    public static int accent() { return ACCENTS[Math.floorMod(accent, ACCENTS.length)]; }

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("mackros.properties"); }

    public static int cycle(int cur, int... vals) {
        for (int i = 0; i < vals.length; i++) if (vals[i] == cur) return vals[(i + 1) % vals.length];
        return vals[0];
    }

    private static int geti(Properties p, String k, int d) {
        try { return Integer.parseInt(p.getProperty(k, String.valueOf(d))); } catch (Exception e) { return d; }
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Properties p = new Properties();
            try (var in = Files.newInputStream(f)) { p.load(in); }
            reach = geti(p, "reach", reach);
            previewLimit = geti(p, "previewLimit", previewLimit);
            undoLimit = geti(p, "undoLimit", undoLimit);
            cmdRate = geti(p, "cmdRate", cmdRate);
            colorMode = geti(p, "colorMode", colorMode);
            fillMode = geti(p, "fillMode", fillMode);
            maxVolume = geti(p, "maxVolume", (int) maxVolume);
            showHud = geti(p, "showHud", 1) == 1;
            showOutlines = geti(p, "showOutlines", 1) == 1;
            relMove = geti(p, "relMove", 1) == 1;
            autoSelect = geti(p, "autoSelect", 0) == 1;
            hudCompact = geti(p, "hudCompact", 0) == 1;
            accent = geti(p, "accent", 0);
            Move.count = geti(p, "moveCount", 1);
            Move.air = geti(p, "moveAir", 0) == 1;
            Editor.pasteAir = geti(p, "pasteAir", 0) == 1;
            Editor.preview = geti(p, "preview", 1) == 1;
            Editor.radius = geti(p, "radius", Editor.radius);
            Editor.height = geti(p, "height", Editor.height);
            Editor.step = geti(p, "step", Editor.step);
        } catch (Exception ignored) { }
    }

    public static void save() {
        try {
            Properties p = new Properties();
            p.setProperty("reach", String.valueOf(reach));
            p.setProperty("previewLimit", String.valueOf(previewLimit));
            p.setProperty("undoLimit", String.valueOf(undoLimit));
            p.setProperty("cmdRate", String.valueOf(cmdRate));
            p.setProperty("colorMode", String.valueOf(colorMode));
            p.setProperty("fillMode", String.valueOf(fillMode));
            p.setProperty("maxVolume", String.valueOf((int) maxVolume));
            p.setProperty("showHud", showHud ? "1" : "0");
            p.setProperty("showOutlines", showOutlines ? "1" : "0");
            p.setProperty("relMove", relMove ? "1" : "0");
            p.setProperty("autoSelect", autoSelect ? "1" : "0");
            p.setProperty("hudCompact", hudCompact ? "1" : "0");
            p.setProperty("accent", String.valueOf(accent));
            p.setProperty("moveCount", String.valueOf(Move.count));
            p.setProperty("moveAir", Move.air ? "1" : "0");
            p.setProperty("pasteAir", Editor.pasteAir ? "1" : "0");
            p.setProperty("preview", Editor.preview ? "1" : "0");
            p.setProperty("radius", String.valueOf(Editor.radius));
            p.setProperty("height", String.valueOf(Editor.height));
            p.setProperty("step", String.valueOf(Editor.step));
            Files.createDirectories(file().getParent());
            try (var out = Files.newOutputStream(file())) { p.store(out, "Mackros"); }
        } catch (Exception ignored) { }
    }

    public static void reset() {
        reach = 128; previewLimit = 2500; undoLimit = 50; cmdRate = 12; colorMode = 1; fillMode = 0;
        maxVolume = 3_000_000L; showHud = true; showOutlines = true;
        relMove = true; autoSelect = false; hudCompact = false; accent = 0;
        Editor.pasteAir = false; Editor.preview = true;
        save();
    }
}
