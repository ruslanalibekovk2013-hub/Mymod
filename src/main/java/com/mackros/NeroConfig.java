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
    /** Выбранная и проверенная модель запоминается и не меняется без разрешения пользователя. */
    public static String providerId = "openrouter", lockedModel = "", modelName = "", lastStatus = "не проверялось";
    public static long lastStatusAt = 0;
    /** Резерв: если модель недоступна, явно разрешить локальный режим. */
    public static boolean fallbackLocal = false;
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
            String enc = p.getProperty("keyEnc", "");
            key = !enc.isEmpty() ? NeroCrypto.dec(enc) : p.getProperty("key", "").trim();
            model = p.getProperty("model", "").trim();
            if (model.isEmpty()) model = "auto";
            timeoutSec = NeroSpec.clamp(Math.max(90, geti(p, "timeoutSec", 90)), 3, 300);
            maxResponse = NeroSpec.clamp(Math.max(120000, geti(p, "maxResponse", 200000)), 1000, 400000);
            if (!key.isEmpty() && !p.containsKey("enabled")) enabled = true;
            blocksPerTick = NeroSpec.clamp(geti(p, "blocksPerTick", 300), 20, 2000);
            providerId = p.getProperty("providerId", "openrouter");
            lockedModel = p.getProperty("lockedModel", "").trim();
            modelName = p.getProperty("modelName", "").trim();
            lastStatus = p.getProperty("lastStatus", "не проверялось");
            lastStatusAt = (long) geti(p, "lastStatusAt", 0) * 1000L;
            fallbackLocal = "1".equals(p.getProperty("fallbackLocal", "0"));
        } catch (Exception ignored) { }
    }

    public static void save() {
        try {
            Properties p = new Properties();
            p.setProperty("enabled", enabled ? "1" : "0");
            p.setProperty("url", url);
            String encKey = NeroCrypto.enc(key);
            if (encKey.isEmpty()) p.setProperty("key", key); else p.setProperty("keyEnc", encKey);
            p.setProperty("model", model);
            p.setProperty("timeoutSec", String.valueOf(timeoutSec));
            p.setProperty("maxResponse", String.valueOf(maxResponse));
            p.setProperty("blocksPerTick", String.valueOf(blocksPerTick));
            p.setProperty("providerId", providerId);
            p.setProperty("lockedModel", lockedModel);
            p.setProperty("modelName", modelName);
            p.setProperty("lastStatus", lastStatus);
            p.setProperty("lastStatusAt", String.valueOf(lastStatusAt / 1000L));
            p.setProperty("fallbackLocal", fallbackLocal ? "1" : "0");
            try (var out = Files.newOutputStream(file())) { p.store(out, "Mackros NERO AI (ключ хранится локально, не публикуй этот файл)"); }
        } catch (Exception ignored) { }
    }

    public static synchronized void status(String s) { lastStatus = s; lastStatusAt = System.currentTimeMillis(); save(); }

    public static String modelInfo() {
        String m = model.equalsIgnoreCase("auto") || model.isEmpty() ? (lockedModel.isEmpty() ? "auto (ещё не выбрана)" : lockedModel + " (выбрана автоматически и закреплена)") : model + " (задана вручную)";
        return "Провайдер: " + providerId + "; модель: " + m + "; статус: " + lastStatus + "; резерв локальный: " + (fallbackLocal ? "вкл" : "выкл");
    }

    public static boolean isOpenRouter() { return url.contains("openrouter.ai"); }

    /** Внешний ИИ готов: включён, адрес верный, для OpenRouter есть ключ (для локальных серверов ключ не нужен). */
    public static boolean remoteReady() { return enabled && !url.isEmpty() && (url.startsWith("https://") || url.startsWith("http://")) && (!key.isEmpty() || !isOpenRouter()); }

    private NeroConfig() { }
}
