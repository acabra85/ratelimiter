package com.acabra.ratelimiter.config;

public class RLConfig {
    public final long windowSizeMillis;
    public final int maxHits;

    public RLConfig(long windowSizeMillis, int maxHits) {
        this.windowSizeMillis = windowSizeMillis;
        this.maxHits = maxHits;
    }
}
