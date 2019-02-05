package com.hyd.ssdb;

import com.hyd.ssdb.conf.Cluster;
import com.hyd.ssdb.conf.Server;
import com.hyd.ssdb.conf.Sharding;
import com.hyd.ssdb.sharding.ConsistentHashSharding;

import java.util.Arrays;

/**
 * (description)
 * created at 15-12-4
 *
 * @author Yiding
 */
public class MultiClustersTest {

    public static void main(String[] args) {
        Sharding sharding = new ConsistentHashSharding(Arrays.asList(
                Cluster.fromServers(
                        new Server("localhost", 8011, true),
                        new Server("localhost", 8012, false),
                        new Server("localhost", 8013, false)
                ),
                Cluster.fromSingleServer("localhost", 8021),
                Cluster.fromServers(
                        new Server("localhost", 8031, true),
                        new Server("localhost", 8032, false),
                        new Server("localhost", 8033, false)
                )
        ));

        SsdbClient ssdbClient = new SsdbClient(sharding);
        for (int i = 100; i < 2000; i++) {
            String key = "key" + i;
            String value = "value" + i;
            ssdbClient.set(key, value);
            assert ssdbClient.get(key).equals(value);
        }

        ssdbClient.close();
    }
}
