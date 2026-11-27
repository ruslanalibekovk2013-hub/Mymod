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
        String ak0 = NeroArsenal.detect(t);
        if (ak0 != null) return ak0;
        String mk0 = NeroMech.detect(t);
        if (mk0 != null) return mk0;
        String mk = NeroMachines.detect(t);
        if (mk != null) return mk;
        String xk = NeroMega.detect(t);
        if (xk != null) return xk;
        String bk = NeroMega.detectBuilder(t);
        if (bk != null) return bk;
        String wk = NeroWeapons.detect(t);
        if (wk != null) return wk;
        if (NeroFree.has(t, "portal\\s*gun|портал\\w*\\s+(пушк|ружь|пистолет|ган)|пушк\\w*\\s+портал")) return "portalgun";
        if (NeroFree.has(t, "принтер|3d|3д|3-д")) return "printer";
        if (NeroFree.has(t, "космическ\\w* корабл|звездолет|корабл|ракет|шаттл|самолет|звездн\\w* истребител")) return "ship";
        if (NeroFree.has(t, "робот|голем|андроид")) return "robot";
        if (NeroFree.has(t, "мост")) return "bridge";
        if (NeroFree.has(t, "поезд|электричк|вагон|метро|локомотив|железн\\w* дорог")) return "train";
        if (NeroFree.has(t, "компьютер|ноутбук|монитор")) return "computer";
        if (NeroFree.has(t, "портал")) return "portal";
        if (NeroFree.has(t, "лифт|подъемник") && !NeroFree.wantsBuilding(t)) return "lift";
        return null;
    }

    static String nameOf(String k) {
        if (k.startsWith("x_")) return NeroMega.name(k);
        if (k.startsWith("m_")) return NeroMachines.name(k);
        if (k.startsWith("k_")) return NeroMech.name(k);
        if (k.startsWith("wr_")) return NeroArsenal.name(k);
        if (k.startsWith("w_")) return NeroWeapons.name(k);
        return switch (k) { case "lift" -> "лифт"; case "portalgun" -> "портальная пушка"; case "printer" -> "3D-принтер"; case "ship" -> "корабль"; case "robot" -> "робот"; case "bridge" -> "мост"; case "train" -> "поезд"; case "computer" -> "компьютер"; case "portal" -> "портал"; default -> k; };
    }

    static String works(String k) {
        if (k.startsWith("x_")) return NeroMega.works(k);
        if (k.startsWith("m_")) return NeroMachines.works(k);
        if (k.startsWith("k_")) return NeroMech.works(k);
        if (k.startsWith("wr_")) return NeroArsenal.works(k);
        if (k.startsWith("w_")) return NeroWeapons.works(k);
        return switch (k) {
            case "lift" -> "кабина (модель из display-сущностей) плавно ездит между этажами, стеклянные двери этажей открываются и закрываются, вызов кнопками на этажах и на пульте, очередь вызовов, табло этажа и индикатор. Кабина сама не твёрдая: игрока везёт команда. Кнопки внутри кабины сделаны как оформление, этаж выбирается кнопками на этажах или пульте";
            case "portalgun" -> "настоящий предмет Portal Gun (ПКМ ставит синий портал, Shift+ПКМ оранжевый на вертикальную стену, два портала соединены телепортацией и частицами) и модель-подставка из блоков; настоящих портальных текстур нет, портал рисуется частицами";
            case "printer" -> "две платформы: исходная постройка слева, на правой платформе растёт уменьшенная копия из мини-блоков (масштаб 1:2, 1:4 или 1:8), движется печатающая головка, есть частицы и экран состояния";
            case "ship" -> "работают двигатели (эффекты выхлопа) и люк на пульте; корабль неподвижный";
            case "robot" -> "работают поднятие рук и глаза на пульте";
            case "bridge" -> "это конструкция без механизмов (механизмы мосту не нужны)";
            case "train" -> "работают пути с питанием и выпуск вагонеток на пульте; состав из блоков в Minecraft двигаться не может, он стоит моделью";
            case "computer" -> "работают экран и заставка на пульте";
            case "portal" -> "работает телепортация между двумя арками (включается на пульте)";
            default -> "";
        };
    }

    static void build(NeroPlanner.Ctx c, String key, String t) { NeroFree.blueprint(c, bp(key, t)); }

    /** Чертёж заготовки (его можно хранить в проекте и править точечно через чат). */
    static JsonObject bp(String key, String t) {
        if (key.startsWith("x_")) return NeroMega.bp(key, t);
        if (key.startsWith("m_")) return NeroMachines.bp(key, t);
        if (key.startsWith("k_")) return NeroMech.bp(key, t);
        if (key.startsWith("wr_")) return NeroArsenal.bp(key, t);
        if (key.startsWith("w_")) return NeroWeapons.bp(key, t);
        B b = new B();
        switch (key) {
            case "lift" -> liftPro(b, t);
            case "portalgun" -> portalGun(b);
            case "printer" -> printer(b);
            case "ship" -> ship(b);
            case "robot" -> robot(b);
            case "bridge" -> bridge(b, t);
            case "train" -> train(b);
            case "computer" -> computer(b);
            case "portal" -> portal(b);
            default -> { }
        }
        return b.done();
    }

    static final class B {
        final JsonArray ops = new JsonArray(), chains = new JsonArray(), setup = new JsonArray(), buttons = new JsonArray();
        String help = "";
        String cur = null;

        void part(String p) { cur = p; }

        private void tag(JsonObject o) { if (cur != null) o.addProperty("part", cur); }

        private static String nm(String b) { return b.contains(":") ? b : "minecraft:" + b; }

        void box(int x1, int y1, int z1, int x2, int y2, int z2, String blk, String mode) {
            JsonObject o = new JsonObject();
            o.addProperty("op", "box");
            o.addProperty("x1", x1); o.addProperty("y1", y1); o.addProperty("z1", z1);
            o.addProperty("x2", x2); o.addProperty("y2", y2); o.addProperty("z2", z2);
            o.addProperty("block", nm(blk));
            o.addProperty("mode", mode);
            tag(o);
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
            tag(o);
            ops.add(o);
        }

        void line(int x1, int y1, int z1, int x2, int y2, int z2, String blk) {
            JsonObject o = new JsonObject();
            o.addProperty("op", "line");
            o.addProperty("x1", x1); o.addProperty("y1", y1); o.addProperty("z1", z1);
            o.addProperty("x2", x2); o.addProperty("y2", y2); o.addProperty("z2", z2);
            o.addProperty("block", nm(blk));
            tag(o);
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


    // ------------------------------------------------------------------ современный лифт
    private static String f3(double v) { return String.format(java.util.Locale.ROOT, "%.3f", v); }

    private static String disp(String type, String payload, int ox, int oy, int oz, double[] tr, double[] sc, String... tags) {
        StringBuilder tg = new StringBuilder("\"{tag}\"");
        for (String t : tags) tg.append(",\"").append(t).append('"');
        String tf = "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[" + f3(tr[0]) + "f," + f3(tr[1]) + "f," + f3(tr[2]) + "f],scale:[" + f3(sc[0]) + "f," + f3(sc[1]) + "f," + f3(sc[2]) + "f]}";
        String head = "summon minecraft:" + type + " {x+" + ox + "} {y+" + oy + "} {z+" + oz + "} {";
        return head + payload + "," + tf + ",teleport_duration:1,Tags:[" + tg + "]}";
    }

    private static String blockPl(String block) { return "block_state:{Name:\"minecraft:" + block + "\"}"; }

    private static String textPl(String text, String color) { return "text:'{\"text\":\"" + text + "\",\"color\":\"" + color + "\"}',billboard:\"center\""; }

    /**
     * Лифт: шахта с площадками, стеклянные двери, вызовы настоящими кнопками на этажах, очередь, кабина из display-сущностей
     * (панель, мини-кнопки, индикатор, табло), плавное движение, пока движется игрок в шахте. Логика: счётчики и 3 цепочки командных блоков.
     */
    private static void liftPro(B b, String t) {
        int n = 5;
        java.util.regex.Matcher m = Pattern.compile("(\\d{1,2})\\s*-?\\s*(этаж|этажей|этажа)").matcher(t);
        while (m.find()) n = Integer.parseInt(m.group(1));
        final int asked = n;
        n = Math.max(2, Math.min(10, n));
        int h = NeroFree.has(t, "компакт") ? 4 : 5;
        String wall = "smooth_quartz", glass = "light_blue_stained_glass", frame = "quartz_pillar", floor = "smooth_quartz", style = "Modern";
        if (NeroFree.has(t, "роскош|люкс|luxury")) { wall = "polished_blackstone"; glass = "gray_stained_glass"; frame = "gold_block"; floor = "polished_blackstone_bricks"; style = "Luxury"; }
        else if (NeroFree.has(t, "футурист|high-tech|хай-тек|хайтек|будущ")) { wall = "white_concrete"; glass = "cyan_stained_glass"; frame = "iron_block"; floor = "white_concrete"; style = "Futuristic"; }
        else if (NeroFree.has(t, "индустриал|завод")) { wall = "gray_concrete"; glass = "glass"; frame = "iron_block"; floor = "stone_bricks"; style = "Industrial"; }
        else if (NeroFree.has(t, "минимал")) { wall = "white_concrete"; glass = "glass"; frame = "white_concrete"; floor = "white_concrete"; style = "Minimalist"; }
        else if (NeroFree.has(t, "панорам|стеклян")) { wall = "light_blue_stained_glass"; glass = "light_blue_stained_glass"; frame = "iron_block"; style = "Panoramic"; }
        int top = (n - 1) * h;
        final String L = "{id}_l";

        b.part("Шахта");
        b.box(0, -1, 0, 4, top + 3, 4, wall, "walls");
        b.box(0, top + 4, 0, 4, top + 4, 4, wall);
        b.box(1, -1, 1, 3, -1, 3, floor);
        b.air(1, 0, 1, 3, top + 3, 3);
        for (int[] c : new int[][]{{0, 0}, {4, 0}, {0, 4}, {4, 4}}) b.box(c[0], -1, c[1], c[0], top + 4, c[1], frame);
        b.part("Площадки этажей");
        for (int i = 0; i < n; i++) b.box(-1, i * h - 1, -3, 5, i * h - 1, -1, floor);
        b.part("Двери этажей");
        for (int i = 0; i < n; i++) { b.box(1, i * h, 0, 3, i * h + 2, 0, glass); b.box(1, i * h + 3, 0, 3, i * h + 3, 0, frame); }
        b.part("Кнопки вызова");
        for (int i = 0; i < n; i++) b.set(0, i * h + 1, -1, "stone_button[face=wall,facing=north]");

        java.util.List<String> su = new java.util.ArrayList<>();
        su.add("scoreboard objectives add " + L + " dummy");
        su.add("scoreboard players set #cur " + L + " 0");
        su.add("scoreboard players set #tgt " + L + " -1");
        su.add("scoreboard players set #busy " + L + " 0");
        su.add("scoreboard players set #t " + L + " 0");
        su.add("scoreboard players set #y " + L + " {y}00");
        su.add("scoreboard players set #ty " + L + " {y}00");
        for (int i = 0; i < n; i++) su.add("scoreboard players set #q" + i + " " + L + " 0");
        // кабина: пол, стены, потолок, свет, стойки, панель с мини-кнопками, табло, индикатор
        String[] cab = {"cab_{id}"};
        su.add(disp("block_display", blockPl(floor), 1, 0, 1, new double[]{0.05, -0.12, 0.05}, new double[]{2.9, 0.12, 2.9}, cab));
        su.add(disp("block_display", blockPl(glass), 1, 0, 1, new double[]{0.05, 0, 2.94}, new double[]{2.9, 2.6, 0.06}, cab));
        su.add(disp("block_display", blockPl(glass), 1, 0, 1, new double[]{0.0, 0, 0.05}, new double[]{0.06, 2.6, 2.9}, cab));
        su.add(disp("block_display", blockPl(glass), 1, 0, 1, new double[]{2.94, 0, 0.05}, new double[]{0.06, 2.6, 2.9}, cab));
        su.add(disp("block_display", blockPl(wall), 1, 0, 1, new double[]{0.05, 2.6, 0.05}, new double[]{2.9, 0.1, 2.9}, cab));
        su.add(disp("block_display", blockPl("sea_lantern"), 1, 0, 1, new double[]{1.0, 2.55, 1.0}, new double[]{1.0, 0.05, 1.0}, cab));
        for (double[] c : new double[][]{{0, 0}, {2.9, 0}, {0, 2.9}, {2.9, 2.9}})
            su.add(disp("block_display", blockPl(frame), 1, 0, 1, new double[]{c[0], 0, c[1]}, new double[]{0.1, 2.7, 0.1}, cab));
        su.add(disp("block_display", blockPl("black_concrete"), 1, 0, 1, new double[]{1.1, 0.6, 2.86}, new double[]{0.8, 1.2, 0.05}, cab));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 2; c++)
                su.add(disp("block_display", blockPl(r == 2 && c == 0 ? "lime_concrete" : "gray_concrete"), 1, 0, 1, new double[]{1.25 + c * 0.3, 0.7 + r * 0.22, 2.82}, new double[]{0.14, 0.14, 0.04}, cab));
        su.add(disp("block_display", blockPl("lime_concrete"), 1, 0, 1, new double[]{1.45, 1.4, 2.82}, new double[]{0.18, 0.1, 0.04}, "cab_{id}", "ind_{id}"));
        su.add(disp("text_display", textPl("1", "aqua"), 1, 0, 1, new double[]{1.5, 1.7, 2.8}, new double[]{0.7, 0.7, 0.7}, "cab_{id}", "cabdisp_{id}"));
        for (int i = 0; i < n; i++) {
            su.add(disp("text_display", textPl("1", "aqua"), 2, i * h + 3, -1, new double[]{0, 0, 0}, new double[]{1, 1, 1}, "fdisp_{id}"));
            su.add(disp("block_display", blockPl("gray_concrete"), 0, i * h + 1, -1, new double[]{0.2, 0.55, 0.0}, new double[]{0.12, 0.12, 0.04}, "fli_{id}_" + i));
        }
        su.add("summon minecraft:text_display {x+2} {y+" + (top + 6) + "} {z+2} {text:'{\"text\":\"ЛИФТ · " + style + "\",\"color\":\"gold\"}',billboard:\"center\",Tags:[\"{tag}\"]}");
        for (String c : su) b.setup(c);

        String sel = "x={x+1},y={y-1},z={z+1},dx=2,dy=" + (top + 3) + ",dz=2";
        String LS = "scoreboard players set ";
        java.util.List<String[]> c1 = new java.util.ArrayList<>(), c2 = new java.util.ArrayList<>(), c3 = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++)
            c1.add(new String[]{"Вызов этажа " + (i + 1), "execute if block {x+0} {y+" + (i * h + 1) + "} {z-1} minecraft:stone_button[powered=true] run " + LS + "#q" + i + " " + L + " 1", "Кнопка вызова на этаже " + (i + 1) + " ставит этаж в очередь"});
        for (int i = 0; i < n; i++) {
            c1.add(new String[]{"Подсветка вызова " + (i + 1), "execute if score #q" + i + " " + L + " matches 1 run data modify entity @e[type=minecraft:block_display,tag=fli_{id}_" + i + ",limit=1] block_state set value {Name:\"minecraft:orange_concrete\"}", "Индикатор у кнопки светится, пока вызов ждёт"});
            c1.add(new String[]{"Гашение вызова " + (i + 1), "execute unless score #q" + i + " " + L + " matches 1 run data modify entity @e[type=minecraft:block_display,tag=fli_{id}_" + i + ",limit=1] block_state set value {Name:\"minecraft:gray_concrete\"}", "Индикатор у кнопки гаснет, когда вызова нет"});
        }
        for (int i = 0; i < n; i++)
            c1.add(new String[]{"Выбор этажа " + (i + 1) + " из очереди", "execute if score #busy " + L + " matches 0 unless score #tgt " + L + " matches 0.. if score #q" + i + " " + L + " matches 1 run " + LS + "#tgt " + L + " " + i, "Если кабина свободна, берёт ближайший по номеру вызов из очереди"});
        for (int i = 0; i < n; i++)
            c2.add(new String[]{"Высота этажа " + (i + 1), "execute if score #busy " + L + " matches 0 if score #tgt " + L + " matches " + i + " run " + LS + "#ty " + L + " {y+" + (i * h) + "}00", "Запоминает высоту целевого этажа"});
        c2.add(new String[]{"Кабина уже на этаже", "execute if score #busy " + L + " matches 0 if score #tgt " + L + " matches 0.. if score #y " + L + " = #ty " + L + " run " + LS + "#busy " + L + " 3", "Если кабина уже на нужной высоте, сразу переходит к открытию дверей"});
        c2.add(new String[]{"Начало движения", "execute if score #busy " + L + " matches 0 if score #tgt " + L + " matches 0.. run " + LS + "#busy " + L + " 1", "Кабина начинает ехать к выбранному этажу"});
        c2.add(new String[]{"Пассажиры вверх", "execute if score #busy " + L + " matches 1 if score #y " + L + " < #ty " + L + " as @a[" + sel + "] at @s run tp @s ~ ~0.2 ~", "Поднимает игроков в шахте вместе с кабиной"});
        c2.add(new String[]{"Пассажиры вниз", "execute if score #busy " + L + " matches 1 if score #y " + L + " > #ty " + L + " as @a[" + sel + "] at @s run tp @s ~ ~-0.2 ~", "Опускает игроков в шахте вместе с кабиной"});
        c2.add(new String[]{"Защита от падения", "execute if score #busy " + L + " matches 1 run effect give @a[" + sel + "] minecraft:slow_falling 1 0 true", "Игрок в движущейся кабине не падает"});
        c2.add(new String[]{"Шаг вверх", "execute if score #busy " + L + " matches 1 if score #y " + L + " < #ty " + L + " run scoreboard players add #y " + L + " 20", "Высота кабины растёт на 0.2 блока за тик"});
        c2.add(new String[]{"Шаг вниз", "execute if score #busy " + L + " matches 1 if score #y " + L + " > #ty " + L + " run scoreboard players remove #y " + L + " 20", "Высота кабины падает на 0.2 блока за тик"});
        c2.add(new String[]{"Положение кабины", "execute if score #busy " + L + " matches 1 as @e[tag=cab_{id}] store result entity @s Pos[1] double 0.01 run scoreboard players get #y " + L, "Переносит модель кабины на текущую высоту"});
        c2.add(new String[]{"Прибытие", "execute if score #busy " + L + " matches 1 if score #y " + L + " = #ty " + L + " run " + LS + "#busy " + L + " 3", "Достигнув высоты этажа, переходит к открытию дверей"});
        c2.add(new String[]{"Индикатор: движение", "execute if score #busy " + L + " matches 1 run data modify entity @e[type=minecraft:block_display,tag=ind_{id},limit=1] block_state set value {Name:\"minecraft:red_concrete\"}", "Красный индикатор, пока кабина едет"});
        c2.add(new String[]{"Индикатор: свободна", "execute unless score #busy " + L + " matches 1 run data modify entity @e[type=minecraft:block_display,tag=ind_{id},limit=1] block_state set value {Name:\"minecraft:lime_concrete\"}", "Зелёный индикатор, когда кабина стоит"});
        c2.add(new String[]{"Табло: движение", "execute if score #busy " + L + " matches 1 run data modify entity @e[type=minecraft:text_display,tag=fdisp_{id}] text set value '{\"text\":\"⇅\",\"color\":\"yellow\"}'", "Табло на этажах показывает движение"});
        c2.add(new String[]{"Табло кабины: движение", "execute if score #busy " + L + " matches 1 run data modify entity @e[type=minecraft:text_display,tag=cabdisp_{id},limit=1] text set value '{\"text\":\"⇅\",\"color\":\"yellow\"}'", "Дисплей в кабине показывает движение"});
        for (int i = 0; i < n; i++) {
            String at = "execute if score #busy " + L + " matches 3 if score #tgt " + L + " matches " + i + " run ";
            c3.add(new String[]{"Остановка на этаже " + (i + 1), at + "tp @a[" + sel + "] {x+2} {y+" + (i * h) + "} {z+2}", "Точно ставит игроков на уровень этажа"});
            c3.add(new String[]{"Открытие двери этажа " + (i + 1), at + "fill {x+1} {y+" + (i * h) + "} {z+0} {x+3} {y+" + (i * h + 2) + "} {z+0} minecraft:air", "Открывает дверь на этаже"});
            c3.add(new String[]{"Снятие вызова " + (i + 1), at + LS + "#q" + i + " " + L + " 0", "Убирает выполненный вызов из очереди"});
            c3.add(new String[]{"Табло этажей: " + (i + 1), at + "data modify entity @e[type=minecraft:text_display,tag=fdisp_{id}] text set value '{\"text\":\"" + (i + 1) + "\",\"color\":\"aqua\"}'", "Табло на этажах показывает номер этажа"});
            c3.add(new String[]{"Табло кабины: " + (i + 1), at + "data modify entity @e[type=minecraft:text_display,tag=cabdisp_{id},limit=1] text set value '{\"text\":\"" + (i + 1) + "\",\"color\":\"aqua\"}'", "Дисплей в кабине показывает номер этажа"});
        }
        c3.add(new String[]{"Сигнал прибытия", "execute if score #busy " + L + " matches 3 as @a[" + sel + "] at @s run playsound minecraft:block.note_block.chime master @s ~ ~ ~ 1 1.5", "Звук прибытия для пассажиров"});
        c3.add(new String[]{"Запомнить этаж", "execute if score #busy " + L + " matches 3 run scoreboard players operation #cur " + L + " = #tgt " + L, "Текущий этаж равен целевому"});
        c3.add(new String[]{"Таймер дверей", "execute if score #busy " + L + " matches 3 run " + LS + "#t " + L + " 60", "Двери остаются открытыми 3 секунды"});
        c3.add(new String[]{"Двери открыты", "execute if score #busy " + L + " matches 3 run " + LS + "#busy " + L + " 2", "Состояние: двери открыты"});
        c3.add(new String[]{"Отсчёт дверей", "execute if score #busy " + L + " matches 2 run scoreboard players remove #t " + L + " 1", "Отсчитывает время открытых дверей"});
        for (int i = 0; i < n; i++)
            c3.add(new String[]{"Закрытие двери этажа " + (i + 1), "execute if score #busy " + L + " matches 2 if score #t " + L + " matches 0 if score #cur " + L + " matches " + i + " run fill {x+1} {y+" + (i * h) + "} {z+0} {x+3} {y+" + (i * h + 2) + "} {z+0} minecraft:" + glass, "Закрывает дверь на этаже по истечении времени"});
        c3.add(new String[]{"Сброс цели", "execute if score #busy " + L + " matches 2 if score #t " + L + " matches 0 run " + LS + "#tgt " + L + " -1", "Кабина готова к следующему вызову"});
        c3.add(new String[]{"Кабина свободна", "execute if score #busy " + L + " matches 2 if score #t " + L + " matches 0 run " + LS + "#busy " + L + " 0", "Состояние: кабина свободна"});
        b.chain("Вызовы и очередь", c1.toArray(new String[0][]));
        b.chain("Движение кабины", c2.toArray(new String[0][]));
        b.chain("Прибытие и двери", c3.toArray(new String[0][]));
        for (int i = 0; i < n; i++) b.btn("Этаж " + (i + 1), LS + "#q" + i + " " + L + " 1");
        b.help = (asked > 10 ? "Запрошено этажей: " + asked + ", максимум в этой модели 10. " : "") + "Лифт (" + style + ", " + n + " этажей, высота этажа " + h + "): вызывай кабину кнопками рядом с дверями на этажах или кнопками «Этаж N» на пульте; вызовы встают в очередь. Кабина едет плавно, двери этажа открываются на 3 секунды. Кабина состоит из display-сущностей (не твёрдая), пассажира везёт команда. Кнопки на панели внутри кабины сделаны как оформление.";
    }

    // ------------------------------------------------------------------ Portal Gun
    /** Подставка с моделью пушки из блоков. Сам интерактивный предмет mackros:portal_gun выдаётся кнопкой пульта. */
    private static void portalGun(B b) {
        b.part("Подставка");
        b.box(0, -1, 0, 16, -1, 8, "smooth_stone");
        b.box(6, 0, 3, 7, 1, 4, "iron_block");
        b.part("Корпус");
        b.box(2, 3, 2, 10, 5, 5, "white_concrete");
        b.box(3, 6, 3, 8, 6, 4, "light_gray_concrete");
        b.part("Рукоятка");
        b.box(3, 0, 3, 5, 2, 4, "gray_concrete");
        b.box(5, 2, 3, 6, 2, 4, "gray_concrete");
        b.part("Ствол");
        b.box(11, 3, 3, 13, 5, 4, "light_gray_concrete");
        b.box(14, 2, 2, 14, 6, 5, "black_concrete");
        b.air(14, 3, 3, 14, 5, 4);
        b.part("Ядро портала");
        b.box(7, 4, 2, 8, 4, 2, "sea_lantern");
        b.box(7, 4, 5, 8, 4, 5, "sea_lantern");
        b.set(12, 6, 3, "sea_lantern");
        b.part("Кольца портала");
        b.set(11, 6, 3, "blue_concrete");
        b.set(11, 2, 3, "orange_concrete");
        b.set(11, 3, 2, "blue_concrete");
        b.set(11, 5, 5, "orange_concrete");
        b.part("Элементы управления");
        b.set(4, 6, 3, "stone_button[face=floor]");
        b.set(5, 6, 3, "stone_button[face=floor]");
        b.set(9, 6, 2, "cyan_concrete");
        b.set(9, 6, 5, "orange_concrete");
        b.setup("summon minecraft:text_display {x+8} {y+9} {z+3} {text:'{\"text\":\"PORTAL GUN\",\"color\":\"aqua\"}',billboard:\"center\",Tags:[\"{tag}\"]}");
        b.btn("Выдать Portal Gun", "give @p mackros:portal_gun");
        b.btn("Очистить все порталы", "nero portals clear");
        b.help = "Portal Gun: на подставке стоит модель пушки (корпус, рукоятка, ствол, ядро, кольца, управление). Кнопка «Выдать Portal Gun» даёт настоящий предмет: ПКМ ставит синий портал, Shift+ПКМ оранжевый на вертикальную стену. Порталы соединены: войди в один, выйдешь из другого.";
    }

    // ------------------------------------------------------------------ 3D-принтер с миниатюрной печатью
    /**
     * Две платформы: A (слева) с исходной постройкой, B (справа) для уменьшенной копии. Печать выполняет серверная команда «nero print»
     * (NeroPrint): она сканирует блоки платформы A и слой за слоем создаёт мини-блоки (block_display в масштабе 1:N) на платформе B,
     * двигая печатающую головку и показывая частицы.
     */
    private static void printer(B b) {
        b.part("Платформа A");
        b.box(0, -1, 0, 16, -1, 16, "smooth_stone");
        for (int[] k : new int[][]{{0, 0}, {16, 0}, {0, 16}, {16, 16}}) b.set(k[0], -1, k[1], "sea_lantern");
        b.part("Исходная постройка");
        b.hol(2, 0, 2, 14, 6, 14, "stone_bricks");
        b.air(7, 0, 2, 8, 2, 2);
        b.box(4, 3, 2, 5, 4, 2, "light_blue_stained_glass");
        b.box(10, 3, 2, 11, 4, 2, "light_blue_stained_glass");
        b.box(2, 3, 5, 2, 4, 6, "light_blue_stained_glass");
        b.box(14, 3, 5, 14, 4, 6, "light_blue_stained_glass");
        JsonObject py = new JsonObject();
        py.addProperty("op", "pyramid");
        py.addProperty("x1", 2); py.addProperty("z1", 2); py.addProperty("x2", 14); py.addProperty("z2", 14); py.addProperty("y", 7);
        py.addProperty("block", "minecraft:dark_oak_planks");
        py.addProperty("part", "Исходная постройка");
        b.ops.add(py);
        b.box(11, 7, 11, 11, 10, 11, "bricks");
        b.set(11, 11, 11, "campfire[lit=false]");
        b.part("Платформа B");
        b.box(24, -1, 0, 40, -1, 16, "smooth_stone");
        for (int[] k : new int[][]{{24, 0}, {40, 0}, {24, 16}, {40, 16}}) b.set(k[0], -1, k[1], "sea_lantern");
        b.box(28, -1, 4, 40, -1, 16, "polished_andesite");
        b.part("Направляющие");
        b.box(24, 10, 0, 40, 10, 0, "iron_block");
        b.box(24, 10, 16, 40, 10, 16, "iron_block");
        for (int[] k : new int[][]{{24, 0}, {40, 0}, {24, 16}, {40, 16}}) b.box(k[0], 0, k[1], k[0], 10, k[1], "iron_block");
        b.part("Сканер");
        b.box(0, 0, 8, 0, 15, 8, "iron_block");
        b.box(16, 0, 8, 16, 15, 8, "iron_block");
        b.box(0, 15, 8, 16, 15, 8, "iron_block");
        b.set(8, 14, 8, "sea_lantern");
        b.part("Контроллер");
        b.box(18, 0, 6, 20, 2, 10, "gray_concrete");
        b.set(19, 3, 8, "redstone_lamp[lit=true]");
        b.set(18, 1, 7, "lime_concrete");
        b.set(18, 1, 9, "red_concrete");
        b.part("Хранилище данных");
        b.box(18, 0, 12, 20, 1, 14, "gray_concrete");
        b.set(19, 2, 13, "ender_chest[facing=south]");
        b.part("Экран управления");
        b.box(18, 0, 2, 22, 1, 2, "gray_concrete");
        b.box(18, 2, 2, 22, 5, 2, "black_concrete");
        b.box(17, 2, 2, 17, 5, 2, "iron_block");
        b.box(23, 2, 2, 23, 5, 2, "iron_block");
        b.setup("summon minecraft:block_display {x+34} {y+9} {z+8} {block_state:{Name:\"minecraft:iron_block\"},teleport_duration:3,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.7f,0.7f,0.7f]},Tags:[\"{tag}\",\"nero_head_{id}\"]}",
                "summon minecraft:block_display {x+34} {y+10} {z} {block_state:{Name:\"minecraft:iron_block\"},teleport_duration:3,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.4f,0.4f,16f]},Tags:[\"{tag}\",\"nero_beam_{id}\"]}",
                "summon minecraft:text_display {x+20} {y+4} {z+1} {text:'{\"text\":\"ПРИНТЕР: готов\",\"color\":\"green\"}',billboard:\"center\",Tags:[\"{tag}\",\"nero_screen_{id}\"]}",
                "summon minecraft:text_display {x+8} {y+16} {z+8} {text:'{\"text\":\"ПЛАТФОРМА A: оригинал\",\"color\":\"aqua\"}',billboard:\"center\",Tags:[\"{tag}\"]}",
                "summon minecraft:text_display {x+32} {y+12} {z+8} {text:'{\"text\":\"ПЛАТФОРМА B: миниатюра\",\"color\":\"gold\"}',billboard:\"center\",Tags:[\"{tag}\"]}");
        String pr = "nero print {x+2} {y} {z+2} 13 14 13 {x+28} {y} {z+4} ";
        b.btn("Печать 1:2", pr + "2 {id}");
        b.btn("Печать 1:4", pr + "4 {id}");
        b.btn("Печать 1:8", pr + "8 {id}");
        b.btn("Печать: стоп", "nero stop {id} {x+34} {y+9} {z+8}");
        b.btn("Очистить копию", "kill @e[type=minecraft:block_display,tag=nero_mini_{id}]");
        b.help = "3D-принтер: на платформе A стоит исходная постройка (её можно заменить своей в области 13x13x14). Кнопка «Печать 1:N» сканирует её и слой за слоем строит на платформе B уменьшенную копию из мини-блоков. Движется печатающая головка, частицы показывают сканирование и печать, экран показывает ход.";
    }

    // ------------------------------------------------------------------ космический корабль
    private static void ship(B b) {
        b.part("Корпус");
        b.box(4, 0, 2, 10, 2, 26, "light_gray_concrete");
        b.hol(3, 1, 4, 11, 6, 24, "white_concrete");
        b.air(4, 2, 5, 10, 5, 23);
        b.part("Нос");
        for (int k = 0; k < 8; k++) { int hw = 3 - k / 3; b.box(7 - hw, 1, 25 + k, 7 + hw, Math.max(2, 5 - k / 2), 25 + k, "white_concrete"); }
        b.part("Кабина");
        b.box(5, 5, 19, 9, 6, 26, "light_blue_stained_glass");
        b.air(6, 5, 20, 8, 5, 24);
        b.part("Крылья");
        for (int k = 0; k < 7; k++) { b.box(2 - k, 2, 8 + k, 3, 2, 16, "gray_concrete"); b.box(11, 2, 8 + k, 12 + k, 2, 16, "gray_concrete"); }
        b.part("Хвост и двигатели");
        b.box(7, 6, 2, 7, 11, 9, "white_concrete");
        for (int x : new int[]{5, 7, 9}) { b.box(x, 1, 0, x, 3, 1, "iron_block"); b.set(x, 2, -1, "magma_block"); }
        b.part("Интерьер кабины");
        b.set(6, 2, 20, "oak_stairs[facing=south]");
        b.set(8, 2, 20, "oak_stairs[facing=south]");
        b.box(5, 2, 22, 9, 2, 22, "smooth_quartz");
        for (int z : new int[]{10, 16, 21}) b.set(7, 5, z, "sea_lantern");
        b.part("Люк");
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
        b.part("Ноги");
        b.box(3, 0, 2, 5, 5, 4, "iron_block");
        b.box(7, 0, 2, 9, 5, 4, "iron_block");
        b.box(2, 0, 1, 5, 0, 5, "gray_concrete");
        b.box(7, 0, 1, 10, 0, 5, "gray_concrete");
        b.part("Туловище");
        b.box(2, 6, 1, 10, 12, 5, "light_gray_concrete");
        b.box(5, 9, 1, 7, 11, 1, "black_concrete");
        b.set(6, 10, 1, "sea_lantern");
        b.part("Голова");
        b.box(3, 13, 1, 9, 18, 5, "iron_block");
        b.box(5, 14, 1, 7, 14, 1, "black_concrete");
        b.set(4, 16, 1, "black_concrete");
        b.set(8, 16, 1, "black_concrete");
        b.line(6, 19, 3, 6, 22, 3, "end_rod");
        b.set(6, 23, 3, "sea_lantern");
        b.part("Руки");
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
        b.part("Настил");
        b.box(0, -1, 0, 6, -1, L, "stone_bricks");
        b.box(3, -1, 0, 3, -1, L, "smooth_stone");
        b.part("Перила и фонари");
        b.box(0, 0, 0, 0, 0, L, "stone_brick_wall");
        b.box(6, 0, 0, 6, 0, L, "stone_brick_wall");
        for (int z = 4; z < L; z += 8) { b.set(0, 1, z, "lantern[hanging=false]"); b.set(6, 1, z, "lantern[hanging=false]"); }
        b.part("Опоры");
        for (int z = 6; z < L; z += 12) {
            b.box(1, -12, z, 2, -2, z + 1, "stone_bricks");
            b.box(4, -12, z, 5, -2, z + 1, "stone_bricks");
            b.box(1, -2, z, 5, -2, z + 1, "stone_bricks");
        }
        if (hang) {
            b.part("Башни и тросы");
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
            b.part("Арки");
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
        b.part("Путь");
        b.box(7, -2, 0, 9, -2, L, "gravel");
        for (int z = 0; z <= L; z++) b.set(8, -1, z, "powered_rail[shape=north_south]");
        for (int z = 0; z <= L; z += 6) b.set(9, -1, z, "redstone_block");
        b.box(7, -1, L + 1, 9, 0, L + 1, "red_concrete");
        b.part("Станция");
        b.box(2, -1, 4, 6, -1, 22, "smooth_stone");
        b.box(2, 5, 4, 6, 5, 22, "smooth_stone_slab");
        for (int z : new int[]{4, 13, 22}) { b.box(2, 0, z, 2, 4, z, "stripped_oak_log"); b.box(6, 0, z, 6, 4, z, "stripped_oak_log"); }
        for (int z : new int[]{8, 17}) b.set(4, 4, z, "lantern[hanging=true]");
        b.part("Состав-модель");
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
        b.part("Подставка и монитор");
        b.box(5, 0, 2, 11, 0, 6, "gray_concrete");
        b.box(7, 1, 3, 9, 2, 5, "gray_concrete");
        b.box(0, 3, 3, 16, 13, 4, "black_concrete");
        b.box(1, 4, 2, 15, 12, 2, "black_concrete");
        b.part("Клавиатура");
        b.box(3, -1, -1, 13, -1, 1, "gray_concrete");
        b.box(3, 0, 0, 13, 0, 0, "light_gray_concrete");
        for (int x = 4; x <= 12; x++) b.set(x, 1, 0, "stone_button[face=floor]");
        b.set(15, 0, 0, "polished_andesite");
        b.part("Системный блок");
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
            b.part(ox == 0 ? "Арка A" : "Арка B");
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
