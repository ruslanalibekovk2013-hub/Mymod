package com.mackros;

import static com.mackros.NeroMech.*;

/** Механизмы 16-25. */
final class NeroMechC {
    static String cc(int i) { return NeroMechA.cc(i); }

    static final String[] FOES = {"#minecraft:undead", "minecraft:creeper", "minecraft:spider", "minecraft:cave_spider", "minecraft:witch", "#minecraft:raiders", "minecraft:slime", "minecraft:magma_cube", "minecraft:phantom", "minecraft:blaze", "minecraft:silverfish"};

    // ---------------------------------------------------------------- 16. ракета
    static void rocket(M m) {
        var b = m.b;
        b.part("Стартовый стол");
        b.box(3, -1, 3, 13, -1, 13, "smooth_stone");
        b.box(6, 0, 6, 10, 0, 10, "iron_block");
        b.part("Ракета");
        b.box(7, 1, 7, 9, 9, 9, "quartz_block");
        b.box(8, 10, 8, 8, 12, 8, "red_concrete");
        b.box(7, 10, 7, 9, 10, 9, "red_concrete");
        b.box(6, 1, 8, 6, 3, 8, "red_concrete");
        b.box(10, 1, 8, 10, 3, 8, "red_concrete");
        b.box(8, 1, 6, 8, 3, 6, "red_concrete");
        b.box(8, 1, 10, 8, 3, 10, "red_concrete");
        b.air(8, 1, 8, 8, 2, 8);
        b.part("Башня обслуживания");
        b.box(4, 0, 8, 4, 10, 8, "iron_bars");
        b.box(5, 5, 8, 6, 5, 8, "iron_bars");
        b.box(5, 8, 8, 6, 8, 8, "iron_bars");
        m.init.addAll(java.util.List.of(m.sc("#fuel", 0)));
        m.a("Заправка", m.p(2) + "run " + m.ad("#fuel", 25));
        m.a("Заправка: предел", m.isr("#fuel", "101..") + "run " + m.sc("#fuel", 100));
        m.a("Заправка: звук", m.p(2) + "run " + snd(8, 1, 8, "item.bucket.fill_lava", "1", "1"));
        m.a("Заправка: табло", m.p(2) + "run " + act(8, 1, 6, "Топливо добавлено", "gold"));
        m.a("Запуск: топливо есть", m.p(0) + "if score #fuel " + m.L + " matches 50.. if score #f " + m.L + " matches 0 run " + m.sc("#f", 1));
        m.a("Запуск: мало топлива", m.p(0) + "if score #fuel " + m.L + " matches ..49 run " + act(8, 1, 6, "Нужно минимум 50 топлива (кнопка ЗАПРАВКА)", "red"));
        m.a("Отмена: счёт", m.p(1) + "run " + m.sc("#f", 0));
        m.a("Отмена: заголовок", m.p(1) + "run " + ttl(8, 1, 6, "Запуск отменён", "red"));
        m.a("Отмена: лампа", m.p(1) + "run " + set(4, 1, 0, "red_concrete"));
        for (int i = 0; i < 10; i++) {
            int f = 1 + 20 * i;
            m.a("Отсчёт " + (10 - i), m.f(f) + "run " + ttl(8, 1, 6, String.valueOf(10 - i), i < 7 ? "yellow" : "red"));
            m.a("Сигнал " + (10 - i), m.f(f) + "run " + snd(8, 1, 6, "block.note_block.pling", "2", i < 7 ? "1" : "1.6"));
            m.a("Огни стола " + (10 - i), m.f(f) + "run " + fill(3, -1, 3, 13, -1, 3, i % 2 == 0 ? "orange_concrete" : "yellow_concrete"));
        }
        m.a("Лампа: отсчёт", m.f(1) + "run " + set(4, 1, 0, "orange_concrete"));
        m.a("Башня: отвод 1", m.f(100) + "run " + fill(5, 5, 8, 6, 5, 8, "air"));
        m.a("Башня: отвод 2", m.f(110) + "run " + fill(5, 8, 8, 6, 8, 8, "air"));
        m.a("Башня: звук", m.f(100) + "run " + snd(5, 5, 8, "block.piston.contract", "1.5", "0.7"));
        m.a("Зажигание: дым", m.fr(200, 259) + "run " + par(8, 0, 8, "large_smoke ~0.5 ~0.2 ~0.5 2 0.2 2 0.05 12"));
        m.a("Зажигание: огонь", m.fr(200, 259) + "run " + par(8, 0, 8, "flame ~0.5 ~0.2 ~0.5 0.5 0.1 0.5 0.1 10"));
        m.a("Зажигание: гул", m.f(200) + "run " + snd(8, 1, 8, "entity.generic.explode", "2", "0.5"));
        m.a("Пуск: заголовок", m.f(260) + "run " + ttl(8, 1, 6, "ПУСК", "gold"));
        m.a("Пуск: топливо", m.f(260) + "run " + m.ad("#fuel", -50));
        m.a("Пуск: подъём игроков", m.f(260) + "run " + at(8, 1, 8) + "effect give @a[distance=..2.5] minecraft:levitation 9 5 true");
        m.a("Пуск: звук", m.f(260) + "run " + snd(8, 1, 8, "entity.firework_rocket.launch", "3", "0.5"));
        m.a("Пуск: взрыв", m.f(260) + "run " + par(8, 0, 8, "explosion_emitter ~0.5 ~ ~0.5 0 0 0 0 1"));
        for (int i = 0; i < 4; i++) m.a("Пуск: кольцо " + (i + 1), m.f(262 + 4 * i) + "run " + par(8, 1 + 3 * i, 8, "cloud ~0.5 ~ ~0.5 2.5 0.1 2.5 0.02 40"));
        m.a("Полёт: след", m.fr(260, 330) + "run " + at(8, 1, 8) + "particle minecraft:flame ~0.5 ~ ~0.5 0.4 0.2 0.4 0.05 8");
        m.a("Полёт: дым", m.fr(260, 330) + "run " + at(8, 1, 8) + "particle minecraft:large_smoke ~0.5 ~ ~0.5 0.6 0.2 0.6 0.02 6");
        m.a("Полёт: огонь двигателей", m.fr(260, 330) + "run " + par(8, 0, 8, "lava ~0.5 ~ ~0.5 0.8 0.1 0.8 0 3"));
        m.a("Мягкая посадка", m.f(330) + "run " + at(8, 1, 8) + "effect give @a[distance=..40] minecraft:slow_falling 25 0 true");
        m.a("Посадка: сообщение", m.f(420) + "run " + act(8, 1, 6, "Полёт завершён", "green"));
        m.a("Лампа: завершено", m.f(420) + "run " + set(4, 1, 0, "lime_concrete"));
        m.a("Топливо: табло", at(8, 1, 6) + "title @a[distance=..10] actionbar [{\"text\":\"Топливо: \",\"color\":\"gray\"},{\"score\":{\"name\":\"#fuel\",\"objective\":\"" + m.L + "\"},\"color\":\"gold\"}]");
        m.frames(430);
        for (int i = 0; i < 4; i++) {
            m.a("Шкала топлива " + (i + 1) + ": есть", m.isr("#fuel", (25 * (i + 1)) + "..") + "run " + set(3, i + 1, 3, "lime_concrete"));
            m.a("Шкала топлива " + (i + 1) + ": нет", m.isr("#fuel", ".." + (25 * (i + 1) - 1)) + "run " + set(3, i + 1, 3, "gray_concrete"));
        }
        for (int i = 0; i < 6; i++) m.a("Двигатели: стадия " + (i + 1), m.f(180 + 3 * i) + "run " + par(8, 0, 8, "smoke ~0.5 ~0.2 ~0.5 1 0.1 1 0.02 " + (6 + 4 * i)));
        m.a("Сирена старта", m.fr(1, 199) + "if score #f " + m.L + " matches 100 run " + snd(8, 1, 6, "block.note_block.bell", "3", "0.5"));
        m.a("Сирена: повтор", m.f(150) + "run " + snd(8, 1, 6, "block.note_block.bell", "3", "0.8"));
        m.a("Сирена: отмена", m.p(1) + "run " + snd(8, 1, 6, "block.note_block.bass", "3", "0.5"));
    }

