package com.hyd.ssdb.conf;

import com.hyd.ssdb.SsdbClientException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 多台服务器的拓扑结构配置
 * created at 15-12-3
 *
 * @author Yiding
 */
public abstract class Sharding {

    protected List<Cluster> clusters;

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
        initClusters();
    }

    //////////////////////////////////////////////////////////////

    /**
     * 初始化
     */
    public abstract void initClusters();

    /**
     * 根据 key 获取所对应的 Cluster
     *
     * @param key 键
     *
     * @return 对应的 Cluster
     */
    public abstract Cluster getClusterByKey(String key);

    /**
     * 当整个 Cluster 下线时的处理
     *
     * @param invalidCluster 下线的 Cluster
     */
    public abstract void clusterFailed(Cluster invalidCluster);

    //////////////////////////////////////////////////////////////

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

    public void reportInvalidCluster(Cluster cluster) {
        clusterFailed(cluster);
    }
}
