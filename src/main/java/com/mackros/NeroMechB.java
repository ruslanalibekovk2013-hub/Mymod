package com.mackros;

import static com.mackros.NeroMech.*;

/** Механизмы 9-15. */
final class NeroMechB {
    static String cc(int i) { return NeroMechA.cc(i); }

    // ---------------------------------------------------------------- 9. бур
    static void drill(M m) {
        var b = m.b;
        b.part("Буровая вышка");
        b.box(3, 0, 4, 3, 6, 4, "iron_bars");
        b.box(7, 0, 4, 7, 6, 4, "iron_bars");
        b.box(3, 0, 8, 3, 6, 8, "iron_bars");
        b.box(7, 0, 8, 7, 6, 8, "iron_bars");
        b.box(3, 7, 4, 7, 7, 8, "iron_block");
        b.box(4, 0, 5, 6, 0, 7, "iron_block");
        b.set(5, 8, 6, "lightning_rod");
        int D = 14;
        m.init.addAll(java.util.List.of(m.sc("#run", 0), m.sc("#dep", 0), m.sc("#tk", 0), m.sc("#new", 0)));
        m.a("Бур вниз", m.p(0) + "run " + m.sc("#run", 1));
        m.a("Бур вверх", m.p(1) + "run " + m.sc("#run", 2));
        m.a("Стоп", m.p(2) + "run " + m.sc("#run", 0));
        m.a("Сброс: режим", m.p(3) + "run " + m.sc("#run", 0));
        m.a("Сброс: глубина", m.p(3) + "run " + m.sc("#dep", 0));
        m.a("Сброс: головка", m.p(3) + "run " + fill(4, 0, 5, 6, 0, 7, "iron_block"));
        m.a("Сообщение: вниз", m.p(0) + "run " + act(5, 1, 6, "Бур идёт вниз", "gold"));
        m.a("Сообщение: вверх", m.p(1) + "run " + act(5, 1, 6, "Бур поднимается", "aqua"));
        m.a("Сообщение: стоп", m.p(2) + "run " + act(5, 1, 6, "Бур остановлен", "red"));
        m.a("Такт", m.isr("#run", "1..2") + "run " + m.ad("#tk", 1));
        m.a("Шаг вниз", m.isr("#tk", "8..") + "if score #run " + m.L + " matches 1 if score #dep " + m.L + " matches .." + (D - 1) + " run " + m.ad("#dep", 1));
        m.a("Шаг вверх", m.isr("#tk", "8..") + "if score #run " + m.L + " matches 2 if score #dep " + m.L + " matches 1.. run " + m.ad("#dep", -1));
        m.a("Флаг шага", m.isr("#tk", "8..") + "if score #run " + m.L + " matches 1..2 run " + m.sc("#new", 1));
        m.a("Сброс такта", m.isr("#tk", "8..") + "run " + m.sc("#tk", 0));
        m.a("Дно достигнуто", m.is("#run", 1) + "if score #dep " + m.L + " matches " + D + " run " + m.sc("#run", 0));
        m.a("Верх достигнут", m.is("#run", 2) + "if score #dep " + m.L + " matches 0 run " + m.sc("#run", 0));
        for (int n = 1; n <= D; n++) {
            String dn = m.is("#new", 1) + "if score #run " + m.L + " matches 1 if score #dep " + m.L + " matches " + n + " run ";
            m.a("Слой " + n + ": копать", dn + destroy(4, -n, 5, 6, -n, 7));
            m.a("Слой " + n + ": головка", dn + fill(4, -n, 5, 6, -n, 7, "iron_block"));
            m.a("Слой " + n + ": убрать прошлую", dn + fill(4, -n + 1, 5, 6, -n + 1, 7, "air"));
        }
        for (int n = 0; n < D; n++) {
            String up = m.is("#new", 1) + "if score #run " + m.L + " matches 2 if score #dep " + m.L + " matches " + n + " run ";
            m.a("Подъём " + n + ": убрать", up + fill(4, -n - 1, 5, 6, -n - 1, 7, "air"));
            m.a("Подъём " + n + ": поставить", up + fill(4, -n, 5, 6, -n, 7, "iron_block"));
        }
        m.a("Звук бура", m.is("#new", 1) + "if score #run " + m.L + " matches 1 run " + snd(5, 1, 6, "block.stone.break", "1.5", "0.6"));
        m.a("Звук подъёма", m.is("#new", 1) + "if score #run " + m.L + " matches 2 run " + snd(5, 1, 6, "block.piston.contract", "1", "0.7"));
        m.a("Пыль у шахты", m.is("#new", 1) + "run " + par(5, 1, 6, "campfire_cosy_smoke ~ ~0.5 ~ 0.6 0.2 0.6 0.01 3"));
        m.a("Табло глубины", m.isr("#run", "1..2") + "run " + at(5, 1, 6) + "title @a[distance=..20] actionbar [{\"text\":\"Глубина: \",\"color\":\"gray\"},{\"score\":{\"name\":\"#dep\",\"objective\":\"" + m.L + "\"},\"color\":\"gold\"},{\"text\":\" из " + D + "\",\"color\":\"gray\"}]");
        m.a("Лампа: работа", m.isr("#run", "1..2") + "run " + set(4, 1, 0, "orange_concrete"));
        m.a("Лампа: покой", m.is("#run", 0) + "run " + set(4, 1, 0, "red_concrete"));
        m.a("Сброс флага шага", m.sc("#new", 0));
    }

