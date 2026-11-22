package com.mackros;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Извлечение требований из запроса (каждое получает ID) и трассировка: где в плане реализовано каждое требование.
 * Статус «проверено командами» ставится только если требование связано с механизмом/командами и проверка проекта прошла.
 * Проверка в самой игре не выполняется, поэтому такого статуса нет.
 */
public final class NeroRequirements {
    public static final class Req {
        public String id = "", text = "", category = "особые пожелания", interp = "", component = "", how = "", status = "не проверено", result = "";

        public Req copy() {
            Req r = new Req();
            r.id = id; r.text = text; r.category = category; r.interp = interp; r.component = component; r.how = how; r.status = status; r.result = result;
            return r;
        }
    }

    private static final String[][] CATS = {
            {"ограничения", "\\bбез\\b|\\bтолько\\b|нельзя|не должн|не надо|ограничен|максимум|минимум"},
            {"условия работы", "\\bесли\\b|\\bкогда\\b|\\bпри\\b |после того|пока "},
            {"связи между компонентами", "связан|подключ|соедин|зависит|управля\\w+ |от кнопк"},
            {"частицы", "частиц|искр|дым|эффект"},
            {"анимации", "анимац|плавн|двигающ|подвижн|движен|вращ|движет"},
            {"интерфейс", "кнопк|экран|дисплей|панел|индикатор|меню|табло"},
            {"размеры", "\\d+\\s*(x|х|на)\\s*\\d+|размер|высот|ширин|длин|в несколько раз|масштаб|\\d+\\s*этаж|больш|маленьк|уменьшен|\\d+\\s*блок"},
            {"количество компонентов", "\\d+\\s+\\p{L}+|несколько|пара |две |три |четыре "},
            {"расположение", "слева|справа|рядом|над |под |внутри|снаружи|сзади|спереди|посередине|между"},
            {"цвета", "красн|син|зелен|желт|жёлт|чёрн|черн|бел|серый|серая|оранж|фиолет|розов|цветн|подсвет"},
            {"материалы", "стекл|камен|метал|дерев|кварц|бетон|железн|золот|алмаз|кирпич|неонов"},
            {"форма", "форм|круг|куб|пирамид|арк|купол|цилиндр|шар"},
            {"мелкие детали", "лампа|лампоч|рамк|вставк|направляющ|соединен|индикатор|декор|подсветк"},
            {"механика", "механизм|работает|запуск|автомат|открыва|закрыва|печат|сканир|телепорт|лифт|двер"},
            {"взаимодействие", "нажим|кликн|игрок|взаимодейств|выбор|вызов"},
            {"назначение", "для того|чтобы|нужн|предназнач"},
    };

    public static String classify(String t) {
        String l = t.toLowerCase(Locale.ROOT);
        for (String[] c : CATS) if (Pattern.compile(c[1]).matcher(l).find()) return c[0];
        return "особые пожелания";
    }

    /** Полное разбиение запроса на требования: ни одно предложение или пункт списка не отбрасывается. */
    public static List<Req> extract(String text) {
        List<Req> out = new ArrayList<>();
        if (text == null) return out;
        String t = text.replace('\r', ' ').trim();
        String[] clauses = t.split("(?<=[.!?;])\\s+|\\n+|\\s+(?:а также|также|плюс|и ещё|и еще)\\s+|,\\s+(?=(?:и\\s+)?(?:добавь|сделай|пусть|чтобы|причём|причем|а\\s))");
        int n = 0;
        for (String c0 : clauses) {
            String c = c0.trim().replaceAll("^[,;:\\-–—\\s]+|[.!?;\\s]+$", "");
            if (c.length() < 3) continue;
            int commas = c.length() - c.replace(",", "").length();
            List<String> parts = new ArrayList<>();
            if (commas >= 2) {
                for (String p : c.split(",\\s*")) { String q = p.trim().replaceFirst("^(?i)и\\s+", ""); if (q.length() >= 3) parts.add(q); }
            } else parts.add(c);
            for (String p : parts) {
                Req r = new Req();
                r.id = "R" + (++n);
                r.text = p;
                r.category = n == 1 ? "основной объект" : classify(p);
                out.add(r);
            }
        }
        return out;
    }

