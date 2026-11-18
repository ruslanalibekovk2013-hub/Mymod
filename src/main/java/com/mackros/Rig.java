package com.mackros;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CommandBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Конструктор механизмов: каталог крупных конструкций из командных блоков (только одиночная игра).
 * Каждая запись генерирует План (блоки + цепочка КБ + команды настройки) для выбранной на карте точки.
 */
public final class Rig {
    public record Put(int x, int y, int z, BlockState st, Direction f, String cmd, boolean repeat) { }

    public static final class Plan {
        public final List<Put> puts = new ArrayList<>();
        public final List<String> setup = new ArrayList<>();
        public String help = "";
        public int cb, other;
        public int minX = 0, minZ = 0, maxX = 0, maxZ = 0, minY = 0, maxY = 0;
    }

    public interface Gen { Plan make(BlockPos base, String id, int[] p); }

    public record Def(String key, String cat, String name, String desc, String[] pn, int[] mn, int[] mx, int[] df, int[] st, Gen gen) { }

    public static final List<Def> ALL = new ArrayList<>();
    private static final double[] STEPS = {0.15, 0.3, 0.45, 0.55};

    // ------------------------------------------------------------ построитель
    private static final class B {
        final BlockPos base;
        final String id;
        final Plan pl = new Plan();
        final List<String> cmds = new ArrayList<>();

        B(BlockPos base, String id) { this.base = base; this.id = id; }

        int X(int dx) { return base.getX() + dx; }
        int Y(int dy) { return base.getY() + dy; }
        int Z(int dz) { return base.getZ() + dz; }
        String o(String n) { return id + "_" + n; }
        void setup(String s) { pl.setup.add(s); }
        void obj(String n, String crit) { setup("scoreboard objectives add " + o(n) + " " + crit); }
        void c(String cmd) { cmds.add(cmd.replace('\'', '"')); }
        void put(int x, int y, int z, Block b) { pl.puts.add(new Put(x, y, z, b.getDefaultState(), null, null, false)); }
        void put(int x, int y, int z, BlockState s) { pl.puts.add(new Put(x, y, z, s, null, null, false)); }
        void box(int x1, int y1, int z1, int x2, int y2, int z2, Block b) {
            for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
                for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                    for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) put(x, y, z, b);
        }

