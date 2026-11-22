package com.mackros;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 15 вымышленных игровых «оружий» на командных блоках (турели и установки). Они бьют только враждебных мобов поблизости
 * (нежить, криперы, пауки, ведьмы, налётчики, слизни, фантомы); игроков, животных и жителей не трогают.
 * Каждое: модель из блоков, пульт (ВКЛ, ВЫКЛ, ВЫСТРЕЛ/ТЕСТ), таблички «что и как» и логика из десятков командных блоков.
 */
final class NeroWeapons {
    record W(String key, String name, String re, int range, boolean auto, int cd, String how) { }

    static final W[] ALL = {
            new W("w_laser", "Лазерная турель", "лазер", 24, true, 15, "сама находит врага и бьёт лучом"),
            new W("w_rail", "Рельсотрон", "рельсотрон|рейлган", 30, false, 60, "заряжается и пробивает всех на линии выстрела"),
            new W("w_flame", "Огнемёт", "огнемет", 9, true, 10, "поджигает врагов перед собой"),
            new W("w_ice", "Ледяная пушка", "ледян\\w*\\s+пушк|криопушк", 16, true, 25, "замораживает и замедляет врагов"),
            new W("w_tesla", "Тесла-катушка", "тесла|электрошок", 12, true, 30, "бьёт молнией до трёх врагов сразу"),
            new W("w_grenade", "Гранатомёт", "гранатомет", 28, false, 40, "стреляет вперёд, взрыв в 18 блоках от ствола"),
            new W("w_shotgun", "Дробовик-турель", "дробовик|картечниц", 14, false, 30, "стреляет веером из пяти зарядов"),
            new W("w_mortar", "Миномёт", "миномет", 30, true, 80, "выбирает врага и через секунду бьёт по нему с неба"),
            new W("w_gravity", "Гравитационная пушка", "гравитацион|гравипушк", 14, true, 10, "притягивает врагов и поднимает над землёй"),
            new W("w_plasma", "Плазмомёт", "плазмомет|плазменн", 22, true, 25, "стреляет плазменными сгустками"),
            new W("w_flak", "Зенитка", "зенитк", 26, true, 12, "сбивает летающих врагов (фантомов) и бьёт наземных"),
            new W("w_mines", "Минное поле", "минн\\w*\\s+пол", 14, true, 0, "девять мин взрываются под врагами"),
            new W("w_shield", "Силовой щит-генератор", "силов\\w*\\s+щит|щит-генератор", 7, true, 0, "отталкивает врагов от центра и защищает зону"),
            new W("w_orbital", "Орбитальная пушка", "орбитальн", 40, true, 120, "выбирает врага, заряжает луч 3 секунды и бьёт с неба"),
            new W("w_sniper", "Снайперская башня", "снайперск", 40, false, 80, "один сильный выстрел вперёд по линии")};

    static W find(String k) { for (W w : ALL) if (w.key().equals(k)) return w; return ALL[0]; }

    static String detect(String t) { for (W w : ALL) if (NeroFree.has(t, w.re())) return w.key(); return null; }

    static String name(String k) { return find(k).name(); }

    static String works(String k) {
        W w = find(k);
        return "вымышленное игровое устройство Minecraft: " + w.how() + ". Бьёт только враждебных мобов в радиусе " + w.range() + " блоков, игроков, животных и жителей не трогает. Пульт: ВКЛ, ВЫКЛ, "
                + (w.auto() ? "ТЕСТ" : "ВЫСТРЕЛ") + ", таблички объясняют кнопки";
    }

