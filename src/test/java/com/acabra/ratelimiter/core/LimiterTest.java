package com.acabra.ratelimiter.core;

import com.acabra.ratelimiter.config.RLConfig;
import com.acabra.ratelimiter.utils.KeyGenerator;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class LimiterTest {

    private final RLConfig config = new RLConfig(1000L, 10);
    private Limiter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new Limiter(config);
    }

    @Test
    public void shouldLimitAbout1900_given10Keys_equalProbability() {
        KeyGenerator keys = KeyGenerator.ofEqualProbability(10);

        ExecutorService ex = Executors.newFixedThreadPool(2);
        ScheduledExecutorService sched = Executors.newSingleThreadScheduledExecutor();

        sched.scheduleAtFixedRate(() ->
                        ex.submit(() -> underTest.shouldLimit(keys.next())),
                0, 5L, TimeUnit.MILLISECONDS);

        CompletableFuture<List<Runnable>> handle = CompletableFuture
                .supplyAsync(() -> {
                    ex.shutdownNow();
                    return sched.shutdownNow();
                }, CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS));

        handle.join();

        long actual = underTest.totalLimited();
        System.out.println("total limited actual: " + actual);
        Assertions.assertThat(actual).isCloseTo(1900L, Percentage.withPercentage(5));

        List<String> limitedKeys = underTest.getLimitedKeys();
        double expected = Double.valueOf(actual) / limitedKeys.size();
        System.out.println("total expected: " + expected);
        for (String key : limitedKeys) {
            long keyLimitCount = underTest.limitCount(key);
            System.out.printf("Key[%s] limited [%d] times \n", key, keyLimitCount);
            Assertions.assertThat(Double.valueOf(keyLimitCount))
                    .isCloseTo(expected, Percentage.withPercentage(20.0d));
        }
    }

    @Test
    public void shouldLimitAbout1900_given3Keys_fixProbability() {
        List<Integer> percentages = List.of(10, 45, 45);
        List<String> limitedKeys = IntStream.range(0, percentages.size())
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());
        KeyGenerator keys = KeyGenerator.ofFixProbability(limitedKeys, percentages);

        ExecutorService ex = Executors.newFixedThreadPool(2);
        ScheduledExecutorService sched = Executors.newSingleThreadScheduledExecutor();

        sched.scheduleAtFixedRate(() ->
                        ex.submit(() -> underTest.shouldLimit(keys.next())),
                0, 5L, TimeUnit.MILLISECONDS);

        CompletableFuture<List<Runnable>> handle = CompletableFuture
                .supplyAsync(() -> {
                    ex.shutdownNow();
                    return sched.shutdownNow();
                }, CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS));

        handle.join();

        double actual = underTest.totalLimited();
        Assertions.assertThat(actual).isCloseTo(1980.0, Percentage.withPercentage(5.0));

        limitedKeys.forEach(key -> System.out.printf("key: %s count: %d \n", key, underTest.limitCount(key)));
        for (int i=0; i<limitedKeys.size(); ++i) {
            String key = limitedKeys.get(i);
            long keyLimitCount = underTest.limitCount(key);
            double expected = actual * (percentages.get(i) / 100.0);

            Assertions.assertThat(Double.valueOf(keyLimitCount))
                    .isCloseTo(expected, Percentage.withPercentage(10.0d));
        }
    }
}