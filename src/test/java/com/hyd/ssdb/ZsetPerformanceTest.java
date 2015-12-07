package com.hyd.ssdb;

import org.junit.Test;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * (description)
 * created at 15-12-7
 *
 * @author Yiding
 */
public class ZsetPerformanceTest extends BaseTest {

    @Test
    public void testZrankPerformance() throws Exception {

    }

    private void initData() {
        Random random = new Random();
        AtomicInteger counter = new AtomicInteger(0);

        ssdbClient.zclear("total_score");
        for (int i = 0; i < 20000; i++) {
            ssdbClient.zset("total_score", "student" + counter.incrementAndGet(), random.nextInt(500) + 100);
        }

        System.out.println("Initialization finished.");
    }
}