    private static String[] style(String k) {
        return switch (k) {
            case "w_laser" -> new String[]{"gray_concrete", "white_concrete", "cyan_concrete", "4"};
            case "w_rail" -> new String[]{"black_concrete", "gray_concrete", "light_blue_concrete", "9"};
            case "w_flame" -> new String[]{"bricks", "red_terracotta", "orange_concrete", "3"};
            case "w_ice" -> new String[]{"packed_ice", "light_blue_concrete", "blue_ice", "4"};
            case "w_tesla" -> new String[]{"iron_block", "copper_block", "sea_lantern", "2"};
            case "w_grenade" -> new String[]{"green_concrete", "green_terracotta", "iron_block", "5"};
            case "w_shotgun" -> new String[]{"brown_concrete", "oak_planks", "iron_block", "3"};
            case "w_mortar" -> new String[]{"stone_bricks", "gray_concrete", "iron_block", "2"};
            case "w_gravity" -> new String[]{"purple_concrete", "magenta_concrete", "amethyst_block", "3"};
            case "w_plasma" -> new String[]{"black_concrete", "purple_concrete", "sea_lantern", "4"};
            case "w_flak" -> new String[]{"gray_concrete", "iron_block", "iron_block", "4"};
            case "w_mines" -> new String[]{"sandstone", "cut_sandstone", "iron_block", "2"};
            case "w_shield" -> new String[]{"white_concrete", "quartz_block", "sea_lantern", "1"};
            case "w_orbital" -> new String[]{"black_concrete", "blue_concrete", "sea_lantern", "3"};
            default -> new String[]{"dark_oak_planks", "spruce_planks", "iron_block", "8"};
        };
    }

    private static final int[][] MINES = {{22, 16}, {26, 16}, {30, 16}, {22, 20}, {26, 20}, {30, 20}, {22, 24}, {26, 24}, {30, 24}};