    // ---------------------------------------------------------------- 17. салют
    static void fireworks(M m) {
        var b = m.b;
        b.part("Пусковая платформа");
        b.box(2, -1, 3, 18, -1, 9, "smooth_stone");
        for (int i = 0; i < 8; i++) { b.box(3 + 2 * i, 0, 6, 3 + 2 * i, 0, 6, "iron_block"); b.set(3 + 2 * i, 1, 6, "dispenser[facing=up]"); }
        int[] col = {16711680, 16744192, 16776960, 65280, 65535, 255, 16711935, 16777215};
        int[] typ = {1, 4, 2, 3, 1, 0, 4, 2};
        m.init.addAll(java.util.List.of(m.sc("#sh", 0)));
        for (int s = 0; s < 3; s++) {
            m.a("Шоу " + (s + 1) + ": старт", m.p(s) + "if score #f " + m.L + " matches 0 run " + m.sc("#sh", s + 1));
            m.a("Шоу " + (s + 1) + ": запуск", m.p(s) + "if score #f " + m.L + " matches 0 run " + m.sc("#f", 1));
        }
        m.a("Финал: старт", m.p(3) + "if score #f " + m.L + " matches 0 run " + m.sc("#sh", 4));
        m.a("Финал: запуск", m.p(3) + "if score #f " + m.L + " matches 0 run " + m.sc("#f", 1));
        for (int s = 1; s <= 3; s++) {
            for (int i = 0; i < 10; i++) {
                int tube = (i * (s + 1) + s) % 8, c = (i * 3 + s * 2) % 8, c2 = (c + 3) % 8, t = (typ[tube] + s + i) % 5;
                String sh = m.is("#sh", s) + "if score #f " + m.L + " matches " + (8 + 12 * i) + " run ";
                m.a("Шоу " + s + ": залп " + (i + 1), sh + "summon minecraft:firework_rocket " + ph3(3 + 2 * tube, 2, 6) + " {LifeTime:" + (22 + (i % 3) * 4) + ",FireworksItem:{id:\"minecraft:firework_rocket\",Count:1b,tag:{Fireworks:{Flight:2b,Explosions:[{Type:" + t + ",Colors:[I;" + col[c] + "],FadeColors:[I;" + col[c2] + "],Trail:1b,Flicker:1b}]}}}}");
                m.a("Шоу " + s + ": звук " + (i + 1), sh + snd(3 + 2 * tube, 1, 6, "entity.firework_rocket.launch", "2", "1"));
            }
        }
        for (int i = 0; i < 24; i++) {
            int tube = i % 8, c = (i * 5) % 8, c2 = (c + 4) % 8;
            m.a("Финал: залп " + (i + 1), m.is("#sh", 4) + "if score #f " + m.L + " matches " + (6 + 2 * i) + " run summon minecraft:firework_rocket " + ph3(3 + 2 * tube, 2, 6) + " {LifeTime:" + (20 + (i % 4) * 3) + ",FireworksItem:{id:\"minecraft:firework_rocket\",Count:1b,tag:{Fireworks:{Flight:2b,Explosions:[{Type:" + (i % 5) + ",Colors:[I;" + col[c] + "," + col[c2] + "],FadeColors:[I;16777215],Trail:1b,Flicker:1b}]}}}}");
        }
        for (int i = 0; i < 8; i++) m.a("Финал: звук " + (i + 1), m.is("#sh", 4) + "if score #f " + m.L + " matches " + (6 + 6 * i) + " run " + snd(10, 1, 6, "entity.firework_rocket.blast", "3", String.valueOf(0.7 + i * 0.08)));
        m.a("Огни платформы: шоу", m.isr("#f", "1..") + "run " + fill(2, -1, 3, 18, -1, 3, "yellow_concrete"));
        m.a("Огни платформы: покой", m.f(0) + "run " + fill(2, -1, 3, 18, -1, 3, "smooth_stone"));
        m.a("Заголовок шоу", m.f(2) + "run " + ttl(10, 1, 6, "Салют", "gold"));
        m.a("Счёт кадров", m.isr("#f", "1..") + "run " + m.ad("#f", 1));
        m.a("Конец шоу", m.isr("#f", "135..") + "run " + m.sc("#f", 0));
        m.a("Конец: сообщение", m.is("#f", 135) + "run " + act(10, 1, 6, "Шоу закончено", "aqua"));
        m.a("Конец: сброс шоу", m.f(0) + "run " + m.sc("#sh", 0));
    }

    // ---------------------------------------------------------------- 18. фонтан
    static void fountain(M m) {
        var b = m.b;
        b.part("Чаша");
        b.hol(3, -1, 3, 13, 0, 13, "stone_bricks");
        b.box(4, -1, 4, 12, -1, 12, "water");
        b.box(7, 0, 7, 9, 0, 9, "stone_bricks");
        b.box(8, 1, 8, 8, 3, 8, "stone_bricks");
        int[][] ring = new int[12][2];
        for (int k = 0; k < 12; k++) {
            double a = Math.PI * 2 * k / 12;
            ring[k][0] = 8 + (int) Math.round(Math.sin(a) * 6);
            ring[k][1] = 8 + (int) Math.round(Math.cos(a) * 6);
            b.set(ring[k][0], 0, ring[k][1], "gray_concrete");
        }
        m.init.addAll(java.util.List.of(m.sc("#on", 0), m.sc("#md", 1), m.sc("#ph", 0), m.sc("#tk", 0), m.sc("#pu", 0)));
        m.a("Включить", m.p(0) + "run " + m.sc("#on", 1));
        m.a("Выключить", m.p(1) + "run " + m.sc("#on", 0));
        m.a("Режим: шаг", m.p(2) + "run " + m.ad("#md", 1));
        m.a("Режим: круг", m.isr("#md", "4..") + "run " + m.sc("#md", 1));
        m.a("Режим: табло", m.p(2) + "run " + at(8, 2, 8) + "title @a[distance=..16] actionbar [{\"text\":\"Режим фонтана: \",\"color\":\"aqua\"},{\"score\":{\"name\":\"#md\",\"objective\":\"" + m.L + "\"},\"color\":\"gold\"}]");
        m.a("Фаза: такт", m.is("#on", 1) + "run " + m.ad("#tk", 1));
        m.a("Фаза: шаг", m.isr("#tk", "3..") + "run " + m.ad("#ph", 1));
        m.a("Фаза: сброс такта", m.isr("#tk", "3..") + "run " + m.sc("#tk", 0));
        m.a("Фаза: круг", m.isr("#ph", "12..") + "run " + m.sc("#ph", 0));
        m.a("Пульс: счёт", m.is("#on", 1) + "run " + m.ad("#pu", 1));
        m.a("Пульс: круг", m.isr("#pu", "40..") + "run " + m.sc("#pu", 0));
        for (int h = 0; h < 3; h++) m.a("Режим 1: центральная струя " + (h + 1), m.is("#on", 1) + "if score #md " + m.L + " matches 1 run " + par(8, 3 + h * 2, 8, "splash ~0.5 ~ ~0.5 0.2 0.5 0.2 0.1 12"));
        for (int d = 0; d < 4; d++) {
            int[][] dd = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            m.a("Режим 1: боковая струя " + (d + 1), m.is("#on", 1) + "if score #md " + m.L + " matches 1 run " + par(8 + dd[d][0] * 2, 2, 8 + dd[d][1] * 2, "falling_water ~0.5 ~ ~0.5 0.2 0.3 0.2 0 6"));
        }
        for (int h = 0; h < 4; h++) m.a("Режим 2: пульс высота " + (h + 1), m.is("#on", 1) + "if score #md " + m.L + " matches 2 if score #pu " + m.L + " matches " + (h * 10) + ".." + (h * 10 + 9) + " run " + par(8, 3 + h, 8, "splash ~0.5 ~ ~0.5 0.3 " + (h + 1) + " 0.3 0.2 20"));
        m.a("Режим 2: звук пульса", m.is("#on", 1) + "if score #md " + m.L + " matches 2 if score #pu " + m.L + " matches 0 run " + snd(8, 2, 8, "block.water.ambient", "1.5", "0.8"));
        for (int k = 0; k < 8; k++) {
            double a = Math.PI * 2 * k / 8;
            int ox = (int) Math.round(Math.sin(a) * 3), oz = (int) Math.round(Math.cos(a) * 3);
            m.a("Режим 3: луч " + (k + 1), m.is("#on", 1) + "if score #md " + m.L + " matches 3 if score #ph " + m.L + " matches " + k + ".." + (k + 4) + " run " + par(8 + ox, 2, 8 + oz, "splash ~0.5 ~ ~0.5 0.2 1.2 0.2 0.15 14"));
        }
        m.a("Режим 3: центр", m.is("#on", 1) + "if score #md " + m.L + " matches 3 run " + par(8, 4, 8, "bubble_pop ~0.5 ~ ~0.5 0.5 2 0.5 0.05 10"));
        for (int k = 0; k < 12; k++) {
            m.a("Кольцо: зажечь " + (k + 1), m.is("#on", 1) + "if score #ph " + m.L + " matches " + k + " run " + set(ring[k][0], 0, ring[k][1], cc(k + 3)));
            m.a("Кольцо: погасить " + (k + 1), m.is("#on", 1) + "if score #ph " + m.L + " matches " + ((k + 1) % 12) + " run " + set(ring[k][0], 0, ring[k][1], "gray_concrete"));
        }
        m.a("Звук воды", m.is("#on", 1) + "if score #tk " + m.L + " matches 0 run " + snd(8, 1, 8, "block.water.ambient", "0.6", "1"));
        m.a("Выкл: кольцо", m.is("#on", 0) + "if score #ph " + m.L + " matches 0 run " + set(ring[0][0], 0, ring[0][1], "gray_concrete"));
        m.a("Лампа: работа", m.is("#on", 1) + "run " + set(4, 1, 0, "lime_concrete"));
        m.a("Лампа: стоп", m.is("#on", 0) + "run " + set(4, 1, 0, "red_concrete"));
        m.a("Брызги по краю", m.is("#on", 1) + "run " + par(8, 0, 4, "splash ~0.5 ~0.5 ~0.5 3 0.1 0.2 0 6"));
        m.a("Брызги по краю 2", m.is("#on", 1) + "run " + par(8, 0, 12, "splash ~0.5 ~0.5 ~0.5 3 0.1 0.2 0 6"));
        m.a("Брызги по краю 3", m.is("#on", 1) + "run " + par(4, 0, 8, "splash ~0.5 ~0.5 ~0.5 0.2 0.1 3 0 6"));
        m.a("Брызги по краю 4", m.is("#on", 1) + "run " + par(12, 0, 8, "splash ~0.5 ~0.5 ~0.5 0.2 0.1 3 0 6"));
        for (int k = 0; k < 8; k++) m.a("Режим 2: кольцо брызг " + (k + 1), m.is("#on", 1) + "if score #md " + m.L + " matches 2 if score #ph " + m.L + " matches " + k + " run " + par(8 + (int) Math.round(Math.sin(Math.PI * k / 4) * 5), 0, 8 + (int) Math.round(Math.cos(Math.PI * k / 4) * 5), "splash ~0.5 ~0.5 ~0.5 0.3 0.2 0.3 0.1 8"));
        for (int i = 0; i < 4; i++) m.a("Режим 3: цветная вспышка " + (i + 1), m.is("#on", 1) + "if score #md " + m.L + " matches 3 if score #ph " + m.L + " matches " + (3 * i) + " run " + set(8, 4, 8, cc(i * 4 + 3)));
        for (int i = 0; i < 4; i++) m.a("Звук режима " + (i + 1), m.is("#on", 1) + "if score #ph " + m.L + " matches " + (3 * i) + " if score #tk " + m.L + " matches 0 run " + snd(8, 2, 8, "block.bubble_column.upwards_ambient", "1", String.valueOf(0.8 + i * 0.1)));
    }

