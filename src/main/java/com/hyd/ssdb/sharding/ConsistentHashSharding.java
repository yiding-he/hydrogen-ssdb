package com.hyd.ssdb.sharding;

import com.hyd.ssdb.SsdbClientException;
import com.hyd.ssdb.SsdbException;
import com.hyd.ssdb.SsdbNoClusterAvailableException;
import com.hyd.ssdb.conf.Cluster;
import com.hyd.ssdb.conf.SPOFStrategy;
import com.hyd.ssdb.conf.Sharding;
import com.hyd.ssdb.util.MD5;
import com.hyd.ssdb.util.Range;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于一致性哈希的分片策略。这是 hydrogen-ssdb 实现的缺省分片策略。
 * <p/>
 * ConsistentHashSharding 有一个属性叫做 {@link #spofStrategy}，用于决定当出现单点故障时如何处理。
 * <p/>
 * created at 15-12-8
 *
 * @author Yiding
 */
public class ConsistentHashSharding extends Sharding {

    private Map<Cluster, Range<Integer>> rangeMap = new HashMap<Cluster, Range<Integer>>();

    /**
     * 单点故障处理策略，参考 {@link SPOFStrategy}
     */
    private SPOFStrategy spofStrategy = SPOFStrategy.AutoExpandStrategy;

    public ConsistentHashSharding(Cluster cluster) {
        super(cluster);
    }

    //////////////////////////////////////////////////////////////

    public ConsistentHashSharding(List<Cluster> clusters) {
        super(clusters);
    }

    public SPOFStrategy getSpofStrategy() {
        return spofStrategy;
    }

    public void setSpofStrategy(SPOFStrategy spofStrategy) {
        this.spofStrategy = spofStrategy;
    }


    public void removeCluster(String clusterId) {
        clusterFailed(getClusterById(clusterId));
    }

    /**
     * 添加一个 Cluster
     *
     * @param newCluster  要加入的 Cluster
     * @param prevCluster 需要被分担负载的 Cluster
     */
    public synchronized void addCluster(Cluster newCluster, Cluster prevCluster) {
        if (clusters.contains(newCluster)) {
            return;
        }

        clusters.add(newCluster);
        Range<Integer> prevClusterRange = rangeMap.get(prevCluster);
        int prevMax = prevClusterRange.getMax();
        int split = prevClusterRange.getMin() +
                (prevClusterRange.getMax() - prevClusterRange.getMin()) * prevCluster.getWeight()
                        / (prevCluster.getWeight() + newCluster.getWeight());

        prevClusterRange.setMax(split);

        Range<Integer> newClusterRange = new Range<Integer>(split + 1, prevMax);
        rangeMap.put(newCluster, newClusterRange);
    }

    @Override
    public void initClusters() {

        if (clusters.size() == 1) {
            setClusterRange(clusters.get(0), Integer.MIN_VALUE, Integer.MAX_VALUE);
            return;
        }

        // 计算所有权重总和
        int maxWeight = 0;
        for (Cluster cluster : clusters) {
            maxWeight += cluster.getWeight();
        }

        // 为每个权重分配 hash 段
        long maxrange = (long) Integer.MAX_VALUE - (long) Integer.MIN_VALUE;
        long weightcounter = 0;
        long pointer = 0;

        for (int i = 0; i < clusters.size(); i++) {
            Cluster cluster = clusters.get(i);
            int min, max;

            if (i == clusters.size() - 1) {
                min = ((int) (pointer + Integer.MIN_VALUE) + 1);
                max = (Integer.MAX_VALUE);
                setClusterRange(cluster, min, max);

            } else {
                min = i == 0 ? Integer.MIN_VALUE : (int) (pointer + Integer.MIN_VALUE) + 1;

                weightcounter += cluster.getWeight();
                pointer = maxrange * weightcounter / maxWeight;
                max = ((int) (pointer + Integer.MIN_VALUE));

                setClusterRange(cluster, min, max);
            }
        }
    }

    private void setClusterRange(Cluster cluster, int minValue, int maxValue) {
        Range<Integer> range = new Range<Integer>(minValue, maxValue);
        rangeMap.put(cluster, range);
    }

    @Override
    public void clusterFailed(Cluster invalidCluster) {
        if (invalidCluster == null) {
            return;
        }

        if (this.spofStrategy == SPOFStrategy.AutoExpandStrategy) {
            autoExpand(invalidCluster);
            clusters.remove(invalidCluster);
        }

        // 否则就保留 key 空间，直到 Cluster 恢复上线
    }

    /**
     * 自动扩展 key 空间，将 invalidCluster 的 key 空间交给其他 Cluster
     *
     * @param invalidCluster 已下线的 Cluster
     */
    private synchronized void autoExpand(Cluster invalidCluster) {
        if (!rangeMap.containsKey(invalidCluster)) {
            return;
        }

        clusters.remove(invalidCluster);
        if (clusters.isEmpty()) {
            throw new SsdbNoClusterAvailableException("No cluster exists");
        }

        int minHash = rangeMap.get(invalidCluster).getMin();
        int maxHash = rangeMap.get(invalidCluster).getMax();

        // 如果是第一个 Cluster，则将后面的向前扩展；否则令前面的向后扩展
        if (minHash == Integer.MIN_VALUE) {
            rangeMap.get(clusters.get(0)).setMin(Integer.MIN_VALUE);
            return;
        } else {
            for (Cluster cluster : clusters) {
                if (rangeMap.get(cluster).getMax() + 1 == minHash) {   // 找到上一个 cluster
                    rangeMap.get(cluster).setMax(maxHash);
                    return;
                }
            }
        }

        throw new SsdbClientException("should not be here");
    }

    @Override
    public Cluster getClusterByKey(String key) {
        if (clusters.isEmpty()) {
            throw new SsdbNoClusterAvailableException("ALL CLUSTERS DOWN");
        }

        if (clusters.size() == 1) {
            return clusters.get(0);
        }

        int hash = MD5.md5Hash(key);

        for (Cluster cluster : clusters) {
            if (rangeMap.containsKey(cluster) && rangeMap.get(cluster).contains(hash)) {
                return cluster;
            }
        }

        // 因为 clusters 列表一定会包含 Integer 的所有值
        throw new SsdbException("Unable to choose a cluster for key '" + key + "'");
    }
}
