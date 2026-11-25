package com.mackros;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * OpenRouter: адрес API задан по умолчанию, нужен только токен. Модели подбираются сами из живого списка /models:
 * «бесплатные» (цена 0) или «лучшие» (платные, но не дороже порога, плюс бесплатный запас). Для изображений берутся модели с поддержкой картинок.
 */
public final class OpenRouter {
    public static final String CHAT_URL = "https://openrouter.ai/api/v1/chat/completions";
    public static final String MODELS_URL = "https://openrouter.ai/api/v1/models";
    private static final List<String> FALLBACK = List.of("openrouter/free", "qwen/qwen3-coder:free", "deepseek/deepseek-r1-0528:free",
            "openai/gpt-oss-120b:free", "meta-llama/llama-3.3-70b-instruct:free", "z-ai/glm-4.5-air:free", "google/gemma-3-27b-it:free");
    private static final String[][] PREFER = {{"qwen3-coder", "9"}, {"gpt-oss-120b", "8"}, {"deepseek", "7"}, {"glm", "7"}, {"kimi", "7"}, {"qwen3", "6"},
            {"llama-3.3-70b", "6"}, {"gemini", "5"}, {"mistral", "4"}, {"gemma", "3"}, {"llama", "3"}};
    private static final String[][] BEST = {{"claude-sonnet", "10"}, {"claude-opus", "9"}, {"gpt-5", "9"}, {"gemini-2.5-pro", "9"}, {"gemini-3", "9"}, {"deepseek", "8"},
            {"qwen3-max", "7"}, {"qwen3-coder", "7"}, {"kimi", "7"}, {"glm", "7"}, {"grok", "6"}};
    private static final String[] BAD_BEST = {"nano", "mini", "lite", "haiku", "flash", "image", "audio", "embed", "guard", "search", "oss"};
    private static final Map<String, List<String>> CACHE = new HashMap<>();
    private static final Map<String, Long> AT = new HashMap<>();
    private static volatile String good = null;
    private static volatile boolean lastVision = false;

    public static void markGood(String m) {
        good = m;
        if ((NeroConfig.model.trim().isEmpty() || NeroConfig.model.equalsIgnoreCase("auto")) && !lastVision && NeroConfig.quality == 0) { NeroConfig.lockedModel = m; NeroConfig.modelName = m; }
        NeroConfig.status("работает");
    }

    private static double price(JsonElement e) {
        try { return e == null || e.isJsonNull() ? -1 : Double.parseDouble(e.getAsString()); } catch (Exception ex) { return -1; }
    }

    public static CompletableFuture<List<String>> candidates(HttpClient http) { return candidates(http, false); }