    // ---------------------------------------------------------------- 19. решётка ворот
    static void portcullis(M m) {
        var b = m.b;
        b.part("Арка");
        b.box(3, 0, 5, 4, 10, 7, "stone_bricks");
        b.box(12, 0, 5, 13, 10, 7, "stone_bricks");
        b.box(3, 9, 5, 13, 10, 7, "stone_bricks");
        b.air(5, 0, 5, 11, 8, 7);
        b.part("Решётка");
        b.box(5, 0, 6, 11, 7, 6, "iron_bars");
        b.part("Цепи");
        b.box(5, 8, 5, 5, 8, 5, "chain");
        b.box(11, 8, 5, 11, 8, 5, "chain");
        m.init.addAll(java.util.List.of(m.sc("#dg", 0), m.sc("#pz", 0)));
        m.a("Поднять: старт", m.p(0) + "if score #f " + m.L + " matches 0 run " + m.sc("#dg", 1));
        m.a("Опустить: старт", m.p(1) + "if score #f " + m.L + " matches 0 run " + m.sc("#dg", 2));
        m.a("Запуск", "execute if score #dg " + m.L + " matches 1..2 if score #f " + m.L + " matches 0 run " + m.sc("#f", 1));
        m.toggle(2, "#pz", "Пауза");
        for (int r = 0; r < 8; r++) {
            String U = m.is("#dg", 1) + "if score #f " + m.L + " matches " + (3 + 3 * r) + " if score #pz " + m.L + " matches 0 run ";
            String Dn = m.is("#dg", 2) + "if score #f " + m.L + " matches " + (3 + 3 * (7 - r)) + " if score #pz " + m.L + " matches 0 run ";
            m.a("Ряд " + (r + 1) + ": поднять", U + fill(5, r, 6, 11, r, 6, "air"));
            m.a("Ряд " + (r + 1) + ": опустить", Dn + fill(5, r, 6, 11, r, 6, "iron_bars"));
            m.a("Ряд " + (r + 1) + ": звук подъёма", U + snd(8, r, 6, "block.chain.break", "1.5", String.valueOf(0.6 + r * 0.05)));
            m.a("Ряд " + (r + 1) + ": звук спуска", Dn + snd(8, r, 6, "block.iron_door.close", "1.5", "0.7"));
            m.a("Ряд " + (r + 1) + ": пыль", U + par(8, r, 6, "smoke ~0.5 ~0.5 ~0.5 3 0.1 0.3 0.01 6"));
        }
        m.a("Счёт кадров", m.isr("#f", "1..") + "if score #pz " + m.L + " matches 0 run " + m.ad("#f", 1));
        m.a("Конец", m.isr("#f", "30..") + "run " + m.sc("#f", 0));
        m.a("Конец: режим", m.f(0) + "run " + m.sc("#dg", 0));
        for (int i = 0; i < 3; i++) m.a("Рог " + (i + 1), m.is("#dg", 1) + "if score #f " + m.L + " matches " + (1 + 4 * i) + " run " + snd(8, 5, 6, "block.note_block.didgeridoo", "3", String.valueOf(0.5 + i * 0.2)));
        m.a("Заголовок: открыто", m.is("#f", 29) + "if score #dg " + m.L + " matches 1 run " + act(8, 1, 3, "Ворота открыты", "green"));
        m.a("Заголовок: закрыто", m.is("#f", 29) + "if score #dg " + m.L + " matches 2 run " + act(8, 1, 3, "Ворота закрыты", "red"));
        m.a("Заголовок: идёт", m.is("#f", 2) + "run " + act(8, 1, 3, "Решётка движется", "yellow"));
        m.a("Лампа: движется", m.isr("#f", "1..") + "run " + set(4, 1, 0, "orange_concrete"));
        m.a("Лампа: покой", m.f(0) + "run " + set(4, 1, 0, "red_concrete"));
        m.a("Цепь вверх", m.is("#dg", 1) + "if score #f " + m.L + " matches 24 run " + fill(5, 8, 5, 5, 10, 5, "chain"));
        m.a("Огни арки: движение", m.isr("#f", "1..") + "run " + fill(3, 8, 5, 3, 8, 7, "sea_lantern"));
        m.a("Огни арки: покой", m.f(0) + "run " + fill(3, 8, 5, 3, 8, 7, "stone_bricks"));
        m.a("Огни арки 2: движение", m.isr("#f", "1..") + "run " + fill(13, 8, 5, 13, 8, 7, "sea_lantern"));
        m.a("Огни арки 2: покой", m.f(0) + "run " + fill(13, 8, 5, 13, 8, 7, "stone_bricks"));
        for (int r = 0; r < 8; r++) m.a("Цепь " + (r + 1), m.is("#dg", 1) + "if score #f " + m.L + " matches " + (3 + 3 * r) + " run " + par(5, 8, 5, "crit ~0.5 ~" + (r % 3) + " ~0.5 0.1 0.3 0.1 0 2"));
        for (int i = 0; i < 4; i++) m.a("Колокол " + (i + 1), m.is("#dg", 2) + "if score #f " + m.L + " matches " + (24 + i) + " run " + snd(8, 5, 6, "block.bell.use", "2", String.valueOf(0.6 + i * 0.1)));
        m.a("Стражи: свет вкл", m.isr("#f", "1..") + "run " + set(2, 4, 6, "sea_lantern"));
        m.a("Стражи: свет выкл", m.f(0) + "run " + set(2, 4, 6, "gray_concrete"));
        m.a("Стражи: свет 2 вкл", m.isr("#f", "1..") + "run " + set(14, 4, 6, "sea_lantern"));
        m.a("Стражи: свет 2 выкл", m.f(0) + "run " + set(14, 4, 6, "gray_concrete"));
    }

