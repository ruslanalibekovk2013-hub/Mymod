package com.mackros;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;

/**
 * Глобальная библиотека дизайна: проверенные инженерные и визуальные идеи из проектов и документации разных стран и языков.
 * Запись: название, источник, ссылка, язык, категория, описание, конструкция, принцип, материалы, идеи, ограничения, лицензия, статус.
 * Библиотека только подсказывает идеи; чужие постройки и код не копируются. Записи, добавленные командой «исследуй», по умолчанию непроверенные.
 */
public final class NeroLibrary {
    private static final List<JsonObject> SEED = new ArrayList<>();
    private static final List<JsonObject> USER = new ArrayList<>();
    private static boolean loaded = false;

    private static Path userFile() { return FabricLoader.getInstance().getConfigDir().resolve("mackros_design_library_user.json"); }

    private static synchronized void load() {
        if (loaded) return;
        loaded = true;
        try (InputStream in = NeroLibrary.class.getResourceAsStream("/mackros_design_library.json")) {
            if (in != null) for (JsonElement e : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray()) if (e.isJsonObject()) SEED.add(e.getAsJsonObject());
        } catch (Exception ignored) { }
        try {
            Path f = userFile();
            if (Files.exists(f))
                for (JsonElement e : JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonArray()) if (e.isJsonObject()) USER.add(e.getAsJsonObject());
        } catch (Exception ignored) { }
    }

    public static synchronized List<JsonObject> all() {
        load();
        List<JsonObject> r = new ArrayList<>(SEED);
        r.addAll(USER);
        return r;
    }

    private static String s(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }

    private static String cut(String s, int n) { return s.length() > n ? s.substring(0, n - 1) + "…" : s; }

    /** Добавляет запись пользователя (дубликаты по названию пропускаются). Возвращает true, если добавлена. */
    public static synchronized boolean add(JsonObject e) {
        load();
        String name = s(e, "name").trim().toLowerCase(Locale.ROOT);
        if (name.isEmpty()) return false;
        for (JsonObject o : all()) if (s(o, "name").trim().toLowerCase(Locale.ROOT).equals(name)) return false;
        USER.add(e);
        try {
            JsonArray a = new JsonArray();
            for (JsonObject o : USER) a.add(o);
            Files.writeString(userFile(), new Gson().toJson(a), StandardCharsets.UTF_8);
        } catch (Exception ignored) { }
        return true;
    }

    private static Set<String> stems(String text) {
        Set<String> r = new HashSet<>();
        for (String t : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}_]+")) {
            if (t.isEmpty()) continue;
            boolean cjk = t.codePointAt(0) >= 0x2E80;
            if (t.length() < (cjk ? 2 : 3)) continue;
            r.add(!cjk && t.length() > 5 ? t.substring(0, 5) : t);
        }
        return r;
    }

    private static int score(JsonObject e, String req, Set<String> rs) {
        StringBuilder sb = new StringBuilder(s(e, "name") + " " + s(e, "category") + " ");
        int sc = 0;
        if (e.has("tags") && e.get("tags").isJsonArray())
            for (JsonElement t : e.getAsJsonArray("tags")) {
                String tag = t.getAsString().toLowerCase(Locale.ROOT);
                sb.append(tag).append(' ');
                boolean cjk = !tag.isEmpty() && tag.codePointAt(0) >= 0x2E80;
                if (tag.length() >= (cjk ? 2 : 3) && req.contains(tag)) sc += 3;
            }
        for (String st : stems(sb.toString())) if (rs.contains(st)) sc++;
        return sc;
    }

    /** Лучшие подходящие записи для запроса на любом языке (по тегам на нескольких языках). */
    public static List<JsonObject> relevant(String request, int n) {
        String req = request == null ? "" : request.toLowerCase(Locale.ROOT);
        Set<String> rs = stems(req);
        TreeMap<Integer, List<JsonObject>> by = new TreeMap<>((a, b) -> b - a);
        for (JsonObject e : all()) {
            int sc = score(e, req, rs);
            if (sc > 0) by.computeIfAbsent(sc, k -> new ArrayList<>()).add(e);
        }
        List<JsonObject> r = new ArrayList<>();
        for (List<JsonObject> l : by.values()) for (JsonObject e : l) { if (r.size() < n) r.add(e); }
        return r;
    }

    /** Раздел системного запроса для внешнего ИИ: идеи из библиотеки, подходящие к запросу. */
    public static String promptSection(String request) {
        List<JsonObject> r = relevant(request, 4);
        if (r.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("\nDESIGN LIBRARY (ideas gathered from Minecraft engineering projects and documentation in many languages; use them as inspiration and adapt, never copy other people's builds or code; respect licenses; target Java 1.20.4):\n");
        for (JsonObject e : r)
            sb.append("- ").append(s(e, "name")).append(" [").append(s(e, "category")).append("; ").append(s(e, "lang")).append("; license: ").append(s(e, "license")).append("]: ")
                    .append(cut(s(e, "principle"), 200)).append(" Ideas: ").append(cut(s(e, "ideas"), 300)).append(" Limits: ").append(cut(s(e, "limits"), 200)).append('\n');
        return sb.toString();
    }

    /** Текст для команды «библиотека [слово]». */
    public static String describe(String q) {
        List<JsonObject> all = all();
        if (q == null || q.isBlank()) {
            Set<String> langs = new HashSet<>(), cats = new HashSet<>();
            int unverified = 0;
            for (JsonObject e : all) { langs.add(s(e, "lang")); cats.add(s(e, "category")); if (s(e, "status").startsWith("ai") || s(e, "status").equals("model-memory")) unverified++; }
            return "Библиотека дизайна: " + all.size() + " записей, языков источников: " + langs.size() + " (" + String.join(", ", langs) + "), категорий: " + String.join(", ", cats)
                    + (unverified > 0 ? ". Непроверенных (добавлено ИИ): " + unverified : "") + ". Напиши «библиотека лифт», чтобы увидеть записи по теме.";
        }
        List<JsonObject> r = relevant(q, 8);
        if (r.isEmpty()) return "В библиотеке пока нет записей по теме «" + q + "». Напиши «исследуй " + q + "», чтобы дополнить её через внешний ИИ.";
        StringBuilder sb = new StringBuilder("Записи по теме «" + q + "»: ");
        for (JsonObject e : r) sb.append("\n• ").append(s(e, "name")).append(" (").append(s(e, "lang")).append(", ").append(s(e, "source")).append(", лицензия: ").append(s(e, "license")).append(")");
        return sb.toString();
    }

    private NeroLibrary() { }
}
