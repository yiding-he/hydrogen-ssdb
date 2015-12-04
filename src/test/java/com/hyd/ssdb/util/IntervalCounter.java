package com.hyd.ssdb.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 分范围的计数器
 *
 * @author yiding.he
 */
public class IntervalCounter {

    private final ThreadLocal<Long> startTime = new ThreadLocal<Long>();

    private int[] splitters;

    private int[] counters;

    public IntervalCounter(int... splitters) {
        this.splitters = splitters;
        this.counters = new int[this.splitters.length - 1];
    }

    public void add(long value) {
        int index = 0;
        while (index < splitters.length && value > this.splitters[index]) {
            index++;
        }
        if (index > 0) {
            this.counters[index - 1] += 1;
        }
    }

    public int[] getSplitters() {
        return splitters;
    }

    public int[] getCounters() {
        return counters;
    }

    public Map<String, Integer> getCountResult() {
        Map<String, Integer> counters = new LinkedHashMap<String, Integer>();
        int[] splitters = getSplitters();
        int[] counts = getCounters();

        for (int i = 0; i < counts.length; i++) {
            int start = splitters[i];
            int end = splitters[i + 1];
            int count = counts[i];
            counters.put(start + ":" + end, count);
        }

        return counters;
    }

    public void start() {
        startTime.set(System.currentTimeMillis());
    }

    public void finish() {
        Long start = startTime.get();
        if (start == null) {
            return;
        }

        long end = System.currentTimeMillis();
        this.add(end - start);
    }

    ///////////////////////////////////////////////////////////////


    @Override
    public String toString() {
        return getCountResult().toString();
    }
}
