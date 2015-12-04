package com.hyd.ssdb.largedata;

import com.hyd.ssdb.SsdbClient;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * (description)
 * created at 15-12-4
 *
 * @author Yiding
 */
public class LargeNumberKeysTest {

    public static final int POOL_SIZE = 8;

    public static void main(String[] args) {
        SsdbClient client = new SsdbClient("192.168.1.180", 8888, null, 2000, POOL_SIZE);
        String value = "123456789012345678901234567890123456789012345678901234567890" +
                "1234567890123456789012345678901234567890123456789012345678901234567890" +
                "1234567890123456789012345678901234567890123456789012345678901234567890" +
                "1234567890123456789012345678901234567890123456789012345678901234567890" +
                "1234567890123456789012345678901234567890123456789012345678901234567890" +
                "1234567890123456789012345678901234567890123456789012345678901234567890" +
                "1234567890123456789012345678901234567890123456789012345678901234567890" +
                "1234567890123456789012345678901234567890123456789012345678901234567890" +
                "1234567890123456789012345678901234567890123456789012345678901234567890" +
                "1234567890123456789012345678901234567890123456789012345678901234567890" +
                "1234567890123456789012345678901234567890123456789012345678901234567890" +
                "1234567890123456789012345678901234567890123456789012345678901234567890" +
                "1234567890123456789012345678901234567890123456789012345678901234567890" +
                "1234567890123456789012345678901234567890123456789012345678901234567890" +
                "";

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++) {
            String key = "key:" + i;
            setKeyValue(client, key, value);

            if (i % 10000 == 0) {
                int ps = (int) (i * 1000.0 / ((System.currentTimeMillis() - (double) start)));
                System.out.println(i + " keys completed (" + ps + "/s)");
            }
        }
    }

    private static ThreadPoolExecutor executor;

    static {
        executor = new ThreadPoolExecutor(
                POOL_SIZE, POOL_SIZE, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(100)
        );
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private static void setKeyValue(final SsdbClient client, final String key, final String value) {
        executor.submit(new Runnable() {

            @Override
            public void run() {
                client.set(key, value);
            }
        });
    }
}
