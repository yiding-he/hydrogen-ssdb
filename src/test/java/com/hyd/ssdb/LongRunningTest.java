package com.hyd.ssdb;

import org.junit.Test;

public class LongRunningTest extends BaseTest {

    @Test
    public void testLongRun() {
        while (true) {
            ssdbClient.set("name", "value");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
