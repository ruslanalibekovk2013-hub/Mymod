package com.mackros;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Замеры времени этапов генерации (показываются в отчёте и по команде «скорость»). Числа реальные, измеряются при каждом запуске. */
public final class NeroPerf {
    private static final Map<String, Long> LAST = new LinkedHashMap<>();
    private static final Map<String, Long> BEST = new LinkedHashMap<>();

    public static synchronized void put(String stage, long ms) {
        LAST.put(stage, ms);
        BEST.merge(stage, ms, Math::min);
    }

    public static <T> T time(String stage, Supplier<T> s) {
        long t = System.nanoTime();
        try { return s.get(); } finally { put(stage, (System.nanoTime() - t) / 1_000_000L); }
    }

    public static synchronized String summary() {
        if (LAST.isEmpty()) return "Замеров пока нет.";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Long> e : LAST.entrySet()) sb.append(sb.length() > 0 ? "; " : "").append(e.getKey()).append(": ").append(e.getValue()).append(" мс");
        return sb.toString();
    }

    private NeroPerf() { }
}
