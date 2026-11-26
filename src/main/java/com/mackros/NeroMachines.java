package com.mackros;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Пять больших механизмов-строителей. Каждый стоит рядом с пультом (кнопки ПУСК, ПАУЗА, СТОП, СНОС, x2, x1 и таблички «что и как»)
 * и строит рядом сооружение по сценарию из командных блоков (по ~100 командных блоков на механизм): этап за этапом, с частицами и табло.
 */
final class NeroMachines {
    static String detect(String t) {
        if (NeroFree.has(t, "автобашн|строител\\w*\\s+башн|башн\\w*[- ]?(автомат|строител)")) return "m_tower";
        if (NeroFree.has(t, "мост[- ]?раскладушк|выдвижн\\w*\\s+мост|строител\\w*\\s+мост|автомост")) return "m_bridge";
        if (NeroFree.has(t, "дом[- ]?принтер|автодом|строител\\w*\\s+дом|автосборк\\w*\\s+дом|дом[- ]автомат")) return "m_house";
        if (NeroFree.has(t, "крепост\\w*[- ]?(автосборк|автомат|строител)|строител\\w*\\s+крепост|автокрепост")) return "m_fort";
        if (NeroFree.has(t, "пирамид\\w*[- ]?(генератор|автомат|строител)|строител\\w*\\s+пирамид|автопирамид|зиккурат")) return "m_pyramid";
        return null;
    }

    static String name(String k) {
        if (NeroMega.find(k).key().equals(k)) return NeroMega.name(k);
        return switch (k) { case "m_tower" -> "автобашня"; case "m_bridge" -> "мост-раскладушка"; case "m_house" -> "дом-принтер"; case "m_fort" -> "крепость-автосборка"; default -> "пирамида-генератор"; };
    }

    static String works(String k) {
        if (NeroMega.find(k).key().equals(k)) return NeroMega.works(k);
        return "пульт с кнопками ПУСК, ПАУЗА, СТОП, СНОС, x2, x1 и табличками; справа от пульта механизм сам строит сооружение этап за этапом (командные блоки по сценарию, частицы, табло хода). Кнопка СНОС убирает постройку. Если нажать ПУСК после окончания, сначала нажми СТОП (сброс времени)";
    }

    static JsonObject bp(String key, String t) {
        NeroArchetypes.B b = new NeroArchetypes.B();
        M m = new M(b);
        switch (key) {
            case "m_tower" -> tower(m);
            case "m_bridge" -> bridge(m);
            case "m_house" -> house(m);
            case "m_fort" -> fort(m);
            case "m_lighthouse" -> lighthouse(m);
            case "m_maze" -> maze(m);
            case "m_village" -> village(m);
            case "m_stadium" -> stadium(m);
            default -> pyramid(m);
        }
        return b.done();
    }

    private static final class M {
        final NeroArchetypes.B b;
        final String L = "{id}_m";
        final List<String[]> seq = new ArrayList<>();
        int tick = 20;

        M(NeroArchetypes.B b) { this.b = b; }

        void cmd(String label, String c) {
            seq.add(new String[]{label, "execute if score #run " + L + " matches 1 if score #t " + L + " matches " + tick + ".." + (tick + 1) + " run " + c, "Выполняется на тике сценария " + tick});
        }

        void wait(int dt) { tick += dt; }

        String show(String text) { return "data modify entity @e[type=minecraft:text_display,tag=st_{id},limit=1] text set value '{\"text\":\"" + NeroKit.esc(text) + "\",\"color\":\"aqua\"}'"; }

        void display(String text) { cmd("Табло: " + text, show(text)); }

