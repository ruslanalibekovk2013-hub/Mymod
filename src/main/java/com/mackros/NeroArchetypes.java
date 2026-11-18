package com.mackros;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Локальные «заготовки-проекты» для открытых запросов без интернета: корабль, робот, мост, поезд, компьютер, портал.
 * Каждая собирается из блоков + кнопок/цепочек командных блоков с подписями. Это вымышленные игровые конструкции Minecraft.
 */
final class NeroArchetypes {
    static String detect(String t) {
        if (NeroFree.has(t, "космическ\\w* корабл|звездолет|корабл|ракет|шаттл|самолет|звездн\\w* истребител")) return "ship";
        if (NeroFree.has(t, "робот|голем|андроид")) return "robot";
        if (NeroFree.has(t, "мост")) return "bridge";
        if (NeroFree.has(t, "поезд|электричк|вагон|метро|локомотив|железн\\w* дорог")) return "train";
        if (NeroFree.has(t, "компьютер|ноутбук|монитор")) return "computer";
        if (NeroFree.has(t, "портал")) return "portal";
        return null;
    }

    static String nameOf(String k) {
        return switch (k) { case "ship" -> "корабль"; case "robot" -> "робот"; case "bridge" -> "мост"; case "train" -> "поезд"; case "computer" -> "компьютер"; case "portal" -> "портал"; default -> k; };
    }

    static String works(String k) {
        return switch (k) {
            case "ship" -> "работают двигатели (эффекты выхлопа) и люк на пульте; корабль неподвижный";
            case "robot" -> "работают поднятие рук и глаза на пульте";
            case "bridge" -> "это конструкция без механизмов (механизмы мосту не нужны)";
            case "train" -> "работают пути с питанием и выпуск вагонеток на пульте; состав из блоков в Minecraft двигаться не может, он стоит моделью";
            case "computer" -> "работают экран и заставка на пульте";
            case "portal" -> "работает телепортация между двумя арками (включается на пульте)";
            default -> "";
        };
    }

    static void build(NeroPlanner.Ctx c, String key, String t) {
        B b = new B();
        switch (key) {
            case "ship" -> ship(b);
            case "robot" -> robot(b);
            case "bridge" -> bridge(b, t);
            case "train" -> train(b);
            case "computer" -> computer(b);
            case "portal" -> portal(b);
            default -> { }
        }
        NeroFree.blueprint(c, b.done());
    }

    private static final class B {
        final JsonArray ops = new JsonArray(), chains = new JsonArray(), setup = new JsonArray(), buttons = new JsonArray();
        String help = "";

        private static String nm(String b) { return b.contains(":") ? b : "minecraft:" + b; }

        void box(int x1, int y1, int z1, int x2, int y2, int z2, String blk, String mode) {
            JsonObject o = new JsonObject();
            o.addProperty("op", "box");
            o.addProperty("x1", x1); o.addProperty("y1", y1); o.addProperty("z1", z1);
            o.addProperty("x2", x2); o.addProperty("y2", y2); o.addProperty("z2", z2);
            o.addProperty("block", nm(blk));
            o.addProperty("mode", mode);
            ops.add(o);
        }

        void box(int x1, int y1, int z1, int x2, int y2, int z2, String blk) { box(x1, y1, z1, x2, y2, z2, blk, "solid"); }
        void hol(int x1, int y1, int z1, int x2, int y2, int z2, String blk) { box(x1, y1, z1, x2, y2, z2, blk, "hollow"); }
        void air(int x1, int y1, int z1, int x2, int y2, int z2) { box(x1, y1, z1, x2, y2, z2, "air", "solid"); }

        void set(int x, int y, int z, String blk) {
            JsonObject o = new JsonObject();
            o.addProperty("op", "set");
            o.addProperty("x", x); o.addProperty("y", y); o.addProperty("z", z);
            o.addProperty("block", nm(blk));
            ops.add(o);
        }

        void line(int x1, int y1, int z1, int x2, int y2, int z2, String blk) {
            JsonObject o = new JsonObject();
            o.addProperty("op", "line");
            o.addProperty("x1", x1); o.addProperty("y1", y1); o.addProperty("z1", z1);
            o.addProperty("x2", x2); o.addProperty("y2", y2); o.addProperty("z2", z2);
            o.addProperty("block", nm(blk));
            ops.add(o);
        }

