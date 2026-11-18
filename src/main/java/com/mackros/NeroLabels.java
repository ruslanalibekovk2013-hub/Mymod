package com.mackros;

import java.util.HashMap;
import java.util.Map;

/** Автоматические подписи и описания командных блоков (если ИИ/механизм не задал их сам). */
public final class NeroLabels {
    private static String cut(String s, int n) { return s.length() > n ? s.substring(0, n - 1) + "…" : s; }

    /** Возвращает {подпись, описание действия} по тексту команды. */
    public static String[] describe(String cmd) {
        String c = cmd == null ? "" : cmd.trim();
        boolean cond = c.startsWith("execute") && (c.contains(" if ") || c.contains(" unless "));
        String act = c;
        int ri = c.lastIndexOf(" run ");
        if (c.startsWith("execute") && ri > 0) act = c.substring(ri + 5).trim();
        String[] t = act.split("\\s+");
        String v = t.length > 0 ? t[0] : "";
        String label;
        String what;
        switch (v) {
            case "clone" -> { label = "Копирование области"; what = "копирует блоки из одной области в другую"; }
            case "fill" -> {
                boolean air = act.endsWith(" minecraft:air") || act.endsWith(" air");
                label = air ? "Очистка области" : "Заполнение области";
                what = air ? "убирает блоки в области" : "заполняет область блоками";
            }
            case "setblock" -> { label = "Установка блока"; what = "ставит один блок"; }
            case "tp", "teleport" -> { label = "Телепортация"; what = "перемещает игрока или сущность"; }
            case "effect" -> { label = "Эффект"; what = "выдаёт или снимает эффект"; }
            case "particle" -> { label = "Частицы"; what = "показывает частицы"; }
            case "playsound" -> { label = "Звук"; what = "проигрывает звук"; }
            case "title", "tellraw" -> { label = "Сообщение"; what = "показывает сообщение игрокам"; }
            case "summon" -> { label = "Создание сущности"; what = "создаёт сущность (моба, предмет, вагонетку)"; }
            case "kill" -> { label = "Удаление сущностей"; what = "удаляет подходящие сущности"; }
            case "data" -> { label = "Изменение данных"; what = "меняет данные блока или сущности"; }
            case "tag" -> { label = "Метка"; what = "ставит или снимает метку сущности"; }
            case "weather" -> { label = "Погода"; what = "меняет погоду"; }
            case "time" -> { label = "Время"; what = "меняет время суток"; }
            case "damage" -> { label = "Урон"; what = "наносит урон"; }
            case "gamerule" -> { label = "Правило игры"; what = "меняет правило мира"; }
            case "schedule" -> { label = "Отложенный запуск"; what = "запускает функцию позже"; }
            case "scoreboard" -> {
                if (act.contains("objectives add")) { label = "Создание счётчика"; what = "создаёт счётчик, который хранит состояние механизма"; }
                else if (act.contains("players enable")) { label = "Разрешение /trigger"; what = "разрешает игроку использовать /trigger"; }
                else if (act.contains("players add")) { label = "Увеличение счётчика"; what = "увеличивает счётчик (таймер или шаг)"; }
                else if (act.contains("players reset")) { label = "Сброс счётчика"; what = "сбрасывает счётчик"; }
                else { label = "Установка счётчика"; what = "записывает значение счётчика (состояние механизма)"; }
            }
            default -> { label = "Команда " + v; what = "выполняет команду " + v; }
        }
        if (cond) {
            label = "Проверка → " + label;
            int e = c.indexOf(" run ");
            String cd = e > 0 ? c.substring(7, e).trim() : "";
            what = what + ", только если выполнено условие: " + cut(cd, 90);
        }
        return new String[]{cut(label, 44), cut(what, 160)};
    }

    /** Заполняет подписи всех командных блоков плана, где они пусты. */
    public static void apply(NeroBuildPlan pl) {
        Map<Integer, Integer> total = new HashMap<>(), seen = new HashMap<>();
        for (NeroBuildPlan.Cb k : pl.cbs) total.merge(k.el(), 1, Integer::sum);
        for (int i = 0; i < pl.cbs.size(); i++) {
            NeroBuildPlan.Cb k = pl.cbs.get(i);
            int step = seen.merge(k.el(), 1, Integer::sum);
            if (!k.label().isEmpty()) continue;
            String[] d = describe(k.cmd());
            String ctx = "";
            if (k.el() >= 0 && k.el() < pl.elements.size()) {
                NeroBuildPlan.Element e = pl.elements.get(k.el());
                ctx = "Механизм «" + e.name() + "»" + (e.logic() == null || e.logic().isBlank() || e.logic().equals("-") ? "" : " (" + cut(e.logic(), 80) + ")") + ". ";
            }
            String role = ctx + "Шаг " + step + " из " + total.getOrDefault(k.el(), 1) + ": " + d[1] + ".";
            pl.cbs.set(i, k.named(d[0], role));
        }
    }

    private NeroLabels() { }
}
