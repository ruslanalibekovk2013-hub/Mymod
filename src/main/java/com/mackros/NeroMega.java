package com.mackros;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 30 больших механизмов (по ~100 командных блоков): ходячие роботы, транспорт, летающие аппараты, вращающиеся машины, автостроители, световые шоу.
 * Движущиеся модели собираются из display-сущностей (block_display) с плавной анимацией; логика (движение, столкновения, батарея, анимация шагов,
 * пульт с кнопками и табличками) выполняется командными блоками. Это вымышленные игровые конструкции.
 */
final class NeroMega {
    record V(String key, String name, String cat, String desc, String re) { }

    static final V[] ALL = {
            new V("x_walker", "Шагающий робот", "Ходячие роботы", "идёт к игроку, обходит ступени, шагает руками и ногами", "шагающ\\w*\\s+робот|робот[- ]?ходок"),
            new V("x_spider", "Робот-паук", "Ходячие роботы", "шесть ног, следует за игроком, переползает ступени", "робот[- ]?паук|паук[- ]?робот"),
            new V("x_dog", "Робот-пёс", "Ходячие роботы", "четыре лапы, виляет хвостом, бегает за игроком", "робот[- ]?пес|собак\\w*[- ]?робот"),
            new V("x_titan", "Мех-титан", "Ходячие роботы", "огромный мех, бьёт враждебных мобов рукой", "мех[- ]?титан|боевой\\s+мех|титан"),
            new V("x_guard", "Робот-страж", "Ходячие роботы", "охраняет зону, отгоняет и бьёт врагов", "робот[- ]?страж|страж[- ]?робот"),
            new V("x_crab", "Робот-краб", "Ходячие роботы", "восемь лап и клешни, быстрый шаг", "робот[- ]?краб"),
            new V("x_centipede", "Робот-сороконожка", "Ходячие роботы", "десять пар ног, волнообразный шаг", "сороконожк"),
            new V("x_car", "Автомобиль с пультом", "Транспорт", "рулишь кнопками, колёса крутятся, фары, гудок, автопилот", "автомобил"),
            new V("x_tank", "Танк", "Транспорт", "гусеницы, башня сама наводится на врагов и стреляет", "(^|\\s)танк"),
            new V("x_rover", "Вездеход", "Транспорт", "шесть колёс, переезжает ступени, антенна", "вездеход|марсоход"),
            new V("x_buggy", "Багги", "Транспорт", "лёгкая машина с открытым каркасом и автопилотом", "багги"),
            new V("x_drone", "Дрон-следопыт", "Летающие", "летит за игроком, вращает винты, мигает огнями", "(^|\\s)дрон"),
            new V("x_heli", "Вертолёт", "Летающие", "взлёт, посадка, полёт вперёд и повороты, вращение винтов", "вертолет"),
            new V("x_rocket", "Ракета с запуском", "Летающие", "обратный отсчёт, старт, разгон, салют и возврат", "ракет\\w*\\s+(с\\s+)?(запуск|старт)|пусков\\w*\\s+ракет|запуск\\w*\\s+ракет"),
            new V("x_ufo", "НЛО", "Летающие", "парит по кругу, светит лучом, поднимает врагов", "(^|\\s)нло|летающ\\w*\\s+тарелк"),
            new V("x_windmill", "Ветряная мельница", "Вращающиеся машины", "четыре лопасти, скорость зависит от погоды", "мельниц"),
            new V("x_ferris", "Колесо обозрения", "Вращающиеся машины", "кабины идут по кругу и остаются ровными", "колес\\w*\\s+обозрен"),
            new V("x_carousel", "Карусель", "Вращающиеся машины", "сиденья едут по кругу и качаются", "карусел"),
            new V("x_radar", "Радар", "Вращающиеся машины", "вращающаяся антенна подсвечивает врагов поблизости", "радар"),
            new V("x_clock", "Часовая башня", "Вращающиеся машины", "стрелки идут по игровому времени, бой часов", "часов\\w*\\s+башн|башенн\\w*\\s+час"),
            new V("x_crane", "Башенный кран", "Вращающиеся машины", "поворот стрелы, тележка, крюк с грузом, пульт", "(^|\\s)(башенн\\w*\\s+)?кран"),
            new V("x_arm", "Механическая рука", "Вращающиеся машины", "три сустава, захват, перенос груза по сценарию", "механическ\\w*\\s+рук|робо[- ]?рук|манипулятор"),
            new V("m_lighthouse", "Маяк-автосборка", "Автостроители", "строит маяк слоями с лампой и вращающимся лучом", "маяк"),
            new V("m_maze", "Лабиринт-генератор", "Автостроители", "строит лабиринт из стен по готовой схеме", "лабиринт"),
            new V("m_village", "Деревня-автосборка", "Автостроители", "строит шесть домов, колодец и дорожки", "деревн"),
            new V("m_stadium", "Стадион-автосборка", "Автостроители", "овальная чаша с трибунами, полем и прожекторами", "стадион"),
            new V("x_disco", "Дискотека", "Световые шоу", "танцпол меняет цвета, лазеры, дым-машина", "дискотек|танцпол"),
            new V("x_fountain", "Фонтан-шоу", "Световые шоу", "струи воды из частиц по программе", "фонтан"),
            new V("x_fireworks", "Салют-станция", "Световые шоу", "программа залпов настоящими фейерверками", "салют|фейерверк"),
            new V("x_aurora", "Северное сияние", "Световые шоу", "разноцветные волны света над площадкой", "северн\\w*\\s+сияни|аврор")};

    static V find(String k) { for (V v : ALL) if (v.key().equals(k)) return v; return ALL[0]; }

    static String detect(String t) { for (V v : ALL) if (!v.key().startsWith("m_") && NeroFree.has(t, v.re())) return v.key(); return null; }

    static String detectBuilder(String t) { for (V v : ALL) if (v.key().startsWith("m_") && NeroFree.has(t, v.re())) return v.key(); return null; }