        void btn(String label, String... cmds) {
            JsonObject o = new JsonObject();
            o.addProperty("label", label);
            o.addProperty("command", String.join("\n", cmds));
            buttons.add(o);
        }

        void setup(String... cmds) { for (String s : cmds) setup.add(s); }

        /** steps: {подпись, команда, описание}. */
        void chain(String name, String[]... steps) {
            JsonObject o = new JsonObject();
            o.addProperty("name", name);
            JsonArray a = new JsonArray();
            for (String[] s : steps) {
                JsonObject st = new JsonObject();
                st.addProperty("label", s[0]);
                st.addProperty("cmd", s[1]);
                if (s.length > 2) st.addProperty("role", s[2]);
                a.add(st);
            }
            o.add("commands", a);
            chains.add(o);
        }

        JsonObject done() {
            JsonObject bp = new JsonObject();
            bp.add("ops", ops);
            bp.add("chains", chains);
            bp.add("setup", setup);
            bp.add("buttons", buttons);
            bp.addProperty("help", help);
            return bp;
        }
    }

    // ------------------------------------------------------------------ космический корабль
    private static void ship(B b) {
        b.box(4, 0, 2, 10, 2, 26, "light_gray_concrete");
        b.hol(3, 1, 4, 11, 6, 24, "white_concrete");
        b.air(4, 2, 5, 10, 5, 23);
        for (int k = 0; k < 8; k++) { int hw = 3 - k / 3; b.box(7 - hw, 1, 25 + k, 7 + hw, Math.max(2, 5 - k / 2), 25 + k, "white_concrete"); }
        b.box(5, 5, 19, 9, 6, 26, "light_blue_stained_glass");
        b.air(6, 5, 20, 8, 5, 24);
        for (int k = 0; k < 7; k++) { b.box(2 - k, 2, 8 + k, 3, 2, 16, "gray_concrete"); b.box(11, 2, 8 + k, 12 + k, 2, 16, "gray_concrete"); }
        b.box(7, 6, 2, 7, 11, 9, "white_concrete");
        for (int x : new int[]{5, 7, 9}) { b.box(x, 1, 0, x, 3, 1, "iron_block"); b.set(x, 2, -1, "magma_block"); }
        b.set(6, 2, 20, "oak_stairs[facing=south]");
        b.set(8, 2, 20, "oak_stairs[facing=south]");
        b.box(5, 2, 22, 9, 2, 22, "smooth_quartz");
        for (int z : new int[]{10, 16, 21}) b.set(7, 5, z, "sea_lantern");
        b.box(11, 2, 12, 11, 3, 12, "white_concrete");
        b.setup("scoreboard objectives add {id}_e dummy", "scoreboard players set #on {id}_e 0");
        b.chain("Двигатели",
                new String[]{"Выхлоп левого двигателя", "execute if score #on {id}_e matches 1 run particle minecraft:flame {x+5} {y+2} {z-1} 0.3 0.3 0.1 0.08 12", "Пока двигатели включены, показывает пламя из левого сопла"},
                new String[]{"Выхлоп центрального двигателя", "execute if score #on {id}_e matches 1 run particle minecraft:flame {x+7} {y+2} {z-1} 0.3 0.3 0.1 0.08 12", "Пока двигатели включены, показывает пламя из центрального сопла"},
                new String[]{"Выхлоп правого двигателя", "execute if score #on {id}_e matches 1 run particle minecraft:flame {x+9} {y+2} {z-1} 0.3 0.3 0.1 0.08 12", "Пока двигатели включены, показывает пламя из правого сопла"});
        b.btn("Двигатели: запуск", "scoreboard players set #on {id}_e 1", "playsound minecraft:entity.firework_rocket.launch block @a {x+7} {y+2} {z} 1 0.6");
        b.btn("Двигатели: стоп", "scoreboard players set #on {id}_e 0");
        b.btn("Люк: открыть", "fill {x+11} {y+2} {z+12} {x+11} {y+3} {z+12} minecraft:air");
        b.btn("Люк: закрыть", "fill {x+11} {y+2} {z+12} {x+11} {y+3} {z+12} minecraft:white_concrete");
        b.help = "Космический корабль: неподвижная модель. Работают двигатели (эффекты) и люк на правом борту.";
    }

