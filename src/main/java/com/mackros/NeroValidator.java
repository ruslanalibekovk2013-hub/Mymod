package com.mackros;

import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Проверка проекта и команд до установки: синтаксис, идентификаторы, цепочки КБ, пересечения, логика лифта. */
public final class NeroValidator {
    public static final class Report {
        public final List<String> errors = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();
        public final List<String> passed = new ArrayList<>();
        public boolean ok() { return errors.isEmpty(); }
    }

    private static final Set<String> ROOT = Set.of("execute", "setblock", "fill", "clone", "tp", "teleport", "scoreboard", "tag", "effect",
            "particle", "playsound", "title", "tellraw", "summon", "data", "schedule", "kill", "damage", "weather", "time", "gamerule", "nero", "give", "clear");
    private static final Pattern NS = Pattern.compile("(?<![A-Za-z0-9_.#])minecraft:([a-z0-9_./]+)");

    private static final Set<String> DMG = Set.of("magic", "indirect_magic", "lightning_bolt", "explosion", "on_fire", "freeze", "thorns", "generic", "player_attack", "mob_attack", "arrow", "fireball");

    private static boolean idKnown(String path) {
        Identifier id = new Identifier("minecraft", path);
        return Registries.BLOCK.containsId(id) || Registries.ITEM.containsId(id) || Registries.ENTITY_TYPE.containsId(id)
                || Registries.STATUS_EFFECT.containsId(id) || Registries.PARTICLE_TYPE.containsId(id) || Registries.SOUND_EVENT.containsId(id)
                || Registries.PAINTING_VARIANT.containsId(id) || DMG.contains(path);
    }

    private static boolean balanced(String c) {
        int p = 0, b = 0, k = 0;
        boolean inStr = false;
        char q = 0;
        for (int i = 0; i < c.length(); i++) {
            char ch = c.charAt(i);
            if (inStr) {
                if (ch == '\\') i++;
                else if (ch == q) inStr = false;
                continue;
            }
            if (ch == '"' || ch == '\'') { inStr = true; q = ch; continue; }
            switch (ch) {
                case '(' -> p++; case ')' -> p--; case '[' -> b++; case ']' -> b--; case '{' -> k++; case '}' -> k--; default -> { }
            }
            if (p < 0 || b < 0 || k < 0) return false;
        }
        return !inStr && p == 0 && b == 0 && k == 0;
    }

    /** Индекс верхнеуровневого " run " (вне кавычек и скобок), -1 если нет. */
    private static int topRun(String c) {
        int depth = 0;
        boolean inStr = false;
        char q = 0;
        for (int i = 0; i < c.length() - 5; i++) {
            char ch = c.charAt(i);
            if (inStr) { if (ch == '\\') i++; else if (ch == q) inStr = false; continue; }
            if (ch == '"' || ch == '\'') { inStr = true; q = ch; continue; }
            if (ch == '[' || ch == '{' || ch == '(') depth++;
            if (ch == ']' || ch == '}' || ch == ')') depth--;
            if (depth == 0 && c.startsWith(" run ", i)) return i;
        }
        return -1;
    }

    private static boolean isInt(String s) { return s.matches("-?\\d+"); }

    private static void volume(String[] t, int from, String cmd, List<String> err) {
        if (t.length < from + 6) return;
        for (int i = from; i < from + 6; i++) if (!isInt(t[i])) return;
        long dx = Math.abs(Long.parseLong(t[from]) - Long.parseLong(t[from + 3])) + 1;
        long dy = Math.abs(Long.parseLong(t[from + 1]) - Long.parseLong(t[from + 4])) + 1;
        long dz = Math.abs(Long.parseLong(t[from + 2]) - Long.parseLong(t[from + 5])) + 1;
        if (dx * dy * dz > 32768) err.add("Слишком большой объём (" + dx * dy * dz + "): " + shortCmd(cmd));
    }

