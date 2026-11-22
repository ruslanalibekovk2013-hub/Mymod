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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Менеджер проектов NERO: библиотека «Мои проекты», версии (ничего не перезаписывается без возможности восстановления),
 * чат каждого проекта, экспорт/импорт, версия схемы данных и миграции.
 * Хранилище: config/mackros/nero_projects/&lt;id&gt;/ project.json (актуальная версия), v1.json, v2.json..., chat.json
 */
public final class NeroProjects {
    public static final int SCHEMA = 2;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public record Meta(String id, String name, String desc, String created, String modified, int versions) { }

    public record Version(int n, String time, String note) { }

    public record Msg(String role, String text, String time) { }

    /** Текущий открытый проект (null, пока не создан). */
    public static String currentId = "";

    private static Path root() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("mackros").resolve("nero_projects");
        try { Files.createDirectories(p); } catch (Exception ignored) { }
        return p;
    }

    private static Path dir(String id) { return root().resolve(id.replaceAll("[^A-Za-z0-9_-]", "")); }

    private static String now() { return LocalDateTime.now().format(FMT); }

    private static JsonObject read(Path f) {
        try { return JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject(); } catch (Exception e) { return null; }
    }

    private static boolean write(Path f, JsonObject o) {
        try {
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) { return false; }
    }

    private static String str(JsonObject o, String k) { return o != null && o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }

    /** Миграция данных проекта к текущей схеме. Схема 1: просто {spec,x,y,z} от старого NeroStore, без метаданных. */
    public static JsonObject migrate(JsonObject o) {
        int v = o.has("schema") ? o.get("schema").getAsInt() : 1;
        if (v < 2) {
            JsonObject n = new JsonObject();
            n.addProperty("schema", SCHEMA);
            n.addProperty("id", str(o, "id").isEmpty() ? "p" + Long.toHexString(System.nanoTime()) : str(o, "id"));
            JsonObject spec = o.has("spec") && o.get("spec").isJsonObject() ? o.getAsJsonObject("spec") : o;
            String nm = str(spec, "name");
            n.addProperty("name", nm.isEmpty() ? "Проект из старой версии" : nm);
            n.addProperty("desc", str(spec, "desc"));
            n.addProperty("created", now());
            n.addProperty("modified", now());
            n.add("spec", spec);
            return n;
        }
        return o;
    }

    public static List<Meta> list() {
        List<Meta> r = new ArrayList<>();
        try (Stream<Path> s = Files.list(root())) {
            s.filter(Files::isDirectory).forEach(d -> {
                JsonObject o = read(d.resolve("project.json"));
                if (o == null) return;
                o = migrate(o);
                int vs = 0;
                try (Stream<Path> f = Files.list(d)) { vs = (int) f.filter(p -> p.getFileName().toString().matches("v\\d+\\.json")).count(); } catch (Exception ignored) { }
                r.add(new Meta(str(o, "id"), str(o, "name"), str(o, "desc"), str(o, "created"), str(o, "modified"), vs));
            });
        } catch (Exception ignored) { }
        r.sort(Comparator.comparing(Meta::modified).reversed());
        return r;
    }

    private static int lastVersion(String id) {
        int n = 0;
        try (Stream<Path> f = Files.list(dir(id))) {
            for (Path p : (Iterable<Path>) f::iterator) {
                String nm = p.getFileName().toString();
                if (nm.matches("v\\d+\\.json")) n = Math.max(n, Integer.parseInt(nm.substring(1, nm.length() - 5)));
            }
        } catch (Exception ignored) { }
        return n;
    }

    /** Создаёт проект из спецификации и возвращает его ID. */
    public static String create(String name, NeroSpec spec) {
        String id = "p" + Long.toHexString(System.currentTimeMillis()) + UUID.randomUUID().toString().substring(0, 4);
        spec.projectId = id;
        JsonObject o = new JsonObject();
        o.addProperty("schema", SCHEMA);
        o.addProperty("id", id);
        o.addProperty("name", name);
        o.addProperty("desc", spec.desc == null ? "" : spec.desc);
        o.addProperty("created", now());
        o.addProperty("modified", now());
        o.add("spec", spec.toJson());
        write(dir(id).resolve("project.json"), o);
        saveVersion(id, spec.toJson(), "Создан");
        currentId = id;
        return id;
    }

    private static void saveVersion(String id, JsonObject spec, String note) {
        JsonObject v = new JsonObject();
        v.addProperty("n", lastVersion(id) + 1);
        v.addProperty("time", now());
        v.addProperty("note", note);
        v.add("spec", spec);
        write(dir(id).resolve("v" + (lastVersion(id) + 1) + ".json"), v);
        // хранится до 60 последних версий
        int last = lastVersion(id);
        for (int i = 1; i <= last - 60; i++) try { Files.deleteIfExists(dir(id).resolve("v" + i + ".json")); } catch (Exception ignored) { }
    }

    /** Сохраняет текущее состояние как новую версию (предыдущие остаются), если оно изменилось. */
    public static void save(NeroSpec spec, String note) {
        if (spec == null) return;
        if (spec.projectId == null || spec.projectId.isEmpty() || !Files.exists(dir(spec.projectId).resolve("project.json"))) {
            create(spec.title(), spec);
            return;
        }
        String id = spec.projectId;
        JsonObject cur = spec.toJson();
        JsonObject prev = read(dir(id).resolve("project.json"));
        JsonObject o = prev == null ? new JsonObject() : migrate(prev);
        boolean changed = prev == null || !cur.equals(o.get("spec"));
        o.addProperty("schema", SCHEMA);
        o.addProperty("id", id);
        if (str(o, "name").isEmpty()) o.addProperty("name", spec.title());
        o.addProperty("desc", spec.desc == null ? str(o, "desc") : spec.desc);
        if (str(o, "created").isEmpty()) o.addProperty("created", now());
        o.addProperty("modified", now());
        o.add("spec", cur);
        write(dir(id).resolve("project.json"), o);
        if (changed) saveVersion(id, cur, note);
        currentId = id;
    }

    public static NeroSpec open(String id) {
        JsonObject o = read(dir(id).resolve("project.json"));
        if (o == null) return null;
        o = migrate(o);
        NeroSpec s = NeroSpec.fromJson(o.getAsJsonObject("spec"));
        if (s != null) { s.projectId = id; currentId = id; }
        return s;
    }

    public static List<Version> versions(String id) {
        List<Version> r = new ArrayList<>();
        for (int n = lastVersion(id); n >= 1; n--) {
            JsonObject v = read(dir(id).resolve("v" + n + ".json"));
            if (v != null) r.add(new Version(n, str(v, "time"), str(v, "note")));
        }
        return r;
    }

    /** Восстанавливает версию n: она становится актуальной как НОВАЯ версия, история сохраняется. */
    public static NeroSpec restore(String id, int n) {
        JsonObject v = read(dir(id).resolve("v" + n + ".json"));
        if (v == null) return null;
        NeroSpec s = NeroSpec.fromJson(v.getAsJsonObject("spec"));
        if (s == null) return null;
        s.projectId = id;
        save(s, "Восстановлена версия " + n);
        return s;
    }

    public static String duplicate(String id) {
        NeroSpec s = open(id);
        if (s == null) return null;
        JsonObject o = read(dir(id).resolve("project.json"));
        String nid = create(str(o, "name") + " (копия)", s.copy());
        return nid;
    }

    public static boolean rename(String id, String name) {
        JsonObject o = read(dir(id).resolve("project.json"));
        if (o == null || name.isBlank()) return false;
        o = migrate(o);
        o.addProperty("name", name.trim());
        o.addProperty("modified", now());
        return write(dir(id).resolve("project.json"), o);
    }

    public static boolean delete(String id) {
        try (Stream<Path> w = Files.walk(dir(id))) {
            w.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.delete(p); } catch (Exception ignored) { } });
            if (currentId.equals(id)) currentId = "";
            return true;
        } catch (Exception e) { return false; }
    }

    public static Path exportDir() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("mackros").resolve("nero_export");
        try { Files.createDirectories(p); } catch (Exception ignored) { }
        return p;
    }

    /** Экспорт проекта вместе с чатом в config/mackros/nero_export/&lt;имя&gt;.nero.json. */
    public static String export(String id) {
        JsonObject o = read(dir(id).resolve("project.json"));
        if (o == null) return null;
        o = migrate(o);
        JsonArray chat = new JsonArray();
        for (Msg m : chat(id)) { JsonObject c = new JsonObject(); c.addProperty("role", m.role()); c.addProperty("text", m.text()); c.addProperty("time", m.time()); chat.add(c); }
        o.add("chat", chat);
        String fn = str(o, "name").replaceAll("[^\\p{L}\\p{N}_-]+", "_") + ".nero.json";
        Path f = exportDir().resolve(fn);
        return write(f, o) ? f.toString() : null;
    }

    /** Импорт всех файлов *.nero.json из папки экспорта (и совместимых старых). Возвращает число импортированных проектов. */
    public static int importAll() {
        int n = 0;
        try (Stream<Path> s = Files.list(exportDir())) {
            for (Path p : (Iterable<Path>) s.filter(x -> x.getFileName().toString().endsWith(".nero.json"))::iterator) {
                JsonObject o = read(p);
                if (o == null) continue;
                o = migrate(o);
                NeroSpec sp = NeroSpec.fromJson(o.getAsJsonObject("spec"));
                if (sp == null) continue;
                String id = create(str(o, "name").isEmpty() ? sp.title() : str(o, "name"), sp);
                if (o.has("chat") && o.get("chat").isJsonArray()) {
                    for (JsonElement e : o.getAsJsonArray("chat")) {
                        if (!e.isJsonObject()) continue;
                        JsonObject c = e.getAsJsonObject();
                        appendChat(id, str(c, "role"), str(c, "text"));
                    }
                }
                n++;
            }
        } catch (Exception ignored) { }
        return n;
    }

    /** Переносит последний построенный проект старой версии (mackros_nero_project.json) в библиотеку, один раз. */
    public static void migrateLegacy() {
        try {
            Path f = FabricLoader.getInstance().getConfigDir().resolve("mackros_nero_project.json");
            Path mark = FabricLoader.getInstance().getConfigDir().resolve("mackros_nero_project.json.migrated");
            if (!Files.exists(f) || Files.exists(mark)) return;
            JsonObject o = read(f);
            if (o != null) {
                o = migrate(o);
                NeroSpec s = NeroSpec.fromJson(o.getAsJsonObject("spec"));
                if (s != null) create(str(o, "name").isEmpty() ? s.title() : str(o, "name"), s);
            }
            Files.writeString(mark, "migrated", StandardCharsets.UTF_8);
        } catch (Exception ignored) { }
    }

    // ------------------------------------------------------------------ чат проекта
    public static List<Msg> chat(String id) {
        List<Msg> r = new ArrayList<>();
        if (id == null || id.isEmpty()) return r;
        try {
            Path f = dir(id).resolve("chat.json");
            if (!Files.exists(f)) return r;
            for (JsonElement e : JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                r.add(new Msg(str(o, "role"), str(o, "text"), str(o, "time")));
            }
        } catch (Exception ignored) { }
        return r;
    }

    public static synchronized void appendChat(String id, String role, String text) {
        if (id == null || id.isEmpty()) return;
        try {
            JsonArray a = new JsonArray();
            for (Msg m : chat(id)) { JsonObject o = new JsonObject(); o.addProperty("role", m.role()); o.addProperty("text", m.text()); o.addProperty("time", m.time()); a.add(o); }
            JsonObject n = new JsonObject();
            n.addProperty("role", role); n.addProperty("text", text); n.addProperty("time", now());
            a.add(n);
            while (a.size() > 300) a.remove(0);
            Files.createDirectories(dir(id));
            Files.writeString(dir(id).resolve("chat.json"), GSON.toJson(a), StandardCharsets.UTF_8);
        } catch (Exception ignored) { }
    }

    private NeroProjects() { }
}
