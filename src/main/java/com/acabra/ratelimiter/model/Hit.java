package com.acabra.ratelimiter.model;

public class Hit implements Comparable<Hit> {
    public final long timestamp;

    public Hit(long timestamp) {
        this.timestamp = timestamp;
    }

    public static Hit ofNow(long t) {
        return new Hit(t);
    }

    @Override
    public int compareTo(Hit o) {
        return Long.compare(this.timestamp, o.timestamp);
    }
}