    // ---------------------------------------------------------------- 10. тир
    static void range(M m) {
        var b = m.b;
        b.part("Огневой рубеж");
        b.box(3, 0, 2, 15, 0, 2, "oak_fence");
        b.box(2, -1, 3, 16, -1, 13, "smooth_stone");
        b.part("Стена мишеней");
        b.box(3, 0, 13, 15, 3, 13, "stone_bricks");
        for (int i = 0; i < 6; i++) b.box(4 + 2 * i, 1, 12, 4 + 2 * i, 2, 12, "air");
        m.init.addAll(java.util.List.of(m.sc("#game", 0), m.sc("#tm", 0), m.sc("#cur", 0), m.sc("#lt", 0), m.sc("#sp", 0), m.sc("#pts", 0), m.sc("#life", 40), m.sc("#dly", 20), m.sc("#spw", 0), m.sc("#hit", 0), m.sc("#r", 0), m.sc("#6", 6)));
        m.a("Старт игры", m.p(0) + "run " + m.sc("#game", 1));
        m.a("Старт: очки", m.p(0) + "run " + m.sc("#pts", 0));
        m.a("Старт: время", m.p(0) + "run " + m.sc("#tm", 1200));
        m.a("Старт: мишень", m.p(0) + "run " + m.sc("#cur", 0));
        m.a("Старт: пауза до мишени", m.p(0) + "run " + m.sc("#sp", 20));
        m.a("Старт: заголовок", m.p(0) + "run " + ttl(9, 1, 2, "Тир открыт", "gold"));
        m.a("Стоп: игра", m.p(1) + "run " + m.sc("#game", 0));
        m.a("Стоп: убрать мишени", m.p(1) + "run " + fill(4, 1, 12, 14, 2, 12, "air"));
        m.a("Стоп: мишень", m.p(1) + "run " + m.sc("#cur", 0));
        m.a("Лёгко: жизнь", m.p(2) + "run " + m.sc("#life", 70));
        m.a("Лёгко: пауза", m.p(2) + "run " + m.sc("#dly", 30));
        m.a("Лёгко: сообщение", m.p(2) + "run " + act(9, 1, 2, "Лёгкий уровень", "green"));
        m.a("Средне: жизнь", m.p(3) + "run " + m.sc("#life", 45));
        m.a("Средне: пауза", m.p(3) + "run " + m.sc("#dly", 20));
        m.a("Средне: сообщение", m.p(3) + "run " + act(9, 1, 2, "Средний уровень", "yellow"));
        m.a("Сложно: жизнь", m.p(4) + "run " + m.sc("#life", 25));
        m.a("Сложно: пауза", m.p(4) + "run " + m.sc("#dly", 10));
        m.a("Сложно: сообщение", m.p(4) + "run " + act(9, 1, 2, "Сложный уровень", "red"));
        m.a("Время идёт", m.is("#game", 1) + "run " + m.ad("#tm", -1));
        m.a("Пауза до мишени", m.is("#game", 1) + "if score #cur " + m.L + " matches 0 run " + m.ad("#sp", -1));
        m.a("Флаг появления", m.is("#game", 1) + "if score #cur " + m.L + " matches 0 if score #sp " + m.L + " matches ..0 run " + m.sc("#spw", 1));
        m.a("Метка случайности", m.is("#spw", 1) + "run summon minecraft:marker ~ ~ ~ {Tags:[\"rnd\"]}");
        m.a("Случайное число", m.is("#spw", 1) + "run execute store result score #r " + m.L + " run data get entity @e[type=minecraft:marker,tag=rnd,limit=1] UUID[0]");
        m.a("Случайное: сдвиг", m.is("#spw", 1) + "run " + m.ad("#r", 6));
        m.a("Случайное: остаток", m.is("#spw", 1) + "run scoreboard players operation #r " + m.L + " %= #6 " + m.L);
        m.a("Случайное: ещё сдвиг", m.is("#spw", 1) + "run " + m.ad("#r", 6));
        m.a("Случайное: ещё остаток", m.is("#spw", 1) + "run scoreboard players operation #r " + m.L + " %= #6 " + m.L);
        m.a("Убрать метку", "kill @e[type=minecraft:marker,tag=rnd]");
        for (int k = 0; k < 6; k++) {
            m.a("Мишень " + (k + 1) + ": появилась", m.is("#spw", 1) + "if score #r " + m.L + " matches " + k + " run " + fill(4 + 2 * k, 1, 12, 4 + 2 * k, 2, 12, "target"));
            m.a("Мишень " + (k + 1) + ": номер", m.is("#spw", 1) + "if score #r " + m.L + " matches " + k + " run " + m.sc("#cur", k + 1));
        }
        m.a("Мишень: время жизни", m.is("#spw", 1) + "run scoreboard players operation #lt " + m.L + " = #life " + m.L);
        m.a("Мишень: пауза", m.is("#spw", 1) + "run scoreboard players operation #sp " + m.L + " = #dly " + m.L);
        m.a("Мишень: звук", m.is("#spw", 1) + "run " + snd(9, 1, 8, "block.note_block.pling", "1", "1.5"));
        m.a("Сброс флага появления", m.sc("#spw", 0));
        m.a("Жизнь мишени", m.isr("#cur", "1..") + "run " + m.ad("#lt", -1));
        for (int k = 0; k < 6; k++) {
            int x = 4 + 2 * k;
            m.a("Мишень " + (k + 1) + ": попадание", m.is("#cur", k + 1) + pos(x, 1, 11) + "if entity @e[type=#minecraft:arrows,distance=..2.6] run " + m.sc("#hit", 1));
            m.a("Мишень " + (k + 1) + ": убрать стрелу", m.is("#cur", k + 1) + "if score #hit " + m.L + " matches 1 run " + at(x, 1, 11) + "kill @e[type=#minecraft:arrows,distance=..2.6]");
            m.a("Мишень " + (k + 1) + ": убрать при попадании", m.is("#cur", k + 1) + "if score #hit " + m.L + " matches 1 run " + fill(x, 1, 12, x, 2, 12, "air"));
            m.a("Мишень " + (k + 1) + ": убрать по времени", m.is("#cur", k + 1) + "if score #lt " + m.L + " matches ..0 run " + fill(x, 1, 12, x, 2, 12, "air"));
            m.a("Мишень " + (k + 1) + ": вспышка", m.is("#cur", k + 1) + "if score #hit " + m.L + " matches 1 run " + par(x, 1, 11, "crit ~0.5 ~1 ~0.5 0.3 0.3 0.3 0.3 20"));
            m.a("Мишень " + (k + 1) + ": звук", m.is("#cur", k + 1) + "if score #hit " + m.L + " matches 1 run " + snd(x, 1, 11, "entity.arrow.hit_player", "1", "1"));
        }
        m.a("Очко", m.is("#hit", 1) + "run " + m.ad("#pts", 1));
        m.a("Мишень снята: попадание", m.is("#hit", 1) + "run " + m.sc("#cur", 0));
        m.a("Мишень снята: время", m.isr("#lt", "..0") + "if score #cur " + m.L + " matches 1.. run " + m.sc("#cur", 0));
        m.a("Сброс попадания", m.sc("#hit", 0));
        m.a("Табло", m.is("#game", 1) + "run " + at(9, 1, 2) + "title @a[distance=..30] actionbar [{\"text\":\"Очки: \",\"color\":\"gold\"},{\"score\":{\"name\":\"#pts\",\"objective\":\"" + m.L + "\"},\"color\":\"yellow\"},{\"text\":\"  Время: \",\"color\":\"gray\"},{\"score\":{\"name\":\"#tm\",\"objective\":\"" + m.L + "\"},\"color\":\"aqua\"}]");
        m.a("Конец игры", m.is("#game", 1) + "if score #tm " + m.L + " matches ..0 run " + m.sc("#game", 0));
        m.a("Конец: убрать мишени", m.is("#tm", 0) + "run " + fill(4, 1, 12, 14, 2, 12, "air"));
        m.a("Конец: итог", m.is("#tm", 0) + "run " + at(9, 1, 2) + "title @a[distance=..30] title [{\"text\":\"Очки: \",\"color\":\"gold\"},{\"score\":{\"name\":\"#pts\",\"objective\":\"" + m.L + "\"},\"color\":\"yellow\"}]");
        m.a("Конец: звук", m.is("#tm", 0) + "run " + snd(9, 1, 2, "entity.player.levelup", "1", "1"));
    }

