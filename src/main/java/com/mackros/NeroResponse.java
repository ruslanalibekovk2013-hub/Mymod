package com.mackros;

public final class NeroResponse {
    public final boolean ok;
    public final NeroSpec spec;
    public final String reply;
    public final String pending;
    public final String error;
    public final String provider;

    public NeroResponse(boolean ok, NeroSpec spec, String reply, String pending, String error, String provider) {
        this.ok = ok; this.spec = spec; this.reply = reply; this.pending = pending; this.error = error; this.provider = provider;
    }

    public static NeroResponse fail(String err, String provider) { return new NeroResponse(false, null, "", null, err, provider); }
}