        /** Укладывает цепочку КБ змейкой (ширина 16) от точки (ax, ay, az) и считает границы. */
        Plan done(int ax, int ay, int az) {
            int n = cmds.size(), w = 16;
            for (int i = 0; i < n; i++) {
                int row = i / w, col = i % w;
                int x = ax + (row % 2 == 0 ? col : w - 1 - col);
                Direction f = (col == w - 1 && i < n - 1) ? Direction.SOUTH : (row % 2 == 0 ? Direction.EAST : Direction.WEST);
                pl.puts.add(new Put(x, ay, az + row, null, f, cmds.get(i), i == 0));
            }
            pl.cb = n;
            boolean first = true;
            for (Put p : pl.puts) {
                if (p.cmd() == null && p.st().isAir()) { } else if (p.cmd() == null) pl.other++;
                if (first) { pl.minX = pl.maxX = p.x(); pl.minZ = pl.maxZ = p.z(); pl.minY = pl.maxY = p.y(); first = false; }
                pl.minX = Math.min(pl.minX, p.x()); pl.maxX = Math.max(pl.maxX, p.x());
                pl.minZ = Math.min(pl.minZ, p.z()); pl.maxZ = Math.max(pl.maxZ, p.z());
                pl.minY = Math.min(pl.minY, p.y()); pl.maxY = Math.max(pl.maxY, p.y());
            }
            return pl;
        }
    }

    private static String fd(double v) { return String.format(Locale.ROOT, "%.1f", v); }
    private static String sc(String... kv) { return "scores={" + String.join(",", kv) + "}"; }

    private static void reg(String key, String cat, String name, String desc, String[] pn, int[] mn, int[] mx, int[] df, int[] st, Gen g) {
        ALL.add(new Def(key, cat, name, desc, pn, mn, mx, df, st, g));
    }

    public static Def find(String key) {
        for (Def d : ALL) if (d.key().equals(key)) return d;
        return ALL.get(0);
    }

    public static int[] defaults(Def d) { return d.df().clone(); }

    public static String newId() {
        String a = "abcdefghijklmnopqrstuvwxyz0123456789";
        Random r = new Random();
        StringBuilder s = new StringBuilder("m");
        for (int i = 0; i < 3; i++) s.append(a.charAt(r.nextInt(a.length())));
        return s.toString();
    }

    private static final String[] HOSTILE = {"zombie", "skeleton", "spider", "cave_spider", "creeper", "witch", "pillager", "vindicator",
            "evoker", "ravager", "phantom", "husk", "drowned", "stray", "slime", "magma_cube", "silverfish", "endermite", "guardian",
            "wither_skeleton", "blaze", "ghast", "zombie_villager", "vex", "hoglin", "zoglin", "piglin_brute", "shulker"};

    // ------------------------------------------------------------ каталог
    static {
        // ===== ЛИФТ =====
        reg("lift", "Лифты", "Супер-лифт",
                "Шахта 3x3 с кварцевыми площадками. Прыжок в шахте = этаж вверх, присед = вниз, /trigger go set N = сразу на этаж N. Плавная езда.",
                new String[]{"Этажей", "Высота", "Скорость"}, new int[]{2, 4, 1}, new int[]{14, 16, 4}, new int[]{6, 6, 2}, new int[]{1, 1, 1},
                (base, id, p) -> {
                    B b = new B(base, id);
                    int n = p[0], h = p[1], top = (n - 1) * h;
                    double step = STEPS[p[2] - 1];
                    b.obj("jmp", "minecraft.custom:minecraft.jump");
                    b.obj("snk", "minecraft.custom:minecraft.sneak_time");
                    b.obj("tgt", "dummy");
                    b.obj("go", "trigger");
                    b.obj("cd", "dummy");
                    b.setup("gamerule commandBlockOutput false");
                    b.box(0, 0, 0, 2, top + 3, 2, Blocks.AIR);
                    for (int[] c : new int[][]{{-1, -1}, {3, -1}, {-1, 3}, {3, 3}}) b.box(c[0], 0, c[1], c[0], top + 3, c[1], Blocks.QUARTZ_PILLAR);
                    for (int i = 0; i < n; i++) {
                        b.box(0, i * h - 1, 0, 2, i * h - 1, 2, Blocks.SMOOTH_QUARTZ);
                        b.put(1, i * h - 1, 1, Blocks.SEA_LANTERN);
                    }
                    String cd = b.o("cd"), jmp = b.o("jmp"), snk = b.o("snk"), tgt = b.o("tgt"), go = b.o("go");
                    String sh = "x=" + b.X(0) + ",y=-64,z=" + b.Z(0) + ",dx=2,dy=384,dz=2";
                    String cx = fd(b.X(0) + 1.5), cz = fd(b.Z(0) + 1.5);
                    String st = fd(step);
                    b.c("scoreboard players enable @a " + go);
                    b.c("scoreboard players add @a " + cd + " 0");
                    b.c("scoreboard players remove @a[" + sc(cd + "=1..") + "] " + cd + " 1");
                    b.c("execute as @a[" + sc(tgt + "=1..") + "] unless entity @s[" + sh + "] run scoreboard players set @s " + tgt + " 0");
                    for (int k = 1; k <= n; k++)
                        b.c("execute as @a[" + sh + "," + sc(go + "=" + k) + "] run scoreboard players set @s " + tgt + " " + k);
                    b.c("scoreboard players set @a " + go + " 0");
                    for (int i = 0; i < n - 1; i++) {
                        double y = base.getY() + i * h;
                        b.c("execute as @a[" + sh + "," + sc(jmp + "=1..", tgt + "=0", cd + "=0") + "] if entity @s[y=" + fd(y - 1.0) + ",dy=2.0] run scoreboard players set @s " + tgt + " " + (i + 2));
                    }
                    for (int i = 1; i < n; i++) {
                        double y = base.getY() + i * h;
                        b.c("execute as @a[" + sh + "," + sc(snk + "=1..", tgt + "=0", cd + "=0") + "] if entity @s[y=" + fd(y - 1.0) + ",dy=2.0] run scoreboard players set @s " + tgt + " " + i);
                    }
                    b.c("execute as @a[" + sc(tgt + "=1..") + "] run effect give @s minecraft:resistance 2 4 true");
                    b.c("execute as @a[" + sc(tgt + "=1..") + "] run effect give @s minecraft:slow_falling 2 0 true");
                    b.c("execute as @a[" + sc(tgt + "=1..") + "] at @s run particle minecraft:end_rod ~ ~1 ~ 0.3 0.5 0.3 0.02 4");
                    for (int i = 0; i < n; i++) {
                        double y = base.getY() + i * h;
                        String lo = "y=-64.0,dy=" + fd(y - 0.3 + 64), hi = "y=-64.0,dy=" + fd(y + 0.3 + 64);
                        String s = "execute as @a[" + sc(tgt + "=" + (i + 1)) + "] at @s ";
                        b.c(s + "if entity @s[" + lo + "] run tp @s " + cx + " ~" + st + " " + cz);
                        b.c(s + "unless entity @s[" + hi + "] run tp @s " + cx + " ~-" + st + " " + cz);
                    }
                    for (int i = 0; i < n; i++) {
                        double y = base.getY() + i * h;
                        String lo = "y=-64.0,dy=" + fd(y - 0.3 + 64), hi = "y=-64.0,dy=" + fd(y + 0.3 + 64);
                        String s = "execute as @a[" + sc(tgt + "=" + (i + 1)) + "] at @s if entity @s[" + hi + "] unless entity @s[" + lo + "] run ";
                        b.c(s + "title @s actionbar {'text':'Этаж " + (i + 1) + " из " + n + "','color':'aqua'}");
                        b.c(s + "playsound minecraft:block.note_block.chime master @s ~ ~ ~ 1 1.5");
                        b.c(s + "tp @s " + cx + " " + fd(y) + " " + cz);
                        b.c(s + "scoreboard players set @s " + cd + " 20");
                        b.c(s + "scoreboard players set @s " + tgt + " 0");
                    }
                    b.c("scoreboard players set @a " + jmp + " 0");
                    b.c("scoreboard players set @a " + snk + " 0");
                    b.pl.help = "Встань в шахту: прыжок = вверх, присед = вниз. Прямой вызов: /trigger " + go + " set N (N = этаж 1.." + n + ").";
                    return b.done(5, 0, -1);
                });

        // ===== СЕТЬ ТЕЛЕПОРТОВ =====
        reg("tele", "Лифты", "Сеть телепортов",
                "Кольцо площадок. Прыжок на площадке = на следующую, присед = на предыдущую. Эффекты и звук.",
                new String[]{"Площадок", "Радиус"}, new int[]{2, 4}, new int[]{8, 40}, new int[]{4, 12}, new int[]{1, 2},
                (base, id, p) -> {
                    B b = new B(base, id);
                    int n = p[0], r = p[1];
                    int[] px = new int[n], pz = new int[n];
                    for (int i = 0; i < n; i++) {
                        px[i] = (int) Math.round(r * Math.cos(2 * Math.PI * i / n));
                        pz[i] = (int) Math.round(r * Math.sin(2 * Math.PI * i / n));
                        b.box(px[i] - 1, 0, pz[i] - 1, px[i] + 1, 3, pz[i] + 1, Blocks.AIR);
                        b.box(px[i] - 1, -1, pz[i] - 1, px[i] + 1, -1, pz[i] + 1, Blocks.POLISHED_BLACKSTONE);
                        b.put(px[i], -1, pz[i], Blocks.CRYING_OBSIDIAN);
                    }
                    b.obj("jmp", "minecraft.custom:minecraft.jump");
                    b.obj("snk", "minecraft.custom:minecraft.sneak_time");
                    b.obj("cd", "dummy");
                    String cd = b.o("cd"), jmp = b.o("jmp"), snk = b.o("snk"), tf = b.id + "_f", tb = b.id + "_b";
                    b.c("scoreboard players add @a " + cd + " 0");
                    b.c("scoreboard players remove @a[" + sc(cd + "=1..") + "] " + cd + " 1");
                    String[] sel = new String[n];
                    for (int i = 0; i < n; i++)
                        sel[i] = "x=" + b.X(px[i] - 1) + ",y=" + fd(base.getY() - 0.5) + ",z=" + b.Z(pz[i] - 1) + ",dx=2,dy=2.0,dz=2";
                    for (int i = 0; i < n; i++) {
                        b.c("execute as @a[" + sel[i] + "," + sc(jmp + "=1..", cd + "=0") + "] run tag @s add " + tf);
                        b.c("execute as @a[" + sel[i] + "," + sc(snk + "=1..", cd + "=0") + "] run tag @s add " + tb);
                    }
                    b.c("execute as @a[tag=" + tf + "] at @s run particle minecraft:portal ~ ~1 ~ 0.3 0.6 0.3 0.5 40");
                    b.c("execute as @a[tag=" + tb + "] at @s run particle minecraft:portal ~ ~1 ~ 0.3 0.6 0.3 0.5 40");
                    for (int i = 0; i < n; i++) {
                        int nx = (i + 1) % n, pv = (i - 1 + n) % n;
                        b.c("execute as @a[tag=" + tf + "," + sel[i] + "] run tp @s " + fd(b.X(px[nx]) + 0.5) + " " + fd(base.getY()) + " " + fd(b.Z(pz[nx]) + 0.5));
                        b.c("execute as @a[tag=" + tb + "," + sel[i] + "] run tp @s " + fd(b.X(px[pv]) + 0.5) + " " + fd(base.getY()) + " " + fd(b.Z(pz[pv]) + 0.5));
                    }
                    b.c("execute as @a[tag=" + tf + "] at @s run playsound minecraft:entity.enderman.teleport master @a ~ ~ ~ 1 1");
                    b.c("execute as @a[tag=" + tb + "] at @s run playsound minecraft:entity.enderman.teleport master @a ~ ~ ~ 1 1");
                    b.c("scoreboard players set @a[tag=" + tf + "] " + cd + " 20");
                    b.c("scoreboard players set @a[tag=" + tb + "] " + cd + " 20");
                    b.c("tag @a remove " + tf);
                    b.c("tag @a remove " + tb);
                    b.c("scoreboard players set @a " + jmp + " 0");
                    b.c("scoreboard players set @a " + snk + " 0");
                    b.pl.help = "Встань на чёрную площадку: прыжок = на следующую, присед = на предыдущую.";
                    return b.done(-8, 0, -r - 6);
                });

        // ===== ВОРОТА =====
        for (boolean keyed : new boolean[]{false, true}) {
            reg(keyed ? "gate_key" : "gate", "Двери и защита", keyed ? "Ворота по ключу" : "Умные ворота",
                    keyed ? "Открываются только для игроков с меткой. Тебе метка выдаётся; другим: /tag Имя add (метка из справки)."
                            : "Автоворота из камня и решёток: открываются, когда игрок рядом, закрываются сами.",
                    new String[]{"Ширина", "Высота", "Радиус"}, new int[]{3, 3, 2}, new int[]{9, 6, 8}, new int[]{5, 4, 4}, new int[]{1, 1, 1},
                    (base, id, p) -> {
                        B b = new B(base, id);
                        int w = p[0], h = p[1], rad = Math.max(p[2], w / 2 + 2);
                        b.box(-1, 0, 0, -1, h, 0, Blocks.STONE_BRICKS);
                        b.box(w, 0, 0, w, h, 0, Blocks.STONE_BRICKS);
                        b.box(-1, h, 0, w, h, 0, Blocks.STONE_BRICKS);
                        b.box(0, 0, 0, w - 1, h - 1, 0, Blocks.IRON_BARS);
                        String who = keyed ? "@a[distance=.." + rad + ",tag=" + id + "_ok]" : "@a[distance=.." + rad + "]";
                        if (keyed) b.setup("tag @s add " + id + "_ok");
                        String pos = "positioned " + fd(b.X(0) + w / 2.0) + " " + fd(b.Y(0) + h / 2.0) + " " + fd(b.Z(0) + 0.5);
                        String f1 = b.X(0) + " " + b.Y(0) + " " + b.Z(0), f2 = b.X(w - 1) + " " + b.Y(h - 1) + " " + b.Z(0);
                        b.c("execute " + pos + " if entity " + who + " if block " + f1 + " minecraft:iron_bars run playsound minecraft:block.iron_door.open master @a ~ ~ ~ 1 1");
                        b.c("execute " + pos + " if entity " + who + " run fill " + f1 + " " + f2 + " minecraft:air replace minecraft:iron_bars");
                        b.c("execute " + pos + " unless entity " + who + " if block " + f1 + " minecraft:air run playsound minecraft:block.iron_door.close master @a ~ ~ ~ 1 1");
                        b.c("execute " + pos + " unless entity " + who + " run fill " + f1 + " " + f2 + " minecraft:iron_bars replace minecraft:air");
                        b.pl.help = keyed ? "Метка доступа: " + id + "_ok. Другу: /tag Имя add " + id + "_ok" : "Подойди к воротам — они откроются сами.";
                        return b.done(w + 3, 0, 0);
                    });
        }

        // ===== СЕЙФ С КОДОМ =====
        reg("vault", "Двери и защита", "Сейф с кодом",
                "Бронированная дверь 3x3. Код вводится командой /trigger. Открыта заданное время, потом закрывается сама.",
                new String[]{"Цифра 1", "Цифра 2", "Цифра 3", "Цифра 4"}, new int[]{1, 0, 0, 0}, new int[]{9, 9, 9, 9}, new int[]{1, 2, 3, 4}, new int[]{1, 1, 1, 1},
                (base, id, p) -> {
                    B b = new B(base, id);
                    int code = p[0] * 1000 + p[1] * 100 + p[2] * 10 + p[3];
                    b.obj("code", "trigger");
                    b.obj("t", "dummy");
                    b.setup("scoreboard players set #open " + b.o("t") + " 0");
                    b.box(-1, -1, 0, 3, 3, 0, Blocks.POLISHED_DEEPSLATE);
                    b.box(0, 0, 0, 2, 2, 0, Blocks.IRON_BLOCK);
                    b.put(-2, 0, -1, Blocks.CHISELED_DEEPSLATE);
                    String cd = b.o("code"), t = b.o("t");
                    String f1 = b.X(0) + " " + b.Y(0) + " " + b.Z(0), f2 = b.X(2) + " " + b.Y(2) + " " + b.Z(0);
                    String ctr = "positioned " + fd(b.X(0) + 1.5) + " " + fd(b.Y(0) + 1.5) + " " + fd(b.Z(0) + 0.5);
                    b.c("scoreboard players enable @a " + cd);
                    b.c("execute as @a[" + sc(cd + "=" + code) + "] run scoreboard players set #open " + t + " 100");
                    b.c("execute as @a[" + sc(cd + "=" + code) + "] run title @s actionbar {'text':'Сейф открыт','color':'green'}");
                    b.c("execute as @a[" + sc(cd + "=" + code) + "] at @s run playsound minecraft:block.iron_door.open master @s ~ ~ ~ 1 1");
                    b.c("execute as @a[" + sc(cd + "=1..") + "] unless score @s " + cd + " matches " + code + " run title @s actionbar {'text':'Неверный код','color':'red'}");
                    b.c("scoreboard players set @a " + cd + " 0");
                    b.c("execute if score #open " + t + " matches 1.. run fill " + f1 + " " + f2 + " minecraft:air replace minecraft:iron_block");
                    b.c("execute if score #open " + t + " matches 1.. run scoreboard players remove #open " + t + " 1");
                    b.c("execute " + ctr + " if score #open " + t + " matches 0 unless entity @a[distance=..2.5] run fill " + f1 + " " + f2 + " minecraft:iron_block replace minecraft:air");
                    b.pl.help = "Код: " + code + ". Ввод: /trigger " + cd + " set " + code + ". Открыто 5 секунд.";
                    return b.done(5, 0, 0);
                });

        // ===== МАГИЧЕСКАЯ ФЕРМА =====
        reg("farm", "Хозяйство", "Автоферма",
                "Грядка с водой, мгновенный рост, автосбор урожая, пересадка и сбор в сундук через воронку.",
                new String[]{"Культура", "Размер", "Рост, сек"}, new int[]{1, 2, 5}, new int[]{4, 4, 120}, new int[]{1, 4, 30}, new int[]{1, 1, 5},
                (base, id, p) -> {
                    Block[] crops = {Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS};
                    String[] cn = {"wheat", "carrots", "potatoes", "beetroots"};
                    int[] age = {7, 7, 7, 3};
                    int ci = p[0] - 1, h = p[1], g = p[2] * 20;
                    B b = new B(base, id);
                    b.obj("t", "dummy");
                    b.setup("scoreboard players set #t " + b.o("t") + " 0");
                    b.box(-h - 1, 1, -h - 1, h + 3, 3, h + 1, Blocks.AIR);
                    b.box(-h - 1, -1, -h - 1, h + 1, -1, h + 1, Blocks.STONE_BRICKS);
                    BlockState farm = Blocks.FARMLAND.getDefaultState().with(FarmlandBlock.MOISTURE, 7);
                    for (int x = -h; x <= h; x++)
                        for (int z = -h; z <= h; z++) {
                            b.put(x, -1, z, farm);
                            if (x == 0 && z == 0) continue;
                            b.put(x, 0, z, crops[ci]);
                        }
                    b.put(0, -1, 0, Blocks.WATER);
                    b.put(0, 0, 0, Blocks.OAK_SLAB);
                    b.put(h + 2, -1, 0, Blocks.CHEST);
                    b.put(h + 2, 0, 0, Blocks.HOPPER);
                    String t = b.o("t");
                    String r1 = b.X(-h) + " " + b.Y(0) + " " + b.Z(-h), r2 = b.X(h) + " " + b.Y(0) + " " + b.Z(h);
                    String crop = "minecraft:" + cn[ci];
                    b.c("scoreboard players add #t " + t + " 1");
                    b.c("execute if score #t " + t + " matches " + g + " run fill " + r1 + " " + r2 + " " + crop + "[age=" + age[ci] + "] replace " + crop);
                    b.c("execute if score #t " + t + " matches " + (g + 10) + " run fill " + r1 + " " + r2 + " minecraft:air destroy");
                    b.c("execute if score #t " + t + " matches " + (g + 14) + " run fill " + r1 + " " + r2 + " " + crop + "[age=0] replace minecraft:air");
                    b.c("execute if score #t " + t + " matches " + (g + 16) + " run setblock " + b.X(0) + " " + b.Y(0) + " " + b.Z(0) + " minecraft:oak_slab");
                    b.c("execute if score #t " + t + " matches " + (g + 20) + ".. run scoreboard players set #t " + t + " 0");
                    b.c("execute positioned " + b.X(0) + " " + b.Y(0) + " " + b.Z(0) + " as @e[type=item,distance=.." + (h + 5) + "] run tp @s " + fd(b.X(h + 2) + 0.5) + " " + fd(b.Y(0) + 1.2) + " " + fd(b.Z(0) + 0.5));
                    b.pl.help = "Ферма работает сама: урожай вырастает каждые " + p[2] + " сек, собирается в сундук справа.";
                    return b.done(-h - 1, 0, h + 3);
                });

        // ===== АВТОБУР =====
        reg("miner", "Хозяйство", "Автобур",
                "Бурит шахту вниз слой за слоем, добыча улетает в сундук. Пуск: /trigger go set 1, стоп: set 2.",
                new String[]{"Глубина", "Радиус"}, new int[]{8, 0}, new int[]{80, 2}, new int[]{32, 1}, new int[]{8, 1},
                (base, id, p) -> {
                    B b = new B(base, id);
                    int r = p[1], d = Math.max(1, Math.min(p[0], base.getY() + 60));
                    b.obj("go", "trigger");
                    b.obj("t", "dummy");
                    b.setup("scoreboard players set #run " + b.o("t") + " 0");
                    b.setup("scoreboard players set #d " + b.o("t") + " 1");
                    b.setup("scoreboard players set #w " + b.o("t") + " 0");
                    b.box(-r - 1, -1, -r - 1, r + 1, -1, r + 1, Blocks.IRON_BLOCK);
                    b.box(-r, -1, -r, r, -1, r, Blocks.AIR);
                    b.put(r + 3, -1, 0, Blocks.CHEST);
                    b.put(r + 3, 0, 0, Blocks.HOPPER);
                    String go = b.o("go"), t = b.o("t");
                    b.c("scoreboard players enable @a " + go);
                    b.c("execute as @a[" + sc(go + "=1") + "] if score #run " + t + " matches 0 run scoreboard players set #d " + t + " 1");
                    b.c("execute as @a[" + sc(go + "=1") + "] run scoreboard players set #w " + t + " 0");
                    b.c("execute as @a[" + sc(go + "=1") + "] run scoreboard players set #run " + t + " 1");
                    b.c("execute as @a[" + sc(go + "=2") + "] run scoreboard players set #run " + t + " 0");
                    b.c("scoreboard players set @a " + go + " 0");
                    for (int i = 1; i <= d; i++)
                        b.c("execute if score #run " + t + " matches 1 if score #d " + t + " matches " + i + " run fill " + b.X(-r) + " " + b.Y(-i) + " " + b.Z(-r) + " " + b.X(r) + " " + b.Y(-i) + " " + b.Z(r) + " minecraft:air destroy");
                    b.c("execute if score #run " + t + " matches 1 run scoreboard players add #d " + t + " 1");
                    b.c("execute if score #run " + t + " matches 1 if score #d " + t + " matches " + (d + 1) + ".. run scoreboard players set #run " + t + " 2");
                    b.c("execute if score #run " + t + " matches 2 run scoreboard players add #w " + t + " 1");
                    b.c("execute if score #run " + t + " matches 2 if score #w " + t + " matches 160.. run scoreboard players set #run " + t + " 0");
                    b.c("execute if score #run " + t + " matches 1..2 positioned " + b.X(0) + " " + b.Y(-d / 2) + " " + b.Z(0) + " as @e[type=item,distance=.." + (d / 2 + 8) + "] run tp @s " + fd(b.X(r + 3) + 0.5) + " " + fd(b.Y(0) + 1.2) + " " + fd(b.Z(0) + 0.5));
                    b.pl.help = "Пуск: /trigger " + go + " set 1. Стоп: set 2. Глубина " + d + " слоёв, добыча в сундук рядом.";
                    return b.done(-2, 0, r + 3);
                });

        // ===== ТУРЕЛЬ =====
        reg("sentry", "Двери и защита", "Турель-защитник",
                "Башня, наносящая магический урон всем враждебным мобам в радиусе.",
                new String[]{"Радиус", "Урон", "Темп, тики"}, new int[]{6, 2, 5}, new int[]{24, 20, 40}, new int[]{12, 6, 10}, new int[]{2, 2, 5},
                (base, id, p) -> {
                    B b = new B(base, id);
                    int rad = p[0], dmg = p[1], rate = p[2];
                    b.obj("t", "dummy");
                    b.box(-1, -1, -1, 1, -1, 1, Blocks.STONE_BRICKS);
                    b.box(0, 0, 0, 0, 2, 0, Blocks.IRON_BLOCK);
                    b.put(0, 3, 0, Blocks.LIGHTNING_ROD);
                    String t = b.o("t");
                    String pos = "positioned " + fd(b.X(0) + 0.5) + " " + fd(b.Y(0) + 1.5) + " " + fd(b.Z(0) + 0.5);
                    b.c("scoreboard players add #t " + t + " 1");
                    for (String m : HOSTILE)
                        b.c("execute if score #t " + t + " matches " + rate + ".. " + pos + " as @e[type=minecraft:" + m + ",distance=.." + rad + "] at @s run damage @s " + dmg + " minecraft:magic");
                    b.c("execute if score #t " + t + " matches " + rate + ".. " + pos + " if entity @e[type=#minecraft:skeletons,distance=.." + rad + "] run playsound minecraft:entity.arrow.shoot master @a ~ ~ ~ 1 1.2");
                    b.c("execute if score #t " + t + " matches " + rate + ".. run scoreboard players set #t " + t + " 0");
                    b.pl.help = "Турель бьёт " + HOSTILE.length + " видов враждебных мобов в радиусе " + rad + ".";
                    return b.done(-8, 0, -4);
                });

        // ===== КУПОЛ =====
        reg("dome", "Двери и защита", "Защитный купол",
                "Стеклянный купол. Внутри: лечение, сытость и мгновенное удаление враждебных мобов.",
                new String[]{"Радиус"}, new int[]{5}, new int[]{20}, new int[]{10}, new int[]{1},
                (base, id, p) -> {
                    B b = new B(base, id);
                    int r = p[0];
                    b.obj("t", "dummy");
                    for (int x = -r - 1; x <= r + 1; x++)
                        for (int y = 0; y <= r + 1; y++)
                            for (int z = -r - 1; z <= r + 1; z++) {
                                double dd = Math.sqrt(x * x + y * y + z * z);
                                if (dd >= r - 0.5 && dd <= r + 0.5 && !(z < 0 && Math.abs(x) <= 1 && y <= 2)) b.put(x, y, z, Blocks.GLASS);
                            }
                    String t = b.o("t");
                    String pos = "positioned " + fd(b.X(0) + 0.5) + " " + fd(b.Y(0)) + " " + fd(b.Z(0) + 0.5);
                    b.c("scoreboard players add #t " + t + " 1");
                    for (String m : HOSTILE)
                        b.c("execute if score #t " + t + " matches 20.. " + pos + " as @e[type=minecraft:" + m + ",distance=.." + (r - 1) + "] run kill @s");
                    b.c("execute if score #t " + t + " matches 20.. " + pos + " run effect give @a[distance=.." + (r - 1) + "] minecraft:regeneration 5 0 true");
                    b.c("execute if score #t " + t + " matches 20.. " + pos + " run effect give @a[distance=.." + (r - 1) + "] minecraft:saturation 5 0 true");
                    b.c("execute if score #t " + t + " matches 20.. run scoreboard players set #t " + t + " 0");
                    b.pl.help = "Вход с севера (проём 3x3). Внутри купола враждебные мобы исчезают.";
                    return b.done(-8, 0, -r - 4);
                });

        // ===== ВОЛНЫ МОБОВ =====
        reg("wave", "Игры", "Арена волн",
                "Бесконечные волны мобов: зомби, потом скелеты, пауки, криперы, ведьмы. Награда каждые 5 волн. Пуск: /trigger go set 1.",
                new String[]{"Пауза, сек", "Мобов", "Радиус"}, new int[]{5, 1, 4}, new int[]{120, 8, 12}, new int[]{30, 4, 7}, new int[]{5, 1, 1},
                (base, id, p) -> {
                    B b = new B(base, id);
                    int iv = p[0] * 20, cnt = p[1], r = p[2];
                    b.obj("go", "trigger");
                    b.obj("t", "dummy");
                    b.setup("scoreboard players set #run " + b.o("t") + " 0");
                    b.setup("scoreboard players set #w " + b.o("t") + " 0");
                    b.setup("scoreboard players set #t " + b.o("t") + " 0");
                    for (int a = 0; a < 360; a += Math.max(3, 180 / r)) {
                        int x = (int) Math.round((r + 2) * Math.cos(Math.toRadians(a))), z = (int) Math.round((r + 2) * Math.sin(Math.toRadians(a)));
                        b.put(x, 0, z, Blocks.STONE_BRICKS);
                    }
                    b.put(0, -1, 0, Blocks.CRYING_OBSIDIAN);
                    String go = b.o("go"), t = b.o("t");
                    b.c("scoreboard players enable @a " + go);
                    b.c("execute as @a[" + sc(go + "=1") + "] run scoreboard players set #run " + t + " 1");
                    b.c("execute as @a[" + sc(go + "=1") + "] run scoreboard players set #t " + t + " " + (iv - 60));
                    b.c("execute as @a[" + sc(go + "=2") + "] run scoreboard players set #run " + t + " 0");
                    b.c("scoreboard players set @a " + go + " 0");
                    b.c("execute if score #run " + t + " matches 1 run scoreboard players add #t " + t + " 1");
                    b.c("execute if score #t " + t + " matches " + iv + ".. run scoreboard players add #w " + t + " 1");
                    String on = "execute if score #t " + t + " matches " + iv + ".. ";
                    String cen = "positioned " + fd(b.X(0) + 0.5) + " " + fd(b.Y(0)) + " " + fd(b.Z(0) + 0.5);
                    for (int k = 0; k < cnt; k++) {
                        double a = 2 * Math.PI * k / cnt;
                        String at = fd(b.X(0) + 0.5 + (r - 1) * Math.cos(a)) + " " + fd(b.Y(0)) + " " + fd(b.Z(0) + 0.5 + (r - 1) * Math.sin(a));
                        b.c(on + "run summon minecraft:zombie " + at);
                        b.c(on + "if score #w " + t + " matches 3.. run summon minecraft:skeleton " + at);
                        b.c(on + "if score #w " + t + " matches 5.. run summon minecraft:spider " + at);
                        b.c(on + "if score #w " + t + " matches 7.. run summon minecraft:creeper " + at);
                    }
                    b.c(on + "if score #w " + t + " matches 10.. " + cen + " run summon minecraft:witch ~ ~ ~");
                    for (int w : new int[]{5, 10, 15, 20})
                        b.c(on + "if score #w " + t + " matches " + w + " " + cen + " run summon item ~ ~1 ~ {Item:{id:'minecraft:diamond',Count:" + (w / 5 * 2) + "b}}");
                    b.c(on + "run tellraw @a[distance=..60] ['',{'text':'Волна ','color':'gold'},{'score':{'name':'#w','objective':'" + t + "'},'color':'yellow'}]");
                    b.c(on + "run scoreboard players set #t " + t + " 0");
                    b.pl.help = "Пуск: /trigger " + go + " set 1, стоп: set 2. Награда (алмазы) каждые 5 волн в центре арены.";
                    return b.done(-8, 0, r + 4);
                });

        // ===== КОНВЕЙЕР =====
        reg("belt", "Транспорт", "Конвейер-эскалатор",
                "Лента везёт игроков, мобов и предметы. Переключение направления: /trigger go set 1.",
                new String[]{"Длина", "Скорость", "Ширина"}, new int[]{4, 1, 1}, new int[]{48, 4, 3}, new int[]{16, 2, 2}, new int[]{1, 1, 1},
                (base, id, p) -> {
                    B b = new B(base, id);
                    int len = p[0], w = p[2];
                    String st = fd(0.1 * p[1]);
                    b.obj("go", "trigger");
                    b.obj("t", "dummy");
                    b.setup("scoreboard players set #dir " + b.o("t") + " 1");
                    for (int x = 0; x < len; x++)
                        for (int z = 0; z < w; z++) b.put(x, -1, z, x % 2 == 0 ? Blocks.BLACK_CONCRETE : Blocks.YELLOW_CONCRETE);
                    b.box(0, 0, 0, len - 1, 3, w - 1, Blocks.AIR);
                    String go = b.o("go"), t = b.o("t");
                    String vol = "@e[type=!minecraft:marker,x=" + b.X(0) + ",y=" + b.Y(0) + ",z=" + b.Z(0) + ",dx=" + (len - 1) + ",dy=1,dz=" + (w - 1) + "]";
                    b.c("scoreboard players enable @a " + go);
                    b.c("execute if entity @a[" + sc(go + "=1") + "] if score #dir " + t + " matches 1 run scoreboard players set #dir " + t + " 3");
                    b.c("execute if entity @a[" + sc(go + "=1") + "] if score #dir " + t + " matches 2 run scoreboard players set #dir " + t + " 1");
                    b.c("execute if entity @a[" + sc(go + "=1") + "] if score #dir " + t + " matches 3 run scoreboard players set #dir " + t + " 2");
                    b.c("scoreboard players set @a " + go + " 0");
                    b.c("execute if score #dir " + t + " matches 1 as " + vol + " at @s run tp @s ~" + st + " ~ ~");
                    b.c("execute if score #dir " + t + " matches 2 as " + vol + " at @s run tp @s ~-" + st + " ~ ~");
                    b.pl.help = "Лента идёт вдоль +X. Смена направления: /trigger " + go + " set 1.";
                    return b.done(0, 0, w + 1);
                });

        // ===== СВЕТОВОЕ ШОУ =====
        reg("show", "Игры", "Световое шоу",
                "Фейерверки, цветные частицы и звуки по сценарию 10 сцен. Старт/стоп: /trigger go set 1.",
                new String[]{"Радиус"}, new int[]{3}, new int[]{16}, new int[]{8}, new int[]{1},
                (base, id, p) -> {
                    B b = new B(base, id);
                    int r = p[0];
                    b.obj("go", "trigger");
                    b.obj("t", "dummy");
                    b.setup("scoreboard players set #run " + b.o("t") + " 0");
                    b.setup("scoreboard players set #t " + b.o("t") + " 0");
                    for (int[] c : new int[][]{{r, 0}, {-r, 0}, {0, r}, {0, -r}}) {
                        b.put(c[0], 0, c[1], Blocks.IRON_BLOCK);
                        b.put(c[0], 1, c[1], Blocks.SEA_LANTERN);
                    }
                    b.put(0, -1, 0, Blocks.BEACON);
                    String go = b.o("go"), t = b.o("t");
                    int[] cols = {16711680, 16776960, 65280, 65535, 255, 16711935, 16744448, 16777215, 11743532, 2437522};
                    String[] rgb = {"1 0 0", "1 1 0", "0 1 0", "0 1 1", "0 0.2 1", "1 0 1", "1 0.5 0", "1 1 1", "0.9 0.2 0.2", "0.2 0.8 0.5"};
                    b.c("scoreboard players enable @a " + go);
                    b.c("execute as @a[" + sc(go + "=1") + "] run scoreboard players set #run " + t + " 1");
                    b.c("execute as @a[" + sc(go + "=2") + "] run scoreboard players set #run " + t + " 0");
                    b.c("scoreboard players set @a " + go + " 0");
                    b.c("execute if score #run " + t + " matches 1 run scoreboard players add #t " + t + " 1");
                    for (int s = 0; s < 10; s++) {
                        String on = "execute if score #run " + t + " matches 1 if score #t " + t + " matches " + (s * 20 + 1) + " ";
                        double a = 2 * Math.PI * s / 10;
                        String at = fd(b.X(0) + 0.5 + r * 0.7 * Math.cos(a)) + " " + fd(b.Y(0) + 1) + " " + fd(b.Z(0) + 0.5 + r * 0.7 * Math.sin(a));
                        b.c(on + "run summon minecraft:firework_rocket " + at + " {LifeTime:24,FireworksItem:{id:'minecraft:firework_rocket',Count:1,tag:{Fireworks:{Explosions:[{Type:1,Colors:[I;" + cols[s] + "],FadeColors:[I;" + cols[(s + 3) % 10] + "]}]}}}}");
                        b.c(on + "run particle minecraft:dust " + rgb[s] + " 1.5 " + fd(b.X(0) + 0.5) + " " + fd(b.Y(0) + 1) + " " + fd(b.Z(0) + 0.5) + " " + r + " 0.5 " + r + " 0 60 force");
                        b.c(on + "run playsound minecraft:block.note_block.bell master @a[distance=..64] " + fd(b.X(0) + 0.5) + " " + b.Y(0) + " " + fd(b.Z(0) + 0.5) + " 2 " + fd(0.6 + s * 0.15));
                    }
                    b.c("execute if score #t " + t + " matches 200.. run scoreboard players set #t " + t + " 0");
                    b.pl.help = "Старт: /trigger " + go + " set 1, стоп: set 2. Цикл 10 сцен.";
                    return b.done(-r - 4, 0, -2);
                });

        // ===== МЕТЕОПАНЕЛЬ =====
        reg("weather", "Игры", "Метеопанель",
                "Пульт погоды и времени: /trigger go set 1..9 (ясно, дождь, гроза, утро, день, полдень, вечер, ночь, полночь).",
                new String[]{"Размер"}, new int[]{1}, new int[]{1}, new int[]{1}, new int[]{1},
                (base, id, p) -> {
                    B b = new B(base, id);
                    b.obj("go", "trigger");
                    b.box(-1, -1, -1, 1, -1, 1, Blocks.CHISELED_STONE_BRICKS);
                    b.put(0, 0, 0, Blocks.LECTERN);
                    String go = b.o("go");
                    String[] cmd = {"weather clear", "weather rain", "weather thunder", "time set 0", "time set 1000", "time set 6000", "time set 12000", "time set 13000", "time set 18000"};
                    String[] nm = {"Ясно", "Дождь", "Гроза", "Утро", "День", "Полдень", "Вечер", "Ночь", "Полночь"};
                    b.c("scoreboard players enable @a " + go);
                    for (int i = 0; i < 9; i++) {
                        b.c("execute as @a[" + sc(go + "=" + (i + 1)) + "] run " + cmd[i]);
                        b.c("execute as @a[" + sc(go + "=" + (i + 1)) + "] run title @s actionbar {'text':'" + nm[i] + "','color':'yellow'}");
                    }
                    b.c("scoreboard players set @a " + go + " 0");
                    b.pl.help = "/trigger " + go + " set N: 1 ясно, 2 дождь, 3 гроза, 4 утро, 5 день, 6 полдень, 7 вечер, 8 ночь, 9 полночь.";
                    return b.done(-8, 0, -3);
                });

        // ===== ДОМА =====
        reg("homes", "Игры", "Система домов",
                "Личные точки дома для каждого игрока: /trigger hset set 1 запоминает место, /trigger hgo set 1 телепортирует.",
                new String[]{"Размер"}, new int[]{1}, new int[]{1}, new int[]{1}, new int[]{1},
                (base, id, p) -> {
                    B b = new B(base, id);
                    b.obj("hset", "trigger");
                    b.obj("hgo", "trigger");
                    b.obj("uid", "dummy");
                    b.obj("t", "dummy");
                    b.setup("scoreboard players set #n " + b.o("t") + " 0");
                    b.put(0, -1, 0, Blocks.LODESTONE);
                    String hs = b.o("hset"), hg = b.o("hgo"), uid = b.o("uid"), t = b.o("t"), me = id + "_me", mk = id + "_h";
                    b.c("scoreboard players enable @a " + hs);
                    b.c("scoreboard players enable @a " + hg);
                    b.c("execute as @a unless score @s " + uid + " matches 0.. run scoreboard players add #n " + t + " 1");
                    b.c("execute as @a unless score @s " + uid + " matches 0.. run scoreboard players operation @s " + uid + " = #n " + t);
                    b.c("execute as @a[" + sc(hs + "=1") + "] run tag @s add " + me);
                    b.c("execute as @e[type=minecraft:marker,tag=" + mk + "] if score @s " + uid + " = @a[tag=" + me + ",limit=1] " + uid + " run kill @s");
                    b.c("execute as @a[tag=" + me + "] at @s run summon minecraft:marker ~ ~ ~ {Tags:['" + mk + "','" + id + "_new']}");
                    b.c("execute as @e[type=minecraft:marker,tag=" + id + "_new] run scoreboard players operation @s " + uid + " = @a[tag=" + me + ",limit=1] " + uid);
                    b.c("tag @e[type=minecraft:marker,tag=" + id + "_new] remove " + id + "_new");
                    b.c("execute as @a[tag=" + me + "] run title @s actionbar {'text':'Дом сохранён','color':'green'}");
                    b.c("tag @a remove " + me);
                    b.c("execute as @a[" + sc(hg + "=1") + "] run tag @s add " + me);
                    b.c("execute as @e[type=minecraft:marker,tag=" + mk + "] if score @s " + uid + " = @a[tag=" + me + ",limit=1] " + uid + " run tag @s add " + id + "_mine");
                    b.c("execute as @a[tag=" + me + "] at @e[type=minecraft:marker,tag=" + id + "_mine,limit=1] run tp @s ~ ~ ~");
                    b.c("execute as @a[tag=" + me + "] at @s run playsound minecraft:entity.enderman.teleport master @s ~ ~ ~ 1 1");
                    b.c("tag @e[type=minecraft:marker] remove " + id + "_mine");
                    b.c("tag @a remove " + me);
                    b.c("scoreboard players set @a " + hs + " 0");
                    b.c("scoreboard players set @a " + hg + " 0");
                    b.pl.help = "Запомнить: /trigger " + hs + " set 1. Домой: /trigger " + hg + " set 1. У каждого игрока свой дом.";
                    return b.done(-8, 0, -3);
                });

        // ===== УМНОЕ ОСВЕЩЕНИЕ =====
        reg("lamps", "Хозяйство", "Умные фонари",
                "Ряд фонарей, которые сами включаются ночью и гаснут днём.",
                new String[]{"Фонарей", "Шаг"}, new int[]{2, 3}, new int[]{12, 8}, new int[]{6, 4}, new int[]{1, 1},
                (base, id, p) -> {
                    B b = new B(base, id);
                    int n = p[0], sp = p[1];
                    b.obj("t", "dummy");
                    String t = b.o("t");
                    b.c("execute store result score #d " + t + " run time query daytime");
                    for (int i = 0; i < n; i++) {
                        int x = i * sp;
                        b.box(x, 0, 0, x, 2, 0, Blocks.IRON_BARS);
                        b.put(x, 3, 0, Blocks.GLASS);
                        String at = b.X(x) + " " + b.Y(3) + " " + b.Z(0);
                        b.c("execute if score #d " + t + " matches 12500..23500 run setblock " + at + " minecraft:sea_lantern");
                        b.c("execute unless score #d " + t + " matches 12500..23500 run setblock " + at + " minecraft:glass");
                    }
                    b.pl.help = "Фонари включаются с 12500 до 23500 тика (ночь).";
                    return b.done(0, 0, 3);
                });
    }

    // ------------------------------------------------------------ строительство (одиночная игра)
    private record Old(BlockPos p, BlockState s) { }
    private static final Deque<List<Old>> HIST = new ArrayDeque<>();

    public static void build(MinecraftClient mc, Def d, int[] prm, BlockPos base) {
        IntegratedServer srv = mc.getServer();
        if (srv == null || mc.world == null) { Editor.msg("Конструктор работает только в одиночной игре"); return; }
        ServerWorld w = srv.getWorld(mc.world.getRegistryKey());
        if (w == null) return;
        final Plan pl = d.gen().make(base, newId(), prm);
        if (pl.puts.size() > 12000) { Editor.msg("Слишком большая конструкция: " + pl.puts.size() + " блоков"); return; }
        for (String s : pl.setup) Editor.cmd(mc, s);
        srv.execute(() -> {
            List<Old> old = new ArrayList<>();
            for (Put pt : pl.puts) {
                BlockPos p = base.add(pt.x(), pt.y(), pt.z());
                old.add(new Old(p, w.getBlockState(p)));
                if (pt.cmd() == null) {
                    w.setBlockState(p, pt.st(), 3);
                } else {
                    Block cb = pt.repeat() ? Blocks.REPEATING_COMMAND_BLOCK : Blocks.CHAIN_COMMAND_BLOCK;
                    w.setBlockState(p, cb.getDefaultState().with(CommandBlock.FACING, pt.f()), 3);
                    if (w.getBlockEntity(p) instanceof CommandBlockBlockEntity be) {
                        be.getCommandExecutor().setCommand(pt.cmd());
                        be.setAuto(true);
                        be.markDirty();
                    }
                }
            }
            HIST.push(old);
            while (HIST.size() > 6) HIST.removeLast();
        });
        Editor.msg(d.name() + ": блоков " + pl.puts.size() + ", из них КБ " + pl.cb);
        if (!pl.help.isEmpty()) Editor.say(pl.help);
    }

    public static void undo(MinecraftClient mc) {
        IntegratedServer srv = mc.getServer();
        if (srv == null || mc.world == null || HIST.isEmpty()) { Editor.msg("Нечего отменять"); return; }
        ServerWorld w = srv.getWorld(mc.world.getRegistryKey());
        if (w == null) return;
        final List<Old> old = HIST.pop();
        srv.execute(() -> {
            for (int i = old.size() - 1; i >= 0; i--) w.setBlockState(old.get(i).p(), old.get(i).s(), 3);
        });
        Editor.msg("Последняя конструкция отменена (" + old.size() + " блоков)");
    }

    private Rig() { }
}
