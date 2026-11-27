package com.mackros;

import static com.mackros.NeroMech.*;

/** Механизмы 1-8. */
final class NeroMechA {
    static final String[] COL = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "cyan", "purple", "blue", "green", "red", "gray", "light_gray", "brown", "black"};

    static String cc(int i) { return COL[((i % 16) + 16) % 16] + "_concrete"; }

    static String dn(M m, String sel, int f, String cmd) { return m.is(sel, 0).replace("matches 0", "matches " + f) + cmd; }

    // ---------------------------------------------------------------- 1. шлюз
    static void airlock(M m) {
        var b = m.b;
        b.part("Шлюзовая камера");
        b.hol(5, -1, 3, 11, 4, 9, "stone_bricks");
        b.air(6, 0, 4, 10, 3, 8);
        b.box(0, -1, 4, 4, -1, 8, "smooth_stone");
        b.box(12, -1, 4, 16, -1, 8, "smooth_stone");
        b.box(5, 0, 5, 5, 2, 7, "iron_block");
        b.box(11, 0, 5, 11, 2, 7, "iron_block");
        b.setup(fill(5, 3, 5, 5, 3, 7, "red_concrete"), fill(11, 3, 5, 11, 3, 7, "red_concrete"), fill(7, 4, 6, 9, 4, 6, "white_concrete"));
        m.init.add(m.sc("#dir", 0));
        m.a("Старт: войти", m.p(0) + "if score #f " + m.L + " matches 0 run " + m.sc("#dir", 1));
        m.a("Старт: выйти", m.p(1) + "if score #f " + m.L + " matches 0 run " + m.sc("#dir", 2));
        m.a("Запуск сценария", "execute if score #dir " + m.L + " matches 1..2 if score #f " + m.L + " matches 0 run " + m.sc("#f", 1));
        m.a("Сброс: закрыть двери", m.p(2) + "run " + fill(5, 0, 5, 5, 2, 7, "iron_block"));
        m.a("Сброс: закрыть вторую", m.p(2) + "run " + fill(11, 0, 5, 11, 2, 7, "iron_block"));
        m.a("Сброс: лампы", m.p(2) + "run " + fill(5, 3, 5, 5, 3, 7, "red_concrete"));
        m.a("Сброс: лампы 2", m.p(2) + "run " + fill(11, 3, 5, 11, 3, 7, "red_concrete"));
        m.a("Сброс: счётчик", m.p(2) + "run " + m.sc("#f", 0));
        m.a("Сброс: направление", m.p(2) + "run " + m.sc("#dir", 0));
        for (int d = 1; d <= 2; d++) {
            int x1 = d == 1 ? 5 : 11, x2 = d == 1 ? 11 : 5;
            String D = m.is("#dir", d);
            int[][] tl = {{1, 1}, {8, 2}, {70, 3}, {77, 4}};
            String[] cl = {"yellow_concrete", "lime_concrete", "yellow_concrete", "red_concrete"};
            for (int i = 0; i < 4; i++) m.a("Дверь 1: лампа " + (i + 1) + " (напр. " + d + ")", D + m.f(tl[i][0]).replace("execute ", "") + "run " + fill(x1, 3, 5, x1, 3, 7, cl[i]));
            int[][] tl2 = {{140, 1}, {148, 2}, {210, 3}, {217, 4}};
            for (int i = 0; i < 4; i++) m.a("Дверь 2: лампа " + (i + 1) + " (напр. " + d + ")", D + m.f(tl2[i][0]).replace("execute ", "") + "run " + fill(x2, 3, 5, x2, 3, 7, cl[i]));
            for (int r = 0; r < 3; r++) {
                m.a("Дверь 1 открывается: ряд " + (r + 1) + " (напр. " + d + ")", D + m.f(3 + 2 * r).replace("execute ", "") + "run " + fill(x1, r, 5, x1, r, 7, "air"));
                m.a("Дверь 1 закрывается: ряд " + (3 - r) + " (напр. " + d + ")", D + m.f(72 + 2 * r).replace("execute ", "") + "run " + fill(x1, 2 - r, 5, x1, 2 - r, 7, "iron_block"));
                m.a("Дверь 2 открывается: ряд " + (r + 1) + " (напр. " + d + ")", D + m.f(143 + 2 * r).replace("execute ", "") + "run " + fill(x2, r, 5, x2, r, 7, "air"));
                m.a("Дверь 2 закрывается: ряд " + (3 - r) + " (напр. " + d + ")", D + m.f(212 + 2 * r).replace("execute ", "") + "run " + fill(x2, 2 - r, 5, x2, 2 - r, 7, "iron_block"));
            }
            m.a("Табло: входите (напр. " + d + ")", D + m.f(9).replace("execute ", "") + "run " + act(8, 1, 6, "Шлюз открыт, входите", "green"));
            m.a("Табло: закрытие (напр. " + d + ")", D + m.f(70).replace("execute ", "") + "run " + act(8, 1, 6, "Двери закрываются", "yellow"));
            m.a("Табло: выход (напр. " + d + ")", D + m.f(149).replace("execute ", "") + "run " + act(8, 1, 6, "Выход свободен", "green"));
            m.a("Табло: конец (напр. " + d + ")", D + m.f(210).replace("execute ", "") + "run " + act(8, 1, 6, "Двери закрываются", "yellow"));
        }
        int[] sn = {3, 5, 7, 72, 74, 76, 143, 145, 147, 212, 214, 216};
        for (int f : sn) m.a("Звук двери в кадре " + f, m.f(f) + "run " + snd(8, 1, 6, "block.piston.extend", "1", "0.7"));
        m.a("Центр: синий", m.f(80) + "run " + fill(7, 4, 6, 9, 4, 6, "light_blue_concrete"));
        m.a("Центр: белый", m.f(140) + "run " + fill(7, 4, 6, 9, 4, 6, "white_concrete"));
        m.a("Герметизация 3", m.f(80) + "run " + ttl(8, 1, 6, "3", "aqua"));
        m.a("Герметизация 2", m.f(100) + "run " + ttl(8, 1, 6, "2", "aqua"));
        m.a("Герметизация 1", m.f(120) + "run " + ttl(8, 1, 6, "1", "aqua"));
        m.a("Пар в камере", m.fr(80, 139) + "run " + par(8, 1, 6, "cloud ~ ~0.5 ~ 1.5 0.8 1.5 0.02 6"));
        m.a("Пар у двери", m.fr(80, 139) + "run " + par(6, 1, 6, "large_smoke ~ ~0.5 ~ 0.2 0.8 0.8 0.01 2"));
        m.a("Пар у двери 2", m.fr(80, 139) + "run " + par(10, 1, 6, "large_smoke ~ ~0.5 ~ 0.2 0.8 0.8 0.01 2"));
        m.frames(220);
        m.a("Конец: направление", m.f(0) + "if score #dir " + m.L + " matches 1..2 run " + m.sc("#dir", 0));
    }

    // ---------------------------------------------------------------- 2. кодовый замок
    static void codelock(M m) {
        var b = m.b;
        b.part("Хранилище");
        b.hol(3, -1, 3, 11, 5, 9, "stone_bricks");
        b.air(4, 0, 4, 10, 4, 8);
        b.box(6, 0, 3, 8, 2, 3, "iron_block");
        b.box(5, 3, 3, 9, 3, 3, "red_concrete");
        b.set(7, 0, 6, "chest");
        b.set(5, 1, 3, "polished_blackstone_button[face=wall,facing=north]");
        m.init.addAll(java.util.List.of(m.sc("#code", 1234), m.sc("#10", 10), m.sc("#v", 0), m.sc("#n", 0), m.sc("#setm", 0), m.sc("#bad", 0), m.sc("#lk", 0), m.sc("#op", 0), m.sc("#ok", 0)));
        for (int i = 0; i < 10; i++) {
            int dg = i < 9 ? i + 1 : 0;
            String c = m.p(i) + "if score #lk " + m.L + " matches 0 if score #op " + m.L + " matches 0 if score #n " + m.L + " matches ..3 run ";
            m.a("Цифра " + dg + ": сдвиг", c + "scoreboard players operation #v " + m.L + " *= #10 " + m.L);
            m.a("Цифра " + dg + ": добавить", c + m.ad("#v", dg));
            m.a("Цифра " + dg + ": счётчик", c + m.ad("#n", 1));
            m.a("Цифра " + dg + ": звук", m.p(i) + "run " + snd(7, 1, 2, "block.note_block.hat", "1", String.valueOf(1.0 + dg * 0.08)));
        }
        m.a("Табло ввода", at(7, 1, 2) + "title @a[distance=..8] actionbar [{\"text\":\"Код: \",\"color\":\"gray\"},{\"score\":{\"name\":\"#v\",\"objective\":\"" + m.L + "\"},\"color\":\"gold\"},{\"text\":\"  цифр: \",\"color\":\"gray\"},{\"score\":{\"name\":\"#n\",\"objective\":\"" + m.L + "\"},\"color\":\"gold\"}]");
        m.a("Сброс ввода: число", m.p(11) + "run " + m.sc("#v", 0));
        m.a("Сброс ввода: счётчик", m.p(11) + "run " + m.sc("#n", 0));
        m.a("Звук сброса", m.p(11) + "run " + snd(7, 1, 2, "block.note_block.bass", "1", "0.7"));
        m.a("Режим нового кода", m.p(12) + "run " + m.sc("#setm", 1));
        m.a("Подсказка нового кода", m.p(12) + "run " + act(7, 1, 2, "Введи 4 цифры нового кода и нажми ВВОД", "aqua"));
        m.a("Сохранить новый код", m.p(10) + "if score #setm " + m.L + " matches 1 if score #n " + m.L + " matches 4 run scoreboard players operation #code " + m.L + " = #v " + m.L);
        m.a("Сообщение: код изменён", m.p(10) + "if score #setm " + m.L + " matches 1 if score #n " + m.L + " matches 4 run " + act(7, 1, 2, "Код изменён", "green"));
        m.a("Звук: код изменён", m.p(10) + "if score #setm " + m.L + " matches 1 if score #n " + m.L + " matches 4 run " + snd(7, 1, 2, "entity.player.levelup", "1", "1.5"));
        m.a("Выход из режима кода", m.p(10) + "if score #setm " + m.L + " matches 1 if score #n " + m.L + " matches 4 run " + m.sc("#setm", 0));
        m.a("Проверка кода: верно", m.p(10) + "if score #setm " + m.L + " matches 0 if score #n " + m.L + " matches 4 if score #lk " + m.L + " matches 0 if score #v " + m.L + " = #code " + m.L + " run " + m.sc("#ok", 1));
        m.a("Проверка кода: неверно", m.p(10) + "if score #setm " + m.L + " matches 0 if score #n " + m.L + " matches 4 if score #lk " + m.L + " matches 0 unless score #v " + m.L + " = #code " + m.L + " run " + m.ad("#bad", 1));
        m.a("Неверно: сообщение", m.p(10) + "if score #setm " + m.L + " matches 0 if score #n " + m.L + " matches 4 if score #ok " + m.L + " matches 0 run " + act(7, 1, 2, "Неверный код", "red"));
        m.a("Неверно: звук", m.p(10) + "if score #setm " + m.L + " matches 0 if score #n " + m.L + " matches 4 if score #ok " + m.L + " matches 0 run " + snd(7, 1, 2, "block.note_block.bass", "1.5", "0.5"));
        m.a("Верно: сообщение", m.is("#ok", 1) + "run " + act(7, 1, 2, "Код верный, хранилище открывается", "green"));
        m.a("Верно: запуск двери", m.is("#ok", 1) + "run " + m.sc("#op", 1));
        m.a("Верно: сброс попыток", m.is("#ok", 1) + "run " + m.sc("#bad", 0));
        m.a("Конец ввода: число", m.p(10) + "run " + m.sc("#v", 0));
        m.a("Конец ввода: счётчик", m.p(10) + "run " + m.sc("#n", 0));
        m.a("Три ошибки: блокировка", m.isr("#bad", "3..") + "run " + m.sc("#lk", 200));
        m.a("Три ошибки: сброс попыток", m.isr("#bad", "3..") + "run " + m.sc("#bad", 0));
        m.a("Дверь: счётчик", m.isr("#op", "1..") + "run " + m.ad("#op", 1));
        int[] fr = {2, 4, 6};
        for (int r = 0; r < 3; r++) {
            m.a("Дверь открывается: ряд " + (3 - r), m.is("#op", fr[r]) + "run " + fill(6, 2 - r, 3, 8, 2 - r, 3, "air"));
            m.a("Дверь закрывается: ряд " + (r + 1), m.is("#op", 102 + 2 * r) + "run " + fill(6, r, 3, 8, r, 3, "iron_block"));
            m.a("Звук двери " + (r + 1), m.is("#op", fr[r]) + "run " + snd(7, 1, 3, "block.piston.extend", "1", "0.6"));
            m.a("Звук двери закрытие " + (r + 1), m.is("#op", 102 + 2 * r) + "run " + snd(7, 1, 3, "block.piston.contract", "1", "0.6"));
        }
        m.a("Лампа зелёная", m.is("#op", 8) + "run " + fill(5, 3, 3, 9, 3, 3, "lime_concrete"));
        m.a("Лампа красная", m.is("#op", 108) + "run " + fill(5, 3, 3, 9, 3, 3, "red_concrete"));
        m.a("Сообщение: закрытие", m.is("#op", 100) + "run " + act(7, 1, 2, "Хранилище закрывается", "yellow"));
        m.a("Блеск при открытии", m.fr0("#op", "8..99") + "run " + par(7, 1, 5, "end_rod ~ ~1 ~ 1.5 0.8 1.5 0.01 2"));
        m.a("Конец сценария двери", m.isr("#op", "110..") + "run " + m.sc("#op", 0));
        m.a("Блокировка: счётчик", m.isr("#lk", "1..") + "run " + m.ad("#lk", -1));
        m.a("Блокировка: сообщение", m.is("#lk", 199) + "run " + ttl(7, 1, 2, "ТРЕВОГА", "red"));
        for (int i = 0; i < 10; i++) {
            m.a("Сирена " + (i + 1), m.is("#lk", 190 - 20 * i) + "run " + snd(7, 1, 4, "block.note_block.bell", "2", i % 2 == 0 ? "0.5" : "1.5"));
            m.a("Мигание " + (i + 1), m.is("#lk", 190 - 20 * i) + "run " + fill(5, 3, 3, 9, 3, 3, i % 2 == 0 ? "orange_concrete" : "red_concrete"));
        }
        m.a("Блокировка снята: лампа", m.is("#lk", 1) + "run " + fill(5, 3, 3, 9, 3, 3, "red_concrete"));
        m.a("Блокировка снята: сообщение", m.is("#lk", 1) + "run " + act(7, 1, 2, "Блокировка снята", "green"));
        m.a("Сброс подтверждения", m.sc("#ok", 0));
    }

    // ---------------------------------------------------------------- 3. светофор
    static void traffic(M m) {
        var b = m.b;
        b.part("Перекрёсток");
        b.box(0, -1, 4, 16, -1, 8, "black_concrete");
        b.box(6, -1, 0, 10, -1, 12, "black_concrete");
        for (int i = 0; i < 5; i++) { b.box(1 + 3 * i, -1, 5, 2 + 3 * i, -1, 7, "white_concrete"); }
        int[][] heads = {{6, 4}, {10, 8}, {10, 4}, {6, 8}};
        for (int[] h : heads) { b.box(h[0], 0, h[1], h[0], 1, h[1], "stone_brick_wall"); b.box(h[0], 2, h[1], h[0], 4, h[1], "gray_concrete"); }
        int[][] ped = {{5, 4}, {11, 8}, {11, 4}, {5, 8}};
        for (int[] p : ped) b.set(p[0], 0, p[1], "red_concrete");
        m.init.addAll(java.util.List.of(m.sc("#mode", 0), m.sc("#em", 0), m.sc("#bt", 0), m.sc("#tg", 0), m.sc("#f", 1)));
        m.a("Ход цикла", m.is("#em", 0) + "if score #mode " + m.L + " matches 0 run " + m.ad("#f", 1));
        m.a("Конец цикла", m.isr("#f", "320..") + "run " + m.sc("#f", 1));
        m.a("Пешеход: прыжок в жёлтую (NS)", m.p(0) + "if score #mode " + m.L + " matches 0 if score #f " + m.L + " matches 2..98 run " + m.sc("#f", 100));
        m.a("Пешеход: прыжок в жёлтую (EW)", m.p(0) + "if score #mode " + m.L + " matches 0 if score #f " + m.L + " matches 162..258 run " + m.sc("#f", 260));
        m.a("Режим: нормальный?", m.p(1) + "if score #mode " + m.L + " matches 0 run " + m.sc("#tg", 1));
        m.a("Режим: мигающий?", m.p(1) + "if score #mode " + m.L + " matches 1 run " + m.sc("#tg", 0));
        m.a("Режим: применить", m.p(1) + "run scoreboard players operation #mode " + m.L + " = #tg " + m.L);
        m.a("Режим: сообщение", m.p(1) + "run " + act(8, 1, 6, "Режим светофора изменён", "aqua"));
        m.a("Аварийная пауза: включить?", m.p(2) + "if score #em " + m.L + " matches 0 run " + m.sc("#tg", 1));
        m.a("Аварийная пауза: выключить?", m.p(2) + "if score #em " + m.L + " matches 1 run " + m.sc("#tg", 0));
        m.a("Аварийная пауза: применить", m.p(2) + "run scoreboard players operation #em " + m.L + " = #tg " + m.L);
        String[] on = {"red_concrete", "yellow_concrete", "lime_concrete"};
        int[][] ev = {{1, 0, 2}, {101, 0, 1}, {121, 0, 0}, {161, 1, 2}, {261, 1, 1}, {281, 1, 0}};
        for (int[] e : ev) {
            int[][] gr = e[1] == 0 ? new int[][]{heads[0], heads[1]} : new int[][]{heads[2], heads[3]};
            for (int i = 0; i < 2; i++) {
                m.a("Светофор " + (e[1] == 0 ? "NS" : "EW") + (i + 1) + ": погасить (кадр " + e[0] + ")", m.f(e[0]) + "run " + fill(gr[i][0], 2, gr[i][1], gr[i][0], 4, gr[i][1], "gray_concrete"));
                String col = e[2] == 0 ? on[0] : e[2] == 1 ? on[1] : on[2];
                int y = e[2] == 0 ? 4 : e[2] == 1 ? 3 : 2;
                m.a("Светофор " + (e[1] == 0 ? "NS" : "EW") + (i + 1) + ": свет (кадр " + e[0] + ")", m.f(e[0]) + "run " + set(gr[i][0], y, gr[i][1], col));
            }
        }
        m.a("EW красный при старте: погасить", m.f(1) + "run " + fill(10, 2, 4, 10, 4, 4, "gray_concrete"));
        m.a("EW красный при старте 1", m.f(1) + "run " + set(10, 4, 4, "red_concrete"));
        m.a("EW красный при старте 2", m.f(1) + "run " + fill(6, 2, 8, 6, 4, 8, "gray_concrete"));
        m.a("EW красный при старте 3", m.f(1) + "run " + set(6, 4, 8, "red_concrete"));
        for (int[] ev2 : new int[][]{{121, 1}, {161, 0}, {281, 1}, {320, 0}}) {
            for (int i = 0; i < 4; i++)
                m.a("Пешеходный " + (i + 1) + (ev2[1] == 1 ? ": зелёный" : ": красный") + " (кадр " + ev2[0] + ")", m.f(ev2[0] == 320 ? 1 : ev2[0]) + "run " + set(ped[i][0], 0, ped[i][1], ev2[1] == 1 ? "lime_concrete" : "red_concrete"));
        }
        m.a("Мигание: счёт", m.is("#mode", 1) + "run " + m.ad("#bt", 1));
        for (int i = 0; i < 4; i++) {
            m.a("Мигание: жёлтый " + (i + 1), m.is("#mode", 1) + "if score #bt " + m.L + " matches 10 run " + fill(heads[i][0], 2, heads[i][1], heads[i][0], 4, heads[i][1], "gray_concrete"));
            m.a("Мигание: свет " + (i + 1), m.is("#mode", 1) + "if score #bt " + m.L + " matches 10 run " + set(heads[i][0], 3, heads[i][1], "yellow_concrete"));
            m.a("Мигание: погасить " + (i + 1), m.is("#mode", 1) + "if score #bt " + m.L + " matches 20 run " + fill(heads[i][0], 2, heads[i][1], heads[i][0], 4, heads[i][1], "gray_concrete"));
        }
        m.a("Мигание: сброс счёта", m.isr("#bt", "20..") + "run " + m.sc("#bt", 0));
        m.a("Выход из мигания", m.p(1) + "if score #mode " + m.L + " matches 0 run " + m.sc("#f", 1));
        int[] cl = {1, 101, 121, 161, 261, 281};
        for (int c : cl) m.a("Щелчок фазы " + c, m.f(c) + "run " + snd(8, 1, 6, "block.note_block.hat", "1", "1.2"));
        m.a("Пауза: красные", m.is("#em", 1) + "run " + fill(6, 2, 4, 6, 4, 4, "red_concrete"));
    }

    // ---------------------------------------------------------------- 4. дискотека
    static void disco(M m) {
        var b = m.b;
        b.part("Танцпол");
        b.box(3, -1, 3, 12, -1, 12, "black_concrete");
        b.box(4, 0, 4, 11, 0, 11, "gray_concrete");
        b.box(2, 0, 3, 2, 3, 12, "black_concrete");
        b.box(13, 0, 3, 13, 3, 12, "black_concrete");
        b.set(3, 4, 7, "gray_concrete");
        b.set(12, 4, 7, "gray_concrete");
        m.init.addAll(java.util.List.of(m.sc("#on", 0), m.sc("#bt", 0), m.sc("#per", 12), m.sc("#pat", 0), m.sc("#chg", 0)));
        m.a("Включить", m.p(0) + "run " + m.sc("#on", 1));
        m.a("Выключить", m.p(1) + "run " + m.sc("#on", 0));
        m.a("Выкл: пол", m.p(1) + "run " + fill(4, 0, 4, 11, 0, 11, "gray_concrete"));
        m.a("Выкл: прожектор 1", m.p(1) + "run " + set(3, 4, 7, "gray_concrete"));
        m.a("Выкл: прожектор 2", m.p(1) + "run " + set(12, 4, 7, "gray_concrete"));
        m.a("Быстрее", m.p(2) + "if score #per " + m.L + " matches 4.. run " + m.ad("#per", -2));
        m.a("Медленнее", m.p(3) + "if score #per " + m.L + " matches ..28 run " + m.ad("#per", 2));
        m.a("Сообщение темпа", "execute if score #p2 " + m.L + " matches 1 run " + act(8, 1, 6, "Темп быстрее", "aqua"));
        m.a("Сообщение темпа 2", "execute if score #p3 " + m.L + " matches 1 run " + act(8, 1, 6, "Темп медленнее", "aqua"));
        m.a("Бит: счёт", m.is("#on", 1) + "run " + m.ad("#bt", 1));
        m.a("Бит: узор", m.is("#on", 1) + "if score #bt " + m.L + " >= #per " + m.L + " run " + m.ad("#pat", 1));
        m.a("Бит: флаг", m.is("#on", 1) + "if score #bt " + m.L + " >= #per " + m.L + " run " + m.sc("#chg", 1));
        m.a("Бит: сброс счёта", m.is("#on", 1) + "if score #bt " + m.L + " >= #per " + m.L + " run " + m.sc("#bt", 0));
        m.a("Узор: по кругу", m.isr("#pat", "12..") + "run " + m.sc("#pat", 0));
        for (int p = 0; p < 12; p++) {
            for (int r = 0; r < 4; r++)
                m.a("Узор " + (p + 1) + ": ряд " + (r + 1), m.is("#chg", 1) + "if score #pat " + m.L + " matches " + p + " run " + fill(4, 0, 4 + 2 * r, 11, 0, 5 + 2 * r, cc(p * 3 + r * 5)));
            m.a("Узор " + (p + 1) + ": прожектор 1", m.is("#chg", 1) + "if score #pat " + m.L + " matches " + p + " run " + set(3, 4, 7, cc(p * 5 + 2)));
            m.a("Узор " + (p + 1) + ": прожектор 2", m.is("#chg", 1) + "if score #pat " + m.L + " matches " + p + " run " + set(12, 4, 7, cc(p * 7 + 9)));
        }
        m.a("Бас", m.is("#chg", 1) + "run " + snd(8, 1, 6, "block.note_block.basedrum", "2", "1"));
        m.a("Тарелка", m.is("#chg", 1) + "if score #bt " + m.L + " matches 0 run " + snd(8, 1, 6, "block.note_block.hat", "1", "1.6"));
        for (int i = 0; i < 6; i++)
            m.a("Ноты " + (i + 1), m.is("#chg", 1) + "run " + par(4 + 2 * i, 1, 7, "note ~ ~1 ~ 0.4 0.3 0.4 1 1"));
        m.a("Сброс флага бита", m.sc("#chg", 0));
    }

    // ---------------------------------------------------------------- 5. радар
    static void radar(M m) {
        var b = m.b;
        b.part("Башня радара");
        b.box(5, 0, 4, 9, 0, 8, "stone_bricks");
        b.box(7, 1, 6, 7, 5, 6, "iron_bars");
        b.box(6, 6, 5, 8, 6, 7, "gray_concrete");
        b.set(7, 7, 6, "lightning_rod");
        m.init.addAll(java.util.List.of(m.sc("#on", 0), m.sc("#far", 0), m.sc("#tk", 0), m.sc("#sc", 0), m.sc("#ph", 0), m.sc("#hos", 0), m.sc("#pas", 0), m.sc("#vil", 0), m.sc("#c", 0)));
        m.a("Включить", m.p(0) + "run " + m.sc("#on", 1));
        m.a("Выключить", m.p(1) + "run " + m.sc("#on", 0));
        m.a("Дальность: ближняя?", m.p(2) + "if score #far " + m.L + " matches 0 run " + m.sc("#tg", 1));
        m.a("Дальность: дальняя?", m.p(2) + "if score #far " + m.L + " matches 1 run " + m.sc("#tg", 0));
        m.a("Дальность: применить", m.p(2) + "run scoreboard players operation #far " + m.L + " = #tg " + m.L);
        m.a("Дальность: сообщение", m.p(2) + "run " + act(7, 1, 2, "Дальность радара изменена", "aqua"));
        m.a("Антенна: счёт", m.is("#on", 1) + "run " + m.ad("#ph", 1));
        m.a("Антенна: круг", m.isr("#ph", "8..") + "run " + m.sc("#ph", 0));
        int[][] off = {{2, 0}, {1, 1}, {0, 2}, {-1, 1}, {-2, 0}, {-1, -1}, {0, -2}, {1, -1}};
        for (int k = 0; k < 8; k++) {
            int pr = (k + 7) % 8;
            m.a("Антенна " + (k + 1) + ": убрать прошлую", m.is("#ph", k) + "if score #on " + m.L + " matches 1 run " + set(7 + off[pr][0], 7, 6 + off[pr][1], "air"));
            m.a("Антенна " + (k + 1) + ": поставить", m.is("#ph", k) + "if score #on " + m.L + " matches 1 run " + set(7 + off[k][0], 7, 6 + off[k][1], "end_rod"));
        }
        m.a("Счёт опроса", m.is("#on", 1) + "run " + m.ad("#tk", 1));
        m.a("Флаг опроса", m.isr("#tk", "20..") + "run " + m.sc("#sc", 1));
        m.a("Сброс опроса", m.isr("#tk", "20..") + "run " + m.sc("#tk", 0));
        m.a("Обнулить врагов", m.is("#sc", 1) + "run " + m.sc("#hos", 0));
        m.a("Обнулить мирных", m.is("#sc", 1) + "run " + m.sc("#pas", 0));
        m.a("Обнулить жителей", m.is("#sc", 1) + "run " + m.sc("#vil", 0));
        String[] foes = {"#minecraft:undead", "minecraft:creeper", "minecraft:spider", "minecraft:cave_spider", "minecraft:witch", "#minecraft:raiders", "minecraft:slime", "minecraft:magma_cube", "minecraft:phantom", "minecraft:blaze", "minecraft:silverfish"};
        for (int far = 0; far <= 1; far++) {
            int r = far == 0 ? 24 : 48;
            for (String t : foes) {
                m.a("Враги " + t.replace("minecraft:", "").replace("#", "") + " (" + r + ")", m.is("#sc", 1) + "if score #far " + m.L + " matches " + far + " run execute store result score #c " + m.L + " positioned " + ph3(7, 3, 6) + " if entity @e[type=" + t + ",distance=.." + r + "]");
                m.a("Сумма врагов (" + r + ")", m.is("#sc", 1) + "if score #far " + m.L + " matches " + far + " run scoreboard players operation #hos " + m.L + " += #c " + m.L);
            }
        }
        String[] pas = {"minecraft:cow", "minecraft:pig", "minecraft:sheep", "minecraft:chicken", "minecraft:villager"};
        for (String t : pas) {
            String who = t.endsWith("villager") ? "#vil" : "#pas";
            m.a("Подсчёт " + t.replace("minecraft:", ""), m.is("#sc", 1) + "run execute store result score #c " + m.L + " positioned " + ph3(7, 3, 6) + " if entity @e[type=" + t + ",distance=..48]");
            m.a("Сумма " + t.replace("minecraft:", ""), m.is("#sc", 1) + "run scoreboard players operation " + who + " " + m.L + " += #c " + m.L);
        }
        m.a("Табло радара", m.is("#sc", 1) + "run " + at(7, 1, 6) + "title @a[distance=..30] actionbar [{\"text\":\"Враги: \",\"color\":\"red\"},{\"score\":{\"name\":\"#hos\",\"objective\":\"" + m.L + "\"},\"color\":\"red\"},{\"text\":\"  Животные: \",\"color\":\"green\"},{\"score\":{\"name\":\"#pas\",\"objective\":\"" + m.L + "\"},\"color\":\"green\"},{\"text\":\"  Жители: \",\"color\":\"aqua\"},{\"score\":{\"name\":\"#vil\",\"objective\":\"" + m.L + "\"},\"color\":\"aqua\"}]");
        m.a("Тревога: звук", m.is("#sc", 1) + "if score #hos " + m.L + " matches 1.. run " + snd(7, 3, 6, "block.note_block.bell", "2", "0.6"));
        m.a("Тревога: лампа красная", m.is("#sc", 1) + "if score #hos " + m.L + " matches 1.. run " + set(6, 1, 0, "red_concrete"));
        m.a("Чисто: лампа зелёная", m.is("#sc", 1) + "if score #hos " + m.L + " matches 0 run " + set(6, 1, 0, "lime_concrete"));
        m.a("Импульс радара", m.is("#sc", 1) + "run " + par(7, 7, 6, "end_rod ~ ~ ~ 2 0.1 2 0.05 40"));
        m.a("Сброс флага опроса", m.sc("#sc", 0));
    }

    // ---------------------------------------------------------------- 6. телепорт-хаб
    static void tphub(M m) {
        var b = m.b;
        b.part("Центральная площадка");
        b.box(7, 0, 5, 9, 0, 7, "polished_blackstone");
        b.set(8, 0, 6, "sea_lantern");
        int[][] pads = {{2, 2}, {14, 2}, {2, 10}, {14, 10}};
        b.part("Площадки назначения");
        for (int[] p : pads) { b.box(p[0] - 1, 0, p[1] - 1, p[0] + 1, 0, p[1] + 1, "quartz_block"); b.set(p[0], 0, p[1], "sea_lantern"); }
        m.init.addAll(java.util.List.of(m.sc("#dst", 0)));
        for (int i = 0; i < 4; i++) {
            m.a("Пункт " + (i + 1) + ": выбор", m.p(i) + "if score #f " + m.L + " matches 0 run " + m.sc("#dst", i + 1));
            m.a("Пункт " + (i + 1) + ": старт", m.p(i) + "if score #f " + m.L + " matches 0 run " + m.sc("#f", 1));
        }
        m.a("Отмена: счётчик", m.p(4) + "run " + m.sc("#f", 0));
        m.a("Отмена: пункт", m.p(4) + "run " + m.sc("#dst", 0));
        m.a("Отмена: сообщение", m.p(4) + "run " + act(8, 1, 6, "Телепорт отменён", "red"));
        for (int i = 0; i < 4; i++) {
            m.a("Площадка " + (i + 1) + ": свет", m.f(1) + "if score #dst " + m.L + " matches " + (i + 1) + " run " + set(pads[i][0], 0, pads[i][1], "light_blue_concrete"));
            m.a("Площадка " + (i + 1) + ": погасить", m.f(75) + "run " + set(pads[i][0], 0, pads[i][1], "sea_lantern"));
            m.a("Объявление пункта " + (i + 1), m.f(1) + "if score #dst " + m.L + " matches " + (i + 1) + " run " + act(8, 1, 6, "Пункт назначения " + (i + 1), "aqua"));
            m.a("Перенос на пункт " + (i + 1), m.f(70) + "if score #dst " + m.L + " matches " + (i + 1) + " run execute positioned " + ph3(8, 1, 6) + " as @a[distance=..1.8] positioned " + ph3(pads[i][0], 1, pads[i][1]) + " run tp @s ~0.5 ~ ~0.5");
            m.a("Прибытие: частицы " + (i + 1), m.f(70) + "if score #dst " + m.L + " matches " + (i + 1) + " run " + par(pads[i][0], 1, pads[i][1], "reverse_portal ~0.5 ~ ~0.5 0.5 1 0.5 0.3 60"));
            m.a("Прибытие: звук " + (i + 1), m.f(70) + "if score #dst " + m.L + " matches " + (i + 1) + " run " + snd(pads[i][0], 1, pads[i][1], "entity.enderman.teleport", "1", "1"));
        }
        for (int k = 0; k < 12; k++) {
            double a = Math.PI * 2 * k / 12;
            int ox = (int) Math.round(Math.cos(a) * 2), oz = (int) Math.round(Math.sin(a) * 2);
            m.a("Спираль заряда " + (k + 1), m.f(5 * (k + 1)) + "run " + par(8 + ox, 1, 6 + oz, "portal ~0.5 ~ ~0.5 0.1 1.2 0.1 0.5 25"));
        }
        m.a("Отсчёт 3", m.f(20) + "run " + ttl(8, 1, 6, "3", "aqua"));
        m.a("Отсчёт 2", m.f(40) + "run " + ttl(8, 1, 6, "2", "aqua"));
        m.a("Отсчёт 1", m.f(60) + "run " + ttl(8, 1, 6, "1", "aqua"));
        for (int f : new int[]{1, 20, 40, 60}) m.a("Звук заряда " + f, m.f(f) + "run " + snd(8, 1, 6, "block.beacon.ambient", "1.5", String.valueOf(0.6 + f / 80.0)));
        m.a("Вспышка отправления", m.f(70) + "run " + par(8, 1, 6, "flash ~0.5 ~1 ~0.5 0 0 0 0 1"));
        m.a("Звук отправления", m.f(70) + "run " + snd(8, 1, 6, "entity.enderman.teleport", "1.5", "0.8"));
        m.a("Лампы центра: заряд", m.fr(1, 69) + "run " + set(8, 0, 6, "light_blue_concrete"));
        m.a("Лампы центра: готово", m.f(75) + "run " + set(8, 0, 6, "sea_lantern"));
        m.a("Подсказка: нет игрока", m.p(0) + "unless entity @a[distance=..1.8] run " + act(8, 1, 6, "Встань на центральную площадку", "yellow"));
        m.frames(76);
        m.a("Конец: пункт", m.f(0) + "run " + m.sc("#dst", 0));
        for (int i = 0; i < 4; i++) {
            m.a("Площадка " + (i + 1) + ": мерцание", m.is("#f", 0) + "run " + par(pads(i)[0], 1, pads(i)[1], "end_rod ~0.5 ~ ~0.5 1 0.1 1 0.01 1"));
            m.a("Нет игрока: пункт " + (i + 1), m.p(i) + "unless entity @a[distance=..1.8] run " + snd(8, 1, 6, "block.note_block.bass", "1", "0.6"));
            m.a("Центр: пункт " + (i + 1) + " выбран", m.p(i) + "run " + set(7 + (i % 2) * 2, 0, 5 + (i / 2) * 2, "light_blue_concrete"));
            m.a("Центр: вернуть " + (i + 1), m.f(75) + "run " + set(7 + (i % 2) * 2, 0, 5 + (i / 2) * 2, "polished_blackstone"));
        }
    }

    // ---------------------------------------------------------------- 7. раскладной мост
    static void bridge(M m) {
        var b = m.b;
        b.part("Опоры моста");
        b.box(0, -1, 4, 3, -1, 8, "stone_bricks");
        b.box(28, -1, 4, 31, -1, 8, "stone_bricks");
        b.box(3, 0, 4, 3, 1, 4, "oak_fence");
        b.box(3, 0, 8, 3, 1, 8, "oak_fence");
        m.init.addAll(java.util.List.of(m.sc("#db", 0), m.sc("#pz", 0), m.sc("#tg", 0)));
        m.a("Разложить: старт", m.p(0) + "if score #f " + m.L + " matches 0 run " + m.sc("#db", 1));
        m.a("Сложить: старт", m.p(1) + "if score #f " + m.L + " matches 0 run " + m.sc("#db", 2));
        m.a("Запуск моста", "execute if score #db " + m.L + " matches 1..2 if score #f " + m.L + " matches 0 run " + m.sc("#f", 1));
        m.a("Пауза: включить?", m.p(2) + "if score #pz " + m.L + " matches 0 run " + m.sc("#tg", 1));
        m.a("Пауза: выключить?", m.p(2) + "if score #pz " + m.L + " matches 1 run " + m.sc("#tg", 0));
        m.a("Пауза: применить", m.p(2) + "run scoreboard players operation #pz " + m.L + " = #tg " + m.L);
        for (int i = 0; i < 12; i++) {
            int x = 4 + 2 * i;
            int fe = 4 + 4 * i, fr = 4 + 4 * (11 - i);
            String E = m.is("#db", 1) + "if score #f " + m.L + " matches " + fe + " if score #pz " + m.L + " matches 0 run ";
            String R = m.is("#db", 2) + "if score #f " + m.L + " matches " + fr + " if score #pz " + m.L + " matches 0 run ";
            m.a("Секция " + (i + 1) + ": настил", E + fill(x, -1, 5, x + 1, -1, 7, "polished_andesite"));
            m.a("Секция " + (i + 1) + ": перила 1", E + fill(x, 0, 4, x + 1, 0, 4, "oak_fence"));
            m.a("Секция " + (i + 1) + ": перила 2", E + fill(x, 0, 8, x + 1, 0, 8, "oak_fence"));
            m.a("Секция " + (i + 1) + ": убрать настил", R + fill(x, -1, 5, x + 1, -1, 7, "air"));
            m.a("Секция " + (i + 1) + ": убрать перила 1", R + fill(x, 0, 4, x + 1, 0, 4, "air"));
            m.a("Секция " + (i + 1) + ": убрать перила 2", R + fill(x, 0, 8, x + 1, 0, 8, "air"));
            m.a("Секция " + (i + 1) + ": звук раскладки", E + snd(x, 0, 6, "block.piston.extend", "1", "0.8"));
            m.a("Секция " + (i + 1) + ": звук складки", R + snd(x, 0, 6, "block.piston.contract", "1", "0.8"));
        }
        m.a("Счёт кадров моста", m.isr("#f", "1..") + "if score #pz " + m.L + " matches 0 run " + m.ad("#f", 1));
        m.a("Конец моста", m.isr("#f", "52..") + "run " + m.sc("#f", 0));
        m.a("Конец: направление", m.f(0) + "run " + m.sc("#db", 0));
        m.a("Табло: разложен", m.is("#f", 52) + "if score #db " + m.L + " matches 1 run " + act(2, 1, 6, "Мост разложен", "green"));
        m.a("Табло: сложен", m.is("#f", 52) + "if score #db " + m.L + " matches 2 run " + act(2, 1, 6, "Мост сложен", "yellow"));
        m.a("Лампа пульта", m.isr("#f", "1..") + "run " + set(4, 1, 0, "orange_concrete"));
        m.a("Лампа пульта: покой", m.f(0) + "run " + set(4, 1, 0, "red_concrete"));
        m.a("Пауза: сообщение", m.p(2) + "run " + act(2, 1, 6, "Пауза или продолжение", "aqua"));
    }

    // ---------------------------------------------------------------- 8. сигнализация
    static void alarm(M m) {
        var b = m.b;
        b.part("Охраняемая комната");
        b.hol(2, -1, 3, 14, 5, 11, "stone_bricks");
        b.air(3, 0, 4, 13, 4, 10);
        b.box(8, 0, 3, 8, 2, 3, "air");
        b.box(8, 0, 11, 8, 2, 11, "air");
        for (int[] c : new int[][]{{3, 4}, {13, 4}, {3, 10}, {13, 10}}) { b.set(c[0], 4, c[1], "gray_concrete"); b.set(c[0], 0, c[1], "chiseled_stone_bricks"); }
        m.init.addAll(java.util.List.of(m.sc("#arm", 0), m.sc("#cd", 0), m.sc("#al", 0), m.sc("#sr", 0)));
        m.a("Вооружить: старт отсчёта", m.p(0) + "if score #arm " + m.L + " matches 0 if score #cd " + m.L + " matches 0 run " + m.sc("#cd", 100));
        m.a("Вооружить: сообщение", m.p(0) + "run " + act(8, 1, 6, "Охрана включится через 5 секунд", "yellow"));
        m.a("Отсчёт идёт", m.isr("#cd", "1..") + "run " + m.ad("#cd", -1));
        for (int c : new int[]{80, 60, 40, 20}) m.a("Отсчёт: " + c / 20 + " с", m.is("#cd", c) + "run " + ttl(8, 1, 6, String.valueOf(c / 20), "yellow"));
        m.a("Охрана включена", m.is("#cd", 1) + "run " + m.sc("#arm", 1));
        m.a("Охрана: сообщение", m.is("#cd", 1) + "run " + ttl(8, 1, 6, "Охрана включена", "green"));
        m.a("Охрана: лампа", m.is("#cd", 1) + "run " + set(4, 1, 0, "lime_concrete"));
        m.a("Снять: охрана", m.p(1) + "run " + m.sc("#arm", 0));
        m.a("Снять: отсчёт", m.p(1) + "run " + m.sc("#cd", 0));
        m.a("Снять: тревога", m.p(1) + "run " + m.sc("#al", 0));
        m.a("Снять: лампа", m.p(1) + "run " + set(4, 1, 0, "red_concrete"));
        m.a("Снять: выход 1", m.p(1) + "run " + fill(8, 0, 3, 8, 2, 3, "air"));
        m.a("Снять: выход 2", m.p(1) + "run " + fill(8, 0, 11, 8, 2, 11, "air"));
        m.a("Снять: сообщение", m.p(1) + "run " + act(8, 1, 6, "Охрана снята", "aqua"));
        m.a("Тест", m.p(2) + "if score #al " + m.L + " matches 0 run " + m.sc("#al", 1));
        m.a("Датчик движения", m.is("#arm", 1) + "if score #al " + m.L + " matches 0 positioned " + ph3(3, 0, 4) + " if entity @a[dx=10,dy=3,dz=6] run " + m.sc("#al", 1));
        m.a("Тревога: счёт", m.isr("#al", "1..") + "run " + m.ad("#al", 1));
        m.a("Тревога: заголовок", m.is("#al", 2) + "run " + ttl(8, 1, 6, "ТРЕВОГА", "red"));
        for (int r = 0; r < 3; r++) {
            m.a("Блокировка выхода 1: ряд " + (r + 1), m.is("#al", 4 + 2 * r) + "run " + fill(8, r, 3, 8, r, 3, "iron_bars"));
            m.a("Блокировка выхода 2: ряд " + (r + 1), m.is("#al", 4 + 2 * r) + "run " + fill(8, r, 11, 8, r, 11, "iron_bars"));
            m.a("Открытие выхода 1: ряд " + (3 - r), m.is("#al", 400 + 2 * r) + "run " + fill(8, 2 - r, 3, 8, 2 - r, 3, "air"));
            m.a("Открытие выхода 2: ряд " + (3 - r), m.is("#al", 400 + 2 * r) + "run " + fill(8, 2 - r, 11, 8, 2 - r, 11, "air"));
            m.a("Звук блокировки " + (r + 1), m.is("#al", 4 + 2 * r) + "run " + snd(8, 1, 6, "block.iron_door.close", "1.5", "0.8"));
        }
        m.a("Сирена: счёт", m.isr("#al", "1..") + "run " + m.ad("#sr", 1));
        m.a("Сирена: звук", m.isr("#sr", "10..") + "run " + snd(8, 2, 6, "block.note_block.bell", "3", "0.5"));
        m.a("Сирена: сброс", m.isr("#sr", "10..") + "run " + m.sc("#sr", 0));
        int[][] cn = {{3, 4}, {13, 4}, {3, 10}, {13, 10}};
        for (int i = 0; i < 4; i++) {
            m.a("Лампа сирены " + (i + 1) + ": красная", m.isr("#al", "1..") + "if score #sr " + m.L + " matches 5 run " + set(cn[i][0], 4, cn[i][1], "red_concrete"));
            m.a("Лампа сирены " + (i + 1) + ": погасить", m.isr("#al", "1..") + "if score #sr " + m.L + " matches 0 run " + set(cn[i][0], 4, cn[i][1], "gray_concrete"));
            m.a("Луч сирены " + (i + 1), m.isr("#al", "1..") + "run " + par(cn[i][0], 1, cn[i][1], "end_rod ~0.5 ~ ~0.5 0 1.5 0 0 2"));
        }
        for (int t = 1; t <= 3; t++) m.a("Напоминание " + t, m.is("#al", 100 * t) + "run " + act(8, 1, 6, "Нарушитель в охраняемой зоне", "red"));
        m.a("Конец тревоги", m.isr("#al", "406..") + "run " + m.sc("#al", 0));
        m.a("Конец тревоги: лампы", m.is("#al", 406) + "run " + set(4, 1, 0, "lime_concrete"));
        m.init.addAll(java.util.List.of(m.sc("#cnt", 0)));
        for (int i = 0; i < 6; i++) m.a("Лазерная линия " + (i + 1), m.is("#arm", 1) + "run " + par(3, 1, 4 + i, "dust 1 0 0 0.6 ~0 ~0.5 ~0.5 5 0.1 0 0 8"));
        m.a("Журнал: счёт нарушений", m.is("#al", 2) + "run " + m.ad("#cnt", 1));
        m.a("Журнал: табло", m.p(1) + "run " + at(8, 1, 6) + "title @a[distance=..20] actionbar [{\"text\":\"Нарушений: \",\"color\":\"gray\"},{\"score\":{\"name\":\"#cnt\",\"objective\":\"" + m.L + "\"},\"color\":\"red\"}]");
        for (int i = 0; i < 4; i++) {
            m.a("Охрана: мигание " + (i + 1) + " вкл", m.isr("#cd", "1..") + "if score #cd " + m.L + " matches " + (95 - 20 * i) + " run " + set(4, 1, 0, "orange_concrete"));
            m.a("Охрана: мигание " + (i + 1) + " выкл", m.isr("#cd", "1..") + "if score #cd " + m.L + " matches " + (85 - 20 * i) + " run " + set(4, 1, 0, "red_concrete"));
        }
        int[][] cn2 = {{3, 4}, {13, 4}, {13, 10}, {3, 10}};
        for (int i = 0; i < 4; i++) m.a("Бегущий сигнал " + (i + 1), m.isr("#al", "1..") + "if score #sr " + m.L + " matches " + (i * 2) + " run " + par(cn2[i][0], 3, cn2[i][1], "flame ~0.5 ~ ~0.5 0.1 0.1 0.1 0.01 6"));
    }

    static int[] pads(int i) { int[][] p = {{2, 2}, {14, 2}, {2, 10}, {14, 10}}; return p[i]; }

    private NeroMechA() { }
}