    // ---------------------------------------------------------------- 20. ловушка
    static void trap(M m) {
        var b = m.b;
        b.part("Яма");
        b.hol(4, -8, 4, 10, -1, 10, "stone_bricks");
        b.air(5, -7, 5, 9, -2, 9);
        b.box(5, -1, 5, 9, -1, 9, "smooth_stone");
        b.box(4, 0, 4, 4, 1, 4, "stone_brick_wall");
        b.box(10, 0, 4, 10, 1, 4, "stone_brick_wall");
        m.init.addAll(java.util.List.of(m.sc("#arm", 0), m.sc("#st", 0), m.sc("#dm", 0)));
        m.a("Взвести: включить", m.p(0) + "run " + m.sc("#arm", 1));
        m.a("Взвести: сообщение", m.p(0) + "run " + act(7, 1, 7, "Ловушка включена", "red"));
        m.a("Выключить", m.p(1) + "run " + m.sc("#arm", 0));
        m.a("Выключить: сообщение", m.p(1) + "run " + act(7, 1, 7, "Ловушка выключена", "green"));
        m.a("Сброс: закрыть пол", m.p(2) + "run " + fill(5, -1, 5, 9, -1, 9, "smooth_stone"));
        m.a("Сброс: состояние", m.p(2) + "run " + m.sc("#st", 0));
        m.a("Сброс: кадры", m.p(2) + "run " + m.sc("#f", 0));
        for (String t : FOES)
            m.a("Датчик: " + t.replace("minecraft:", "").replace("#", ""), m.is("#arm", 1) + "if score #st " + m.L + " matches 0 " + pos(5, -1, 5) + "if entity @e[type=" + t + ",dx=4,dy=2,dz=4] run " + m.sc("#st", 1));
        m.a("Запуск", m.is("#st", 1) + "if score #f " + m.L + " matches 0 run " + m.sc("#f", 1));
        for (int i = 0; i < 5; i++) {
            m.a("Пол открывается " + (i + 1), m.is("#st", 1) + "if score #f " + m.L + " matches " + (3 + 2 * i) + " run " + fill(5 + i, -1, 5, 5 + i, -1, 9, "air"));
            m.a("Пол закрывается " + (i + 1), m.is("#st", 1) + "if score #f " + m.L + " matches " + (203 + 2 * i) + " run " + fill(5 + i, -1, 5, 5 + i, -1, 9, "smooth_stone"));
            m.a("Звук открытия " + (i + 1), m.is("#st", 1) + "if score #f " + m.L + " matches " + (3 + 2 * i) + " run " + snd(7, -1, 7, "block.piston.extend", "1", "0.6"));
            m.a("Звук закрытия " + (i + 1), m.is("#st", 1) + "if score #f " + m.L + " matches " + (203 + 2 * i) + " run " + snd(7, -1, 7, "block.piston.contract", "1", "0.6"));
        }
        m.a("Заголовок: сработала", m.is("#f", 2) + "run " + act(7, 1, 7, "Ловушка сработала", "red"));
        m.a("Заголовок: закрыта", m.is("#f", 213) + "run " + act(7, 1, 7, "Ловушка закрыта", "yellow"));
        m.a("Пыль при открытии", m.fr(3, 12) + "run " + par(7, -1, 7, "campfire_cosy_smoke ~0.5 ~0.5 ~0.5 2 0.1 2 0.01 4"));
        m.a("Счёт кадров", m.isr("#f", "1..") + "run " + m.ad("#f", 1));
        m.a("Конец", m.isr("#f", "216..") + "run " + m.sc("#f", 0));
        m.a("Конец: состояние", m.is("#f", 0) + "if score #st " + m.L + " matches 1 if score #arm " + m.L + " matches 1 run " + m.sc("#st", 0));
        m.a("Урон: такт", m.is("#st", 1) + "run " + m.ad("#dm", 1));
        for (String t : FOES) m.a("Урон в яме: " + t.replace("minecraft:", "").replace("#", ""), m.is("#st", 1) + "if score #dm " + m.L + " matches 20.. " + pos(5, -7, 5) + "run damage @e[type=" + t + ",dx=4,dy=6,dz=4] 2 minecraft:magic");
        m.a("Урон: сброс такта", m.isr("#dm", "20..") + "run " + m.sc("#dm", 0));
        m.a("Лампа: взведена", m.is("#arm", 1) + "run " + set(4, 1, 0, "red_concrete"));
        m.a("Лампа: выключена", m.is("#arm", 0) + "run " + set(4, 1, 0, "lime_concrete"));
        m.a("Яма: подсветка", m.is("#st", 1) + "run " + par(7, -2, 7, "soul_fire_flame ~0.5 ~0.5 ~0.5 2 0.3 2 0.01 6"));
        for (int i = 0; i < 4; i++) m.a("Взвод: отсчёт " + (4 - i), m.p(0) + "run " + act(7, 1, 7, "Ловушка взводится", "yellow"));
        for (int i = 0; i < 3; i++) m.a("Сирена ловушки " + (i + 1), m.is("#f", 3 + 6 * i) + "run " + snd(7, 1, 7, "block.note_block.bell", "2", String.valueOf(0.6 + i * 0.3)));
        for (int i = 0; i < 5; i++) m.a("Огни ямы " + (i + 1), m.is("#f", 3 + 2 * i) + "run " + set(4, 0, 5 + i, "red_concrete"));
        for (int i = 0; i < 5; i++) m.a("Огни ямы вернуть " + (i + 1), m.is("#f", 203 + 2 * i) + "run " + set(4, 0, 5 + i, "gray_concrete"));
        m.a("Приманка", m.is("#arm", 1) + "run " + par(7, 0, 7, "composter ~0.5 ~0.2 ~0.5 1 0.1 1 0 1"));
    }