    // ------------------------------------------------------------------ робот
    private static void robot(B b) {
        b.box(3, 0, 2, 5, 5, 4, "iron_block");
        b.box(7, 0, 2, 9, 5, 4, "iron_block");
        b.box(2, 0, 1, 5, 0, 5, "gray_concrete");
        b.box(7, 0, 1, 10, 0, 5, "gray_concrete");
        b.box(2, 6, 1, 10, 12, 5, "light_gray_concrete");
        b.box(5, 9, 1, 7, 11, 1, "black_concrete");
        b.set(6, 10, 1, "sea_lantern");
        b.box(3, 13, 1, 9, 18, 5, "iron_block");
        b.box(5, 14, 1, 7, 14, 1, "black_concrete");
        b.set(4, 16, 1, "black_concrete");
        b.set(8, 16, 1, "black_concrete");
        b.line(6, 19, 3, 6, 22, 3, "end_rod");
        b.set(6, 23, 3, "sea_lantern");
        b.box(-1, 12, 2, 1, 12, 4, "iron_block");
        b.box(11, 12, 2, 13, 12, 4, "iron_block");
        b.box(-1, 7, 2, 1, 11, 4, "iron_block");
        b.box(11, 7, 2, 13, 11, 4, "iron_block");
        b.btn("Левая рука: вверх", "fill {x-1} {y+7} {z+2} {x+1} {y+11} {z+4} minecraft:air", "fill {x-1} {y+13} {z+2} {x+1} {y+17} {z+4} minecraft:iron_block");
        b.btn("Левая рука: вниз", "fill {x-1} {y+13} {z+2} {x+1} {y+17} {z+4} minecraft:air", "fill {x-1} {y+7} {z+2} {x+1} {y+11} {z+4} minecraft:iron_block");
        b.btn("Правая рука: вверх", "fill {x+11} {y+7} {z+2} {x+13} {y+11} {z+4} minecraft:air", "fill {x+11} {y+13} {z+2} {x+13} {y+17} {z+4} minecraft:iron_block");
        b.btn("Правая рука: вниз", "fill {x+11} {y+13} {z+2} {x+13} {y+17} {z+4} minecraft:air", "fill {x+11} {y+7} {z+2} {x+13} {y+11} {z+4} minecraft:iron_block");
        b.btn("Глаза: включить", "setblock {x+4} {y+16} {z+1} minecraft:glowstone", "setblock {x+8} {y+16} {z+1} minecraft:glowstone");
        b.btn("Глаза: выключить", "setblock {x+4} {y+16} {z+1} minecraft:black_concrete", "setblock {x+8} {y+16} {z+1} minecraft:black_concrete");
        b.help = "Робот: неподвижная модель с функциональными частями. Руки поднимаются и опускаются, глаза включаются кнопками.";
    }

    // ------------------------------------------------------------------ мост
    private static void bridge(B b, String t) {
        int L = 36;
        Matcher m = Pattern.compile("(\\d{2,3})\\s*(блок|м\\b|метр)").matcher(t);
        if (m.find()) L = Math.max(16, Math.min(90, Integer.parseInt(m.group(1))));
        boolean hang = NeroFree.has(t, "подвесн|вантов|тросов");
        b.box(0, -1, 0, 6, -1, L, "stone_bricks");
        b.box(3, -1, 0, 3, -1, L, "smooth_stone");
        b.box(0, 0, 0, 0, 0, L, "stone_brick_wall");
        b.box(6, 0, 0, 6, 0, L, "stone_brick_wall");
        for (int z = 4; z < L; z += 8) { b.set(0, 1, z, "lantern[hanging=false]"); b.set(6, 1, z, "lantern[hanging=false]"); }
        for (int z = 6; z < L; z += 12) {
            b.box(1, -12, z, 2, -2, z + 1, "stone_bricks");
            b.box(4, -12, z, 5, -2, z + 1, "stone_bricks");
            b.box(1, -2, z, 5, -2, z + 1, "stone_bricks");
        }
        if (hang) {
            int z1 = L / 4, z2 = L * 3 / 4;
            for (int z : new int[]{z1, z2}) {
                b.box(-1, 0, z, 0, 15, z + 1, "iron_block");
                b.box(6, 0, z, 7, 15, z + 1, "iron_block");
                b.box(-1, 15, z, 7, 15, z + 1, "iron_block");
            }
            for (int x : new int[]{0, 6}) {
                b.line(x, 0, 0, x, 15, z1, "iron_bars");
                b.line(x, 15, z1, x, 15, z2, "iron_bars");
                b.line(x, 15, z2, x, 0, L, "iron_bars");
                for (int z = z1 + 4; z < z2; z += 4) b.line(x, 0, z, x, 15, z, "iron_bars");
            }
        } else {
            for (int z = 0; z + 12 <= L; z += 12) for (int k = 0; k <= 12; k++) {
                int h = (int) Math.round(3 * Math.sin(Math.PI * k / 12.0));
                b.set(0, -2 - h + 3, z + k, "stone_bricks");
                b.set(6, -2 - h + 3, z + k, "stone_bricks");
            }
        }
        b.help = "Мост длиной " + L + " блоков, " + (hang ? "подвесной, с башнями и тросами" : "балочный с арочными пролётами") + ". Механизмов нет: это конструкция.";
    }

