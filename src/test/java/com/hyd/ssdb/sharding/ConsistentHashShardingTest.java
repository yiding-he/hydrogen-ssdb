package com.hyd.ssdb.sharding;

import static org.junit.Assert.assertEquals;

import com.hyd.ssdb.conf.Cluster;
import com.hyd.ssdb.conf.Server;
import org.junit.Test;

public class ConsistentHashShardingTest {

  @Test
  public void init() throws Exception {
    Cluster cluster1 = new Cluster(new Server("localhost", 8888), 100);
    ConsistentHashSharding sharding = new ConsistentHashSharding(cluster1);
    sharding.initClusters();

    assertEquals(Integer.MIN_VALUE, cluster1.getHashRange().getMin().intValue());
    assertEquals(Integer.MAX_VALUE, cluster1.getHashRange().getMax().intValue());

    Cluster cluster2 = new Cluster(new Server("localhost", 8889), 100);
    sharding.addCluster(cluster2, cluster1);

    assertEquals(-1, cluster1.getHashRange().getMax().intValue());
    assertEquals(0, cluster2.getHashRange().getMin().intValue());

    Cluster cluster3 = new Cluster(new Server("localhost", 8890), 100);
    sharding.addCluster(cluster3, cluster2);
    assertEquals(Integer.MAX_VALUE / 2 + 1, cluster3.getHashRange().getMin().intValue());

    assertEquals(cluster1, sharding.getClusterByKey("87"));
    assertEquals(cluster2, sharding.getClusterByKey("88"));
    assertEquals(cluster3, sharding.getClusterByKey("89"));

    sharding.clusterFailed(cluster3);
    assertEquals(cluster2, sharding.getClusterByKey("89"));

    sharding.clusterFailed(cluster2);
    assertEquals(cluster1, sharding.getClusterByKey("89"));

    cluster2.markValid(cluster2.getMaster());
    assertEquals(cluster2, sharding.getClusterByKey("89"));

    cluster3.markValid(cluster2.getMaster());
    assertEquals(cluster3, sharding.getClusterByKey("89"));
  }
}