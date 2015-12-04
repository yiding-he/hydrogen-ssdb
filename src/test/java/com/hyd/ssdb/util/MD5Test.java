package com.hyd.ssdb.util;

import java.util.Map;

/**
 * (description)
 * created at 15-12-4
 *
 * @author Yiding
 */
public class MD5Test {


    // 检查 md5Hash 结果的平均分布程度
    public static void main(String[] args) {
        long range = (long) Integer.MAX_VALUE - Integer.MIN_VALUE;
        int segmentCount = 100;
        int segmentSize = (int) (range / segmentCount);
        int[] splitters = new int[segmentCount + 1];

        for (int i = 0; i < segmentCount; i++) {
            splitters[i] = Integer.MIN_VALUE + i * segmentSize;
        }

        splitters[splitters.length - 1] = Integer.MAX_VALUE;
        IntervalCounter counter = new IntervalCounter(splitters);

        for (int i = 1000000; i < 1100000; i++) {
            int hash = MD5.md5Hash(String.valueOf(i));
            counter.add(hash);
        }

        for (Map.Entry<String, Integer> entry : counter.getCountResult().entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
    }
}