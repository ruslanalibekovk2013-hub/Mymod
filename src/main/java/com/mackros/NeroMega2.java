package com.mackros;

import java.util.ArrayList;
import java.util.List;

/** Вторая часть больших механизмов: летающие аппараты, вращающиеся машины и световые шоу. */
final class NeroMega2 {
    private static final String S = "scoreboard players set ";

    private static void std(NeroMega.K k, String[] labels, String[] cmds, String[][] signs, String[] board) { k.pultAll(labels, cmds, signs, board); }

    private static String[][] sg(String... a) {
        String[][] r = new String[6][4];
        for (int i = 0; i < 6; i++) for (int j = 0; j < 4; j++) r[i][j] = i * 4 + j < a.length ? a[i * 4 + j] : "";
        return r;
    }

    // ------------------------------------------------------------------ летающие
    static void flyer(NeroMega.K k, String key) {
        String L = k.L, T = k.T;
        k.sy = 4;
        String ALLs = k.ALL_SEL, HULL = k.sel("body");
        switch (key) {
            case "x_drone" -> {
                k.block("body", "gray_concrete", new double[]{-0.4, 0, -0.4}, new double[]{0.8, 0.25, 0.8}, false);
                k.block("led0", "red_concrete", new double[]{-0.1, 0.1, 0.4}, new double[]{0.08, 0.08, 0.05}, false);
                k.block("led1", "lime_concrete", new double[]{0.02, 0.1, 0.4}, new double[]{0.08, 0.08, 0.05}, false);
                double[][] pos = {{-0.45, -0.45}, {0.45, -0.45}, {-0.45, 0.45}, {0.45, 0.45}};
                double[] rs = {0.5, 0.03, 0.08};
                for (int i = 0; i < 4; i++) {
                    k.block("arm" + i, "black_concrete", new double[]{pos[i][0] - 0.05, 0.05, pos[i][1] - 0.05}, new double[]{0.1, 0.1, 0.1}, false);
                    k.block("r" + i, "light_gray_concrete", new double[]{pos[i][0] - 0.25, 0.2, pos[i][1] - 0.04}, rs, false);
                }
                String gate = "execute if score #on " + L + " matches 1 if score #low " + L + " matches 0 if score #hold " + L + " matches 0 ";
                k.add(k.brain, "Сброс следования", S + "#go " + L + " 0", "Каждый такт решает заново");
                k.add(k.brain, "Игрок далеко", gate + "as " + HULL + " at @s if entity @p[distance=4..40] run " + S + "#go " + L + " 1", "Летит к игроку, если он дальше 4 блоков");
                k.add(k.brain, "Повернуться к игроку", "execute if score #go " + L + " matches 1 as " + HULL + " at @s run tp " + ALLs + " ~ ~ ~ facing entity @p feet", "Поворот к игроку");
                k.add(k.brain, "Выровнять", "execute if score #go " + L + " matches 1 as " + ALLs + " at @s run tp @s ~ ~ ~ ~ 0", "Без наклона");
                k.add(k.brain, "Полёт вперёд", "execute if score #go " + L + " matches 1 as " + HULL + " at @s run tp " + ALLs + " ^ ^ ^0.28", "Движение к игроку");
                k.add(k.brain, "Набор высоты", "execute if score #on " + L + " matches 1 as " + HULL + " at @s positioned ~ ~-2.8 ~ if entity @p[x=~-40,z=~-40,dx=80,dz=80,dy=60] run tp " + ALLs + " ~ ~2.9 ~", "Поднимается, если игрок почти на одной высоте");
                k.add(k.brain, "Снижение", "execute if score #on " + L + " matches 1 as " + HULL + " at @s positioned ~ ~-4.2 ~ unless entity @p[x=~-40,z=~-40,dx=80,dz=80,dy=60] if entity @p[distance=..60] run tp " + ALLs + " ~ ~4.1 ~", "Опускается, если игрок ниже");
                k.add(k.brain, "Не касаться земли", "execute as " + HULL + " at @s unless block ~ ~-1 ~ minecraft:air run tp " + ALLs + " ~ ~0.3 ~", "Не опускается ниже блока над землёй");
                k.add(k.brain, "Покачивание вверх", "execute if score #ph " + L + " matches 0..3 as " + ALLs + " at @s run tp @s ~ ~0.02 ~", "Лёгкое покачивание");
                k.add(k.brain, "Покачивание вниз", "execute if score #ph " + L + " matches 4..7 as " + ALLs + " at @s run tp @s ~ ~-0.02 ~", "Лёгкое покачивание");
                k.add(k.brain, "Фаза", "execute if score #on " + L + " matches 1 run scoreboard players add #ph " + L + " 1", "Счётчик фазы винтов");
                k.add(k.brain, "Фаза: круг", "execute if score #ph " + L + " matches 8.. run " + S + "#ph " + L + " 0", "Круг фазы");
                k.add(k.brain, "Звук винтов", "execute if score #on " + L + " matches 1 if score #ph " + L + " matches 0 as " + HULL + " at @s run playsound minecraft:block.beacon.ambient neutral @a ~ ~ ~ 0.3 2", "Гул винтов");
                k.add(k.brain, "Огонь 1: красный", "execute if score #ph " + L + " matches 0..3 run " + k.recolor("led0", "red_concrete"), "Мигающий огонь");
                k.add(k.brain, "Огонь 1: серый", "execute if score #ph " + L + " matches 4..7 run " + k.recolor("led0", "gray_concrete"), "Мигающий огонь");
                k.add(k.brain, "Огонь 2: серый", "execute if score #ph " + L + " matches 0..3 run " + k.recolor("led1", "gray_concrete"), "Мигающий огонь");
                k.add(k.brain, "Огонь 2: зелёный", "execute if score #ph " + L + " matches 4..7 run " + k.recolor("led1", "lime_concrete"), "Мигающий огонь");
                for (int f = 0; f < 8; f++) for (int i = 0; i < 4; i++)
                    k.add(k.anim, "Винт " + (i + 1) + ": кадр " + (f + 1), "execute if score #ph " + L + " matches " + f + " run " + k.spin("r" + i, new double[]{pos[i][0], 0.215, pos[i][1]}, rs, NeroMega.K.qa('y', (i % 2 == 0 ? 1 : -1) * f * 22.5), 2), "Вращение винта");
                k.hostiles(k.anim, "execute if score #on " + L + " matches 1 if score #ph " + L + " matches 0 ", "as " + HULL + " at @s", 18);
                k.add(k.anim, "Подсветка врагов", "execute if entity @e[tag=" + T + "] run effect give @e[tag=" + T + "] minecraft:glowing 3 0 true", "Разведка: враги светятся");
                k.add(k.anim, "Счёт врагов", "execute store result score #st " + L + " if entity @e[tag=" + T + "]", "Считает найденных врагов");
                k.add(k.anim, "Сигнал разведки", "execute if score #st " + L + " matches 1.. as " + HULL + " at @s run playsound minecraft:block.note_block.pling neutral @a ~ ~ ~ 1 2", "Сигнал, если есть враги");
                k.add(k.anim, "Снять метки", "tag @e[tag=" + T + "] remove " + T, "Убирает метки");
                k.systems("");
                std(k, new String[]{"ВКЛ", "ВЫКЛ", "ДОМОЙ", "ЗАРЯД", "ВИСЕТЬ", "ЛЕТЕТЬ"}, new String[]{S + "#on " + L + " 1", S + "#on " + L + " 0", "tp " + ALLs + " {x+24} {y+4} {z+10}", S + "#bat " + L + " 1000", S + "#hold " + L + " 1", S + "#hold " + L + " 0"},
                        sg("ВКЛ", "включает", "дрона", "", "ВЫКЛ", "выключает", "дрона", "", "ДОМОЙ", "возврат", "на старт", "", "ЗАРЯД", "полная", "батарея", "", "ВИСЕТЬ", "дрон", "на месте", "", "ЛЕТЕТЬ", "снова", "за игроком", ""),
                        new String[]{"ДРОН-СЛЕДОПЫТ", "Летит за игроком,", "держит высоту, мигает,", "подсвечивает врагов.", "ВКЛ - запуск", "ВИСЕТЬ - пауза"});
            }
            case "x_heli" -> {
                k.block("body", "red_concrete", new double[]{-0.6, 0.3, -1.0}, new double[]{1.2, 0.8, 2.0}, false);
                k.block("glass", "light_blue_stained_glass", new double[]{-0.5, 0.6, 0.6}, new double[]{1.0, 0.45, 0.9}, false);
                k.block("tail", "red_concrete", new double[]{-0.1, 0.7, -2.6}, new double[]{0.2, 0.2, 1.7}, false);
                k.block("fin", "white_concrete", new double[]{-0.04, 0.7, -2.7}, new double[]{0.08, 0.7, 0.4}, false);
                k.block("skidL", "black_concrete", new double[]{-0.7, 0, -0.9}, new double[]{0.08, 0.08, 1.8}, false);
                k.block("skidR", "black_concrete", new double[]{0.62, 0, -0.9}, new double[]{0.08, 0.08, 1.8}, false);
                k.block("mast", "iron_block", new double[]{-0.06, 1.1, -0.06}, new double[]{0.12, 0.35, 0.12}, false);
                double[] bs = {3.4, 0.04, 0.2}, ts = {0.04, 0.9, 0.12};
                k.block("rotA", "gray_concrete", new double[]{-1.7, 1.45, -0.1}, bs, false);
                k.block("rotB", "gray_concrete", new double[]{-0.1, 1.45, -1.7}, new double[]{0.2, 0.04, 3.4}, false);
                k.block("trot", "gray_concrete", new double[]{0.12, 0.75, -2.65}, ts, false);
                String gate = "execute if score #on " + L + " matches 1 if score #low " + L + " matches 0 ";
                k.add(k.brain, "Режим полёта: сброс", S + "#go " + L + " 0", "Сброс признака движения");
                k.add(k.brain, "Режим полёта: вперёд", gate + "if score #hold " + L + " matches 1 if score #dir " + L + " matches 1 run " + S + "#go " + L + " 1", "Летит вперёд только после взлёта");
                k.add(k.brain, "Взлёт: подъём", gate + "if score #hold " + L + " matches 1 if score #st " + L + " matches ..60 as " + HULL + " at @s run tp " + ALLs + " ~ ~0.2 ~", "Набирает высоту 12 блоков");
                k.add(k.brain, "Взлёт: счётчик", gate + "if score #hold " + L + " matches 1 if score #st " + L + " matches ..60 run scoreboard players add #st " + L + " 1", "Считает набранную высоту");
                k.add(k.brain, "Посадка: спуск", "execute if score #hold " + L + " matches 0 if score #st " + L + " matches 1.. as " + HULL + " at @s unless block ~ ~-0.3 ~ minecraft:air run " + S + "#st " + L + " 0", "Сели на землю");
                k.add(k.brain, "Посадка: спуск 2", "execute if score #hold " + L + " matches 0 if score #st " + L + " matches 1.. as " + HULL + " at @s if block ~ ~-0.3 ~ minecraft:air run tp " + ALLs + " ~ ~-0.2 ~", "Опускается до земли");
                k.add(k.brain, "Полёт вперёд: свободно", "execute if score #go " + L + " matches 1 as " + HULL + " at @s if block ^ ^ ^2 minecraft:air if block ^ ^1 ^2 minecraft:air run tp " + ALLs + " ^ ^ ^0.35", "Летит вперёд, если свободно");
                k.add(k.brain, "Фаза винта", "execute if score #hold " + L + " matches 1 run scoreboard players add #ph " + L + " 1", "Винт крутится в воздухе");
                k.add(k.brain, "Фаза винта: круг", "execute if score #ph " + L + " matches 16.. run " + S + "#ph " + L + " 0", "Круг винта");
                k.add(k.brain, "Звук винта", "execute if score #hold " + L + " matches 1 if score #ph " + L + " matches 0 as " + HULL + " at @s run playsound minecraft:entity.bee.loop neutral @a ~ ~ ~ 1 0.5", "Гул винта");
                k.add(k.brain, "Пыль при посадке", "execute if score #hold " + L + " matches 1 if score #st " + L + " matches ..5 as " + HULL + " at @s run particle minecraft:cloud ~ ~-0.2 ~ 1.5 0 1.5 0.01 6", "Пыль под вертолётом");
                for (int f = 0; f < 16; f++) {
                    double a = f * 22.5;
                    k.add(k.anim, "Несущий винт A: кадр " + (f + 1), "execute if score #ph " + L + " matches " + f + " run " + k.spin("rotA", new double[]{0, 1.47, 0}, bs, NeroMega.K.qa('y', a), 2), "Вращение несущего винта");
                    k.add(k.anim, "Несущий винт B: кадр " + (f + 1), "execute if score #ph " + L + " matches " + f + " run " + k.spin("rotB", new double[]{0, 1.47, 0}, new double[]{0.2, 0.04, 3.4}, NeroMega.K.qa('y', a), 2), "Вращение несущего винта");
                }
                for (int f = 0; f < 8; f++) k.add(k.anim, "Хвостовой винт: кадр " + (f + 1), "execute if score #ph " + L + " matches " + (2 * f) + " run " + k.spin("trot", new double[]{0.14, 1.2, -2.59}, ts, NeroMega.K.qa('x', f * 45), 2), "Вращение хвостового винта");
                k.add(k.pult, "Поворот влево: фронт", NeroKit.poll(4) + "execute unless score #pL " + L + " matches 1 as " + ALLs + " at @s run tp @s ~ ~ ~ ~-25 ~", "Поворот влево");
                k.add(k.pult, "Поворот влево: отметка", NeroKit.poll(4) + S + "#pL " + L + " 1", "Кнопка нажата");
                k.add(k.pult, "Поворот вправо: фронт", NeroKit.poll(5) + "execute unless score #pR " + L + " matches 1 as " + ALLs + " at @s run tp @s ~ ~ ~ ~25 ~", "Поворот вправо");
                k.add(k.pult, "Поворот вправо: отметка", NeroKit.poll(5) + S + "#pR " + L + " 1", "Кнопка нажата");
                k.add(k.pult, "Поворот: сброс влево", "execute unless block {x+9} {y+1} {z-1} minecraft:stone_button[powered=true] run " + S + "#pL " + L + " 0", "Кнопка отпущена");
                k.add(k.pult, "Поворот: сброс вправо", "execute unless block {x+11} {y+1} {z-1} minecraft:stone_button[powered=true] run " + S + "#pR " + L + " 0", "Кнопка отпущена");
                k.systems("");
                std(k, new String[]{"ВЗЛЁТ", "ПОСАДКА", "ВПЕРЁД", "СТОП", "ВЛЕВО", "ВПРАВО"}, new String[]{S + "#on " + L + " 1", S + "#hold " + L + " 0", S + "#dir " + L + " 1", S + "#dir " + L + " 0", "particle minecraft:note ~ ~ ~ 0 0 0 0 1", "particle minecraft:note ~ ~ ~ 0 0 0 0 1"},
                        sg("ВЗЛЁТ", "включает", "двигатель", "и взлетает", "ПОСАДКА", "спуск на", "землю", "", "ВПЕРЁД", "летит", "вперёд", "", "СТОП", "висит на", "месте", "", "ВЛЕВО", "поворот", "на 25 градусов", "", "ВПРАВО", "поворот", "на 25 градусов", ""),
                        new String[]{"ВЕРТОЛЁТ", "ВЗЛЁТ: подъём на 12 блоков.", "ВПЕРЁД: полёт, СТОП: висеть.", "ПОСАДКА: спуск на землю.", "Винты крутятся при взлёте.", "Поворот: ВЛЕВО и ВПРАВО"});
                k.add(k.pult, "Взлёт: включение", NeroKit.poll(0) + S + "#hold " + L + " 1", "ВЗЛЁТ включает полёт");
                k.add(k.pult, "Посадка: выключение движения", NeroKit.poll(1) + S + "#dir " + L + " 0", "ПОСАДКА останавливает движение вперёд");
            }
            case "x_rocket" -> rocket(k);
            default -> ufo(k);
        }
    }