    // ------------------------------------------------------------------ поезд
    private static void train(B b) {
        int L = 44;
        b.box(7, -2, 0, 9, -2, L, "gravel");
        for (int z = 0; z <= L; z++) b.set(8, -1, z, "powered_rail[shape=north_south]");
        for (int z = 0; z <= L; z += 6) b.set(9, -1, z, "redstone_block");
        b.box(7, -1, L + 1, 9, 0, L + 1, "red_concrete");
        b.box(2, -1, 4, 6, -1, 22, "smooth_stone");
        b.box(2, 5, 4, 6, 5, 22, "smooth_stone_slab");
        for (int z : new int[]{4, 13, 22}) { b.box(2, 0, z, 2, 4, z, "stripped_oak_log"); b.box(6, 0, z, 6, 4, z, "stripped_oak_log"); }
        for (int z : new int[]{8, 17}) b.set(4, 4, z, "lantern[hanging=true]");
        for (int z = 0; z <= L; z++) b.set(15, -1, z, "rail[shape=north_south]");
        b.box(14, 0, 4, 16, 3, 12, "blue_concrete");
        b.box(14, 4, 4, 16, 5, 7, "blue_concrete");
        b.box(15, 4, 10, 15, 6, 10, "black_concrete");
        b.set(15, 1, 13, "sea_lantern");
        for (int w = 0; w < 3; w++) {
            int z0 = 14 + w * 10;
            b.hol(14, 0, z0, 16, 3, z0 + 8, "red_concrete");
            b.box(14, 2, z0 + 1, 14, 2, z0 + 7, "light_blue_stained_glass");
            b.box(16, 2, z0 + 1, 16, 2, z0 + 7, "light_blue_stained_glass");
            b.set(15, 0, z0 - 1, "iron_bars");
        }
        b.btn("Вагонетка: выпустить", "execute positioned {x+8} {y-1} {z+1} run summon minecraft:minecart ~0.5 ~ ~0.5");
        b.btn("Состав: 3 вагонетки", "execute positioned {x+8} {y-1} {z+1} run summon minecraft:minecart ~0.5 ~ ~0.5",
                "execute positioned {x+8} {y-1} {z+3} run summon minecraft:minecart ~0.5 ~ ~0.5", "execute positioned {x+8} {y-1} {z+5} run summon minecraft:minecart ~0.5 ~ ~0.5");
        b.btn("Убрать вагонетки", "kill @e[type=minecraft:minecart,x={x+8},y={y},z={z+20},distance=..40]");
        b.help = "Поезд: путь с питанием, станция и состав-модель. В Minecraft блоки не ездят сами, поэтому ездят вагонетки: кнопка «Вагонетка» выпускает их на станции.";
    }

