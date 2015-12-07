package com.hyd.ssdb.conf;

import com.hyd.ssdb.SsdbClientException;
import com.hyd.ssdb.SsdbException;
import com.hyd.ssdb.util.MD5;

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

    /**
     * 构造方法
     *
     * @param clusters 集群配置，Sharding 对象创建后不能再增删 Cluster
     */
    public Sharding(List<Cluster> clusters) {

        clusters.removeAll(Collections.singleton((Cluster) null));
        if (clusters.isEmpty()) {
            throw new SsdbClientException("clusters is empty");
        }

        this.clusters = Collections.unmodifiableList(clusters);
        setupHashRing();
    }

    /**
     * 构造方法
     *
     * @param cluster 集群配置，整个负载均衡当中只会有一个集群
     */
    public Sharding(Cluster cluster) {
        this(Collections.singletonList(cluster));
    }

    public static Sharding fromSingleServer(Server server) {
        return new Sharding(Collections.singletonList(
                new Cluster(Collections.singletonList(server))));
    }

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

    ////////////////////////////////////////////////////////////////

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

    public List<Cluster> getClusters() {
        return clusters;
    }

    public Cluster getClusterById(String id) {
        for (Cluster cluster : clusters) {
            if (cluster.getId().equals(id)) {
                return cluster;
            }
        }

        return null;
    }

    // 对服务器节点构建一个 Hash 环，每一个 Key 都在环当中的某个位置上
    private void setupHashRing() {

        if (clusters.size() == 1) {
            clusters.get(0).setMinHash(Integer.MIN_VALUE);
            clusters.get(0).setMaxHash(Integer.MAX_VALUE);
            return;
        }

        int maxWeight = 0;
        for (Cluster cluster : clusters) {
            maxWeight += cluster.getWeight();
        }

        long maxrange = (long) Integer.MAX_VALUE - (long) Integer.MIN_VALUE;
        long weightcounter = 0;
        long pointer = 0;

        for (int i = 0; i < clusters.size(); i++) {
            Cluster cluster = clusters.get(i);
            if (i == 0) {
                cluster.setMinHash(Integer.MIN_VALUE);
            } else if (i == clusters.size() - 1) {
                cluster.setMinHash((int) (pointer + Integer.MIN_VALUE) + 1);
                cluster.setMaxHash(Integer.MAX_VALUE);
                continue;
            }

            if (i > 0) {
                cluster.setMinHash((int) (pointer + Integer.MIN_VALUE) + 1);
            }

            weightcounter += cluster.getWeight();
            pointer = maxrange * weightcounter / maxWeight;
            cluster.setMaxHash((int) (pointer + Integer.MIN_VALUE));
        }
    }

    /**
     * 通过给定的 key 判断其所属的集群
     *
     * @param key key
     *
     * @return key 所属的集群
     */
    public Cluster getClusterByKey(String key) {

        if (clusters.size() == 1) {
            return clusters.get(0);
        }

        int hash = MD5.md5Hash(key);

        for (Cluster cluster : clusters) {
            if (cluster.getMinHash() <= hash && cluster.getMaxHash() >= hash) {
                return cluster;
            }
        }

        // 因为 clusters 列表一定会包含 Integer 的所有值
        throw new SsdbException("Unable to choose a cluster for key '" + key + "'");
    }
}
