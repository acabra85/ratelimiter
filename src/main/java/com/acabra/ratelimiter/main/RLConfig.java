package com.acabra.ratelimiter.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class RLConfig {
    public final long windowSizeMillis;
    public final int maxHits;
    public final List<String> allowedEntries;

    private RLConfig(long windowSizeMillis, int maxHits, List<String> allowedEntries) {
        this.windowSizeMillis = windowSizeMillis;
        this.maxHits = maxHits;
        this.allowedEntries = allowedEntries;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Long windowSizeMillis = null;
        private Integer maxHits = null;
        private final List<String> allowedEntries = new ArrayList<>();

        private Builder(){} //not for external instantiate

        private void validateConfig() {
            Objects.requireNonNull(this.windowSizeMillis);
            Objects.requireNonNull(this.maxHits);
        }

        public RLConfig build(){
            validateConfig();
            return new RLConfig(this.windowSizeMillis, this.maxHits, this.allowedEntries);
        }

        public Builder withWindowSizeMillis(long windowSizeMillis) {
            if(windowSizeMillis <=  0) {
                throw new IllegalArgumentException("Window size must be greater than zero given: " + windowSizeMillis);
            }
            this.windowSizeMillis = windowSizeMillis;
            return this;
        }

        public Builder withAllowedEntry(String allowedEntry) {
            if(allowedEntry != null) {
                this.allowedEntries.add(allowedEntry);
            }
            return this;
        }

        public Builder withAllowedEntries(Collection<String> allowedEntries) {
            if(allowedEntries != null && allowedEntries.size() > 0) {
                this.allowedEntries.addAll(allowedEntries);
            }
            return this;
        }

        public Builder withMaxHits(int maxHits) {
            if(maxHits <  0) {
                throw new IllegalArgumentException("Max hits must be non-negative given: " + maxHits);
            }
            this.maxHits = maxHits;
            return this;
        }
    }
}
