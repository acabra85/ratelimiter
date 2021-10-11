package com.acabra.ratelimiter.core;

import com.acabra.ratelimiter.main.RLConfig;
import com.acabra.ratelimiter.main.RateLimiter;
import com.acabra.ratelimiter.model.Hit;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class Limiter implements RateLimiter {

    private final RLConfig config;
    private final ConcurrentHashMap<String, LimitedEntry> table = new ConcurrentHashMap<>();
    private final LongAdder limited = new LongAdder();
    private final Set<String> allowList;

    public Limiter(RLConfig config) {
        this.config = config;
        this.allowList = Set.copyOf(config.allowedEntries);
    }

    @Override
    public boolean shouldLimit(String key, long now) {
        if(allowList.contains(key)) return false;
        Hit hit = Hit.ofNow(now);
        LimitedEntry limitedEntry = table.compute(key,
                (k, v) -> {
                    if (v == null) {
                        return new LimitedEntry(hit);
                    }
                    return v.accept(hit);
                });
        if(limitedEntry.shouldLimit()) {
            limited.increment();
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldLimit(String key) {
        return shouldLimit(key, System.currentTimeMillis());
    }

    @Override
    public long totalLimited() {
        return limited.sum();
    }

    @Override
    synchronized public List<String> keysUnderMonitoring() {
        Enumeration<String> keys = table.keys();
        return IntStream.range(0,table.size()).mapToObj(i -> keys.nextElement())
                .collect(Collectors.toList());
    }

    @Override
    public long limitCount(String key) {
        return table.get(key).getLimitCount();
    }

    private class LimitedEntry {
        private final ArrayDeque<Hit> queue;
        private final LongAdder limitCount;

        private LimitedEntry(Hit hit) {
            this.limitCount = new LongAdder();
            this.queue = new ArrayDeque<>(){{
                add(hit);
            }};
        }

        private long getLimitCount() {
            return limitCount.sum();
        }

        private boolean shouldLimit() {
            return queue.size() > config.maxHits;
        }

        private void cleanExpiredEntries(long now) {
            while (!queue.isEmpty() && (now - queue.getLast().timestamp) > config.windowSizeMillis) {
                queue.removeLast();
            }
        }

        private LimitedEntry accept(Hit hit) {
            cleanExpiredEntries(hit.timestamp);
            queue.addFirst(hit);
            if(shouldLimit()) {
                this.limitCount.increment();
            }
            return this;
        }
    }
}