        void finish(String[] board, String[][] signs, String[] clear, String title) {
            int end = tick + 20;
            NeroKit.console(b, 6);
            b.setup("scoreboard objectives add " + L + " dummy", "scoreboard players set #run " + L + " 0", "scoreboard players set #t " + L + " 0",
                    "scoreboard players set #fast " + L + " 0", "scoreboard players set #clr " + L + " 0",
                    "summon minecraft:text_display {x+7} {y+5} {z+0} {text:'{\"text\":\"Готов к работе\",\"color\":\"green\"}',billboard:\"center\",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f]},Tags:[\"{tag}\",\"st_{id}\"]}");
            for (int i = 0; i < signs.length; i++) b.setup(NeroKit.sign(1 + 2 * i, 0, -1, signs[i][0], signs[i][1], signs[i][2], signs[i][3]));
            NeroKit.board(b, 7, 8, 0, board);
            String S = "scoreboard players set ";
            List<String[]> c = new ArrayList<>();
            c.add(new String[]{"Кнопка ПУСК", NeroKit.poll(0) + S + "#run " + L + " 1", "Нажатие ПУСК запускает сценарий"});
            c.add(new String[]{"Кнопка ПАУЗА", NeroKit.poll(1) + S + "#run " + L + " 0", "Нажатие ПАУЗА останавливает таймер, сценарий продолжится с места"});
            c.add(new String[]{"Кнопка СТОП: остановка", NeroKit.poll(2) + S + "#run " + L + " 0", "СТОП останавливает работу"});
            c.add(new String[]{"Кнопка СТОП: сброс времени", NeroKit.poll(2) + S + "#t " + L + " 0", "СТОП возвращает сценарий в начало"});
            c.add(new String[]{"Кнопка СТОП: табло", NeroKit.poll(2) + show("Остановлено"), "СТОП пишет состояние на табло"});
            c.add(new String[]{"Кнопка СНОС: остановка", NeroKit.poll(3) + S + "#run " + L + " 0", "СНОС сначала останавливает работу"});
            c.add(new String[]{"Кнопка СНОС: сброс времени", NeroKit.poll(3) + S + "#t " + L + " 0", "СНОС возвращает сценарий в начало"});
            c.add(new String[]{"Кнопка СНОС: запуск уборки", NeroKit.poll(3) + S + "#clr " + L + " 1", "СНОС включает уборку постройки"});
            c.add(new String[]{"Кнопка x2", NeroKit.poll(4) + S + "#fast " + L + " 1", "Ускоряет сценарий вдвое"});
            c.add(new String[]{"Кнопка x1", NeroKit.poll(5) + S + "#fast " + L + " 0", "Обычная скорость сценария"});
            c.add(new String[]{"Лампа: работает", "execute if score #run " + L + " matches 1 run setblock {x+13} {y+2} {z+0} minecraft:lime_concrete", "Зелёная лампа на пульте, пока механизм работает"});
            c.add(new String[]{"Лампа: стоит", "execute unless score #run " + L + " matches 1 run setblock {x+13} {y+2} {z+0} minecraft:red_concrete", "Красная лампа на пульте, когда механизм стоит"});
            b.chain("Пульт управления: " + title, c.toArray(new String[0][]));
            List<String[]> tm = new ArrayList<>();
            tm.add(new String[]{"Таймер: тик", "execute if score #run " + L + " matches 1 run scoreboard players add #t " + L + " 1", "Считает время работы"});
            tm.add(new String[]{"Таймер: ускорение x2", "execute if score #run " + L + " matches 1 if score #fast " + L + " matches 1 run scoreboard players add #t " + L + " 1", "Второй тик за такт при режиме x2"});
            tm.add(new String[]{"Финиш: сообщение", "execute if score #run " + L + " matches 1 if score #t " + L + " matches " + end + ".. run tellraw @a {\"text\":\"" + NeroKit.esc(title) + ": строительство завершено\",\"color\":\"green\"}", "Сообщает о завершении"});
            tm.add(new String[]{"Финиш: табло", "execute if score #run " + L + " matches 1 if score #t " + L + " matches " + end + ".. run " + show("ГОТОВО"), "Табло показывает ГОТОВО"});
            tm.add(new String[]{"Финиш: остановка", "execute if score #run " + L + " matches 1 if score #t " + L + " matches " + end + ".. run " + S + "#run " + L + " 0", "Останавливает механизм после окончания"});
            b.chain("Таймер: " + title, tm.toArray(new String[0][]));
            b.chain("Сценарий строительства: " + title, seq.toArray(new String[0][]));
            List<String[]> cl = new ArrayList<>();
            for (int i = 0; i < clear.length; i++) cl.add(new String[]{"Снос: часть " + (i + 1), "execute if score #clr " + L + " matches 1 run " + clear[i], "Убирает часть постройки"});
            cl.add(new String[]{"Снос: частицы", "execute if score #clr " + L + " matches 1 run particle minecraft:cloud {x+22} {y+3} {z+5} 8 3 8 0 120", "Облако пыли при сносе"});
            cl.add(new String[]{"Снос: табло", "execute if score #clr " + L + " matches 1 run " + show("Снесено"), "Табло показывает, что постройка убрана"});
            cl.add(new String[]{"Снос: готово", "execute if score #clr " + L + " matches 1 run " + S + "#clr " + L + " 0", "Выключает режим сноса"});
            b.chain("Снос: " + title, cl.toArray(new String[0][]));
            b.help = title + ": пульт слева (ПУСК, ПАУЗА, СТОП, СНОС, x2, x1, таблички под кнопками); сооружение строится справа от пульта. Командных блоков в механизме: " + (c.size() + tm.size() + seq.size() + cl.size()) + ".";
        }
    }

    private static String[][] signs(String start) {
        return new String[][]{{"ПУСК", start, "по сценарию", "этап за этапом"}, {"ПАУЗА", "стоп на месте", "ПУСК - дальше", ""}, {"СТОП", "в начало", "сброс времени", ""},
                {"СНОС", "убрать всё", "что построено", ""}, {"СКОРОСТЬ x2", "строит вдвое", "быстрее", ""}, {"СКОРОСТЬ x1", "обычная", "скорость", ""}};
    }

    // ------------------------------------------------------------------ автобашня
    private static void tower(M m) {
        final int F = 11;
        for (int f = 0; f < F; f++) {
            int y0 = f * 5;
            String p = "Этаж " + (f + 1) + ": ";
            m.cmd(p + "каркас", "fill {x+16} {y+" + y0 + "} {z+0} {x+24} {y+" + (y0 + 5) + "} {z+8} minecraft:stone_bricks hollow");
            m.cmd(p + "окна спереди", "fill {x+18} {y+" + (y0 + 2) + "} {z+0} {x+22} {y+" + (y0 + 3) + "} {z+0} minecraft:light_blue_stained_glass");
            m.cmd(p + "окна сзади", "fill {x+18} {y+" + (y0 + 2) + "} {z+8} {x+22} {y+" + (y0 + 3) + "} {z+8} minecraft:light_blue_stained_glass");
            m.cmd(p + "окна слева", "fill {x+16} {y+" + (y0 + 2) + "} {z+2} {x+16} {y+" + (y0 + 3) + "} {z+6} minecraft:light_blue_stained_glass");
            m.cmd(p + "окна справа", "fill {x+24} {y+" + (y0 + 2) + "} {z+2} {x+24} {y+" + (y0 + 3) + "} {z+6} minecraft:light_blue_stained_glass");
            m.cmd(p + "освещение", "setblock {x+20} {y+" + (y0 + 4) + "} {z+4} minecraft:sea_lantern");
            m.cmd(p + "искры сборки", "particle minecraft:happy_villager {x+20} {y+" + (y0 + 3) + "} {z+4} 4 2 4 0 30");
            m.display("Этаж " + (f + 1) + " из " + F);
            if (f == 0) {
                m.cmd("Дверь: проём", "fill {x+19} {y+1} {z+0} {x+21} {y+3} {z+0} minecraft:air");
                m.cmd("Крыльцо", "fill {x+18} {y-1} {z-3} {x+22} {y-1} {z-1} minecraft:smooth_stone");
            }
            m.wait(20);
        }
        m.cmd("Крыша", "fill {x+15} {y+55} {z-1} {x+25} {y+55} {z+9} minecraft:stone_brick_slab");
        m.cmd("Шпиль", "fill {x+20} {y+56} {z+4} {x+20} {y+62} {z+4} minecraft:lightning_rod");
        m.cmd("Маяк на шпиле", "setblock {x+20} {y+63} {z+4} minecraft:sea_lantern");
        m.cmd("Салют", "particle minecraft:firework {x+20} {y+64} {z+4} 2 2 2 0.1 80");
        m.cmd("Звук завершения", "playsound minecraft:entity.player.levelup master @a {x+20} {y+3} {z+4} 1 1");
        m.display("Башня готова");
        m.finish(new String[]{"АВТОБАШНЯ", "Строит башню справа:", "11 этажей по 5 блоков", "с окнами, светом и шпилем.", "ПУСК - начать, ПАУЗА - пауза", "СТОП - сброс, СНОС - убрать"}, signs("строит башню"),
                new String[]{"fill {x+14} {y+0} {z-4} {x+26} {y+64} {z+10} minecraft:air"}, "Автобашня");
    }