    // ---------------------------------------------------------------- 11. автомат
    static void vending(M m) {
        var b = m.b;
        b.part("Автомат");
        b.box(3, 0, 4, 13, 4, 4, "gray_concrete");
        b.box(3, 0, 5, 13, 0, 7, "smooth_stone");
        for (int i = 0; i < 5; i++) { b.box(4 + 2 * i, 1, 4, 4 + 2 * i, 3, 4, "glass"); }
        String[][] tr = {{"iron_ingot", "5", "diamond", "1", "5 железа"}, {"coal", "9", "emerald", "1", "9 угля"}, {"gold_ingot", "3", "bread", "12", "3 золота"}, {"diamond", "1", "arrow", "16", "1 алмаз"}, {"emerald", "2", "golden_apple", "1", "2 изумруда"}};
        String[] nm = {"Алмаз", "Изумруд", "12 хлеба", "16 стрел", "Золотое яблоко"};
        for (int i = 0; i < 5; i++) {
            b.setup(at(4 + 2 * i, 1, 5) + "summon minecraft:item_display ~0.5 ~2.2 ~-0.6 {item:{id:\"minecraft:" + tr[i][2] + "\",Count:1b},item_display:\"fixed\",billboard:\"vertical\",Tags:[\"{tag}\"]}");
            m.init.add(m.sc("#s" + i, 10));
        }
        m.init.addAll(java.util.List.of(m.sc("#c", 0), m.sc("#last", 0)));
        for (int i = 0; i < 5; i++) {
            String P = m.p(i) + "run ";
            String ok = m.p(i) + "if score #c " + m.L + " matches " + tr[i][1] + ".. if score #s" + i + " " + m.L + " matches 1.. run ";
            m.a("Товар " + (i + 1) + ": посчитать", P + "execute store result score #c " + m.L + " run clear @p minecraft:" + tr[i][0] + " 0");
            m.a("Товар " + (i + 1) + ": забрать оплату", ok + "clear @p minecraft:" + tr[i][0] + " " + tr[i][1]);
            m.a("Товар " + (i + 1) + ": выдать", ok + "give @p minecraft:" + tr[i][2] + " " + tr[i][3]);
            m.a("Товар " + (i + 1) + ": остаток", ok + m.ad("#s" + i, -1));
            m.a("Товар " + (i + 1) + ": запомнить", ok + m.sc("#last", i + 1));
            m.a("Товар " + (i + 1) + ": сообщение", ok + act(8, 1, 3, "Куплено: " + nm[i], "green"));
            m.a("Товар " + (i + 1) + ": звук", ok + snd(8, 1, 3, "entity.player.levelup", "1", "1.4"));
            m.a("Товар " + (i + 1) + ": блеск", ok + par(4 + 2 * i, 1, 4, "happy_villager ~0.5 ~1.5 ~-0.5 0.3 0.4 0.3 0 8"));
            m.a("Товар " + (i + 1) + ": мало денег", m.p(i) + "if score #c " + m.L + " matches .." + (Integer.parseInt(tr[i][1]) - 1) + " run " + act(8, 1, 3, "Нужно: " + tr[i][4], "red"));
            m.a("Товар " + (i + 1) + ": нет в наличии", m.p(i) + "if score #s" + i + " " + m.L + " matches ..0 run " + act(8, 1, 3, "Товар закончился, нажми ВЕРНУТЬ или пополни запас", "red"));
            m.a("Лампа товара " + (i + 1), m.is("#s" + i, 0) + "run " + set(4 + 2 * i, 4, 4, "red_concrete"));
            m.a("Лампа товара " + (i + 1) + ": есть", m.isr("#s" + i, "1..") + "run " + set(4 + 2 * i, 4, 4, "lime_concrete"));
        }
        for (int i = 0; i < 5; i++) {
            m.a("Возврат: проверка " + (i + 1), m.p(5) + "if score #last " + m.L + " matches " + (i + 1) + " run give @p minecraft:" + tr[i][0] + " " + tr[i][1]);
            m.a("Возврат: склад " + (i + 1), m.p(5) + "if score #last " + m.L + " matches " + (i + 1) + " run " + m.ad("#s" + i, 1));
        }
        m.a("Возврат: сообщение", m.p(5) + "if score #last " + m.L + " matches 1.. run " + act(8, 1, 3, "Оплата возвращена", "aqua"));
        m.a("Возврат: нечего", m.p(5) + "if score #last " + m.L + " matches 0 run " + act(8, 1, 3, "Нет покупки для возврата", "red"));
        m.a("Возврат: очистить", m.p(5) + "run " + m.sc("#last", 0));
        m.a("Склад: табло", at(8, 1, 3) + "title @a[distance=..6] actionbar [{\"text\":\"Склад: \",\"color\":\"gray\"},{\"score\":{\"name\":\"#s0\",\"objective\":\"" + m.L + "\"}},{\"text\":\" \"},{\"score\":{\"name\":\"#s1\",\"objective\":\"" + m.L + "\"}},{\"text\":\" \"},{\"score\":{\"name\":\"#s2\",\"objective\":\"" + m.L + "\"}},{\"text\":\" \"},{\"score\":{\"name\":\"#s3\",\"objective\":\"" + m.L + "\"}},{\"text\":\" \"},{\"score\":{\"name\":\"#s4\",\"objective\":\"" + m.L + "\"}}]");
        m.a("Сброс покупки", m.sc("#c", 0));
    }

