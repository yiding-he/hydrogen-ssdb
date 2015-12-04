package com.hyd.ssdb.conf;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

/**
 * (description)
 * created at 15-12-3
 *
 * @author Yiding
 */
public class ShardingTest {

    @Test
    public void testSetupRing() throws Exception {
        ArrayList<Cluster> clusters = new ArrayList<Cluster>();
        Cluster cluster1 = new Cluster(Collections.singletonList(new Server("host", 1234)));
        cluster1.setWeight(400);
        clusters.add(cluster1);
        Cluster cluster2 = new Cluster(Collections.singletonList(new Server("host", 1234)));
        cluster2.setWeight(300);
        clusters.add(cluster2);
        Cluster cluster3 = new Cluster(Collections.singletonList(new Server("host", 1234)));
        cluster3.setWeight(200);
        clusters.add(cluster3);
        Cluster cluster4 = new Cluster(Collections.singletonList(new Server("host", 1234)));
        cluster4.setWeight(100);
        clusters.add(cluster4);

        Sharding sharding = new Sharding(clusters);

        outputCluster(cluster1);
        outputCluster(cluster2);
        outputCluster(cluster3);
        outputCluster(cluster4);

        System.out.println();

        chooseCluster(sharding, "key1");
        chooseCluster(sharding, "key2");
        chooseCluster(sharding, "key3");
        chooseCluster(sharding, "key4");
        chooseCluster(sharding, "key5");
        chooseCluster(sharding, "key6");
    }

    private void outputCluster(Cluster cluster) {
        System.out.println("cluster1: " + cluster.getMinHash() + " ~ " + cluster.getMaxHash());
    }

    private void chooseCluster(Sharding sharding, String key) {
        Cluster c = sharding.getCluster(key);
        outputCluster(c);
    }

    @Test
    public void testSingleServer() throws Exception {
        Sharding sharding = Sharding.fromSingleServer("host", 1234);
        Cluster cluster = sharding.getCluster("key");
        outputCluster(cluster);
    }
}