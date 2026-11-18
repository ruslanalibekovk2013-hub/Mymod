package com.mackros;

import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.RailBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.RailShape;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.mackros.NeroSpec.clamp;

/** Планировщик: превращает NeroSpec в NeroBuildPlan, комбинируя строительные элементы и механизмы Rig. */
public final class NeroPlanner {
    record Pal(Block wall, Block floor, Block roof, Block glass, Block pillar, Block light, Block accent) { }

    static Pal pal(String style) {
        if ("medieval".equals(style))
            return new Pal(Blocks.STONE_BRICKS, Blocks.OAK_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.GLASS, Blocks.OAK_LOG, Blocks.GLOWSTONE, Blocks.COBBLESTONE);
        if ("futuristic".equals(style))
            return new Pal(Blocks.SMOOTH_QUARTZ, Blocks.POLISHED_BLACKSTONE, Blocks.BLACK_CONCRETE, Blocks.CYAN_STAINED_GLASS, Blocks.IRON_BLOCK, Blocks.SEA_LANTERN, Blocks.LIGHT_BLUE_CONCRETE);
        return new Pal(Blocks.WHITE_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.GRAY_CONCRETE, Blocks.GLASS, Blocks.BLACK_CONCRETE, Blocks.SEA_LANTERN, Blocks.SPRUCE_PLANKS);
    }

    // ------------------------------------------------------------------ контекст построения
    static final class Ctx {
        final BlockPos base;
        final NeroBuildPlan pl;
        int el = -1, bayZ = 0, idN = 0;
        final long seed;

        Ctx(BlockPos b, NeroBuildPlan p, long seed) { base = b; pl = p; this.seed = seed; }

        /** Детерминированный id: при перепланировании проекта имена целей и тегов не меняются. */
        String nextId() {
            long h = (seed + 0x9E3779B97F4A7C15L * (++idN)) * 0xBF58476D1CE4E5B9L;
            h ^= (h >>> 31);
            String a = "abcdefghijklmnopqrstuvwxyz0123456789";
            StringBuilder s = new StringBuilder("m");
            for (int i = 0; i < 3; i++) { s.append(a.charAt((int) Math.floorMod(h, 36L))); h /= 36; }
            return s.toString();
        }

        int begin(String name, String kind, String activation, String logic, int... deps) {
            List<Integer> d = new ArrayList<>();
            for (int x : deps) d.add(x);
            pl.elements.add(new NeroBuildPlan.Element(name, kind, d, activation, logic, new java.util.HashMap<>()));
            el = pl.elements.size() - 1;
            return el;
        }

        void put(int x, int y, int z, BlockState s, int stage) {
            pl.blocks.add(new NeroBuildPlan.Block(base.getX() + x, base.getY() + y, base.getZ() + z, s, el, stage));
        }

        void set(int x, int y, int z, Block b) { put(x, y, z, b.getDefaultState(), b == Blocks.AIR ? 0 : 1); }
        void set(int x, int y, int z, BlockState s) { put(x, y, z, s, s.isAir() ? 0 : 1); }
        void det(int x, int y, int z, Block b) { put(x, y, z, b.getDefaultState(), 2); }
        void det(int x, int y, int z, BlockState s) { put(x, y, z, s, 2); }

        void box(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
                    for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, b);
        }

        void carve(int x1, int y1, int z1, int x2, int y2, int z2) { box(x1, y1, z1, x2, y2, z2, Blocks.AIR); }

