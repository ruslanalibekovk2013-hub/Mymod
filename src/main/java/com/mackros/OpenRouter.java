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
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** OpenRouter: адрес API задан по умолчанию, бесплатные модели подбираются автоматически (список /models, цена = 0). */
public final class OpenRouter {
    public static final String CHAT_URL = "https://openrouter.ai/api/v1/chat/completions";
    public static final String MODELS_URL = "https://openrouter.ai/api/v1/models";
    private static final List<String> FALLBACK = List.of("openrouter/free", "qwen/qwen3-coder:free", "deepseek/deepseek-r1-0528:free",
            "openai/gpt-oss-120b:free", "meta-llama/llama-3.3-70b-instruct:free", "z-ai/glm-4.5-air:free", "google/gemma-3-27b-it:free");
    private static final String[][] PREFER = {{"qwen3-coder", "9"}, {"gpt-oss-120b", "8"}, {"deepseek", "7"}, {"glm", "7"}, {"kimi", "7"}, {"qwen3", "6"},
            {"llama-3.3-70b", "6"}, {"gemini", "5"}, {"mistral", "4"}, {"gemma", "3"}, {"llama", "3"}};
    private static volatile List<String> cache = null;
    private static volatile long cachedAt = 0;
    private static volatile String good = null;

    public static void markGood(String m) { good = m; }

    private static boolean zero(JsonElement e) {
        try { return e != null && !e.isJsonNull() && Double.parseDouble(e.getAsString()) == 0.0; } catch (Exception ex) { return false; }
    }

    /** Список моделей для попыток: заданная вручную или лучшие бесплатные (первая — последняя удачная). */
    public static CompletableFuture<List<String>> candidates(HttpClient http) {
        String m = NeroConfig.model.trim();
        if (!m.isEmpty() && !m.equalsIgnoreCase("auto")) return CompletableFuture.completedFuture(List.of(m));
        if (!NeroConfig.isOpenRouter()) return CompletableFuture.completedFuture(List.of("local-model"));
        List<String> c = cache;
        if (c != null && System.currentTimeMillis() - cachedAt < 1_800_000L) return CompletableFuture.completedFuture(withGood(c));
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(MODELS_URL)).timeout(Duration.ofSeconds(15));
            if (!NeroConfig.key.isEmpty()) b.header("Authorization", "Bearer " + NeroConfig.key);
            return http.sendAsync(b.GET().build(), HttpResponse.BodyHandlers.ofString()).thenApply(resp -> {
                List<String> out = parse(resp.statusCode() / 100 == 2 ? resp.body() : "");
                if (out.isEmpty()) out = new ArrayList<>(FALLBACK);
                cache = out;
                cachedAt = System.currentTimeMillis();
                return withGood(out);
            }).exceptionally(e -> withGood(new ArrayList<>(FALLBACK)));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(withGood(new ArrayList<>(FALLBACK)));
        }
    }

    private static List<String> withGood(List<String> l) {
        List<String> r = new ArrayList<>(l);
        String g = good;
        if (g != null && r.remove(g)) r.add(0, g);
        return r;
    }

    private static List<String> parse(String raw) {
        List<String> out = new ArrayList<>();
        try {
            JsonArray data = JsonParser.parseString(raw).getAsJsonObject().getAsJsonArray("data");
            List<double[]> sc = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            for (JsonElement e : data) {
                JsonObject o = e.getAsJsonObject();
                String id = o.get("id").getAsString();
                String low = id.toLowerCase();
                if (low.contains("embed") || low.contains("image") || low.contains("audio") || low.contains("guard") || low.contains("vision")) continue;
                JsonObject pr = o.has("pricing") && o.get("pricing").isJsonObject() ? o.getAsJsonObject("pricing") : null;
                if (pr == null || !zero(pr.get("prompt")) || !zero(pr.get("completion"))) continue;
                if (o.has("architecture") && o.get("architecture").isJsonObject() && o.getAsJsonObject("architecture").has("output_modalities")) {
                    boolean text = false;
                    for (JsonElement mo : o.getAsJsonObject("architecture").getAsJsonArray("output_modalities")) if (mo.getAsString().equals("text")) text = true;
                    if (!text) continue;
                }
                double ctx = o.has("context_length") && !o.get("context_length").isJsonNull() ? o.get("context_length").getAsDouble() : 0;
                double w = 0;
                for (String[] p : PREFER) if (low.contains(p[0])) { w = Double.parseDouble(p[1]); break; }
                if (low.equals("openrouter/free")) w = 20;
                ids.add(id);
                sc.add(new double[]{w * 1_000_000 + Math.min(ctx, 999_999)});
            }
            for (int n = 0; n < 7 && !ids.isEmpty(); n++) {
                int best = 0;
                for (int i = 1; i < ids.size(); i++) if (sc.get(i)[0] > sc.get(best)[0]) best = i;
                out.add(ids.remove(best));
                sc.remove(best);
            }
        } catch (Exception ignored) { }
        return out;
    }

    private OpenRouter() { }
}