    // ------------------------------------------------------------------ мост-раскладушка
    private static void bridge(M m) {
        final int S = 14;
        for (int s = 0; s < S; s++) {
            int X = 16 + 2 * s;
            String p = "Секция " + (s + 1) + ": ";
            m.cmd(p + "настил", "fill {x+" + X + "} {y-1} {z+0} {x+" + (X + 1) + "} {y-1} {z+4} minecraft:stone_bricks");
            m.cmd(p + "разметка", "fill {x+" + X + "} {y-1} {z+2} {x+" + (X + 1) + "} {y-1} {z+2} minecraft:smooth_stone");
            m.cmd(p + "перила слева", "fill {x+" + X + "} {y+0} {z+0} {x+" + (X + 1) + "} {y+0} {z+0} minecraft:stone_brick_wall");
            m.cmd(p + "перила справа", "fill {x+" + X + "} {y+0} {z+4} {x+" + (X + 1) + "} {y+0} {z+4} minecraft:stone_brick_wall");
            m.cmd(p + "фонарь", "setblock {x+" + X + "} {y+1} {z+0} minecraft:lantern");
            m.cmd(p + "опора", "fill {x+" + X + "} {y-8} {z+2} {x+" + X + "} {y-2} {z+2} minecraft:stone_bricks");
            m.display("Секция " + (s + 1) + " из " + S);
            m.wait(15);
        }
        m.cmd("Арка въезда: левая стойка", "fill {x+15} {y+0} {z+0} {x+15} {y+4} {z+0} minecraft:stone_bricks");
        m.cmd("Арка въезда: правая стойка", "fill {x+15} {y+0} {z+4} {x+15} {y+4} {z+4} minecraft:stone_bricks");
        m.cmd("Арка въезда: перемычка", "fill {x+15} {y+5} {z+0} {x+15} {y+5} {z+4} minecraft:stone_bricks");
        m.cmd("Салют", "particle minecraft:firework {x+30} {y+3} {z+2} 6 2 2 0.1 80");
        m.display("Мост готов");
        m.finish(new String[]{"МОСТ-РАСКЛАДУШКА", "Выдвигает мост вперёд:", "14 секций по 2 блока,", "перила, фонари и опоры.", "ПУСК - строить, СНОС - сложить"}, signs("выдвигает мост"),
                new String[]{"fill {x+15} {y+0} {z+0} {x+46} {y+5} {z+4} minecraft:air", "fill {x+16} {y-1} {z+0} {x+45} {y-1} {z+4} minecraft:grass_block", "fill {x+16} {y-8} {z+2} {x+45} {y-2} {z+2} minecraft:dirt replace minecraft:stone_bricks"}, "Мост-раскладушка");
    }

    // ------------------------------------------------------------------ дом-принтер
    private static void house(M m) {
        m.cmd("Фундамент: плита", "fill {x+16} {y+0} {z+0} {x+28} {y+0} {z+10} minecraft:stone_bricks");
        m.cmd("Фундамент: цоколь", "fill {x+15} {y-1} {z-1} {x+29} {y-1} {z+11} minecraft:cobblestone");
        m.display("Фундамент готов");
        m.wait(20);
        for (int l = 1; l <= 6; l++) {
            String p = "Стены, слой " + l + ": ";
            m.cmd(p + "наружная стена", "fill {x+16} {y+" + l + "} {z+0} {x+28} {y+" + l + "} {z+10} minecraft:white_terracotta");
            m.cmd(p + "внутренний объём", "fill {x+17} {y+" + l + "} {z+1} {x+27} {y+" + l + "} {z+9} minecraft:air");
            m.cmd(p + "искры", "particle minecraft:end_rod {x+22} {y+" + l + "} {z+5} 6 0 5 0 40");
            m.display("Стены: слой " + l + " из 6");
            m.wait(15);
        }
        for (int i = 0; i < 3; i++) {
            m.cmd("Окно спереди " + (i + 1), "fill {x+" + (18 + i * 3) + "} {y+3} {z+0} {x+" + (19 + i * 3) + "} {y+4} {z+0} minecraft:glass_pane");
            m.cmd("Окно сзади " + (i + 1), "fill {x+" + (18 + i * 3) + "} {y+3} {z+10} {x+" + (19 + i * 3) + "} {y+4} {z+10} minecraft:glass_pane");
        }
        m.cmd("Окно слева", "fill {x+16} {y+3} {z+4} {x+16} {y+4} {z+6} minecraft:glass_pane");
        m.cmd("Окно справа", "fill {x+28} {y+3} {z+4} {x+28} {y+4} {z+6} minecraft:glass_pane");
        m.cmd("Дверь: проём", "fill {x+21} {y+1} {z+0} {x+23} {y+3} {z+0} minecraft:air");
        m.cmd("Дверь: ступени", "fill {x+20} {y+0} {z-2} {x+24} {y+0} {z-1} minecraft:stone_brick_slab");
        m.display("Окна и дверь готовы");
        m.wait(15);
        for (int t = 0; t < 7; t++) {
            m.cmd("Крыша, ярус " + (t + 1), "fill {x+" + (16 + t) + "} {y+" + (7 + t) + "} {z+" + t + "} {x+" + (28 - t) + "} {y+" + (7 + t) + "} {z+" + (10 - t) + "} minecraft:dark_oak_planks");
            m.display("Крыша: ярус " + (t + 1) + " из 7");
            m.wait(12);
        }
        m.cmd("Труба: основание", "fill {x+26} {y+7} {z+8} {x+26} {y+10} {z+8} minecraft:bricks");
        m.cmd("Труба: оголовок", "setblock {x+26} {y+11} {z+8} minecraft:campfire");
        m.cmd("Труба: дым", "particle minecraft:campfire_cosy_smoke {x+26} {y+12} {z+8} 0 1 0 0.02 10");
        for (int i = 0; i < 4; i++) m.cmd("Лампа " + (i + 1), "setblock {x+" + (19 + (i % 2) * 6) + "} {y+6} {z+" + (3 + (i / 2) * 4) + "} minecraft:lantern[hanging=true]");
        m.cmd("Стол: ножка", "setblock {x+22} {y+1} {z+5} minecraft:oak_fence");
        m.cmd("Стол: столешница", "setblock {x+22} {y+2} {z+5} minecraft:oak_pressure_plate");
        m.cmd("Книжные полки: ряд 1", "fill {x+17} {y+1} {z+9} {x+19} {y+2} {z+9} minecraft:bookshelf");
        m.cmd("Книжные полки: ряд 2", "fill {x+25} {y+1} {z+9} {x+27} {y+2} {z+9} minecraft:bookshelf");
        m.cmd("Ковёр", "fill {x+19} {y+1} {z+3} {x+25} {y+1} {z+7} minecraft:red_carpet");
        m.cmd("Факел у двери слева", "setblock {x+20} {y+2} {z-1} minecraft:wall_torch[facing=north]");
        m.cmd("Факел у двери справа", "setblock {x+24} {y+2} {z-1} minecraft:wall_torch[facing=north]");
        m.cmd("Дорожка", "fill {x+21} {y-1} {z-6} {x+23} {y-1} {z-3} minecraft:gravel");
        m.cmd("Забор: перед домом", "fill {x+15} {y+0} {z-7} {x+29} {y+0} {z-7} minecraft:oak_fence");
        m.cmd("Забор: слева", "fill {x+15} {y+0} {z-7} {x+15} {y+0} {z-2} minecraft:oak_fence");
        m.cmd("Забор: справа", "fill {x+29} {y+0} {z-7} {x+29} {y+0} {z-2} minecraft:oak_fence");
        m.cmd("Калитка", "setblock {x+22} {y+0} {z-7} minecraft:oak_fence_gate[facing=north]");
        m.cmd("Клумба слева", "fill {x+17} {y+0} {z-4} {x+19} {y+0} {z-3} minecraft:poppy");
        m.cmd("Клумба справа", "fill {x+25} {y+0} {z-4} {x+27} {y+0} {z-3} minecraft:dandelion");
        m.cmd("Звук завершения", "playsound minecraft:entity.player.levelup master @a {x+22} {y+3} {z+5} 1 1");
        m.display("Дом готов");
        m.finish(new String[]{"ДОМ-ПРИНТЕР", "Печатает дом справа:", "фундамент, стены по слоям,", "окна, крыша, интерьер, сад.", "ПУСК - печать, СНОС - убрать"}, signs("печатает дом"),
                new String[]{"fill {x+14} {y+0} {z-8} {x+30} {y+12} {z+12} minecraft:air", "fill {x+15} {y-1} {z-1} {x+29} {y-1} {z+11} minecraft:grass_block"}, "Дом-принтер");
    }

