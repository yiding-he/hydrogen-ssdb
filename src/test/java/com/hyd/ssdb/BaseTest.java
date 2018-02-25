package com.hyd.ssdb;

import com.hyd.ssdb.conf.Cluster;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.ArrayList;

/**
 * (description)
 * created at 15-12-7
 *
 * @author Yiding
 */
public class BaseTest {

    protected static SsdbClient ssdbClient;

    @BeforeClass
    public static void init() {
        String hostStr = System.getProperty("h", "127.0.0.1");
        String portStr = System.getProperty("p", "8881");

        if (hostStr.contains(",") && portStr.contains(",")) {
            String[] hostArr = hostStr.split(",");
            String[] portArr = portStr.split(",");

            ArrayList<Cluster> clusters = new ArrayList<Cluster>();
            for (int i = 0; i < hostArr.length; i++) {
                String host = hostArr[i];
                int port = Integer.parseInt(portArr[i]);
                clusters.add(Cluster.fromSingleServer(host, port));
            }

            ssdbClient = SsdbClient.fromClusters(clusters);
        } else {
            ssdbClient = new SsdbClient(hostStr, Integer.parseInt(portStr));
        }

    }

    @AfterClass
    public static void finish() {
        ssdbClient.close();
    }

}