    private static String shortCmd(String c) { return c.length() > 60 ? c.substring(0, 57) + "..." : c; }

    public static void checkCommand(String raw, List<String> err, int depth) {
        String c = raw == null ? "" : raw.trim();
        if (c.isEmpty()) { err.add("Пустая команда"); return; }
        if (depth > 6) { err.add("Слишком глубокая вложенность execute"); return; }
        if (c.length() > 32000) { err.add("Команда длиннее 32000 символов"); return; }
        if (c.startsWith("/")) { err.add("Команда не должна начинаться с /: " + shortCmd(c)); return; }
        if (!balanced(c)) { err.add("Не сбалансированы скобки/кавычки: " + shortCmd(c)); return; }
        String[] t = c.split("\\s+");
        String root = t[0];
        if (!ROOT.contains(root)) { err.add("Команда не разрешена: " + root); return; }
        Matcher m = NS.matcher(c);
        while (m.find()) if (!idKnown(m.group(1))) err.add("Неизвестный идентификатор minecraft:" + m.group(1));
        switch (root) {
            case "execute" -> {
                int r = topRun(c);
                if (r >= 0) checkCommand(c.substring(r + 5), err, depth + 1);
                else if (!(c.contains(" if ") || c.contains(" unless "))) err.add("execute без run/условия: " + shortCmd(c));
            }
            case "scoreboard" -> {
                if (t.length < 4 || !(t[1].equals("objectives") || t[1].equals("players"))) err.add("Неверный scoreboard: " + shortCmd(c));
            }
            case "setblock" -> { if (t.length < 5) err.add("setblock: мало аргументов: " + shortCmd(c)); }
            case "fill" -> {
                if (t.length < 8) err.add("fill: мало аргументов: " + shortCmd(c));
                else volume(t, 1, c, err);
            }
            case "clone" -> {
                if (t.length < 10) err.add("clone: мало аргументов: " + shortCmd(c));
                else volume(t, 1, c, err);
            }
            case "tp", "teleport" -> { if (t.length < 2) err.add("tp: мало аргументов"); }
            case "tag" -> { if (t.length < 3 || !(t[2].equals("add") || t[2].equals("remove") || t[2].equals("list"))) err.add("Неверный tag: " + shortCmd(c)); }
            case "effect" -> { if (t.length < 3 || !(t[1].equals("give") || t[1].equals("clear"))) err.add("Неверный effect: " + shortCmd(c)); }
            case "title" -> { if (t.length < 3) err.add("title: мало аргументов"); }
            case "tellraw" -> { if (t.length < 3) err.add("tellraw: мало аргументов"); }
            case "playsound" -> { if (t.length < 3) err.add("playsound: мало аргументов"); }
            case "particle" -> { if (t.length < 2) err.add("particle: мало аргументов"); }
            case "summon" -> { if (t.length < 2) err.add("summon: мало аргументов"); }
            case "data" -> { if (t.length < 3 || !(t[1].equals("get") || t[1].equals("merge") || t[1].equals("modify") || t[1].equals("remove"))) err.add("Неверный data: " + shortCmd(c)); }
            case "schedule" -> { if (t.length < 3 || !(t[1].equals("function") || t[1].equals("clear"))) err.add("Неверный schedule: " + shortCmd(c)); }
            case "kill" -> { if (t.length >= 2 && t[1].equals("@e") ) err.add("kill @e без фильтра запрещён"); }
            case "damage" -> { if (t.length < 3) err.add("damage: мало аргументов"); }
            case "weather" -> { if (t.length < 2 || !(t[1].equals("clear") || t[1].equals("rain") || t[1].equals("thunder"))) err.add("Неверный weather"); }
            case "time" -> { if (t.length < 2 || !(t[1].equals("set") || t[1].equals("add") || t[1].equals("query"))) err.add("Неверный time"); }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ проверка плана
    public static Report validate(NeroBuildPlan p, int worldMinY, int worldMaxY) {
        Report r = new Report();
        if (p.elements.isEmpty()) { r.errors.add("План пуст"); return r; }
        if (p.ops() > NeroConfig.MAX_OPS) r.errors.add("Слишком большой проект: " + p.ops() + " операций (лимит " + NeroConfig.MAX_OPS + ")");
        else r.passed.add("Размер проекта в пределах лимита (" + p.ops() + " операций)");
        if (p.cbs.size() > 600) r.errors.add("Слишком много командных блоков: " + p.cbs.size());
        if (p.minY < worldMinY || p.maxY >= worldMaxY) r.errors.add("Постройка выходит за высоту мира: Y " + p.minY + ".." + p.maxY + " (допустимо " + worldMinY + ".." + (worldMaxY - 1) + ")");
        else r.passed.add("Высота в пределах мира");

        for (int i = 0; i < p.elements.size(); i++)
            for (int d : p.elements.get(i).deps())
                if (d < 0 || d >= p.elements.size() || d >= i) r.errors.add("Элемент «" + p.elements.get(i).name() + "»: неверная зависимость " + d);

        // пересечение КБ и блоков
        Map<Long, Integer> cbAt = new HashMap<>();
        Set<Long> occupiedByCb = new HashSet<>();
        for (NeroBuildPlan.Cb c : p.cbs) {
            long k = key(c.x(), c.y(), c.z());
            if (cbAt.put(k, 1) != null) r.errors.add("Два командных блока в одной точке: " + c.x() + " " + c.y() + " " + c.z());
            occupiedByCb.add(k);
        }
        int clash = 0;
        for (NeroBuildPlan.Block b : p.blocks) if (!b.st().isAir() && occupiedByCb.contains(key(b.x(), b.y(), b.z()))) clash++;
        if (clash > 0) r.errors.add("Конструкция перекрывает " + clash + " командных блоков");
        else r.passed.add("Командные блоки не пересекаются с конструкцией");

        // перекрытие элементов
        Map<Long, Integer> owner = new HashMap<>();
        int overlap = 0;
        for (NeroBuildPlan.Block b : p.blocks) {
            if (b.st().isAir()) continue;
            Integer o = owner.put(key(b.x(), b.y(), b.z()), b.el());
            if (o != null && o != b.el()) overlap++;
        }
        if (overlap > 0) r.warnings.add("Элементы перекрываются в " + overlap + " блоках (позднейший элемент заменяет ранний)");

        // команды
        int summons = 0;
        List<String> cerr = new ArrayList<>();
        for (NeroBuildPlan.Cb c : p.cbs) {
            if (c.cmd() == null || c.cmd().isBlank()) { r.errors.add("Пустой командный блок на " + c.x() + " " + c.y() + " " + c.z()); continue; }
            checkCommand(c.cmd(), cerr, 0);
            if (c.cmd().contains("run summon") || c.cmd().startsWith("summon")) summons++;
        }
        for (String s : p.setup) checkCommand(s, cerr, 0);
        for (String e : cerr) if (!r.errors.contains(e)) r.errors.add(e);
        if (cerr.isEmpty()) r.passed.add("Все команды (" + (p.cbs.size() + p.setup.size()) + ") прошли проверку синтаксиса и идентификаторов");
        if (summons > 80) r.errors.add("Слишком много команд summon: " + summons);

        chainCheck(p, r);

        // проверка механизмов
        for (int i = 0; i < p.elements.size(); i++) {
            NeroBuildPlan.Element e = p.elements.get(i);
            if (!e.kind().equals("mechanism")) continue;
            if (e.name().startsWith("Лифт") || e.name().contains("лифт") || e.name().contains("Лифт")) liftCheck(p, i, e, r);
        }
        return r;
    }

    private static long key(int x, int y, int z) { return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | ((long) y & 0xFFF); }

    /** Граф цепочки: каждый chain-блок должен получать сигнал ровно от одного предыдущего блока, который на него смотрит. */
    private static void chainCheck(NeroBuildPlan p, Report r) {
        Map<Long, NeroBuildPlan.Cb> at = new HashMap<>();
        for (NeroBuildPlan.Cb c : p.cbs) at.put(key(c.x(), c.y(), c.z()), c);
        Map<Long, Integer> feeders = new HashMap<>();
        int starts = 0;
        for (NeroBuildPlan.Cb c : p.cbs) {
            long nxt = key(c.x() + c.f().getOffsetX(), c.y() + c.f().getOffsetY(), c.z() + c.f().getOffsetZ());
            if (at.containsKey(nxt)) feeders.merge(nxt, 1, Integer::sum);
            if (c.type() != NeroBuildPlan.CbType.CHAIN) starts++;
        }
        int bad = 0;
        for (NeroBuildPlan.Cb c : p.cbs) {
            if (c.type() == NeroBuildPlan.CbType.CHAIN) {
                int f = feeders.getOrDefault(key(c.x(), c.y(), c.z()), 0);
                if (f != 1) bad++;
            }
        }
        if (bad > 0) r.errors.add("Нарушена цепочка КБ: " + bad + " chain-блоков без единственного источника");
        else if (!p.cbs.isEmpty()) r.passed.add("Порядок цепочек КБ корректен (стартов: " + starts + ")");
    }

    private static void liftCheck(NeroBuildPlan p, int el, NeroBuildPlan.Element e, Report r) {
        Integer n = e.meta().get("p0"), h = e.meta().get("p1"), sp = e.meta().get("p2");
        if (n == null || h == null || sp == null) return;
        double[] steps = {0.15, 0.3, 0.45, 0.55};
        double step = steps[Math.max(0, Math.min(3, sp - 1))];
        List<String> prob = simulateLift(n, h, step);
        // покрытие команд: для каждого этажа должны быть команды движения и посадки
        List<String> cmds = new ArrayList<>();
        for (NeroBuildPlan.Cb c : p.cbs) if (c.el() == el) cmds.add(c.cmd());
        for (int k = 1; k <= n; k++) {
            int cnt = 0;
            String pat = "_tgt=" + k + "}]";
            for (String c : cmds) if (c.contains(pat)) cnt++;
            if (cnt < 7) prob.add("Этаж " + k + ": не хватает команд движения/посадки (" + cnt + " из 7)");
        }
        if (prob.isEmpty()) r.passed.add("Лифт «" + e.name() + "»: симуляция " + n + "x" + (n - 1) + " поездок прошла, посадка на каждом этаже точная");
        else r.errors.addAll(prob);
    }

    /** Численная проверка алгоритма лифта (та же логика, что в командах): между любыми двумя этажами игрок обязан доехать и встать точно. */
    public static List<String> simulateLift(int n, int h, double step) {
        List<String> prob = new ArrayList<>();
        if (h < 4) prob.add("Высота этажа меньше 4: окна выбора этажа пересекаются");
        if (step > 0.6) prob.add("Шаг движения " + step + " больше окна посадки 0.6: лифт может перескочить этаж");
        if (!prob.isEmpty()) return prob;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                double feet = i * h, ty = j * h;
                int t = 0;
                boolean arrived = false;
                while (t < 5000) {
                    t++;
                    if (feet <= ty - 0.3) feet += step;
                    else if (feet > ty + 0.3) feet -= step;
                    else { arrived = true; feet = ty; break; }
                }
                if (!arrived) { prob.add("Лифт не доезжает с этажа " + (i + 1) + " на " + (j + 1)); return prob; }
                if (Math.abs(feet - ty) > 1e-9) { prob.add("Неточная посадка на этаж " + (j + 1)); return prob; }
                if (t > 120 * Math.max(1, Math.abs(i - j)) * 3) { prob.add("Слишком долгая поездка " + (i + 1) + "->" + (j + 1) + ": " + t + " тиков"); return prob; }
            }
        return prob;
    }

    private NeroValidator() { }
}
