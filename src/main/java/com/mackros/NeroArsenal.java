package com.mackros;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Арсенал: 27 игровых оружий-удилищ. Предмет (морковь на палке с именем) работает как удочка: ПКМ в воздух.
 * «Мозг» оружия целиком из командных блоков: считает перезарядку, замечает выстрел по счётчику minecraft.used,
 * проверяет, что в руке именно это оружие, метит враждебных мобов, бьёт лучом, конусом или по площади, играет звуки и частицы.
 * Бьёт только враждебных мобов (нежить, криперы, пауки, ведьмы, налётчики, слизни, фантомы, блейзы...), игроков и животных не трогает.
 * Пульт маленький: ВЫДАТЬ оружие, ВЫКЛ и ВКЛ.
 */
final class NeroArsenal {
    record WR(String key, String name, String re, String how, int cd, int range, boolean foes) { }

    static final WR[] ALL = {
            new WR("wr_laser", "Лазерное удилище", "лазерн\\w*\\s+удил", "быстрый луч, бьёт первого врага (8 урона)", 8, 30, true),
            new WR("wr_rail", "Рельсовое удилище", "рельсов\\w*\\s+удил", "пробивает всех врагов на линии (18 урона)", 45, 42, true),
            new WR("wr_flame", "Огненный хлыст", "огненн\\w*\\s+хлыст|хлыст", "конус пламени, поджигает врагов", 8, 10, true),
            new WR("wr_ice", "Ледяной посох", "ледян\\w*\\s+посох", "замораживает и замедляет первого врага", 18, 26, true),
            new WR("wr_tesla", "Громовой жезл", "громов\\w*\\s+жезл", "молния по цели и ещё двум врагам рядом", 25, 30, true),
            new WR("wr_grenade", "Гранатометное удилище", "гранатомет\\w*\\s+удил", "граната в точку впереди, взрыв по площади без разрушений", 35, 20, true),
            new WR("wr_shotgun", "Дробовое удилище", "дробов\\w*\\s+удил", "веер дроби вблизи, отбрасывает врагов", 25, 12, true),
            new WR("wr_mortar", "Миномётное удилище", "миномет\\w*\\s+удил", "захват ближайшего врага, удар с неба через секунду", 60, 32, true),
            new WR("wr_gravity", "Гравитационное удилище", "гравитац\\w*\\s+удил", "притягивает врагов и приподнимает над землёй", 12, 10, true),
            new WR("wr_plasma", "Плазменное удилище", "плазм\\w*\\s+удил|плазменн\\w*\\s+удил", "плазменный сгусток (12 урона) со вспышкой", 14, 28, true),
            new WR("wr_sniper", "Снайперское удилище", "снайперск\\w*\\s+удил", "один сильный выстрел на 70 блоков (28 урона)", 40, 72, true),
            new WR("wr_vampire", "Вампирский коготь", "вампирск\\w*\\s+когот|вампирск\\w*\\s+когт", "удар вблизи, лечит тебя", 10, 6, true),
            new WR("wr_blink", "Жезл телепортации", "жезл\\w*\\s+телепорт", "прыжок на 8 блоков вперёд", 40, 1, false),
            new WR("wr_meteor", "Метеоритный жезл", "метеор\\w*\\s+жезл|жезл\\w*\\s+метеор", "через 1,5 секунды падает метеор в точку прицела", 90, 34, true),
            new WR("wr_toxic", "Ядовитый плевок", "ядовит\\w*\\s+плевок|плевок", "яд на 8 секунд первому врагу", 12, 20, true),
            new WR("wr_icenova", "Ледяная нова", "ледян\\w*\\s+нов", "волна холода вокруг тебя, замедляет врагов", 80, 7, true),
            new WR("wr_shock", "Ударная волна", "ударн\\w*\\s+волн", "отбрасывает врагов вокруг и бьёт взрывом", 60, 9, true),
            new WR("wr_firenova", "Огненное кольцо", "огненн\\w*\\s+кольц", "кольцо огня вокруг тебя поджигает врагов", 70, 6, true),
            new WR("wr_heal", "Целительный жезл", "целительн\\w*\\s+жезл", "регенерация и поглощение для тебя", 100, 1, false),
            new WR("wr_haste", "Боевой стимулятор", "боев\\w*\\s+стимулятор|стимулятор", "скорость, сила и защита на 12 секунд", 400, 1, false),
            new WR("wr_smoke", "Дымовая завеса", "дымов\\w*\\s+завес", "облако дыма: враги слепнут и замедляются", 80, 12, true),
            new WR("wr_harpoon", "Гарпун", "гарпун", "цепляет врага и подтягивает к тебе", 25, 22, true),
            new WR("wr_blade", "Энергетический клинок", "клинок", "широкий взмах перед собой с отбрасыванием", 8, 6, true),
            new WR("wr_sonic", "Звуковая пушка", "звуков\\w*\\s+пушк", "звуковой удар пробивает всех на линии (10 урона)", 35, 26, true),
            new WR("wr_void", "Жезл пустоты", "жезл\\w*\\s+пустот", "иссушение: яд тьмы и слепота первому врагу", 20, 26, true),
            new WR("wr_orbital", "Орбитальный удар", "орбитальн\\w*\\s+удар", "захват врага, через 2 секунды столб света и 28 урона", 160, 42, true),
            new WR("wr_web", "Паутинная пушка", "паутинн\\w*\\s+пушк", "опутывает первого врага паутиной", 40, 22, true)};