    // ---------------------------------------------------------------- 12. конвейер
    static void conveyor(M m) {
        var b = m.b;
        b.part("Лента");
        b.box(3, -1, 5, 16, -1, 7, "gray_concrete");
        b.box(3, 0, 4, 16, 0, 4, "iron_bars");
        b.box(3, 0, 8, 16, 0, 8, "iron_bars");
        b.set(2, -1, 6, "furnace");
        b.set(17, -1, 6, "hopper");
        m.init.addAll(java.util.List.of(m.sc("#on", 0), m.sc("#dir", 1), m.sc("#spd", 1), m.sc("#ph", 0), m.sc("#tk", 0), m.sc("#cnt", 0)));
        m.a("Пуск", m.p(0) + "run " + m.sc("#on", 1));
        m.a("Стоп", m.p(1) + "run " + m.sc("#on", 0));
        m.a("Реверс: 1?", m.p(2) + "if score #dir " + m.L + " matches 1 run " + m.sc("#tg", 2));
        m.a("Реверс: 2?", m.p(2) + "if score #dir " + m.L + " matches 2 run " + m.sc("#tg", 1));
        m.a("Реверс: применить", m.p(2) + "run scoreboard players operation #dir " + m.L + " = #tg " + m.L);
        m.a("Скорость: шаг", m.p(3) + "run " + m.ad("#spd", 1));
        m.a("Скорость: круг", m.isr("#spd", "4..") + "run " + m.sc("#spd", 1));
        m.a("Скорость: сообщение", m.p(3) + "run " + at(9, 1, 3) + "title @a[distance=..12] actionbar [{\"text\":\"Скорость ленты: \",\"color\":\"gold\"},{\"score\":{\"name\":\"#spd\",\"objective\":\"" + m.L + "\"},\"color\":\"yellow\"}]");
        String sel = "@e[type=!minecraft:marker,dx=13,dy=1,dz=2]";
        double[] sp = {0.1, 0.2, 0.3};
        for (int d = 1; d <= 2; d++) for (int s = 1; s <= 3; s++)
            m.a("Движение: направление " + d + ", скорость " + s, m.is("#on", 1) + "if score #dir " + m.L + " matches " + d + " if score #spd " + m.L + " matches " + s + " run " + at(3, 0, 5) + "execute as " + sel + " at @s run tp @s ~" + (d == 1 ? sp[s - 1] : -sp[s - 1]) + " ~ ~");
        m.a("Счёт анимации", m.is("#on", 1) + "run " + m.ad("#tk", 1));
        m.a("Смена фазы", m.isr("#tk", "4..") + "run " + m.ad("#ph", 1));
        m.a("Сброс счёта анимации", m.isr("#tk", "4..") + "run " + m.sc("#tk", 0));
        m.a("Фаза по кругу", m.isr("#ph", "2..") + "run " + m.sc("#ph", 0));
        for (int ph = 0; ph < 2; ph++) for (int x = 3; x <= 16; x++)
            m.a("Лента: фаза " + ph + ", колонка " + (x - 2), m.is("#on", 1) + "if score #ph " + m.L + " matches " + ph + " if score #tk " + m.L + " matches 0 run " + fill(x, -1, 5, x, -1, 7, (x + ph) % 2 == 0 ? "black_concrete" : "gray_concrete"));
        m.a("Звук ленты", m.is("#on", 1) + "if score #tk " + m.L + " matches 0 run " + snd(9, 0, 6, "block.piston.extend", "0.4", "2"));
        m.a("Пыль в начале", m.is("#on", 1) + "run " + par(3, 0, 6, "dust 0.6 0.6 0.6 1 ~ ~0.1 ~ 0.4 0 0.8 0 1"));
        m.a("Подсчёт грузов", "execute store result score #cnt " + m.L + " positioned " + ph3(3, 0, 5) + " if entity @e[type=minecraft:item,dx=13,dy=1,dz=2]");
        m.a("Табло грузов", m.is("#on", 1) + "run " + at(9, 1, 3) + "title @a[distance=..12] actionbar [{\"text\":\"Грузов на ленте: \",\"color\":\"gray\"},{\"score\":{\"name\":\"#cnt\",\"objective\":\"" + m.L + "\"},\"color\":\"gold\"}]");
        m.a("Лампа: работа", m.is("#on", 1) + "run " + set(4, 1, 0, "lime_concrete"));
        m.a("Лампа: стоп", m.is("#on", 0) + "run " + set(4, 1, 0, "red_concrete"));
        m.a("Выход: звук", m.is("#on", 1) + pos(16, 0, 5) + "if entity @e[type=minecraft:item,dx=1,dy=1,dz=2] run " + snd(16, 0, 6, "entity.item.pickup", "1", "1"));
        m.a("Выход: частицы", m.is("#on", 1) + pos(16, 0, 5) + "if entity @e[type=minecraft:item,dx=1,dy=1,dz=2] run " + par(16, 0, 6, "happy_villager ~0.5 ~0.5 ~0.5 0.3 0.3 0.3 0 5"));
        for (int i = 0; i < 4; i++) {
            m.a("Стрелки " + (i + 1) + ": фаза 0", m.is("#on", 1) + "if score #ph " + m.L + " matches 0 run " + set(4 + 3 * i, 0, 9, "yellow_concrete"));
            m.a("Стрелки " + (i + 1) + ": фаза 1", m.is("#on", 1) + "if score #ph " + m.L + " matches 1 run " + set(4 + 3 * i, 0, 9, "gray_concrete"));
        }
        String[] it = {"diamond", "iron_ingot", "gold_ingot", "coal"};
        String[] co = {"light_blue_concrete", "white_concrete", "yellow_concrete", "black_concrete"};
        for (int i = 0; i < 4; i++) {
            m.a("Сортировка " + (i + 1) + ": есть", m.is("#on", 1) + pos(16, 0, 5) + "if entity @e[type=minecraft:item,nbt={Item:{id:\"minecraft:" + it[i] + "\"}},dx=1,dy=1,dz=2] run " + set(17 + i, 1, 9, co[i]));
            m.a("Сортировка " + (i + 1) + ": нет", m.is("#on", 1) + pos(16, 0, 5) + "unless entity @e[type=minecraft:item,nbt={Item:{id:\"minecraft:" + it[i] + "\"}},dx=1,dy=1,dz=2] run " + set(17 + i, 1, 9, "gray_concrete"));
        }
        for (int s = 1; s <= 3; s++) {
            m.a("Лампа скорости " + s + ": есть", m.isr("#spd", s + "..") + "run " + set(5 + s, 1, 0, "lime_concrete"));
            m.a("Лампа скорости " + s + ": нет", m.isr("#spd", ".." + (s - 1)) + "run " + set(5 + s, 1, 0, "gray_concrete"));
        }
    }

