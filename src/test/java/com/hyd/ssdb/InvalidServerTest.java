package com.hyd.ssdb;

import com.hyd.ssdb.conf.Cluster;
import com.hyd.ssdb.conf.Server;
import com.hyd.ssdb.sharding.ConsistentHashSharding;

import java.util.Arrays;

/**
 * (description)
 * created at 15-12-7
 *
 * @author Yiding
 */
public class InvalidServerTest {

    public static void main(String[] args) throws Exception {

        // 创建一个主从集合的 Cluster
        SsdbClient ssdbClient = new SsdbClient(new ConsistentHashSharding(Cluster.fromServers(Arrays.asList(
                new Server("localhost", 18800),
                new Server("localhost", 18801),
                new Server("localhost", 18802)
        ))));

        // 当 Cluster 中不是全部服务器挂掉时，个别服务器的挂掉和恢复可以自动识别。
        ssdbClient.set("name", "hydrogen-ssdb");
        while (true) {
            System.out.println(ssdbClient.get("name"));
            Thread.sleep(500);
        }
    }
}