    private static final String[][] FOES = {{"нежить", "#minecraft:undead"}, {"криперы", "minecraft:creeper"}, {"пауки", "minecraft:spider"}, {"пещерные пауки", "minecraft:cave_spider"},
            {"ведьмы", "minecraft:witch"}, {"налётчики", "#minecraft:raiders"}, {"слизни", "minecraft:slime"}, {"магмовые кубы", "minecraft:magma_cube"},
            {"фантомы", "minecraft:phantom"}, {"блейзы", "minecraft:blaze"}, {"чешуйницы", "minecraft:silverfish"}};
    private static final String[] COLORS = {"aqua", "gold", "red", "light_purple", "green", "yellow", "blue", "white", "dark_aqua"};
    private static final String[][] STYLE = {{"gray_concrete", "cyan_concrete"}, {"black_concrete", "light_blue_concrete"}, {"red_terracotta", "orange_concrete"}, {"packed_ice", "blue_ice"},
            {"iron_block", "sea_lantern"}, {"green_concrete", "lime_concrete"}, {"brown_concrete", "gold_block"}, {"purple_concrete", "amethyst_block"}, {"white_concrete", "quartz_block"}};

    static WR find(String k) { for (WR w : ALL) if (w.key().equals(k)) return w; return ALL[0]; }

    static String detect(String t) { for (WR w : ALL) if (NeroFree.has(t, w.re())) return w.key(); return null; }

    static String name(String k) { return find(k).name(); }

    static String works(String k) {
        WR w = find(k);
        return "вымышленное игровое оружие: предмет-удилище (ПКМ в воздух), " + w.how() + ". Мозг из командных блоков, бьёт только враждебных мобов, игроков и животных не трогает. Пульт: ВЫДАТЬ, ВЫКЛ, ВКЛ";
    }

    private static final String F = "@a[tag=mkf]", AS = "execute as " + F + " at @s ", EYE = AS + "anchored eyes ";

    private static String itemNbt(WR w, int idx) {
        return "minecraft:carrot_on_a_stick{display:{Name:'{\"text\":\"" + w.name() + "\",\"color\":\"" + COLORS[idx % COLORS.length] + "\",\"italic\":false}',Lore:['{\"text\":\"ПКМ: "
                + NeroKit.esc(w.how()) + "\",\"color\":\"gray\",\"italic\":false}']},mkw:\"" + w.key() + "\",Unbreakable:1b,HideFlags:4,CustomModelData:" + (7100 + idx) + "}";
    }

    private static void ray(List<String[]> s, String label, String particle, int steps, int sp) {
        for (int n = 1; n <= steps; n++) s.add(new String[]{label + ": точка " + n, EYE + "positioned ^ ^ ^" + (n * sp) + " run particle minecraft:" + particle, "Видимая линия выстрела"});
    }

    private static void scanFirst(List<String[]> s, int steps, int sp, String rad) {
        for (int n = 1; n <= steps; n++)
            s.add(new String[]{"Прицел: точка " + n, EYE + "positioned ^ ^ ^" + (n * sp) + " unless entity @e[tag=mkhit] run tag @e[tag=mkfoe,distance=.." + rad + ",limit=1,sort=nearest] add mkhit", "Находит первого врага на линии"});
    }

    private static void scanAll(List<String[]> s, int steps, int sp, String rad, String label) {
        for (int n = 1; n <= steps; n++)
            s.add(new String[]{label + ": зона " + n, EYE + "positioned ^ ^ ^" + (n * sp) + " run tag @e[tag=mkfoe,distance=.." + rad + "] add mkhit", "Помечает всех врагов в этой зоне"});
    }

    private static void say(List<String[]> s, String label, String snd, String vol, String pitch) {
        s.add(new String[]{label, AS + "run playsound minecraft:" + snd + " player @a ~ ~ ~ " + vol + " " + pitch, "Звук"});
    }

    private static void fx(List<String[]> s, String label, String cmd, String role) { s.add(new String[]{label, cmd, role}); }

    private static String atHit(String particle) { return "execute as @e[tag=mkhit] at @s run particle minecraft:" + particle; }

    private static String push(String back) { return "execute as @e[tag=mkhit] at @s facing entity @a[tag=mkf,limit=1] feet run tp @s " + back; }