    // ---------------------------------------------------------------- 21. метро
    static void metro(M m) {
        var b = m.b;
        b.part("Станция А");
        b.box(1, -1, 4, 5, -1, 8, "polished_andesite");
        b.box(1, 0, 4, 1, 3, 8, "iron_bars");
        b.part("Станция Б");
        b.box(27, -1, 4, 31, -1, 8, "polished_andesite");
        b.box(31, 0, 4, 31, 3, 8, "iron_bars");
        b.part("Тоннель");
        b.hol(6, -1, 4, 26, 3, 8, "glass");
        b.box(6, -1, 4, 26, -1, 8, "smooth_stone");
        for (int i = 0; i < 10; i++) b.set(8 + 2 * i, 3, 6, "sea_lantern");
        m.init.addAll(java.util.List.of(m.sc("#dr", 0), m.sc("#pz", 0)));
        m.a("А в Б: старт", m.p(0) + "if score #f " + m.L + " matches 0 run " + m.sc("#dr", 1));
        m.a("Б в А: старт", m.p(1) + "if score #f " + m.L + " matches 0 run " + m.sc("#dr", 2));
        m.a("Запуск", "execute if score #dr " + m.L + " matches 1..2 if score #f " + m.L + " matches 0 run " + m.sc("#f", 1));
        m.a("Метка капсулы: А", m.is("#dr", 1) + "if score #f " + m.L + " matches 1 run summon minecraft:marker " + ph3(3, 0, 6) + " {Tags:[\"mkcap\"]}");
        m.a("Метка капсулы: Б", m.is("#dr", 2) + "if score #f " + m.L + " matches 1 run summon minecraft:marker " + ph3(29, 0, 6) + " {Tags:[\"mkcap\"]}");
        m.a("Стоп: счёт", m.p(2) + "run " + m.sc("#f", 0));
        m.a("Стоп: метка", m.p(2) + "run kill @e[type=minecraft:marker,tag=mkcap]");
        m.a("Стоп: режим", m.p(2) + "run " + m.sc("#dr", 0));
        m.a("Стоп: сообщение", m.p(2) + "run " + act(3, 1, 6, "Метро остановлено", "red"));
        m.a("Двери А: закрыть", m.f(1) + "run " + fill(1, 0, 4, 1, 3, 8, "iron_bars"));
        m.a("Объявление: отправление", m.f(2) + "run " + act(3, 1, 6, "Осторожно, двери закрываются", "yellow"));
        m.a("Звук отправления", m.f(2) + "run " + snd(3, 1, 6, "block.note_block.chime", "2", "1"));
        m.a("Старт: свет", m.f(4) + "run " + fill(1, 3, 4, 1, 3, 8, "sea_lantern"));
        m.a("Движение игроков вправо", m.is("#dr", 1) + "if score #f " + m.L + " matches 10..49 as @e[type=minecraft:marker,tag=mkcap] at @s as @a[distance=..1.8] at @s run tp @s ~0.6 ~ ~");
        m.a("Движение игроков влево", m.is("#dr", 2) + "if score #f " + m.L + " matches 10..49 as @e[type=minecraft:marker,tag=mkcap] at @s as @a[distance=..1.8] at @s run tp @s ~-0.6 ~ ~");
        m.a("Движение метки вправо", m.is("#dr", 1) + "if score #f " + m.L + " matches 10..49 as @e[type=minecraft:marker,tag=mkcap] at @s run tp @s ~0.6 ~ ~");
        m.a("Движение метки влево", m.is("#dr", 2) + "if score #f " + m.L + " matches 10..49 as @e[type=minecraft:marker,tag=mkcap] at @s run tp @s ~-0.6 ~ ~");
        m.a("След капсулы", m.fr(10, 49) + "as @e[type=minecraft:marker,tag=mkcap] at @s run particle minecraft:end_rod ~ ~1 ~ 0.3 0.4 0.3 0.01 6");
        m.a("Ветер в тоннеле", m.fr(10, 49) + "as @e[type=minecraft:marker,tag=mkcap] at @s run particle minecraft:cloud ~ ~0.5 ~ 0.8 0.3 0.8 0.02 4");
        m.a("Шум движения", m.fr(10, 49) + "as @e[type=minecraft:marker,tag=mkcap] at @s run playsound minecraft:block.beacon.ambient block @a ~ ~ ~ 1 1.8");
        for (int i = 0; i < 10; i++) {
            m.a("Огни тоннеля " + (i + 1) + ": зажечь", m.fr(10 + 4 * i, 13 + 4 * i) + "run " + set(8 + 2 * i, 3, 6, "light_blue_concrete"));
            m.a("Огни тоннеля " + (i + 1) + ": вернуть", m.f(14 + 4 * i) + "run " + set(8 + 2 * i, 3, 6, "sea_lantern"));
        }
        m.a("Прибытие: убрать метку", m.f(50) + "run kill @e[type=minecraft:marker,tag=mkcap]");
        m.a("Прибытие: двери Б", m.is("#dr", 1) + "if score #f " + m.L + " matches 52 run " + fill(31, 0, 4, 31, 3, 8, "air"));
        m.a("Прибытие: двери А", m.is("#dr", 2) + "if score #f " + m.L + " matches 52 run " + fill(1, 0, 4, 1, 3, 8, "air"));
        m.a("Прибытие: объявление Б", m.is("#dr", 1) + "if score #f " + m.L + " matches 52 run " + act(29, 1, 6, "Станция Б. Выходите", "green"));
        m.a("Прибытие: объявление А", m.is("#dr", 2) + "if score #f " + m.L + " matches 52 run " + act(3, 1, 6, "Станция А. Выходите", "green"));
        m.a("Прибытие: звук Б", m.is("#dr", 1) + "if score #f " + m.L + " matches 52 run " + snd(29, 1, 6, "block.note_block.chime", "2", "1.4"));
        m.a("Прибытие: звук А", m.is("#dr", 2) + "if score #f " + m.L + " matches 52 run " + snd(3, 1, 6, "block.note_block.chime", "2", "1.4"));
        m.a("Закрыть двери Б", m.is("#dr", 1) + "if score #f " + m.L + " matches 120 run " + fill(31, 0, 4, 31, 3, 8, "iron_bars"));
        m.a("Закрыть двери А", m.is("#dr", 2) + "if score #f " + m.L + " matches 120 run " + fill(1, 0, 4, 1, 3, 8, "iron_bars"));
        m.a("Огни станции: конец", m.f(120) + "run " + fill(1, 3, 4, 1, 3, 8, "iron_bars"));
        m.a("Лампа: в пути", m.isr("#f", "1..") + "run " + set(4, 1, 0, "orange_concrete"));
        m.a("Лампа: покой", m.f(0) + "run " + set(4, 1, 0, "red_concrete"));
        m.a("Счёт кадров", m.isr("#f", "1..") + "run " + m.ad("#f", 1));
        m.a("Конец", m.isr("#f", "125..") + "run " + m.sc("#f", 0));
        m.a("Конец: режим", m.f(0) + "run " + m.sc("#dr", 0));
        for (int i = 0; i < 10; i++) {
            m.a("Путь " + (i + 1) + ": лампа пола", m.fr(10 + 4 * i, 13 + 4 * i) + "run " + set(8 + 2 * i, -1, 6, "lime_concrete"));
            m.a("Путь " + (i + 1) + ": вернуть", m.f(14 + 4 * i) + "run " + set(8 + 2 * i, -1, 6, "smooth_stone"));
        }
        m.a("Объявление: следующая", m.f(20) + "run " + act(14, 1, 6, "Следующая остановка скоро", "yellow"));
        m.a("Объявление: скорость", m.f(30) + "run " + act(18, 1, 6, "Держитесь, капсула разгоняется", "yellow"));
        m.a("Объявление: торможение", m.f(42) + "run " + act(24, 1, 6, "Торможение", "yellow"));
        for (int i = 0; i < 4; i++) m.a("Гудок " + (i + 1), m.f(2 + i) + "run " + snd(3, 1, 6, "block.note_block.bit", "1.5", String.valueOf(0.8 + i * 0.2)));
        m.a("Станция А: огни", m.is("#dr", 1) + "if score #f " + m.L + " matches 6 run " + fill(2, -1, 4, 2, -1, 8, "yellow_concrete"));
        m.a("Станция А: огни вернуть", m.is("#dr", 1) + "if score #f " + m.L + " matches 12 run " + fill(2, -1, 4, 2, -1, 8, "polished_andesite"));
        m.a("Станция Б: огни", m.is("#dr", 2) + "if score #f " + m.L + " matches 6 run " + fill(30, -1, 4, 30, -1, 8, "yellow_concrete"));
        m.a("Станция Б: огни вернуть", m.is("#dr", 2) + "if score #f " + m.L + " matches 12 run " + fill(30, -1, 4, 30, -1, 8, "polished_andesite"));
        m.a("Прибытие: огни Б", m.is("#dr", 1) + "if score #f " + m.L + " matches 50 run " + fill(30, -1, 4, 30, -1, 8, "lime_concrete"));
        m.a("Прибытие: огни А", m.is("#dr", 2) + "if score #f " + m.L + " matches 50 run " + fill(2, -1, 4, 2, -1, 8, "lime_concrete"));
        m.a("Отбытие: огни Б", m.is("#dr", 1) + "if score #f " + m.L + " matches 118 run " + fill(30, -1, 4, 30, -1, 8, "polished_andesite"));
        m.a("Отбытие: огни А", m.is("#dr", 2) + "if score #f " + m.L + " matches 118 run " + fill(2, -1, 4, 2, -1, 8, "polished_andesite"));
    }

