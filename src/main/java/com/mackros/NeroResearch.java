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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Команда «исследуй <тема>»: внешний ИИ ищет идеи по всему миру на разных языках (на OpenRouter с веб-поиском) и дополняет библиотеку дизайна.
 * Записи помечаются как непроверенные: они служат вдохновением, а не готовыми чертежами. Чужие работы не копируются.
 */
public final class NeroResearch {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final String P = "Исследование";

    private static String system() {
        return "You are a Minecraft engineering researcher for the NERO building assistant (Minecraft Java Edition 1.20.4). "
                + "Research the given topic worldwide. Search in MANY languages (English, Russian, German, French, Spanish, Chinese, Japanese, Korean, Polish and others if useful) and across sources: YouTube, Reddit, Planet Minecraft, Minecraft Forum, CurseForge, Modrinth, GitHub, Minecraft Wiki, technical redstone forums and international map/schematic sites. "
                + "Study not only finished builds but also 3D models, Blockbench and JSON block models, animations, mechanism diagrams, open-source mod code, rendering systems and interaction mechanics, paying attention to compact details such as buttons, displays, panels, door mechanisms, lighting and moving parts. "
                + "Never judge a project by its language, country or the popularity of its author. Do not copy anyone's work: extract ideas only, and write the license if you know it (otherwise \"не указана\"). Check compatibility with Java 1.20.4 and state limits honestly. "
                + "Never invent sources: if you cannot find a real source, set source to \"model-memory\" and url to \"\". "
                + "Return ONLY a JSON array (max 8 entries) of objects with fields: name, source, url, lang (ISO code of the source language), category, tags (array of up to 10 short keywords in several languages), description, design, principle, materials, ideas (how to adapt it in a Minecraft command-block mod), limits, license. Write all text fields in Russian.";
    }

    public static CompletableFuture<NeroResponse> run(String topic) {
        return OpenRouter.candidates(CLIENT).thenCompose(ms -> attempt(topic, ms, 0, true));
    }

    private static CompletableFuture<NeroResponse> attempt(String topic, List<String> ms, int i, boolean web) {
        if (i >= ms.size()) return CompletableFuture.completedFuture(NeroResponse.fail("модель не вернула разборчивых записей", P));
        boolean useWeb = web && NeroConfig.isOpenRouter();
        return call(ms.get(i), topic, useWeb).thenCompose(r -> {
            if (r.ok) return CompletableFuture.completedFuture(r);
            return useWeb ? attempt(topic, ms, i, false) : attempt(topic, ms, i + 1, true);
        });
    }

    private static String str(JsonObject o, String k, int max) {
        try {
            String s = o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString().trim() : "";
            return s.length() > max ? s.substring(0, max) : s;
        } catch (Exception e) { return ""; }
    }