    static String name(String k) { return find(k).name(); }

    static String works(String k) {
        V v = find(k);
        return "большой механизм «" + v.name() + "» (вымышленная игровая конструкция): " + v.desc() + ". Пульт с кнопками и табличками, ~100 командных блоков логики; подвижные части из display-сущностей, поэтому они не твёрдые";
    }

    static JsonObject bp(String key, String t) {
        K k = new K(key);
        switch (key) {
            case "x_walker" -> walker(k, 2, true, false, "iron_block", "gray_concrete", "white_concrete", "lime_concrete", 1.0, 0.16, 8);
            case "x_spider" -> walker(k, 6, false, false, "black_concrete", "gray_concrete", "black_concrete", "red_concrete", 1.0, 0.2, 8);
            case "x_dog" -> walker(k, 4, false, false, "brown_concrete", "orange_terracotta", "brown_concrete", "lime_concrete", 0.9, 0.2, 8);
            case "x_titan" -> walker(k, 2, true, true, "iron_block", "gray_concrete", "light_gray_concrete", "orange_concrete", 2.2, 0.14, 8);
            case "x_guard" -> walker(k, 2, true, true, "blue_concrete", "white_concrete", "iron_block", "cyan_concrete", 1.1, 0.18, 8);
            case "x_crab" -> walker(k, 4, true, false, "red_concrete", "red_terracotta", "orange_concrete", "yellow_concrete", 0.8, 0.22, 8);
            case "x_centipede" -> walker(k, 8, false, false, "green_concrete", "lime_concrete", "green_concrete", "red_concrete", 0.8, 0.2, 8);
            case "x_car" -> vehicle(k, false, 4, "red_concrete", "black_concrete", "light_blue_stained_glass", 0.3);
            case "x_tank" -> vehicle(k, true, 4, "green_terracotta", "black_concrete", "iron_block", 0.18);
            case "x_rover" -> vehicle(k, false, 6, "white_concrete", "gray_concrete", "light_gray_stained_glass", 0.2);
            case "x_buggy" -> vehicle(k, false, 4, "orange_concrete", "black_concrete", "iron_bars", 0.35);
            case "x_drone", "x_heli", "x_rocket", "x_ufo" -> NeroMega2.flyer(k, key);
            default -> NeroMega2.other(k, key);
        }
        return k.done();
    }

    // ------------------------------------------------------------------ каркас
    static final class K {
        final NeroArchetypes.B b = new NeroArchetypes.B();
        final String key, L = "{id}_x", G = "mg_{id}", T = "nw_{id}";
        final List<String[]> pult = new ArrayList<>(), brain = new ArrayList<>(), anim = new ArrayList<>(), sys = new ArrayList<>();
        int sx = 24, sy = 0, sz = 10;
        String[][] signs = new String[0][];
        String[] board = new String[0];
        final String ALL_SEL = "@e[type=minecraft:block_display,tag=mg_{id}]";

        K(String key) { this.key = key; }

        static String f(double v) { return String.format(Locale.ROOT, "%.4f", v); }

        static String fl(double v) { return f(v) + "f"; }

        static String arr(double[] v) { return "[" + fl(v[0]) + "," + fl(v[1]) + "," + fl(v[2]) + "]"; }

        static String quat(double[] q) { return "[" + fl(q[0]) + "," + fl(q[1]) + "," + fl(q[2]) + "," + fl(q[3]) + "]"; }

        static double[] qa(char ax, double deg) {
            double h = Math.toRadians(deg) / 2, s = Math.sin(h), c = Math.cos(h);
            return ax == 'x' ? new double[]{s, 0, 0, c} : ax == 'y' ? new double[]{0, s, 0, c} : new double[]{0, 0, s, c};
        }

        static double[] qm(double[] a, double[] b) {
            return new double[]{a[3] * b[0] + a[0] * b[3] + a[1] * b[2] - a[2] * b[1], a[3] * b[1] - a[0] * b[2] + a[1] * b[3] + a[2] * b[0],
                    a[3] * b[2] + a[0] * b[1] - a[1] * b[0] + a[2] * b[3], a[3] * b[3] - a[0] * b[0] - a[1] * b[1] - a[2] * b[2]};
        }

        static double[] rot(double[] q, double[] v) {
            double[] u = {q[0], q[1], q[2]};
            double[] t = {2 * (u[1] * v[2] - u[2] * v[1]), 2 * (u[2] * v[0] - u[0] * v[2]), 2 * (u[0] * v[1] - u[1] * v[0])};
            return new double[]{v[0] + q[3] * t[0] + (u[1] * t[2] - u[2] * t[1]), v[1] + q[3] * t[1] + (u[2] * t[0] - u[0] * t[2]), v[2] + q[3] * t[2] + (u[0] * t[1] - u[1] * t[0])};
        }

        String sel(String n) { return "@e[type=minecraft:block_display,tag=p_{id}_" + n + ",limit=1]"; }

        String selT(String n) { return "@e[type=minecraft:text_display,tag=p_{id}_" + n + ",limit=1]"; }

        void add(List<String[]> l, String label, String cmd, String role) { l.add(new String[]{label, cmd, role}); }

        String summon(String type, String payload, double[] t, double[] s, boolean flip, String name) {
            return "summon minecraft:" + type + " {x+" + sx + "} {y+" + sy + "} {z+" + sz + "} {" + payload + ",interpolation_duration:4,teleport_duration:2,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:"
                    + (flip ? "[1f,0f,0f,0f]" : "[0f,0f,0f,1f]") + ",translation:" + arr(t) + ",scale:" + arr(s) + "},Tags:[\"{tag}\",\"mg_{id}\",\"p_{id}_" + name + "\"]}";
        }

        void block(String n, String blk, double[] t, double[] s, boolean flip) { b.setup(summon("block_display", "block_state:{Name:\"minecraft:" + blk + "\"}", t, s, flip, n)); }

