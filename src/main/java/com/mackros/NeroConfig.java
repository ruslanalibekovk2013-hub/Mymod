package com.mackros;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Настройки NERO AI. Файл config/mackros_nero.properties лежит только на твоём компьютере; ключ нигде не логируется. */
public final class NeroConfig {
    public static boolean enabled = false;
    public static String url = "", key = "", model = "";
    public static int timeoutSec = 30, maxResponse = 20000, blocksPerTick = 300;
    public static final int MAX_OPS = 40000;

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("mackros_nero.properties"); }

    private static int geti(Properties p, String k, int d) {
        try { return Integer.parseInt(p.getProperty(k, String.valueOf(d)).trim()); } catch (Exception e) { return d; }
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Properties p = new Properties();
            try (var in = Files.newInputStream(f)) { p.load(in); }
            enabled = "1".equals(p.getProperty("enabled", "0"));
            url = p.getProperty("url", "").trim();
            key = p.getProperty("key", "").trim();
            model = p.getProperty("model", "").trim();
            timeoutSec = NeroSpec.clamp(geti(p, "timeoutSec", 30), 3, 120);
            maxResponse = NeroSpec.clamp(geti(p, "maxResponse", 20000), 1000, 200000);
            blocksPerTick = NeroSpec.clamp(geti(p, "blocksPerTick", 300), 20, 2000);
        } catch (Exception ignored) { }
    }

    public static void save() {
        try {
            Properties p = new Properties();
            p.setProperty("enabled", enabled ? "1" : "0");
            p.setProperty("url", url);
            p.setProperty("key", key);
            p.setProperty("model", model);
            p.setProperty("timeoutSec", String.valueOf(timeoutSec));
            p.setProperty("maxResponse", String.valueOf(maxResponse));
            p.setProperty("blocksPerTick", String.valueOf(blocksPerTick));
            try (var out = Files.newOutputStream(file())) { p.store(out, "Mackros NERO AI (ключ хранится локально, не публикуй этот файл)"); }
        } catch (Exception ignored) { }
    }

    public static boolean remoteReady() { return enabled && !url.isEmpty() && (url.startsWith("https://") || url.startsWith("http://")); }

    private NeroConfig() { }
}
