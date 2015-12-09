package com.hyd.ssdb.sharding;

import com.hyd.ssdb.conf.Cluster;

import java.util.List;

/**
 * 分片策略
 * created at 15-12-8
 *
 * @author Yiding
 */
public abstract class ShardingStrategy {

    protected List<Cluster> clusters;

    public void setClusters(List<Cluster> clusters) {
        this.clusters = clusters;
        initClusters();
    }

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
}