    // ------------------------------------------------------------------ компьютер
    private static void computer(B b) {
        b.box(5, 0, 2, 11, 0, 6, "gray_concrete");
        b.box(7, 1, 3, 9, 2, 5, "gray_concrete");
        b.box(0, 3, 3, 16, 13, 4, "black_concrete");
        b.box(1, 4, 2, 15, 12, 2, "black_concrete");
        b.box(3, -1, -1, 13, -1, 1, "gray_concrete");
        b.box(3, 0, 0, 13, 0, 0, "light_gray_concrete");
        for (int x = 4; x <= 12; x++) b.set(x, 1, 0, "stone_button[face=floor]");
        b.set(15, 0, 0, "polished_andesite");
        b.box(18, 0, 2, 21, 8, 6, "gray_concrete");
        b.set(19, 6, 2, "lime_concrete");
        b.set(20, 6, 2, "red_concrete");
        b.box(19, 1, 2, 20, 2, 2, "black_concrete");
        b.setup("scoreboard objectives add {id}_c dummy", "scoreboard players set #on {id}_c 0", "scoreboard players set #t {id}_c 0");
        String scr = "{x+1} {y+4} {z+2} {x+15} {y+12} {z+2} ";
        b.chain("Заставка экрана",
                new String[]{"Таймер заставки", "execute if score #on {id}_c matches 1 run scoreboard players add #t {id}_c 1", "Считает тики, пока заставка включена"},
                new String[]{"Кадр 1: синий", "execute if score #on {id}_c matches 1 if score #t {id}_c matches 10 run fill " + scr + "minecraft:blue_concrete", "Через 10 тиков заливает экран синим"},
                new String[]{"Кадр 2: фиолетовый", "execute if score #on {id}_c matches 1 if score #t {id}_c matches 20 run fill " + scr + "minecraft:purple_concrete", "Через 20 тиков заливает экран фиолетовым"},
                new String[]{"Кадр 3: бирюзовый", "execute if score #on {id}_c matches 1 if score #t {id}_c matches 30 run fill " + scr + "minecraft:cyan_concrete", "Через 30 тиков заливает экран бирюзовым"},
                new String[]{"Сброс таймера", "execute if score #on {id}_c matches 1 if score #t {id}_c matches 40.. run scoreboard players set #t {id}_c 0", "Начинает цикл заставки заново"});
        b.btn("Экран: включить", "scoreboard players set #on {id}_c 0", "fill {x+1} {y+4} {z+2} {x+15} {y+12} {z+2} minecraft:light_blue_concrete");
        b.btn("Экран: заставка", "scoreboard players set #on {id}_c 1");
        b.btn("Экран: выключить", "scoreboard players set #on {id}_c 0", "fill {x+1} {y+4} {z+2} {x+15} {y+12} {z+2} minecraft:black_concrete");
        b.help = "Компьютер: игровая модель (монитор, клавиатура, системный блок). Экран включается, выключается и показывает заставку кнопками.";
    }

    // ------------------------------------------------------------------ портал
    private static void portal(B b) {
        for (int ox : new int[]{0, 14}) {
            b.box(ox, 0, 0, ox + 4, 5, 0, "obsidian");
            b.box(ox + 1, 1, 0, ox + 3, 4, 0, "black_stained_glass");
            b.box(ox - 1, -1, -3, ox + 5, -1, 3, "polished_blackstone_bricks");
            b.set(ox - 1, 5, 0, "sea_lantern");
            b.set(ox + 5, 5, 0, "sea_lantern");
        }
        b.setup("scoreboard objectives add {id}_p dummy", "scoreboard players set #on {id}_p 0");
        b.chain("Порталы",
                new String[]{"Телепорт A → B", "execute if score #on {id}_p matches 1 positioned {x+1} {y+1} {z} as @a[dx=2,dy=3,dz=0] run tp @s {x+16} {y+1} {z+3}", "Игрока в арке A переносит перед аркой B (пока портал включён)"},
                new String[]{"Телепорт B → A", "execute if score #on {id}_p matches 1 positioned {x+15} {y+1} {z} as @a[dx=2,dy=3,dz=0] run tp @s {x+2} {y+1} {z+3}", "Игрока в арке B переносит перед аркой A (пока портал включён)"},
                new String[]{"Частицы арки A", "execute if score #on {id}_p matches 1 run particle minecraft:portal {x+2} {y+3} {z} 0.8 1.4 0.2 0.1 20", "Показывает частицы в арке A"},
                new String[]{"Частицы арки B", "execute if score #on {id}_p matches 1 run particle minecraft:portal {x+16} {y+3} {z} 0.8 1.4 0.2 0.1 20", "Показывает частицы в арке B"});
        b.btn("Портал: включить", "scoreboard players set #on {id}_p 1", "fill {x+1} {y+1} {z} {x+3} {y+4} {z} minecraft:purple_stained_glass", "fill {x+15} {y+1} {z} {x+17} {y+4} {z} minecraft:purple_stained_glass");
        b.btn("Портал: выключить", "scoreboard players set #on {id}_p 0", "fill {x+1} {y+1} {z} {x+3} {y+4} {z} minecraft:black_stained_glass", "fill {x+15} {y+1} {z} {x+17} {y+4} {z} minecraft:black_stained_glass");
        b.help = "Портал: две арки. Включи на пульте и войди в одну арку, окажешься перед другой.";
    }

    private NeroArchetypes() { }
}
