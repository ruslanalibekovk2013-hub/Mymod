package com.mackros;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Подробный отчёт о проекте: обзор, компоненты, мелкие детали, механика, командные блоки, выполнение требований, ограничения. */
public final class NeroReport {
    private static String cut(String s, int n) { return s == null ? "" : s.length() > n ? s.substring(0, n - 1) + "…" : s; }

    public static List<String> make(NeroSpec spec, NeroBuildPlan plan, NeroValidator.Report rep) {
        List<String> L = new ArrayList<>();
        if (spec == null || plan == null) { L.add("Отчёт недоступен: проект ещё не рассчитан."); return L; }
        NeroRequirements.trace(spec.reqs, plan, rep);
        L.add("== ОБЗОР ==");
        L.add("Проект: " + spec.title());
        if (spec.desc != null && !spec.desc.isBlank()) L.add("Назначение (по запросу): " + cut(spec.desc, 200));
        L.add("Размер: " + plan.sx() + "x" + plan.sy() + "x" + plan.sz() + ", блоков: " + plan.solid() + ", механизмов: " + plan.mechanisms() + ", командных блоков: " + plan.cbs.size());
        for (String h : plan.help) if (L.size() < 12) L.add("• " + cut(h, 180));

        L.add("== КОМПОНЕНТЫ ==");
        int ne = plan.elements.size();
        int[][] bb = new int[ne][6];
        for (int[] b : bb) { b[0] = b[1] = b[2] = Integer.MAX_VALUE; b[3] = b[4] = b[5] = Integer.MIN_VALUE; }
        for (NeroBuildPlan.Block b : plan.blocks) {
            if (b.st().isAir() || b.el() < 0 || b.el() >= ne) continue;
            int[] q = bb[b.el()];
            q[0] = Math.min(q[0], b.x()); q[1] = Math.min(q[1], b.y()); q[2] = Math.min(q[2], b.z());
            q[3] = Math.max(q[3], b.x()); q[4] = Math.max(q[4], b.y()); q[5] = Math.max(q[5], b.z());
        }
        for (int i = 0; i < ne; i++) {
            NeroBuildPlan.Element e = plan.elements.get(i);
            String where = bb[i][0] == Integer.MAX_VALUE ? "только команды" : "от " + (bb[i][0] - plan.minX) + "," + (bb[i][1] - plan.minY) + "," + (bb[i][2] - plan.minZ) + " размер " + (bb[i][3] - bb[i][0] + 1) + "x" + (bb[i][4] - bb[i][1] + 1) + "x" + (bb[i][5] - bb[i][2] + 1);
            StringBuilder deps = new StringBuilder();
            for (int d : e.deps()) if (d >= 0 && d < ne) deps.append(deps.length() > 0 ? ", " : "").append(plan.elements.get(d).name());
            L.add("• " + e.name() + " (" + where + ")" + (e.logic() != null && !e.logic().equals("-") ? ": " + cut(e.logic(), 90) : "") + (deps.length() > 0 ? "; связан с: " + deps : ""));
            if (L.size() > 60) { L.add("… и ещё компонентов: " + (ne - i - 1)); break; }
        }

        L.add("== МЕЛКИЕ ДЕТАЛИ ==");
        int det = 0;
        String dre = "кнопк|индикатор|ламп|панел|рамк|вставк|подсвет|экран|дисплей|направля|сканер|контроллер|головк|хранилищ|фонар|табличк|антенн";
        for (NeroBuildPlan.Element e : plan.elements)
            if (NeroFree.has(e.name().toLowerCase(Locale.ROOT), dre)) { L.add("• " + e.name()); det++; }
        for (String[] c : plan.controls) { L.add("• кнопка пульта: " + c[0]); det++; }
        if (det == 0) L.add("Отдельных мелких деталей в плане нет (если они нужны, попроси их в чате).");

        L.add("== МЕХАНИКА ==");
        int mech = 0;
        for (NeroBuildPlan.Element e : plan.elements)
            if ("mechanism".equals(e.kind())) { L.add("• " + e.name() + ": запуск — " + cut(e.activation(), 80) + "; работа — " + cut(e.logic(), 100)); mech++; }
        if (mech == 0) L.add("Механизмов с командами нет: конструкция статичная.");

        L.add("== КОМАНДНЫЕ БЛОКИ ==");
        int imp = 0, ch = 0, rp = 0;
        for (NeroBuildPlan.Cb k : plan.cbs) { if (k.type() == NeroBuildPlan.CbType.IMPULSE) imp++; else if (k.type() == NeroBuildPlan.CbType.CHAIN) ch++; else rp++; }
        L.add("Всего " + plan.cbs.size() + ": импульсных " + imp + ", цепочек " + ch + ", повторяющихся " + rp);
        int shown = 0;
        for (NeroBuildPlan.Cb k : plan.cbs) { if (shown++ >= 10) { L.add("… остальные в окне «КБ»"); break; } L.add("• " + k.label()); }
        L.add(rep == null ? "Проверка: не выполнялась" : rep.ok() ? "Проверка проекта: ошибок нет (пройдено проверок: " + rep.passed.size() + "). В самой игре работа автоматически не проверялась." : "Проверка проекта: ЕСТЬ ОШИБКИ (" + rep.errors.size() + "), проект не готов");

        L.add("== ВЫПОЛНЕНИЕ ТРЕБОВАНИЙ ==");
        int found = 0;
        L.add("ID | Требование | Результат | Статус");
        for (NeroRequirements.Req r : spec.reqs) {
            L.add(r.id + " | " + cut(r.text, 60) + " | " + (r.result.isEmpty() ? "-" : cut(r.result, 40)) + " | " + r.status);
            if (!r.status.startsWith("Не найдено")) found++;
        }
        L.add("Требований: " + spec.reqs.size() + ", найдено в плане: " + found + (found < spec.reqs.size() ? ". НЕ найдено: " + (spec.reqs.size() - found) + " (попроси добавить их в чате)" : ""));

        L.add("== ОГРАНИЧЕНИЯ ==");
        for (String n : plan.notes) L.add("• " + cut(n, 160));
        if (rep != null) for (String w : rep.warnings) L.add("• " + cut(w, 160));
        L.add("• Статусы «Проверено командами» означают успешную проверку синтаксиса и связей команд до постройки, а не испытание в игре.");

        L.add("== ВРЕМЯ ==");
        L.add(NeroPerf.summary());

        L.add("== ЧТО МОЖНО ИЗМЕНИТЬ ==");
        List<String> parts = NeroPatch.parts(spec);
        if (!parts.isEmpty()) L.add("Попроси в чате изменить компонент: " + String.join(", ", parts) + ". Меняется только он, остальное сохраняется.");
        else L.add("Попроси в чате добавить, убрать или изменить любую часть, либо измени размер и положение кнопками X/Y/Z.");
        return L;
    }

    private NeroReport() { }
}