    // ------------------------------------------------------------------ крепость-автосборка
    private static void fort(M m) {
        for (int l = 1; l <= 7; l++) {
            String p = "Стена, слой " + l + ": ";
            m.cmd(p + "кольцо", "fill {x+16} {y+" + l + "} {z+0} {x+40} {y+" + l + "} {z+24} minecraft:stone_bricks");
            m.cmd(p + "двор", "fill {x+17} {y+" + l + "} {z+1} {x+39} {y+" + l + "} {z+23} minecraft:air");
            m.cmd(p + "искры", "particle minecraft:cloud {x+28} {y+" + l + "} {z+12} 12 0 12 0 60");
            m.display("Стены: слой " + l + " из 7");
            m.wait(15);
        }
        for (int i = 0; i < 6; i++) {
            m.cmd("Зубец спереди " + (i + 1), "setblock {x+" + (17 + i * 4) + "} {y+8} {z+0} minecraft:stone_brick_wall");
            m.cmd("Зубец сзади " + (i + 1), "setblock {x+" + (17 + i * 4) + "} {y+8} {z+24} minecraft:stone_brick_wall");
            m.cmd("Зубец слева " + (i + 1), "setblock {x+16} {y+8} {z+" + (2 + i * 4) + "} minecraft:stone_brick_wall");
            m.cmd("Зубец справа " + (i + 1), "setblock {x+40} {y+8} {z+" + (2 + i * 4) + "} minecraft:stone_brick_wall");
        }
        m.display("Зубцы готовы");
        int[][] corner = {{14, -2}, {38, -2}, {14, 22}, {38, 22}};
        for (int i = 0; i < 4; i++) {
            m.cmd("Башня " + (i + 1) + ": каркас", "fill {x+" + corner[i][0] + "} {y+0} {z+" + corner[i][1] + "} {x+" + (corner[i][0] + 4) + "} {y+11} {z+" + (corner[i][1] + 4) + "} minecraft:stone_bricks hollow");
            m.cmd("Башня " + (i + 1) + ": крыша", "fill {x+" + (corner[i][0] - 1) + "} {y+12} {z+" + (corner[i][1] - 1) + "} {x+" + (corner[i][0] + 5) + "} {y+12} {z+" + (corner[i][1] + 5) + "} minecraft:stone_brick_slab");
            m.cmd("Башня " + (i + 1) + ": бойница", "fill {x+" + (corner[i][0] + 2) + "} {y+7} {z+" + corner[i][1] + "} {x+" + (corner[i][0] + 2) + "} {y+8} {z+" + corner[i][1] + "} minecraft:air");
            m.cmd("Башня " + (i + 1) + ": факел", "setblock {x+" + (corner[i][0] + 2) + "} {y+13} {z+" + (corner[i][1] + 2) + "} minecraft:torch");
            m.wait(10);
        }
        m.cmd("Ворота: проём", "fill {x+26} {y+1} {z+0} {x+30} {y+5} {z+0} minecraft:air");
        m.cmd("Ворота: решётка", "fill {x+27} {y+1} {z+0} {x+29} {y+4} {z+0} minecraft:iron_bars");
        m.cmd("Ворота: левый столб", "fill {x+25} {y+1} {z+0} {x+25} {y+6} {z+0} minecraft:chiseled_stone_bricks");
        m.cmd("Ворота: правый столб", "fill {x+31} {y+1} {z+0} {x+31} {y+6} {z+0} minecraft:chiseled_stone_bricks");
        m.display("Ворота готовы");
        m.cmd("Двор: пол", "fill {x+17} {y+0} {z+1} {x+39} {y+0} {z+23} minecraft:coarse_dirt");
        m.cmd("Двор: дорога", "fill {x+27} {y+0} {z+1} {x+29} {y+0} {z+12} minecraft:gravel");
        m.cmd("Колодец: кольцо", "fill {x+20} {y+1} {z+18} {x+22} {y+1} {z+20} minecraft:cobblestone");
        m.cmd("Колодец: вода", "setblock {x+21} {y+1} {z+19} minecraft:water");
        m.cmd("Колодец: опоры", "fill {x+20} {y+2} {z+18} {x+22} {y+3} {z+20} minecraft:oak_fence hollow");
        m.cmd("Донжон: каркас", "fill {x+24} {y+0} {z+13} {x+32} {y+9} {z+21} minecraft:stone_bricks hollow");
        m.cmd("Донжон: дверь", "fill {x+27} {y+1} {z+13} {x+29} {y+3} {z+13} minecraft:air");
        m.cmd("Донжон: окно слева", "fill {x+24} {y+4} {z+16} {x+24} {y+5} {z+18} minecraft:glass_pane");
        m.cmd("Донжон: окно справа", "fill {x+32} {y+4} {z+16} {x+32} {y+5} {z+18} minecraft:glass_pane");
        for (int t = 0; t < 3; t++) m.cmd("Донжон: крыша, ярус " + (t + 1), "fill {x+" + (23 + t) + "} {y+" + (10 + t) + "} {z+" + (12 + t) + "} {x+" + (33 - t) + "} {y+" + (10 + t) + "} {z+" + (22 - t) + "} minecraft:dark_oak_planks");
        m.display("Донжон готов");
        for (int i = 0; i < 8; i++) m.cmd("Факел на стене " + (i + 1), "setblock {x+" + (18 + i * 3) + "} {y+3} {z+1} minecraft:wall_torch[facing=south]");
        m.cmd("Знамя", "setblock {x+28} {y+13} {z+17} minecraft:red_banner");
        m.cmd("Салют", "particle minecraft:firework {x+28} {y+14} {z+17} 4 3 4 0.1 100");
        m.cmd("Звук завершения", "playsound minecraft:block.bell.use master @a {x+28} {y+3} {z+12} 2 1");
        m.display("Крепость готова");
        m.finish(new String[]{"КРЕПОСТЬ-АВТОСБОРКА", "Строит крепость справа:", "стены по слоям, 4 башни,", "ворота, колодец и донжон.", "ПУСК - строить, СНОС - убрать"}, signs("строит крепость"),
                new String[]{"fill {x+13} {y+0} {z-3} {x+43} {y+16} {z+26} minecraft:air"}, "Крепость-автосборка");
    }

