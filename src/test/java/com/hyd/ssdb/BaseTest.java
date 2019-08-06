package com.hyd.ssdb;

import com.hyd.ssdb.conf.Cluster;
import java.util.ArrayList;
import org.junit.AfterClass;
import org.junit.BeforeClass;

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
        String passStr = System.getProperty("ps");

        if (hostStr.contains(",") && portStr.contains(",")) {
            String[] hostArr = hostStr.split(",");
            String[] portArr = portStr.split(",");
            String[] passArr = passStr.split(",");

            ArrayList<Cluster> clusters = new ArrayList<Cluster>();
            for (int i = 0; i < hostArr.length; i++) {
                String host = hostArr[i];
                int port = Integer.parseInt(portArr[i]);
                String pass = passArr[i];
                clusters.add((pass == null || pass.length() == 0) ?
                    Cluster.fromSingleServer(host, port) :
                    Cluster.fromSingleServer(host, port, passStr)
                );
            }

            ssdbClient = SsdbClient.fromClusters(clusters);
        } else {
            ssdbClient = new SsdbClient(hostStr, Integer.parseInt(portStr), passStr);
        }

    }

    @AfterClass
    public static void finish() {
        ssdbClient.close();
    }

}