    // ---------------------------------------------------------------- 13. часы и таймер
    static void clock(M m) {
        var b = m.b;
        b.part("Циферблат");
        b.box(3, 0, 3, 13, 12, 3, "black_concrete");
        int[][] ring = new int[12][2];
        for (int k = 0; k < 12; k++) {
            double a = Math.PI * 2 * k / 12;
            ring[k][0] = 8 + (int) Math.round(Math.sin(a) * 4);
            ring[k][1] = 6 + (int) Math.round(Math.cos(a) * 4);
            b.set(ring[k][0], ring[k][1], 2, "gray_concrete");
        }
        b.set(8, 6, 2, "white_concrete");
        m.init.addAll(java.util.List.of(m.sc("#sw", 0), m.sc("#sec", 0), m.sc("#tk", 0), m.sc("#tmr", 0), m.sc("#armed", 0), m.sc("#alr", 0), m.sc("#sx", 0), m.sc("#idx", 0), m.sc("#dt", 0),
                m.sc("#h", 0), m.sc("#mi", 0), m.sc("#1000", 1000), m.sc("#60", 60), m.sc("#24", 24), m.sc("#12", 12)));
        m.toggle(0, "#sw", "Секундомер");
        m.a("Таймер 10: время", m.p(1) + "run " + m.sc("#tmr", 10));
        m.a("Таймер 10: взвод", m.p(1) + "run " + m.sc("#armed", 1));
        m.a("Таймер 10: сообщение", m.p(1) + "run " + act(8, 1, 3, "Таймер: 10 секунд", "aqua"));
        m.a("Таймер 60: время", m.p(2) + "run " + m.sc("#tmr", 60));
        m.a("Таймер 60: взвод", m.p(2) + "run " + m.sc("#armed", 1));
        m.a("Таймер 60: сообщение", m.p(2) + "run " + act(8, 1, 3, "Таймер: 60 секунд", "aqua"));
        m.a("Сброс: секундомер", m.p(3) + "run " + m.sc("#sec", 0));
        m.a("Сброс: ход", m.p(3) + "run " + m.sc("#sw", 0));
        m.a("Сброс: таймер", m.p(3) + "run " + m.sc("#tmr", 0));
        m.a("Сброс: взвод", m.p(3) + "run " + m.sc("#armed", 0));
        m.a("Сброс: тревога", m.p(3) + "run " + m.sc("#alr", 0));
        for (int k = 0; k < 12; k++) m.a("Сброс: лампа " + (k + 1), m.p(3) + "run " + set(ring[k][0], ring[k][1], 2, "gray_concrete"));
        m.a("Время суток", "execute store result score #dt " + m.L + " run time query daytime");
        m.a("Часы: копия", "scoreboard players operation #h " + m.L + " = #dt " + m.L);
        m.a("Часы: тысячи", "scoreboard players operation #h " + m.L + " /= #1000 " + m.L);
        m.a("Часы: сдвиг на 6", m.ad("#h", 6));
        m.a("Часы: сутки", "scoreboard players operation #h " + m.L + " %= #24 " + m.L);
        m.a("Минуты: копия", "scoreboard players operation #mi " + m.L + " = #dt " + m.L);
        m.a("Минуты: остаток", "scoreboard players operation #mi " + m.L + " %= #1000 " + m.L);
        m.a("Минуты: умножить", "scoreboard players operation #mi " + m.L + " *= #60 " + m.L);
        m.a("Минуты: разделить", "scoreboard players operation #mi " + m.L + " /= #1000 " + m.L);
        m.a("Такт секунды", m.ad("#tk", 1));
        m.a("Импульс секунды", m.isr("#tk", "20..") + "run " + m.sc("#sx", 1));
        m.a("Сброс такта", m.isr("#tk", "20..") + "run " + m.sc("#tk", 0));
        m.a("Секундомер идёт", m.is("#sx", 1) + "if score #sw " + m.L + " matches 1 run " + m.ad("#sec", 1));
        m.a("Таймер идёт", m.is("#sx", 1) + "if score #tmr " + m.L + " matches 1.. run " + m.ad("#tmr", -1));
        m.a("Тик таймера: звук", m.is("#sx", 1) + "if score #tmr " + m.L + " matches 1..5 run " + snd(8, 1, 3, "block.note_block.hat", "1", "1.5"));
        m.a("Индекс лампы: копия", m.is("#sx", 1) + "run scoreboard players operation #idx " + m.L + " = #sec " + m.L);
        m.a("Индекс: таймер", m.is("#sx", 1) + "if score #tmr " + m.L + " matches 1.. run scoreboard players operation #idx " + m.L + " = #tmr " + m.L);
        m.a("Индекс: остаток", m.is("#sx", 1) + "run scoreboard players operation #idx " + m.L + " %= #12 " + m.L);
        for (int k = 0; k < 12; k++) {
            m.a("Циферблат: зажечь " + (k + 1), m.is("#sx", 1) + "if score #idx " + m.L + " matches " + k + " run " + set(ring[k][0], ring[k][1], 2, "sea_lantern"));
            m.a("Циферблат: погасить " + (k + 1), m.is("#sx", 1) + "if score #idx " + m.L + " matches " + ((k + 1) % 12) + " run " + set(ring[k][0], ring[k][1], 2, "gray_concrete"));
        }
        m.a("Звонок: пуск", m.is("#sx", 1) + "if score #tmr " + m.L + " matches 0 if score #armed " + m.L + " matches 1 run " + m.sc("#alr", 60));
        m.a("Звонок: снять взвод", m.is("#sx", 1) + "if score #tmr " + m.L + " matches 0 if score #armed " + m.L + " matches 1 run " + m.sc("#armed", 0));
        m.a("Звонок: заголовок", m.is("#alr", 60) + "run " + ttl(8, 1, 3, "Время вышло", "red"));
        m.a("Звонок: счёт", m.isr("#alr", "1..") + "run " + m.ad("#alr", -1));
        for (int i = 0; i < 6; i++) {
            m.a("Звонок: звук " + (i + 1), m.is("#alr", 55 - 10 * i) + "run " + snd(8, 1, 3, "block.note_block.bell", "2", i % 2 == 0 ? "0.8" : "1.4"));
            m.a("Звонок: лампа " + (i + 1), m.is("#alr", 55 - 10 * i) + "run " + set(8, 6, 2, i % 2 == 0 ? "red_concrete" : "white_concrete"));
        }
        m.a("Табло", at(8, 1, 3) + "title @a[distance=..14] actionbar [{\"text\":\"Время мира \",\"color\":\"gray\"},{\"score\":{\"name\":\"#h\",\"objective\":\"" + m.L + "\"},\"color\":\"gold\"},{\"text\":\":\",\"color\":\"gold\"},{\"score\":{\"name\":\"#mi\",\"objective\":\"" + m.L + "\"},\"color\":\"gold\"},{\"text\":\"  Секундомер \",\"color\":\"gray\"},{\"score\":{\"name\":\"#sec\",\"objective\":\"" + m.L + "\"},\"color\":\"green\"},{\"text\":\"  Таймер \",\"color\":\"gray\"},{\"score\":{\"name\":\"#tmr\",\"objective\":\"" + m.L + "\"},\"color\":\"aqua\"}]");
        m.a("Сброс импульса", m.sc("#sx", 0));
    }