    // ------------------------------------------------------------------ пирамида-генератор
    private static void pyramid(M m) {
        final int T = 15;
        for (int k = 0; k < T; k++) {
            String p = "Ярус " + (k + 1) + ": ";
            m.cmd(p + "блоки", "fill {x+" + (16 + k) + "} {y+" + k + "} {z+" + k + "} {x+" + (46 - k) + "} {y+" + k + "} {z+" + (30 - k) + "} minecraft:sandstone");
            m.cmd(p + "кант спереди", "fill {x+" + (16 + k) + "} {y+" + k + "} {z+" + k + "} {x+" + (46 - k) + "} {y+" + k + "} {z+" + k + "} minecraft:cut_sandstone");
            m.cmd(p + "кант сзади", "fill {x+" + (16 + k) + "} {y+" + k + "} {z+" + (30 - k) + "} {x+" + (46 - k) + "} {y+" + k + "} {z+" + (30 - k) + "} minecraft:cut_sandstone");
            m.cmd(p + "пыль", "particle minecraft:dust_plume {x+31} {y+" + k + "} {z+15} 10 0 10 0 40");
            m.display("Ярус " + (k + 1) + " из " + T);
            m.wait(15);
        }
        m.cmd("Вход: тоннель", "fill {x+30} {y+1} {z+0} {x+32} {y+3} {z+14} minecraft:air");
        m.cmd("Вход: портал", "fill {x+29} {y+1} {z+0} {x+29} {y+4} {z+0} minecraft:chiseled_sandstone");
        m.cmd("Вход: портал справа", "fill {x+33} {y+1} {z+0} {x+33} {y+4} {z+0} minecraft:chiseled_sandstone");
        m.cmd("Зал: камера", "fill {x+27} {y+1} {z+14} {x+35} {y+6} {z+22} minecraft:air");
        m.cmd("Зал: пол", "fill {x+27} {y+0} {z+14} {x+35} {y+0} {z+22} minecraft:smooth_sandstone");
        m.cmd("Зал: сундук", "setblock {x+31} {y+1} {z+21} minecraft:chest[facing=north]");
        m.cmd("Зал: золото 1", "setblock {x+30} {y+1} {z+21} minecraft:gold_block");
        m.cmd("Зал: золото 2", "setblock {x+32} {y+1} {z+21} minecraft:gold_block");
        m.cmd("Зал: свет 1", "setblock {x+28} {y+5} {z+15} minecraft:lantern[hanging=true]");
        m.cmd("Зал: свет 2", "setblock {x+34} {y+5} {z+15} minecraft:lantern[hanging=true]");
        m.cmd("Зал: свет 3", "setblock {x+31} {y+5} {z+21} minecraft:lantern[hanging=true]");
        m.cmd("Ловушка: плита", "setblock {x+31} {y+1} {z+10} minecraft:stone_pressure_plate");
        m.cmd("Ловушка: подсказка", "setblock {x+31} {y+3} {z+9} minecraft:cobweb");
        m.cmd("Вершина: золото", "setblock {x+31} {y+15} {z+15} minecraft:gold_block");
        m.cmd("Вершина: свет", "setblock {x+31} {y+16} {z+15} minecraft:sea_lantern");
        m.cmd("Вершина: луч", "particle minecraft:end_rod {x+31} {y+17} {z+15} 0.2 3 0.2 0.02 60");
        m.cmd("Звук завершения", "playsound minecraft:block.beacon.activate master @a {x+31} {y+3} {z+15} 2 1");
        m.display("Пирамида готова");
        m.finish(new String[]{"ПИРАМИДА-ГЕНЕРАТОР", "Строит ступенчатую пирамиду:", "15 ярусов с кантом,", "вход, зал с сокровищами.", "ПУСК - строить, СНОС - убрать"}, signs("строит пирамиду"),
                new String[]{"fill {x+14} {y+0} {z-2} {x+48} {y+17} {z+32} minecraft:air"}, "Пирамида-генератор");
    }


