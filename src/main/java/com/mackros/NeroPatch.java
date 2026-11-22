package com.mackros;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Точечные правки проекта из чата: затрагивается только указанный компонент (часть), остальной чертёж остаётся как есть и перестраивается локально,
 * без нового запроса к модели. Формат блока в ответе модели: ```nero-patch {"ops":[{"type":"replace_part","part":"Шахта","ops":[...]}]} ```
 */
public final class NeroPatch {
    private static final Pattern FENCE = Pattern.compile("```nero-patch\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL);

    /** Результат применения: изменённые компоненты или ошибка. */
    public static final class Result {
        public final List<String> changed = new ArrayList<>();
        public String error = null;
        public final List<String> warnings = new ArrayList<>();
    }

    public static String extract(String reply) {
        Matcher m = FENCE.matcher(reply == null ? "" : reply);
        return m.find() ? m.group(1) : null;
    }

    public static String stripPatch(String reply) { return FENCE.matcher(reply == null ? "" : reply).replaceAll("").trim(); }

    private static String s(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }

    private static JsonArray arr(JsonObject o, String k) {
        if (!o.has(k) || !o.get(k).isJsonArray()) { JsonArray a = new JsonArray(); o.add(k, a); }
        return o.getAsJsonArray(k);
    }

    private static boolean same(String a, String b) { return a.trim().toLowerCase(Locale.ROOT).equals(b.trim().toLowerCase(Locale.ROOT)); }

    /** Применяет патч к спецификации (меняет spec.bp). Спецификация без чертежа (шаблон) точечно не правится. */
    public static Result apply(NeroSpec spec, String patchJson) {
        Result r = new Result();
        if (spec == null || spec.bp == null) { r.error = "у этого проекта нет редактируемого чертежа (он создан из встроенного шаблона), пересоздай его запросом"; return r; }
        JsonObject bp = spec.bp.deepCopy();
        try {
            JsonObject patch = JsonParser.parseString(patchJson).getAsJsonObject();
            for (JsonElement e : arr(patch, "ops")) {
                if (!e.isJsonObject()) continue;
                JsonObject o = e.getAsJsonObject();
                String type = s(o, "type"), part = s(o, "part");
                switch (type) {
                    case "replace_part", "add_part" -> {
                        if (part.isBlank()) { r.error = "в правке нет названия компонента (part)"; return r; }
                        JsonArray ops = arr(bp, "ops");
                        if (type.equals("replace_part")) removePart(ops, part);
                        for (JsonElement n : arr(o, "ops")) if (n.isJsonObject()) { JsonObject c = n.getAsJsonObject().deepCopy(); c.addProperty("part", part); ops.add(c); }
                        r.changed.add(part);
                    }
                    case "move_part" -> {
                        int dx = o.has("dx") ? o.get("dx").getAsInt() : 0, dy = o.has("dy") ? o.get("dy").getAsInt() : 0, dz = o.has("dz") ? o.get("dz").getAsInt() : 0;
                        int moved = 0;
                        for (JsonElement n : arr(bp, "ops")) {
                            if (!n.isJsonObject() || !same(s(n.getAsJsonObject(), "part"), part)) continue;
                            JsonObject c = n.getAsJsonObject();
                            for (String k : new String[]{"x", "x1", "x2", "cx"}) if (c.has(k) && c.get(k).isJsonPrimitive()) c.addProperty(k, c.get(k).getAsInt() + dx);
                            for (String k : new String[]{"y", "y1", "y2", "cy"}) if (c.has(k) && c.get(k).isJsonPrimitive()) c.addProperty(k, c.get(k).getAsInt() + dy);
                            for (String k : new String[]{"z", "z1", "z2", "cz"}) if (c.has(k) && c.get(k).isJsonPrimitive()) c.addProperty(k, c.get(k).getAsInt() + dz);
                            moved++;
                        }
                        if (moved == 0) { r.error = "компонент «" + part + "» не найден. Части: " + String.join(", ", parts(spec)); return r; }
                        r.changed.add(part + " (сдвиг " + dx + "," + dy + "," + dz + ")");
                        if (arr(bp, "chains").size() + arr(bp, "buttons").size() + arr(bp, "setup").size() + arr(bp, "mechs").size() > 0)
                            r.warnings.add("команды механизмов, кнопки и сущности привязаны к координатам и НЕ сдвигались вместе с компонентом: проверь их или сдвинь проект целиком кнопками X/Y/Z");
                    }
                    case "rotate_part" -> {
                        List<JsonObject> mine = new ArrayList<>();
                        for (JsonElement n : arr(bp, "ops")) if (n.isJsonObject() && same(s(n.getAsJsonObject(), "part"), part)) mine.add(n.getAsJsonObject());
                        if (mine.isEmpty()) { r.error = "компонент «" + part + "» не найден"; return r; }
                        double mnx = 1e9, mxx = -1e9, mnz = 1e9, mxz = -1e9;
                        String[][] pairs = {{"x", "z"}, {"x1", "z1"}, {"x2", "z2"}, {"cx", "cz"}};
                        for (JsonObject c : mine) for (String[] pr : pairs) {
                            if (c.has(pr[0]) && c.get(pr[0]).isJsonPrimitive()) { double v = c.get(pr[0]).getAsDouble(); mnx = Math.min(mnx, v); mxx = Math.max(mxx, v); }
                            if (c.has(pr[1]) && c.get(pr[1]).isJsonPrimitive()) { double v = c.get(pr[1]).getAsDouble(); mnz = Math.min(mnz, v); mxz = Math.max(mxz, v); }
                        }
                        double ccx = (mnx + mxx) / 2.0, ccz = (mnz + mxz) / 2.0;
                        for (JsonObject c : mine) {
                            for (String[] pr : pairs)
                                if (c.has(pr[0]) && c.has(pr[1]) && c.get(pr[0]).isJsonPrimitive() && c.get(pr[1]).isJsonPrimitive()) {
                                    double x = c.get(pr[0]).getAsDouble(), z = c.get(pr[1]).getAsDouble();
                                    c.addProperty(pr[0], (int) Math.round(ccx - (z - ccz)));
                                    c.addProperty(pr[1], (int) Math.round(ccz + (x - ccx)));
                                }
                            if (c.has("block") && c.get("block").isJsonPrimitive()) {
                                String b = c.get("block").getAsString();
                                b = b.replace("facing=north", "facing=@E").replace("facing=east", "facing=@S").replace("facing=south", "facing=@W").replace("facing=west", "facing=@N")
                                        .replace("facing=@E", "facing=east").replace("facing=@S", "facing=south").replace("facing=@W", "facing=west").replace("facing=@N", "facing=north")
                                        .replace("axis=x", "axis=@z").replace("axis=z", "axis=x").replace("axis=@z", "axis=z");
                                c.addProperty("block", b);
                            }
                        }
                        r.changed.add(part + " (поворот 90°)");
                        r.warnings.add("поворот меняет положение блоков и направления (facing, axis); команды, кнопки и сущности механизмов не поворачивались");
                    }
                    case "copy_part" -> {
                        int dx = o.has("dx") ? o.get("dx").getAsInt() : 0, dy = o.has("dy") ? o.get("dy").getAsInt() : 0, dz = o.has("dz") ? o.get("dz").getAsInt() : 0;
                        String nn = s(o, "name").isBlank() ? part + " копия" : s(o, "name");
                        JsonArray ops = arr(bp, "ops");
                        List<JsonObject> add = new ArrayList<>();
                        for (JsonElement n : ops) if (n.isJsonObject() && same(s(n.getAsJsonObject(), "part"), part)) add.add(n.getAsJsonObject().deepCopy());
                        if (add.isEmpty()) { r.error = "компонент «" + part + "» не найден"; return r; }
                        for (JsonObject c : add) {
                            for (String k : new String[]{"x", "x1", "x2", "cx"}) if (c.has(k) && c.get(k).isJsonPrimitive()) c.addProperty(k, c.get(k).getAsInt() + dx);
                            for (String k : new String[]{"y", "y1", "y2", "cy"}) if (c.has(k) && c.get(k).isJsonPrimitive()) c.addProperty(k, c.get(k).getAsInt() + dy);
                            for (String k : new String[]{"z", "z1", "z2", "cz"}) if (c.has(k) && c.get(k).isJsonPrimitive()) c.addProperty(k, c.get(k).getAsInt() + dz);
                            c.addProperty("part", nn);
                            ops.add(c);
                        }
                        r.changed.add(nn + " (копия)");
                    }
                    case "set_cmd" -> {
                        String chain = s(o, "chain");
                        int idx = o.has("index") ? o.get("index").getAsInt() : 0;
                        JsonObject found = null;
                        for (JsonElement n : arr(bp, "chains")) if (n.isJsonObject() && same(s(n.getAsJsonObject(), "name"), chain)) found = n.getAsJsonObject();
                        if (found == null) { r.error = "команду можно править только в собственных цепочках проекта; «" + chain + "» — механизм из каталога или пульта"; return r; }
                        JsonArray cmds = arr(found, "commands");
                        if (idx < 0 || idx >= cmds.size()) { r.error = "в цепочке нет команды №" + (idx + 1); return r; }
                        String nc = s(o, "command").trim();
                        if (nc.startsWith("/")) nc = nc.substring(1);
                        if (cmds.get(idx).isJsonObject()) cmds.get(idx).getAsJsonObject().addProperty("cmd", nc);
                        else cmds.set(idx, new com.google.gson.JsonPrimitive(nc));
                        r.changed.add("команда №" + (idx + 1) + " цепочки «" + chain + "»");
                        r.warnings.add("команда сохранена как есть: если в ней записаны абсолютные координаты, при сдвиге проекта они не изменятся");
                    }
                    case "remove_part" -> { removePart(arr(bp, "ops"), part); r.changed.add(part + " (удалён)"); }
                    case "set_button" -> {
                        String label = s(o, "label");
                        JsonArray bs = arr(bp, "buttons");
                        removeBy(bs, "label", label);
                        JsonObject b = new JsonObject();
                        b.addProperty("label", label); b.addProperty("command", s(o, "command"));
                        bs.add(b);
                        r.changed.add("кнопка «" + label + "»");
                    }
                    case "remove_button" -> { removeBy(arr(bp, "buttons"), "label", s(o, "label")); r.changed.add("кнопка «" + s(o, "label") + "» (удалена)"); }
                    case "set_chain" -> {
                        String name = s(o, "name");
                        JsonArray cs = arr(bp, "chains");
                        removeBy(cs, "name", name);
                        JsonObject c = new JsonObject();
                        c.addProperty("name", name);
                        c.add("commands", o.has("commands") ? o.get("commands") : new JsonArray());
                        cs.add(c);
                        r.changed.add("логика «" + name + "»");
                    }
                    case "remove_chain" -> { removeBy(arr(bp, "chains"), "name", s(o, "name")); r.changed.add("логика «" + s(o, "name") + "» (удалена)"); }
                    case "set_mech" -> {
                        JsonArray ms = arr(bp, "mechs");
                        removeBy(ms, "key", s(o, "key"));
                        JsonObject m = o.deepCopy();
                        m.remove("type");
                        ms.add(m);
                        r.changed.add("механизм " + s(o, "key"));
                    }
                    case "add_setup" -> { for (JsonElement n : arr(o, "commands")) arr(bp, "setup").add(n); r.changed.add("начальные команды"); }
                    default -> { r.error = "неизвестный тип правки: " + type; return r; }
                }
            }
        } catch (Exception ex) {
            r.error = "правка не разобрана: " + ex.getClass().getSimpleName();
            return r;
        }
        if (r.changed.isEmpty()) { r.error = "правка пуста"; return r; }
        spec.bp = bp;
        return r;
    }

    private static void removePart(JsonArray ops, String part) {
        for (int i = ops.size() - 1; i >= 0; i--) {
            JsonElement e = ops.get(i);
            if (e.isJsonObject() && same(s(e.getAsJsonObject(), "part"), part)) ops.remove(i);
        }
    }

    private static void removeBy(JsonArray a, String key, String val) {
        for (int i = a.size() - 1; i >= 0; i--) {
            JsonElement e = a.get(i);
            if (e.isJsonObject() && same(s(e.getAsJsonObject(), key), val)) a.remove(i);
        }
    }

    /** Список компонентов (части чертежа) для контекста модели. */
    public static List<String> parts(NeroSpec spec) {
        List<String> r = new ArrayList<>();
        if (spec == null || spec.bp == null || !spec.bp.has("ops")) return r;
        for (JsonElement e : spec.bp.getAsJsonArray("ops")) {
            if (!e.isJsonObject()) continue;
            String p = s(e.getAsJsonObject(), "part");
            if (!p.isBlank() && !r.contains(p)) r.add(p);
        }
        return r;
    }

    private NeroPatch() { }
}
