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

    public static boolean busy() { return busy; }

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
        final NeroRequest req = new NeroRequest(text, project, pending, NeroHistory.requests());
        final AIProvider main = NeroConfig.remoteReady() ? new HttpProvider() : LOCAL;
        busy = true;
        MinecraftClient mc = MinecraftClient.getInstance();
        main.process(req).whenComplete((resp, ex) -> mc.execute(() -> {
            NeroResponse r = resp;
            if (ex != null || r == null || !r.ok) {
                String why = ex != null ? ex.getClass().getSimpleName() : (r == null ? "нет ответа" : r.error);
                if (main != LOCAL) {
                    NeroHistory.log("Внешний ИИ недоступен (" + why + "). Перехожу на локальный режим.");
                    r = ((LocalProvider) LOCAL).process(req).join();
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
            project = r.spec;
        }
        pending = r.pending;
        if (!r.reply.isEmpty()) NeroHistory.log("NERO (" + r.provider + "): " + r.reply);
    }

    public static List<String> history() { return NeroHistory.requests(); }

    private NeroAI() { }
}