    // ------------------------------------------------------------------ маяк
    private static void lighthouse(M m) {
        final int H = 18;
        for (int l = 0; l < H; l++) {
            int half = Math.max(2, 5 - l / 4);
            String blk = (l / 2) % 2 == 0 ? "red_concrete" : "white_concrete";
            int x0 = 24 - half, x1 = 24 + half, z0 = 10 - half, z1 = 10 + half;
            m.cmd("Слой " + (l + 1) + ": стена", "fill {x+" + x0 + "} {y+" + l + "} {z+" + z0 + "} {x+" + x1 + "} {y+" + l + "} {z+" + z1 + "} minecraft:" + blk);
            m.cmd("Слой " + (l + 1) + ": просвет", "fill {x+" + (x0 + 1) + "} {y+" + l + "} {z+" + (z0 + 1) + "} {x+" + (x1 - 1) + "} {y+" + l + "} {z+" + (z1 - 1) + "} minecraft:air");
            m.cmd("Слой " + (l + 1) + ": искры", "particle minecraft:cloud {x+24} {y+" + l + "} {z+10} " + half + " 0 " + half + " 0 20");
            m.display("Маяк: слой " + (l + 1) + " из " + H);
            m.wait(12);
        }
        m.cmd("Лестница внутри", "fill {x+23} {y+1} {z+9} {x+23} {y+" + (H - 1) + "} {z+9} minecraft:ladder[facing=east]");
        m.cmd("Дверь: проём", "fill {x+23} {y+1} {z+5} {x+25} {y+3} {z+5} minecraft:air");
        m.cmd("Крыльцо", "fill {x+22} {y+0} {z+3} {x+26} {y+0} {z+4} minecraft:stone_brick_slab");
        m.cmd("Галерея: пол", "fill {x+20} {y+" + H + "} {z+6} {x+28} {y+" + H + "} {z+14} minecraft:smooth_stone");
        m.cmd("Галерея: перила", "fill {x+20} {y+" + (H + 1) + "} {z+6} {x+28} {y+" + (H + 1) + "} {z+14} minecraft:iron_bars outline");
        m.cmd("Фонарная: стекло", "fill {x+22} {y+" + (H + 1) + "} {z+8} {x+26} {y+" + (H + 3) + "} {z+12} minecraft:glass hollow");
        m.cmd("Фонарная: лампа", "setblock {x+24} {y+" + (H + 2) + "} {z+10} minecraft:sea_lantern");
        m.cmd("Крыша", "fill {x+21} {y+" + (H + 4) + "} {z+7} {x+27} {y+" + (H + 4) + "} {z+13} minecraft:red_concrete");
        m.cmd("Шпиль", "fill {x+24} {y+" + (H + 5) + "} {z+10} {x+24} {y+" + (H + 7) + "} {z+10} minecraft:lightning_rod");
        for (int i = 0; i < 4; i++) m.cmd("Луч маяка " + (i + 1), "particle minecraft:end_rod {x+24} {y+" + (H + 2) + "} {z+10} " + (i < 2 ? 12 : 0) + " 0 " + (i < 2 ? 0 : 12) + " 0.05 60");
        m.cmd("Звук завершения", "playsound minecraft:block.bell.use master @a {x+24} {y+3} {z+10} 3 0.7");
        m.display("Маяк готов");
        m.finish(new String[]{"МАЯК-АВТОСБОРКА", "Строит маяк слоями:", "18 слоёв в полоску, галерея,", "лампа и шпиль.", "ПУСК - строить, СНОС - убрать"}, signs("строит маяк"),
                new String[]{"fill {x+18} {y+0} {z+2} {x+30} {y+28} {z+16} minecraft:air"}, "Маяк-автосборка");
    }

