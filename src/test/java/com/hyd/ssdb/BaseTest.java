package com.hyd.ssdb;

import org.junit.After;
import org.junit.Before;

/**
 * (description)
 * created at 15-12-7
 *
 * @author Yiding
 */
public class BaseTest {

    protected SsdbClient ssdbClient;

    @Before
    public void init() {
        this.ssdbClient = new SsdbClient("192.168.1.180", 8888);
    }

    @After
    public void finish() {
        this.ssdbClient.close();
    }

}
