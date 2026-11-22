package com.mackros;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Общий путь применения правки к открытому проекту (из чата и из команд): проверка до применения, undo, автосохранение, честный отчёт. */
public final class NeroEditor {
    private static Map<String, String> partHashes(NeroSpec s) {
        Map<String, String> m = new LinkedHashMap<>();
        if (s == null || s.bp == null || !s.bp.has("ops")) return m;
        Map<String, StringBuilder> b = new LinkedHashMap<>();
        for (com.google.gson.JsonElement e : s.bp.getAsJsonArray("ops")) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            String p = o.has("part") ? o.get("part").getAsString() : "Конструкция";
            b.computeIfAbsent(p, k -> new StringBuilder()).append(o);
        }
        for (Map.Entry<String, StringBuilder> e : b.entrySet()) m.put(e.getKey(), Integer.toHexString(e.getValue().toString().hashCode()));
        return m;
    }

    /** @return сообщение для пользователя (успех или причина отказа). Проект меняется только при успешной проверке. */
    public static String apply(String patchJson, String note) {
        NeroSpec before = NeroAI.project;
        if (before == null) return "Проект не открыт.";
        long t1 = System.nanoTime();
        NeroSpec np = before.copy();
        NeroPatch.Result res = NeroPatch.apply(np, patchJson);
        if (res.error != null) return "Правка не применена: " + res.error;
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            NeroBuildPlan p2 = NeroPlanner.plan(np, NeroScreen.base());
            NeroValidator.Report r2 = NeroValidator.validate(p2, mc.world != null ? mc.world.getBottomY() : -64, mc.world != null ? mc.world.getTopY() : 320);
            if (!r2.ok()) return "Правка отклонена проверкой, проект не изменён: " + String.join("; ", r2.errors.subList(0, Math.min(3, r2.errors.size())));
        } catch (Exception e) {
            return "Правка отклонена: не удалось пересчитать проект (" + e.getClass().getSimpleName() + ")";
        }
        Map<String, String> h0 = partHashes(before), h1 = partHashes(np);
        List<String> changed = new ArrayList<>(), same = new ArrayList<>();
        for (Map.Entry<String, String> e : h1.entrySet()) { if (!e.getValue().equals(h0.get(e.getKey()))) changed.add(e.getKey()); else same.add(e.getKey()); }
        for (String k : h0.keySet()) if (!h1.containsKey(k)) changed.add(k + " (удалён)");
        NeroAI.pushUndo(before);
        NeroAI.project = np;
        NeroScreen.replan(false);
        NeroRequirements.trace(np.reqs, NeroScreen.plan(), NeroScreen.report());
        NeroProjects.save(np, note + ": " + String.join(", ", res.changed));
        NeroPerf.put("Применение правки (локально)", (System.nanoTime() - t1) / 1_000_000L);
        StringBuilder sb = new StringBuilder("Правка применена. Изменены компоненты: " + (changed.isEmpty() ? String.join(", ", res.changed) : String.join(", ", changed))
                + (same.isEmpty() ? "" : ". Без изменений: " + String.join(", ", same)) + ". Пересчитано локально без запроса к модели (" + NeroPerf.summary() + "). Отмена: команда «отмени».");
        for (String w : res.warnings) sb.append("\nВНИМАНИЕ: ").append(w);
        return sb.toString();
    }

    public static String movePart(String part, int dx, int dy, int dz) {
        JsonObject p = new JsonObject(), o = new JsonObject();
        JsonArray ops = new JsonArray();
        o.addProperty("type", "move_part");
        o.addProperty("part", part);
        o.addProperty("dx", dx); o.addProperty("dy", dy); o.addProperty("dz", dz);
        ops.add(o);
        p.add("ops", ops);
        return apply(p.toString(), "Сдвиг части");
    }

    public static String rotatePart(String part) {
        return simple("rotate_part", part, "Поворот части");
    }

    public static String dupPart(String part, int dx, int dy, int dz) {
        JsonObject p = new JsonObject(), o = new JsonObject();
        JsonArray ops = new JsonArray();
        o.addProperty("type", "copy_part");
        o.addProperty("part", part);
        o.addProperty("dx", dx); o.addProperty("dy", dy); o.addProperty("dz", dz);
        ops.add(o);
        p.add("ops", ops);
        return apply(p.toString(), "Копия части");
    }

    public static String setCmd(String chain, int index, String command) {
        JsonObject p = new JsonObject(), o = new JsonObject();
        JsonArray ops = new JsonArray();
        o.addProperty("type", "set_cmd");
        o.addProperty("chain", chain);
        o.addProperty("index", index);
        o.addProperty("command", command);
        ops.add(o);
        p.add("ops", ops);
        return apply(p.toString(), "Правка команды");
    }

    private static String simple(String type, String part, String note) {
        JsonObject p = new JsonObject(), o = new JsonObject();
        JsonArray ops = new JsonArray();
        o.addProperty("type", type);
        o.addProperty("part", part);
        ops.add(o);
        p.add("ops", ops);
        return apply(p.toString(), note);
    }

    public static String removePart(String part) {
        JsonObject p = new JsonObject(), o = new JsonObject();
        JsonArray ops = new JsonArray();
        o.addProperty("type", "remove_part");
        o.addProperty("part", part);
        ops.add(o);
        p.add("ops", ops);
        return apply(p.toString(), "Удаление части");
    }

    public static String insertModel(String name, int dx, int dy, int dz) {
        if (NeroModels.get(name) == null) return "Модели «" + name + "» нет в библиотеке. Список: «библиотека моделей».";
        JsonObject p = new JsonObject(), o = new JsonObject(), op = new JsonObject();
        JsonArray ops = new JsonArray(), inner = new JsonArray();
        op.addProperty("op", "model"); op.addProperty("name", name); op.addProperty("x", dx); op.addProperty("y", dy); op.addProperty("z", dz);
        inner.add(op);
        o.addProperty("type", "add_part");
        o.addProperty("part", "Модель: " + name);
        o.add("ops", inner);
        ops.add(o);
        p.add("ops", ops);
        String r = apply(p.toString(), "Вставка модели");
        if (r.startsWith("Правка применена") && NeroAI.project != null) NeroModels.markUsed(name, NeroAI.project.title());
        return r;
    }

    private NeroEditor() { }
}
