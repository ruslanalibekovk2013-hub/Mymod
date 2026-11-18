package com.mackros;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Структурированное описание проекта NERO AI (то, что понимает планировщик). Команды сюда не входят никогда. */
public final class NeroSpec {
    public static final List<String> KINDS = List.of("house", "tower", "hotel", "bunker", "castle", "road", "bridge", "tunnel", "parking",
            "station", "airport", "warehouse", "factory", "secret", "alarm", "lift", "tele", "gate", "gate_key", "vault", "farm", "miner",
            "sentry", "dome", "wave", "belt", "show", "weather", "homes", "lamps", "custom");
    public static final List<String> STYLES = List.of("modern", "medieval", "futuristic");
    public static final List<String> EXTRAS = List.of("garage", "pool", "lighting", "autogate", "lift", "furniture", "alarm");
    public static final List<String> SIDES = List.of("left", "right", "back");

    public String kind = "";
    public String style = null;
    public String garageSide = "right";
    public int floors = 0, w = 0, d = 0, len = 0, code = 0, copies = 1;
    public final Set<String> extras = new LinkedHashSet<>();
    public boolean askedStyle = false, askedExtras = false;
    public long seed = 0;
    /** Свободная постройка: исходное описание, название и чертёж ИИ (может быть null: тогда локальный композитор). */
    public String desc = "", name = "";
    public JsonObject bp = null;

    public NeroSpec copy() {
        NeroSpec s = new NeroSpec();
        s.kind = kind; s.style = style; s.garageSide = garageSide;
        s.floors = floors; s.w = w; s.d = d; s.len = len; s.code = code; s.copies = copies;
        s.extras.addAll(extras);
        s.askedStyle = askedStyle; s.askedExtras = askedExtras; s.seed = seed;
        s.desc = desc; s.name = name; s.bp = bp;
        return s;
    }

    public static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    /** Этажи с учётом значения по умолчанию для типа. */
    public int fl() {
        if (floors > 0) return floors;
        return switch (kind) { case "house" -> 2; case "tower" -> 10; case "hotel" -> 8; case "lift" -> 5; default -> 1; };
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("kind", kind);
        if (style != null) o.addProperty("style", style);
        o.addProperty("floors", floors);
        o.addProperty("width", w);
        o.addProperty("depth", d);
        o.addProperty("length", len);
        o.addProperty("code", code);
        o.addProperty("copies", copies);
        o.addProperty("seed", seed);
        o.addProperty("garage_side", garageSide);
        JsonArray a = new JsonArray();
        for (String e : extras) a.add(e);
        o.add("extras", a);
        if (kind.equals("custom")) {
            o.addProperty("desc", desc);
            o.addProperty("name", name);
            if (bp != null) o.add("blueprint", bp);
        }
        return o;
    }

    private static int geti(JsonObject o, String k) {
        try { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : 0; } catch (Exception e) { return 0; }
    }

    /** Безопасный разбор: берутся только известные поля, числа ограничиваются. Неизвестный kind => null. */
    public static NeroSpec fromJson(JsonObject o) {
        try {
            String k = o.has("kind") ? o.get("kind").getAsString().trim().toLowerCase() : "";
            if (!KINDS.contains(k)) return null;
            NeroSpec s = new NeroSpec();
            s.kind = k;
            if (k.equals("custom")) {
                if (o.has("desc") && !o.get("desc").isJsonNull()) s.desc = o.get("desc").getAsString();
                if (o.has("name") && !o.get("name").isJsonNull()) s.name = o.get("name").getAsString();
                if (s.name.length() > 60) s.name = s.name.substring(0, 60);
                if (o.has("blueprint") && o.get("blueprint").isJsonObject()) s.bp = o.getAsJsonObject("blueprint");
                else if (o.has("ops") || o.has("mechs") || o.has("chains")) s.bp = o;
            }
            if (o.has("style") && !o.get("style").isJsonNull()) {
                String st = o.get("style").getAsString().trim().toLowerCase();
                if (STYLES.contains(st)) s.style = st;
            }
            s.floors = clamp(geti(o, "floors"), 0, 40);
            s.w = clamp(geti(o, "width"), 0, 64);
            s.d = clamp(geti(o, "depth"), 0, 64);
            s.len = clamp(geti(o, "length"), 0, 120);
            s.code = clamp(geti(o, "code"), 0, 9999);
            s.copies = clamp(geti(o, "copies"), 1, 4);
            try { s.seed = o.has("seed") ? o.get("seed").getAsLong() : 0; } catch (Exception e) { s.seed = 0; }
            if (o.has("garage_side")) {
                String g = o.get("garage_side").getAsString().trim().toLowerCase();
                if (SIDES.contains(g)) s.garageSide = g;
            }
            if (o.has("extras") && o.get("extras").isJsonArray())
                for (JsonElement e : o.getAsJsonArray("extras")) {
                    String x = e.getAsString().trim().toLowerCase();
                    if (EXTRAS.contains(x)) s.extras.add(x);
                }
            return s;
        } catch (Exception e) {
            return null;
        }
    }

    public static String kindRu(String k) {
        return switch (k) {
            case "house" -> "Дом"; case "tower" -> "Небоскрёб"; case "hotel" -> "Отель"; case "bunker" -> "Бункер";
            case "castle" -> "Замок"; case "road" -> "Дорога"; case "bridge" -> "Мост"; case "tunnel" -> "Тоннель";
            case "parking" -> "Парковка"; case "station" -> "Станция"; case "airport" -> "Аэропорт"; case "warehouse" -> "Склад";
            case "factory" -> "Фабрика"; case "secret" -> "Секретная комната"; case "alarm" -> "Сигнализация"; case "lift" -> "Лифт";
            case "tele" -> "Сеть телепортов"; case "gate" -> "Автоворота"; case "gate_key" -> "Ворота по ключу"; case "vault" -> "Кодовый замок";
            case "farm" -> "Автоферма"; case "miner" -> "Автобур"; case "sentry" -> "Турель"; case "dome" -> "Купол";
            case "wave" -> "Арена волн"; case "belt" -> "Конвейер"; case "show" -> "Световое шоу"; case "weather" -> "Метеопанель";
            case "homes" -> "Система домов"; case "lamps" -> "Умные фонари"; case "custom" -> "Свободная постройка";
            default -> k;
        };
    }

    public static String styleRu(String s) {
        if (s == null) return "обычный";
        return switch (s) { case "modern" -> "современный"; case "medieval" -> "средневековый"; default -> "футуристический"; };
    }

    public static String extraRu(String e) {
        return switch (e) {
            case "garage" -> "гараж"; case "pool" -> "бассейн"; case "lighting" -> "освещение"; case "autogate" -> "автоворота";
            case "lift" -> "лифт"; case "furniture" -> "мебель"; default -> "сигнализация";
        };
    }

    public String summary() {
        StringBuilder b = new StringBuilder(title());
        if (floors > 0) b.append(", этажей: ").append(floors);
        if (w > 0) b.append(", ширина ").append(w);
        if (d > 0) b.append(", глубина ").append(d);
        if (len > 0) b.append(", длина ").append(len);
        if (code > 0) b.append(", код ").append(code);
        if (copies > 1) b.append(", копий ").append(copies);
        if (!extras.isEmpty()) {
            b.append(", добавлено: ");
            boolean f = true;
            for (String e : extras) { if (!f) b.append(", "); b.append(extraRu(e)); f = false; }
        }
        return b.toString();
    }

    public String title() { return (kind.equals("custom") && !name.isEmpty() ? name : kindRu(kind)) + (style != null ? " (" + styleRu(style) + ")" : ""); }
}
