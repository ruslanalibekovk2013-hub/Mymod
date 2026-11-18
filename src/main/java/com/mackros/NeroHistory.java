package com.mackros;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** История запросов игрока (последние 30) и журнал действий NERO (в памяти). */
public final class NeroHistory {
    private static final List<String> REQ = new ArrayList<>();
    private static final List<String> LOG = new ArrayList<>();

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("mackros_nero_history.txt"); }

    public static void load() {
        try {
            if (!Files.exists(file())) return;
            for (String l : Files.readAllLines(file(), StandardCharsets.UTF_8)) if (!l.isBlank()) REQ.add(l.replace("\\n", "\n"));
            while (REQ.size() > 30) REQ.remove(0);
        } catch (Exception ignored) { }
    }

    public static synchronized void addRequest(String t) {
        t = t.trim();
        if (t.isEmpty()) return;
        REQ.remove(t);
        REQ.add(t);
        while (REQ.size() > 30) REQ.remove(0);
        try {
            List<String> out = new ArrayList<>();
            for (String r : REQ) out.add(r.replace("\n", "\\n"));
            Files.write(file(), out, StandardCharsets.UTF_8);
        } catch (Exception ignored) { }
    }

    public static synchronized List<String> requests() { return new ArrayList<>(REQ); }

    public static synchronized void log(String s) {
        LOG.add(s);
        while (LOG.size() > 200) LOG.remove(0);
    }

    public static synchronized List<String> log() { return new ArrayList<>(LOG); }

    public static synchronized void clearLog() { LOG.clear(); }

    private NeroHistory() { }
}