    // ---------------------------------------------------------------- 14. купол
    static void dome(M m) {
        var b = m.b;
        b.part("Генератор");
        b.box(7, 0, 7, 9, 1, 9, "iron_block");
        b.set(8, 2, 8, "beacon");
        b.box(7, -1, 7, 9, -1, 9, "iron_block");
        int[] w = {6, 6, 6, 5, 5, 4, 3};
        m.init.addAll(java.util.List.of(m.sc("#on", 0), m.sc("#en", 40), m.sc("#tk", 0), m.sc("#wv", 0), m.sc("#grow", 0)));
        m.a("Включить: есть энергия", m.p(0) + "if score #en " + m.L + " matches 10.. if score #on " + m.L + " matches 0 run " + m.sc("#on", 1));
        m.a("Включить: рост", m.p(0) + "if score #en " + m.L + " matches 10.. if score #f " + m.L + " matches 0 run " + m.sc("#f", 1));
        m.a("Включить: нет энергии", m.p(0) + "if score #en " + m.L + " matches ..9 run " + act(8, 1, 8, "Мало энергии, нажми ЗАРЯД", "red"));
        m.a("Выключить", m.p(1) + "if score #on " + m.L + " matches 1 run " + m.sc("#f", 101));
        m.a("Выключить: флаг", m.p(1) + "run " + m.sc("#on", 0));
        m.a("Заряд", m.p(2) + "run " + m.ad("#en", 20));
        m.a("Заряд: предел", m.isr("#en", "101..") + "run " + m.sc("#en", 100));
        m.a("Заряд: звук", m.p(2) + "run " + snd(8, 2, 8, "block.beacon.activate", "1", "1.4"));
        m.a("Заряд: частицы", m.p(2) + "run " + par(8, 3, 8, "end_rod ~0.5 ~ ~0.5 0.3 0.5 0.3 0.1 15"));
        for (int i = 0; i < 7; i++) {
            m.a("Слой " + (i + 1) + ": вырастает", m.is("#f", 3 * (i + 1)) + "run " + fill(8 - w[i], i, 8 - w[i], 8 + w[i], i, 8 + w[i], "light_blue_stained_glass") + " outline");
            m.a("Слой " + (i + 1) + ": звук", m.is("#f", 3 * (i + 1)) + "run " + snd(8, i, 8, "block.glass.place", "1", String.valueOf(0.6 + i * 0.1)));
            m.a("Слой " + (i + 1) + ": частицы", m.is("#f", 3 * (i + 1)) + "run " + par(8, i, 8, "end_rod ~0.5 ~0.5 ~0.5 " + w[i] + " 0.1 " + w[i] + " 0 25"));
            int fo = 101 + 3 * (6 - i);
            m.a("Слой " + (i + 1) + ": исчезает", m.is("#f", fo) + "run " + repl(8 - w[i], i, 8 - w[i], 8 + w[i], i, 8 + w[i], "air", "light_blue_stained_glass"));
        }
        m.a("Макушка: вырастает", m.f(24) + "run " + fill(6, 7, 6, 10, 7, 10, "light_blue_stained_glass"));
        m.a("Макушка: исчезает", m.f(101) + "run " + repl(6, 7, 6, 10, 7, 10, "air", "light_blue_stained_glass"));
        m.a("Звук запуска", m.f(1) + "run " + snd(8, 2, 8, "block.beacon.power_select", "2", "1"));
        m.a("Звук выключения", m.f(101) + "run " + snd(8, 2, 8, "block.beacon.deactivate", "2", "1"));
        m.a("Счёт кадров: рост", m.isr("#f", "1..25") + "run " + m.ad("#f", 1));
        m.a("Счёт кадров: выключение", m.isr("#f", "101..") + "run " + m.ad("#f", 1));
        m.a("Конец выключения", m.isr("#f", "125..") + "run " + m.sc("#f", 0));
        m.a("Расход: такт", m.is("#on", 1) + "run " + m.ad("#tk", 1));
        m.a("Расход: энергия", m.isr("#tk", "20..") + "run " + m.ad("#en", -1));
        m.a("Расход: сброс такта", m.isr("#tk", "20..") + "run " + m.sc("#tk", 0));
        m.a("Энергия кончилась", m.is("#en", 0) + "if score #on " + m.L + " matches 1 run " + m.sc("#f", 101));
        m.a("Энергия кончилась: флаг", m.is("#en", 0) + "run " + m.sc("#on", 0));
        m.a("Энергия: табло", at(8, 1, 8) + "title @a[distance=..16] actionbar [{\"text\":\"Энергия купола: \",\"color\":\"gray\"},{\"score\":{\"name\":\"#en\",\"objective\":\"" + m.L + "\"},\"color\":\"aqua\"}]");
        m.a("Волна: такт", m.is("#on", 1) + "run " + m.ad("#wv", 1));
        for (int i = 0; i < 7; i++) m.a("Волна по слою " + (i + 1), m.is("#wv", 40 + 2 * i) + "if score #on " + m.L + " matches 1 run " + par(8, i, 8, "electric_spark ~0.5 ~0.5 ~0.5 " + w[i] + " 0.1 " + w[i] + " 0 20"));
        m.a("Волна: сброс", m.isr("#wv", "54..") + "run " + m.sc("#wv", 0));
        m.a("Лампа: включено", m.is("#on", 1) + "run " + set(4, 1, 0, "lime_concrete"));
        m.a("Лампа: выключено", m.is("#on", 0) + "run " + set(4, 1, 0, "red_concrete"));
        for (String t : NeroMechC.FOES) m.a("Щит бьёт: " + t.replace("minecraft:", "").replace("#", ""), m.is("#on", 1) + "if score #tk " + m.L + " matches 0 " + pos(8, 0, 8) + "run damage @e[type=" + t + ",distance=6..7.5] 1 minecraft:magic");
        for (int i = 0; i < 4; i++) m.a("Мерцание щита " + (i + 1), m.is("#on", 1) + "if score #en " + m.L + " matches ..15 if score #tk " + m.L + " matches " + (i * 5) + " run " + par(8, 2 + i, 8, "electric_spark ~0.5 ~ ~0.5 5 1 5 0 10"));
        m.a("Предупреждение: мало энергии", m.is("#on", 1) + "if score #en " + m.L + " matches 10 if score #tk " + m.L + " matches 0 run " + act(8, 1, 8, "Энергия купола на исходе", "red"));
    }

