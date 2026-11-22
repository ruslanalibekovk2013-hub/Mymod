package com.mackros;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.EmptyBlockView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mackros.NeroSpec.clamp;

/**
 * Свободная постройка: (1) интерпретатор «чертежа» от внешнего ИИ (примитивы + готовые механизмы Rig + свои цепочки КБ + кнопки),
 * (2) локальный композитор (без интернета собирает здание + любые механизмы по ключевым словам),
 * (3) 3D-принтер, (4) стена управления и командный отсек рядом с постройкой.
 */
public final class NeroFree {
    private static final Set<String> BANNED = Set.of("minecraft:command_block", "minecraft:chain_command_block", "minecraft:repeating_command_block",
            "minecraft:structure_block", "minecraft:jigsaw", "minecraft:spawner", "minecraft:barrier");
    private static final Pattern PH = Pattern.compile("\\{([xyz])([+-]\\d+)?\\}|\\{id\\}|\\{tag\\}");

    public static boolean has(String t, String re) { return Pattern.compile(re).matcher(t).find(); }

    // ------------------------------------------------------------------ разбор блоков
    private static <T extends Comparable<T>> BlockState with(BlockState s, Property<T> p, String v) {
        return p.parse(v).map(x -> s.with(p, x)).orElse(s);
    }

    public static BlockState state(String s) {
        if (s == null) return null;
        s = s.trim();
        String props = null;
        int b = s.indexOf('[');
        if (b > 0 && s.endsWith("]")) { props = s.substring(b + 1, s.length() - 1); s = s.substring(0, b); }
        if (!s.contains(":")) s = "minecraft:" + s;
        if (BANNED.contains(s)) return null;
        Identifier id = Identifier.tryParse(s);
        if (id == null || !Registries.BLOCK.containsId(id)) return null;
        BlockState st = Registries.BLOCK.get(id).getDefaultState();
        if (props != null)
            for (String kv : props.split(",")) {
                String[] p = kv.split("=", 2);
                if (p.length < 2) continue;
                Property<?> pr = st.getBlock().getStateManager().getProperty(p[0].trim());
                if (pr != null) st = with(st, pr, p[1].trim());
            }
        return st;
    }

    private static int gi(JsonObject o, String k, int d, int lo, int hi) {
        try { if (o.has(k) && !o.get(k).isJsonNull()) return clamp((int) Math.round(o.get(k).getAsDouble()), lo, hi); } catch (Exception ignored) { }
        return d;
    }

    private static String gs(JsonObject o, String k) {
        try { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null; } catch (Exception e) { return null; }
    }

    private static JsonArray arr(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonArray() ? o.getAsJsonArray(k) : new JsonArray();
    }