    private static void rocket(NeroMega.K k) {
        String L = k.L;
        k.sy = 0;
        String ALLs = k.ALL_SEL;
        k.block("body", "white_concrete", new double[]{-0.6, 0, -0.6}, new double[]{1.2, 3.0, 1.2}, false);
        k.block("band", "red_concrete", new double[]{-0.62, 1.2, -0.62}, new double[]{1.24, 0.4, 1.24}, false);
        k.block("nose", "red_concrete", new double[]{-0.4, 3.0, -0.4}, new double[]{0.8, 0.8, 0.8}, false);
        k.block("tip", "red_concrete", new double[]{-0.2, 3.8, -0.2}, new double[]{0.4, 0.6, 0.4}, false);
        for (int i = 0; i < 4; i++) {
            double[][] f = {{-0.9, -0.05}, {0.6, -0.05}, {-0.05, -0.9}, {-0.05, 0.6}};
            k.block("fin" + i, "gray_concrete", new double[]{f[i][0], 0, f[i][1]}, i < 2 ? new double[]{0.3, 1.0, 0.1} : new double[]{0.1, 1.0, 0.3}, false);
        }
        k.block("flame", "orange_concrete", new double[]{-0.3, -0.5, -0.3}, new double[]{0.6, 0.5, 0.6}, false);
        String HULL = k.sel("body"), gate = "execute if score #run " + L + " matches 1 ";
        k.add(k.brain, "Таймер пуска", gate + "run scoreboard players add #t " + L + " 1", "Время пуска идёт");
        for (int s = 10; s >= 1; s--) {
            int t = (10 - s) * 20 + 20;
            k.add(k.brain, "Отсчёт " + s, gate + "if score #t " + L + " matches " + t + ".." + (t + 1) + " run title @a actionbar {\"text\":\"Пуск через " + s + "\",\"color\":\"yellow\"}", "Обратный отсчёт");
            k.add(k.brain, "Сигнал " + s, gate + "if score #t " + L + " matches " + t + ".." + (t + 1) + " as " + HULL + " at @s run playsound minecraft:block.note_block.hat neutral @a ~ ~ ~ 1 " + (1 + (10 - s) * 0.08), "Сигнал отсчёта");
        }
        k.add(k.brain, "Пар перед стартом", gate + "if score #t " + L + " matches 120..219 as " + HULL + " at @s run particle minecraft:cloud ~ ~0.1 ~ 0.8 0.1 0.8 0.02 4", "Пар у стартового стола");
        k.add(k.brain, "Зажигание: огонь", gate + "if score #t " + L + " matches 220..560 as " + HULL + " at @s run particle minecraft:flame ~ ~-0.6 ~ 0.15 0.1 0.15 0.05 10", "Пламя двигателя");
        k.add(k.brain, "Зажигание: дым", gate + "if score #t " + L + " matches 220..560 as " + HULL + " at @s run particle minecraft:large_smoke ~ ~-1 ~ 0.3 0.3 0.3 0.02 6", "Дым двигателя");
        k.add(k.brain, "Зажигание: гул", gate + "if score #t " + L + " matches 220..221 as " + HULL + " at @s run playsound minecraft:entity.generic.explode neutral @a ~ ~ ~ 2 0.6", "Грохот старта");
        k.add(k.brain, "Пламя: видимое", gate + "if score #t " + L + " matches 220 run " + k.move("flame", new double[]{-0.3, -1.5, -0.3}, 4), "Пламя вытягивается");
        double[] dv = {0.05, 0.1, 0.15, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
        for (int i = 0; i < dv.length; i++) {
            int a = 230 + i * 10;
            k.add(k.anim, "Разгон " + (i + 1), gate + "if score #t " + L + " matches " + a + ".." + (a + 9) + " as " + ALLs + " at @s run tp @s ~ ~" + NeroMega.K.f(dv[i]) + " ~", "Скорость растёт: " + dv[i] + " блока за такт");
        }
        String[] col = {"16711680", "16776960", "65280", "255", "16711935", "65535"};
        for (int i = 0; i < col.length; i++)
            k.add(k.anim, "Салют " + (i + 1), gate + "if score #t " + L + " matches " + (360 + i * 4) + " as " + HULL + " at @s run summon minecraft:firework_rocket ~ ~2 ~ {LifeTime:12,FireworksItem:{id:\"minecraft:firework_rocket\",Count:1,tag:{Fireworks:{Flight:1,Explosions:[{Type:1,Colors:[I;" + col[i] + "],FadeColors:[I;16777215]}]}}}}", "Фейерверк на высоте");
        k.add(k.anim, "Купол: пламя гаснет", gate + "if score #t " + L + " matches 361 run " + k.move("flame", new double[]{-0.3, -0.5, -0.3}, 3), "Пламя уменьшается");
        for (int i = 0; i < 10; i++) {
            int a = 380 + i * 14;
            k.add(k.anim, "Спуск " + (i + 1), gate + "if score #t " + L + " matches " + a + ".." + (a + 13) + " as " + ALLs + " at @s run tp @s ~ ~-0.45 ~", "Плавный спуск на парашюте");
        }
        k.add(k.anim, "Парашют: частицы", gate + "if score #t " + L + " matches 380..520 as " + HULL + " at @s run particle minecraft:end_rod ~ ~4.5 ~ 0.4 0 0.4 0.01 3", "След парашюта");
        k.add(k.anim, "Посадка: возврат", gate + "if score #t " + L + " matches 521 run tp " + ALLs + " {x+24} {y+0} {z+10}", "Ракета возвращается на стол");
        k.add(k.anim, "Посадка: табло", gate + "if score #t " + L + " matches 522 run title @a actionbar {\"text\":\"Ракета вернулась\",\"color\":\"green\"}", "Сообщение");
        k.add(k.anim, "Конец программы", gate + "if score #t " + L + " matches 523.. run " + S + "#run " + L + " 0", "Программа завершена");
        k.systems("");
        std(k, new String[]{"ПУСК", "СТОП", "СБРОС", "ВКЛ", "ВЫКЛ", "ТЕСТ"}, new String[]{S + "#run " + L + " 1", S + "#run " + L + " 0", "tp " + ALLs + " {x+24} {y+0} {z+10}", S + "#on " + L + " 1", S + "#on " + L + " 0", "particle minecraft:end_rod {x+24} {y+4} {z+10} 0.5 1 0.5 0.05 30"},
                sg("ПУСК", "обратный", "отсчёт 10 с", "и старт", "СТОП", "пауза", "программы", "", "СБРОС", "ракета на", "стол", "", "ВКЛ", "питание", "включить", "", "ВЫКЛ", "питание", "выключить", "", "ТЕСТ", "частицы", "у ракеты", ""),
                new String[]{"РАКЕТА С ЗАПУСКОМ", "ПУСК: отсчёт 10 секунд,", "старт, разгон, салют,", "спуск и возврат на стол.", "СБРОС: вернуть и обнулить", "Ракета: display-модель"});
        k.add(k.pult, "СБРОС: время", NeroKit.poll(2) + S + "#t " + L + " 0", "СБРОС обнуляет время");
        k.add(k.pult, "СБРОС: остановка", NeroKit.poll(2) + S + "#run " + L + " 0", "СБРОС останавливает программу");
    }

    private static void ufo(NeroMega.K k) {
        String L = k.L, T = k.T, ALLs = k.ALL_SEL;
        k.sy = 6;
        k.block("body", "light_gray_concrete", new double[]{-1.1, 0, -1.1}, new double[]{2.2, 0.2, 2.2}, false);
        k.block("disc", "gray_concrete", new double[]{-1.5, 0.2, -1.5}, new double[]{3.0, 0.12, 3.0}, false);
        k.block("belly", "gray_concrete", new double[]{-0.6, -0.25, -0.6}, new double[]{1.2, 0.25, 1.2}, false);
        k.block("dome", "light_blue_stained_glass", new double[]{-0.6, 0.32, -0.6}, new double[]{1.2, 0.5, 1.2}, false);
        for (int i = 0; i < 4; i++) k.block("lt" + i, "yellow_concrete", new double[]{1.2 - 0.05, 0.18, -0.05}, new double[]{0.1, 0.1, 0.1}, false);
        String HULL = k.sel("body"), gate = "execute if score #on " + L + " matches 1 if score #low " + L + " matches 0 ";
        k.add(k.brain, "Фаза облёта", gate + "run scoreboard players add #ph " + L + " 1", "Счётчик круга облёта");
        k.add(k.brain, "Фаза: круг", "execute if score #ph " + L + " matches 64.. run " + S + "#ph " + L + " 0", "Круг облёта замыкается");
        for (int f = 0; f < 16; f++) {
            double a = f * 22.5;
            k.add(k.brain, "Облёт: шаг " + (f + 1), gate + "if score #hold " + L + " matches 0 if score #ph " + L + " matches " + (f * 4) + ".." + (f * 4 + 3) + " as " + ALLs + " at @s run tp @s ~" + NeroMega.K.f(0.35 * Math.cos(Math.toRadians(a))) + " ~" + NeroMega.K.f(0.02 * Math.sin(Math.toRadians(a * 2))) + " ~" + NeroMega.K.f(0.35 * Math.sin(Math.toRadians(a))), "Движение по кругу");
        }
        for (int i = 0; i < 4; i++) for (int f = 0; f < 8; f++) {
            double a = Math.toRadians(f * 45 + i * 90);
            k.add(k.anim, "Огонь " + (i + 1) + ": кадр " + (f + 1), gate + "if score #ph " + L + " matches " + (f * 8) + ".." + (f * 8 + 3) + " run " + k.move("lt" + i, new double[]{1.25 * Math.cos(a) - 0.05, 0.18, 1.25 * Math.sin(a) - 0.05}, 5), "Огни бегут по ободу");
        }
        k.add(k.anim, "Луч: нижний", gate + "as " + HULL + " at @s run particle minecraft:end_rod ~ ~-1 ~ 0.3 0.9 0.3 0 3", "Световой луч вниз");
        k.add(k.anim, "Луч: низ", gate + "as " + HULL + " at @s run particle minecraft:end_rod ~ ~-3 ~ 0.5 0.9 0.5 0 3", "Световой луч вниз");
        k.add(k.anim, "Луч: земля", gate + "as " + HULL + " at @s run particle minecraft:glow ~ ~-5 ~ 0.7 0.9 0.7 0 3", "Луч у земли");
        k.hostiles(k.anim, gate, "as " + HULL + " at @s positioned ~ ~-4 ~", 5);
        k.add(k.anim, "Похищение: подъём", "execute if entity @e[tag=" + T + "] run effect give @e[tag=" + T + "] minecraft:levitation 2 1 true", "Враги поднимаются");
        k.add(k.anim, "Похищение: урон", "execute if entity @e[tag=" + T + "] run damage @e[tag=" + T + "] 2 minecraft:magic", "Лёгкий урон от луча");
        k.add(k.anim, "Похищение: звук", "execute if entity @e[tag=" + T + "] as " + HULL + " at @s run playsound minecraft:block.beacon.ambient neutral @a ~ ~ ~ 1.5 1.4", "Звук луча");
        k.add(k.anim, "Снять метки", "tag @e[tag=" + T + "] remove " + T, "Убирает метки");
        k.systems("");
        std(k, new String[]{"ВКЛ", "ВЫКЛ", "ДОМОЙ", "ЗАРЯД", "ВИСЕТЬ", "ЛЕТАТЬ"}, new String[]{S + "#on " + L + " 1", S + "#on " + L + " 0", "tp " + ALLs + " {x+24} {y+6} {z+10}", S + "#bat " + L + " 1000", S + "#hold " + L + " 1", S + "#hold " + L + " 0"},
                sg("ВКЛ", "включает", "НЛО", "", "ВЫКЛ", "выключает", "НЛО", "", "ДОМОЙ", "возврат", "на старт", "", "ЗАРЯД", "полная", "батарея", "", "ВИСЕТЬ", "стоит на", "месте", "", "ЛЕТАТЬ", "облёт по", "кругу", ""),
                new String[]{"НЛО", "Парит по кругу, светит", "лучом, поднимает врагов", "под собой.", "ВКЛ - запуск", "ВИСЕТЬ - стоять на месте"});
    }

    // ------------------------------------------------------------------ вращающиеся машины и шоу
    static void other(NeroMega.K k, String key) {
        switch (key) {
            case "x_windmill" -> windmill(k);
            case "x_ferris" -> ferris(k, false);
            case "x_carousel" -> ferris(k, true);
            case "x_radar" -> radar(k);
            case "x_clock" -> clock(k);
            case "x_crane" -> crane(k);
            case "x_arm" -> arm(k);
            case "x_disco" -> disco(k);
            case "x_fountain" -> fountain(k);
            case "x_fireworks" -> fireworks(k);
            default -> aurora(k);
        }
    }

    private static void speedPult(NeroMega.K k, String L, String[] extra) {
        String[] labels = {"СТОП", "МЕДЛЕННО", "ОБЫЧНО", "БЫСТРО", extra[0], extra[1]};
        String[] cmds = {S + "#spd " + L + " 0", S + "#spd " + L + " 1", S + "#spd " + L + " 2", S + "#spd " + L + " 3", extra[2], extra[3]};
        k.pultAll(labels, cmds, sg("СТОП", "останавливает", "вращение", "", "МЕДЛЕННО", "скорость 1", "", "", "ОБЫЧНО", "скорость 2", "", "", "БЫСТРО", "скорость 3", "", "", extra[0], extra[4], extra[5], "", extra[1], extra[6], extra[7], ""), new String[]{NeroMega.name(k.key).toUpperCase(), "Скорость: СТОП, МЕДЛЕННО,", "ОБЫЧНО, БЫСТРО.", extra[8], extra[9], "Вращение по кнопкам"});
    }

    private static void phase(NeroMega.K k, String L, int frames2) {
        k.add(k.brain, "Счётчик скорости", "scoreboard players add #sc " + L + " 1", "Чётность такта для медленной скорости");
        k.add(k.brain, "Счётчик скорости: круг", "execute if score #sc " + L + " matches 2.. run " + S + "#sc " + L + " 0", "Круг чётности");
        k.add(k.brain, "Фаза: медленно", "execute if score #on " + L + " matches 1 if score #spd " + L + " matches 1 if score #sc " + L + " matches 0 run scoreboard players add #ph " + L + " 1", "Скорость 1");
        k.add(k.brain, "Фаза: обычно", "execute if score #on " + L + " matches 1 if score #spd " + L + " matches 2 run scoreboard players add #ph " + L + " 1", "Скорость 2");
        k.add(k.brain, "Фаза: быстро", "execute if score #on " + L + " matches 1 if score #spd " + L + " matches 3 run scoreboard players add #ph " + L + " 2", "Скорость 3");
        k.add(k.brain, "Фаза: круг", "execute if score #ph " + L + " matches " + frames2 + ".. run scoreboard players remove #ph " + L + " " + frames2, "Круг вращения");
        k.add(k.brain, "Питание", S + "#on " + L + " 0", "Сбрасывает питание");
        k.add(k.brain, "Питание: есть скорость", "execute if score #spd " + L + " matches 1.. run " + S + "#on " + L + " 1", "Машина работает, если задана скорость");
    }

    private static void windmill(NeroMega.K k) {
        String L = k.L;
        k.sx = 24; k.sy = 10; k.sz = 12;
        k.b.part("Башня");
        k.b.box(23, 0, 9, 25, 9, 11, "stone_bricks");
        k.b.box(22, 10, 8, 26, 10, 12, "smooth_stone");
        k.b.box(24, 8, 12, 24, 9, 12, "oak_planks");
        k.block("hub", "iron_block", new double[]{-0.25, -0.25, -0.1}, new double[]{0.5, 0.5, 0.5}, false);
        double[] bs = {0.35, 4.0, 0.12};
        for (int i = 0; i < 4; i++) k.block("bl" + i, "white_concrete", new double[]{-0.175, 0, 0.45}, bs, false);
        for (int f = 0; f < 16; f++) for (int i = 0; i < 4; i++) {
            double a = f * 5.625 + i * 90;
            double[] q = NeroMega.K.qa('z', a), o = NeroMega.K.rot(q, new double[]{0.175, 0, 0});
            k.add(k.anim, "Лопасть " + (i + 1) + ": кадр " + (f + 1), "execute if score #ph " + L + " matches " + (2 * f) + ".." + (2 * f + 1) + " run data merge entity " + k.sel("bl" + i) + " {start_interpolation:0,interpolation_duration:2,transformation:{translation:" + NeroMega.K.arr(new double[]{-o[0], -o[1], 0.45}) + ",left_rotation:" + NeroMega.K.quat(q) + "}}", "Вращение лопасти");
        }
        phase(k, L, 32);
        k.add(k.brain, "Лампа генератора: горит", "execute if score #on " + L + " matches 1 run setblock {x+24} {y+8} {z+13} minecraft:redstone_lamp[lit=true]", "Лампа питается от ветряка");
        k.add(k.brain, "Лампа генератора: не горит", "execute unless score #on " + L + " matches 1 run setblock {x+24} {y+8} {z+13} minecraft:redstone_lamp[lit=false]", "Без вращения лампа гаснет");
        k.add(k.brain, "Шум ветра", "execute if score #on " + L + " matches 1 if score #ph " + L + " matches 0 as " + k.sel("hub") + " at @s run playsound minecraft:item.elytra.flying neutral @a ~ ~ ~ 0.4 0.6", "Свист лопастей");
        k.add(k.brain, "Ветер: частицы", "execute if score #on " + L + " matches 1 as " + k.sel("hub") + " at @s run particle minecraft:cloud ~ ~ ~1 2 2 0.1 0.05 2", "Потоки воздуха");
        k.add(k.brain, "Табло: стоит", "execute if score #spd " + L + " matches 0 run " + k.status("Стоит", "gray"), "Состояние на табло");
        k.add(k.brain, "Табло: медленно", "execute if score #spd " + L + " matches 1 run " + k.status("Медленно", "yellow"), "Состояние на табло");
        k.add(k.brain, "Табло: обычно", "execute if score #spd " + L + " matches 2 run " + k.status("Обычно", "green"), "Состояние на табло");
        k.add(k.brain, "Табло: быстро", "execute if score #spd " + L + " matches 3 run " + k.status("Быстро", "red"), "Состояние на табло");
        speedPult(k, L, new String[]{"ВКЛ", "ВЫКЛ", S + "#spd " + L + " 2", S + "#spd " + L + " 0", "включает", "ветряк", "выключает", "ветряк", "Лампа на башне горит,", "пока лопасти вращаются."});
    }

    private static void ferris(NeroMega.K k, boolean carousel) {
        String L = k.L;
        k.sx = 24; k.sy = carousel ? 1 : 6; k.sz = 12;
        int frames = 12, cabins = carousel ? 4 : 6;
        double R = carousel ? 2.8 : 5.0;
        k.b.part(carousel ? "Платформа" : "Опоры");
        if (carousel) { k.b.box(20, -1, 8, 28, -1, 16, "smooth_stone"); k.b.box(23, 0, 11, 25, 3, 13, "red_concrete"); }
        else { for (int sgn : new int[]{-1, 1}) k.b.line(24 + sgn * 5, 0, 12, 24, 6, 12, "iron_block"); k.b.box(23, 6, 11, 25, 6, 13, "iron_block"); }
        double[] cs = carousel ? new double[]{0.6, 0.5, 0.6} : new double[]{0.9, 0.9, 0.9};
        k.block("hub", "iron_block", new double[]{-0.3, -0.3, -0.3}, new double[]{0.6, 0.6, 0.6}, false);
        double[] beam = {R * 2, 0.15, 0.15};
        if (!carousel) { k.block("sp0", "gray_concrete", new double[]{-R, -0.075, 0.2}, beam, false); k.block("sp1", "gray_concrete", new double[]{-R, -0.075, 0.2}, beam, false); }
        else { k.block("sp0", "gray_concrete", new double[]{-R, 0, -0.075}, new double[]{R * 2, 0.12, 0.15}, false); k.block("sp1", "gray_concrete", new double[]{-0.075, 0, -R}, new double[]{0.15, 0.12, R * 2}, false); }
        for (int i = 0; i < cabins; i++) k.block("c" + i, carousel ? (i % 2 == 0 ? "red_concrete" : "yellow_concrete") : (i % 2 == 0 ? "orange_concrete" : "light_blue_concrete"), new double[]{R - cs[0] / 2, -cs[1] / 2, carousel ? 0 : 0.3}, cs, false);
        for (int f = 0; f < frames; f++) {
            int a0 = 2 * f * (32 / frames) / 2;
            double th = f * 360.0 / frames;
            String when = "execute if score #ph " + L + " matches " + (f * 32 / frames) + ".." + ((f + 1) * 32 / frames - 1) + " run ";
            if (!carousel) {
                for (int s = 0; s < 2; s++) {
                    double[] q = NeroMega.K.qa('z', th + s * 90);
                    double[] c = NeroMega.K.rot(q, new double[]{beam[0] / 2, beam[1] / 2, beam[2] / 2});
                    k.add(k.anim, "Спица " + (s + 1) + ": кадр " + (f + 1), when + "data merge entity " + k.sel("sp" + s) + " {start_interpolation:0,interpolation_duration:3,transformation:{translation:" + NeroMega.K.arr(new double[]{-c[0], -c[1], 0.2}) + ",left_rotation:" + NeroMega.K.quat(q) + "}}", "Вращение спицы колеса");
                }
            } else {
                for (int s = 0; s < 2; s++) {
                    double[] q = NeroMega.K.qa('y', th + s * 90);
                    double[] sz2 = s == 0 ? new double[]{R * 2, 0.12, 0.15} : new double[]{0.15, 0.12, R * 2};
                    double[] c = NeroMega.K.rot(q, new double[]{sz2[0] / 2, 0, sz2[2] / 2});
                    k.add(k.anim, "Перекладина " + (s + 1) + ": кадр " + (f + 1), when + "data merge entity " + k.sel("sp" + s) + " {start_interpolation:0,interpolation_duration:3,transformation:{translation:" + NeroMega.K.arr(new double[]{-c[0] + 0, 0.05 - 0, -c[2]}) + ",left_rotation:" + NeroMega.K.quat(q) + "}}", "Вращение перекладины");
                }
            }
            for (int i = 0; i < cabins; i++) {
                double a = Math.toRadians(th + i * 360.0 / cabins);
                double[] t = carousel ? new double[]{R * Math.cos(a) - cs[0] / 2, 0.2 + 0.15 * Math.sin(3 * a), R * Math.sin(a) - cs[2] / 2} : new double[]{R * Math.cos(a) - cs[0] / 2, R * Math.sin(a) - cs[1], 0.3};
                k.add(k.anim, (carousel ? "Сиденье " : "Кабина ") + (i + 1) + ": кадр " + (f + 1), when + k.move("c" + i, t, 3), carousel ? "Сиденье едет по кругу и качается" : "Кабина идёт по кругу и остаётся ровной");
            }
        }
        phase(k, L, 32);
        k.add(k.brain, "Сигнал", "execute if score #on " + L + " matches 1 if score #ph " + L + " matches 0 as " + k.sel("hub") + " at @s run playsound minecraft:block.note_block.chime neutral @a ~ ~ ~ 1 " + (carousel ? "1.2" : "0.8"), "Музыка вращения");
        k.add(k.brain, "Огни", "execute if score #on " + L + " matches 1 as " + k.sel("hub") + " at @s run particle minecraft:end_rod ~ ~ ~ " + R + " " + R + " 0.2 0 2", "Огни по кругу");
        k.add(k.brain, "Табло: стоит", "execute if score #spd " + L + " matches 0 run " + k.status("Стоит", "gray"), "Состояние");
        k.add(k.brain, "Табло: едет", "execute if score #spd " + L + " matches 1.. run " + k.status("Работает", "green"), "Состояние");
        speedPult(k, L, new String[]{"ВКЛ", "ВЫКЛ", S + "#spd " + L + " 2", S + "#spd " + L + " 0", "запускает", "обычная", "останавливает", "вращение", carousel ? "Сиденья качаются при" : "Кабины остаются ровными", carousel ? "движении по кругу." : "на любой высоте."});
    }

    private static void radar(NeroMega.K k) {
        String L = k.L, T = k.T;
        k.sx = 24; k.sy = 5; k.sz = 12;
        k.b.part("Башня радара");
        k.b.box(23, 0, 11, 25, 4, 13, "iron_block");
        k.b.box(22, 5, 10, 26, 5, 14, "smooth_stone");
        double[] arm = {0.2, 0.2, 3.6}, dish = {1.4, 1.4, 0.15};
        k.block("armA", "gray_concrete", new double[]{-0.1, 0, -0.2}, arm, false);
        k.block("dish", "light_gray_concrete", new double[]{-0.7, 0.2, 3.0}, dish, false);
        k.block("mast", "iron_bars", new double[]{-0.05, -1.4, -0.05}, new double[]{0.1, 1.4, 0.1}, false);
        for (int f = 0; f < 32; f++) {
            double[] q = NeroMega.K.qa('y', f * 11.25);
            String when = "execute if score #ph " + L + " matches " + f + " run ";
            double[] c1 = NeroMega.K.rot(q, new double[]{0.1, 0.1, 1.8}), c2 = NeroMega.K.rot(q, new double[]{0.7, 0.7, 0.075});
            k.add(k.anim, "Антенна: кадр " + (f + 1), when + "data merge entity " + k.sel("armA") + " {start_interpolation:0,interpolation_duration:2,transformation:{translation:" + NeroMega.K.arr(new double[]{-c1[0], 0.1 - c1[1] + 0.1, -c1[2] + 1.5 - 1.5 + 0}) + ",left_rotation:" + NeroMega.K.quat(q) + "}}", "Вращение антенны");
            double[] pos = NeroMega.K.rot(q, new double[]{0, 0, 3.0});
            k.add(k.anim, "Тарелка: кадр " + (f + 1), when + k.spin("dish", new double[]{pos[0] + 0, 0.9, pos[2] + 0.075 * 0}, dish, q, 2), "Вращение тарелки");
        }
        phase(k, L, 32);
        k.add(k.brain, "Скан: метки", S + "#st " + L + " 0", "Сброс счёта врагов");
        k.hostiles(k.brain, "execute if score #on " + L + " matches 1 if score #ph " + L + " matches 0 ", "as " + k.sel("armA") + " at @s", 30);
        k.add(k.brain, "Скан: подсветка", "execute if entity @e[tag=" + T + "] run effect give @e[tag=" + T + "] minecraft:glowing 2 0 true", "Враги подсвечены");
        k.add(k.brain, "Скан: счёт", "execute store result score #st " + L + " if entity @e[tag=" + T + "]", "Считает врагов");
        k.add(k.brain, "Скан: сигнал", "execute if score #st " + L + " matches 1.. as " + k.sel("armA") + " at @s run playsound minecraft:block.note_block.bell neutral @a ~ ~ ~ 1 1.6", "Сигнал, если враги рядом");
        k.add(k.brain, "Скан: снять метки", "tag @e[tag=" + T + "] remove " + T, "Убирает метки");
        k.add(k.brain, "Табло: тихо", "execute if score #st " + L + " matches 0 run " + k.status("Чисто", "green"), "Врагов нет");
        k.add(k.brain, "Табло: мало", "execute if score #st " + L + " matches 1..3 run " + k.status("Враги рядом", "yellow"), "Врагов от 1 до 3");
        k.add(k.brain, "Табло: много", "execute if score #st " + L + " matches 4.. run " + k.status("ТРЕВОГА", "red"), "Врагов 4 и больше");
        for (int i = 0; i < 16; i++) {
            double a = Math.toRadians(i * 22.5);
            k.add(k.brain, "Кольцо радиуса " + (i + 1), "execute if score #on " + L + " matches 1 if score #ph " + L + " matches " + (i * 2) + " as " + k.sel("armA") + " at @s run particle minecraft:end_rod ~" + NeroMega.K.f(12 * Math.cos(a)) + " ~-4 ~" + NeroMega.K.f(12 * Math.sin(a)) + " 0 0 0 0 1", "Граница обзора радара");
        }
        speedPult(k, L, new String[]{"ВКЛ", "ВЫКЛ", S + "#spd " + L + " 2", S + "#spd " + L + " 0", "запускает", "радар", "останавливает", "радар", "Враги в радиусе 30", "подсвечиваются."});
    }

    private static void clock(NeroMega.K k) {
        String L = k.L;
        k.sx = 24; k.sy = 11; k.sz = 8;
        k.b.part("Башня часов");
        k.b.box(21, 0, 9, 27, 17, 15, "stone_bricks");
        k.b.box(22, 18, 10, 26, 18, 14, "dark_oak_planks");
        k.b.box(22, 9, 8, 26, 13, 8, "white_concrete");
        double[] hh = {0.18, 1.1, 0.1}, mh = {0.12, 1.7, 0.1};
        k.block("hour", "black_concrete", new double[]{-0.09, 0, 0}, hh, false);
        k.block("min", "black_concrete", new double[]{-0.06, 0, -0.12}, mh, false);
        k.block("pend", "gold_block", new double[]{-0.2, -9.0, 0.5}, new double[]{0.4, 0.5, 0.2}, false);
        k.block("gear0", "iron_block", new double[]{-0.9, -2.5, 0.6}, new double[]{0.6, 0.6, 0.1}, false);
        k.block("gear1", "iron_block", new double[]{0.4, -2.5, 0.6}, new double[]{0.6, 0.6, 0.1}, false);
        k.add(k.brain, "Время суток", "execute store result score #t " + L + " run time query daytime", "Читает игровое время");
        k.add(k.brain, "Час: копия", "scoreboard players operation #h " + L + " = #t " + L, "Копия времени");
        k.add(k.brain, "Час: делитель", S + "#k " + L + " 1000", "Делитель 1000");
        k.add(k.brain, "Час: деление", "scoreboard players operation #h " + L + " /= #k " + L, "Час от 0 до 23");
        k.add(k.brain, "Минуты: копия", "scoreboard players operation #m " + L + " = #t " + L, "Копия времени");
        k.add(k.brain, "Минуты: остаток", "scoreboard players operation #m " + L + " %= #k " + L, "Остаток от часа");
        for (int h = 0; h < 24; h++) {
            double[] q = NeroMega.K.qa('z', -h * 15.0);
            double[] c = NeroMega.K.rot(q, new double[]{hh[0] / 2, hh[1] / 2, 0});
            k.add(k.anim, "Часовая стрелка: " + h + " ч", "execute if score #h " + L + " matches " + h + " run data merge entity " + k.sel("hour") + " {start_interpolation:0,interpolation_duration:20,transformation:{translation:" + NeroMega.K.arr(new double[]{-c[0], -c[1], 0}) + ",left_rotation:" + NeroMega.K.quat(q) + "}}", "Положение часовой стрелки");
        }
        for (int m = 0; m < 20; m++) {
            double[] q = NeroMega.K.qa('z', -m * 18.0);
            double[] c = NeroMega.K.rot(q, new double[]{mh[0] / 2, mh[1] / 2, 0});
            k.add(k.anim, "Минутная стрелка: " + m * 50 + "/1000", "execute if score #m " + L + " matches " + (m * 50) + ".." + (m * 50 + 49) + " run data merge entity " + k.sel("min") + " {start_interpolation:0,interpolation_duration:20,transformation:{translation:" + NeroMega.K.arr(new double[]{-c[0], -c[1], -0.12}) + ",left_rotation:" + NeroMega.K.quat(q) + "}}", "Положение минутной стрелки");
        }
        for (int f = 0; f < 8; f++) {
            k.add(k.anim, "Маятник: кадр " + (f + 1), "execute if score #ph " + L + " matches " + (f * 4) + " run " + k.swing("pend", NeroMega.K.qa('z', 18 * Math.sin(2 * Math.PI * f / 8)), 4), "Качание маятника");
            k.add(k.anim, "Шестерня 1: кадр " + (f + 1), "execute if score #ph " + L + " matches " + (f * 4) + " run " + k.spin("gear0", new double[]{-0.6, -2.2, 0.65}, new double[]{0.6, 0.6, 0.1}, NeroMega.K.qa('z', f * 45), 4), "Вращение шестерни");
            k.add(k.anim, "Шестерня 2: кадр " + (f + 1), "execute if score #ph " + L + " matches " + (f * 4) + " run " + k.spin("gear1", new double[]{0.7, -2.2, 0.65}, new double[]{0.6, 0.6, 0.1}, NeroMega.K.qa('z', -f * 45), 4), "Вращение шестерни");
        }
        k.add(k.brain, "Фаза часов", "scoreboard players add #ph " + L + " 1", "Счётчик маятника");
        k.add(k.brain, "Фаза часов: круг", "execute if score #ph " + L + " matches 32.. run " + S + "#ph " + L + " 0", "Круг маятника");
        for (int i = 0; i < 3; i++) k.add(k.brain, "Бой часов " + (i + 1), "execute if score #m " + L + " matches " + (i * 8) + ".." + (i * 8 + 1) + " if score #on " + L + " matches 1 as " + k.sel("hour") + " at @s run playsound minecraft:block.bell.use neutral @a ~ ~ ~ 3 " + (0.6 + i * 0.2), "Бой часов в начале часа");
        k.add(k.brain, "Табло времени: день", "execute if score #h " + L + " matches 6..17 run " + k.status("День", "yellow"), "Время суток");
        k.add(k.brain, "Табло времени: ночь", "execute unless score #h " + L + " matches 6..17 run " + k.status("Ночь", "blue"), "Время суток");
        k.add(k.brain, "Подсветка циферблата", "execute unless score #h " + L + " matches 6..17 run setblock {x+24} {y-2} {z+0} minecraft:sea_lantern", "Ночью циферблат светится");
        k.add(k.brain, "Подсветка выключена", "execute if score #h " + L + " matches 6..17 run setblock {x+24} {y-2} {z+0} minecraft:white_concrete", "Днём подсветка выключена");
        k.pultAll(new String[]{"ВКЛ", "ВЫКЛ", "ЗВОН", "ТЕСТ", "ДЕНЬ", "НОЧЬ"}, new String[]{S + "#on " + L + " 1", S + "#on " + L + " 0", "playsound minecraft:block.bell.use master @a {x+24} {y+11} {z+8} 3 0.8", "particle minecraft:note {x+24} {y+11} {z+8} 1 1 1 0 10", "time set day", "time set night"},
                sg("ВКЛ", "включает бой", "часов", "", "ВЫКЛ", "выключает", "бой", "", "ЗВОН", "один удар", "колокола", "", "ТЕСТ", "ноты у", "часов", "", "ДЕНЬ", "ставит", "день", "", "НОЧЬ", "ставит", "ночь", ""),
                new String[]{"ЧАСОВАЯ БАШНЯ", "Стрелки показывают", "игровое время.", "Бой часов каждый час.", "ДЕНЬ/НОЧЬ меняют время", "Маятник и шестерни"});
    }

    private static void crane(NeroMega.K k) {
        String L = k.L, ALLs = k.ALL_SEL;
        k.sx = 24; k.sy = 12; k.sz = 12;
        k.b.part("Мачта крана");
        k.b.box(23, 0, 11, 25, 12, 13, "orange_concrete");
        k.b.box(22, -1, 10, 26, -1, 14, "smooth_stone");
        k.block("cab", "yellow_concrete", new double[]{-0.5, 0, -0.5}, new double[]{1, 0.8, 1}, false);
        k.block("jib", "orange_concrete", new double[]{-0.1, 0.8, -3.5}, new double[]{0.2, 0.2, 11}, false);
        k.block("cw", "gray_concrete", new double[]{-0.4, 0.6, -4.5}, new double[]{0.8, 0.8, 1.0}, false);
        k.block("troll", "black_concrete", new double[]{-0.2, 0.6, 1.0}, new double[]{0.4, 0.2, 0.4}, false);
        k.block("rope", "iron_bars", new double[]{-0.02, -2.0, 1.18}, new double[]{0.04, 2.6, 0.04}, false);
        k.block("hook", "iron_block", new double[]{-0.15, -2.15, 1.05}, new double[]{0.3, 0.15, 0.3}, false);
        k.block("load", "oak_planks", new double[]{-0.35, -3.0, 0.85}, new double[]{0.7, 0.7, 0.7}, false);
        String S1 = S;
        for (int e = 0; e < 8; e++) {
            double z = 1.0 + e * 0.9;
            k.add(k.brain, "Тележка: вылет " + e, "execute if score #ext " + L + " matches " + e + " run " + k.move("troll", new double[]{-0.2, 0.6, z}, 5), "Тележка едет по стреле");
            for (int lo = 0; lo < 4; lo++) {
                double len = 1.0 + lo * 1.8;
                k.add(k.brain, "Крюк: вылет " + e + ", спуск " + lo, "execute if score #ext " + L + " matches " + e + " if score #low " + L + " matches " + lo + " run " + k.move("hook", new double[]{-0.15, 0.6 - len, z + 0.05}, 5), "Положение крюка");
                k.add(k.brain, "Груз: вылет " + e + ", спуск " + lo, "execute if score #ext " + L + " matches " + e + " if score #low " + L + " matches " + lo + " if score #load " + L + " matches 1 run " + k.move("load", new double[]{-0.35, 0.6 - len - 0.8, z - 0.15}, 5), "Груз висит на крюке");
            }
        }
        for (int lo = 0; lo < 4; lo++) k.add(k.brain, "Трос: длина " + lo, "execute if score #low " + L + " matches " + lo + " run " + "data merge entity " + k.sel("rope") + " {start_interpolation:0,interpolation_duration:5,transformation:{scale:[0.04f," + NeroMega.K.fl(0.8 + lo * 1.8) + ",0.04f]}}", "Трос растягивается");
        k.add(k.brain, "Груз: подцепить", "execute if score #low " + L + " matches 3 if score #load " + L + " matches 0 run " + S + "#load " + L + " 1", "На нижней точке крюк подцепляет груз");
        k.add(k.brain, "Груз: сообщение", "execute if score #low " + L + " matches 3 if score #load " + L + " matches 0 run title @a actionbar {\"text\":\"Груз подцеплен\",\"color\":\"green\"}", "Сообщение");
        String[] ed = {"ВЛЕВО", "ВПРАВО", "ВЫЛЕТ+", "ВЫЛЕТ-", "ВНИЗ", "ВВЕРХ"};
        String[] act = {"execute as " + ALLs + " at @s run tp @s ~ ~ ~ ~-15 ~", "execute as " + ALLs + " at @s run tp @s ~ ~ ~ ~15 ~", "execute if score #ext " + L + " matches ..6 run scoreboard players add #ext " + L + " 1", "execute if score #ext " + L + " matches 1.. run scoreboard players remove #ext " + L + " 1", "execute if score #low " + L + " matches ..2 run scoreboard players add #low " + L + " 1", "execute if score #low " + L + " matches 1.. run scoreboard players remove #low " + L + " 1"};
        for (int i = 0; i < 6; i++) {
            k.add(k.pult, ed[i] + ": действие", NeroKit.poll(i) + "execute unless score #p" + i + " " + L + " matches 1 run " + act[i], "Одно действие за нажатие");
            k.add(k.pult, ed[i] + ": отметка", NeroKit.poll(i) + S1 + "#p" + i + " " + L + " 1", "Запоминает нажатие");
            k.add(k.pult, ed[i] + ": сброс", "execute unless block {x+" + (1 + 2 * i) + "} {y+1} {z-1} minecraft:stone_button[powered=true] run " + S1 + "#p" + i + " " + L + " 0", "Кнопка отпущена");
        }
        k.add(k.brain, "Звук тележки", "execute if score #ext " + L + " matches 0..7 if score #t " + L + " matches 0 run playsound minecraft:block.chain.place neutral @a {x+24} {y+12} {z+12} 0.3 1", "Звук цепи");
        k.add(k.brain, "Счётчик звука", "scoreboard players add #t " + L + " 1", "Счётчик");
        k.add(k.brain, "Счётчик звука: круг", "execute if score #t " + L + " matches 40.. run " + S1 + "#t " + L + " 0", "Круг");
        k.add(k.brain, "Табло: груз", "execute if score #load " + L + " matches 1 run " + k.status("Груз на крюке", "green"), "Состояние");
        k.add(k.brain, "Табло: без груза", "execute if score #load " + L + " matches 0 run " + k.status("Крюк пуст", "gray"), "Состояние");
        k.add(k.pult, "Лампа: пульт", "execute if score #load " + L + " matches 1 run setblock {x+13} {y+2} {z+0} minecraft:lime_concrete", "Лампа горит с грузом");
        k.add(k.pult, "Лампа: без груза", "execute unless score #load " + L + " matches 1 run setblock {x+13} {y+2} {z+0} minecraft:red_concrete", "Лампа красная без груза");
        k.signs = sg("ВЛЕВО", "поворот", "стрелы", "", "ВПРАВО", "поворот", "стрелы", "", "ВЫЛЕТ+", "тележка", "дальше", "", "ВЫЛЕТ-", "тележка", "ближе", "", "ВНИЗ", "опускает", "крюк", "и цепляет груз", "ВВЕРХ", "поднимает", "крюк", "");
        k.board = new String[]{"БАШЕННЫЙ КРАН", "ВЛЕВО/ВПРАВО: поворот.", "ВЫЛЕТ: тележка по стреле.", "ВНИЗ: крюк, на дне цепляет", "груз. ВВЕРХ: поднять."};
    }

    /** Механическая рука: кинематика считается при создании (3 сустава и захват), сценарий «взять и переставить». */
    private static void arm(NeroMega.K k) {
        String L = k.L;
        k.sx = 24; k.sy = 1; k.sz = 10;
        k.b.part("Основание руки");
        k.b.box(22, -1, 8, 26, 0, 12, "iron_block");
        k.b.box(20, -1, 6, 28, -1, 14, "smooth_stone");
        k.block("seg1", "orange_concrete", new double[]{-0.2, 0, -0.2}, new double[]{0.4, 2.2, 0.4}, false);
        k.block("seg2", "yellow_concrete", new double[]{-0.15, 0, -0.15}, new double[]{0.3, 1.8, 0.3}, false);
        k.block("fgL", "black_concrete", new double[]{-0.2, 0, -0.05}, new double[]{0.08, 0.5, 0.1}, false);
        k.block("fgR", "black_concrete", new double[]{0.12, 0, -0.05}, new double[]{0.08, 0.5, 0.1}, false);
        k.block("box", "light_blue_concrete", new double[]{2.0, 0, -0.2}, new double[]{0.4, 0.4, 0.4}, false);
        k.b.part("Площадки");
        k.b.box(26, 0, 8, 27, 0, 9, "gray_concrete");
        k.b.box(20, 0, 8, 21, 0, 9, "gray_concrete");
        int N = 20;
        double[][] key = {{0, 0, 0, 1, 0}, {0, 20, 10, 1, 0}, {-40, 70, 25, 1, 0}, {-40, 75, 40, 1, 0}, {-40, 75, 40, 0, 0}, {-40, 40, 20, 0, 1}, {0, 20, 10, 0, 1}, {60, 40, 20, 0, 1}, {60, 75, 40, 0, 1}, {60, 75, 40, 1, 0}, {60, 20, 10, 1, 0}, {0, 0, 0, 1, 0}};
        double L1 = 2.2, L2 = 1.8, w1 = 0.4, w2 = 0.3;
        for (int f = 0; f < N; f++) {
            double pos = f * (key.length - 1) / (double) (N - 1);
            int i0 = (int) Math.min(key.length - 2, Math.floor(pos));
            double fr = pos - i0;
            double[] a = key[i0], b = key[i0 + 1];
            double th0 = a[0] + (b[0] - a[0]) * fr, th1 = a[1] + (b[1] - a[1]) * fr, th2 = a[2] + (b[2] - a[2]) * fr, grip = a[3] + (b[3] - a[3]) * fr, carry = fr < 0.5 ? a[4] : b[4];
            double[] R1 = NeroMega.K.qm(NeroMega.K.qa('y', th0), NeroMega.K.qa('x', th1));
            double[] R2 = NeroMega.K.qm(R1, NeroMega.K.qa('x', th2));
            double[] elbow = NeroMega.K.rot(R1, new double[]{0, L1, 0});
            double[] c1 = NeroMega.K.rot(R1, new double[]{w1 / 2, 0, w1 / 2});
            double[] c2 = NeroMega.K.rot(R2, new double[]{w2 / 2, 0, w2 / 2});
            double[] wrist = {elbow[0] + NeroMega.K.rot(R2, new double[]{0, L2, 0})[0], elbow[1] + NeroMega.K.rot(R2, new double[]{0, L2, 0})[1], elbow[2] + NeroMega.K.rot(R2, new double[]{0, L2, 0})[2]};
            String when = "execute if score #t " + L + " matches " + (f * 6) + ".." + (f * 6 + 5) + " run ";
            k.add(k.anim, "Плечо: кадр " + (f + 1), when + "data merge entity " + k.sel("seg1") + " {start_interpolation:0,interpolation_duration:6,transformation:{translation:" + NeroMega.K.arr(new double[]{-c1[0], -c1[1], -c1[2]}) + ",left_rotation:" + NeroMega.K.quat(R1) + "}}", "Положение плеча");
            k.add(k.anim, "Предплечье: кадр " + (f + 1), when + "data merge entity " + k.sel("seg2") + " {start_interpolation:0,interpolation_duration:6,transformation:{translation:" + NeroMega.K.arr(new double[]{elbow[0] - c2[0], elbow[1] - c2[1], elbow[2] - c2[2]}) + ",left_rotation:" + NeroMega.K.quat(R2) + "}}", "Положение предплечья");
            double g = 0.08 + 0.16 * grip;
            for (int s = 0; s < 2; s++) {
                double[] off = NeroMega.K.rot(R2, new double[]{(s == 0 ? -g - 0.08 : g), 0, -0.05});
                k.add(k.anim, (s == 0 ? "Палец левый" : "Палец правый") + ": кадр " + (f + 1), when + "data merge entity " + k.sel(s == 0 ? "fgL" : "fgR") + " {start_interpolation:0,interpolation_duration:6,transformation:{translation:" + NeroMega.K.arr(new double[]{wrist[0] + off[0], wrist[1] + off[1], wrist[2] + off[2]}) + ",left_rotation:" + NeroMega.K.quat(R2) + "}}", "Положение захвата");
            }
            double[] cb = carry > 0 ? new double[]{wrist[0] - 0.2, wrist[1] + 0.3, wrist[2] - 0.2} : (th0 > 30 ? new double[]{-2.0 - 0.2, 0, -0.2 + 0} : new double[]{2.0, 0, -0.2});
            if (carry == 0 && key[0][0] == 0 && th0 < 30) cb = new double[]{2.0, 0, -0.2};
            k.add(k.anim, "Груз: кадр " + (f + 1), when + k.move("box", cb, 6), "Груз едет в захвате");
        }
        k.add(k.brain, "Таймер сценария", "execute if score #run " + L + " matches 1 run scoreboard players add #t " + L + " 1", "Время сценария");
        k.add(k.brain, "Ускорение x2", "execute if score #run " + L + " matches 1 if score #fast " + L + " matches 1 run scoreboard players add #t " + L + " 1", "Второй тик в режиме x2");
        k.add(k.brain, "Цикл", "execute if score #t " + L + " matches " + (N * 6) + ".. run " + S + "#t " + L + " 0", "Сценарий повторяется");
        k.add(k.brain, "Звук привода", "execute if score #run " + L + " matches 1 if score #t " + L + " matches 0..1 as " + k.sel("seg1") + " at @s run playsound minecraft:block.piston.extend neutral @a ~ ~ ~ 0.6 0.5", "Звук привода");
        k.add(k.brain, "Искры сварки", "execute if score #run " + L + " matches 1 as " + k.sel("seg2") + " at @s run particle minecraft:electric_spark ~ ~ ~ 0.1 0.1 0.1 0.1 1", "Искры у сустава");
        k.add(k.brain, "Табло: работа", "execute if score #run " + L + " matches 1 run " + k.status("Переносит груз", "green"), "Состояние");
        k.add(k.brain, "Табло: стоп", "execute unless score #run " + L + " matches 1 run " + k.status("Пауза", "gray"), "Состояние");
        k.pultAll(new String[]{"ПУСК", "ПАУЗА", "СБРОС", "x2", "x1", "ТЕСТ"}, new String[]{S + "#run " + L + " 1", S + "#run " + L + " 0", S + "#t " + L + " 0", S + "#fast " + L + " 1", S + "#fast " + L + " 0", "particle minecraft:electric_spark {x+24} {y+3} {z+10} 0.5 0.5 0.5 0.2 20"},
                sg("ПУСК", "рука берёт", "и переносит", "груз", "ПАУЗА", "стоп на", "месте", "", "СБРОС", "в начало", "сценария", "", "x2", "вдвое", "быстрее", "", "x1", "обычная", "скорость", "", "ТЕСТ", "искры", "у руки", ""),
                new String[]{"МЕХАНИЧЕСКАЯ РУКА", "Три сустава и захват.", "Берёт груз слева и ставит", "справа, возвращается.", "ПУСК, ПАУЗА, СБРОС", "Кинематика посчитана заранее"});
    }

    private static void disco(NeroMega.K k) {
        String L = k.L;
        k.sx = 24; k.sy = 0; k.sz = 10;
        k.b.part("Танцпол");
        k.b.box(20, -1, 6, 28, -1, 14, "black_concrete");
        k.b.box(19, 0, 5, 19, 5, 5, "iron_block"); k.b.box(29, 0, 5, 29, 5, 5, "iron_block"); k.b.box(19, 0, 15, 19, 5, 15, "iron_block"); k.b.box(29, 0, 15, 29, 5, 15, "iron_block");
        String[] col = {"red", "orange", "yellow", "lime", "cyan", "light_blue", "blue", "purple", "magenta", "pink", "white", "green"};
        for (int f = 0; f < 24; f++) {
            String w = "execute if score #ph " + L + " matches " + (f * 2) + ".." + (f * 2 + 1) + " run ";
            k.add(k.anim, "Танцпол A: кадр " + (f + 1), w + "fill {x+20} {y-1} {z+6} {x+24} {y-1} {z+10} minecraft:" + col[f % 12] + "_concrete", "Левая часть пола");
            k.add(k.anim, "Танцпол B: кадр " + (f + 1), w + "fill {x+24} {y-1} {z+10} {x+28} {y-1} {z+14} minecraft:" + col[(f + 6) % 12] + "_concrete", "Правая часть пола");
            double a = Math.toRadians(f * 15);
            k.add(k.anim, "Лазер: кадр " + (f + 1), w + "particle minecraft:dust 1 0.2 0.9 1.3 {x+24} {y+5} {z+10} " + NeroMega.K.f(6 * Math.cos(a)) + " -3 " + NeroMega.K.f(6 * Math.sin(a)) + " 0 3", "Луч лазера по кругу");
        }
        for (int i = 0; i < 4; i++) k.add(k.brain, "Прожектор " + (i + 1), "execute if score #on " + L + " matches 1 run setblock {x+" + (i % 2 == 0 ? 19 : 29) + "} {y+6} {z+" + (i < 2 ? 5 : 15) + "} minecraft:" + col[(i * 3) % 12] + "_stained_glass", "Прожектор на стойке");
        phase(k, L, 48);
        k.add(k.brain, "Дым", "execute if score #on " + L + " matches 1 run particle minecraft:campfire_cosy_smoke {x+24} {y+0} {z+10} 3 0.2 3 0.01 2", "Дым-машина");
        for (int i = 0; i < 8; i++) k.add(k.brain, "Бит " + (i + 1), "execute if score #on " + L + " matches 1 if score #ph " + L + " matches " + (i * 6) + " run playsound minecraft:block.note_block." + (i % 2 == 0 ? "basedrum" : "hat") + " master @a {x+24} {y+1} {z+10} 2 " + (i % 4 == 0 ? "0.7" : "1.2"), "Ритм");
        speedPult(k, L, new String[]{"ВКЛ", "ВЫКЛ", S + "#spd " + L + " 2", S + "#spd " + L + " 0", "включает", "дискотеку", "выключает", "дискотеку", "Танцпол меняет цвета,", "лазеры и ритм."});
    }

    private static void fountain(NeroMega.K k) {
        String L = k.L;
        k.sx = 24; k.sy = 0; k.sz = 10;
        k.b.part("Чаша фонтана");
        k.b.box(18, 0, 4, 30, 0, 16, "stone_bricks"); k.b.box(19, 1, 5, 29, 1, 15, "water");
        k.b.box(23, 1, 9, 25, 3, 11, "quartz_pillar");
        int[][] jets = {{0, 0}, {-4, 0}, {4, 0}, {0, -4}, {0, 4}};
        for (int f = 0; f < 16; f++) for (int j = 0; j < jets.length; j++) {
            double h = j == 0 ? 3.5 + 2.5 * Math.sin(Math.PI * f / 8) : 1.5 + 1.3 * Math.sin(Math.PI * (f + j * 3) / 8);
            k.add(k.anim, "Струя " + (j + 1) + ": кадр " + (f + 1), "execute if score #on " + L + " matches 1 if score #ph " + L + " matches " + (f * 3) + ".." + (f * 3 + 2) + " run particle minecraft:splash {x+" + (24 + jets[j][0]) + "} {y+2} {z+" + (10 + jets[j][1]) + "} 0.1 " + NeroMega.K.f(h) + " 0.1 0.5 30", "Струя воды");
        }
        phase(k, L, 48);
        for (int i = 0; i < 4; i++) k.add(k.brain, "Подсветка " + (i + 1), "execute if score #on " + L + " matches 1 run setblock {x+" + (i % 2 == 0 ? 18 : 30) + "} {y+1} {z+" + (i < 2 ? 4 : 16) + "} minecraft:sea_lantern", "Подводная подсветка");
        k.add(k.brain, "Брызги", "execute if score #on " + L + " matches 1 run particle minecraft:falling_water {x+24} {y+3} {z+10} 4 0.5 4 0 12", "Капли");
        k.add(k.brain, "Шум воды", "execute if score #on " + L + " matches 1 if score #ph " + L + " matches 0 run playsound minecraft:block.water.ambient master @a {x+24} {y+1} {z+10} 1 1", "Шум воды");
        speedPult(k, L, new String[]{"ВКЛ", "ВЫКЛ", S + "#spd " + L + " 2", S + "#spd " + L + " 0", "включает", "фонтан", "выключает", "фонтан", "Пять струй качаются", "по программе из 16 кадров."});
    }

    private static void fireworks(NeroMega.K k) {
        String L = k.L;
        k.sx = 24; k.sy = 0; k.sz = 10;
        k.b.part("Пусковые площадки");
        for (int[] p : new int[][]{{20, 6}, {28, 6}, {20, 14}, {28, 14}}) { k.b.box(p[0] - 1, 0, p[1] - 1, p[0] + 1, 0, p[1] + 1, "iron_block"); k.b.box(p[0], 1, p[1], p[0], 2, p[1], "iron_bars"); }
        int[] colors = {16711680, 16776960, 65280, 255, 16711935, 65535, 16744192, 11141375};
        int[][] pads = {{20, 6}, {28, 6}, {20, 14}, {28, 14}};
        for (int i = 0; i < 40; i++) {
            int[] p = pads[i % 4];
            int c1 = colors[i % 8], c2 = colors[(i * 3 + 1) % 8], type = i % 5;
            k.add(k.anim, "Залп " + (i + 1), "execute if score #run " + L + " matches 1 if score #t " + L + " matches " + (20 + i * 12) + ".." + (21 + i * 12) + " run summon minecraft:firework_rocket {x+" + p[0] + "} {y+2} {z+" + p[1] + "} {LifeTime:" + (22 + (i % 3) * 5) + ",FireworksItem:{id:\"minecraft:firework_rocket\",Count:1,tag:{Fireworks:{Flight:" + (1 + i % 3) + ",Explosions:[{Type:" + (type % 5) + ",Colors:[I;" + c1 + "," + c2 + "],FadeColors:[I;16777215],Trail:1b}]}}}}", "Фейерверк цвета " + c1);
        }
        for (int i = 0; i < 20; i++) k.add(k.anim, "Звук залпа " + (i + 1), "execute if score #run " + L + " matches 1 if score #t " + L + " matches " + (20 + i * 24) + " run playsound minecraft:entity.firework_rocket.launch master @a {x+24} {y+1} {z+10} 2 " + (0.8 + (i % 4) * 0.15), "Звук пуска");
        k.add(k.brain, "Таймер программы", "execute if score #run " + L + " matches 1 run scoreboard players add #t " + L + " 1", "Время программы");
        k.add(k.brain, "Конец программы", "execute if score #t " + L + " matches 520.. run " + S + "#run " + L + " 0", "Программа завершена");
        k.add(k.brain, "Сообщение в конце", "execute if score #t " + L + " matches 520 run title @a actionbar {\"text\":\"Салют окончен\",\"color\":\"gold\"}", "Сообщение");
        k.add(k.brain, "Дым площадок", "execute if score #run " + L + " matches 1 run particle minecraft:campfire_cosy_smoke {x+24} {y+1} {z+10} 5 0.1 3 0.01 4", "Дым");
        k.pultAll(new String[]{"ПУСК", "СТОП", "СБРОС", "ТЕСТ", "ВКЛ", "ВЫКЛ"}, new String[]{S + "#run " + L + " 1", S + "#run " + L + " 0", S + "#t " + L + " 0", "summon minecraft:firework_rocket {x+24} {y+2} {z+10} {LifeTime:20,FireworksItem:{id:\"minecraft:firework_rocket\",Count:1,tag:{Fireworks:{Flight:1,Explosions:[{Type:1,Colors:[I;16711680]}]}}}}", S + "#on " + L + " 1", S + "#on " + L + " 0"},
                sg("ПУСК", "программа", "из 40 залпов", "", "СТОП", "пауза", "", "", "СБРОС", "в начало", "программы", "", "ТЕСТ", "один залп", "красный", "", "ВКЛ", "питание", "", "", "ВЫКЛ", "питание", "", ""),
                new String[]{"САЛЮТ-СТАНЦИЯ", "40 залпов с четырёх", "площадок: цвета, типы,", "траектории.", "ПУСК - начать программу", "ТЕСТ - один залп"});
        k.add(k.pult, "СБРОС: остановка", NeroKit.poll(2) + S + "#run " + L + " 0", "СБРОС также останавливает программу");
    }

    private static void aurora(NeroMega.K k) {
        String L = k.L;
        k.sx = 24; k.sy = 14; k.sz = 10;
        k.b.part("Опоры сияния");
        k.b.box(15, 0, 4, 15, 13, 4, "iron_block"); k.b.box(33, 0, 4, 33, 13, 4, "iron_block"); k.b.box(15, 0, 16, 15, 13, 16, "iron_block"); k.b.box(33, 0, 16, 33, 13, 16, "iron_block");
        double[][] rgb = {{0.2, 1.0, 0.5}, {0.4, 0.6, 1.0}, {0.9, 0.3, 1.0}};
        for (int f = 0; f < 24; f++) for (int r = 0; r < 3; r++) {
            double x = 10 * Math.cos(Math.toRadians(f * 15 + r * 120)), z = 6 * Math.sin(Math.toRadians(f * 15 + r * 120));
            k.add(k.anim, "Лента " + (r + 1) + ": кадр " + (f + 1), "execute if score #on " + L + " matches 1 if score #ph " + L + " matches " + (f * 2) + ".." + (f * 2 + 1) + " run particle minecraft:dust " + rgb[r][0] + " " + rgb[r][1] + " " + rgb[r][2] + " 3 {x+24} {y+10} {z+10} " + NeroMega.K.f(x) + " 4 " + NeroMega.K.f(z) + " 0 60", "Световая лента " + (r + 1));
        }
        phase(k, L, 48);
        for (int i = 0; i < 10; i++) k.add(k.brain, "Звезда " + (i + 1), "execute if score #on " + L + " matches 1 if score #ph " + L + " matches " + (i * 5) + " run particle minecraft:glow {x+" + (14 + (i * 7) % 20) + "} {y+" + (12 + i % 5) + "} {z+" + (4 + (i * 5) % 12) + "} 0.2 0.2 0.2 0 3", "Мерцающая звезда");
        k.add(k.brain, "Фоновый звук", "execute if score #on " + L + " matches 1 if score #ph " + L + " matches 0 run playsound minecraft:block.amethyst_block.resonate master @a {x+24} {y+3} {z+10} 1.5 0.5", "Тихий звук сияния");
        speedPult(k, L, new String[]{"ВКЛ", "ВЫКЛ", S + "#spd " + L + " 2", S + "#spd " + L + " 0", "включает", "сияние", "выключает", "сияние", "Три цветные ленты света", "плывут над площадкой."});
    }

    private NeroMega2() { }
}