    // ------------------------------------------------------------------ лабиринт
    private static void maze(M m) {
        final int n = 10, size = 2 * n + 1;
        boolean[][] wall = new boolean[size][size];
        for (boolean[] r : wall) java.util.Arrays.fill(r, true);
        java.util.Random rnd = new java.util.Random(7);
        java.util.ArrayDeque<int[]> st = new java.util.ArrayDeque<>();
        boolean[][] seen = new boolean[n][n];
        st.push(new int[]{0, 0});
        seen[0][0] = true;
        wall[1][1] = false;
        int[][] dir = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!st.isEmpty()) {
            int[] c = st.peek();
            java.util.List<int[]> opts = new java.util.ArrayList<>();
            for (int[] d : dir) { int nx = c[0] + d[0], nz = c[1] + d[1]; if (nx >= 0 && nz >= 0 && nx < n && nz < n && !seen[nx][nz]) opts.add(d); }
            if (opts.isEmpty()) { st.pop(); continue; }
            int[] d = opts.get(rnd.nextInt(opts.size()));
            int nx = c[0] + d[0], nz = c[1] + d[1];
            wall[2 * c[0] + 1 + d[0]][2 * c[1] + 1 + d[1]] = false;
            wall[2 * nx + 1][2 * nz + 1] = false;
            seen[nx][nz] = true;
            st.push(new int[]{nx, nz});
        }
        wall[0][1] = false;
        wall[size - 1][size - 2] = false;
        m.cmd("Пол лабиринта", "fill {x+18} {y-1} {z+0} {x+38} {y-1} {z+20} minecraft:smooth_stone");
        m.display("Лабиринт: пол готов");
        m.wait(10);
        int runs = 0;
        for (int z = 0; z < size; z++) {
            int x = 0;
            while (x < size) {
                if (!wall[x][z]) { x++; continue; }
                int e = x;
                while (e + 1 < size && wall[e + 1][z]) e++;
                m.cmd("Стена: ряд " + (z + 1) + ", блоки " + (x + 1) + "-" + (e + 1), "fill {x+" + (18 + x) + "} {y+0} {z+" + z + "} {x+" + (18 + e) + "} {y+2} {z+" + z + "} minecraft:" + ((x + z) % 3 == 0 ? "mossy_stone_bricks" : "stone_bricks"));
                runs++;
                if (runs % 8 == 0) m.display("Лабиринт: построено " + runs + " стен");
                m.wait(4);
                x = e + 1;
            }
        }
        for (int i = 0; i < 8; i++) m.cmd("Факел " + (i + 1), "setblock {x+" + (19 + (i * 5) % 19) + "} {y+0} {z+" + (1 + (i * 7) % 19) + "} minecraft:lantern");
        m.cmd("Вход: арка", "fill {x+19} {y+3} {z+0} {x+19} {y+3} {z+0} minecraft:stone_brick_slab");
        m.cmd("Выход: маяк", "setblock {x+37} {y+3} {z+20} minecraft:sea_lantern");
        m.cmd("Выход: луч", "particle minecraft:end_rod {x+37} {y+4} {z+20} 0.2 3 0.2 0.02 60");
        m.cmd("Звук завершения", "playsound minecraft:entity.player.levelup master @a {x+28} {y+1} {z+10} 1 1");
        m.display("Лабиринт готов");
        m.finish(new String[]{"ЛАБИРИНТ-ГЕНЕРАТОР", "Строит лабиринт 21x21:", "стены по готовой схеме,", "вход слева, выход справа.", "ПУСК - строить, СНОС - убрать"}, signs("строит лабиринт"),
                new String[]{"fill {x+17} {y+0} {z-1} {x+39} {y+4} {z+21} minecraft:air", "fill {x+18} {y-1} {z+0} {x+38} {y-1} {z+20} minecraft:grass_block"}, "Лабиринт-генератор");
    }

    // ------------------------------------------------------------------ деревня
    private static void village(M m) {
        String[] roof = {"dark_oak_planks", "spruce_planks", "oak_planks", "birch_planks", "acacia_planks", "jungle_planks"};
        for (int h = 0; h < 6; h++) {
            int bx = 18 + (h % 3) * 10, bz = 2 + (h / 3) * 11;
            String p = "Дом " + (h + 1) + ": ";
            m.cmd(p + "фундамент", "fill {x+" + bx + "} {y-1} {z+" + bz + "} {x+" + (bx + 6) + "} {y-1} {z+" + (bz + 6) + "} minecraft:cobblestone");
            m.cmd(p + "стены", "fill {x+" + bx + "} {y+0} {z+" + bz + "} {x+" + (bx + 6) + "} {y+3} {z+" + (bz + 6) + "} minecraft:oak_planks hollow");
            m.cmd(p + "крыша 1", "fill {x+" + (bx - 1) + "} {y+4} {z+" + (bz - 1) + "} {x+" + (bx + 7) + "} {y+4} {z+" + (bz + 7) + "} minecraft:" + roof[h]);
            m.cmd(p + "крыша 2", "fill {x+" + bx + "} {y+5} {z+" + bz + "} {x+" + (bx + 6) + "} {y+5} {z+" + (bz + 6) + "} minecraft:" + roof[h]);
            m.cmd(p + "дверь", "fill {x+" + (bx + 3) + "} {y+0} {z+" + bz + "} {x+" + (bx + 3) + "} {y+1} {z+" + bz + "} minecraft:air");
            m.cmd(p + "окно 1", "fill {x+" + (bx + 1) + "} {y+2} {z+" + bz + "} {x+" + (bx + 1) + "} {y+2} {z+" + bz + "} minecraft:glass_pane");
            m.cmd(p + "окно 2", "fill {x+" + (bx + 5) + "} {y+2} {z+" + bz + "} {x+" + (bx + 5) + "} {y+2} {z+" + bz + "} minecraft:glass_pane");
            m.cmd(p + "лампа", "setblock {x+" + (bx + 3) + "} {y+3} {z+" + (bz + 3) + "} minecraft:lantern[hanging=true]");
            m.cmd(p + "дым", "particle minecraft:campfire_cosy_smoke {x+" + (bx + 3) + "} {y+6} {z+" + (bz + 3) + "} 0.3 0.5 0.3 0.01 6");
            m.display("Деревня: дом " + (h + 1) + " из 6");
            m.wait(15);
        }
        m.cmd("Колодец: кольцо", "fill {x+27} {y+0} {z+10} {x+29} {y+0} {z+12} minecraft:cobblestone");
        m.cmd("Колодец: вода", "setblock {x+28} {y+0} {z+11} minecraft:water");
        m.cmd("Колодец: опоры", "fill {x+27} {y+1} {z+10} {x+29} {y+2} {z+12} minecraft:oak_fence hollow");
        m.cmd("Колодец: крыша", "fill {x+27} {y+3} {z+10} {x+29} {y+3} {z+12} minecraft:oak_slab");
        m.cmd("Дорожка вдоль", "fill {x+17} {y-1} {z+9} {x+39} {y-1} {z+9} minecraft:gravel");
        m.cmd("Дорожка поперёк", "fill {x+28} {y-1} {z+1} {x+28} {y-1} {z+21} minecraft:gravel");
        for (int i = 0; i < 4; i++) {
            m.cmd("Дерево " + (i + 1) + ": ствол", "fill {x+" + (16 + i * 7) + "} {y+0} {z+22} {x+" + (16 + i * 7) + "} {y+4} {z+22} minecraft:oak_log");
            m.cmd("Дерево " + (i + 1) + ": крона", "fill {x+" + (15 + i * 7) + "} {y+4} {z+21} {x+" + (17 + i * 7) + "} {y+6} {z+23} minecraft:oak_leaves");
        }
        for (int i = 0; i < 6; i++) m.cmd("Фонарь на столбе " + (i + 1), "setblock {x+" + (19 + i * 4) + "} {y+0} {z+9} minecraft:lantern");
        m.cmd("Ограда: перед", "fill {x+16} {y+0} {z+0} {x+40} {y+0} {z+0} minecraft:oak_fence");
        m.cmd("Ограда: слева", "fill {x+16} {y+0} {z+0} {x+16} {y+0} {z+21} minecraft:oak_fence");
        m.cmd("Ограда: справа", "fill {x+40} {y+0} {z+0} {x+40} {y+0} {z+21} minecraft:oak_fence");
        m.cmd("Ворота деревни", "fill {x+27} {y+0} {z+0} {x+29} {y+0} {z+0} minecraft:oak_fence_gate[facing=north]");
        m.cmd("Звук завершения", "playsound minecraft:entity.villager.celebrate master @a {x+28} {y+1} {z+10} 2 1");
        m.display("Деревня готова");
        m.finish(new String[]{"ДЕРЕВНЯ-АВТОСБОРКА", "Строит 6 домов, колодец,", "дорожки, деревья и ограду.", "У каждого дома своя крыша.", "ПУСК - строить, СНОС - убрать"}, signs("строит деревню"),
                new String[]{"fill {x+15} {y+0} {z-1} {x+41} {y+8} {z+24} minecraft:air", "fill {x+15} {y-1} {z-1} {x+41} {y-1} {z+24} minecraft:grass_block"}, "Деревня-автосборка");
    }

    // ------------------------------------------------------------------ стадион
    private static void stadium(M m) {
        m.cmd("Поле: газон", "fill {x+22} {y-1} {z+4} {x+46} {y-1} {z+18} minecraft:grass_block");
        for (int i = 0; i < 6; i++) {
            String[] lines = {"fill {x+22} {y-1} {z+4} {x+46} {y-1} {z+4} minecraft:white_concrete", "fill {x+22} {y-1} {z+18} {x+46} {y-1} {z+18} minecraft:white_concrete", "fill {x+22} {y-1} {z+4} {x+22} {y-1} {z+18} minecraft:white_concrete",
                    "fill {x+46} {y-1} {z+4} {x+46} {y-1} {z+18} minecraft:white_concrete", "fill {x+34} {y-1} {z+4} {x+34} {y-1} {z+18} minecraft:white_concrete", "fill {x+32} {y-1} {z+10} {x+36} {y-1} {z+12} minecraft:white_concrete"};
            m.cmd("Разметка " + (i + 1), lines[i]);
        }
        m.display("Поле готово");
        m.wait(15);
        for (int t = 0; t < 8; t++) {
            int o = 2 + t;
            String blk = t % 2 == 0 ? "quartz_block" : "light_gray_concrete";
            m.cmd("Трибуна, ярус " + (t + 1) + ": кольцо", "fill {x+" + (22 - o) + "} {y+" + t + "} {z+" + (4 - o) + "} {x+" + (46 + o) + "} {y+" + t + "} {z+" + (18 + o) + "} minecraft:" + blk);
            m.cmd("Трибуна, ярус " + (t + 1) + ": просвет", "fill {x+" + (22 - o + 1) + "} {y+" + t + "} {z+" + (4 - o + 1) + "} {x+" + (46 + o - 1) + "} {y+" + t + "} {z+" + (18 + o - 1) + "} minecraft:air");
            m.cmd("Трибуна, ярус " + (t + 1) + ": сиденья", "fill {x+" + (22 - o) + "} {y+" + (t + 1) + "} {z+" + (4 - o) + "} {x+" + (46 + o) + "} {y+" + (t + 1) + "} {z+" + (4 - o) + "} minecraft:" + (t % 3 == 0 ? "red_concrete" : t % 3 == 1 ? "blue_concrete" : "yellow_concrete"));
            m.cmd("Трибуна, ярус " + (t + 1) + ": пыль", "particle minecraft:cloud {x+34} {y+" + t + "} {z+11} 14 0 8 0 25");
            m.display("Трибуны: ярус " + (t + 1) + " из 8");
            m.wait(12);
        }
        for (int g = 0; g < 2; g++) {
            int gx = g == 0 ? 22 : 46;
            m.cmd("Ворота " + (g + 1) + ": штанги", "fill {x+" + gx + "} {y+0} {z+9} {x+" + gx + "} {y+2} {z+13} minecraft:iron_bars outline");
            m.cmd("Ворота " + (g + 1) + ": сетка", "fill {x+" + (gx + (g == 0 ? -1 : 1)) + "} {y+0} {z+9} {x+" + (gx + (g == 0 ? -1 : 1)) + "} {y+2} {z+13} minecraft:white_stained_glass");
            m.cmd("Ворота " + (g + 1) + ": свет", "setblock {x+" + gx + "} {y+3} {z+11} minecraft:sea_lantern");
        }
        int[][] tw = {{18, 0}, {50, 0}, {18, 22}, {50, 22}};
        for (int i = 0; i < 4; i++) {
            m.cmd("Прожектор " + (i + 1) + ": мачта", "fill {x+" + tw[i][0] + "} {y+0} {z+" + tw[i][1] + "} {x+" + tw[i][0] + "} {y+14} {z+" + tw[i][1] + "} minecraft:iron_block");
            m.cmd("Прожектор " + (i + 1) + ": лампы", "fill {x+" + (tw[i][0] - 1) + "} {y+15} {z+" + tw[i][1] + "} {x+" + (tw[i][0] + 1) + "} {y+15} {z+" + tw[i][1] + "} minecraft:sea_lantern");
            m.cmd("Прожектор " + (i + 1) + ": свет", "particle minecraft:end_rod {x+" + tw[i][0] + "} {y+15} {z+" + tw[i][1] + "} 0.5 0 0.5 0 12");
            m.cmd("Прожектор " + (i + 1) + ": луч", "particle minecraft:glow {x+" + ((tw[i][0] + 34) / 2) + "} {y+8} {z+" + ((tw[i][1] + 11) / 2) + "} 3 3 3 0 20");
        }
        m.cmd("Вход: тоннель", "fill {x+33} {y+0} {z+0} {x+35} {y+2} {z+3} minecraft:air");
        m.cmd("Вход: арка", "fill {x+32} {y+3} {z+0} {x+36} {y+3} {z+0} minecraft:stone_bricks");
        m.cmd("Табло: рама", "fill {x+30} {y+9} {z+2} {x+38} {y+11} {z+2} minecraft:black_concrete");
        m.cmd("Табло: счёт", "fill {x+31} {y+10} {z+1} {x+37} {y+10} {z+1} minecraft:lime_concrete");
        m.cmd("Звук завершения", "playsound minecraft:block.bell.use master @a {x+34} {y+2} {z+11} 3 1");
        m.display("Стадион готов");
        m.finish(new String[]{"СТАДИОН-АВТОСБОРКА", "Строит стадион:", "поле с разметкой, 8 ярусов", "трибун, ворота, прожекторы.", "ПУСК - строить, СНОС - убрать"}, signs("строит стадион"),
                new String[]{"fill {x+10} {y+0} {z-8} {x+56} {y+18} {z+30} minecraft:air", "fill {x+20} {y-1} {z+2} {x+48} {y-1} {z+20} minecraft:grass_block"}, "Стадион-автосборка");
    }

    private NeroMachines() { }
}
