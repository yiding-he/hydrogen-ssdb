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
        String host = System.getProperty("h", "127.0.0.1");
        int port = Integer.parseInt(System.getProperty("p", "8881"));
        this.ssdbClient = new SsdbClient(host, port);
    }

    @After
    public void finish() {
        this.ssdbClient.close();
    }

}