    /** Список моделей для попыток: заданная вручную (только она) или лучшие по режиму; последняя удачная идёт первой. */
    public static CompletableFuture<List<String>> candidates(HttpClient http, boolean vision) {
        String m = NeroConfig.model.trim();
        lastVision = vision;
        if (!m.isEmpty() && !m.equalsIgnoreCase("auto")) return CompletableFuture.completedFuture(List.of(m));
        if (!NeroConfig.isOpenRouter()) return CompletableFuture.completedFuture(List.of("local-model"));
        boolean paid = NeroConfig.quality == 1;
        String ck = (paid ? "p" : "f") + (vision ? "v" : "t");
        List<String> c;
        Long at;
        synchronized (CACHE) { c = CACHE.get(ck); at = AT.get(ck); }
        if (c != null && at != null && System.currentTimeMillis() - at < 1_800_000L) return CompletableFuture.completedFuture(withGood(c));
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(MODELS_URL)).timeout(Duration.ofSeconds(15));
            if (!NeroConfig.key.isEmpty()) b.header("Authorization", "Bearer " + NeroConfig.key);
            return http.sendAsync(b.GET().build(), HttpResponse.BodyHandlers.ofString()).thenApply(resp -> {
                String body = resp.statusCode() / 100 == 2 ? resp.body() : "";
                List<String> out = new ArrayList<>();
                if (paid) {
                    out.addAll(parse(body, true, vision, 5));
                    for (String f : parse(body, false, vision, 3)) if (!out.contains(f)) out.add(f);
                } else out.addAll(parse(body, false, vision, 7));
                if (out.isEmpty() && !vision) out = new ArrayList<>(FALLBACK);
                synchronized (CACHE) { CACHE.put(ck, out); AT.put(ck, System.currentTimeMillis()); }
                return withGood(out);
            }).exceptionally(e -> withGood(vision ? new ArrayList<>() : new ArrayList<>(FALLBACK)));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(withGood(vision ? new ArrayList<>() : new ArrayList<>(FALLBACK)));
        }
    }

    private static List<String> withGood(List<String> l) {
        List<String> r = new ArrayList<>(l);
        String g = good != null ? good : (!NeroConfig.lockedModel.isEmpty() ? NeroConfig.lockedModel : null);
        if (g != null && r.remove(g)) r.add(0, g);
        if (r.isEmpty()) r.add("openrouter/free");
        return r;
    }

    private static boolean has(JsonObject arch, String key, String val) {
        if (arch == null || !arch.has(key) || !arch.get(key).isJsonArray()) return false;
        for (JsonElement mo : arch.getAsJsonArray(key)) if (mo.getAsString().equals(val)) return true;
        return false;
    }

    private static List<String> parse(String raw, boolean paid, boolean vision, int take) {
        List<String> out = new ArrayList<>();
        try {
            JsonArray data = JsonParser.parseString(raw).getAsJsonObject().getAsJsonArray("data");
            List<Double> sc = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            for (JsonElement e : data) {
                JsonObject o = e.getAsJsonObject();
                String id = o.get("id").getAsString();
                String low = id.toLowerCase();
                if (low.contains("embed") || low.contains("image") || low.contains("audio") || low.contains("guard")) continue;
                if (!vision && low.contains("vision")) continue;
                JsonObject pr = o.has("pricing") && o.get("pricing").isJsonObject() ? o.getAsJsonObject("pricing") : null;
                if (pr == null) continue;
                double pp = price(pr.get("prompt")), pc = price(pr.get("completion"));
                if (pp < 0 || pc < 0) continue;
                if (paid) { if (pc > 0.000015 || pp > 0.000005 || pc == 0) continue; }
                else if (pp != 0.0 || pc != 0.0) continue;
                JsonObject arch = o.has("architecture") && o.get("architecture").isJsonObject() ? o.getAsJsonObject("architecture") : null;
                if (arch != null && arch.has("output_modalities") && !has(arch, "output_modalities", "text")) continue;
                if (vision && !has(arch, "input_modalities", "image")) continue;
                double ctx = o.has("context_length") && !o.get("context_length").isJsonNull() ? o.get("context_length").getAsDouble() : 0;
                if (ctx > 0 && ctx < 32000) continue;
                double w = 0;
                if (paid) {
                    boolean bad = false;
                    for (String x : BAD_BEST) if (low.contains(x)) { bad = true; break; }
                    if (bad && !vision) continue;
                    for (String[] p : BEST) if (low.contains(p[0])) { w = Double.parseDouble(p[1]); break; }
                    if (w == 0) continue;
                    double created = o.has("created") && !o.get("created").isJsonNull() ? o.get("created").getAsDouble() : 0;
                    ids.add(id);
                    sc.add(w * 1e12 + created);
                } else {
                    for (String[] p : PREFER) if (low.contains(p[0])) { w = Double.parseDouble(p[1]); break; }
                    if (low.equals("openrouter/free")) w = 20;
                    ids.add(id);
                    sc.add(w * 1_000_000 + Math.min(ctx, 999_999));
                }
            }
            for (int n = 0; n < take && !ids.isEmpty(); n++) {
                int best = 0;
                for (int i = 1; i < ids.size(); i++) if (sc.get(i) > sc.get(best)) best = i;
                out.add(ids.remove(best));
                sc.remove(best);
            }
        } catch (Exception ignored) { }
        return out;
    }

    private OpenRouter() { }
}
