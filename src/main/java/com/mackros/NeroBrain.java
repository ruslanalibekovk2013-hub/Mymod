package com.mackros;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * «Мышление» NERO: сначала модель разбирает запрос и пишет план (виден в интерфейсе в реальном времени),
 * потом по этому плану проектируется чертёж. Здесь же живое состояние для индикатора процесса.
 */
public final class NeroBrain {
    private static final StringBuilder THOUGHT = new StringBuilder();
    public static volatile String stage = "";
    public static volatile long startedAt = 0;
    public static volatile int chars = 0;
    public static volatile String lastPlan = "";

    public static synchronized void reset(String s) {
        THOUGHT.setLength(0);
        chars = 0;
        startedAt = System.currentTimeMillis();
        stage = s;
    }

    public static void stage(String s) { stage = s; }

    public static synchronized void think(String d) {
        THOUGHT.append(d);
        chars += d.length();
        if (THOUGHT.length() > 8000) THOUGHT.delete(0, THOUGHT.length() - 6000);
    }

    public static synchronized String thought() { return THOUGHT.toString(); }

    public static void finish() { stage = ""; }

    public static boolean active() { return !stage.isEmpty(); }

    private static final String SYS = "You are the planning brain of NERO, an architect inside a Minecraft 1.20.4 (Java, Fabric, singleplayer, cheats) building mod. "
            + "The player writes in Russian or English. Think carefully about what the player REALLY wants, then write a concise DESIGN PLAN in Russian (max 350 words, plain text, numbered list, no JSON, no code blocks): "
            + "1) exactly what the player asked for, every detail (sizes, counts, colors, positions, behaviours); 2) the components/parts; 3) blocks and style; "
            + "4) how it works: which command-block logic or ready mechanisms; 5) rough dimensions and layout; 6) limits of Minecraft 1.20.4 and the honest closest working solution. "
            + "Never replace the request with a generic template, design exactly what was asked. If attached files are given, use their content.";

    /** Этап 1: план. Ошибка или отключённое «глубокое мышление» дают пустую строку, чертёж всё равно строится. */
    public static CompletableFuture<String> plan(String request, NeroSpec current, List<String> history) {
        if (!NeroConfig.deepThink) return CompletableFuture.completedFuture("");
        stage("Анализирую запрос");
        JsonArray msgs = new JsonArray();
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", SYS);
        msgs.add(sys);
        StringBuilder u = new StringBuilder(request);
        if (current != null) u.append("\n\nCurrent project: ").append(current.title()).append(" (").append(current.summary()).append(")");
        if (history != null && history.size() > 1) {
            u.append("\nEarlier player requests: ");
            for (int i = Math.max(0, history.size() - 5); i < history.size() - 1; i++) u.append(history.get(i)).append(" | ");
        }
        u.append(NeroAttach.textBlock());
        JsonObject usr = new JsonObject();
        usr.addProperty("role", "user");
        JsonArray withImg = NeroAttach.userContent(u.toString());
        if (withImg != null) usr.add("content", withImg); else usr.addProperty("content", u.toString());
        msgs.add(usr);
        StringBuilder out = new StringBuilder();
        CompletableFuture<String> f = new CompletableFuture<>();
        try {
            NeroChatClient.stream(msgs, NeroAttach.hasImages(), d -> { synchronized (out) { out.append(d); } think(d); }, err -> {
                String r;
                synchronized (out) { r = out.toString().trim(); }
                lastPlan = r;
                f.complete(r);
            });
        } catch (Exception e) {
            f.complete("");
        }
        return f;
    }

    private NeroBrain() { }
}