    static JsonObject bp(String key, String t) {
        W w = find(key);
        NeroArchetypes.B b = new NeroArchetypes.B();
        String[] st = style(key);
        int blen = Integer.parseInt(st[3]);
        b.part("Основание");
        b.box(11, -1, 5, 19, -1, 13, "smooth_stone");
        b.box(12, 0, 6, 18, 1, 12, st[0]);
        b.part("Корпус");
        b.box(13, 2, 7, 17, 4, 11, st[1]);
        b.box(14, 5, 8, 16, 5, 10, st[1]);
        b.part("Ствол");
        b.box(15, 3, 12, 15, 3, 11 + blen, st[2]);
        b.box(14, 2, 12, 16, 4, 12, st[1]);
        b.part("Подсветка");
        b.set(13, 3, 9, "sea_lantern");
        b.set(17, 3, 9, "sea_lantern");
        if (key.equals("w_mines")) { b.part("Минное поле"); for (int[] m : MINES) b.set(m[0], 0, m[1], "stone_pressure_plate"); }
        if (key.equals("w_shield")) { b.part("Излучатели"); for (int[] p : new int[][]{{9, 3}, {21, 3}, {9, 15}, {21, 15}}) { b.box(p[0], 0, p[1], p[0], 4, p[1], "iron_block"); b.set(p[0], 5, p[1], "sea_lantern"); } }
        if (key.equals("w_orbital")) { b.part("Антенна"); b.box(15, 6, 9, 15, 14, 9, "iron_block"); b.set(15, 15, 9, "sea_lantern"); b.box(13, 12, 9, 17, 12, 9, "iron_block"); }
        if (key.equals("w_flak") || key.equals("w_shotgun")) { b.part("Второй ствол"); b.box(17, 3, 12, 17, 3, 11 + blen, st[2]); b.box(13, 3, 12, 13, 3, 11 + blen, st[2]); }
        if (key.equals("w_tesla")) { b.part("Катушка"); b.box(15, 5, 9, 15, 11, 9, "copper_block"); b.set(15, 12, 9, "lightning_rod"); }
        if (key.equals("w_mortar")) { b.part("Труба миномёта"); b.box(15, 5, 9, 15, 7, 9, "iron_block"); }

        String L = "{id}_w", T = "nw_{id}", LOCK = "nl_{id}";
        String POS = "positioned " + NeroKit.ph('x', 15) + " " + NeroKit.ph('y', 3) + " " + NeroKit.ph('z', 9) + " ";
        String S = "scoreboard players set ";
        String gate = key.equals("w_mines") || key.equals("w_shield") ? "execute if score #on " + L + " matches 1 " : w.auto()
                ? "execute if score #on " + L + " matches 1 if score #cd " + L + " matches 0 " : "execute if score #fire " + L + " matches 1 if score #cd " + L + " matches 0 ";
        String H = "execute if score #hit " + L + " matches 1 ";
        String AIM = H + POS + "facing entity @e[tag=" + T + ",sort=nearest,limit=1] feet ";
        List<String[]> s = new ArrayList<>();
        int R = w.range();
        String PC = key.equals("w_mines") ? "positioned " + NeroKit.ph('x', 26) + " " + NeroKit.ph('y', 1) + " " + NeroKit.ph('z', 20) + " " : POS;
        s.add(new String[]{"Перезарядка: отсчёт", "execute if score #cd " + L + " matches 1.. run scoreboard players remove #cd " + L + " 1", "Уменьшает время перезарядки"});
        String[][] hostile = {{"нежить", "#minecraft:undead"}, {"криперы", "minecraft:creeper"}, {"пауки", "minecraft:spider"}, {"пещерные пауки", "minecraft:cave_spider"}, {"ведьмы", "minecraft:witch"},
                {"налётчики", "#minecraft:raiders"}, {"слизни", "minecraft:slime"}, {"фантомы", "minecraft:phantom"}};
        for (String[] h : hostile)
            s.add(new String[]{"Поиск врагов: " + h[0], gate + PC + "run tag @e[type=" + h[1] + ",distance=.." + R + "] add " + T, "Помечает враждебных мобов типа «" + h[0] + "» в радиусе " + R});
        if (!key.equals("w_mines") && !key.equals("w_shield")) {
            boolean needTarget = w.auto() || key.equals("w_sniper");
            s.add(new String[]{"Захват цели", gate + (needTarget ? "if entity @e[tag=" + T + "] " : "") + "run " + S + "#hit " + L + " 1", needTarget ? "Выстрел произойдёт только если враг найден" : "Выстрел по кнопке"});
        }
        switch (key) {
            case "w_laser" -> {
                ray(s, AIM, "end_rod ~ ~ ~ 0 0 0 0 1", 12, 2, "Луч");
                s.add(new String[]{"Вспышка на стволе", H + POS + "positioned ~ ~ ~4 run particle minecraft:flash ~ ~ ~ 0 0 0 0 1", "Вспышка выстрела"});
                s.add(new String[]{"Звук лазера", H + POS + "run playsound minecraft:entity.guardian.attack hostile @a ~ ~ ~ 1.5 1.6", "Звук выстрела"});
                s.add(new String[]{"Урон лазера", H + "run damage @e[tag=" + T + ",sort=nearest,limit=1] 8 minecraft:magic", "Урон ближайшему врагу"});
            }
            case "w_flak" -> {
                ray(s, AIM, "smoke ~ ~ ~ 0.1 0.1 0.1 0 4", 8, 3, "Трассер");
                s.add(new String[]{"Разрыв снаряда", H + "as @e[tag=" + T + ",sort=nearest,limit=2] at @s run particle minecraft:explosion ~ ~1 ~ 0 0 0 0 1", "Разрыв у цели"});
                s.add(new String[]{"Звук зенитки", H + POS + "run playsound minecraft:entity.firework_rocket.blast hostile @a ~ ~ ~ 2 0.8", "Звук выстрела"});
                s.add(new String[]{"Урон зенитки", H + "run damage @e[tag=" + T + ",sort=nearest,limit=2] 10 minecraft:magic", "Урон двум ближайшим"});
            }
            case "w_tesla" -> {
                s.add(new String[]{"Разряд вокруг катушки", H + POS + "run particle minecraft:electric_spark ~ ~3 ~ 2 1.5 2 0.5 60", "Искры вокруг катушки"});
                s.add(new String[]{"Дуги до целей", H + "as @e[tag=" + T + ",sort=nearest,limit=3] at @s run particle minecraft:electric_spark ~ ~1 ~ 0.3 0.6 0.3 0.3 30", "Искры на целях"});
                s.add(new String[]{"Гром", H + POS + "run playsound minecraft:entity.lightning_bolt.thunder hostile @a ~ ~ ~ 1 1.8", "Звук разряда"});
                s.add(new String[]{"Урон молнии", H + "run damage @e[tag=" + T + ",sort=nearest,limit=3] 7 minecraft:lightning_bolt", "Урон до трёх врагов"});
                s.add(new String[]{"Замедление после удара", H + "run effect give @e[tag=" + T + ",sort=nearest,limit=3] minecraft:slowness 2 2", "Оглушение на 2 секунды"});
            }
            case "w_flame" -> {
                ray(s, AIM, "flame ~ ~ ~ 0.3 0.3 0.3 0.02 8", 8, 1, "Язык пламени");
                s.add(new String[]{"Звук огня", H + POS + "run playsound minecraft:item.firecharge.use hostile @a ~ ~ ~ 1 1", "Звук"});
                s.add(new String[]{"Поджог", H + "as @e[tag=" + T + "] run data modify entity @s Fire set value 100s", "Поджигает врагов"});
                s.add(new String[]{"Урон огнём", H + "run damage @e[tag=" + T + "] 4 minecraft:on_fire", "Урон огнём"});
            }
            case "w_ice" -> {
                ray(s, AIM, "snowflake ~ ~ ~ 0.2 0.2 0.2 0.02 6", 10, 2, "Ледяной поток");
                s.add(new String[]{"Замедление", H + "run effect give @e[tag=" + T + "] minecraft:slowness 5 4", "Сильное замедление"});
                s.add(new String[]{"Слабость", H + "run effect give @e[tag=" + T + "] minecraft:weakness 5 1", "Слабость врагов"});
                s.add(new String[]{"Заморозка", H + "as @e[tag=" + T + ",sort=nearest,limit=3] run data modify entity @s TicksFrozen set value 300", "Замораживает до трёх врагов"});
                s.add(new String[]{"Урон холодом", H + "run damage @e[tag=" + T + "] 3 minecraft:freeze", "Урон холодом"});
            }
            case "w_plasma" -> {
                ray(s, AIM, "soul_fire_flame ~ ~ ~ 0.1 0.1 0.1 0 3", 10, 2, "Сгусток");
                s.add(new String[]{"Взрыв плазмы", H + "as @e[tag=" + T + ",sort=nearest,limit=1] at @s run particle minecraft:explosion ~ ~1 ~ 0 0 0 0 1", "Вспышка на цели"});
                s.add(new String[]{"Звук плазмы", H + POS + "run playsound minecraft:entity.blaze.shoot hostile @a ~ ~ ~ 1.5 0.6", "Звук"});
                s.add(new String[]{"Урон плазмы", H + "run damage @e[tag=" + T + ",sort=nearest,limit=1] 12 minecraft:magic", "Урон цели"});
            }
            case "w_gravity" -> {
                s.add(new String[]{"Притяжение", H + "as @e[tag=" + T + "] at @s facing " + NeroKit.ph('x', 15) + " " + NeroKit.ph('y', 3) + " " + NeroKit.ph('z', 9) + " run tp @s ^ ^ ^0.7", "Тянет врагов к пушке"});
                s.add(new String[]{"Левитация", H + "run effect give @e[tag=" + T + "] minecraft:levitation 1 0", "Поднимает врагов"});
                s.add(new String[]{"Воронка частиц", H + "as @e[tag=" + T + "] at @s run particle minecraft:reverse_portal ~ ~1 ~ 0.2 0.5 0.2 0.05 12", "Частицы на целях"});
                s.add(new String[]{"Урон притяжением", H + "run damage @e[tag=" + T + "] 1 minecraft:magic", "Лёгкий урон"});
                s.add(new String[]{"Звук", H + POS + "run playsound minecraft:block.beacon.ambient hostile @a ~ ~ ~ 1 0.5", "Гул"});
            }
            case "w_shield" -> {
                int[][] ring = {{5, 0}, {4, 4}, {0, 5}, {-4, 4}, {-5, 0}, {-4, -4}, {0, -5}, {4, -4}};
                for (int i = 0; i < ring.length; i++)
                    s.add(new String[]{"Купол: точка " + (i + 1), "execute if score #on " + L + " matches 1 " + POS + "positioned ~" + ring[i][0] + " ~2 ~" + ring[i][1] + " run particle minecraft:end_rod ~ ~ ~ 0 1.2 0 0 2", "Светящаяся граница щита"});
                s.add(new String[]{"Отталкивание", gate + "as @e[tag=" + T + "] at @s facing " + NeroKit.ph('x', 15) + " " + NeroKit.ph('y', 3) + " " + NeroKit.ph('z', 9) + " run tp @s ^ ^ ^-0.9", "Выталкивает врагов из зоны"});
                s.add(new String[]{"Урон щита", gate + "run damage @e[tag=" + T + "] 1 minecraft:thorns", "Колючий урон"});
            }
            case "w_mines" -> {
                for (int i = 0; i < MINES.length; i++) {
                    String at = "execute if score #on " + L + " matches 1 positioned " + NeroKit.ph('x', MINES[i][0]) + " " + NeroKit.ph('y', 0) + " " + NeroKit.ph('z', MINES[i][1]) + " if entity @e[tag=" + T + ",distance=..1.8] run ";
                    s.add(new String[]{"Мина " + (i + 1) + ": урон", at + "damage @e[tag=" + T + ",distance=..3.5] 14 minecraft:explosion", "Урон врагам у мины"});
                    s.add(new String[]{"Мина " + (i + 1) + ": взрыв", at + "particle minecraft:explosion_emitter ~ ~1 ~ 0 0 0 0 1", "Эффект взрыва"});
                    s.add(new String[]{"Мина " + (i + 1) + ": звук", at + "playsound minecraft:entity.generic.explode hostile @a ~ ~ ~ 2 1", "Звук взрыва"});
                    s.add(new String[]{"Мина " + (i + 1) + ": замедление", at + "effect give @e[tag=" + T + ",distance=..3.5] minecraft:slowness 2 3", "Оглушает уцелевших"});
                }
            }
            case "w_rail" -> {
                s.add(new String[]{"Заряд: свечение", H + POS + "positioned ~ ~ ~9 run particle minecraft:end_rod ~ ~ ~ 0.3 0.3 0.3 0.05 30", "Свечение при заряде"});
                for (int n = 1; n <= 24; n++) {
                    s.add(new String[]{"Рельсовый луч: сегмент " + n, H + POS + "positioned ~ ~ ~" + (n + 8) + " run particle minecraft:end_rod ~ ~ ~ 0 0 0 0 1", "Видимая линия выстрела"});
                    s.add(new String[]{"Пробитие: сегмент " + n, H + POS + "positioned ~ ~ ~" + (n + 8) + " run damage @e[tag=" + T + ",distance=..1.6] 20 minecraft:magic", "Урон всем на линии"});
                }
                s.add(new String[]{"Звук рельсотрона", H + POS + "run playsound minecraft:entity.lightning_bolt.impact hostile @a ~ ~ ~ 2 2", "Громкий выстрел"});
            }
            case "w_grenade" -> {
                for (int n = 1; n <= 14; n++) s.add(new String[]{"Полёт гранаты: точка " + n, H + POS + "positioned ~ ~ ~" + (n + 4) + " run particle minecraft:smoke ~ ~ ~ 0 0 0 0 3", "След гранаты"});
                s.add(new String[]{"Взрыв гранаты", H + POS + "positioned ~ ~ ~18 run particle minecraft:explosion_emitter ~ ~ ~ 0 0 0 0 1", "Эффект взрыва"});
                s.add(new String[]{"Урон гранаты", H + POS + "positioned ~ ~ ~18 run damage @e[tag=" + T + ",distance=..4] 12 minecraft:explosion", "Урон по площади"});
                s.add(new String[]{"Звук гранаты", H + POS + "positioned ~ ~ ~18 run playsound minecraft:entity.generic.explode hostile @a ~ ~ ~ 2 1", "Звук взрыва"});
            }
            case "w_shotgun" -> {
                for (int i = -2; i <= 2; i++) {
                    for (int n = 1; n <= 3; n++) s.add(new String[]{"Дробь " + (i + 3) + ": точка " + n, H + POS + "positioned ~" + i + " ~ ~" + (n * 3 + 2) + " run particle minecraft:crit ~ ~ ~ 0 0 0 0 2", "След заряда"});
                    s.add(new String[]{"Дробь " + (i + 3) + ": попадание", H + POS + "positioned ~" + i + " ~ ~11 run damage @e[tag=" + T + ",distance=..2,limit=1] 6 minecraft:magic", "Урон одному врагу"});
                }
                s.add(new String[]{"Звук дробовика", H + POS + "run playsound minecraft:entity.generic.explode hostile @a ~ ~ ~ 1 1.5", "Звук"});
            }
            case "w_sniper" -> {
                for (int n = 1; n <= 16; n++) s.add(new String[]{"Трассер: точка " + n, H + POS + "positioned ~ ~ ~" + (n * 2 + 8) + " run particle minecraft:crit ~ ~ ~ 0 0 0 0 1", "След пули"});
                for (int n = 1; n <= 16; n++) s.add(new String[]{"Попадание: точка " + n, H + POS + "positioned ~ ~ ~" + (n * 2 + 8) + " run damage @e[tag=" + T + ",distance=..1.4,limit=1] 30 minecraft:arrow", "Сильный урон"});
                s.add(new String[]{"Звук выстрела", H + POS + "run playsound minecraft:entity.arrow.hit_player hostile @a ~ ~ ~ 2 0.5", "Звук"});
            }
            case "w_mortar" -> {
                s.add(new String[]{"Захват цели миномёта", H + "run tag @e[tag=" + T + ",sort=nearest,limit=1] add " + LOCK, "Выбирает ближайшего врага"});
                s.add(new String[]{"Время полёта", H + "run " + S + "#del " + L + " 20", "Мина летит секунду"});
                for (int n = 1; n <= 6; n++) s.add(new String[]{"Дуга мины: точка " + n, H + POS + "positioned ~ ~" + (n * 3) + " ~ run particle minecraft:smoke ~ ~ ~ 0 0 0 0 4", "След мины вверх"});
                s.add(new String[]{"Звук выстрела", H + POS + "run playsound minecraft:entity.firework_rocket.launch hostile @a ~ ~ ~ 2 0.6", "Звук"});
                s.add(new String[]{"Удар: взрыв", "execute if score #del " + L + " matches 1 as @e[tag=" + LOCK + "] at @s run particle minecraft:explosion_emitter ~ ~1 ~ 0 0 0 0 1", "Взрыв на цели"});
                s.add(new String[]{"Удар: урон", "execute if score #del " + L + " matches 1 run damage @e[tag=" + LOCK + "] 15 minecraft:explosion", "Урон выбранной цели"});
                s.add(new String[]{"Удар: звук", "execute if score #del " + L + " matches 1 as @e[tag=" + LOCK + "] at @s run playsound minecraft:entity.generic.explode hostile @a ~ ~ ~ 2 1", "Звук взрыва"});
                s.add(new String[]{"Снять захват", "execute if score #del " + L + " matches 1 run tag @e[tag=" + LOCK + "] remove " + LOCK, "Очищает метку цели"});
                s.add(new String[]{"Отсчёт полёта", "execute if score #del " + L + " matches 1.. run scoreboard players remove #del " + L + " 1", "Время полёта идёт"});
            }
            case "w_orbital" -> {
                s.add(new String[]{"Захват цели с орбиты", H + "run tag @e[tag=" + T + ",sort=nearest,limit=1] add " + LOCK, "Выбирает ближайшего врага"});
                s.add(new String[]{"Зарядка луча", H + "run " + S + "#del " + L + " 60", "Заряд идёт 3 секунды"});
                s.add(new String[]{"Сообщение о заряде", H + "run tellraw @a {\"text\":\"Орбитальная пушка: цель захвачена, заряд 3 секунды\",\"color\":\"red\"}", "Предупреждение"});
                for (int n = 0; n < 12; n++) s.add(new String[]{"Столб света: уровень " + (n + 1), "execute if score #del " + L + " matches 1.. as @e[tag=" + LOCK + "] at @s run particle minecraft:end_rod ~ ~" + (n * 2) + " ~ 0 0 0 0 1", "Луч над целью во время заряда"});
                s.add(new String[]{"Удар: вспышка", "execute if score #del " + L + " matches 1 as @e[tag=" + LOCK + "] at @s run particle minecraft:explosion_emitter ~ ~1 ~ 1 1 1 0 3", "Вспышка удара"});
                s.add(new String[]{"Удар: урон", "execute if score #del " + L + " matches 1 run damage @e[tag=" + LOCK + "] 30 minecraft:magic", "Сильный урон цели"});
                s.add(new String[]{"Удар: гром", "execute if score #del " + L + " matches 1 as @e[tag=" + LOCK + "] at @s run playsound minecraft:entity.lightning_bolt.thunder hostile @a ~ ~ ~ 3 0.7", "Гром"});
                s.add(new String[]{"Снять захват", "execute if score #del " + L + " matches 1 run tag @e[tag=" + LOCK + "] remove " + LOCK, "Очищает метку цели"});
                s.add(new String[]{"Отсчёт заряда", "execute if score #del " + L + " matches 1.. run scoreboard players remove #del " + L + " 1", "Заряд идёт"});
            }
            default -> { }
        }
        if (!key.equals("w_mines") && !key.equals("w_shield")) {
            s.add(new String[]{"Перезарядка: старт", H + "run " + S + "#cd " + L + " " + w.cd(), "Запускает перезарядку после выстрела"});
            s.add(new String[]{"Сброс захвата", S + "#hit " + L + " 0", "Готовит следующий выстрел"});
            s.add(new String[]{"Сброс кнопки выстрела", S + "#fire " + L + " 0", "Кнопка ВЫСТРЕЛ срабатывает один раз"});
        }
        s.add(new String[]{"Снять метки врагов", "tag @e[tag=" + T + "] remove " + T, "Убирает временные метки"});
        b.chain(w.name() + ": боевая логика", s.toArray(new String[0][]));

        NeroKit.console(b, 3);
        b.setup("scoreboard objectives add " + L + " dummy", S + "#on " + L + " 0", S + "#fire " + L + " 0", S + "#cd " + L + " 0", S + "#hit " + L + " 0", S + "#del " + L + " 0");
        String test = "execute " + POS + "run particle minecraft:end_rod ~ ~2 ~ 1 1 1 0.2 40";
        b.chain(w.name() + ": пульт", new String[][]{
                {"Кнопка ВКЛ", NeroKit.poll(0) + S + "#on " + L + " 1", "Включает установку"},
                {"Кнопка ВЫКЛ", NeroKit.poll(1) + S + "#on " + L + " 0", "Выключает установку"},
                {w.auto() ? "Кнопка ТЕСТ" : "Кнопка ВЫСТРЕЛ", w.auto() ? NeroKit.poll(2) + test : NeroKit.poll(2) + S + "#fire " + L + " 1", w.auto() ? "Проверка: частицы у установки" : "Один выстрел вперёд"},
                {"Лампа: включено", "execute if score #on " + L + " matches 1 run setblock {x+7} {y+2} {z+0} minecraft:lime_concrete", "Зелёная лампа, когда установка включена"},
                {"Лампа: выключено", "execute unless score #on " + L + " matches 1 run setblock {x+7} {y+2} {z+0} minecraft:red_concrete", "Красная лампа, когда выключена"}});
        b.setup(NeroKit.sign(1, 0, -1, "ВКЛ", "включает", w.auto() ? "авто-поиск" : "установку", w.auto() ? "врагов" : ""));
        b.setup(NeroKit.sign(3, 0, -1, "ВЫКЛ", "выключает", "установку", ""));
        b.setup(NeroKit.sign(5, 0, -1, w.auto() ? "ТЕСТ" : "ВЫСТРЕЛ", w.auto() ? "частицы" : "один выстрел", w.auto() ? "без урона" : "вперёд", ""));
        NeroKit.board(b, 4, 6, 0, w.name().toUpperCase(), w.how(), "Радиус: " + R + " блоков", "Бьёт только врагов-мобов,", "игроков не трогает.", "ВКЛ - включить, ВЫКЛ - выключить");
        b.help = w.name() + " (вымышленное игровое устройство): " + w.how() + ". Радиус " + R + " блоков, бьёт только враждебных мобов. Пульт слева, таблички под кнопками. Командных блоков в логике: " + s.size() + ".";
        return b.done();
    }

    private static void ray(List<String[]> s, String AIM, String particle, int steps, int sp, String label) {
        for (int n = 1; n <= steps; n++) s.add(new String[]{label + ": сегмент " + n, AIM + "positioned ^ ^ ^" + (n * sp) + " run particle minecraft:" + particle, "Видимая линия выстрела"});
    }

    private NeroWeapons() { }
}
