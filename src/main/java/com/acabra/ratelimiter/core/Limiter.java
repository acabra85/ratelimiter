package com.acabra.ratelimiter.core;

import com.acabra.ratelimiter.config.RLConfig;
import com.acabra.ratelimiter.model.Hit;

import java.util.ArrayDeque;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Limiter {

    private final RLConfig config;
    private final ConcurrentHashMap<String, LimitedEntry> table = new ConcurrentHashMap<>();
    private final LongAdder limited = new LongAdder();

    public Limiter(RLConfig config) {
        this.config = config;
    }

    public boolean shouldLimit(String key, long now) {
        Hit hit = Hit.ofNow(now);
        LimitedEntry limitedEntry = table.compute(key,
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

    public boolean shouldLimit(String key) {
        return shouldLimit(key, System.currentTimeMillis());
    }

    public long totalLimited() {
        return limited.sum();
    }

    synchronized public List<String> getLimitedKeys() {
        Enumeration<String> keys = table.keys();
        return IntStream.range(0,table.size()).mapToObj(i -> keys.nextElement())
                .collect(Collectors.toList());
    }

    public long limitCount(String key) {
        return table.get(key).getLimitCount();
    }

    private class LimitedEntry {
        private final ArrayDeque<Hit> queue;
        private LongAdder limitCount = new LongAdder();

        public LimitedEntry(Hit hit) {
            this.queue = new ArrayDeque<>(){{
                add(hit);
            }};
        }

        public long getLimitCount() {
            return limitCount.sum();
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
            if(shouldLimit()) {
                this.limitCount.increment();
            }
            return this;
        }
    }
}