    static JsonObject bp(String key, String t) {
        WR w = find(key);
        int idx = 0;
        for (int i = 0; i < ALL.length; i++) if (ALL[i].key().equals(key)) idx = i;
        NeroArchetypes.B b = new NeroArchetypes.B();
        String[] st = STYLE[idx % STYLE.length];
        String U = "{id}_u", C = "{id}_c", D = "{id}_d", S = "scoreboard players set ";
        b.part("Основание");
        b.box(0, -1, -2, 10, -1, 6, "smooth_stone");
        b.part("Пульт управления");
        b.box(0, 0, 0, 4, 2, 0, "black_concrete");
        for (int i = 0; i < 3; i++) b.set(1 + i, 1, -1, "stone_button[face=wall,facing=north]");
        b.set(4, 1, 0, "red_concrete");
        b.part("Стенд оружия");
        b.box(6, 0, 1, 8, 0, 3, st[0]);
        b.box(7, 1, 2, 7, 1, 2, st[1]);
        b.set(6, 0, 1, "sea_lantern");
        b.set(8, 0, 3, "sea_lantern");

        List<String[]> s = new ArrayList<>();
        s.add(new String[]{"Перезарядка: отсчёт", "scoreboard players remove @a[scores={" + C + "=1..}] " + C + " 1", "Уменьшает время перезарядки"});
        s.add(new String[]{"Выстрел: кто стрелял", "execute if score #on " + D + " matches 1 run tag @a[scores={" + U + "=1..," + C + "=0},nbt={SelectedItem:{tag:{mkw:\"" + key + "\"}}}] add mkf",
                "Метит игрока, у которого в руке это оружие и который нажал ПКМ"});
        s.add(new String[]{"Перезарядка: старт", S + "@a[tag=mkf] " + C + " " + w.cd(), "Запускает перезарядку"});
        s.add(new String[]{"Сброс счётчика ПКМ", S + "@a[scores={" + U + "=1..}] " + U + " 0", "Готовит следующий выстрел"});
        if (w.foes())
            for (String[] f : FOES)
                s.add(new String[]{"Поиск врагов: " + f[0], AS + "run tag @e[type=" + f[1] + ",distance=.." + w.range() + "] add mkfoe", "Помечает враждебных мобов типа «" + f[0] + "» в радиусе " + w.range()});

        String H = "@e[tag=mkhit]";
        switch (key) {
            case "wr_laser" -> {
                scanFirst(s, 14, 2, "1.8");
                ray(s, "Луч", "end_rod ~ ~ ~ 0 0 0 0 1", 14, 2);
                say(s, "Звук лазера", "entity.guardian.attack", "1.5", "1.7");
                fx(s, "Урон лазера", "damage " + H + " 8 minecraft:magic", "Урон первому врагу");
                fx(s, "Вспышка на цели", atHit("flash ~ ~1 ~ 0 0 0 0 1"), "Эффект попадания");
            }
            case "wr_rail" -> {
                scanAll(s, 21, 2, "1.7", "Линия");
                ray(s, "Рельсовый луч", "end_rod ~ ~ ~ 0 0 0 0 1", 21, 2);
                say(s, "Звук рельсотрона", "entity.lightning_bolt.impact", "2", "2");
                fx(s, "Урон по линии", "damage " + H + " 18 minecraft:magic", "Урон всем на линии");
                fx(s, "Вспышки на целях", atHit("electric_spark ~ ~1 ~ 0.3 0.5 0.3 0.3 20"), "Искры");
            }
            case "wr_flame" -> {
                scanAll(s, 3, 3, "2.7", "Конус");
                ray(s, "Пламя", "flame ~ ~ ~ 0.3 0.3 0.3 0.03 8", 6, 2);
                say(s, "Звук огня", "item.firecharge.use", "1", "1");
                fx(s, "Поджог", "execute as " + H + " run data modify entity @s Fire set value 100s", "Поджигает врагов");
                fx(s, "Урон огнём", "damage " + H + " 3 minecraft:on_fire", "Урон огнём");
            }
            case "wr_ice" -> {
                scanFirst(s, 13, 2, "1.8");
                ray(s, "Ледяной поток", "snowflake ~ ~ ~ 0.2 0.2 0.2 0.02 5", 13, 2);
                say(s, "Звук льда", "entity.snow_golem.shoot", "1.3", "0.8");
                fx(s, "Замедление", "effect give " + H + " minecraft:slowness 5 3 true", "Сильное замедление");
                fx(s, "Слабость", "effect give " + H + " minecraft:weakness 5 1 true", "Слабость врага");
                fx(s, "Заморозка", "execute as " + H + " run data modify entity @s TicksFrozen set value 300", "Ледяной эффект");
                fx(s, "Урон холодом", "damage " + H + " 3 minecraft:freeze", "Урон холодом");
            }
            case "wr_tesla" -> {
                scanFirst(s, 15, 2, "1.8");
                fx(s, "Цепь молнии: поиск", "execute as " + H + " at @s run tag @e[tag=mkfoe,tag=!mkhit,distance=..8,limit=2,sort=nearest] add mkchain", "Ещё два врага рядом с целью");
                ray(s, "Дуга", "electric_spark ~ ~ ~ 0.1 0.1 0.1 0.2 3", 15, 2);
                fx(s, "Искры на целях", "execute as @e[tag=mkhit] at @s run particle minecraft:electric_spark ~ ~1 ~ 0.3 0.6 0.3 0.4 30", "Искры");
                fx(s, "Искры на цепи", "execute as @e[tag=mkchain] at @s run particle minecraft:electric_spark ~ ~1 ~ 0.3 0.6 0.3 0.4 30", "Искры");
                say(s, "Гром", "entity.lightning_bolt.thunder", "1", "1.8");
                fx(s, "Урон цели", "damage " + H + " 8 minecraft:lightning_bolt", "Сильный разряд");
                fx(s, "Урон цепи", "damage @e[tag=mkchain] 5 minecraft:lightning_bolt", "Слабее по цепи");
                fx(s, "Оглушение", "effect give @e[tag=mkhit] minecraft:slowness 2 2 true", "Оглушает цель");
            }
            case "wr_grenade" -> {
                ray(s, "След гранаты", "smoke ~ ~ ~ 0 0 0 0 3", 7, 2);
                fx(s, "Зона взрыва", EYE + "positioned ^ ^ ^16 run tag @e[tag=mkfoe,distance=..4.5] add mkhit", "Враги вокруг точки взрыва");
                fx(s, "Взрыв: эффект", EYE + "positioned ^ ^ ^16 run particle minecraft:explosion_emitter ~ ~ ~ 0 0 0 0 1", "Видимый взрыв без разрушений");
                fx(s, "Взрыв: звук", EYE + "positioned ^ ^ ^16 run playsound minecraft:entity.generic.explode player @a ~ ~ ~ 3 1", "Звук взрыва");
                fx(s, "Урон по площади", "damage " + H + " 12 minecraft:explosion", "Урон взрывом");
                say(s, "Звук выстрела", "entity.firework_rocket.launch", "1.5", "0.7");
            }
            case "wr_shotgun" -> {
                fx(s, "Зона дроби: близко", EYE + "positioned ^ ^ ^4 run tag @e[tag=mkfoe,distance=..3.4] add mkhit", "Ближняя зона");
                fx(s, "Зона дроби: далеко", EYE + "positioned ^ ^ ^8 run tag @e[tag=mkfoe,distance=..4.2] add mkhit", "Дальняя зона");
                for (int n = 2; n <= 10; n += 2) s.add(new String[]{"Дробь: след " + n, EYE + "positioned ^ ^ ^" + n + " run particle minecraft:crit ~ ~ ~ 0.8 0.6 0.8 0.1 6", "Разлёт дроби"});
                say(s, "Звук выстрела", "entity.generic.explode", "1", "1.6");
                fx(s, "Урон дробью", "damage " + H + " 9 minecraft:magic", "Урон");
                fx(s, "Отбрасывание", push("^ ^0.3 ^-1.3"), "Отталкивает врагов от тебя");
            }
            case "wr_mortar" -> {
                fx(s, "Захват цели", AS + "run tag @e[tag=mkfoe,limit=1,sort=nearest] add mklock", "Выбирает ближайшего врага");
                fx(s, "Время полёта", "execute if entity " + F + " if entity @e[tag=mklock] run " + S + "#d " + D + " 20", "Мина летит секунду");
                say(s, "Звук выстрела", "entity.firework_rocket.launch", "1.5", "0.6");
            }
            case "wr_gravity" -> {
                fx(s, "Зона притяжения", EYE + "positioned ^ ^ ^5 run tag @e[tag=mkfoe,distance=..5] add mkhit", "Враги перед тобой");
                fx(s, "Притяжение", push("^ ^0.5 ^1.7"), "Тянет врагов к тебе");
                fx(s, "Подъём", "effect give " + H + " minecraft:levitation 1 0 true", "Приподнимает врагов");
                fx(s, "Воронка", atHit("reverse_portal ~ ~1 ~ 0.2 0.5 0.2 0.05 15"), "Частицы");
                fx(s, "Урон", "damage " + H + " 1 minecraft:magic", "Лёгкий урон");
                say(s, "Гул", "block.beacon.ambient", "1.2", "0.6");
            }
            case "wr_plasma" -> {
                scanFirst(s, 14, 2, "1.8");
                ray(s, "Сгусток", "soul_fire_flame ~ ~ ~ 0.1 0.1 0.1 0 3", 14, 2);
                say(s, "Звук плазмы", "entity.blaze.shoot", "1.5", "0.6");
                fx(s, "Взрыв плазмы", atHit("explosion ~ ~1 ~ 0 0 0 0 1"), "Вспышка на цели");
                fx(s, "Урон плазмой", "damage " + H + " 12 minecraft:magic", "Урон цели");
                fx(s, "Отбрасывание", push("^ ^0.3 ^-1"), "Лёгкий толчок");
            }
            case "wr_sniper" -> {
                scanFirst(s, 36, 2, "1.5");
                ray(s, "Трассер", "crit ~ ~ ~ 0 0 0 0 1", 18, 4);
                say(s, "Звук выстрела", "entity.arrow.hit_player", "2", "0.5");
                fx(s, "Урон снайпера", "damage " + H + " 28 minecraft:arrow", "Сильный урон");
                fx(s, "Эффект попадания", atHit("crit ~ ~1.2 ~ 0.2 0.3 0.2 0.3 25"), "Частицы");
            }
            case "wr_vampire" -> {
                scanAll(s, 2, 2, "2.3", "Удар");
                say(s, "Звук удара", "entity.player.attack.sweep", "1", "0.7");
                fx(s, "Урон", "damage " + H + " 5 minecraft:magic", "Урон вблизи");
                fx(s, "Лечение", "execute if entity " + H + " run effect give " + F + " minecraft:instant_health 1 0 true", "Лечит, если попал");
                fx(s, "Частицы крови", atHit("damage_indicator ~ ~1.2 ~ 0.2 0.3 0.2 0.1 6"), "Эффект");
                fx(s, "Сердца", "execute if entity " + H + " run execute as " + F + " at @s run particle minecraft:heart ~ ~2 ~ 0.3 0.3 0.3 0 4", "Эффект");
            }
            case "wr_blink" -> {
                fx(s, "Следы входа", AS + "run particle minecraft:portal ~ ~1 ~ 0.3 0.6 0.3 0.5 40", "Частицы там, где стоял");
                fx(s, "Прыжок", EYE + "positioned ^ ^ ^8 if block ~ ~ ~ minecraft:air if block ~ ~-1 ~ minecraft:air run tp @s ~ ~-1.6 ~", "Переносит вперёд, если место свободно");
                fx(s, "Следы выхода", AS + "run particle minecraft:reverse_portal ~ ~1 ~ 0.3 0.6 0.3 0.3 40", "Частицы на новом месте");
                say(s, "Звук телепорта", "entity.enderman.teleport", "1", "1");
            }
            case "wr_meteor" -> {
                fx(s, "Метка падения", EYE + "positioned ^ ^ ^20 run summon minecraft:marker ~ ~ ~ {Tags:[\"mkmet\"]}", "Ставит невидимую метку в точке прицела");
                fx(s, "Враги в зоне", EYE + "positioned ^ ^ ^20 run tag @e[tag=mkfoe,distance=..5] add mklock", "Кто попадёт под метеор");
                fx(s, "Время падения", "execute if entity " + F + " run " + S + "#d " + D + " 30", "Метеор падает 1,5 секунды");
                say(s, "Звук призыва", "entity.evoker.cast_spell", "1.5", "0.6");
            }
            case "wr_toxic" -> {
                scanFirst(s, 10, 2, "1.8");
                ray(s, "Плевок", "item_slime ~ ~ ~ 0.1 0.1 0.1 0 3", 10, 2);
                say(s, "Звук", "entity.slime.squish", "1.5", "0.7");
                fx(s, "Яд", "effect give " + H + " minecraft:poison 8 2 true", "Яд на 8 секунд");
                fx(s, "Слабость", "effect give " + H + " minecraft:weakness 6 0 true", "Слабость");
                fx(s, "Урон", "damage " + H + " 2 minecraft:magic", "Первый удар");
                fx(s, "Облако", atHit("happy_villager ~ ~1 ~ 0.3 0.5 0.3 0 12"), "Частицы");
            }
            case "wr_icenova" -> {
                fx(s, "Враги в радиусе", AS + "run tag @e[tag=mkfoe,distance=..7] add mkhit", "Все враги рядом");
                for (int[] p : new int[][]{{5, 0}, {-5, 0}, {0, 5}, {0, -5}, {4, 4}, {-4, 4}, {4, -4}, {-4, -4}})
                    s.add(new String[]{"Кольцо холода " + p[0] + "," + p[1], AS + "positioned ~" + p[0] + " ~0.4 ~" + p[1] + " run particle minecraft:snowflake ~ ~ ~ 0.3 0.3 0.3 0.03 12", "Видимая волна"});
                say(s, "Звук льда", "block.glass.break", "1.5", "0.6");
                fx(s, "Замедление", "effect give " + H + " minecraft:slowness 6 3 true", "Замедляет");
                fx(s, "Заморозка", "execute as " + H + " run data modify entity @s TicksFrozen set value 300", "Ледяной эффект");
                fx(s, "Урон", "damage " + H + " 2 minecraft:freeze", "Урон холодом");
            }
            case "wr_shock" -> {
                fx(s, "Враги в радиусе", AS + "run tag @e[tag=mkfoe,distance=..9] add mkhit", "Все враги рядом");
                for (int[] p : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}, {6, 0}, {-6, 0}, {0, 6}, {0, -6}})
                    s.add(new String[]{"Волна " + p[0] + "," + p[1], AS + "positioned ~" + p[0] + " ~0.4 ~" + p[1] + " run particle minecraft:explosion ~ ~ ~ 0 0 0 0 1", "Видимая волна"});
                say(s, "Звук удара", "entity.generic.explode", "2", "0.8");
                fx(s, "Отбрасывание", push("^ ^0.6 ^-3"), "Отбрасывает врагов");
                fx(s, "Урон", "damage " + H + " 6 minecraft:explosion", "Урон взрывом");
            }
            case "wr_firenova" -> {
                fx(s, "Враги в радиусе", AS + "run tag @e[tag=mkfoe,distance=..6] add mkhit", "Все враги рядом");
                for (int[] p : new int[][]{{4, 0}, {-4, 0}, {0, 4}, {0, -4}, {3, 3}, {-3, 3}, {3, -3}, {-3, -3}})
                    s.add(new String[]{"Огонь " + p[0] + "," + p[1], AS + "positioned ~" + p[0] + " ~0.3 ~" + p[1] + " run particle minecraft:flame ~ ~ ~ 0.3 0.2 0.3 0.04 14", "Видимое кольцо"});
                say(s, "Звук огня", "item.firecharge.use", "1.5", "0.8");
                fx(s, "Поджог", "execute as " + H + " run data modify entity @s Fire set value 120s", "Поджигает врагов");
                fx(s, "Урон огнём", "damage " + H + " 4 minecraft:on_fire", "Урон огнём");
            }
            case "wr_heal" -> {
                fx(s, "Регенерация", "effect give " + F + " minecraft:regeneration 6 2 true", "Быстрое лечение");
                fx(s, "Поглощение", "effect give " + F + " minecraft:absorption 10 1 true", "Дополнительные сердца");
                fx(s, "Сияние", AS + "run particle minecraft:totem_of_undying ~ ~1 ~ 0.4 0.8 0.4 0.2 30", "Частицы");
                say(s, "Звук", "entity.player.levelup", "1", "1.4");
            }
            case "wr_haste" -> {
                fx(s, "Скорость", "effect give " + F + " minecraft:speed 12 1 true", "Быстрый бег");
                fx(s, "Сила", "effect give " + F + " minecraft:strength 12 1 true", "Больше урона");
                fx(s, "Защита", "effect give " + F + " minecraft:resistance 12 0 true", "Меньше урона");
                fx(s, "Прыжок", "effect give " + F + " minecraft:jump_boost 12 1 true", "Выше прыжок");
                fx(s, "Сияние", AS + "run particle minecraft:angry_villager ~ ~2 ~ 0.4 0.3 0.4 0 6", "Частицы");
                say(s, "Звук", "block.beacon.activate", "1", "1.5");
            }
            case "wr_smoke" -> {
                fx(s, "Облако дыма", EYE + "positioned ^ ^ ^6 run particle minecraft:large_smoke ~ ~ ~ 2 1 2 0.02 150", "Густой дым");
                fx(s, "Враги в дыму", EYE + "positioned ^ ^ ^6 run tag @e[tag=mkfoe,distance=..5] add mkhit", "Кто в облаке");
                fx(s, "Слепота", "effect give " + H + " minecraft:blindness 5 0 true", "Враги слепнут");
                fx(s, "Замедление", "effect give " + H + " minecraft:slowness 4 1 true", "Замедляет");
                say(s, "Звук", "entity.firework_rocket.blast", "1", "0.6");
            }
            case "wr_harpoon" -> {
                scanFirst(s, 11, 2, "1.7");
                ray(s, "Трос", "crit ~ ~ ~ 0 0 0 0 1", 11, 2);
                fx(s, "Подтягивание", push("^ ^0.2 ^6"), "Тянет врага к тебе");
                fx(s, "Урон", "damage " + H + " 3 minecraft:generic", "Урон крюком");
                say(s, "Звук", "item.trident.thunder", "0.8", "1.5");
            }
            case "wr_blade" -> {
                scanAll(s, 2, 2, "2.4", "Взмах");
                fx(s, "След клинка", EYE + "positioned ^ ^ ^2.5 run particle minecraft:sweep_attack ~ ~ ~ 0.6 0.2 0.6 0 3", "Дуга клинка");
                say(s, "Звук взмаха", "entity.player.attack.sweep", "1.2", "1.2");
                fx(s, "Урон", "damage " + H + " 7 minecraft:magic", "Урон");
                fx(s, "Отбрасывание", push("^ ^0.2 ^-1.2"), "Отталкивает врагов");
            }
            case "wr_sonic" -> {
                scanAll(s, 13, 2, "1.8", "Линия");
                ray(s, "Звуковой удар", "sonic_boom ~ ~ ~ 0 0 0 0 1", 8, 3);
                say(s, "Звук", "entity.warden.sonic_boom", "1.5", "1");
                fx(s, "Урон", "damage " + H + " 10 minecraft:magic", "Урон по линии");
                fx(s, "Оглушение", "effect give " + H + " minecraft:slowness 2 2 true", "Оглушает");
            }
            case "wr_void" -> {
                scanFirst(s, 13, 2, "1.8");
                ray(s, "Тьма", "sculk_soul ~ ~ ~ 0.1 0.1 0.1 0.02 2", 13, 2);
                say(s, "Звук", "entity.wither.shoot", "1", "1.4");
                fx(s, "Иссушение", "effect give " + H + " minecraft:wither 5 1 true", "Урон со временем");
                fx(s, "Слепота", "effect give " + H + " minecraft:blindness 3 0 true", "Слепота");
                fx(s, "Урон", "damage " + H + " 2 minecraft:magic", "Первый удар");
                fx(s, "Тьма на цели", atHit("squid_ink ~ ~1 ~ 0.3 0.5 0.3 0.03 14"), "Частицы");
            }
            case "wr_orbital" -> {
                fx(s, "Захват цели", AS + "run tag @e[tag=mkfoe,limit=1,sort=nearest] add mklock", "Выбирает ближайшего врага");
                fx(s, "Заряд луча", "execute if entity " + F + " if entity @e[tag=mklock] run " + S + "#d " + D + " 40", "Заряд идёт 2 секунды");
                fx(s, "Сообщение", "execute if entity " + F + " if entity @e[tag=mklock] run tellraw @a {\"text\":\"Орбитальный удар: цель захвачена\",\"color\":\"red\"}", "Предупреждение");
                say(s, "Звук захвата", "block.respawn_anchor.charge", "1.5", "1");
            }
            case "wr_web" -> {
                scanFirst(s, 11, 2, "1.8");
                ray(s, "Нить", "cloud ~ ~ ~ 0.05 0.05 0.05 0 2", 11, 2);
                fx(s, "Паутина на враге", "execute as " + H + " at @s if block ~ ~ ~ minecraft:air run setblock ~ ~ ~ minecraft:cobweb", "Опутывает врага");
                fx(s, "Паутина выше", "execute as " + H + " at @s if block ~ ~1 ~ minecraft:air run setblock ~ ~1 ~ minecraft:cobweb", "Опутывает выше");
                fx(s, "Замедление", "effect give " + H + " minecraft:slowness 4 4 true", "Враг еле двигается");
                say(s, "Звук", "entity.spider.ambient", "1", "0.8");
            }
            default -> { }
        }
        s.add(new String[]{"Снять метки: стрелок", "tag @a[tag=mkf] remove mkf", "Очищает временную метку"});
        s.add(new String[]{"Снять метки: враги", "tag @e[tag=mkfoe] remove mkfoe", "Очищает временные метки врагов"});
        s.add(new String[]{"Снять метки: цели", "tag @e[tag=mkhit] remove mkhit", "Очищает метки целей"});
        if (key.equals("wr_tesla")) s.add(new String[]{"Снять метки: цепь", "tag @e[tag=mkchain] remove mkchain", "Очищает метки цепи"});
        if (key.equals("wr_mortar") || key.equals("wr_orbital") || key.equals("wr_meteor")) {
            String LK = "@e[tag=mklock]";
            if (key.equals("wr_mortar")) {
                s.add(new String[]{"Полёт мины", "execute if score #d " + D + " matches 1.. as " + LK + " at @s run particle minecraft:smoke ~ ~3 ~ 0.1 1 0.1 0 4", "Дым над целью"});
                s.add(new String[]{"Удар: взрыв", "execute if score #d " + D + " matches 1 as " + LK + " at @s run particle minecraft:explosion_emitter ~ ~1 ~ 0 0 0 0 1", "Взрыв на цели"});
                s.add(new String[]{"Удар: урон", "execute if score #d " + D + " matches 1 run damage " + LK + " 16 minecraft:explosion", "Урон выбранной цели"});
                s.add(new String[]{"Удар: звук", "execute if score #d " + D + " matches 1 as " + LK + " at @s run playsound minecraft:entity.generic.explode player @a ~ ~ ~ 2 1", "Звук взрыва"});
            } else if (key.equals("wr_orbital")) {
                for (int n = 0; n < 13; n++)
                    s.add(new String[]{"Столб света: уровень " + (n + 1), "execute if score #d " + D + " matches 1.. as " + LK + " at @s run particle minecraft:end_rod ~ ~" + (n * 3) + " ~ 0 0 0 0 1", "Луч с орбиты"});
                s.add(new String[]{"Удар: вспышка", "execute if score #d " + D + " matches 1 as " + LK + " at @s run particle minecraft:explosion_emitter ~ ~1 ~ 1 1 1 0 3", "Вспышка"});
                s.add(new String[]{"Удар: урон", "execute if score #d " + D + " matches 1 run damage " + LK + " 28 minecraft:magic", "Сильный урон цели"});
                s.add(new String[]{"Удар: гром", "execute if score #d " + D + " matches 1 as " + LK + " at @s run playsound minecraft:entity.lightning_bolt.thunder player @a ~ ~ ~ 3 0.7", "Гром"});
            } else {
                String MK = "@e[type=minecraft:marker,tag=mkmet]";
                s.add(new String[]{"Метеор: падение", "execute if score #d " + D + " matches 1.. as " + MK + " at @s run particle minecraft:flame ~ ~6 ~ 0.4 4 0.4 0.02 8", "Огненный столб"});
                s.add(new String[]{"Метеор: тень", "execute if score #d " + D + " matches 1.. as " + MK + " at @s run particle minecraft:large_smoke ~ ~0.2 ~ 2 0.1 2 0 3", "Тень на земле"});
                s.add(new String[]{"Метеор: взрыв", "execute if score #d " + D + " matches 1 as " + MK + " at @s run particle minecraft:explosion_emitter ~ ~1 ~ 2 1 2 0 4", "Взрыв"});
                s.add(new String[]{"Метеор: урон", "execute if score #d " + D + " matches 1 run damage " + LK + " 14 minecraft:explosion", "Урон по площади"});
                s.add(new String[]{"Метеор: звук", "execute if score #d " + D + " matches 1 as " + MK + " at @s run playsound minecraft:entity.generic.explode player @a ~ ~ ~ 3 0.6", "Звук"});
                s.add(new String[]{"Метеор: убрать метку", "execute if score #d " + D + " matches 1 run kill " + MK, "Убирает метку падения"});
            }
            s.add(new String[]{"Снять захват", "execute if score #d " + D + " matches 1 run tag " + LK + " remove mklock", "Очищает метку цели"});
            s.add(new String[]{"Отсчёт полёта", "execute if score #d " + D + " matches 1.. run scoreboard players remove #d " + D + " 1", "Время идёт"});
        }
        b.chain(w.name() + ": мозг оружия", s.toArray(new String[0][]));

        String BTN0 = "{x+1} {y+1} {z-1}", BTN1 = "{x+2} {y+1} {z-1}", BTN2 = "{x+3} {y+1} {z-1}";
        String P = "minecraft:stone_button[powered=true]";
        b.chain(w.name() + ": пульт", new String[][]{
                {"Кнопка ВЫДАТЬ: нажата", "execute if block " + BTN0 + " " + P + " if score #lat " + D + " matches 0 run " + S + "#g " + D + " 1", "Один раз за нажатие"},
                {"Выдача оружия", "execute if score #g " + D + " matches 1 run give @p " + itemNbt(w, idx) + " 1", "Выдаёт предмет-удилище ближайшему игроку"},
                {"Защита от повтора", "execute if score #g " + D + " matches 1 run " + S + "#lat " + D + " 1", "Кнопка не выдаёт сто штук подряд"},
                {"Сброс выдачи", S + "#g " + D + " 0", "Готовит следующее нажатие"},
                {"Кнопка отпущена", "execute unless block " + BTN0 + " " + P + " run " + S + "#lat " + D + " 0", "Снова можно нажимать"},
                {"Кнопка ВЫКЛ", "execute if block " + BTN1 + " " + P + " run " + S + "#on " + D + " 0", "Оружие перестаёт стрелять"},
                {"Кнопка ВКЛ", "execute if block " + BTN2 + " " + P + " run " + S + "#on " + D + " 1", "Оружие снова работает"},
                {"Лампа: включено", "execute if score #on " + D + " matches 1 run setblock {x+4} {y+1} {z+0} minecraft:lime_concrete", "Зелёная лампа"},
                {"Лампа: выключено", "execute unless score #on " + D + " matches 1 run setblock {x+4} {y+1} {z+0} minecraft:red_concrete", "Красная лампа"}});
        b.setup("scoreboard objectives add " + U + " minecraft.used:minecraft.carrot_on_a_stick", "scoreboard objectives add " + C + " dummy", "scoreboard objectives add " + D + " dummy",
                S + "#on " + D + " 1", S + "#lat " + D + " 0", S + "#g " + D + " 0", S + "#d " + D + " 0");
        b.setup(NeroKit.sign(1, 0, -1, "ВЫДАТЬ", "оружие", "", ""), NeroKit.sign(2, 0, -1, "ВЫКЛ", "", "", ""), NeroKit.sign(3, 0, -1, "ВКЛ", "", "", ""));
        b.setup(NeroKit.sign(2, 2, -1, w.name().length() > 15 ? w.name().substring(0, 15) : w.name(), "ПКМ в воздух", "", ""));
        b.setup("execute positioned {x+7} {y+1} {z+2} run summon minecraft:item_display ~0.5 ~1.1 ~0.5 {item:{id:\"minecraft:carrot_on_a_stick\",Count:1b},item_display:\"fixed\",billboard:\"vertical\","
                + "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[1.4f,1.4f,1.4f]},Tags:[\"{tag}\"]}");
        b.help = w.name() + " (вымышленное игровое оружие): " + w.how() + ". Нажми кнопку ВЫДАТЬ на пульте, возьми удилище в руку и жми ПКМ в воздух. Бьёт только враждебных мобов. Командных блоков в мозге: " + s.size() + ".";
        return b.done();
    }

    private NeroArsenal() { }
}
