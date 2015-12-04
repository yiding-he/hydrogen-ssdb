package com.hyd.ssdb;

import com.hyd.ssdb.protocol.Request;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

/**
 * (description)
 * created at 15-12-3
 *
 * @author Yiding
 */
public class SsdbClientMultiThreadTest {

    private SsdbClient ssdbClient;

    @Before
    public void init() {
        this.ssdbClient = new SsdbClient("192.168.1.180", 8888);
    }

    @After
    public void finish() {
        this.ssdbClient.close();
    }


    @Test
    public void testMultiThread() throws Exception {
        for (int i = 0; i < 50; i++) {
            new Thread(new SsdbTask(ssdbClient)).start();
        }

        Thread.sleep(10000);
    }

    ////////////////////////////////////////////////////////////////

    private static class SsdbTask implements Runnable {

        private final SsdbClient ssdbClient;

        private Random random;

        public SsdbTask(SsdbClient ssdbClient) {
            this.ssdbClient = ssdbClient;
            random = new Random();
        }

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            long end = start + 4000;

            while (System.currentTimeMillis() < end) {
                ssdbClient.sendRequest(new Request(
                        "set key:" + random.nextInt(10000) + " value"));
            }

            System.out.println("Thread " + Thread.currentThread().getName() + " finished in " +
                    (System.currentTimeMillis() - start) + " milliseconds.");
        }
    }


}
