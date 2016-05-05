package com.hyd.ssdb.conf;

import com.hyd.ssdb.SsdbClientException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 根据服务器的拓扑结构，决定一个请求应该被发送到哪台服务器
 * <p>
 * created at 15-12-3
 *
 * @author Yiding
 */
public abstract class Sharding {

    protected List<Cluster> clusters = new ArrayList<Cluster>();

    public Sharding() {
    }

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
     * @param clusters 集群配置。注意本方法将会使用 clusters 的拷贝，因此调用本方法之后再操作
     *                 clusters（清空或增减元素），不会对 Sharding 有任何影响。
     */
    public Sharding(List<Cluster> clusters) {

        // 清掉可能的 null 元素
        clusters.removeAll(Collections.singleton((Cluster) null));

        if (clusters.isEmpty()) {
            throw new SsdbClientException("clusters is empty");
        }

        this.clusters = new ArrayList<Cluster>(clusters);
    }

    //////////////////////////////////////////////////////////////

    /**
     * 对 {@link #clusters} 进行初始化配置，或执行其他初始化工作
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
     * 当某个 Cluster 下线（即 Cluster 中的所有的服务器都不可用）时的处理（交给子类实现）
     *
     * @param invalidCluster 下线的 Cluster
     */
    protected abstract void clusterFailed(Cluster invalidCluster);

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

    /**
     * 报告无法连接的 Cluster
     *
     * @param cluster 无法连接的 Cluster
     */
    public void reportInvalidCluster(Cluster cluster) {
        clusterFailed(cluster);
    }
}