    private static CompletableFuture<NeroResponse> call(String model, String topic, boolean web) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.addProperty("temperature", 0.2);
            body.addProperty("max_tokens", 8000);
            if (web) {
                JsonArray pl = new JsonArray();
                JsonObject w = new JsonObject();
                w.addProperty("id", "web");
                w.addProperty("max_results", 10);
                pl.add(w);
                body.add("plugins", pl);
            }
            JsonArray msgs = new JsonArray();
            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", system());
            msgs.add(sys);
            JsonObject u = new JsonObject();
            u.addProperty("role", "user");
            StringBuilder known = new StringBuilder();
            for (JsonObject k : NeroLibrary.relevant(topic, 8)) known.append(k.has("name") ? k.get("name").getAsString() : "").append("; ");
            u.addProperty("content", "Topic: " + topic + (known.length() > 0 ? "\nAlready in the local library (do not repeat these, look for NEW ideas): " + known : ""));
            msgs.add(u);
            body.add("messages", msgs);
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(NeroConfig.url)).timeout(Duration.ofSeconds(Math.max(120, NeroConfig.timeoutSec))).header("Content-Type", "application/json");
            if (!NeroConfig.key.isEmpty()) b.header("Authorization", "Bearer " + NeroConfig.key);
            if (NeroConfig.isOpenRouter()) { b.header("HTTP-Referer", "https://github.com/mackros-mod"); b.header("X-Title", "Mackros NERO"); }
            return CLIENT.sendAsync(b.POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(), HttpResponse.BodyHandlers.ofString())
                    .thenApply(r -> parse(r, topic, web))
                    .exceptionally(e -> NeroResponse.fail("Сеть", P));
        } catch (Exception e) {
            return CompletableFuture.completedFuture(NeroResponse.fail("Неверный адрес API", P));
        }
    }

    private static NeroResponse parse(HttpResponse<String> resp, String topic, boolean web) {
        if (resp.statusCode() / 100 != 2) return NeroResponse.fail("HTTP " + resp.statusCode(), P);
        try {
            JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
            JsonObject msg = root.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message");
            if (!msg.has("content") || msg.get("content").isJsonNull()) return NeroResponse.fail("пустой ответ", P);
            String c = msg.get("content").getAsString();
            int a = c.indexOf('['), z = c.lastIndexOf(']');
            if (a < 0 || z <= a) return NeroResponse.fail("нет JSON", P);
            JsonArray arr = JsonParser.parseString(c.substring(a, z + 1)).getAsJsonArray();
            int added = 0;
            Set<String> langs = new LinkedHashSet<>();
            List<String> names = new ArrayList<>();
            for (JsonElement el : arr) {
                if (!el.isJsonObject() || added >= 8) continue;
                JsonObject o = el.getAsJsonObject(), e = new JsonObject();
                String name = str(o, "name", 120), cat = str(o, "category", 60), desc = str(o, "description", 600);
                if (name.isEmpty() || cat.isEmpty() || desc.isEmpty()) continue;
                e.addProperty("id", "user-" + Integer.toHexString((name + topic).hashCode()));
                e.addProperty("name", name);
                String src = str(o, "source", 200);
                e.addProperty("source", src.isEmpty() ? "model-memory" : src);
                e.addProperty("url", str(o, "url", 300));
                String lang = str(o, "lang", 12);
                e.addProperty("lang", lang.isEmpty() ? "?" : lang);
                e.addProperty("category", cat);
                JsonArray tags = new JsonArray();
                if (o.has("tags") && o.get("tags").isJsonArray()) for (JsonElement t : o.getAsJsonArray("tags")) { if (tags.size() < 12) try { tags.add(t.getAsString()); } catch (Exception ignored) { } }
                tags.add(topic.toLowerCase());
                e.add("tags", tags);
                for (String k : new String[]{"description", "design", "principle", "materials", "ideas", "limits"}) e.addProperty(k, str(o, k, 600));
                String lic = str(o, "license", 120);
                e.addProperty("license", lic.isEmpty() ? "не указана" : lic);
                e.addProperty("status", web ? "ai-web-research" : "model-memory");
                e.addProperty("verified", false);
                e.addProperty("added", LocalDate.now().toString());
                if (NeroLibrary.add(e)) { added++; names.add(name); langs.add(e.get("lang").getAsString()); }
            }
            if (added == 0) return NeroResponse.fail("новых записей нет", P);
            String reply = "Исследование «" + topic + "»: добавлено идей " + added + " (языки источников: " + String.join(", ", langs) + "). "
                    + (web ? "Источник: веб-поиск через OpenRouter." : "Веб-поиск был недоступен, записи из памяти модели.") + " Записи НЕпроверенные и служат вдохновением. Новое: " + String.join("; ", names);
            return new NeroResponse(true, null, reply, null, null, P);
        } catch (Exception ex) {
            return NeroResponse.fail("не удалось разобрать ответ", P);
        }
    }

    private NeroResearch() { }
}
