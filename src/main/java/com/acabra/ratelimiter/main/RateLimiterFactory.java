package com.acabra.ratelimiter.main;

import com.acabra.ratelimiter.core.Limiter;

public class RateLimiterFactory {

    public static RateLimiter fromConfig(RLConfig config) {
        return new Limiter(config);
    }
}
