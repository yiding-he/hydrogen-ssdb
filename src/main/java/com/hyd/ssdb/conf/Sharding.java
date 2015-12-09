package com.hyd.ssdb.conf;

import com.hyd.ssdb.SsdbClientException;
import com.hyd.ssdb.SsdbNoClusterAvailableException;
import com.hyd.ssdb.sharding.ConsistentHashingShardingStrategy;
import com.hyd.ssdb.sharding.ShardingStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 多台服务器的拓扑结构配置
 * created at 15-12-3
 *
 * @author Yiding
 */
public class Sharding {

    private List<Cluster> clusters;

    private ShardingStrategy shardingStrategy;

    /**
     * 构造方法
     *
     * @param cluster 集群配置，整个负载均衡当中只会有一个集群
     */
    public Sharding(Cluster cluster) {
        this(Collections.singletonList(cluster));
    }

    /**
     * 构造方法
     *
     * @param clusters 集群配置
     */
    public Sharding(List<Cluster> clusters) {

        clusters.removeAll(Collections.singleton((Cluster) null));
        if (clusters.isEmpty()) {
            throw new SsdbClientException("clusters is empty");
        }

        this.clusters = new ArrayList<Cluster>(clusters);
        this.shardingStrategy = new ConsistentHashingShardingStrategy();
        this.shardingStrategy.setClusters(this.clusters);
    }

    // 从单个 Server 创建一个 Cluster
    public static Sharding fromSingleServer(Server server) {
        return new Sharding(Collections.singletonList(
                new Cluster(Collections.singletonList(server))));
    }

    //////////////////////////////////////////////////////////////

    /**
     * 快速创建仅包含一个服务器的 Sharding 配置
     *
     * @param host 服务器地址
     * @param port 服务器端口
     *
     * @return 仅包含一个服务器的 Sharding 配置
     */
    public static Sharding fromSingleServer(String host, int port) {
        return fromSingleServer(new Server(host, port));
    }

    public static Sharding fromSingleServer(String host, int port, String pass) {
        return fromSingleServer(new Server(host, port, pass));
    }

    public static Sharding fromSingleServer(String host, int port, String pass, int soTimeout, int poolMaxTotal) {
        return fromSingleServer(new Server(host, port, pass, true, soTimeout, poolMaxTotal));
    }

    /**
     * 快速创建主从架构的 Sharding 配置
     *
     * @param servers 包含主从服务器的配置
     *
     * @return 主从架构的 Sharding 配置
     */
    public static Sharding fromServerList(List<Server> servers) {
        return new Sharding(Collections.singletonList(new Cluster(servers)));
    }

    ////////////////////////////////////////////////////////////////

    public List<Cluster> getClusters() {
        return clusters;
    }

    public ShardingStrategy getShardingStrategy() {
        return shardingStrategy;
    }

    public void setShardingStrategy(ShardingStrategy shardingStrategy) {
        this.shardingStrategy = shardingStrategy;
        this.shardingStrategy.initClusters();
    }

    public Cluster getClusterById(String id) {
        for (Cluster cluster : clusters) {
            if (cluster.getId().equals(id)) {
                return cluster;
            }
        }

        return null;
    }

    /**
     * 通过给定的 key 判断其所属的集群
     *
     * @param key key
     *
     * @return key 所属的集群
     */
    public Cluster getClusterByKey(String key) {

        if (clusters.isEmpty()) {
            throw new SsdbNoClusterAvailableException("ALL CLUSTERS DOWN");
        }

        return this.shardingStrategy.getClusterByKey(key);
    }

    public void reportInvalidCluster(Cluster cluster) {
        this.shardingStrategy.clusterFailed(cluster);
    }
}