    public static JsonArray toJson(List<Req> l) {
        JsonArray a = new JsonArray();
        for (Req r : l) {
            JsonObject o = new JsonObject();
            o.addProperty("id", r.id); o.addProperty("text", r.text); o.addProperty("category", r.category); o.addProperty("interp", r.interp);
            o.addProperty("component", r.component); o.addProperty("how", r.how); o.addProperty("status", r.status); o.addProperty("result", r.result);
            a.add(o);
        }
        return a;
    }

    private static String s(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }

    public static List<Req> fromJson(JsonArray a) {
        List<Req> l = new ArrayList<>();
        if (a == null) return l;
        int n = 0;
        for (JsonElement e : a) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            Req r = new Req();
            r.text = s(o, "text");
            if (r.text.isBlank()) continue;
            r.id = s(o, "id").isEmpty() ? "R" + (++n) : s(o, "id");
            String c = s(o, "category");
            r.category = c.isEmpty() ? classify(r.text) : c;
            r.interp = s(o, "interp").isEmpty() ? s(o, "interpretation") : s(o, "interp");
            r.component = s(o, "component");
            r.how = s(o, "how").isEmpty() ? s(o, "implementation") : s(o, "how");
            if (!s(o, "status").isEmpty()) r.status = s(o, "status");
            r.result = s(o, "result");
            l.add(r);
        }
        return l;
    }

    private static Set<String> stems(String text) {
        Set<String> r = new HashSet<>();
        for (String t : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) if (t.length() >= 4) r.add(t.length() > 5 ? t.substring(0, 5) : t);
        return r;
    }

    /** Сопоставляет требования с компонентами плана и выставляет честные статусы. */
    public static void trace(List<Req> reqs, NeroBuildPlan plan, NeroValidator.Report report) {
        if (plan == null) return;
        boolean ok = report != null && report.ok();
        List<String[]> comps = new ArrayList<>(); // {имя, тип (mech|struct), текст для поиска}
        for (NeroBuildPlan.Element e : plan.elements) comps.add(new String[]{e.name(), "mechanism".equals(e.kind()) ? "mech" : "struct", (e.name() + " " + e.logic() + " " + e.activation()).toLowerCase(Locale.ROOT)});
        for (NeroBuildPlan.Cb k : plan.cbs) comps.add(new String[]{k.label(), "mech", (k.label() + " " + k.role()).toLowerCase(Locale.ROOT)});
        for (String[] c : plan.controls) comps.add(new String[]{c[0], "mech", c[0].toLowerCase(Locale.ROOT)});
        for (Req r : reqs) {
            String[] best = null;
            if (!r.component.isBlank()) {
                String cc = r.component.toLowerCase(Locale.ROOT);
                for (String[] c : comps) if (c[2].contains(cc) || cc.contains(c[0].toLowerCase(Locale.ROOT))) { best = c; break; }
            }
            if (best == null) {
                Set<String> rs = stems(r.text);
                int bestHits = 0;
                for (String[] c : comps) {
                    Set<String> cs = stems(c[2]);
                    int h = 0;
                    for (String x : rs) if (cs.contains(x)) h++;
                    if (h > bestHits) { bestHits = h; best = c; }
                }
                if (bestHits == 0 || (rs.size() >= 3 && bestHits * 4 < rs.size())) best = null;
            }
            if (best == null) { r.status = "Не найдено в плане"; r.result = "для этого требования не найден компонент"; }
            else if (best[1].equals("mech")) { r.status = ok ? "Проверено командами" : "Есть в плане, проверка не пройдена"; r.result = "компонент «" + best[0] + "»"; }
            else { r.status = "Есть в плане (в игре не проверено)"; r.result = "компонент «" + best[0] + "»"; }
        }
    }

    private NeroRequirements() { }
}
