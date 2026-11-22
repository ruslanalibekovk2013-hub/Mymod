package com.mackros;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Локальная библиотека моделей: готовые части (чертежи) для повторного использования без нового поиска и генерации.
 * Запись: id, название, категория, теги, источник, ссылка, автор, лицензия, формат, версия, совместимость, размеры, где использовалась.
 * Сюда сохраняются только свои конструкции пользователя (команда «сохрани часть»). Автоматического скачивания чужих файлов нет.
 */
public final class NeroModels {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path dir() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("mackros").resolve("models");
        try { Files.createDirectories(p); } catch (Exception ignored) { }
        return p;
    }

    private static String s(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }

    private static String slug(String n) { return n.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "_"); }

    public static List<JsonObject> all() {
        List<JsonObject> r = new ArrayList<>();
        try (Stream<Path> f = Files.list(dir())) {
            for (Path p : (Iterable<Path>) f.filter(x -> x.getFileName().toString().endsWith(".json"))::iterator) {
                try { r.add(JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject()); } catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }
        return r;
    }

    public static JsonObject get(String name) {
        for (JsonObject o : all()) if (s(o, "name").equalsIgnoreCase(name.trim()) || s(o, "id").equals(slug(name))) return o;
        return null;
    }

    private static void bounds(JsonArray ops, int[] b) {
        String[] xs = {"x", "x1", "x2", "cx"}, ys = {"y", "y1", "y2", "cy"}, zs = {"z", "z1", "z2", "cz"};
        for (JsonElement e : ops) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            for (String k : xs) if (o.has(k) && o.get(k).isJsonPrimitive()) { int v = o.get(k).getAsInt(); b[0] = Math.min(b[0], v); b[3] = Math.max(b[3], v); }
            for (String k : ys) if (o.has(k) && o.get(k).isJsonPrimitive()) { int v = o.get(k).getAsInt(); b[1] = Math.min(b[1], v); b[4] = Math.max(b[4], v); }
            for (String k : zs) if (o.has(k) && o.get(k).isJsonPrimitive()) { int v = o.get(k).getAsInt(); b[2] = Math.min(b[2], v); b[5] = Math.max(b[5], v); }
        }
    }

    /** Сохраняет часть текущего проекта как модель. Возвращает сообщение. */
    public static String savePart(NeroSpec spec, String part, String name) {
        if (spec == null || spec.bp == null || !spec.bp.has("ops")) return "У проекта нет чертежа, сохранять нечего.";
        JsonArray ops = new JsonArray();
        for (JsonElement e : spec.bp.getAsJsonArray("ops")) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject().deepCopy();
            if (!s(o, "part").equalsIgnoreCase(part.trim())) continue;
            o.remove("part");
            ops.add(o);
        }
        if (ops.isEmpty()) return "Часть «" + part + "» не найдена. Части проекта: " + String.join(", ", NeroPatch.parts(spec));
        int[] b = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        bounds(ops, b);
        JsonObject m = new JsonObject();
        m.addProperty("id", slug(name));
        m.addProperty("name", name.trim());
        m.addProperty("category", NeroRequirements.classify(name));
        JsonArray tags = new JsonArray();
        for (String t : name.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) if (t.length() >= 3) tags.add(t);
        tags.add(part.toLowerCase(Locale.ROOT));
        m.add("tags", tags);
        m.addProperty("source", "создано пользователем в проекте «" + spec.title() + "»");
        m.addProperty("url", "");
        m.addProperty("author", "пользователь");
        m.addProperty("license", "личное использование (своя конструкция)");
        m.addProperty("format", "nero-blueprint");
        m.addProperty("version", 1);
        m.addProperty("compat", "Minecraft Java 1.20.4, Mackros");
        JsonArray size = new JsonArray();
        size.add(b[3] - b[0] + 1); size.add(b[4] - b[1] + 1); size.add(b[5] - b[2] + 1);
        m.add("size", size);
        m.addProperty("created", LocalDate.now().toString());
        m.add("usedIn", new JsonArray());
        m.add("ops", ops);
        try {
            Files.writeString(dir().resolve(slug(name) + ".json"), GSON.toJson(m), StandardCharsets.UTF_8);
        } catch (Exception e) { return "Не удалось сохранить модель."; }
        return "Модель «" + name.trim() + "» сохранена в библиотеку (размер " + (b[3] - b[0] + 1) + "x" + (b[4] - b[1] + 1) + "x" + (b[5] - b[2] + 1) + "). Вставка: «вставь модель " + name.trim() + " <dx> <dy> <dz>».";
    }

    public static void markUsed(String name, String project) {
        JsonObject m = get(name);
        if (m == null) return;
        JsonArray u = m.has("usedIn") && m.get("usedIn").isJsonArray() ? m.getAsJsonArray("usedIn") : new JsonArray();
        for (JsonElement e : u) if (e.getAsString().equals(project)) return;
        u.add(project);
        m.add("usedIn", u);
        try { Files.writeString(dir().resolve(s(m, "id") + ".json"), GSON.toJson(m), StandardCharsets.UTF_8); } catch (Exception ignored) { }
    }

    public static String describe() {
        List<JsonObject> l = all();
        if (l.isEmpty()) return "Библиотека моделей пуста. Сохрани часть проекта командой «сохрани часть <часть> как <имя>».";
        StringBuilder sb = new StringBuilder("Библиотека моделей (" + l.size() + "):");
        for (JsonObject o : l) sb.append("\n• ").append(s(o, "name")).append(" · ").append(s(o, "size").replace("[", "").replace("]", "")).append(" · лицензия: ").append(s(o, "license"))
                .append(" · использована в проектах: ").append(o.has("usedIn") ? o.getAsJsonArray("usedIn").size() : 0);
        return sb.toString();
    }

    /** Подсказка для внешнего ИИ: подходящие сохранённые модели, чтобы использовать их, а не строить заново. */
    public static String promptSection(String request) {
        Set<String> rs = new HashSet<>();
        for (String t : request.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) if (t.length() >= 4) rs.add(t.length() > 5 ? t.substring(0, 5) : t);
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (JsonObject o : all()) {
            if (n >= 5) break;
            boolean hit = false;
            if (o.has("tags")) for (JsonElement t : o.getAsJsonArray("tags")) { String x = t.getAsString(); if (x.length() >= 4 && rs.contains(x.length() > 5 ? x.substring(0, 5) : x)) hit = true; }
            if (!hit) continue;
            sb.append("- \"").append(s(o, "name")).append("\" size ").append(o.get("size")).append('\n');
            n++;
        }
        if (n == 0) return "";
        return "\nLOCAL MODEL LIBRARY (the player's saved reusable parts; reuse them instead of rebuilding: {\"op\":\"model\",\"name\":\"<name>\",\"x\":0,\"y\":0,\"z\":0,\"part\":\"<part>\"}):\n" + sb;
    }

    private NeroModels() { }
}
