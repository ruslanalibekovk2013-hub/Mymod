package com.mackros;

import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.function.Consumer;

/** Фасад NERO AI: хранит контекст проекта, выбирает провайдера, при сбое внешнего ИИ переходит на локальный режим. */
public final class NeroAI {
    public static NeroSpec project = null;
    public static String pending = null;
    private static final AIProvider LOCAL = new LocalProvider();
    private static boolean busy = false;
    private static int gen = 0;
    private static final java.util.ArrayDeque<NeroSpec> UNDO = new java.util.ArrayDeque<>(), REDO = new java.util.ArrayDeque<>();
    public static String attachment = "";
    /** Каталог: готовые генераторы строятся локально и всегда одинаково, даже если подключена модель. */
    public static boolean forceLocal = false;
    /** Сколько символов ответа модели уже получено (для индикатора процесса). */
    public static volatile int progress = 0;

    /** Запоминает состояние проекта перед изменением (для «отмени» / «повтор»). */
    public static void pushUndo(NeroSpec before) {
        if (before == null) return;
        UNDO.push(before.copy());
        while (UNDO.size() > 30) UNDO.removeLast();
        REDO.clear();
    }
    private static String lastText = "";

    public static boolean busy() { return busy; }

    /** Открывает проект из библиотеки как текущий (без повторной генерации). */
    public static void setProject(NeroSpec s) {
        project = s;
        pending = null;
        gen++;
        busy = false;
    }

    /** Отмена текущего запроса к модели (сетевой запрос прерывается). */
    public static void cancel() {
        if (!busy) return;
        gen++;
        busy = false;
        HttpProvider.cancelCurrent();
        NeroChatClient.cancel();
        NeroBrain.finish();
        NeroHistory.log("Запрос отменён");
    }

    private static void reply(String msg, Consumer<NeroResponse> done) {
        NeroResponse r = new NeroResponse(true, null, msg, null, null, "NERO");
        handle(r);
        done.accept(r);
    }

    /** Отмена (true) или повтор (false) последнего изменения проекта. */
    public static String doUndo(boolean undo) {
        java.util.ArrayDeque<NeroSpec> from = undo ? UNDO : REDO, to = undo ? REDO : UNDO;
        if (from.isEmpty() || project == null) return undo ? "Отменять нечего." : "Повторять нечего.";
        to.push(project.copy());
        NeroSpec s = from.pop();
        NeroScreen.loadProject(s);
        NeroProjects.save(s, undo ? "Отмена изменения" : "Повтор изменения");
        return (undo ? "Отменено" : "Повторено") + ": проект «" + s.title() + "» (в стеке: отмен " + UNDO.size() + ", повторов " + REDO.size() + ").";
    }

    public static void clear() {
        project = null;
        pending = null;
        NeroHistory.log("Проект очищен");
    }

