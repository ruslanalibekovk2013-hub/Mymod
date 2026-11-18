package com.mackros;

import java.util.List;

public final class NeroRequest {
    public final String text;
    public final NeroSpec current;
    public final String pending;
    public final List<String> history;

    public NeroRequest(String text, NeroSpec current, String pending, List<String> history) {
        this.text = text; this.current = current; this.pending = pending; this.history = history;
    }
}
