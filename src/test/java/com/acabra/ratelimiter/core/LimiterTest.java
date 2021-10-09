package com.acabra.ratelimiter.core;

import com.acabra.ratelimiter.config.RLConfig;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class LimiterTest {

    private final RLConfig config = new RLConfig(10000L, 10);
    private Limiter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new Limiter(config);
    }

    @Test
    public void should_limit() {
        int size = 10;
        Random r = new Random();
        List<String> collect = IntStream.range(0, size)
                .mapToObj(i -> UUID.randomUUID().toString()).collect(Collectors.toList());

        ScheduledExecutorService sched = Executors.newSingleThreadScheduledExecutor();
        ExecutorService ex = Executors.newFixedThreadPool(2);

        sched.scheduleAtFixedRate(() ->
                ex.submit(() -> underTest.shouldLimit(collect.get(r.nextInt(size)), System.currentTimeMillis())),
                10L, 5L, TimeUnit.MILLISECONDS);

        CompletableFuture<List<Runnable>> handle = CompletableFuture
                .supplyAsync(() -> {
                    ex.shutdownNow();
                    return sched.shutdownNow();
                }, CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS));

        handle.join();

        long actual = underTest.getLimited();
        System.out.printf("actual: " + actual);
        Assertions.assertThat(actual).isCloseTo(900L, Offset.offset(5L));
    }
}