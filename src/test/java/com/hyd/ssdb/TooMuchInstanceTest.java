package com.hyd.ssdb;

import com.hyd.ssdb.conf.Server;

import java.util.Arrays;

public class TooMuchInstanceTest {

    public static void main(String[] args) {
        SsdbClient client;
        client = SsdbClient.fromSingleCluster(Arrays.asList(
            new Server("localhost", 8888),
            new Server("localhost", 8887),
            new Server("localhost", 8886)
        ));
        client = new SsdbClient("localhost", 8888);
        client = new SsdbClient("localhost", 8888);
        client = new SsdbClient("localhost", 8888);
        client = new SsdbClient("localhost", 8888);
        client = new SsdbClient("localhost", 8888);
        client = new SsdbClient("localhost", 8888);
        client = new SsdbClient("localhost", 8888);
        client = new SsdbClient("localhost", 8888);
        client = new SsdbClient("localhost", 8888);
        client = new SsdbClient("localhost", 8888);
        client = new SsdbClient("localhost", 8888);
        client = new SsdbClient("localhost", 8888);
        client = new SsdbClient("localhost", 8888);
        client = new SsdbClient("localhost", 8888);
    }
}