        void text(String n, String txt, String color, double[] t, double sc) {
            b.setup(summon("text_display", "text:'{\"text\":\"" + NeroKit.esc(txt) + "\",\"color\":\"" + color + "\"}',billboard:\"center\"", t, new double[]{sc, sc, sc}, false, n));
        }

        /** Поворот части: q вокруг её угла, translation подбирается так, чтобы центр остался на месте (rotateAboutCenter) либо угол был шарниром (pivot). */
        String spin(String n, double[] center, double[] size, double[] q, int dur) {
            double[] c = rot(q, new double[]{size[0] / 2, size[1] / 2, size[2] / 2});
            double[] t = {center[0] - c[0], center[1] - c[1], center[2] - c[2]};
            return "data merge entity " + sel(n) + " {start_interpolation:0,interpolation_duration:" + dur + ",transformation:{translation:" + arr(t) + ",left_rotation:" + quat(q) + "}}";
        }

        String swing(String n, double[] q, int dur) { return "data merge entity " + sel(n) + " {start_interpolation:0,interpolation_duration:" + dur + ",transformation:{left_rotation:" + quat(q) + "}}"; }

        String move(String n, double[] t, int dur) { return "data merge entity " + sel(n) + " {start_interpolation:0,interpolation_duration:" + dur + ",transformation:{translation:" + arr(t) + "}}"; }

        String recolor(String n, String blk) { return "data merge entity " + sel(n) + " {block_state:{Name:\"minecraft:" + blk + "\"}}"; }

        String status(String txt, String color) { return "data modify entity @e[type=minecraft:text_display,tag=st_{id},limit=1] text set value '{\"text\":\"" + NeroKit.esc(txt) + "\",\"color\":\"" + color + "\"}'"; }

        String hostile(String pos, int r, String tagCmd) { return ""; }

        /** Метки врагов: 8 команд. */
        void hostiles(List<String[]> out, String gate, String posRoot, int r) {
            String[][] h = {{"нежить", "#minecraft:undead"}, {"криперы", "minecraft:creeper"}, {"пауки", "minecraft:spider"}, {"пещерные пауки", "minecraft:cave_spider"}, {"ведьмы", "minecraft:witch"},
                    {"налётчики", "#minecraft:raiders"}, {"слизни", "minecraft:slime"}, {"фантомы", "minecraft:phantom"}};
            for (String[] x : h) add(out, "Поиск врагов: " + x[0], gate + posRoot + " run tag @e[type=" + x[1] + ",distance=.." + r + "] add " + T, "Помечает враждебных мобов типа «" + x[0] + "»");
        }

        /** Энергия и состояние: 14 команд. */
        void systems(String movingCond) {
            add(sys, "Батарея: расход", "execute if score #go " + L + " matches 1 if score #bat " + L + " matches 1.. run scoreboard players remove #bat " + L + " 1", "Заряд уменьшается во время работы");
            add(sys, "Батарея: подзарядка", "execute if score #go " + L + " matches 0 if score #bat " + L + " matches ..999 run scoreboard players add #bat " + L + " 2", "Во время простоя заряд растёт");
            add(sys, "Батарея: сброс флага", "scoreboard players set #low " + L + " 0", "Готовит проверку заряда");
            add(sys, "Батарея: низкий заряд", "execute if score #bat " + L + " matches ..40 run scoreboard players set #low " + L + " 1", "При заряде ниже 4% механизм останавливается");
            String[] lv = {"901..1000", "801..900", "701..800", "601..700", "501..600", "401..500", "301..400", "201..300", "101..200", "0..100"};
            for (int i = 0; i < lv.length; i++)
                add(sys, "Табло: заряд " + (100 - i * 10) + "%", "execute if score #bat " + L + " matches " + lv[i] + " run " + status("Заряд " + (100 - i * 10) + "%" + (i >= 8 ? " НИЗКИЙ" : ""), i >= 8 ? "red" : i >= 5 ? "yellow" : "green"), "Показывает уровень заряда на табло");
        }

        void pultAll(String[] labels, String[] cmds, String[][] signLines, String[] boardLines) {
            for (int i = 0; i < labels.length; i++) add(pult, "Кнопка " + labels[i], NeroKit.poll(i) + cmds[i], "Кнопка пульта «" + labels[i] + "»");
            add(pult, "Лампа: включено", "execute if score #on " + L + " matches 1 run setblock {x+13} {y+2} {z+0} minecraft:lime_concrete", "Зелёная лампа, пока механизм включён");
            add(pult, "Лампа: выключено", "execute unless score #on " + L + " matches 1 run setblock {x+13} {y+2} {z+0} minecraft:red_concrete", "Красная лампа, когда выключен");
            signs = signLines;
            board = boardLines;
        }

        JsonObject done() {
            NeroKit.console(b, 6);
            String S = "scoreboard players set ";
            b.setup("scoreboard objectives add " + L + " dummy", S + "#on " + L + " 0", S + "#go " + L + " 0", S + "#ph " + L + " 0", S + "#sc " + L + " 0", S + "#bat " + L + " 1000", S + "#low " + L + " 0",
                    S + "#rest " + L + " 0", S + "#cd " + L + " 0", S + "#hold " + L + " 0", S + "#dir " + L + " 0", S + "#run " + L + " 0", S + "#t " + L + " 0", S + "#st " + L + " 0");
            b.setup("summon minecraft:text_display {x+7} {y+5} {z+0} {text:'{\"text\":\"Выключено\",\"color\":\"gray\"}',billboard:\"center\",Tags:[\"{tag}\",\"st_{id}\"]}");
            for (int i = 0; i < signs.length; i++) b.setup(NeroKit.sign(1 + 2 * i, 0, -1, signs[i][0], signs[i][1], signs[i][2], signs[i][3]));
            NeroKit.board(b, 7, 8, 0, board);
            String nm = NeroMega.name(key);
            if (!pult.isEmpty()) b.chain(nm + ": пульт", pult.toArray(new String[0][]));
            if (!brain.isEmpty()) b.chain(nm + ": управление движением", brain.toArray(new String[0][]));
            if (!anim.isEmpty()) b.chain(nm + ": анимация", anim.toArray(new String[0][]));
            if (!sys.isEmpty()) b.chain(nm + ": системы", sys.toArray(new String[0][]));
            b.help = nm + " (вымышленная игровая конструкция): " + NeroMega.find(key).desc() + ". Пульт слева, таблички под кнопками. Командных блоков в логике: " + (pult.size() + brain.size() + anim.size() + sys.size())
                    + ". Подвижные части из display-сущностей (не твёрдые).";
            return b.done();
        }
    }

