package com.hyd.ssdb.multithread;

import com.hyd.ssdb.BaseTest;
import org.junit.Test;

/**
 * (description)
 * created at 16/04/04
 *
 * @author yiding_he
 */
public class MultiThreadTest extends BaseTest {

    public static final String KEY = "KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKEY";

    public static final String VALUE = "VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVValue";

    @Test
    public void testMultithread() throws Exception {
        for (int i = 0; i < 5; i++) {
            new Thread() {

                @Override
                public void run() {
                    for (int j = 0; j < 10000; j++) {
                        ssdbClient.set(KEY, VALUE);
                    }
                }
            }.start();
        }

        Thread.sleep(10000);
    }
}