    // ---------------------------------------------------------------- 15. рулетка
    static void roulette(M m) {
        var b = m.b;
        b.part("Колесо");
        b.box(8, 0, 4, 8, 2, 4, "iron_block");
        b.box(5, 0, 3, 11, 0, 5, "polished_blackstone");
        int[][] wh = new int[12][2];
        for (int k = 0; k < 12; k++) {
            double a = Math.PI * 2 * k / 12;
            wh[k][0] = 8 + (int) Math.round(Math.sin(a) * 4);
            wh[k][1] = 8 + (int) Math.round(Math.cos(a) * 4);
            b.set(wh[k][0], wh[k][1], 3, k % 2 == 0 ? "red_concrete" : "black_concrete");
        }
        b.set(8, 8, 3, "gold_block");
        m.init.addAll(java.util.List.of(m.sc("#sp", 0), m.sc("#pos", 0), m.sc("#dl", 0), m.sc("#dly", 2), m.sc("#st", 0), m.sc("#bet", 0), m.sc("#mv", 0), m.sc("#r", 0), m.sc("#c", 0), m.sc("#fin", 0), m.sc("#12", 12)));
        for (int[] bet : new int[][]{{1, 1}, {2, 5}}) {
            m.a("Ставка " + bet[1] + ": посчитать", m.p(bet[0]) + "if score #sp " + m.L + " matches 0 run execute store result score #c " + m.L + " run clear @p minecraft:emerald 0");
            m.a("Ставка " + bet[1] + ": списать", m.p(bet[0]) + "if score #sp " + m.L + " matches 0 if score #c " + m.L + " matches " + bet[1] + ".. run clear @p minecraft:emerald " + bet[1]);
            m.a("Ставка " + bet[1] + ": принята", m.p(bet[0]) + "if score #sp " + m.L + " matches 0 if score #c " + m.L + " matches " + bet[1] + ".. run " + m.sc("#bet", bet[1]));
            m.a("Ставка " + bet[1] + ": сообщение", m.p(bet[0]) + "if score #sp " + m.L + " matches 0 if score #c " + m.L + " matches " + bet[1] + ".. run " + act(8, 1, 3, "Ставка " + bet[1] + " изумруд(ов) принята, жми КРУТИТЬ", "green"));
            m.a("Ставка " + bet[1] + ": нет денег", m.p(bet[0]) + "if score #c " + m.L + " matches .." + (bet[1] - 1) + " run " + act(8, 1, 3, "Нужно изумрудов: " + bet[1], "red"));
        }
        m.a("Крутить: старт", m.p(0) + "if score #sp " + m.L + " matches 0 if score #bet " + m.L + " matches 1.. run " + m.sc("#sp", 1));
        m.a("Крутить: метка случайности", m.is("#sp", 1) + "run summon minecraft:marker ~ ~ ~ {Tags:[\"rnd\"]}");
        m.a("Крутить: случайное", m.is("#sp", 1) + "run execute store result score #r " + m.L + " run data get entity @e[type=minecraft:marker,tag=rnd,limit=1] UUID[0]");
        m.a("Крутить: остаток", m.is("#sp", 1) + "run scoreboard players operation #r " + m.L + " %= #12 " + m.L);
        m.a("Крутить: шаги", m.is("#sp", 1) + "run scoreboard players operation #st " + m.L + " = #r " + m.L);
        m.a("Крутить: минимум шагов", m.is("#sp", 1) + "run " + m.ad("#st", 24));
        m.a("Крутить: звук старта", m.is("#sp", 1) + "run " + snd(8, 5, 3, "block.note_block.bell", "1", "1.2"));
        m.a("Крутить: вращение", m.is("#sp", 1) + "run " + m.sc("#sp", 2));
        m.a("Убрать метку", "kill @e[type=minecraft:marker,tag=rnd]");
        m.a("Шаг: такт", m.is("#sp", 2) + "if score #st " + m.L + " matches 1.. run " + m.ad("#dl", 1));
        m.a("Шаг: время", m.is("#sp", 2) + "if score #dl " + m.L + " >= #dly " + m.L + " if score #st " + m.L + " matches 1.. run " + m.sc("#mv", 1));
        m.a("Шаг: позиция", m.is("#mv", 1) + "run " + m.ad("#pos", 1));
        m.a("Шаг: по кругу", m.isr("#pos", "12..") + "run " + m.sc("#pos", 0));
        m.a("Шаг: остаток шагов", m.is("#mv", 1) + "run " + m.ad("#st", -1));
        m.a("Шаг: сброс такта", m.is("#mv", 1) + "run " + m.sc("#dl", 0));
        m.a("Шаг: замедление", m.is("#mv", 1) + "if score #st " + m.L + " matches ..9 run " + m.ad("#dly", 1));
        for (int k = 0; k < 12; k++) {
            m.a("Колесо: зажечь " + (k + 1), m.is("#mv", 1) + "if score #pos " + m.L + " matches " + k + " run " + set(wh[k][0], wh[k][1], 3, "gold_block"));
            m.a("Колесо: вернуть цвет " + (k + 1), m.is("#mv", 1) + "if score #pos " + m.L + " matches " + ((k + 1) % 12) + " run " + set(wh[k][0], wh[k][1], 3, k % 2 == 0 ? "red_concrete" : "black_concrete"));
        }
        m.a("Шаг: щелчок", m.is("#mv", 1) + "run " + snd(8, 5, 3, "block.note_block.hat", "1", "1.8"));
        m.a("Остановка", m.is("#sp", 2) + "if score #st " + m.L + " matches 0 run " + m.sc("#fin", 1));
        m.a("Остановка: конец вращения", m.is("#fin", 1) + "run " + m.sc("#sp", 0));
        m.a("Остановка: скорость", m.is("#fin", 1) + "run " + m.sc("#dly", 2));
        String[][] pr = {{"diamond", "3", "3 алмаза"}, {"emerald", "4", "4 изумруда"}, {"", "0", ""}, {"iron_ingot", "8", "8 железа"}, {"gold_ingot", "2", "2 золота"}, {"", "0", ""},
                {"diamond", "1", "алмаз"}, {"emerald", "6", "6 изумрудов"}, {"", "0", ""}, {"golden_apple", "1", "золотое яблоко"}, {"gold_ingot", "2", "2 золота"}, {"", "0", ""}};
        for (int k = 0; k < 12; k++) {
            String fk = m.is("#fin", 1) + "if score #pos " + m.L + " matches " + k + " run ";
            if (pr[k][0].isEmpty()) {
                m.a("Итог " + (k + 1) + ": пусто", fk + ttl(8, 1, 3, "Не повезло", "red"));
            } else {
                m.a("Итог " + (k + 1) + ": приз", fk + "give @p minecraft:" + pr[k][0] + " " + pr[k][1]);
                m.a("Итог " + (k + 1) + ": заголовок", fk + ttl(8, 1, 3, "Выигрыш: " + pr[k][2], "gold"));
            }
        }
        m.a("Итог: звук победы", m.is("#fin", 1) + "if score #pos " + m.L + " matches 0 run " + snd(8, 2, 3, "entity.player.levelup", "2", "1"));
        m.a("Итог: фейерверк", m.is("#fin", 1) + "if score #pos " + m.L + " matches 0 run summon minecraft:firework_rocket " + ph3(8, 4, 2) + " {LifeTime:20,FireworksItem:{id:\"minecraft:firework_rocket\",Count:1b,tag:{Fireworks:{Flight:1b,Explosions:[{Type:1,Colors:[I;16766720],FadeColors:[I;16777215],Trail:1b,Flicker:1b}]}}}}");
        m.a("Итог: сброс ставки", m.is("#fin", 1) + "run " + m.sc("#bet", 0));
        m.a("Итог: сброс флага", m.sc("#fin", 0));
        m.a("Шаг: сброс флага", m.sc("#mv", 0));
        m.a("Приз: подсказка", m.p(3) + "run " + act(8, 1, 3, "Призы выдаются сразу после остановки колеса", "aqua"));
    }

    private NeroMechB() { }
}