    // ------------------------------------------------------------------ ходячие роботы
    private static void walker(K k, int legs, boolean arms, boolean guard, String body, String limb, String head, String eye, double sc, double speed, int frames) {
        String L = k.L, S = "scoreboard players set ";
        double legH = (legs >= 6 ? 0.6 : 0.9) * sc, bw = (legs >= 6 ? 1.1 : 0.9) * sc, bh = (legs >= 6 ? 0.45 : 0.9) * sc, bd = (legs >= 6 ? (0.5 + 0.18 * legs) : 0.55) * sc, hw = 0.55 * sc, lw = 0.2 * sc;
        if (k.key.equals("x_dog")) { bw = 0.6 * sc; bd = 1.4 * sc; bh = 0.6 * sc; }
        k.block("body", body, new double[]{-bw / 2, legH, -bd / 2}, new double[]{bw, bh, bd}, false);
        double hz = k.key.equals("x_dog") ? bd / 2 : 0;
        k.block("head", head, new double[]{-hw / 2, legH + bh, hz - hw / 2}, new double[]{hw, hw, hw}, false);
        k.block("eyeL", eye, new double[]{-hw * 0.3, legH + bh + hw * 0.55, hz + hw / 2}, new double[]{0.1 * sc, 0.1 * sc, 0.05 * sc}, false);
        k.block("eyeR", eye, new double[]{hw * 0.3 - 0.1 * sc, legH + bh + hw * 0.55, hz + hw / 2}, new double[]{0.1 * sc, 0.1 * sc, 0.05 * sc}, false);
        k.block("ant", "lightning_rod", new double[]{-0.03 * sc, legH + bh + hw, hz - 0.03 * sc}, new double[]{0.06 * sc, 0.35 * sc, 0.06 * sc}, false);
        List<double[]> hips = new ArrayList<>();
        if (legs == 2) { hips.add(new double[]{-bw / 4, 0}); hips.add(new double[]{bw / 4, 0}); }
        else if (legs == 4) { for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) hips.add(new double[]{sx * (bw / 2 - lw), sz * bd / 3}); }
        else { int per = legs / 2; for (int sx : new int[]{-1, 1}) for (int i = 0; i < per; i++) hips.add(new double[]{sx * (bw / 2), -bd / 2 + bd * (i + 0.5) / per}); }
        List<String> limbs = new ArrayList<>();
        for (int i = 0; i < hips.size(); i++) {
            k.block("leg" + i, limb, new double[]{hips.get(i)[0] - lw / 2, legH, hips.get(i)[1] + lw / 2}, new double[]{lw, legH, lw}, true);
            limbs.add("leg" + i);
        }
        List<String> armList = new ArrayList<>();
        if (arms) for (int i = 0; i < 2; i++) {
            double ax = (i == 0 ? -1 : 1) * (bw / 2 + lw * 0.6);
            k.block("arm" + i, limb, new double[]{ax - lw / 2, legH + bh - 0.1 * sc, lw / 2}, new double[]{lw, 0.75 * sc, lw}, true);
            armList.add("arm" + i);
        }
        if (k.key.equals("x_dog")) { k.block("tail", limb, new double[]{-0.04 * sc, legH + bh - 0.05 * sc, -bd / 2 + 0.05 * sc}, new double[]{0.08 * sc, 0.5 * sc, 0.08 * sc}, true); }
        k.text("hud", "", "white", new double[]{0, legH + bh + hw + 0.5 * sc, 0}, 0.01);