    static String sub(String s, BlockPos b, String id, String tag) {
        Matcher m = PH.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String r;
            if (m.group().equals("{id}")) r = id;
            else if (m.group().equals("{tag}")) r = tag;
            else {
                int off = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
                int base = switch (m.group(1)) { case "x" -> b.getX(); case "y" -> b.getY(); default -> b.getZ(); };
                r = String.valueOf(base + off);
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(r));
        }
        m.appendTail(sb);
        return sb.toString().trim();
    }

    private static String esc(String s) {
        String r = s == null ? "" : s.replace("\\", "/").replace("\"", "'").replace("\n", " ");
        return r.length() > 200 ? r.substring(0, 200) : r;
    }

    private static String fd(double v) { return String.format(Locale.ROOT, "%.1f", v); }

    // ------------------------------------------------------------------ сетка чертежа
    private record Cell(int x, int y, int z, BlockState s, String part) { }

    private static final class Grid {
        final LinkedHashMap<Long, Cell> m = new LinkedHashMap<>();
        String curPart = "Конструкция";
        int ox = 0, oy = 0, oz = 0;

        void put(int x, int y, int z, BlockState s) {
            if (m.size() > 45000 || Math.abs(x) > 200 || Math.abs(z) > 200 || y < -64 || y > 300) return;
            x += ox; y += oy; z += oz;
            long k = (((long) (x + 512)) << 22) | (((long) (y + 512)) << 11) | (z + 512);
            m.put(k, new Cell(x, y, z, s, curPart));
        }

        /** Прозрачные/неполные блоки (двери, лестницы, факелы, кнопки) ставятся после полных, чтобы не отваливались. */
        int emit(NeroPlanner.Ctx c) {
            java.util.Map<String, List<Cell>> byPart = new LinkedHashMap<>();
            for (Cell k : m.values()) byPart.computeIfAbsent(k.part(), z -> new ArrayList<>()).add(k);
            int first = -1;
            for (Map.Entry<String, List<Cell>> e : byPart.entrySet()) {
                int el = c.begin(e.getKey(), "structure", "-", "Часть конструкции: " + e.getKey());
                if (first < 0) first = el;
                for (Cell k : e.getValue()) {
                    boolean full = k.s().isAir() || k.s().isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
                    c.put(k.x(), k.y(), k.z(), k.s(), full ? 1 : 2);
                }
            }
            return first;
        }
    }

    private static void op(Grid g, JsonObject o) {
        String t = gs(o, "op");
        t = t == null ? "box" : t.toLowerCase(Locale.ROOT);
        BlockState st = state(gs(o, "block"));
        if (st == null && !"model".equals(t)) return;
        String pn = gs(o, "part");
        g.curPart = pn == null || pn.isBlank() ? "Конструкция" : (pn.length() > 40 ? pn.substring(0, 40) : pn);
        switch (t) {
            case "model" -> {
                String mn = gs(o, "name");
                JsonObject mo = mn == null ? null : NeroModels.get(mn);
                if (mo == null || !mo.has("ops")) return;
                String keep = g.curPart;
                int sx = g.ox, sy = g.oy, sz = g.oz;
                g.ox += gi(o, "x", 0, -200, 200); g.oy += gi(o, "y", 0, -64, 300); g.oz += gi(o, "z", 0, -200, 200);
                for (JsonElement me : mo.getAsJsonArray("ops")) {
                    if (!me.isJsonObject() || "model".equals(gs(me.getAsJsonObject(), "op"))) continue;
                    JsonObject c = me.getAsJsonObject().deepCopy();
                    c.addProperty("part", keep);
                    op(g, c);
                }
                g.ox = sx; g.oy = sy; g.oz = sz;
                g.curPart = keep;
            }
            case "set" -> g.put(gi(o, "x", 0, -200, 200), gi(o, "y", 0, -64, 300), gi(o, "z", 0, -200, 200), st);
            case "box", "fill", "walls", "hollow" -> {
                int x1 = gi(o, "x1", 0, -200, 200), x2 = gi(o, "x2", x1, -200, 200);
                int y1 = gi(o, "y1", 0, -64, 300), y2 = gi(o, "y2", y1, -64, 300);
                int z1 = gi(o, "z1", 0, -200, 200), z2 = gi(o, "z2", z1, -200, 200);
                int ax = Math.min(x1, x2), bx = Math.max(x1, x2), ay = Math.min(y1, y2), by = Math.max(y1, y2), az = Math.min(z1, z2), bz = Math.max(z1, z2);
                if ((long) (bx - ax + 1) * (by - ay + 1) * (bz - az + 1) > 60000) return;
                String mode = gs(o, "mode");
                if (mode == null) mode = t;
                boolean hollow = mode.equals("hollow"), walls = mode.equals("walls");
                for (int y = ay; y <= by; y++)
                    for (int x = ax; x <= bx; x++)
                        for (int z = az; z <= bz; z++) {
                            boolean ex = x == ax || x == bx, ey = y == ay || y == by, ez = z == az || z == bz;
                            if (hollow && !(ex || ey || ez)) continue;
                            if (walls && !(ex || ez)) continue;
                            g.put(x, y, z, st);
                        }
            }
            case "cylinder", "cyl" -> {
                int cx = gi(o, "cx", 0, -200, 200), cz = gi(o, "cz", 0, -200, 200), r = gi(o, "r", 3, 1, 40);
                int y1 = gi(o, "y1", 0, -64, 300), y2 = gi(o, "y2", y1, -64, 300);
                boolean hollow = o.has("hollow") && o.get("hollow").getAsBoolean();
                for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                    for (int dx = -r; dx <= r; dx++)
                        for (int dz = -r; dz <= r; dz++) {
                            int d2 = dx * dx + dz * dz;
                            if (d2 > r * r + r) continue;
                            boolean edge = d2 > (r - 1) * (r - 1) + (r - 1);
                            if (hollow && !edge && y != Math.min(y1, y2) && y != Math.max(y1, y2)) continue;
                            g.put(cx + dx, y, cz + dz, st);
                        }
            }
            case "sphere" -> {
                int cx = gi(o, "cx", 0, -200, 200), cy = gi(o, "cy", 0, -64, 300), cz = gi(o, "cz", 0, -200, 200), r = gi(o, "r", 3, 1, 30);
                boolean hollow = o.has("hollow") && o.get("hollow").getAsBoolean();
                for (int dx = -r; dx <= r; dx++)
                    for (int dy = -r; dy <= r; dy++)
                        for (int dz = -r; dz <= r; dz++) {
                            int d2 = dx * dx + dy * dy + dz * dz;
                            if (d2 > r * r + r) continue;
                            if (hollow && d2 <= (r - 1) * (r - 1) + (r - 1)) continue;
                            g.put(cx + dx, cy + dy, cz + dz, st);
                        }
            }
            case "line" -> {
                int x1 = gi(o, "x1", 0, -200, 200), y1 = gi(o, "y1", 0, -64, 300), z1 = gi(o, "z1", 0, -200, 200);
                int x2 = gi(o, "x2", x1, -200, 200), y2 = gi(o, "y2", y1, -64, 300), z2 = gi(o, "z2", z1, -200, 200);
                int n = Math.max(Math.abs(x2 - x1), Math.max(Math.abs(y2 - y1), Math.abs(z2 - z1)));
                for (int i = 0; i <= n; i++) {
                    double f = n == 0 ? 0 : (double) i / n;
                    g.put((int) Math.round(x1 + (x2 - x1) * f), (int) Math.round(y1 + (y2 - y1) * f), (int) Math.round(z1 + (z2 - z1) * f), st);
                }
            }
            case "pyramid" -> {
                int x1 = gi(o, "x1", 0, -200, 200), x2 = gi(o, "x2", x1, -200, 200), z1 = gi(o, "z1", 0, -200, 200), z2 = gi(o, "z2", z1, -200, 200), y = gi(o, "y", 0, -64, 300);
                int ax = Math.min(x1, x2), bx = Math.max(x1, x2), az = Math.min(z1, z2), bz = Math.max(z1, z2);
                for (int k = 0; ax + k <= bx - k && az + k <= bz - k; k++)
                    for (int x = ax + k; x <= bx - k; x++)
                        for (int z = az + k; z <= bz - k; z++) g.put(x, y + k, z, st);
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ чертёж ИИ
    public static void custom(NeroPlanner.Ctx c, NeroSpec s) {
        if (s.bp != null) blueprint(c, s.bp); else compose(c, s);
    }

    static void blueprint(NeroPlanner.Ctx c, JsonObject bp) { blueprint(c, bp, BlockPos.ORIGIN); }

    /** Собирает чертёж со смещением off (для встраивания части проекта в другой). */
    static void blueprint(NeroPlanner.Ctx c, JsonObject bp, BlockPos off) {
        String bid = c.nextId();
        BlockPos sb = c.base.add(off);
        String tag = c.pl.entityTag;
        Grid g = new Grid();
        g.ox = off.getX(); g.oy = off.getY(); g.oz = off.getZ();
        int n = 0;
        for (JsonElement e : arr(bp, "ops")) { if (++n > 600) break; if (e.isJsonObject()) op(g, e.getAsJsonObject()); }
        int main = g.emit(c);
        if (main < 0) main = c.begin("Чертёж: конструкция", "structure", "-", "Блоки по чертежу");
        n = 0;
        for (JsonElement e : arr(bp, "mechs")) {
            if (++n > 12 || !e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            String key = gs(o, "key");
            Rig.Def d = null;
            for (Rig.Def x : Rig.ALL) if (x.key().equals(key)) d = x;
            if (d == null) continue;
            JsonArray pa = arr(o, "params");
            int[] p = new int[pa.size()];
            for (int i = 0; i < p.length; i++) try { p[i] = pa.get(i).getAsInt(); } catch (Exception ex) { p[i] = d.df()[Math.min(i, d.df().length - 1)]; }
            String nm = gs(o, "name");
            NeroPlanner.rig(c, key, gi(o, "x", 0, -200, 200) + off.getX(), gi(o, "y", 0, -64, 300) + off.getY(), gi(o, "z", 0, -200, 200) + off.getZ(), nm == null ? d.name() : nm,
                    "Кнопки на пульте управления", d.desc(), p, main);
        }
        n = 0;
        int total = 0;
        for (JsonElement e : arr(bp, "chains")) {
            if (++n > 12 || !e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            List<String> cmds = new ArrayList<>();
            List<String[]> lr = new ArrayList<>();
            for (JsonElement ce : arr(o, "commands")) {
                if (cmds.size() >= 150 || ++total > 700) break;
                String raw, label = null, role = null;
                try {
                    if (ce.isJsonObject()) { raw = gs(ce.getAsJsonObject(), "cmd"); label = gs(ce.getAsJsonObject(), "label"); role = gs(ce.getAsJsonObject(), "role"); }
                    else raw = ce.getAsString();
                } catch (Exception ex) { continue; }
                if (raw == null) continue;
                String cm = sub(raw, sb, bid, tag);
                if (cm.startsWith("/")) cm = cm.substring(1);
                if (cm.isBlank()) continue;
                cmds.add(cm);
                lr.add(new String[]{label == null ? "" : esc(label), role == null ? "" : esc(role)});
            }
            if (cmds.isEmpty()) continue;
            String nm = gs(o, "name");
            int el = c.begin(nm == null ? "Логика" : nm, "mechanism", "Командные блоки", "Логика из чертежа", main);
            c.chain(cmds, el);
            int first = c.pl.cbs.size() - cmds.size();
            for (int i = 0; i < cmds.size(); i++)
                if (!lr.get(i)[0].isEmpty()) c.pl.cbs.set(first + i, c.pl.cbs.get(first + i).named(lr.get(i)[0], lr.get(i)[1]));
        }
        n = 0;
        for (JsonElement e : arr(bp, "setup")) {
            if (++n > 150) break;
            String cm = sub(e.getAsString(), sb, bid, tag);
            if (cm.startsWith("/")) cm = cm.substring(1);
            if (!cm.isBlank()) c.pl.setup.add(cm);
        }
        n = 0;
        for (JsonElement e : arr(bp, "buttons")) {
            if (++n > 20 || !e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            String label = gs(o, "label"), cmd = gs(o, "command");
            if (label == null || cmd == null) continue;
            List<String> row = new ArrayList<>();
            row.add(esc(label));
            for (String part : sub(cmd, sb, bid, tag).split("\n")) { String q = part.trim(); if (q.startsWith("/")) q = q.substring(1); if (!q.isEmpty() && row.size() < 5) row.add(q); }
            if (row.size() > 1) c.pl.controls.add(row.toArray(new String[0]));
        }
        String help = gs(bp, "help");
        if (help != null) c.pl.help.add(esc(help));
    }

    // ------------------------------------------------------------------ локальный композитор
    public static List<String> features(String t) {
        List<String> f = new ArrayList<>();
        String[][] x = {{"лифт|подъемник", "лифт"}, {"принтер|3d|3д", "3D-принтер"}, {"ворот|шлагбаум", "автоворота"}, {"гараж", "гараж"}, {"бассейн", "бассейн"},
                {"сигнализ", "сигнализация"}, {"ферм|огород|фабрик|завод|производств", "автоферма"}, {"турел", "турель"}, {"купол", "купол"}, {"телепорт", "телепорты"}, {"сейф|кодов\\w* замок|код", "кодовый замок"},
                {"конвейер|эскалатор|траволатор|фабрик|завод|производств", "конвейер"}, {"шоу|фейерверк", "световое шоу"}, {"погод|метео", "метеопанель"}, {"арен|волн\\w* мобов", "арена волн"},
                {"бур\\b|автобур|шахтер|добыч", "автобур"}, {"фонар|освещ", "умные фонари"}, {"точк\\w* дом|систем\\w* дом", "точки дома"}};
        for (String[] a : x) if (has(t, a[0])) f.add(a[1]);
        return f;
    }

    public static boolean wantsBuilding(String t) {
        return has(t, "фабрик|завод|производств|баз[аыуе]|комплекс|штаб|лаборатор|центр|резиденц|дом|здани|башн|небоскр|вилл|особняк|коттедж|отел|офис|убежищ|ангар|склад");
    }

    private static void compose(NeroPlanner.Ctx c, NeroSpec s) {
        String t = LocalProvider.norm((s.desc == null || s.desc.isEmpty()) ? s.name : s.desc);
        String arch = NeroArchetypes.detect(t);
        if (arch != null && !arch.equals("printer")) { NeroArchetypes.build(c, arch, t); return; }
        boolean big = has(t, "фабрик|завод|производств|баз[аыуе]|комплекс|штаб|лаборатор|центр|резиденц");
        boolean bld = big || wantsBuilding(t);
        boolean wLift = has(t, "лифт|подъемник"), wPrint = has(t, "принтер|3d|3д"), wGate = has(t, "ворот|шлагбаум"), wGarage = has(t, "гараж"),
                wPool = has(t, "бассейн"), wAlarm = has(t, "сигнализ"), wFarm = has(t, "ферм|огород|фабрик|завод|производств"), wSentry = has(t, "турел"), wDome = has(t, "купол"),
                wTele = has(t, "телепорт"), wVault = has(t, "сейф|кодов\\w* замок|код"), wBelt = has(t, "конвейер|эскалатор|траволатор|фабрик|завод|производств"),
                wShow = has(t, "шоу|фейерверк"), wWeather = has(t, "погод|метео"), wWave = has(t, "арен|волн\\w* мобов"),
                wMiner = has(t, "бур\\b|автобур|шахтер|добыч|фабрик|завод|производств"), wLamps = has(t, "фонар|освещ"), wHomes = has(t, "точк\\w* дом|систем\\w* дом");
        boolean any = wLift || wPrint || wGate || wGarage || wPool || wAlarm || wFarm || wSentry || wDome || wTele || wVault || wBelt || wShow || wWeather || wWave || wMiner || wLamps || wHomes;
        if (!bld && !any) { NeroPlanner.house(c, s); return; }
        boolean fut = "futuristic".equals(s.style) || has(t, "футурист|будущ|космич|принтер|лаборатор|хай");
        String style = s.style != null ? s.style : (fut ? "futuristic" : "modern");
        NeroPlanner.Pal p = NeroPlanner.pal(style);
        int W = 0, D = 0, F = 1, fh = 5, main = -1;
        int speed = has(t, "быстр|продвин|скоростн|супер|самы") ? 3 : 2;
        if (bld) {
            W = s.w > 0 ? clamp(s.w, 9, 41) : (big ? 27 : 15);
            D = s.d > 0 ? clamp(s.d, 9, 41) : (big ? 21 : 13);
            F = s.floors > 0 ? clamp(s.floors, 1, 14) : (big ? 3 : (wLift ? 3 : 2));
            main = NeroPlanner.shell(c, big ? "База: корпус" : "Здание: корпус", 0, 0, W, D, F, fh, p, "futuristic".equals(style), true, "medieval".equals(style));
            c.open(W / 2 - 1, 0, 0, W / 2, 2, 0);
            boolean lift = (wLift || big) && F >= 2;
            if (F >= 2 && !lift) {
                c.begin("Лестница", "structure", "-", "Лестница-стремянка между этажами", main);
                for (int f = 0; f < F; f++) {
                    if (f > 0) c.det(1, f * fh - 1, D - 2, Blocks.AIR);
                    c.ladder(1, D - 2, f * fh, f * fh + fh - 1 + (f == F - 1 ? -1 : 0), Direction.NORTH);
                }
            }
            c.begin("Мебель", "decor", "-", "Кровати, кухня, книги", main);
            NeroPlanner.furnish(c, 0, 0, W, D, F, fh, lift, 0, 0, 0, 0);
            if (lift) NeroPlanner.rig(c, "lift", W - 6, 0, D - 6, "Лифт", "Кнопки этажей на пульте, прыжок/присед в шахте", "Плавное перемещение между этажами", new int[]{F, fh, speed}, main);
            if (wGate || big) NeroPlanner.rig(c, "gate", W / 2 - 2, 0, -4, "Въездные автоворота", "Игрок рядом с воротами", "Открывает решётку, закрывает когда рядом никого нет", new int[]{5, 4, 5}, main);
            if (wGarage) {
                int g = NeroPlanner.shell(c, "Гараж", W, 0, 7, D, 1, 5, p, false, true, false);
                c.open(W - 1, 0, D / 2, W, 2, D / 2);
                NeroPlanner.rig(c, "gate", W + 1, 0, 0, "Гараж: автоворота", "Игрок рядом с воротами", "Открывает решётку", new int[]{5, 3, 5}, g);
            }
            if (wAlarm) NeroPlanner.alarm(c, -3, D / 2, 16, main);
            if (wPool) {
                c.begin("Бассейн", "structure", "-", "Яма с водой, облицовка кварцем, подсветка", main);
                c.carve(0, 0, D + 2, 8, 3, D + 9);
                c.box(0, -4, D + 2, 8, -4, D + 9, Blocks.SMOOTH_QUARTZ);
                c.ring(0, -4, D + 2, 8, -1, D + 9, Blocks.SMOOTH_QUARTZ);
                c.box(1, -3, D + 3, 7, -1, D + 8, Blocks.WATER);
                c.det(1, -3, D + 3, Blocks.SEA_LANTERN);
                c.det(7, -3, D + 8, Blocks.SEA_LANTERN);
            }
        } else if (wAlarm) NeroPlanner.alarm(c, 0, 0, 14);
        int[] dep = main >= 0 ? new int[]{main} : new int[0];
        int ex = 0, ez = bld ? D + 12 : 0;
        if (!bld && wLift) { NeroPlanner.rig(c, "lift", ex, 0, ez, "Лифт", "Кнопки этажей на пульте, прыжок/присед в шахте", "Плавное перемещение между этажами", new int[]{s.floors > 0 ? s.floors : 8, 6, speed}); ex += 12; }
        if (!bld && (wGate)) { NeroPlanner.rig(c, "gate", ex, 0, ez, "Автоворота", "Игрок рядом", "Открывает решётку", new int[]{5, 4, 5}); ex += 10; }
        if (wPrint) { blueprint(c, NeroArchetypes.bp("printer", t), new BlockPos(ex, 0, ez)); ex += 44; }
        int code = s.code >= 1000 ? s.code : 1234;
        if (wFarm) { NeroPlanner.rig(c, "farm", ex, 0, ez, "Автоферма", "Таймер", "Рост, сбор, пересадка, сундук", new int[]{1, 4, 30}, dep); ex += 16; }
        if (wSentry) { NeroPlanner.rig(c, "sentry", ex, 0, ez, "Турель", "Враги в радиусе", "Урон враждебным мобам", new int[]{12, 6, 10}, dep); ex += 10; }
        if (wDome) { NeroPlanner.rig(c, "dome", ex, 0, ez, "Защитный купол", "Таймер", "Лечение, удаление врагов", new int[]{10}, dep); ex += 26; }
        if (wTele) { NeroPlanner.rig(c, "tele", ex, 0, ez, "Сеть телепортов", "Прыжок/присед на площадке", "Перенос между площадками", new int[]{4, 12}, dep); ex += 16; }
        if (wVault) { NeroPlanner.rig(c, "vault", ex, 0, ez, "Кодовый замок", "Кнопка «Ввести код» на пульте", "Открывает дверь на 5 секунд", new int[]{code / 1000 % 10, code / 100 % 10, code / 10 % 10, code % 10}, dep); ex += 10; c.pl.help.add("Код замка: " + code); }
        if (wBelt) { NeroPlanner.rig(c, "belt", ex, 0, ez, "Конвейер", "Кнопка «Реверс» на пульте", "Двигает всё вдоль ленты", new int[]{16, 2, 2}, dep); ex += 22; }
        if (wShow) { NeroPlanner.rig(c, "show", ex, 0, ez, "Световое шоу", "Кнопки на пульте", "Сценарий из 10 сцен", new int[]{8}, dep); ex += 12; }
        if (wWeather) { NeroPlanner.rig(c, "weather", ex, 0, ez, "Метеопанель", "Кнопки на пульте", "Погода и время", new int[]{1}, dep); ex += 8; }
        if (wWave) { NeroPlanner.rig(c, "wave", ex, 0, ez, "Арена волн", "Кнопки на пульте", "Волны мобов", new int[]{30, 4, 7}, dep); ex += 36; }
        if (wMiner) { NeroPlanner.rig(c, "miner", ex, 0, ez, "Автобур", "Кнопки на пульте", "Бурит вниз слоями", new int[]{32, 1}, dep); ex += 8; }
        if (wLamps) { NeroPlanner.rig(c, "lamps", ex, 0, ez - 6, "Умные фонари", "Ночь", "Включаются ночью", new int[]{6, 4}, dep); ex += 12; }
        if (wHomes) { NeroPlanner.rig(c, "homes", ex, 0, ez, "Система домов", "Кнопки на пульте", "Личные точки дома", new int[]{1}, dep); }
    }

    // ------------------------------------------------------------------ командный отсек и стена управления
    private static void blk(NeroBuildPlan pl, int el, int x, int y, int z, Block b, int stage) { pl.blocks.add(new NeroBuildPlan.Block(x, y, z, b.getDefaultState(), el, stage)); }

    private static long k3(int x, int y, int z) { return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | ((long) y & 0xFFF); }

    /** Вызывается планировщиком в конце: переносит командные блоки к постройке и строит пульт с кнопками. */
    static void finish(NeroPlanner.Ctx c) {
        NeroBuildPlan pl = c.pl;
        int by = c.base.getY();
        String tag = pl.entityTag;
        int sMinX = Integer.MAX_VALUE, sMaxZ = Integer.MIN_VALUE, sMinZ = Integer.MAX_VALUE;
        for (NeroBuildPlan.Block b : pl.blocks) {
            if (b.st().isAir()) continue;
            sMinX = Math.min(sMinX, b.x()); sMinZ = Math.min(sMinZ, b.z()); sMaxZ = Math.max(sMaxZ, b.z());
        }
        if (sMinX == Integer.MAX_VALUE) { sMinX = c.base.getX(); sMinZ = c.base.getZ(); sMaxZ = c.base.getZ(); }

        // 1. командный отсек: рядом со зданием (позади), на поверхности, а не под землёй
        if (!pl.cbs.isEmpty()) {
            int cMinX = Integer.MAX_VALUE, cMinZ = Integer.MAX_VALUE, cMaxX = Integer.MIN_VALUE, cMaxZ = Integer.MIN_VALUE;
            for (NeroBuildPlan.Cb k : pl.cbs) { cMinX = Math.min(cMinX, k.x()); cMinZ = Math.min(cMinZ, k.z()); cMaxX = Math.max(cMaxX, k.x()); cMaxZ = Math.max(cMaxZ, k.z()); }
            int dx = sMinX - cMinX, dz = sMaxZ + 5 - cMinZ;
            List<NeroBuildPlan.Cb> old = new ArrayList<>(pl.cbs);
            pl.cbs.clear();
            for (NeroBuildPlan.Cb k : old) pl.cbs.add(new NeroBuildPlan.Cb(k.x() + dx, by, k.z() + dz, k.f(), k.type(), k.auto(), k.cond(), k.cmd(), k.el(), k.label(), k.role()));
            int bay = c.begin("Командный отсек", "structure", "-", "Платформа с командными блоками рядом со зданием");
            int x1 = cMinX + dx - 1, x2 = cMaxX + dx + 1, z1 = cMinZ + dz - 1, z2 = cMaxZ + dz + 1;
            for (int x = x1; x <= x2; x++)
                for (int z = z1; z <= z2; z++) {
                    blk(pl, bay, x, by - 1, z, Blocks.STONE_BRICKS, 1);
                    blk(pl, bay, x, by + 1, z, Blocks.AIR, 0);
                    blk(pl, bay, x, by + 2, z, Blocks.AIR, 0);
                }
            pl.setup.add("summon minecraft:text_display " + fd(x1 + 1.5) + " " + fd(by + 1.6) + " " + fd(z1 + 0.5)
                    + " {text:'{\"text\":\"КОМАНДНЫЕ БЛОКИ: не ломать\",\"color\":\"red\"}',billboard:\"center\",Tags:[\"" + tag + "\"]}");
        }

        // 2. пульт управления
        List<String[]> ctl = new ArrayList<>();
        Map<Integer, String[]> byEl = new TreeMap<>();
        for (Map.Entry<String, String> e : pl.ids.entrySet()) {
            String[] kp = e.getKey().split("@");
            if (kp.length != 2) continue;
            try { byEl.put(Integer.parseInt(kp[1]), new String[]{kp[0], e.getValue()}); } catch (Exception ignored) { }
        }
        int lifts = 0, li = 0;
        for (String[] v : byEl.values()) if (v[0].equals("lift")) lifts++;
        for (Map.Entry<Integer, String[]> e : byEl.entrySet()) {
            String key = e.getValue()[0], id = e.getValue()[1];
            Map<String, Integer> meta = pl.elements.get(e.getKey()).meta();
            String go = "scoreboard players set @p[distance=..30] " + id + "_go ";
            switch (key) {
                case "lift" -> {
                    li++;
                    int n = meta.getOrDefault("p0", 0);
                    for (int k = 1; k <= Math.min(n, 14); k++) ctl.add(new String[]{(lifts > 1 ? "Л" + li + " " : "") + "Этаж " + k, go + k});
                }
                case "vault" -> {
                    int code = meta.getOrDefault("p0", 0) * 1000 + meta.getOrDefault("p1", 0) * 100 + meta.getOrDefault("p2", 0) * 10 + meta.getOrDefault("p3", 0);
                    ctl.add(new String[]{"Ввести код", "scoreboard players set @p[distance=..30] " + id + "_code " + code});
                }
                case "miner" -> { ctl.add(new String[]{"Бур: пуск", go + 1}); ctl.add(new String[]{"Бур: стоп", go + 2}); }
                case "wave" -> { ctl.add(new String[]{"Арена: пуск", go + 1}); ctl.add(new String[]{"Арена: стоп", go + 2}); }
                case "show" -> { ctl.add(new String[]{"Шоу: старт", go + 1}); ctl.add(new String[]{"Шоу: стоп", go + 2}); }
                case "belt" -> ctl.add(new String[]{"Конвейер: реверс", go + 1});
                case "weather" -> {
                    String[] w = {"Ясно", "Дождь", "Гроза", "Утро", "День", "Полдень", "Вечер", "Ночь", "Полночь"};
                    for (int k = 0; k < w.length; k++) ctl.add(new String[]{w[k], go + (k + 1)});
                }
                case "homes" -> {
                    ctl.add(new String[]{"Запомнить дом", "scoreboard players set @p[distance=..30] " + id + "_hset 1"});
                    ctl.add(new String[]{"Домой", "scoreboard players set @p[distance=..30] " + id + "_hgo 1"});
                }
                default -> { }
            }
        }
        ctl.addAll(pl.controls);

        StringBuilder menu = new StringBuilder("tellraw @a[distance=..30] [{\"text\":\"=== " + esc(pl.title) + " ===\\n\",\"color\":\"aqua\"}");
        int hl = 0;
        for (String h : pl.help) { if (hl++ >= 10) break; menu.append(",{\"text\":\"").append(esc(h)).append("\\n\",\"color\":\"white\"}"); }
        menu.append(",{\"text\":\"Кнопки на стене рядом запускают механизмы.\",\"color\":\"gray\"}]");
        ctl.add(0, new String[]{"Справка", menu.toString()});
        while (ctl.size() > 32) ctl.remove(ctl.size() - 1);

        int N = ctl.size(), cols = Math.min(8, N), rows = (N + 7) / 8;
        int Xw = sMinX - 7, zs = sMinZ + 1;
        int pe = c.begin("Пульт управления", "structure", "Кнопки на стене слева от постройки", "Командные блоки с кнопками: управление механизмами");
        Set<Long> cell = new HashSet<>();
        for (int i = 0; i < N; i++) {
            int col = i % 8, row = i / 8, z = zs + 2 * col, y = by + 1 + 3 * row;
            String[] cc = ctl.get(i);
            int depth = cc.length;
            for (int d = 0; d < depth; d++) cell.add(k3(Xw + d, y, z));
        }
        int top = by + rows * 3 + 1;
        for (int y = by - 1; y <= top; y++)
            for (int z = zs - 1; z <= zs + 2 * cols - 1; z++)
                for (int x = Xw; x <= Xw + 4; x++) {
                    if (cell.contains(k3(x, y, z))) continue;
                    Block b = y == by - 1 ? Blocks.POLISHED_BLACKSTONE : (y == top ? Blocks.SEA_LANTERN : Blocks.BLACK_CONCRETE);
                    blk(pl, pe, x, y, z, b, 1);
                }
        for (int x = Xw - 3; x < Xw; x++)
            for (int z = zs - 1; z <= zs + 2 * cols - 1; z++) { blk(pl, pe, x, by - 1, z, Blocks.SMOOTH_STONE, 1); for (int y = by; y <= top + 1; y++) blk(pl, pe, x, y, z, Blocks.AIR, 0); }
        pl.setup.add("summon minecraft:text_display " + fd(Xw - 0.9) + " " + fd(top + 1.6) + " " + fd(zs + cols) + " {text:'{\"text\":\"ПУЛЬТ УПРАВЛЕНИЯ\",\"color\":\"gold\"}',billboard:\"center\",Tags:[\"" + tag + "\"]}");
        for (int i = 0; i < N; i++) {
            int col = i % 8, row = i / 8, z = zs + 2 * col, y = by + 1 + 3 * row;
            String[] cc = ctl.get(i);
            String label = esc(cc[0]);
            for (int d = 1; d < cc.length; d++) {
                String sh = cc[d].length() > 70 ? cc[d].substring(0, 69) + "…" : cc[d];
                pl.cbs.add(new NeroBuildPlan.Cb(Xw + d - 1, y, z, Direction.EAST, d == 1 ? NeroBuildPlan.CbType.IMPULSE : NeroBuildPlan.CbType.CHAIN, d != 1, false, cc[d], pe,
                        d == 1 ? "Кнопка «" + label + "»" : "Кнопка «" + label + "»: шаг " + d,
                        "Пульт управления: нажатие кнопки «" + label + "» выполняет: " + sh));
            }
            pl.cbs.add(new NeroBuildPlan.Cb(Xw + cc.length - 1, y, z, Direction.EAST, cc.length == 1 ? NeroBuildPlan.CbType.IMPULSE : NeroBuildPlan.CbType.CHAIN, cc.length != 1, false,
                    "tellraw @a[distance=..30] {\"text\":\"▶ " + label + "\",\"color\":\"green\"}", pe,
                    "Сообщение: «" + label + "»", "Показывает в чате, что кнопка «" + label + "» нажата."));
            pl.blocks.add(new NeroBuildPlan.Block(Xw - 1, y, z, Blocks.STONE_BUTTON.getDefaultState().with(WallMountedBlock.FACING, Direction.WEST), pe, 2));
            pl.setup.add("summon minecraft:text_display " + fd(Xw - 0.9) + " " + fd(y + 1.0) + " " + fd(z + 0.5)
                    + " {text:'{\"text\":\"" + label + "\",\"color\":\"aqua\"}',billboard:\"center\",Tags:[\"" + tag + "\"]}");
        }
        pl.help.add("Пульт управления: стена с кнопками слева от постройки, командный отсек позади неё.");
        NeroLabels.apply(pl);
    }

    private NeroFree() { }
}