    public static void ask(String text, Consumer<NeroResponse> done) {
        if (busy) { NeroHistory.log("Подожди: предыдущий запрос ещё обрабатывается"); return; }
        text = text.trim();
        if (text.isEmpty()) return;
        NeroHistory.addRequest(text);
        NeroHistory.log("Вы: " + text);
        String low = text.toLowerCase(java.util.Locale.ROOT);
        MinecraftClient mc0 = MinecraftClient.getInstance();
        if (low.matches("^(модель|model)(\\s.*)?$")) {
            String arg = text.replaceFirst("(?i)^(модель|model)\\s*", "").trim();
            if (arg.isEmpty()) { reply(NeroConfig.modelInfo(), done); return; }
            NeroConfig.model = arg.equalsIgnoreCase("auto") ? "auto" : arg;
            NeroConfig.lockedModel = "";
            NeroConfig.modelName = arg.equalsIgnoreCase("auto") ? "" : arg;
            NeroConfig.status("не проверялось");
            reply(arg.equalsIgnoreCase("auto") ? "Закрепление модели снято: при следующем запросе будет выбрана и закреплена подходящая бесплатная модель." : "Модель задана вручную: " + arg + ". Она будет использоваться без автоматической замены.", done);
            return;
        }
        if (low.matches("^(резерв|fallback)\\s+(вкл|выкл|on|off)$")) {
            NeroConfig.fallbackLocal = low.endsWith("вкл") || low.endsWith("on");
            NeroConfig.save();
            reply("Резервный локальный режим при недоступной модели: " + (NeroConfig.fallbackLocal ? "включён" : "выключен") + ".", done);
            return;
        }
        if (low.matches("^(прикрепи|attach)\\s+.+")) {
            attachment = text.replaceFirst("(?i)^(прикрепи|attach)\\s+", "").trim();
            reply("Данные прикреплены и будут добавлены к следующим запросам: «" + (attachment.length() > 80 ? attachment.substring(0, 79) + "…" : attachment) + "». Команда «открепи» убирает их.", done);
            return;
        }
        if (low.matches("^(открепи|detach)$")) { attachment = ""; reply("Прикреплённые данные убраны.", done); return; }
        if (low.matches("^(отмени|undo)$") || low.matches("^(повтор|redo)$")) { reply(doUndo(low.matches("^(отмени|undo)$")), done); return; }
        if (low.matches("^(редактор|editor|3d)$")) { MinecraftClient.getInstance().setScreen(new NeroEditorScreen()); return; }
        if (low.matches("^(каталог|меню|catalog|menu)$")) { MinecraftClient.getInstance().setScreen(new NeroCatalogScreen()); return; }
        if (low.matches("^(сдвинь|move)\\s+.+\\s+-?\\d+\\s+-?\\d+\\s+-?\\d+$")) {
            java.util.regex.Matcher mm = java.util.regex.Pattern.compile("^(?:сдвинь|move)\\s+(.+?)\\s+(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)$", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
            if (mm.find()) { reply(NeroEditor.movePart(mm.group(1), Integer.parseInt(mm.group(2)), Integer.parseInt(mm.group(3)), Integer.parseInt(mm.group(4))), done); return; }
        }
        if (low.matches("^(удали часть|remove part)\\s+.+")) { reply(NeroEditor.removePart(text.replaceFirst("(?i)^(удали часть|remove part)\\s+", "").trim()), done); return; }
        if (low.matches("^(сохрани часть)\\s+.+\\s+как\\s+.+")) {
            java.util.regex.Matcher mm = java.util.regex.Pattern.compile("^сохрани часть\\s+(.+?)\\s+как\\s+(.+)$", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
            if (mm.find()) { reply(NeroModels.savePart(project, mm.group(1).trim(), mm.group(2).trim()), done); return; }
        }
        if (low.matches("^вставь модель\\s+.+\\s+-?\\d+\\s+-?\\d+\\s+-?\\d+$")) {
            java.util.regex.Matcher mm = java.util.regex.Pattern.compile("^вставь модель\\s+(.+?)\\s+(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)$", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text);
            if (mm.find()) { reply(NeroEditor.insertModel(mm.group(1).trim(), Integer.parseInt(mm.group(2)), Integer.parseInt(mm.group(3)), Integer.parseInt(mm.group(4))), done); return; }
        }
        if (low.matches("^библиотека моделей$")) { reply(NeroModels.describe(), done); return; }
        if (low.matches("^(самотест|selftest)$")) {
            java.util.List<String> st = NeroSelfTest.run();
            for (String l : st) NeroHistory.log(l);
            reply(st.get(st.size() - 1) + ". Подробный отчёт: config/mackros/selftest.txt", done);
            return;
        }
        if (low.matches("^(скорость|speed)$")) { reply("Замеры этапов: " + NeroPerf.summary(), done); return; }
        if (low.matches("^(отмена|cancel)$")) { cancel(); return; }
        if (low.matches("^(библиотека|library)(\\s.*)?$")) {
            String q = text.replaceFirst("(?i)^(библиотека|library)\\s*", "");
            NeroResponse lr = new NeroResponse(true, null, NeroLibrary.describe(q), null, null, "Библиотека");
            handle(lr);
            done.accept(lr);
            return;
        }
        if (low.matches("^(исследуй|изучи|research)\\s+.+")) {
            String topic = text.replaceFirst("(?i)^(исследуй|изучи|research)\\s+", "");
            if (!NeroConfig.remoteReady()) {
                NeroResponse nr = new NeroResponse(true, null, "Исследование мира идёт через внешний ИИ с веб-поиском: вставь ключ OpenRouter в «Настройки AI». Пока работает встроенная библиотека (команда «библиотека»).", null, null, "Библиотека");
                handle(nr);
                done.accept(nr);
                return;
            }
            busy = true;
            NeroHistory.log("Исследую мир по теме «" + topic + "» на разных языках, это может занять до пары минут");
            NeroResearch.run(topic).whenComplete((resp, ex) -> mc0.execute(() -> {
                NeroResponse rr = (ex != null || resp == null) ? NeroResponse.fail(ex != null ? ex.getClass().getSimpleName() : "нет ответа", "Исследование") : resp;
                busy = false;
                handle(rr);
                done.accept(rr);
            }));
            return;
        }
        if (!attachment.isEmpty()) text = text + ". Дополнительные данные проекта: " + attachment;
        final NeroRequest req = new NeroRequest(text, project, pending, NeroHistory.requests());
        final AIProvider main = (!forceLocal && NeroConfig.remoteReady()) ? new HttpProvider() : LOCAL;
        forceLocal = false;
        busy = true;
        lastText = text;
        final int myGen = ++gen;
        final long t0 = System.nanoTime();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (main != LOCAL) { NeroBrain.reset("Отправляю запрос модели"); NeroHistory.log("Запрос отправлен модели. Можно остановить кнопкой «Стоп»."); }
        else if (NeroAttach.any()) NeroHistory.log("Файлы видит только подключённая модель: вставь токен OpenRouter. Локальный режим их не читает.");
        main.process(req).whenComplete((resp, ex) -> mc.execute(() -> {
            NeroBrain.finish();
            if (myGen != gen) return;
            NeroPerf.put(main == LOCAL ? "Локальный разбор запроса" : "Запрос к ИИ (всего)", (System.nanoTime() - t0) / 1_000_000L);
            NeroResponse r = resp;
            if (ex != null || r == null || !r.ok) {
                String why = ex != null ? ex.getClass().getSimpleName() : (r == null ? "нет ответа" : r.error);
                if (main != LOCAL) {
                    if (NeroConfig.fallbackLocal) {
                        NeroHistory.log("Модель недоступна (" + why + "). Резервный локальный режим включён: использую его.");
                        r = ((LocalProvider) LOCAL).process(req).join();
                    } else {
                        NeroHistory.log("Модель недоступна: " + why);
                        NeroHistory.log("Другая модель не подставляется без твоего разрешения. Повтори запрос, проверь «Настройки AI → Проверить», напиши «модель auto» или «резерв вкл».");
                        r = NeroResponse.fail(why, "Внешний ИИ");
                    }
                } else r = NeroResponse.fail(why, "Локальный");
            }
            busy = false;
            handle(r);
            done.accept(r);
        }));
    }

    private static void handle(NeroResponse r) {
        if (!r.ok) { NeroHistory.log("Ошибка: " + r.error); return; }
        if (r.spec != null) {
            if (r.spec.seed == 0) r.spec.seed = (project != null && project.kind.equals(r.spec.kind) && project.seed != 0) ? project.seed : (new java.util.Random().nextLong() | 1L);
            boolean fresh = LocalProvider.explicitNew(LocalProvider.norm(lastText));
            NeroSpec old = project;
            if (old != null) pushUndo(old);
            project = r.spec;
            if (old != null && !fresh && project.projectId.isEmpty()) project.projectId = old.projectId;
            if (project.desc == null || project.desc.isEmpty()) project.desc = (old != null && !fresh && old.desc != null && !old.desc.isEmpty()) ? old.desc : lastText;
            long te = System.nanoTime();
            if (project.reqs.isEmpty()) {
                if (old != null && !fresh) project.reqs = new java.util.ArrayList<>(old.reqs);
                java.util.List<NeroRequirements.Req> add = NeroRequirements.extract(lastText);
                int n = project.reqs.size();
                for (NeroRequirements.Req q : add) {
                    q.id = "R" + (++n);
                    if (old != null && !fresh && q.category.equals("основной объект")) q.category = NeroRequirements.classify(q.text);
                    project.reqs.add(q);
                }
            }
            NeroPerf.put("Извлечение требований", (System.nanoTime() - te) / 1_000_000L);
            NeroProjects.save(project, "Запрос: " + (lastText.length() > 60 ? lastText.substring(0, 59) + "…" : lastText));
        }
        pending = r.pending;
        if (!r.reply.isEmpty()) NeroHistory.log("NERO (" + r.provider + "): " + r.reply);
    }

    public static List<String> history() { return NeroHistory.requests(); }

    private NeroAI() { }
}
