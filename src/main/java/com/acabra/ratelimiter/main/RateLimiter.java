package com.acabra.ratelimiter.main;

import java.util.List;

public interface RateLimiter {

    /**
     * Determines if the given key in the given instant should be limited
     * @param key the client key
     * @param timestamp time
     * @return true if the key has exceeded the maximum amount of calls within the given window
     */
    boolean shouldLimit(String key, long timestamp);


    /**
     * Determines if the given key should be limited at the current moment taken by #System.currentTimeInMillis().
     * @param key the client key
     * @return true if the key has exceeded the maximum amount of calls within the given window
     */
    boolean shouldLimit(String key);

    /**
     * A counter of all denied calls
     * @return total amount or limited entries
     */
    long totalLimited();

    /**
     * @return A copy of the keys currently being monitored by the rate limiter
     */
    List<String> keysUnderMonitoring();

    /**
     * The total amount of calls limited for the given key
     * @param key client key
     * @return the amount of times the key has exceeded the max amount of calls within the given window
     */
    long limitCount(String key);
}