    // ---------------------------------------------------------------- 22. умный свет
    static void smartlight(M m) {
        var b = m.b;
        String[] rn = {"Комната 1", "Комната 2", "Комната 3"};
        for (int r = 0; r < 3; r++) {
            b.part(rn[r]);
            int x0 = 3 + r * 6;
            b.hol(x0, -1, 3, x0 + 4, 4, 9, "stone_bricks");
            b.air(x0 + 1, 0, 4, x0 + 3, 3, 8);
            b.box(x0 + 1, 4, 6, x0 + 3, 4, 6, "gray_concrete");
            b.air(x0 + 2, 0, 3, x0 + 2, 1, 3);
        }
        b.part("Уличные фонари");
        b.box(2, 0, 11, 2, 3, 11, "oak_fence");
        b.box(22, 0, 11, 22, 3, 11, "oak_fence");
        b.set(2, 4, 11, "gray_concrete");
        b.set(22, 4, 11, "gray_concrete");
        m.init.addAll(java.util.List.of(m.sc("#mode", 0), m.sc("#br", 1), m.sc("#dt", 0), m.sc("#night", 0), m.sc("#pr0", 0), m.sc("#pr1", 0), m.sc("#pr2", 0), m.sc("#w0", 0), m.sc("#w1", 0), m.sc("#w2", 0)));
        m.a("Режим: авто", m.p(0) + "run " + m.sc("#mode", 0));
        m.a("Режим: включено", m.p(1) + "run " + m.sc("#mode", 1));
        m.a("Режим: выключено", m.p(2) + "run " + m.sc("#mode", 2));
        m.a("Яркость: шаг", m.p(3) + "run " + m.ad("#br", 1));
        m.a("Яркость: круг", m.isr("#br", "3..") + "run " + m.sc("#br", 0));
        m.a("Сообщение: режим", m.p(0) + "run " + act(12, 1, 2, "Свет: авто", "aqua"));
        m.a("Сообщение: включено", m.p(1) + "run " + act(12, 1, 2, "Свет: всегда включён", "yellow"));
        m.a("Сообщение: выключено", m.p(2) + "run " + act(12, 1, 2, "Свет: выключен", "gray"));
        m.a("Сообщение: яркость", m.p(3) + "run " + act(12, 1, 2, "Яркость изменена", "gold"));
        m.a("Время суток", "execute store result score #dt " + m.L + " run time query daytime");
        m.a("Ночь: нет", m.sc("#night", 0));
        m.a("Ночь: вечер", m.isr("#dt", "12500..") + "run " + m.sc("#night", 1));
        m.a("Ночь: рассвет", m.isr("#dt", "..300") + "run " + m.sc("#night", 1));
        for (int r = 0; r < 3; r++) {
            int x0 = 3 + r * 6;
            m.a("Присутствие " + (r + 1) + ": сброс", m.sc("#pr" + r, 0));
            m.a("Присутствие " + (r + 1) + ": игрок", "execute " + pos(x0 + 1, 0, 4) + "if entity @a[dx=2,dy=3,dz=4] run " + m.sc("#pr" + r, 1));
            m.a("Нужен свет " + (r + 1) + ": сброс", m.sc("#w" + r, 0));
            m.a("Нужен свет " + (r + 1) + ": всегда", m.is("#mode", 1) + "run " + m.sc("#w" + r, 1));
            m.a("Нужен свет " + (r + 1) + ": авто", m.is("#mode", 0) + "if score #night " + m.L + " matches 1 if score #pr" + r + " " + m.L + " matches 1 run " + m.sc("#w" + r, 1));
            String[] on = {"shroomlight", "glowstone", "sea_lantern"};
            for (int br = 0; br < 3; br++) m.a("Лампы " + (r + 1) + ": яркость " + (br + 1), m.is("#w" + r, 1) + "if score #br " + m.L + " matches " + (br + 1 == 3 ? "2.." : String.valueOf(br)) + " run " + fill(x0 + 1, 4, 6, x0 + 3, 4, 6, on[br]));
            m.a("Лампы " + (r + 1) + ": выкл", m.is("#w" + r, 0) + "run " + fill(x0 + 1, 4, 6, x0 + 3, 4, 6, "gray_concrete"));
            m.a("Свет в окне " + (r + 1), m.is("#w" + r, 1) + "run " + par(x0 + 2, 3, 6, "end_rod ~0.5 ~0.2 ~0.5 0.8 0.1 0.8 0 1"));
            m.a("Индикатор " + (r + 1), m.is("#w" + r, 1) + "run " + set(x0 + 2, 1, 3, "lime_concrete"));
            m.a("Индикатор " + (r + 1) + ": выкл", m.is("#w" + r, 0) + "run " + set(x0 + 2, 1, 3, "red_concrete"));
        }
        for (int s = 0; s < 2; s++) {
            int x = s == 0 ? 2 : 22;
            m.a("Фонарь " + (s + 1) + ": включить", m.is("#night", 1) + "run " + set(x, 4, 11, "lantern"));
            m.a("Фонарь " + (s + 1) + ": выключить", m.is("#night", 0) + "run " + set(x, 4, 11, "gray_concrete"));
        }
        m.a("Табло", at(12, 1, 2) + "title @a[distance=..20] actionbar [{\"text\":\"Свет: режим \",\"color\":\"gray\"},{\"score\":{\"name\":\"#mode\",\"objective\":\"" + m.L + "\"},\"color\":\"gold\"},{\"text\":\"  ночь \",\"color\":\"gray\"},{\"score\":{\"name\":\"#night\",\"objective\":\"" + m.L + "\"},\"color\":\"aqua\"}]");
        String[] hw = {"gray_concrete", "shroomlight", "sea_lantern", "shroomlight"};
        for (int g = 0; g < 3; g++) {
            m.a("Коридор " + (g + 1) + ": день", m.isr("#dt", "300..12000") + "run " + set(3 + 6 * g, 4, 10, "gray_concrete"));
            m.a("Коридор " + (g + 1) + ": вечер", m.isr("#dt", "12001..13999") + "run " + set(3 + 6 * g, 4, 10, "shroomlight"));
            m.a("Коридор " + (g + 1) + ": ночь", m.isr("#dt", "14000..22000") + "run " + set(3 + 6 * g, 4, 10, "sea_lantern"));
            m.a("Коридор " + (g + 1) + ": рассвет", m.isr("#dt", "22001..24000") + "run " + set(3 + 6 * g, 4, 10, "shroomlight"));
            m.a("Коридор " + (g + 1) + ": мерцание при входе", m.is("#pr" + g, 1) + "run " + par(5 + 6 * g, 2, 5, "glow ~0.5 ~ ~0.5 0.6 0.5 0.6 0 2"));
        }
        m.a("Фонарь: частицы 1", m.is("#night", 1) + "run " + par(2, 3, 11, "end_rod ~0.5 ~ ~0.5 0.1 0.1 0.1 0 1"));
        m.a("Фонарь: частицы 2", m.is("#night", 1) + "run " + par(22, 3, 11, "end_rod ~0.5 ~ ~0.5 0.1 0.1 0.1 0 1"));
    }

    // ---------------------------------------------------------------- 23. ферма
    static void farm(M m) {
        var b = m.b;
        b.part("Поле");
        b.hol(2, -1, 2, 12, 0, 12, "oak_planks");
        b.box(3, -1, 3, 11, -1, 11, "farmland");
        b.set(7, -1, 7, "water");
        b.set(7, 0, 7, "oak_fence");
        b.box(3, 0, 1, 3, 2, 1, "oak_fence");
        b.set(3, 3, 1, "carved_pumpkin[facing=south]");
        m.init.addAll(java.util.List.of(m.sc("#hv", 0)));
        for (int r = 0; r < 9; r++) {
            int z = 3 + r;
            String P = m.p(0) + "run ";
            if (z == 7) {
                m.a("Посев ряд " + (r + 1) + "а", P + repl(3, 0, z, 6, 0, z, "wheat[age=0]", "air"));
                m.a("Посев ряд " + (r + 1) + "б", P + repl(8, 0, z, 11, 0, z, "wheat[age=0]", "air"));
            } else m.a("Посев ряд " + (r + 1), P + repl(3, 0, z, 11, 0, z, "wheat[age=0]", "air"));
        }
        m.a("Посев: звук", m.p(0) + "run " + snd(7, 0, 7, "item.hoe.till", "1", "1"));
        m.a("Посев: сообщение", m.p(0) + "run " + act(7, 1, 1, "Поле засеяно", "green"));
        m.a("Рост: старт", m.p(1) + "if score #f " + m.L + " matches 0 run " + m.sc("#f", 1));
        for (int g = 1; g <= 7; g++) {
            for (int r = 0; r < 9; r++) {
                int z = 3 + r, f = 10 * g + 2 * r;
                String F = m.f(f) + "run ";
                if (z == 7) {
                    m.a("Рост " + g + ", ряд " + (r + 1) + "а", F + repl(3, 0, z, 6, 0, z, "wheat[age=" + g + "]", "wheat[age=" + (g - 1) + "]"));
                    m.a("Рост " + g + ", ряд " + (r + 1) + "б", F + repl(8, 0, z, 11, 0, z, "wheat[age=" + g + "]", "wheat[age=" + (g - 1) + "]"));
                } else m.a("Рост " + g + ", ряд " + (r + 1), F + repl(3, 0, z, 11, 0, z, "wheat[age=" + g + "]", "wheat[age=" + (g - 1) + "]"));
            }
        }
        for (int g = 1; g <= 7; g++) m.a("Рост: частицы " + g, m.f(10 * g) + "run " + par(7, 1, 7, "happy_villager ~0.5 ~0.5 ~0.5 4 0.5 4 0 10"));
        m.a("Рост: звук", m.f(40) + "run " + snd(7, 0, 7, "block.grass.break", "1", "1"));
        m.a("Рост: готово", m.f(90) + "run " + act(7, 1, 1, "Урожай созрел", "gold"));
        m.a("Рост: счёт", m.isr("#f", "1..") + "run " + m.ad("#f", 1));
        m.a("Рост: конец", m.isr("#f", "95..") + "run " + m.sc("#f", 0));
        m.a("Сбор: старт", m.p(2) + "if score #hv " + m.L + " matches 0 run " + m.sc("#hv", 1));
        for (int r = 0; r < 9; r++) {
            int z = 3 + r;
            String H = m.is("#hv", 1 + 2 * r) + "run ";
            String H2 = m.is("#hv", 2 + 2 * r) + "run ";
            if (z == 7) {
                m.a("Сбор ряд " + (r + 1) + "а", H + destroy(3, 0, z, 6, 0, z));
                m.a("Сбор ряд " + (r + 1) + "б", H + destroy(8, 0, z, 11, 0, z));
                m.a("Пересев ряд " + (r + 1) + "а", H2 + repl(3, 0, z, 6, 0, z, "wheat[age=0]", "air"));
                m.a("Пересев ряд " + (r + 1) + "б", H2 + repl(8, 0, z, 11, 0, z, "wheat[age=0]", "air"));
            } else {
                m.a("Сбор ряд " + (r + 1), H + destroy(3, 0, z, 11, 0, z));
                m.a("Пересев ряд " + (r + 1), H2 + repl(3, 0, z, 11, 0, z, "wheat[age=0]", "air"));
            }
        }
        m.a("Сбор: счёт", m.isr("#hv", "1..") + "run " + m.ad("#hv", 1));
        m.a("Сбор: конец", m.isr("#hv", "21..") + "run " + m.sc("#hv", 0));
        m.a("Сбор: сообщение", m.is("#hv", 20) + "run " + act(7, 1, 1, "Урожай собран, поле засеяно заново", "green"));
        m.a("Сбор: звук", m.isr("#hv", "1..") + "run " + snd(7, 0, 7, "block.crop.break", "0.6", "1"));
        m.a("Полив: капли", m.p(3) + "run " + par(7, 4, 7, "falling_water ~0.5 ~ ~0.5 4 0.5 4 0 60"));
        m.a("Полив: звук", m.p(3) + "run " + snd(7, 1, 7, "weather.rain", "1", "1"));
        m.a("Полив: вода", m.p(3) + "run " + set(7, -1, 7, "water"));
        m.a("Полив: сообщение", m.p(3) + "run " + act(7, 1, 1, "Поле полито", "aqua"));
    }

