package com.acabra.ratelimiter.core;

import com.acabra.ratelimiter.config.RLConfig;
import com.acabra.ratelimiter.model.Hit;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class Limiter {

    private final RLConfig config;
    private final ConcurrentHashMap<String, LimitedEntry> table = new ConcurrentHashMap<>();
    private final LongAdder limited = new LongAdder();

    public Limiter(RLConfig config) {
        this.config = config;
    }

    public boolean shouldLimit(String apiKey, long now) {
        Hit hit = Hit.ofNow(now);
        LimitedEntry limitedEntry = table.compute(apiKey,
                (k, v) -> {
                    if (v == null) {
                        return new LimitedEntry(hit);
                    }
                    return v.accept(hit);
                });
        boolean shouldLimit = limitedEntry.shouldLimit();
        if(shouldLimit) {
            limited.increment();
            return true;
        }
        return false;
    }

    public long getLimited() {
        return limited.sum();
    }

    private class LimitedEntry {
        private final ArrayDeque<Hit> queue;

        public LimitedEntry(Hit hit) {
            this.queue = new ArrayDeque<>(){{
                add(hit);
            }};
        }

        public boolean shouldLimit() {
            return queue.size() > config.maxHits;
        }

        private void cleanExpiredEntries(long now) {
            while (!queue.isEmpty() && (now - queue.getLast().timestamp) > config.windowSizeMillis) {
                queue.removeLast();
            }
        }

        public LimitedEntry accept(Hit hit) {
            cleanExpiredEntries(hit.timestamp);
            queue.addFirst(hit);
            return this;
        }
    }
}
