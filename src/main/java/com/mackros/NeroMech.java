package com.mackros;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 25 механизмов на командных блоках, по ~100 командных блоков в каждом (шлюз, кодовый замок, светофор, дискотека, радар, телепорт-хаб, мост, сигнализация, бур, тир,
 * автомат, конвейер, часы, купол, рулетка, ракета, салют, фонтан, решётка ворот, ловушка, метро, умный свет, ферма, гравитационная труба, ветряк).
 * Пульт маленький: кнопки на стене с табличками. Все команды: execute/setblock/fill/scoreboard/title/particle/playsound/summon/tp/effect/data/clear/give.
 */
final class NeroMech {
    record K(String key, String name, String re, String how, int per, String... lab) { }

    static final K[] ALL = {
            new K("k_airlock", "Шлюзовая камера", "шлюзов\\w*\\s+камер|шлюзов", "две двери по очереди, герметизация, табло и пар", 3, "ВОЙТИ", "ВЫЙТИ", "СБРОС"),
            new K("k_codelock", "Кодовый замок на 4 цифры", "замок\\w*\\s+на\\s+4\\s+цифр|4\\s+цифр\\w*\\s+замок", "ввод 4 цифр, дверь хранилища, тревога при ошибках, смена кода", 7, "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "ВВОД", "СБРОС", "НОВЫЙ"),
            new K("k_traffic", "Светофор", "светофор", "перекрёсток с фазами, пешеходная кнопка, мигание и аварийный режим", 3, "ПЕШЕХОД", "РЕЖИМ", "СТОП"),
            new K("k_disco", "Дискотека на 12 узоров", "12\\s+узор|дискотек\\w*\\s+на\\s+12", "12 световых узоров, пол и прожекторы, темп", 4, "ВКЛ", "ВЫКЛ", "БЫСТРЕЕ", "МЕДЛЕННЕЕ"),
            new K("k_radar", "Радар мобов", "радар\\w*\\s+моб", "считает мобов вокруг, тревога при врагах, вращающаяся антенна", 3, "ВКЛ", "ВЫКЛ", "ДАЛЬНОСТЬ"),
            new K("k_tphub", "Хаб телепортации", "хаб\\w*\\s+телепорт|телепорт\\w*\\s+хаб", "4 площадки, выбор пункта, отсчёт и перенос", 5, "ПУНКТ 1", "ПУНКТ 2", "ПУНКТ 3", "ПУНКТ 4", "ОТМЕНА"),
            new K("k_bridge", "Раскладной мост", "раскладн\\w*\\s+мост|выдвижн\\w*\\s+мост", "12 секций выдвигаются и складываются по очереди", 3, "РАЗЛОЖИТЬ", "СЛОЖИТЬ", "СТОП"),
            new K("k_alarm", "Охранная сигнализация", "охранн\\w*\\s+сигнализац", "зона охраны, вооружение с отсчётом, сирена и блокировка выходов", 3, "ВООРУЖИТЬ", "СНЯТЬ", "ТЕСТ"),
            new K("k_drill", "Буровая установка", "буров\\w*\\s+установк", "копает шахту 3x3 вниз слой за слоем и поднимается обратно", 4, "ВНИЗ", "ВВЕРХ", "СТОП", "СБРОС"),
            new K("k_range", "Тир с мишенями", "\\bтир\\b|тир\\s+с\\s+мишен", "6 мишеней выскакивают случайно, очки, 3 сложности", 5, "СТАРТ", "СТОП", "ЛЕГКО", "СРЕДНЕ", "СЛОЖНО"),
            new K("k_vending", "Торговый автомат", "торгов\\w*\\s+автомат|обменник", "меняет предметы по таблице цен через clear и give", 3, "ТОВАР 1", "ТОВАР 2", "ТОВАР 3", "ТОВАР 4", "ТОВАР 5", "ВЕРНУТЬ"),
            new K("k_conveyor", "Конвейер с сортировкой", "конвейер\\w*\\s+с\\s+сортировк", "лента везёт предметы и мобов, 3 скорости, реверс", 4, "ПУСК", "СТОП", "РЕВЕРС", "СКОРОСТЬ"),
            new K("k_clock", "Часы и таймер", "часы\\s+и\\s+таймер|секундомер|таймер\\w*\\s+с\\s+тревог", "время мира, секундомер, таймер с тревогой, циферблат из 12 ламп", 4, "СЕКУНДОМЕР", "ТАЙМЕР 10С", "ТАЙМЕР 60С", "СБРОС"),
            new K("k_dome", "Силовой купол", "силов\\w*\\s+купол", "купол из стекла вырастает слоями, энергия тратится и пополняется", 3, "ВКЛ", "ВЫКЛ", "ЗАРЯД"),
            new K("k_roulette", "Рулетка", "рулетк|казино", "колесо из 12 ламп, случайный результат, призы", 4, "КРУТИТЬ", "СТАВКА 1", "СТАВКА 5", "ПРИЗ"),
            new K("k_rocket", "Стартовый стол ракеты", "стартов\\w*\\s+стол", "обратный отсчёт 10, двигатели, подъём игроков в кабине", 3, "ЗАПУСК", "ОТМЕНА", "ЗАПРАВКА"),
            new K("k_fireworks", "Салютная установка", "салютн\\w*\\s+установк", "3 шоу и финал из настоящих фейерверков", 4, "ШОУ 1", "ШОУ 2", "ШОУ 3", "ФИНАЛ"),
            new K("k_fountain", "Танцующий фонтан", "танцующ\\w*\\s+фонтан", "струи из частиц в 3 режимах и световое кольцо", 3, "ВКЛ", "ВЫКЛ", "РЕЖИМ"),
            new K("k_portcullis", "Решётка ворот", "решетк\\w*\\s+ворот|опускн\\w*\\s+решетк|герс", "тяжёлая решётка поднимается рядами с цепями и рогом", 3, "ПОДНЯТЬ", "ОПУСТИТЬ", "СТОП"),
            new K("k_trap", "Ловушка для мобов", "ловушк\\w*\\s+для\\s+моб", "враждебные мобы падают в яму, пол закрывается снова", 3, "ВКЛ", "ВЫКЛ", "ВЗВЕСТИ"),
            new K("k_metro", "Капсульное метро", "капсульн\\w*\\s+метро", "капсула везёт игроков между станциями с огнями и объявлениями", 3, "А В Б", "Б В А", "СТОП"),
            new K("k_smartlight", "Умный свет", "умн\\w*\\s+свет|умн\\w*\\s+освещен", "лампы по времени суток и присутствию игрока, 3 уровня яркости", 4, "АВТО", "ВКЛ", "ВЫКЛ", "ЯРКОСТЬ"),
            new K("k_farm", "Автоферма пшеницы", "автоферм\\w*\\s+пшениц", "посев, рост по стадиям, сбор и повторный посев поля 9x9", 4, "ПОСЕЯТЬ", "РОСТ", "СОБРАТЬ", "ПОЛИВ"),
            new K("k_gravtube", "Гравитационная труба", "гравитацион\\w*\\s+труб|труб\\w*\\s+лифт", "столб, который поднимает или опускает игрока плавно", 3, "ВВЕРХ", "ВНИЗ", "ВЫКЛ"),
            new K("k_windmill", "Ветряная электростанция", "ветряк|ветрогенератор|ветрян\\w*\\s+электростанц", "вращающиеся лопасти из 8 кадров, выработка энергии, лампы заряда", 3, "ПУСК", "СТОП", "ТОРМОЗ")};

    static K find(String k) { for (K x : ALL) if (x.key().equals(k)) return x; return ALL[0]; }

    static String detect(String t) { for (K x : ALL) if (NeroFree.has(t, x.re())) return x.key(); return null; }

    static String name(String k) { return find(k).name(); }

    static String works(String k) {
        K x = find(k);
        return "механизм на командных блоках (около 100): " + x.how() + ". Пульт маленький: кнопки на стене с табличками";
    }

    // ------------------------------------------------------------------ помощники команд
    static String ph3(int x, int y, int z) { return NeroKit.ph('x', x) + " " + NeroKit.ph('y', y) + " " + NeroKit.ph('z', z); }

    static String blk(String b) { return b.contains(":") ? b : "minecraft:" + b; }

    static String set(int x, int y, int z, String b) { return "setblock " + ph3(x, y, z) + " " + blk(b); }

    static String fill(int x1, int y1, int z1, int x2, int y2, int z2, String b) { return "fill " + ph3(x1, y1, z1) + " " + ph3(x2, y2, z2) + " " + blk(b); }

    static String repl(int x1, int y1, int z1, int x2, int y2, int z2, String b, String old) { return fill(x1, y1, z1, x2, y2, z2, b) + " replace " + blk(old); }

    static String destroy(int x1, int y1, int z1, int x2, int y2, int z2) { return fill(x1, y1, z1, x2, y2, z2, "air") + " destroy"; }

    static String pos(int x, int y, int z) { return "positioned " + ph3(x, y, z) + " "; }

    static String at(int x, int y, int z) { return "execute positioned " + ph3(x, y, z) + " run "; }

    static String snd(int x, int y, int z, String s, String v, String p) { return at(x, y, z) + "playsound minecraft:" + s + " block @a ~ ~ ~ " + v + " " + p; }

    static String par(int x, int y, int z, String a) { return at(x, y, z) + "particle minecraft:" + a; }

    static String act(int x, int y, int z, String text, String color) {
        return at(x, y, z) + "title @a[distance=..24] actionbar {\"text\":\"" + NeroKit.esc(text) + "\",\"color\":\"" + color + "\"}";
    }

    static String ttl(int x, int y, int z, String text, String color) {
        return at(x, y, z) + "title @a[distance=..24] title {\"text\":\"" + NeroKit.esc(text) + "\",\"color\":\"" + color + "\"}";
    }

    // ------------------------------------------------------------------ контекст механизма
    static final class M {
        final NeroArchetypes.B b;
        final K k;
        final int n, per;
        final String L = "{id}_k";
        final List<String[]> s = new ArrayList<>();
        final List<String> init = new ArrayList<>();

        M(NeroArchetypes.B b, K k) { this.b = b; this.k = k; this.n = k.lab().length; this.per = k.per(); }

        String bx(int i) { return "" + (1 + i % per); }

        int by(int i) { return 1 + 2 * (i / per); }

        String sc(String w, int v) { return "scoreboard players set " + w + " " + L + " " + v; }

        String ad(String w, int v) { return "scoreboard players add " + w + " " + L + " " + v; }

        String is(String w, int v) { return "execute if score " + w + " " + L + " matches " + v + " "; }

        String isr(String w, String r) { return "execute if score " + w + " " + L + " matches " + r + " "; }

        String fr0(String w, String r) { return isr(w, r); }

        String f(int a) { return is("#f", a); }

        String fr(int a, int c) { return isr("#f", a + ".." + c); }

        String p(int i) { return is("#p" + i, 1); }

        void a(String label, String cmd) { s.add(new String[]{label, cmd, label}); }

        void inputs() {
            for (int i = 0; i < n; i++) {
                String pos = NeroKit.ph('x', 1 + i % per) + " " + NeroKit.ph('y', by(i)) + " " + NeroKit.ph('z', -1);
                String pw = "execute if block " + pos + " minecraft:stone_button[powered=true] ";
                a("Кнопка «" + k.lab()[i] + "»: нажатие", pw + "if score #h" + i + " " + L + " matches 0 run " + sc("#p" + i, 1));
                a("Кнопка «" + k.lab()[i] + "»: держится", pw + "run " + sc("#h" + i, 1));
                a("Кнопка «" + k.lab()[i] + "»: отпущена", "execute unless block " + pos + " minecraft:stone_button[powered=true] run " + sc("#h" + i, 0));
            }
        }

        void toggle(int i, String var, String label) {
            a(label + ": станет 1?", p(i) + "if score " + var + " " + L + " matches 0 run " + sc("#tg", 1));
            a(label + ": станет 0?", p(i) + "if score " + var + " " + L + " matches 1 run " + sc("#tg", 0));
            a(label + ": применить", p(i) + "run scoreboard players operation " + var + " " + L + " = #tg " + L);
        }

        void finish() {
            for (int i = 0; i < n; i++) a("Сброс нажатия «" + k.lab()[i] + "»", sc("#p" + i, 0));
        }

        /** Счётчик кадров: идёт, пока #f > 0, и сбрасывается на last. */
        void frames(int last) {
            a("Кадр: счётчик", "execute if score #f " + L + " matches 1.. run " + ad("#f", 1));
            a("Конец сценария", isr("#f", last + "..") + "run " + sc("#f", 0));
        }
    }

    /** Пульт: кнопки по per в ряд, под каждой табличка, лампа справа. Игрок стоит перед стеной (z меньше 0). */
    static void console(NeroArchetypes.B b, K k) {
        int n = k.lab().length, per = k.per(), rows = (n + per - 1) / per;
        b.part("Пульт управления");
        b.box(0, -1, -2, per + 1, -1, 2, "smooth_stone");
        b.box(0, 0, 0, per + 1, 2 * rows, 0, "black_concrete");
        for (int i = 0; i < n; i++) {
            int x = 1 + i % per, y = 1 + 2 * (i / per);
            b.set(x, y, -1, "stone_button[face=wall,facing=north]");
            String lab = k.lab()[i];
            b.setup(NeroKit.sign(x, y - 1, -1, lab.length() > 15 ? lab.substring(0, 15) : lab, "", "", ""));
        }
        b.set(per + 1, 1, 0, "red_concrete");
    }

    static JsonObject bp(String key, String t) {
        K k = find(key);
        NeroArchetypes.B b = new NeroArchetypes.B();
        console(b, k);
        M m = new M(b, k);
        for (int i = 0; i < m.n; i++) { m.init.add(m.sc("#p" + i, 0)); m.init.add(m.sc("#h" + i, 0)); }
        m.init.add(m.sc("#f", 0));
        m.init.add(m.sc("#tg", 0));
        m.inputs();
        switch (key) {
            case "k_airlock" -> NeroMechA.airlock(m);
            case "k_codelock" -> NeroMechA.codelock(m);
            case "k_traffic" -> NeroMechA.traffic(m);
            case "k_disco" -> NeroMechA.disco(m);
            case "k_radar" -> NeroMechA.radar(m);
            case "k_tphub" -> NeroMechA.tphub(m);
            case "k_bridge" -> NeroMechA.bridge(m);
            case "k_alarm" -> NeroMechA.alarm(m);
            case "k_drill" -> NeroMechB.drill(m);
            case "k_range" -> NeroMechB.range(m);
            case "k_vending" -> NeroMechB.vending(m);
            case "k_conveyor" -> NeroMechB.conveyor(m);
            case "k_clock" -> NeroMechB.clock(m);
            case "k_dome" -> NeroMechB.dome(m);
            case "k_roulette" -> NeroMechB.roulette(m);
            case "k_rocket" -> NeroMechC.rocket(m);
            case "k_fireworks" -> NeroMechC.fireworks(m);
            case "k_fountain" -> NeroMechC.fountain(m);
            case "k_portcullis" -> NeroMechC.portcullis(m);
            case "k_trap" -> NeroMechC.trap(m);
            case "k_metro" -> NeroMechC.metro(m);
            case "k_smartlight" -> NeroMechC.smartlight(m);
            case "k_farm" -> NeroMechC.farm(m);
            case "k_gravtube" -> NeroMechC.gravtube(m);
            case "k_windmill" -> NeroMechC.windmill(m);
            default -> { }
        }
        m.finish();
        b.chain(k.name() + ": мозг", m.s.toArray(new String[0][]));
        List<String> su = new ArrayList<>();
        su.add("scoreboard objectives add " + m.L + " dummy");
        su.addAll(m.init);
        b.setup(su.toArray(new String[0]));
        b.help = k.name() + ": " + k.how() + ". Кнопки на пульте с табличками. Командных блоков в мозге: " + m.s.size() + ".";
        return b.done();
    }

    private NeroMech() { }
}