    // ---------------------------------------------------------------- 24. гравитационная труба
    static void gravtube(M m) {
        var b = m.b;
        b.part("Труба");
        b.hol(5, 0, 5, 9, 17, 9, "glass");
        b.air(6, 1, 6, 8, 16, 8);
        b.box(6, 0, 6, 8, 0, 8, "light_blue_concrete");
        b.box(6, 17, 6, 8, 17, 8, "light_blue_concrete");
        b.air(7, 1, 5, 7, 2, 5);
        b.part("Верхняя площадка");
        b.box(4, 17, 4, 10, 17, 10, "smooth_stone");
        b.air(6, 17, 6, 8, 17, 8);
        b.box(6, 17, 6, 8, 17, 8, "glass");
        m.init.addAll(java.util.List.of(m.sc("#m", 0), m.sc("#ph", 0), m.sc("#tk", 0)));
        m.a("Вверх", m.p(0) + "run " + m.sc("#m", 1));
        m.a("Вниз", m.p(1) + "run " + m.sc("#m", 2));
        m.a("Выкл", m.p(2) + "run " + m.sc("#m", 0));
        m.a("Сообщение вверх", m.p(0) + "run " + act(7, 1, 3, "Труба: вверх", "aqua"));
        m.a("Сообщение вниз", m.p(1) + "run " + act(7, 1, 3, "Труба: вниз", "gold"));
        m.a("Сообщение выкл", m.p(2) + "run " + act(7, 1, 3, "Труба выключена", "gray"));
        m.a("Подъём: левитация", m.is("#m", 1) + pos(6, 0, 6) + "run effect give @a[dx=2,dy=16,dz=2] minecraft:levitation 1 2 true");
        m.a("Подъём: защита", m.is("#m", 1) + pos(6, 0, 6) + "run effect give @a[dx=2,dy=16,dz=2] minecraft:slow_falling 2 0 true");
        m.a("Спуск: медленное падение", m.is("#m", 2) + pos(6, 0, 6) + "run effect give @a[dx=2,dy=16,dz=2] minecraft:slow_falling 1 0 true");
        m.a("Спуск: движение", m.is("#m", 2) + pos(6, 1, 6) + "as @a[dx=2,dy=16,dz=2] at @s if block ~ ~-0.3 ~ minecraft:air run tp @s ~ ~-0.3 ~");
        m.a("Подъём: частицы", m.is("#m", 1) + pos(7, 1, 7) + "run particle minecraft:end_rod ~0.5 ~ ~0.5 0.8 8 0.8 0.01 8");
        m.a("Спуск: частицы", m.is("#m", 2) + pos(7, 1, 7) + "run particle minecraft:reverse_portal ~0.5 ~ ~0.5 0.8 8 0.8 0.05 12");
        m.a("Фаза: такт", m.isr("#m", "1..2") + "run " + m.ad("#tk", 1));
        m.a("Фаза: шаг", m.isr("#tk", "2..") + "run " + m.ad("#ph", 1));
        m.a("Фаза: сброс такта", m.isr("#tk", "2..") + "run " + m.sc("#tk", 0));
        m.a("Фаза: круг", m.isr("#ph", "8..") + "run " + m.sc("#ph", 0));
        for (int k = 0; k < 8; k++) {
            int y = 2 * k + 1;
            m.a("Кольцо " + (k + 1) + ": вверх зажечь", m.is("#m", 1) + "if score #ph " + m.L + " matches " + k + " run " + fill(5, y, 5, 9, y, 9, "light_blue_stained_glass") + " outline");
            m.a("Кольцо " + (k + 1) + ": вверх погасить", m.is("#m", 1) + "if score #ph " + m.L + " matches " + ((k + 1) % 8) + " run " + fill(5, y, 5, 9, y, 9, "glass") + " outline");
            m.a("Кольцо " + (k + 1) + ": вниз зажечь", m.is("#m", 2) + "if score #ph " + m.L + " matches " + (7 - k) + " run " + fill(5, y, 5, 9, y, 9, "orange_stained_glass") + " outline");
            m.a("Кольцо " + (k + 1) + ": вниз погасить", m.is("#m", 2) + "if score #ph " + m.L + " matches " + (7 - ((k + 1) % 8)) + " run " + fill(5, y, 5, 9, y, 9, "glass") + " outline");
        }
        m.a("Звук потока", m.isr("#m", "1..2") + "if score #tk " + m.L + " matches 0 run " + snd(7, 8, 7, "block.beacon.ambient", "1", "1.6"));
        m.a("Выкл: кольца", m.is("#m", 0) + "if score #ph " + m.L + " matches 0 run " + fill(5, 1, 5, 9, 15, 9, "glass") + " replace minecraft:light_blue_stained_glass");
        m.a("Выкл: кольца 2", m.is("#m", 0) + "if score #ph " + m.L + " matches 0 run " + fill(5, 1, 5, 9, 15, 9, "glass") + " replace minecraft:orange_stained_glass");
        m.a("Лампа: работа", m.isr("#m", "1..2") + "run " + set(4, 1, 0, "lime_concrete"));
        m.a("Лампа: стоп", m.is("#m", 0) + "run " + set(4, 1, 0, "red_concrete"));
        for (int k = 0; k < 8; k++) {
            int y = 2 * k + 1;
            m.a("Частицы кольца " + (k + 1) + ": вверх", m.is("#m", 1) + "if score #ph " + m.L + " matches " + k + " run " + par(7, y, 7, "end_rod ~0.5 ~ ~0.5 1 0.1 1 0.02 6"));
            m.a("Частицы кольца " + (k + 1) + ": вниз", m.is("#m", 2) + "if score #ph " + m.L + " matches " + (7 - k) + " run " + par(7, y, 7, "reverse_portal ~0.5 ~ ~0.5 1 0.1 1 0.02 6"));
        }
        m.a("Вход: дверь открыта", m.isr("#m", "1..2") + "run " + fill(7, 1, 5, 7, 2, 5, "air"));
        m.a("Вход: дверь закрыта", m.is("#m", 0) + "run " + fill(7, 1, 5, 7, 2, 5, "iron_bars"));
        m.a("Верх достигнут: стоп", m.is("#m", 1) + pos(6, 15, 6) + "if entity @a[dx=2,dy=1,dz=2] run " + m.sc("#m", 0));
        m.a("Низ достигнут: стоп", m.is("#m", 2) + pos(6, 1, 6) + "if entity @a[dx=2,dy=0,dz=2] if block ~ ~-1 ~ minecraft:light_blue_concrete run " + m.sc("#m", 0));
        for (int k = 0; k < 4; k++) m.a("Звук яруса " + (k + 1), m.isr("#m", "1..2") + "if score #ph " + m.L + " matches " + (2 * k) + " if score #tk " + m.L + " matches 0 run " + snd(7, 4 * k + 2, 7, "block.note_block.chime", "1", String.valueOf(0.7 + k * 0.2)));
    }

