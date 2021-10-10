package com.acabra.ratelimiter.utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class KeyGenerator {
    private final Random rand;
    private final List<String> keys;
    private final int size;

    private KeyGenerator(List<String> keys) {
        this.keys = keys;
        this.rand = new Random();
        this.size = keys.size();
    }

    public static KeyGenerator ofEqualProbability(int size) {
        List<String> keys = IntStream.range(0, size)
                .mapToObj(i-> UUID.randomUUID().toString())
                .collect(Collectors.toList());
        return new KeyGenerator(keys);
    }

    public static KeyGenerator ofFixProbability(List<String> keys, List<Integer> percentages) {
        Integer sum = percentages.stream().reduce(Integer::sum).orElse(0);
        if(sum != 100) {
            throw new IllegalArgumentException("Total must sum 100 but was: " + sum);
        }
        int size = percentages.size();
        List<String> destination = new ArrayList<>(100);
        int i = 0;
        for (int k = 0; k < keys.size(); ++k) {
            for (int j = 0; j < percentages.get(k); ++j, ++i) {
                destination.add(keys.get(k));
            }
        }
        return new KeyGenerator(shuffle(destination));
    }

    private static List<String> shuffle(List<String> keys) {
        List<String> shuffled = new ArrayList<>(keys.size());
        Set<Integer> set = IntStream.range(0, keys.size())
                .boxed().collect(Collectors.toSet());
        Random r = new Random();
        for (int i = 0; i < keys.size(); i++) {
            int idx = -1;
            while (!set.contains(idx)) {
                idx = r.nextInt(keys.size());
            }
            set.remove(idx);
            shuffled.add(keys.get(idx));
        }
        return shuffled;
    }

    public String next() {
        return keys.get(rand.nextInt(size));
    }
}
