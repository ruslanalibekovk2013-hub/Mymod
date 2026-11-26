package com.mackros;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Локальный режим: разбор запроса по правилам, диалог с уточнениями, изменение текущего проекта. Работает без интернета. */
public final class LocalProvider implements AIProvider {
    private static final String[][] KINDS = {
            {"homes", "точк\\w* дом|систем\\w* дом"},
            {"secret", "секретн\\w*\\s*(комнат|проход|помещ|дверь)|потайн|тайн\\w* комнат|скрыт\\w* комнат"},
            {"bunker", "бункер|убежищ|секретн\\w* баз|подземн\\w* баз|подземн\\w* дом"},
            {"castle", "замок|крепост"},
            {"airport", "аэропорт|взлетн|ангар для самолет"},
            {"station", "станци|железнодорож|платформ\\w* для поезд|вокзал"},
            {"tunnel", "тоннел|туннел"},
            {"bridge", "мост"},
            {"road", "дорог|шоссе|трасс"},
            {"parking", "парков|стоянк"},
            {"factory", "фабрик|завод"},
            {"warehouse", "склад"},
            {"tower", "небоскр|башн"},
            {"hotel", "отел|гостиниц"},
            {"house", "(?<![а-я])(дом(?!ой)\\w*|вилл\\w*|особняк\\w*|коттедж\\w*|хижин\\w*)"},
            {"vault", "кодов\\w*\\s*замок|сейф|замок с кодом"},
            {"belt", "эскалатор|конвейер|транспортер|автоматическ\\w*\\s*лестниц|траволатор"},
            {"lift", "лифт|подъемн\\w*\\s*платформ"},
            {"tele", "телепорт"},
            {"gate_key", "ворот\\w*.*ключ|ключ.*ворот"},
            {"gate", "ворот|раздвижн\\w*\\s*стен|шлагбаум|автоматическ\\w*\\s*двер|автодвер"},
            {"farm", "ферм|огород|грядк"},
            {"miner", "автобур|бур\\b|шахтер|добыч"},
            {"sentry", "турел|охранн\\w* пушк"},
            {"dome", "купол"},
            {"wave", "волн\\w* мобов|арен"},
            {"show", "шоу|фейерверк"},
            {"weather", "погод|метео"},
            {"alarm", "сигнализ"},
            {"lamps", "фонар|уличн\\w* освещ"},
            {"house", "гараж|жиль"}
    };

    private static final Map<String, Integer> NUM = Map.ofEntries(
            Map.entry("один", 1), Map.entry("одн", 1), Map.entry("два", 2), Map.entry("двух", 2), Map.entry("три", 3), Map.entry("трех", 3),
            Map.entry("четыре", 4), Map.entry("четырех", 4), Map.entry("пять", 5), Map.entry("пяти", 5), Map.entry("шесть", 6), Map.entry("шести", 6),
            Map.entry("семь", 7), Map.entry("семи", 7), Map.entry("восемь", 8), Map.entry("восьми", 8), Map.entry("девять", 9), Map.entry("девяти", 9),
            Map.entry("десять", 10), Map.entry("десяти", 10), Map.entry("двенадцат", 12), Map.entry("пятнадцат", 14));

    @Override public String name() { return "Локальный"; }

    @Override
    public CompletableFuture<NeroResponse> process(NeroRequest r) { return CompletableFuture.completedFuture(parse(r)); }

    static String norm(String s) { return s.toLowerCase(Locale.ROOT).replace('ё', 'е'); }

    static String kindOf(String t) {
        for (String[] k : KINDS) if (Pattern.compile(k[1]).matcher(t).find()) return k[0];
        return null;
    }

    private static int floorsOf(String t) {
        Matcher m = Pattern.compile("(\\d+)\\s*[- ]?(?:й|х|ти)?\\s*этаж").matcher(t);
        if (m.find()) return Integer.parseInt(m.group(1));
        m = Pattern.compile("(\\d+)\\s*[- ]?(?:й|ти)?\\s*(?:уровн|ярус)").matcher(t);
        if (m.find()) return Integer.parseInt(m.group(1));
        m = Pattern.compile("([а-я]+?)\\s*-?\\s*этаж").matcher(t);
        while (m.find()) {
            String w = m.group(1);
            for (Map.Entry<String, Integer> e : NUM.entrySet()) if (w.startsWith(e.getKey()) || w.endsWith(e.getKey())) return e.getValue();
        }
        return 0;
    }