    // ---------------------------------------------------------------- 25. ветряк
    static void windmill(M m) {
        var b = m.b;
        b.part("Башня");
        b.box(7, 0, 5, 7, 8, 5, "stone_bricks");
        b.box(6, 0, 5, 8, 0, 6, "stone_bricks");
        b.box(6, 9, 5, 8, 11, 6, "white_concrete");
        b.set(7, 9, 4, "iron_block");
        b.part("Шаблоны кадров");
        for (int p = 0; p < 8; p++) {
            int zf = 14 + p;
            b.set(7, 9, zf, "iron_block");
            for (int a = 0; a < 4; a++) {
                double th = Math.toRadians(a * 90 + p * 11.25);
                for (int r = 1; r <= 4; r++) {
                    int dx = (int) Math.round(r * Math.cos(th)), dy = (int) Math.round(r * Math.sin(th));
                    b.set(7 + dx, 9 + dy, zf, r == 4 ? "light_gray_concrete" : "white_concrete");
                }
            }
        }
        m.init.addAll(java.util.List.of(m.sc("#on", 0), m.sc("#ph", 0), m.sc("#tk", 0), m.sc("#per", 6), m.sc("#en", 0), m.sc("#sx", 0), m.sc("#e20", 0), m.sc("#g", 0)));
        m.a("Пуск", m.p(0) + "run " + m.sc("#on", 1));
        m.a("Пуск: ритм", m.p(0) + "run " + m.sc("#per", 4));
        m.a("Стоп", m.p(1) + "run " + m.sc("#on", 0));
        m.a("Тормоз", m.p(2) + "run " + m.sc("#per", 12));
        m.a("Тормоз: сообщение", m.p(2) + "run " + act(7, 1, 3, "Ветряк тормозит", "yellow"));
        m.a("Такт", m.is("#on", 1) + "run " + m.ad("#tk", 1));
        m.a("Кадр: шаг", m.is("#on", 1) + "if score #tk " + m.L + " >= #per " + m.L + " run " + m.ad("#ph", 1));
        m.a("Кадр: сброс такта", m.is("#on", 1) + "if score #tk " + m.L + " >= #per " + m.L + " run " + m.sc("#tk", 0));
        m.a("Кадр: круг", m.isr("#ph", "8..") + "run " + m.sc("#ph", 0));
        for (int p = 0; p < 8; p++)
            m.a("Лопасти: кадр " + (p + 1), m.is("#on", 1) + "if score #ph " + m.L + " matches " + p + " if score #tk " + m.L + " matches 0 run clone " + ph3(3, 5, 14 + p) + " " + ph3(11, 13, 14 + p) + " " + ph3(3, 5, 4) + " replace");
        m.a("Звук ветра", m.is("#on", 1) + "if score #tk " + m.L + " matches 0 if score #ph " + m.L + " matches 0 run " + snd(7, 9, 4, "item.elytra.flying", "0.5", "0.6"));
        m.a("Выработка: такт", m.is("#on", 1) + "run " + m.ad("#e20", 1));
        m.a("Выработка: секунда", m.isr("#e20", "20..") + "if score #on " + m.L + " matches 1 run " + m.ad("#en", 1));
        m.a("Выработка: сброс такта", m.isr("#e20", "20..") + "run " + m.sc("#e20", 0));
        m.a("Предел энергии", m.isr("#en", "101..") + "run " + m.sc("#en", 100));
        for (int i = 0; i < 10; i++) {
            m.a("Лампа заряда " + (i + 1) + ": есть", m.isr("#en", (10 * (i + 1)) + "..") + "run " + set(1 + i, 1, 1, "lime_concrete"));
            m.a("Лампа заряда " + (i + 1) + ": нет", m.isr("#en", ".." + (10 * (i + 1) - 1)) + "run " + set(1 + i, 1, 1, "gray_concrete"));
        }
        m.a("Порыв: метка", m.is("#on", 1) + "if score #tk " + m.L + " matches 1 if score #ph " + m.L + " matches 1 run summon minecraft:marker ~ ~ ~ {Tags:[\"rnd\"]}");
        m.a("Порыв: случайное", "execute if entity @e[type=minecraft:marker,tag=rnd] run execute store result score #g " + m.L + " run data get entity @e[type=minecraft:marker,tag=rnd,limit=1] UUID[0]");
        m.a("Порыв: остаток", "execute if entity @e[type=minecraft:marker,tag=rnd] run scoreboard players operation #g " + m.L + " %= #12 " + m.L);
        m.a("Порыв: убрать метку", "kill @e[type=minecraft:marker,tag=rnd]");
        m.a("Порыв: сильный", m.is("#g", 0) + "if score #on " + m.L + " matches 1 run " + m.sc("#per", 2));
        m.a("Порыв: слабый", m.is("#g", 1) + "if score #on " + m.L + " matches 1 run " + m.sc("#per", 8));
        m.a("Порыв: обычный", m.is("#g", 2) + "if score #on " + m.L + " matches 1 run " + m.sc("#per", 4));
        m.a("Порыв: сообщение", m.is("#g", 0) + "if score #on " + m.L + " matches 1 run " + act(7, 1, 3, "Сильный порыв ветра", "aqua"));
        m.a("Порыв: частицы", m.is("#g", 0) + "if score #on " + m.L + " matches 1 run " + par(7, 9, 4, "cloud ~0.5 ~0.5 ~0.5 3 3 0.5 0.05 20"));
        m.a("Лампа: пуск", m.is("#on", 1) + "run " + set(4, 1, 0, "lime_concrete"));
        m.a("Лампа: стоп", m.is("#on", 0) + "run " + set(4, 1, 0, "red_concrete"));
        m.a("Табло энергии", at(7, 1, 3) + "title @a[distance=..14] actionbar [{\"text\":\"Энергия: \",\"color\":\"gray\"},{\"score\":{\"name\":\"#en\",\"objective\":\"" + m.L + "\"},\"color\":\"green\"},{\"text\":\" из 100\",\"color\":\"gray\"}]");
        for (int p = 0; p < 8; p++) m.a("Искры лопастей " + (p + 1), m.is("#on", 1) + "if score #ph " + m.L + " matches " + p + " if score #tk " + m.L + " matches 0 run " + par(7, 9, 4, "end_rod ~0.5 ~0.5 ~0.5 4 4 0.2 0 2"));
        m.init.addAll(java.util.List.of(m.sc("#dt", 0), m.sc("#night", 0)));
        m.a("Время суток", "execute store result score #dt " + m.L + " run time query daytime");
        m.a("Ночь: сброс", m.sc("#night", 0));
        m.a("Ночь: вечер", m.isr("#dt", "12500..") + "run " + m.sc("#night", 1));
        m.a("Ночь: рассвет", m.isr("#dt", "..300") + "run " + m.sc("#night", 1));
        for (int i = 0; i < 4; i++) {
            m.a("Окно башни " + (i + 1) + ": свет", m.is("#night", 1) + "run " + set(6, 2 * i + 1, 5, "sea_lantern"));
            m.a("Окно башни " + (i + 1) + ": тьма", m.is("#night", 0) + "run " + set(6, 2 * i + 1, 5, "stone_bricks"));
        }
        m.a("Перегрузка: предупреждение", m.is("#on", 1) + "if score #per " + m.L + " matches ..2 if score #tk " + m.L + " matches 0 if score #ph " + m.L + " matches 0 run " + act(7, 1, 3, "Лопасти на пределе", "red"));
        for (int i = 0; i < 5; i++) {
            m.a("Шкала энергии " + (i + 1) + ": есть", m.isr("#en", (20 * (i + 1)) + "..") + "run " + set(9, i + 1, 5, "lime_concrete"));
            m.a("Шкала энергии " + (i + 1) + ": нет", m.isr("#en", ".." + (20 * (i + 1) - 1)) + "run " + set(9, i + 1, 5, "gray_concrete"));
        }
    }

    private NeroMechC() { }
}