        /** Проём в уже построенной стене: воздух на стадии деталей (после стен того же элемента). */
        void open(int x1, int y1, int z1, int x2, int y2, int z2) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
                    for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) det(x, y, z, Blocks.AIR);
        }

        /** Вертикальное кольцо стен по периметру прямоугольника. */
        void ring(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
            for (int y = y1; y <= y2; y++)
                for (int x = x1; x <= x2; x++)
                    for (int z = z1; z <= z2; z++)
                        if (x == x1 || x == x2 || z == z1 || z == z2) set(x, y, z, b);
        }

        void ladder(int x, int z, int y1, int y2, Direction f) {
            for (int y = y1; y <= y2; y++) det(x, y, z, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, f));
        }

        void bed(int x, int y, int z, Direction f) {
            det(x, y, z, Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, f).with(BedBlock.PART, BedPart.FOOT));
            det(x + f.getOffsetX(), y, z + f.getOffsetZ(), Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, f).with(BedBlock.PART, BedPart.HEAD));
        }

        /** Укладывает цепочку КБ змейкой (ширина 16) в «отсек КБ» слева от постройки, под землёй. */
        void chain(List<String> cmds, int el) {
            int w = 16, n = cmds.size();
            int bx = base.getX() - 28, by = base.getY(), bz = base.getZ() + bayZ;
            for (int i = 0; i < n; i++) {
                int row = i / w, col = i % w;
                int x = bx + (row % 2 == 0 ? col : w - 1 - col);
                Direction f = (col == w - 1 && i < n - 1) ? Direction.SOUTH : (row % 2 == 0 ? Direction.EAST : Direction.WEST);
                NeroBuildPlan.CbType t = i == 0 ? NeroBuildPlan.CbType.REPEAT : NeroBuildPlan.CbType.CHAIN;
                pl.cbs.add(new NeroBuildPlan.Cb(x, by, bz + row, f, t, true, false, cmds.get(i), el));
            }
            bayZ += (n + w - 1) / w + 1;
        }
    }

    private static String fd(double v) { return String.format(Locale.ROOT, "%.1f", v); }

    // ------------------------------------------------------------------ мост к каталогу Rig
    static int rig(Ctx c, String key, int ox, int oy, int oz, String name, String activation, String logic, int[] prm, int... deps) {
        Rig.Def d = Rig.find(key);
        int[] p = new int[d.pn().length];
        for (int i = 0; i < p.length; i++) p[i] = clamp(i < prm.length ? prm[i] : d.df()[i], d.mn()[i], d.mx()[i]);
        String id = c.nextId();
        Rig.Plan rp = d.gen().make(new BlockPos(c.base.getX() + ox, c.base.getY() + oy, c.base.getZ() + oz), id, p);
        int el = c.begin(name, "mechanism", activation, logic, deps);
        List<String> cmds = new ArrayList<>();
        for (Rig.Put pt : rp.puts) {
            if (pt.cmd() == null) c.put(ox + pt.x(), oy + pt.y(), oz + pt.z(), pt.st(), pt.st().isAir() ? 0 : 1);
            else cmds.add(pt.cmd());
        }
        c.pl.setup.addAll(rp.setup);
        if (!rp.help.isEmpty()) c.pl.help.add(name + ": " + rp.help);
        c.pl.elements.get(el).meta().put("params", p.length);
        for (int i = 0; i < p.length; i++) c.pl.elements.get(el).meta().put("p" + i, p[i]);
        c.pl.ids.put(key + "@" + el, id);
        c.chain(cmds, el);
        return el;
    }

    // ------------------------------------------------------------------ оболочка здания
    /** Здание: фундамент, этажи высотой fh, стены (или остекление), перекрытия, свет, крыша. Возвращает индекс элемента. */
    static int shell(Ctx c, String name, int x0, int z0, int W, int D, int F, int fh, Pal p, boolean curtain, boolean lights, boolean pyramid) {
        int el = c.begin(name, "structure", "-", "Стены, перекрытия, крыша");
        int x1 = x0 + W - 1, z1 = z0 + D - 1;
        c.carve(x0, 0, z0, x1, F * fh + 1, z1);
        c.box(x0, -1, z0, x1, -1, z1, p.floor());
        for (int f = 0; f < F; f++) {
            int y0 = f * fh;
            if (f > 0) c.box(x0, y0 - 1, z0, x1, y0 - 1, z1, p.floor());
            for (int x = x0; x <= x1; x++)
                for (int z = z0; z <= z1; z++) {
                    if (!(x == x0 || x == x1 || z == z0 || z == z1)) continue;
                    boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                    for (int y = y0; y <= y0 + fh - 2; y++) {
                        Block b = corner ? p.pillar() : (curtain ? (((x - x0) + (z - z0)) % 4 == 0 ? p.pillar() : p.glass()) : p.wall());
                        c.set(x, y, z, b);
                    }
                    if (!curtain && !corner) {
                        boolean zWall = z == z0 || z == z1;
                        int k = zWall ? x - x0 : z - z0;
                        if (k % 3 == 1) { c.det(x, y0 + 1, z, p.glass()); c.det(x, y0 + 2, z, p.glass()); }
                    }
                }
            if (lights) {
                for (int x = x0 + 3; x <= x1 - 3; x += 4)
                    for (int z = z0 + 3; z <= z1 - 3; z += 4) c.det(x, y0 + fh - 2, z, p.light());
            }
            c.det(x0 + W / 2, y0 + fh - 2, z0 + D / 2, p.light());
        }
        int top = F * fh - 1;
        c.box(x0, top, z0, x1, top, z1, p.roof());
        if (pyramid) {
            int k = 1;
            while (W - 2 * k >= 3 && D - 2 * k >= 3 && k < 7) { c.box(x0 + k, top + k, z0 + k, x1 - k, top + k, z1 - k, p.roof()); k++; }
        } else {
            c.ring(x0, top + 1, z0, x1, top + 1, z1, p.wall());
        }
        return el;
    }

    static void furnish(Ctx c, int x0, int z0, int W, int D, int F, int fh, boolean noLiftZone, int lx1, int lx2, int lz1, int lz2) {
        for (int f = 0; f < F; f++) {
            int y0 = f * fh;
            int bx = x0 + 2, bz = z0 + 2;
            c.bed(bx, y0, bz, Direction.SOUTH);
            if (W >= 11) c.bed(bx + 3, y0, bz, Direction.SOUTH);
            if (f == 0) {
                int kz = z0 + D - 2;
                c.det(x0 + 3, y0, kz, Blocks.CRAFTING_TABLE);
                c.det(x0 + 4, y0, kz, Blocks.FURNACE);
                c.det(x0 + 5, y0, kz, Blocks.CHEST);
                c.det(x0 + 6, y0, kz, Blocks.BARREL);
            } else {
                for (int x = x0 + 3; x <= x0 + 6 && x < x0 + W - 1; x++) c.det(x, y0, z0 + D - 2, Blocks.BOOKSHELF);
            }
        }
    }

    // ------------------------------------------------------------------ дом
    static void house(Ctx c, NeroSpec s) {
        Pal p = pal(s.style);
        int W = s.w > 0 ? clamp(s.w, 9, 31) : 13, D = s.d > 0 ? clamp(s.d, 9, 31) : 11, F = clamp(s.floors == 0 ? 2 : s.floors, 1, 6), fh = 5;
        boolean lift = s.extras.contains("lift") && F >= 2;
        boolean lighting = s.extras.contains("lighting");
        int main = shell(c, "Дом: каркас и крыша", 0, 0, W, D, F, fh, p, "futuristic".equals(s.style), true, !"modern".equals(s.style));
        // дверной проём
        c.open(W / 2 - 1, 0, 0, W / 2, 2, 0);
        if (F >= 2 && !lift) {
            c.begin("Дом: лестница", "structure", "-", "Лестница-стремянка между этажами", main);
            for (int f = 0; f < F; f++) {
                if (f > 0) c.det(1, f * fh - 1, D - 2, Blocks.AIR);
                c.ladder(1, D - 2, f * fh, f * fh + fh - 1 + (f == F - 1 ? -1 : 0), Direction.NORTH);
            }
        }
        {
            c.begin("Дом: мебель", "decor", "-", "Кровати, кухня, книги", main);
            furnish(c, 0, 0, W, D, F, fh, lift, W - 7, W - 3, D - 7, D - 3);
        }
        int hint = main;
        if (s.extras.contains("garage")) {
            int side = "left".equals(s.garageSide) ? -1 : ("back".equals(s.garageSide) ? 2 : 1);
            int gx0, gz0, gW, gD, gateX, gateZ;
            if (side == 2) { gx0 = 0; gz0 = D; gW = W; gD = 9; gateX = W / 2 - 2; gateZ = gz0 + gD - 1; }
            else { gW = 7; gD = D; gz0 = 0; gx0 = side == 1 ? W : -gW; gateX = gx0 + 1; gateZ = 0; }
            int g = shell(c, "Гараж", gx0, gz0, gW, gD, 1, 5, p, false, true, false);
            if (side == 2) c.open(W / 2 - 1, 0, D - 1, W / 2, 2, D);
            else c.open(side == 1 ? W - 1 : -1, 0, D / 2, side == 1 ? W : 0, 2, D / 2);
            int gate = rig(c, "gate", gateX, 0, gateZ, "Гараж: автоворота", "Игрок рядом с воротами", "Открывает решётку, закрывает когда рядом никого нет",
                    new int[]{5, 3, 5}, g);
            hint = gate;
        } else if (s.extras.contains("autogate")) {
            rig(c, "gate", W / 2 - 2, 0, -4, "Въездные автоворота", "Игрок рядом с воротами", "Открывает решётку, закрывает когда рядом никого нет", new int[]{5, 4, 5}, main);
        }
        if (s.extras.contains("pool")) {
            c.begin("Бассейн", "structure", "-", "Яма с водой, облицовка кварцем, подсветка", main);
            c.carve(0, 0, -9, 8, 3, -2);
            c.box(0, -4, -9, 8, -4, -2, Blocks.SMOOTH_QUARTZ);
            c.ring(0, -4, -9, 8, -1, -2, Blocks.SMOOTH_QUARTZ);
            c.box(1, -3, -8, 7, -1, -3, Blocks.WATER);
            c.det(1, -3, -8, Blocks.SEA_LANTERN);
            c.det(7, -3, -8, Blocks.SEA_LANTERN);
            c.det(1, -3, -3, Blocks.SEA_LANTERN);
            c.det(7, -3, -3, Blocks.SEA_LANTERN);
        }
        if (lighting) {
            rig(c, "lamps", 0, 0, -12, "Умные фонари у входа", "Ночь (время суток)", "Включаются ночью, гаснут днём", new int[]{6, 4}, main);
        }
        if (lift) {
            rig(c, "lift", W - 6, 0, D - 6, "Лифт в доме", "Прыжок/присед в шахте или /trigger", "Плавное перемещение между этажами",
                    new int[]{F, fh, 2}, main);
        }
        if (s.extras.contains("alarm")) alarm(c, W / 2, -3, 14, main);
    }

    // ------------------------------------------------------------------ небоскрёб и отель
    private static void tower(Ctx c, NeroSpec s, boolean hotel) {
        Pal p = pal(s.style == null ? (hotel ? "modern" : "futuristic") : s.style);
        int W = s.w > 0 ? clamp(s.w, 11, 31) : 15, F = clamp(s.floors == 0 ? (hotel ? 8 : 10) : s.floors, 2, 14), fh = 5;
        boolean curtain = !"medieval".equals(s.style);
        int main = shell(c, hotel ? "Отель: каркас" : "Небоскрёб: каркас", 0, 0, W, W, F, fh, p, curtain, true, false);
        c.open(W / 2 - 1, 0, 0, W / 2 + 1, 2, 0);
        c.begin("Антенна и огни крыши", "decor", "-", "Мачта с подсветкой", main);
        for (int y = 0; y < 8; y++) c.det(W / 2, F * fh + 1 + y, W / 2, Blocks.END_ROD);
        if (hotel) {
            c.begin("Отель: номера", "structure", "-", "Перегородки, двери-проёмы, кровати", main);
            for (int f = 0; f < F; f++) {
                int y0 = f * fh;
                for (int z = 1; z < W - 1; z++) {
                    for (int x : new int[]{4, W - 5}) {
                        boolean door = (z - 1) % 5 == 2;
                        for (int y = y0; y <= y0 + 3; y++) if (!(door && y <= y0 + 1)) c.det(x, y, z, p.wall());
                    }
                }
                for (int x = 1; x < W - 1; x++) {
                    if (x == 4 || x == W - 5) continue;
                    for (int z = 6; z < W - 1; z += 5) {
                        if (x > 4 && x < W - 5) continue;
                        for (int y = y0; y <= y0 + 3; y++) c.det(x, y, z, p.wall());
                    }
                }
                for (int z = 3; z < W - 2; z += 5) {
                    c.bed(2, y0, z, Direction.SOUTH);
                    c.bed(W - 3, y0, z, Direction.SOUTH);
                }
            }
        }
        rig(c, "lift", W / 2 - 1, 0, W / 2 - 1, "Центральный лифт", "Прыжок/присед в шахте или /trigger", "Плавное перемещение между этажами",
                new int[]{F, fh, 2}, main);
        if (s.extras.contains("lighting")) rig(c, "lamps", 0, 0, -6, "Умные фонари", "Ночь", "Автоподсветка у входа", new int[]{6, 4}, main);
        if (s.extras.contains("alarm")) alarm(c, W / 2, -3, 16, main);
    }

    // ------------------------------------------------------------------ бункер
    private static void bunker(Ctx c, NeroSpec s) {
        Pal p = pal("medieval");
        int W = s.w > 0 ? clamp(s.w, 9, 25) : 11, D = s.d > 0 ? clamp(s.d, 9, 25) : 9;
        int code = s.code >= 1000 ? s.code : 1234;
        int cx = W / 2;
        int room = c.begin("Бункер: главный зал", "structure", "-", "Подземный зал с мебелью");
        c.carve(0, -8, 0, W - 1, -4, D - 1);
        c.box(0, -9, 0, W - 1, -9, D - 1, Blocks.STONE_BRICKS);
        c.box(0, -4, 0, W - 1, -4, D - 1, Blocks.STONE_BRICKS);
        c.ring(0, -8, 0, W - 1, -5, D - 1, Blocks.STONE_BRICKS);
        for (int x = 3; x < W - 3; x += 4) for (int z = 3; z < D - 3; z += 4) c.det(x, -5, z, Blocks.SEA_LANTERN);
        c.bed(2, -8, 2, Direction.SOUTH);
        c.bed(4, -8, 2, Direction.SOUTH);
        c.det(W - 3, -8, 2, Blocks.CHEST);
        c.det(W - 3, -8, 3, Blocks.BARREL);
        c.det(W - 3, -8, 4, Blocks.FURNACE);
        c.det(W - 3, -8, 5, Blocks.CRAFTING_TABLE);
        for (int x = 2; x < W - 4; x++) c.det(x, -8, D - 2, Blocks.BOOKSHELF);
        int cor = c.begin("Бункер: коридор и шахта входа", "structure", "-", "Коридор к двери и вертикальная шахта с лестницей", room);
        c.carve(cx - 1, -8, -8, cx + 1, -5, -1);
        c.box(cx - 2, -9, -8, cx + 2, -9, -1, Blocks.STONE_BRICKS);
        c.box(cx - 2, -4, -8, cx + 2, -4, -1, Blocks.STONE_BRICKS);
        for (int z = -8; z <= -1; z++) for (int y = -8; y <= -5; y++) { c.set(cx - 2, y, z, Blocks.STONE_BRICKS); c.set(cx + 2, y, z, Blocks.STONE_BRICKS); }
        c.carve(cx, -8, -7, cx, 1, -7);
        for (int y = -8; y <= 0; y++) c.set(cx, y, -8, Blocks.STONE_BRICKS);
        c.ladder(cx, -7, -8, 0, Direction.SOUTH);
        for (int y = -3; y <= 0; y++) { c.set(cx - 1, y, -7, Blocks.STONE_BRICKS); c.set(cx + 1, y, -7, Blocks.STONE_BRICKS); }
        rig(c, "vault", cx - 1, -7, 0, "Бункер: кодовая дверь", "/trigger с кодом", "Открывает броне-дверь на 5 секунд при верном коде",
                new int[]{code / 1000 % 10, code / 100 % 10, code / 10 % 10, code % 10}, room, cor);
        c.pl.help.add("Код двери бункера: " + code);
    }

    // ------------------------------------------------------------------ замок
    private static void castle(Ctx c, NeroSpec s) {
        Pal p = pal("medieval");
        int R = s.w > 0 ? clamp(s.w, 19, 41) : 25;
        int wall = c.begin("Замок: стены и башни", "structure", "-", "Крепостная стена с зубцами и 4 башни");
        c.carve(-3, 0, -3, R + 2, 16, R + 2);
        c.box(-3, -1, -3, R + 2, -1, R + 2, Blocks.COBBLESTONE);
        for (int y = 0; y < 8; y++)
            for (int x = 0; x < R; x++)
                for (int z = 0; z < R; z++)
                    if (x < 2 || x >= R - 2 || z < 2 || z >= R - 2) c.set(x, y, z, p.wall());
        for (int x = 0; x < R; x += 2) { c.set(x, 8, 0, p.wall()); c.set(x, 8, R - 1, p.wall()); c.set(0, 8, x, p.wall()); c.set(R - 1, 8, x, p.wall()); }
        int[][] corners = {{-2, -2}, {R - 3, -2}, {-2, R - 3}, {R - 3, R - 3}};
        for (int[] k : corners) {
            c.ring(k[0], 0, k[1], k[0] + 4, 13, k[1] + 4, p.wall());
            c.box(k[0], 13, k[1], k[0] + 4, 13, k[1] + 4, p.roof());
            for (int i = 0; i <= 4; i += 2) { c.set(k[0] + i, 14, k[1], p.wall()); c.set(k[0] + i, 14, k[1] + 4, p.wall()); c.set(k[0], 14, k[1] + i, p.wall()); c.set(k[0] + 4, 14, k[1] + i, p.wall()); }
            c.det(k[0] + 2, 12, k[1] + 2, Blocks.GLOWSTONE);
        }
        int gx = R / 2 - 2;
        c.open(gx, 0, 0, gx + 4, 4, 1);
        int gate = rig(c, "gate", gx, 0, 0, "Замок: ворота", "Игрок рядом", "Автооткрытие решётки", new int[]{5, 4, 6}, wall);
        int kx = R / 2 - 5, kz = R / 2 - 5;
        shell(c, "Замок: донжон", kx, kz, 11, 11, 3, 5, p, false, true, true);
        c.open(kx + 5, 0, kz, kx + 5, 2, kz);
        furnish(c, kx, kz, 11, 11, 3, 5, false, 0, 0, 0, 0);
        c.pl.help.add("Замок: стена с автоворотами, 4 башни, донжон на 3 этажа.");
    }

    // ------------------------------------------------------------------ дороги и транспорт
    private static void lampPost(Ctx c, int x, int z) {
        for (int y = 0; y < 4; y++) c.set(x, y, z, Blocks.COBBLESTONE_WALL);
        c.set(x, 4, z, Blocks.SEA_LANTERN);
    }

    private static void road(Ctx c, NeroSpec s) {
        int L = s.len > 0 ? clamp(s.len, 8, 120) : 40;
        c.begin("Дорога", "structure", "-", "Полотно с разметкой и фонарями");
        c.carve(0, 0, -1, L - 1, 5, 5);
        c.box(0, -1, 0, L - 1, -1, 4, Blocks.GRAY_CONCRETE);
        for (int x = 0; x < L; x++) {
            c.set(x, -1, 0, Blocks.WHITE_CONCRETE);
            c.set(x, -1, 4, Blocks.WHITE_CONCRETE);
            if (x % 4 < 2) c.set(x, -1, 2, Blocks.YELLOW_CONCRETE);
        }
        for (int x = 4; x < L; x += 8) { lampPost(c, x, -1); lampPost(c, x, 5); }
    }

    private static void bridge(Ctx c, NeroSpec s) {
        int L = s.len > 0 ? clamp(s.len, 8, 120) : 40;
        c.begin("Мост", "structure", "-", "Настил, опоры, перила, фонари");
        c.carve(0, 0, -1, L - 1, 5, 5);
        c.box(0, -1, 0, L - 1, -1, 4, Blocks.STONE_BRICKS);
        c.box(0, -1, 2, L - 1, -1, 2, Blocks.SMOOTH_STONE);
        for (int x = 0; x < L; x++) { c.set(x, 0, 0, Blocks.OAK_FENCE); c.set(x, 0, 4, Blocks.OAK_FENCE); }
        for (int x = 0; x < L; x += 8) {
            for (int y = -16; y <= -2; y++) { c.set(x, y, 0, Blocks.STONE_BRICKS); c.set(x, y, 4, Blocks.STONE_BRICKS); c.set(x, y, 2, Blocks.STONE_BRICKS); }
            lampPost(c, x, -1);
        }
    }

    private static void tunnel(Ctx c, NeroSpec s) {
        int L = s.len > 0 ? clamp(s.len, 8, 100) : 30;
        c.begin("Тоннель", "structure", "-", "Оболочка из кирпича, дорога, светильники");
        c.carve(0, 0, 0, L - 1, 4, 6);
        for (int x = 0; x < L; x++) {
            for (int z = -1; z <= 7; z++) { c.set(x, -1, z, z >= 0 && z <= 6 ? Blocks.GRAY_CONCRETE : Blocks.STONE_BRICKS); c.set(x, 5, z, Blocks.STONE_BRICKS); }
            for (int y = 0; y <= 4; y++) { c.set(x, y, -1, Blocks.STONE_BRICKS); c.set(x, y, 7, Blocks.STONE_BRICKS); }
            if (x % 6 == 3) c.det(x, 4, 3, Blocks.SEA_LANTERN);
            if (x % 4 < 2) c.set(x, -1, 3, Blocks.YELLOW_CONCRETE);
        }
    }

    private static void parking(Ctx c, NeroSpec s) {
        int W = s.w > 0 ? clamp(s.w, 12, 40) : 20, D = s.d > 0 ? clamp(s.d, 10, 30) : 12;
        int e = c.begin("Парковка", "structure", "-", "Площадка с разметкой мест, ограда, фонари");
        c.carve(0, 0, 0, W - 1, 5, D - 1);
        c.box(0, -1, 0, W - 1, -1, D - 1, Blocks.GRAY_CONCRETE);
        for (int x = 2; x < W - 1; x += 4) for (int z = 3; z < D - 1; z++) if (z != D / 2 - 1 && z != D / 2) c.set(x, -1, z, Blocks.WHITE_CONCRETE);
        for (int x = 0; x < W; x++) { c.set(x, 0, 0, Blocks.OAK_FENCE); c.set(x, 0, D - 1, Blocks.OAK_FENCE); }
        for (int z = 0; z < D; z++) { c.set(0, 0, z, Blocks.OAK_FENCE); c.set(W - 1, 0, z, Blocks.OAK_FENCE); }
        c.open(W / 2 - 2, 0, 0, W / 2 + 2, 3, 0);
        lampPost(c, 1, 1); lampPost(c, W - 2, 1); lampPost(c, 1, D - 2); lampPost(c, W - 2, D - 2);
        rig(c, "gate", W / 2 - 2, 0, 0, "Парковка: автошлагбаум", "Игрок/машина рядом", "Автооткрытие въезда", new int[]{5, 3, 5}, e);
    }

    private static void station(Ctx c, NeroSpec s) {
        int L = s.len > 0 ? clamp(s.len, 12, 100) : 30;
        c.begin("Железнодорожная платформа", "structure", "-", "Платформа, рельсы, навес, освещение");
        c.carve(0, 0, 0, L - 1, 6, 10);
        c.box(0, -1, 5, L - 1, -1, 9, Blocks.GRAVEL);
        c.box(0, -1, 0, L - 1, 0, 3, Blocks.STONE_BRICKS);
        for (int x = 0; x < L; x++) {
            c.set(x, 0, 3, Blocks.YELLOW_CONCRETE);
            c.det(x, 0, 6, Blocks.RAIL.getDefaultState().with(RailBlock.SHAPE, RailShape.EAST_WEST));
            c.det(x, 0, 8, Blocks.RAIL.getDefaultState().with(RailBlock.SHAPE, RailShape.EAST_WEST));
        }
        for (int x = 0; x < L; x += 6) for (int z : new int[]{0, 3}) for (int y = 1; y <= 4; y++) c.set(x, y, z, Blocks.OAK_FENCE);
        c.box(0, 5, 0, L - 1, 5, 3, Blocks.SPRUCE_PLANKS);
        for (int x = 3; x < L; x += 6) c.det(x, 4, 1, Blocks.LANTERN);
    }

    private static void airport(Ctx c, NeroSpec s) {
        int L = s.len > 0 ? clamp(s.len, 30, 120) : 64;
        c.begin("Взлётная полоса", "structure", "-", "Полоса с разметкой и огнями");
        c.carve(0, 0, 0, L - 1, 6, 8);
        c.box(0, -1, 0, L - 1, -1, 8, Blocks.BLACK_CONCRETE);
        for (int x = 0; x < L; x++) {
            if (x % 6 < 3) c.set(x, -1, 4, Blocks.WHITE_CONCRETE);
            if (x % 8 == 0) { c.set(x, -1, 0, Blocks.SEA_LANTERN); c.set(x, -1, 8, Blocks.SEA_LANTERN); }
        }
        for (int z = 1; z < 8; z += 2) { c.set(1, -1, z, Blocks.WHITE_CONCRETE); c.set(2, -1, z, Blocks.WHITE_CONCRETE); }
        Pal p = pal("futuristic");
        int tw = shell(c, "Аэропорт: диспетчерская башня", 4, 12, 7, 7, 4, 5, p, true, true, false);
        c.open(7, 0, 12, 7, 2, 12);
        rig(c, "lift", 8, 0, 14, "Башня: лифт", "Прыжок/присед в шахте", "Перемещение диспетчера", new int[]{4, 5, 2}, tw);
        int hg = shell(c, "Ангар", 20, 12, 21, 15, 1, 8, pal("modern"), false, true, false);
        rig(c, "gate", 25, 0, 12, "Ангар: автоворота", "Игрок/техника рядом", "Автооткрытие", new int[]{9, 6, 8}, hg);
    }

    private static void warehouse(Ctx c, NeroSpec s, boolean factory) {
        Pal p = pal(factory ? "futuristic" : "modern");
        int W = s.w > 0 ? clamp(s.w, 15, 41) : 21, D = s.d > 0 ? clamp(s.d, 11, 31) : 13;
        int e = shell(c, factory ? "Фабрика: цех" : "Склад: ангар", 0, 0, W, D, 1, 7, p, false, true, false);
        rig(c, "gate", W / 2 - 3, 0, 0, "Большие автоворота", "Игрок/техника рядом", "Автооткрытие", new int[]{7, 5, 6}, e);
        c.begin(factory ? "Фабрика: оборудование" : "Склад: стеллажи", "decor", "-", "Стеллажи из бочек и сундуков", e);
        for (int x = 3; x < W - 3; x += 4) for (int z = 4; z < D - 2; z++) { if (factory) continue; c.det(x, 0, z, z % 2 == 0 ? Blocks.BARREL : Blocks.CHEST); c.det(x, 1, z, Blocks.BARREL); }
        if (factory) {
            rig(c, "belt", 2, 0, 4, "Фабрика: конвейер", "/trigger go set 1 меняет направление", "Двигает игроков, мобов и предметы вдоль цеха", new int[]{W - 5, 2, 2}, e);
            c.begin("Фабрика: труба", "structure", "-", "Дымовая труба", e);
            for (int y = 0; y < 16; y++) c.ring(W + 1, y, 2, W + 3, y, 4, Blocks.BRICKS);
            c.det(W + 2, 16, 3, Blocks.CAMPFIRE);
        }
    }

    // ------------------------------------------------------------------ секретная комната
    private static void secret(Ctx c, NeroSpec s) {
        Pal p = pal(s.style);
        String tag = c.pl.entityTag;
        String id = c.nextId();
        int wl = c.begin("Секретная комната: стена и комната", "structure", "-", "Маскирующая стена с дверью и скрытая комната 7x7");
        c.carve(0, 0, -2, 10, 5, 9);
        c.box(0, -1, -2, 10, -1, 9, p.floor());
        for (int x = 0; x < 11; x++) for (int y = 0; y <= 4; y++) c.set(x, y, 0, p.wall());
        c.ring(2, 0, 0, 8, 4, 8, p.wall());
        c.box(2, 4, 0, 8, 4, 8, p.roof());
        c.carve(3, 0, 1, 7, 3, 7);
        c.det(5, 3, 4, p.light());
        c.det(3, 0, 6, Blocks.ENCHANTING_TABLE);
        c.det(7, 0, 6, Blocks.CHEST);
        c.det(5, 0, 6, Blocks.BOOKSHELF);
        c.pl.setup.add("scoreboard objectives add " + id + "_t dummy");
        c.pl.setup.add("scoreboard players set #open " + id + "_t 0");
        int X = c.base.getX(), Y = c.base.getY(), Z = c.base.getZ();
        c.pl.setup.add("summon minecraft:painting " + (X + 4) + " " + (Y + 1) + " " + (Z - 1) + " {variant:'minecraft:kebab',facing:2b,Tags:['" + tag + "']}");
        c.pl.setup.add("summon minecraft:interaction " + (X + 4) + " " + (Y + 1) + " " + (Z - 1) + " {width:1.0f,height:1.0f,response:1b,Tags:['" + tag + "','" + id + "_btn']}");
        String wallId = net.minecraft.registry.Registries.BLOCK.getId(p.wall()).toString();
        String f1 = (X + 4) + " " + Y + " " + Z, f2 = (X + 5) + " " + (Y + 2) + " " + Z;
        for (int x = 4; x <= 5; x++) for (int y = 0; y <= 2; y++) c.set(x, y, 0, p.wall());
        int el = c.begin("Секретная комната: механизм двери", "mechanism", "ПКМ по картине", "Открывает дверь на 5 секунд, затем закрывает", wl);
        List<String> cm = new ArrayList<>();
        String t = id + "_t";
        String btn = "@e[type=minecraft:interaction,tag=" + id + "_btn]";
        cm.add("execute as " + btn + " if data entity @s interaction run scoreboard players set #open " + t + " 100");
        cm.add("execute as " + btn + " if data entity @s interaction run playsound minecraft:block.piston.extend master @a ~ ~ ~ 1 0.7");
        cm.add("execute as " + btn + " if data entity @s interaction run data remove entity @s interaction");
        cm.add("execute if score #open " + t + " matches 1.. run fill " + f1 + " " + f2 + " minecraft:air replace " + wallId);
        cm.add("execute if score #open " + t + " matches 1.. run scoreboard players remove #open " + t + " 1");
        cm.add("execute positioned " + fd(X + 5) + " " + fd(Y + 1) + " " + fd(Z + 0.5) + " if score #open " + t + " matches 0 unless entity @a[distance=..2] run fill " + f1 + " " + f2 + " " + wallId + " replace minecraft:air");
        c.chain(cm, el);
        c.pl.help.add("Секретная комната: ПКМ по картине на стене открывает дверь на 5 секунд.");
    }

    // ------------------------------------------------------------------ сигнализация
    static void alarm(Ctx c, int x, int z, int radius, int... dep) {
        String id = c.nextId();
        int el = c.begin("Сигнализация", "mechanism", "Чужой игрок в радиусе " + radius, "Сирена, надпись, подсветка чужих", dep);
        for (int y = 0; y < 3; y++) c.set(x, y, z, Blocks.IRON_BLOCK);
        c.set(x, 3, z, Blocks.REDSTONE_LAMP);
        c.pl.setup.add("scoreboard objectives add " + id + "_t dummy");
        c.pl.setup.add("tag @s add " + id + "_trust");
        String t = id + "_t";
        int X = c.base.getX() + x, Y = c.base.getY() + 3, Z = c.base.getZ() + z;
        String pos = "positioned " + fd(X + 0.5) + " " + fd(Y) + " " + fd(Z + 0.5);
        String foe = "@a[distance=.." + radius + ",tag=!" + id + "_trust]";
        List<String> cm = new ArrayList<>();
        cm.add("scoreboard players add #t " + t + " 1");
        cm.add("execute if score #t " + t + " matches 20.. " + pos + " if entity " + foe + " run playsound minecraft:block.bell.use master @a ~ ~ ~ 3 0.5");
        cm.add("execute if score #t " + t + " matches 20.. " + pos + " run title " + foe + " actionbar {'text':'ТРЕВОГА: чужой в охраняемой зоне','color':'red'}");
        cm.add("execute if score #t " + t + " matches 20.. " + pos + " run effect give " + foe + " minecraft:glowing 3 0 true");
        cm.add("execute if score #t " + t + " matches 20.. run scoreboard players set #t " + t + " 0");
        c.chain(cm.stream().map(s -> s.replace('\'', '"')).toList(), el);
        c.pl.help.add("Сигнализация: друзьям выдай метку /tag Имя add " + id + "_trust (у тебя она уже есть).");
    }

    // ------------------------------------------------------------------ точка входа
    public static NeroBuildPlan plan(NeroSpec s, BlockPos base) {
        NeroBuildPlan pl = new NeroBuildPlan();
        pl.title = s.title();
        pl.origin = base;
        Ctx c = new Ctx(base, pl, s.seed == 0 ? 12345L : s.seed);
        pl.entityTag = "nero_" + c.nextId();
        switch (s.kind) {
            case "house" -> house(c, s);
            case "tower" -> tower(c, s, false);
            case "hotel" -> tower(c, s, true);
            case "bunker" -> bunker(c, s);
            case "castle" -> castle(c, s);
            case "road" -> road(c, s);
            case "bridge" -> bridge(c, s);
            case "tunnel" -> tunnel(c, s);
            case "parking" -> parking(c, s);
            case "station" -> station(c, s);
            case "airport" -> airport(c, s);
            case "warehouse" -> warehouse(c, s, false);
            case "factory" -> warehouse(c, s, true);
            case "secret" -> secret(c, s);
            case "alarm" -> alarm(c, 0, 0, s.len > 0 ? clamp(s.len, 5, 40) : 14);
            case "lift" -> {
                for (int i = 0; i < s.copies; i++)
                    rig(c, "lift", i * 9, 0, 0, "Лифт" + (s.copies > 1 ? " №" + (i + 1) : ""), "Прыжок/присед в шахте или /trigger",
                            "Плавное перемещение между этажами", new int[]{s.floors > 0 ? s.floors : 5, s.len > 0 ? s.len : 6, 2});
            }
            case "tele" -> rig(c, "tele", 0, 0, 0, "Сеть телепортов", "Прыжок/присед на площадке", "Перенос на соседнюю площадку", new int[]{s.floors > 0 ? s.floors : 4, s.len > 0 ? s.len : 12});
            case "gate" -> rig(c, "gate", 0, 0, 0, "Автоворота", "Игрок рядом", "Автооткрытие решётки", new int[]{s.w > 0 ? s.w : 5, 4, 4});
            case "gate_key" -> rig(c, "gate_key", 0, 0, 0, "Ворота по ключу", "Игрок с меткой рядом", "Открывает только владельцам метки", new int[]{s.w > 0 ? s.w : 5, 4, 4});
            case "vault" -> {
                int code = s.code >= 1000 ? s.code : 1234;
                rig(c, "vault", 0, 0, 0, "Кодовый замок", "/trigger с кодом", "Открывает дверь на 5 секунд", new int[]{code / 1000 % 10, code / 100 % 10, code / 10 % 10, code % 10});
            }
            case "farm" -> rig(c, "farm", 0, 0, 0, "Автоферма", "Таймер", "Рост, сбор, пересадка, сундук", new int[]{1, 4, 30});
            case "miner" -> rig(c, "miner", 0, 0, 0, "Автобур", "/trigger go", "Бурит вниз слоями, собирает в сундук", new int[]{s.len > 0 ? s.len : 32, 1});
            case "sentry" -> rig(c, "sentry", 0, 0, 0, "Турель", "Враги в радиусе", "Магический урон враждебным мобам", new int[]{12, 6, 10});
            case "dome" -> rig(c, "dome", 0, 0, 0, "Защитный купол", "Таймер", "Лечение, удаление врагов", new int[]{s.len > 0 ? s.len : 10});
            case "wave" -> rig(c, "wave", 0, 0, 0, "Арена волн", "/trigger go", "Волны мобов по таймеру", new int[]{30, 4, 7});
            case "belt" -> rig(c, "belt", 0, 0, 0, "Конвейер", "/trigger go меняет направление", "Двигает всё вдоль ленты", new int[]{s.len > 0 ? s.len : 16, 2, 2});
            case "show" -> rig(c, "show", 0, 0, 0, "Световое шоу", "/trigger go", "Сценарий из 10 сцен", new int[]{8});
            case "weather" -> rig(c, "weather", 0, 0, 0, "Метеопанель", "/trigger go", "Погода и время", new int[]{1});
            case "homes" -> rig(c, "homes", 0, 0, 0, "Система домов", "/trigger", "Личные точки дома", new int[]{1});
            case "lamps" -> rig(c, "lamps", 0, 0, 0, "Умные фонари", "Ночь", "Включаются ночью", new int[]{s.len > 0 ? s.len : 6, 4});
            case "custom" -> NeroFree.custom(c, s);
            default -> { }
        }
        NeroFree.finish(c);
        pl.computeBounds();
        return pl;
    }

    private NeroPlanner() { }
}