        String ROOT = k.sel("body"), ALLs = k.ALL_SEL, gate = "execute if score #on " + L + " matches 1 if score #low " + L + " matches 0 if score #hold " + L + " matches 0 ";
        int R = 26;
        String SPD = K.f(speed);
        k.add(k.brain, "Сброс движения", S + "#go " + L + " 0", "Каждый такт решает заново, идти или стоять");
        k.add(k.brain, "Есть ли игрок", gate + "as " + ROOT + " at @s if entity @p[distance=3.." + R + "] run " + S + "#go " + L + " 1", "Идёт к игроку, если он дальше 3 и ближе " + R + " блоков");
        k.add(k.brain, "Повернуться к игроку", "execute if score #go " + L + " matches 1 as " + ROOT + " at @s run tp " + ALLs + " ~ ~ ~ facing entity @p feet", "Все части поворачиваются к игроку");
        k.add(k.brain, "Выровнять наклон", "execute if score #go " + L + " matches 1 as " + ALLs + " at @s run tp @s ~ ~ ~ ~ 0", "Убирает наклон вверх и вниз");
        k.add(k.brain, "Шаг вперёд", "execute if score #go " + L + " matches 1 as " + ROOT + " at @s if block ^ ^ ^0.9 minecraft:air if block ^ ^1 ^0.9 minecraft:air run tp " + ALLs + " ^ ^ ^" + SPD, "Шаг, если впереди свободно");
        k.add(k.brain, "Шаг на ступеньку", "execute if score #go " + L + " matches 1 as " + ROOT + " at @s unless block ^ ^ ^0.9 minecraft:air if block ^ ^1 ^0.9 minecraft:air if block ^ ^2 ^0.9 minecraft:air run tp " + ALLs + " ^ ^1.05 ^" + SPD, "Поднимается на блок высотой 1");
        k.add(k.brain, "Гравитация", "execute as " + ROOT + " at @s if block ~ ~-0.1 ~ minecraft:air if block ~ ~-0.6 ~ minecraft:air run tp " + ALLs + " ~ ~-0.3 ~", "Падает, если под ногами пусто");
        k.add(k.brain, "Выйти из блока", "execute as " + ROOT + " at @s unless block ~ ~ ~ minecraft:air unless block ~ ~ ~ minecraft:water run tp " + ALLs + " ~ ~0.25 ~", "Поднимается, если оказался внутри блока");
        k.add(k.brain, "Фаза шага", "execute if score #go " + L + " matches 1 run scoreboard players add #ph " + L + " 1", "Счётчик цикла шагов");
        k.add(k.brain, "Фаза: круг", "execute if score #ph " + L + " matches 16.. run " + S + "#ph " + L + " 0", "Цикл шагов начинается заново");
        k.add(k.brain, "Звук шага 1", "execute if score #go " + L + " matches 1 if score #ph " + L + " matches 0 as " + ROOT + " at @s run playsound minecraft:block.piston.extend neutral @a ~ ~ ~ 0.5 1.6", "Звук шага");
        k.add(k.brain, "Звук шага 2", "execute if score #go " + L + " matches 1 if score #ph " + L + " matches 8 as " + ROOT + " at @s run playsound minecraft:block.piston.contract neutral @a ~ ~ ~ 0.5 1.6", "Звук шага");
        k.add(k.brain, "Пыль из-под ног", "execute if score #go " + L + " matches 1 if score #ph " + L + " matches 0 as " + ROOT + " at @s run particle minecraft:poof ~ ~0.1 ~ 0.2 0 0.2 0.01 3", "Частицы при шаге");
        k.add(k.brain, "Глаза: идёт", "execute if score #go " + L + " matches 1 run " + k.recolor("eyeL", "red_concrete"), "Левый глаз красный при движении");
        k.add(k.brain, "Глаза: идёт 2", "execute if score #go " + L + " matches 1 run " + k.recolor("eyeR", "red_concrete"), "Правый глаз красный при движении");
        k.add(k.brain, "Глаза: стоит", "execute if score #go " + L + " matches 0 run " + k.recolor("eyeL", eye), "Левый глаз спокойного цвета");
        k.add(k.brain, "Глаза: стоит 2", "execute if score #go " + L + " matches 0 run " + k.recolor("eyeR", eye), "Правый глаз спокойного цвета");
        // шаги
        for (int f = 0; f < frames; f++) {
            for (int i = 0; i < limbs.size(); i++) {
                double a = 32 * Math.sin(2 * Math.PI * (f + (i % 2) * frames / 2.0) / frames);
                k.add(k.anim, "Кадр " + (f + 1) + ": " + limbs.get(i), "execute if score #ph " + L + " matches " + (2 * f) + " run " + k.swing(limbs.get(i), K.qa('x', a), 4), "Качание ноги в кадре шага " + (f + 1));
            }
            for (int i = 0; i < armList.size(); i++) {
                double a = -28 * Math.sin(2 * Math.PI * (f + (i % 2) * frames / 2.0) / frames);
                k.add(k.anim, "Кадр " + (f + 1) + ": " + armList.get(i), "execute if score #ph " + L + " matches " + (2 * f) + " run " + k.swing(armList.get(i), K.qa('x', a), 4), "Качание руки в кадре шага " + (f + 1));
            }
            if (k.key.equals("x_dog")) k.add(k.anim, "Кадр " + (f + 1) + ": хвост", "execute if score #ph " + L + " matches " + (2 * f) + " run " + k.swing("tail", K.qa('z', 35 * Math.sin(4 * Math.PI * f / frames)), 3), "Виляет хвостом");
        }
        // поза покоя и поворот головы
        for (String n : limbs) k.add(k.anim, "Покой: " + n, "execute if score #go " + L + " matches 0 if score #rest " + L + " matches 0 run " + k.swing(n, K.qa('x', 0), 6), "Возвращает конечность в исходное положение");
        for (String n : armList) k.add(k.anim, "Покой: " + n, "execute if score #go " + L + " matches 0 if score #rest " + L + " matches 0 run " + k.swing(n, K.qa('x', 0), 6), "Возвращает руку в исходное положение");
        k.add(k.anim, "Покой: отметка", "execute if score #go " + L + " matches 0 run " + S + "#rest " + L + " 1", "Поза покоя установлена");
        k.add(k.anim, "Движение: снять отметку", "execute if score #go " + L + " matches 1 run " + S + "#rest " + L + " 0", "При движении поза покоя больше не держится");
        k.add(k.anim, "Покой: счётчик головы", "execute if score #go " + L + " matches 0 run scoreboard players add #sc " + L + " 1", "Счётчик осмотра головой");
        k.add(k.anim, "Покой: круг головы", "execute if score #sc " + L + " matches 48.. run " + S + "#sc " + L + " 0", "Осмотр начинается заново");
        for (int f = 0; f < 8; f++) k.add(k.anim, "Осмотр головой: кадр " + (f + 1), "execute if score #go " + L + " matches 0 if score #sc " + L + " matches " + (f * 6) + " run " + k.swing("head", K.qa('y', 25 * Math.sin(2 * Math.PI * f / 8)), 6), "Робот осматривается");
        if (guard) {
            String T = k.T;
            k.hostiles(k.anim, "execute if score #on " + L + " matches 1 if score #cd " + L + " matches 0 ", "as " + ROOT + " at @s", 6);
            k.add(k.anim, "Удар по врагу", "execute if score #cd " + L + " matches 0 if entity @e[tag=" + T + "] run damage @e[tag=" + T + ",sort=nearest,limit=1] 6 minecraft:magic", "Бьёт ближайшего врага");
            k.add(k.anim, "Искры удара", "execute if score #cd " + L + " matches 0 as @e[tag=" + T + ",sort=nearest,limit=1] at @s run particle minecraft:crit ~ ~1 ~ 0.3 0.3 0.3 0.2 15", "Эффект удара");
            k.add(k.anim, "Звук удара", "execute if score #cd " + L + " matches 0 if entity @e[tag=" + T + "] as " + ROOT + " at @s run playsound minecraft:entity.iron_golem.attack neutral @a ~ ~ ~ 1 0.8", "Звук удара");
            if (!armList.isEmpty()) k.add(k.anim, "Взмах рукой", "execute if score #cd " + L + " matches 0 if entity @e[tag=" + T + "] run " + k.swing("arm1", K.qa('x', -85), 3), "Рука поднимается для удара");
            k.add(k.anim, "Перезарядка удара", "execute if score #cd " + L + " matches 0 if entity @e[tag=" + T + "] run " + S + "#cd " + L + " 18", "Удар раз в 18 тактов");
            k.add(k.anim, "Снять метки врагов", "tag @e[tag=" + T + "] remove " + T, "Убирает временные метки");
            if (!armList.isEmpty()) k.add(k.anim, "Рука вниз", "execute if score #cd " + L + " matches 9 run " + k.swing("arm1", K.qa('x', 0), 5), "Рука возвращается после удара");
            k.add(k.anim, "Отсчёт удара", "execute if score #cd " + L + " matches 1.. run scoreboard players remove #cd " + L + " 1", "Время до следующего удара");
        }
        k.systems("");
        String[] labels = {"ВКЛ", "ВЫКЛ", "ДОМОЙ", "ЗАРЯД", "СТОЯТЬ", "ИДТИ"};
        String[] cmds = {S + "#on " + L + " 1", S + "#on " + L + " 0", "tp " + ALLs + " {x+" + k.sx + "} {y+" + k.sy + "} {z+" + k.sz + "}", S + "#bat " + L + " 1000", S + "#hold " + L + " 1", S + "#hold " + L + " 0"};
        k.pultAll(labels, cmds, new String[][]{{"ВКЛ", "включает", "механизм", ""}, {"ВЫКЛ", "выключает", "механизм", ""}, {"ДОМОЙ", "возвращает", "на старт", ""}, {"ЗАРЯД", "полная", "батарея", ""}, {"СТОЯТЬ", "остановка", "на месте", ""}, {"ИДТИ", "снова", "следует", "за игроком"}},
                new String[]{NeroMega.name(k.key).toUpperCase(), "Идёт за игроком, шагает,", "огибает ступени.", "Есть батарея и табло.", "ВКЛ - запуск, СТОЯТЬ - пауза", "ДОМОЙ - вернуть на старт"});
    }

    // ------------------------------------------------------------------ транспорт
    private static void vehicle(K k, boolean tank, int wheels, String body, String wheelB, String accent, double speed) {
        String L = k.L, S = "scoreboard players set ";
        k.block("hull", body, new double[]{-0.8, 0.35, -1.3}, new double[]{1.6, 0.5, 2.6}, false);
        if (tank) {
            k.block("trackL", wheelB, new double[]{-1.05, 0.1, -1.4}, new double[]{0.35, 0.6, 2.8}, false);
            k.block("trackR", wheelB, new double[]{0.7, 0.1, -1.4}, new double[]{0.35, 0.6, 2.8}, false);
            k.block("turret", accent, new double[]{-0.5, 0.85, -0.55}, new double[]{1.0, 0.4, 1.1}, false);
            k.block("barrel", wheelB, new double[]{-0.08, 0.98, 0.4}, new double[]{0.16, 0.16, 1.6}, false);
            k.b.setup(k.summon("block_display", "block_state:{Name:\"minecraft:iron_block\"}", new double[]{-0.18, 1.0, -0.1}, new double[]{0.36, 0.3, 0.3}, false, "hatch"));
        } else {
            k.block("cabin", accent, new double[]{-0.55, 0.85, -0.5}, new double[]{1.1, 0.55, 1.1}, false);
            k.block("roof", body, new double[]{-0.6, 1.4, -0.55}, new double[]{1.2, 0.08, 1.2}, false);
            k.block("lampL", "yellow_concrete", new double[]{-0.7, 0.55, 1.28}, new double[]{0.25, 0.15, 0.05}, false);
            k.block("lampR", "yellow_concrete", new double[]{0.45, 0.55, 1.28}, new double[]{0.25, 0.15, 0.05}, false);
            k.block("ant", "lightning_rod", new double[]{0.6, 1.4, -0.6}, new double[]{0.05, 0.6, 0.05}, false);
        }
        double[] size = {0.22, 0.55, 0.55};
        List<double[]> wc = new ArrayList<>();
        int per = wheels / 2;
        for (int sx : new int[]{-1, 1}) for (int i = 0; i < per; i++) wc.add(new double[]{sx * 0.95, 0.28, -1.0 + 2.0 * i / Math.max(1, per - 1)});
        for (int i = 0; i < wc.size() && !tank; i++) k.block("w" + i, wheelB, new double[]{wc.get(i)[0] - 0.11, 0, wc.get(i)[2] - 0.27}, size, false);
        String HULL = k.sel("hull"), ALLs = k.ALL_SEL, gate = "execute if score #on " + L + " matches 1 if score #low " + L + " matches 0 ";
        String SPD = K.f(speed);
        String[] pr = {"fwd", "back"};
        k.add(k.brain, "Ход вперёд: свободно", gate + "if score #dir " + L + " matches 1 as " + HULL + " at @s if block ^ ^ ^1.4 minecraft:air if block ^ ^1 ^1.4 minecraft:air run tp " + ALLs + " ^ ^ ^" + SPD, "Едет вперёд, если впереди свободно");
        k.add(k.brain, "Ход вперёд: ступенька", gate + "if score #dir " + L + " matches 1 as " + HULL + " at @s unless block ^ ^ ^1.4 minecraft:air if block ^ ^1 ^1.4 minecraft:air if block ^ ^2 ^1.4 minecraft:air run tp " + ALLs + " ^ ^1.05 ^" + SPD, "Заезжает на ступеньку высотой 1");
        k.add(k.brain, "Ход назад: свободно", gate + "if score #dir " + L + " matches -1 as " + HULL + " at @s if block ^ ^ ^-1.4 minecraft:air if block ^ ^1 ^-1.4 minecraft:air run tp " + ALLs + " ^ ^ ^-" + SPD, "Едет назад, если сзади свободно");
        k.add(k.brain, "Ход назад: ступенька", gate + "if score #dir " + L + " matches -1 as " + HULL + " at @s unless block ^ ^ ^-1.4 minecraft:air if block ^ ^1 ^-1.4 minecraft:air if block ^ ^2 ^-1.4 minecraft:air run tp " + ALLs + " ^ ^1.05 ^-" + SPD, "Заезжает назад на ступеньку");
        k.add(k.brain, "Гравитация", "execute as " + HULL + " at @s if block ~ ~-0.1 ~ minecraft:air if block ~ ~-0.6 ~ minecraft:air run tp " + ALLs + " ~ ~-0.3 ~", "Падает, если под колёсами пусто");
        k.add(k.brain, "Выйти из блока", "execute as " + HULL + " at @s unless block ~ ~ ~ minecraft:air unless block ~ ~ ~ minecraft:water run tp " + ALLs + " ~ ~0.25 ~", "Поднимается, если оказался внутри блока");
        k.add(k.brain, "Движение: признак", "scoreboard players set #go " + L + " 0", "Сбрасывает признак движения");
        k.add(k.brain, "Движение: вперёд", "execute if score #dir " + L + " matches 1 if score #on " + L + " matches 1 run " + S + "#go " + L + " 1", "Признак движения вперёд");
        k.add(k.brain, "Движение: назад", "execute if score #dir " + L + " matches -1 if score #on " + L + " matches 1 run " + S + "#go " + L + " 1", "Признак движения назад");
        k.add(k.brain, "Автопилот: поворот", "execute if score #hold " + L + " matches 1 if score #go " + L + " matches 1 as " + ALLs + " at @s run tp @s ~ ~ ~ ~1.2 ~", "Автопилот плавно поворачивает");
        k.add(k.brain, "Фаза колёс вперёд", "execute if score #dir " + L + " matches 1 if score #on " + L + " matches 1 run scoreboard players add #ph " + L + " 1", "Счётчик вращения колёс вперёд");
        k.add(k.brain, "Фаза колёс назад", "execute if score #dir " + L + " matches -1 if score #on " + L + " matches 1 run scoreboard players remove #ph " + L + " 1", "Счётчик вращения колёс назад");
        k.add(k.brain, "Фаза: верхний круг", "execute if score #ph " + L + " matches 16.. run " + S + "#ph " + L + " 0", "Круг вращения замыкается");
        k.add(k.brain, "Фаза: нижний круг", "execute if score #ph " + L + " matches ..-1 run " + S + "#ph " + L + " 15", "Круг вращения замыкается назад");
        k.add(k.brain, "Выхлоп", "execute if score #go " + L + " matches 1 if score #ph " + L + " matches 0 as " + HULL + " at @s run particle minecraft:smoke ^ ^0.4 ^-1.4 0.05 0.05 0.05 0.01 3", "Дым сзади при движении");
        k.add(k.brain, "Звук мотора", "execute if score #go " + L + " matches 1 if score #ph " + L + " matches 0 as " + HULL + " at @s run playsound minecraft:entity.minecart.riding neutral @a ~ ~ ~ 0.6 0.6", "Звук движения");
        if (!tank) {
            k.add(k.brain, "Фары: включены", "execute if score #on " + L + " matches 1 run " + k.recolor("lampL", "yellow_concrete"), "Левая фара горит");
            k.add(k.brain, "Фары 2: включены", "execute if score #on " + L + " matches 1 run " + k.recolor("lampR", "yellow_concrete"), "Правая фара горит");
            k.add(k.brain, "Фары: выключены", "execute unless score #on " + L + " matches 1 run " + k.recolor("lampL", "gray_concrete"), "Левая фара выключена");
            k.add(k.brain, "Фары 2: выключены", "execute unless score #on " + L + " matches 1 run " + k.recolor("lampR", "gray_concrete"), "Правая фара выключена");
        }
        for (int f = 0; f < 8 && !tank; f++)
            for (int i = 0; i < wc.size(); i++) k.add(k.anim, "Колесо " + (i + 1) + ": кадр " + (f + 1), "execute if score #ph " + L + " matches " + (2 * f) + " run " + k.spin("w" + i, new double[]{wc.get(i)[0], wc.get(i)[1], wc.get(i)[2]}, size, K.qa('x', f * 45), 3), "Вращение колеса");
        if (tank) {
            String T = k.T, TUR = k.sel("turret").replace("type=minecraft:block_display", "type=minecraft:block_display");
            String TURS = "@e[type=minecraft:block_display,tag=p_{id}_turret,limit=1]";
            k.hostiles(k.anim, "execute if score #on " + L + " matches 1 if score #cd " + L + " matches 0 ", "as " + HULL + " at @s", 22);
            k.add(k.anim, "Прицел башни", "execute if entity @e[tag=" + T + "] as " + TURS + " at @s run tp @s ~ ~ ~ facing entity @e[tag=" + T + ",sort=nearest,limit=1] feet", "Башня разворачивается на ближайшего врага");
            k.add(k.anim, "Прицел ствола", "execute if entity @e[tag=" + T + "] as @e[type=minecraft:block_display,tag=p_{id}_barrel,limit=1] at @s run tp @s ~ ~ ~ facing entity @e[tag=" + T + ",sort=nearest,limit=1] feet", "Ствол разворачивается вместе с башней");
            k.add(k.anim, "Люк за башней", "execute if entity @e[tag=" + T + "] as @e[type=minecraft:block_display,tag=p_{id}_hatch,limit=1] at @s run tp @s ~ ~ ~ facing entity @e[tag=" + T + ",sort=nearest,limit=1] feet", "Люк вращается с башней");
            for (int n = 1; n <= 8; n++) k.add(k.anim, "Трассер " + n, "execute if score #cd " + L + " matches 0 if entity @e[tag=" + T + "] as " + TURS + " at @s positioned ^ ^1 ^" + (n * 2 + 1) + " run particle minecraft:flame ~ ~ ~ 0 0 0 0 1", "Видимая линия выстрела");
            k.add(k.anim, "Выстрел: урон", "execute if score #cd " + L + " matches 0 if entity @e[tag=" + T + "] run damage @e[tag=" + T + ",sort=nearest,limit=1] 9 minecraft:explosion", "Урон ближайшему врагу");
            k.add(k.anim, "Выстрел: вспышка", "execute if score #cd " + L + " matches 0 if entity @e[tag=" + T + "] as @e[tag=" + T + ",sort=nearest,limit=1] at @s run particle minecraft:explosion ~ ~1 ~ 0 0 0 0 1", "Вспышка на цели");
            k.add(k.anim, "Выстрел: звук", "execute if score #cd " + L + " matches 0 if entity @e[tag=" + T + "] as " + HULL + " at @s run playsound minecraft:entity.generic.explode neutral @a ~ ~ ~ 1.5 1.2", "Звук выстрела");
            k.add(k.anim, "Отдача ствола", "execute if score #cd " + L + " matches 0 if entity @e[tag=" + T + "] run " + k.move("barrel", new double[]{-0.08, 0.98, 0.15}, 1), "Ствол откатывается назад");
            k.add(k.anim, "Перезарядка: старт", "execute if score #cd " + L + " matches 0 if entity @e[tag=" + T + "] run " + S + "#cd " + L + " 40", "Перезарядка 2 секунды");
            k.add(k.anim, "Снять метки врагов", "tag @e[tag=" + T + "] remove " + T, "Убирает временные метки");
            k.add(k.anim, "Ствол возвращается", "execute if score #cd " + L + " matches 34 run " + k.move("barrel", new double[]{-0.08, 0.98, 0.4}, 6), "Ствол возвращается вперёд");
            k.add(k.anim, "Отсчёт перезарядки", "execute if score #cd " + L + " matches 1.. run scoreboard players remove #cd " + L + " 1", "Время до следующего выстрела");
        }
        k.systems("");
        String[] labels = {"ВПЕРЁД", "НАЗАД", "СТОП", "ВЛЕВО", "ВПРАВО", tank ? "АВТО" : "АВТО"};
        k.add(k.pult, "Поворот влево: фронт", NeroKit.poll(3) + "execute unless score #pL " + L + " matches 1 as " + ALLs + " at @s run tp @s ~ ~ ~ ~-22 ~", "Один поворот влево на 22 градуса за нажатие");
        k.add(k.pult, "Поворот влево: отметка", NeroKit.poll(3) + S + "#pL " + L + " 1", "Запоминает, что кнопка нажата");
        k.add(k.pult, "Поворот вправо: фронт", NeroKit.poll(4) + "execute unless score #pR " + L + " matches 1 as " + ALLs + " at @s run tp @s ~ ~ ~ ~22 ~", "Один поворот вправо на 22 градуса за нажатие");
        k.add(k.pult, "Поворот вправо: отметка", NeroKit.poll(4) + S + "#pR " + L + " 1", "Запоминает, что кнопка нажата");
        k.add(k.pult, "Поворот влево: сброс", "execute unless block {x+7} {y+1} {z-1} minecraft:stone_button[powered=true] run " + S + "#pL " + L + " 0", "Кнопка отпущена");
        k.add(k.pult, "Поворот вправо: сброс", "execute unless block {x+9} {y+1} {z-1} minecraft:stone_button[powered=true] run " + S + "#pR " + L + " 0", "Кнопка отпущена");
        String[] cmds = {S + "#dir " + L + " 1", S + "#dir " + L + " -1", S + "#dir " + L + " 0", "particle minecraft:note ~ ~ ~ 0 0 0 0 1", "particle minecraft:note ~ ~ ~ 0 0 0 0 1", S + "#hold " + L + " 1"};
        k.pultAll(new String[]{"ВПЕРЁД", "НАЗАД", "СТОП", "ВЛЕВО", "ВПРАВО", "АВТО"}, cmds, new String[][]{{"ВПЕРЁД", "едет вперёд", "пока не СТОП", ""}, {"НАЗАД", "едет назад", "пока не СТОП", ""}, {"СТОП", "останавливает", "машину", ""}, {"ВЛЕВО", "поворот", "на 22 градуса", ""}, {"ВПРАВО", "поворот", "на 22 градуса", ""}, {"АВТО", "автопилот:", "плавный круг", "(СТОП выкл)"}},
                new String[]{NeroMega.name(k.key).toUpperCase(), "Рули кнопками.", "ВКЛ: поставь на ВПЕРЁД;", "СТОП - остановка.", tank ? "Башня сама бьёт врагов." : "Фары, колёса, автопилот.", "Включение: кнопка на стене"});
        k.add(k.pult, "Включить при движении", NeroKit.poll(0) + S + "#on " + L + " 1", "ВПЕРЁД заодно включает питание");
        k.add(k.pult, "Включить при движении назад", NeroKit.poll(1) + S + "#on " + L + " 1", "НАЗАД заодно включает питание");
        k.add(k.pult, "Автопилот: стоп", NeroKit.poll(2) + S + "#hold " + L + " 0", "СТОП выключает автопилот");
    }
}
