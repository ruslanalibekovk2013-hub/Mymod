package com.mackros;

/** Беспроводные каналы: передатчики, пульты и приёмники работают через них (по всему миру, даже между измерениями). */
public final class Chan {
    public static final int N = 16;
    private static final long[] UNTIL = new long[N];
    private static final boolean[] LATCH = new boolean[N];

    public static void pulse(int ch, long until) {
        ch = ((ch % N) + N) % N;
        if (until > UNTIL[ch]) UNTIL[ch] = until;
    }

    public static boolean toggle(int ch) {
        ch = ((ch % N) + N) % N;
        LATCH[ch] = !LATCH[ch];
        return LATCH[ch];
    }

    public static boolean latched(int ch) { return LATCH[((ch % N) + N) % N]; }

    public static boolean active(int ch, long now) {
        ch = ((ch % N) + N) % N;
        return LATCH[ch] || now <= UNTIL[ch];
    }

    private Chan() { }
}