    private static int ordinalFloor(String t) {
        String[][] o = {{"втор", "2"}, {"трет", "3"}, {"четверт", "4"}, {"пят", "5"}, {"шест", "6"}};
        for (String[] x : o) if (Pattern.compile(x[0] + "\\w*\\s+этаж").matcher(t).find()) return Integer.parseInt(x[1]);
        return 0;
    }

    private static String styleOf(String t) {
        if (t.contains("современ") || t.contains("модерн") || t.contains("хай-тек")) return "modern";
        if (t.contains("средневек") || t.contains("классич") || t.contains("каменн") || t.contains("готич")) return "medieval";
        if (t.contains("футурист") || t.contains("будущ") || t.contains("космич") || t.contains("sci")) return "futuristic";
        return null;
    }

    private static int numAfter(String t, String re) {
        Matcher m = Pattern.compile(re).matcher(t);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static String sideOf(String t) {
        if (t.matches(".*(вправо|справа|правее|направо|правой).*")) return "right";
        if (t.matches(".*(влево|слева|левее|налево|левой).*")) return "left";
        if (t.matches(".*(назад|сзади|за домом|позади).*")) return "back";
        return null;
    }

    private static void applyExtras(String t, NeroSpec s, boolean allowRemove) {
        boolean rm = allowRemove && t.contains("убер");
        String[][] x = {{"garage", "гараж"}, {"pool", "бассейн|басейн"}, {"lighting", "освещ|подсвет|фонар|свет"}, {"autogate", "автоматическ\\w*\\s*ворот|автоворот|ворот\\w*\\s*автомат"},
                {"lift", "лифт"}, {"furniture", "мебел"}, {"alarm", "сигнализ"}};
        for (String[] e : x) {
            if (!Pattern.compile(e[1]).matcher(t).find()) continue;
            if (e[0].equals("lift") && s.kind.equals("lift")) continue;
            if (e[0].equals("alarm") && s.kind.equals("alarm")) continue;
            if (rm) s.extras.remove(e[0]); else s.extras.add(e[0]);
        }
        if (s.extras.contains("autogate") && t.contains("ворот") && !s.kind.equals("house") && !s.kind.equals("castle") && !s.kind.equals("gate")) { /* ворота как свойство */ }
    }

    private static void applyDetails(String t, NeroSpec s) {
        String st = styleOf(t);
        if (st != null) s.style = st;
        int fl = floorsOf(t);
        if (fl > 0) s.floors = NeroSpec.clamp(fl, 1, s.kind.equals("lift") || s.kind.equals("tower") || s.kind.equals("hotel") ? 14 : 40);
        int len = numAfter(t, "длин\\w*\\s*(\\d+)");
        if (len == 0) len = numAfter(t, "(\\d+)\\s*(?:блок|метр)");
        if (len > 0 && !s.kind.equals("lift")) s.len = NeroSpec.clamp(len, 4, 120);
        int w = numAfter(t, "ширин\\w*\\s*(\\d+)");
        if (w > 0) s.w = NeroSpec.clamp(w, 5, 64);
        int d = numAfter(t, "глубин\\w*\\s*(\\d+)");
        if (d > 0) s.d = NeroSpec.clamp(d, 5, 64);
        int code = numAfter(t, "код\\w*\\s*[:=]?\\s*(\\d{4})");
        if (code > 0) s.code = code;
        String side = null;
        Matcher gm = Pattern.compile("гараж\\w*\\s*(справа|слева|сзади|вправо|влево|назад)").matcher(t);
        if (gm.find()) side = sideOf(gm.group(1));
        if (side != null) s.garageSide = side;
        if (Pattern.compile("(два|2|еще один|ещё один|еще)\\s*лифт").matcher(t).find() && s.kind.equals("lift")) s.copies = Math.max(s.copies, 2);
    }

    private static boolean isModification(String t) {
        return Pattern.compile("(добав|сдела|измен|убер|перемест|установ|выше|ниже|больше|меньше|шире|уже|длинн|коротк|еще|ещё|код|замени|поставь|подними|опусти)").matcher(t).find();
    }

    static boolean explicitNew(String t) {
        return Pattern.compile("(построй|создай|сгенерируй|нужен|нужна|нужно|хочу|сделай мне|новый|новая|проектируй)").matcher(t).find();
    }

    private static boolean yes(String t) { return Pattern.compile("^(да|ага|давай|конечно|нужн|ок|ладно|yes|хочу|добавь)").matcher(t.trim()).find() || t.contains("да,") || t.contains("да "); }
    private static boolean no(String t) { return Pattern.compile("^(нет|не надо|не нужн|no|без)").matcher(t.trim()).find(); }

    static NeroResponse parse(NeroRequest r) {
        String t = norm(r.text);
        NeroSpec cur = r.current == null ? null : r.current.copy();
        String pending = r.pending;
        String kind = kindOf(t);
        NeroResponse cr = custom(r, t, cur, kind);
        if (cr != null) return cr;
        boolean fresh = kind != null && (cur == null || explicitNew(t) || (pending == null && !isModification(t)) || (!kind.equals(cur.kind) && !isModification(t)));
        StringBuilder reply = new StringBuilder();

        if (fresh) {
            cur = new NeroSpec();
            cur.kind = kind;
            cur.seed = new java.util.Random().nextLong() | 1L;
            applyDetails(t, cur);
            applyExtras(t, cur, false);
            pending = null;
            reply.append("Принято: ").append(cur.title()).append(". ");
        } else if (cur != null) {
            // ответ на вопрос или изменение существующего проекта
            if ("extras".equals(pending)) {
                if (no(t) && !t.contains("гараж") && !t.contains("бассейн")) { cur.askedExtras = true; reply.append("Хорошо, без дополнений. "); }
                else if (yes(t) && !t.contains("только")) { if (!t.contains("бассейн") || t.contains("гараж")) cur.extras.add("garage"); if (!t.contains("гараж") || t.contains("бассейн")) cur.extras.add("pool"); cur.askedExtras = true; }
                else cur.askedExtras = true;
            }
            int before = cur.floors;
            applyDetails(t, cur);
            applyExtras(t, cur, true);
            boolean changed = false;
            int ord = ordinalFloor(t);
            if (Pattern.compile("(добав\\w*\\s+(еще\\s+|ещё\\s+)?(\\w+\\s+)?этаж|надстро|выше|повыс|подними)").matcher(t).find()) {
                cur.floors = ord > 0 ? ord : Math.max(1, (cur.floors == 0 ? defaultFloors(cur.kind) : cur.floors) + 1);
                if (cur.kind.equals("house") || cur.kind.equals("lift") || cur.kind.equals("tower") || cur.kind.equals("hotel")) changed = true;
                if (!changed) { cur.len = Math.max(cur.len, 30) + 10; changed = true; }
            }
            if (Pattern.compile("(ниже|убер\\w*\\s+этаж|опусти|меньше этаж)").matcher(t).find() && !t.contains("убери бассейн")) { cur.floors = Math.max(1, (cur.floors == 0 ? defaultFloors(cur.kind) : cur.floors) - 1); changed = true; }
            if (Pattern.compile("(еще|ещё)\\s+(один\\s+)?лифт").matcher(t).find()) {
                if (cur.kind.equals("lift")) cur.copies = Math.min(4, cur.copies + 1); else cur.extras.add("lift");
                changed = true;
            }
            if (Pattern.compile("(шире|больше размер|увелич)").matcher(t).find()) { cur.w = (cur.w == 0 ? 13 : cur.w) + 4; cur.d = (cur.d == 0 ? 11 : cur.d) + 4; changed = true; }
            if (Pattern.compile("(длиннее|удлин)").matcher(t).find()) { cur.len = Math.min(120, (cur.len == 0 ? 40 : cur.len) + 10); changed = true; }
            if (Pattern.compile("перемест\\w*\\s+гараж").matcher(t).find()) {
                String sd = sideOf(t);
                if (sd != null) { cur.garageSide = sd; cur.extras.add("garage"); changed = true; }
            }
            if (cur.floors != before || styleOf(t) != null || t.contains("код") || t.contains("добав") || t.contains("убер") || t.contains("сдела") || t.contains("установ")) changed = true;
            if (!changed && !"extras".equals(pending) && !"style".equals(pending)) {
                return new NeroResponse(true, null, "Не понял изменение. Примеры: «сделай выше», «добавь третий этаж», «добавь ещё один лифт», «установи код 2580», «перемести гараж влево».", pending, null, "Локальный");
            }
            if (!"extras".equals(pending)) reply.append("Изменяю проект. ");
        } else {
            return new NeroResponse(true, null, "Локальный режим не узнал такую конструкцию. Уточни, из каких частей она должна состоять и что должна делать. Без интернета я умею: дома и базы с любыми механизмами, лифты, 3D-принтер, корабль, робот, мост, поезд, компьютер, портал. Всё остальное (замок, дракон, станция, любое новое устройство) спроектирует внешний ИИ: вставь токен OpenRouter вверху окна NERO. Без токена я работаю только по готовым шаблонам.", null, null, "Локальный");
        }

        // следующий уточняющий вопрос (диалог)
        String q = null, nextPending = null;
        if (cur.kind.equals("house")) {
            if (cur.style == null && !cur.askedStyle) {
                cur.askedStyle = true;
                q = "Какой дом построить? Могу использовать современный, средневековый или футуристический стиль. Сколько этажей?";
                nextPending = "style";
            } else if (cur.extras.isEmpty() && !cur.askedExtras) {
                if (cur.floors == 0) cur.floors = 2;
                q = "Этажей: " + cur.floors + ". Нужны гараж и бассейн?";
                nextPending = "extras";
            }
        }
        if (cur.kind.equals("house") && cur.floors == 0) cur.floors = 2;
        if (q != null) return new NeroResponse(true, cur, reply + q, nextPending, null, "Локальный");
        reply.append("Проект готов: ").append(cur.summary()).append(". Нажми «Предпросмотр» или «Построить».");
        return new NeroResponse(true, cur, reply.toString(), null, null, "Локальный");
    }

    /** Свободная постройка без интернета: здание + любые механизмы по ключевым словам. Возвращает null, если подходит обычный шаблон. */
    private static NeroResponse custom(NeroRequest r, String t, NeroSpec cur, String kind) {
        boolean mech = NeroFree.has(t, "принтер|3d|3д|ферм|турел|телепорт|сейф|кодов|шоу|купол|конвейер|эскалатор|погод|автобур|шахтер|арен|сигнализ|ворот|гараж|бассейн|лифт");
        boolean base = NeroFree.has(t, "баз[аыуе]|комплекс|штаб|лаборатор|центр|резиденц|умн\\w* дом");
        boolean houseMulti = "house".equals(kind) && NeroFree.has(t, "ферм|турел|телепорт|сейф|кодов|шоу|купол|принтер|3d|3д|конвейер|погод|шахтер|арен|умн");
        boolean nextCustom = cur != null && cur.kind.equals("custom") && !explicitNew(t);
        boolean arch = NeroArchetypes.detect(t) != null;
        boolean wants = arch || base || houseMulti || NeroFree.has(t, "принтер|3d|3д") || (kind == null && cur == null && (mech || NeroFree.wantsBuilding(t)));
        if (!wants && !nextCustom) return null;
        NeroSpec s;
        if (nextCustom && cur.bp != null && NeroConfig.remoteReady())
            return new NeroResponse(true, null, "Этот проект собран по чертежу: правь его в режиме «Вопрос» окна NERO, тогда изменится только нужный компонент, а остальное сохранится. Чтобы создать новый проект, начни запрос со слова «Создай».", null, null, "Локальный");
        if (nextCustom) {
            s = cur.copy();
            s.desc = s.desc + " " + t;
            s.bp = null;
        } else {
            s = new NeroSpec();
            s.kind = "custom";
            s.desc = t;
            s.seed = new java.util.Random().nextLong() | 1L;
        }
        applyDetails(t, s);
        java.util.List<String> f = NeroFree.features(s.desc);
        boolean bld = NeroFree.wantsBuilding(s.desc);
        String ak = NeroArchetypes.detect(s.desc);
        if (ak != null && (!ak.equals("printer") || (!bld && f.size() <= 1))) s.bp = NeroArchetypes.bp(ak, s.desc);
        s.name = ak != null ? "Проект: " + NeroArchetypes.nameOf(ak) : (bld ? "Постройка" : "Установка") + (f.isEmpty() ? "" : ": " + String.join(", ", f));
        if (s.name.length() > 60) s.name = s.name.substring(0, 60);
        String rp = ak != null ? ("Спроектировал локально: " + NeroArchetypes.nameOf(ak) + ". Работает: " + NeroArchetypes.works(ak) + ". Нажми «КБ», чтобы посмотреть подписи командных блоков, затем «Предпросмотр» или «Построить».")
                : (nextCustom ? "Обновил свободный проект. " : "Собрал свободный проект локально. ") + "Состав: " + (bld ? "здание" : "без здания") + (f.isEmpty() ? "" : ", " + String.join(", ", f))
                + ". Нажми «Предпросмотр» или «Построить». Справа от стройки будут пульт с кнопками и командный отсек.";
        return new NeroResponse(true, s, rp, null, null, "Локальный");
    }

    private static int defaultFloors(String kind) {
        return switch (kind) { case "house" -> 2; case "tower" -> 10; case "hotel" -> 8; case "lift" -> 5; default -> 1; };
    }
}
