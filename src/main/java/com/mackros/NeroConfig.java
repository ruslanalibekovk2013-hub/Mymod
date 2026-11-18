package com.mackros;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Настройки NERO AI. Файл config/mackros_nero.properties лежит только на твоём компьютере; ключ нигде не логируется. */
public final class NeroConfig {
    public static boolean enabled = false;
    public static String url = OpenRouter.CHAT_URL, key = "", model = "auto";
    public static int timeoutSec = 90, maxResponse = 200000, blocksPerTick = 300;
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
            if (url.isEmpty()) url = OpenRouter.CHAT_URL;
            key = p.getProperty("key", "").trim();
            model = p.getProperty("model", "").trim();
            if (model.isEmpty()) model = "auto";
            timeoutSec = NeroSpec.clamp(Math.max(90, geti(p, "timeoutSec", 90)), 3, 300);
            maxResponse = NeroSpec.clamp(Math.max(120000, geti(p, "maxResponse", 200000)), 1000, 400000);
            if (!key.isEmpty() && !p.containsKey("enabled")) enabled = true;
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

    public static boolean isOpenRouter() { return url.contains("openrouter.ai"); }

    /** Внешний ИИ готов: включён, адрес верный, для OpenRouter есть ключ (для локальных серверов ключ не нужен). */
    public static boolean remoteReady() { return enabled && !url.isEmpty() && (url.startsWith("https://") || url.startsWith("http://")) && (!key.isEmpty() || !isOpenRouter()); }

    private NeroConfig() { }
}
