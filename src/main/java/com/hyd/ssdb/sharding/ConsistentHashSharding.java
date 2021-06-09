package com.hyd.ssdb.sharding;

import com.hyd.ssdb.SsdbClientException;
import com.hyd.ssdb.SsdbException;
import com.hyd.ssdb.SsdbNoClusterAvailableException;
import com.hyd.ssdb.conf.Cluster;
import com.hyd.ssdb.conf.SPOFStrategy;
import com.hyd.ssdb.conf.Sharding;
import com.hyd.ssdb.util.DebugLogger;
import com.hyd.ssdb.util.MD5;
import com.hyd.ssdb.util.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于一致性哈希的分片策略。这是 hydrogen-ssdb 实现的缺省分片策略。
 * ConsistentHashSharding 有一个属性叫做 {@link #spofStrategy}，用于决定当出现单点故障时如何处理。
 *
 * @author Yiding
 */
public class ConsistentHashSharding extends Sharding {

    private static final Logger LOG = LoggerFactory.getLogger(ConsistentHashSharding.class);

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

    public ConsistentHashSharding(Cluster... clusters) {
        super(Arrays.asList(clusters));
    }

    public SPOFStrategy getSpofStrategy() {
        return spofStrategy;
    }

    public void setSpofStrategy(SPOFStrategy spofStrategy) {
        this.spofStrategy = spofStrategy;
    }

    public Map<String, Range<Integer>> getRangeMap() {
        HashMap<String, Range<Integer>> map = new HashMap<String, Range<Integer>>();
        for (Cluster cluster : clusters) {
            map.put(cluster.getId() + "(" + !cluster.isInvalid() + ")",
                    cluster.getHashRange().duplicate());
        }
        return map;
    }

    public void removeCluster(String clusterId) {

        DebugLogger.trace("Removing cluster {}, current clusters: {}", clusterId, clusters);

        Cluster cluster = getClusterById(clusterId);
        if (cluster == null) {
            throw new SsdbNoClusterAvailableException("Cluster " + clusterId + " not found.");
        }

        int minHash = cluster.getHashRange().getMin();
        int maxHash = cluster.getHashRange().getMax();

        // 如果是第一个 Cluster，则将后面的向前扩展；否则令前面的向后扩展
        boolean isFirstCluster = minHash == Integer.MIN_VALUE;

        if (isFirstCluster) {
            Cluster secondCluster = clusters.get(1);
            secondCluster.getHashRange().setMin(minHash);
            LOG.debug("Expand cluster " + secondCluster.getId() + " left to ring start.");
            return;
        } else {
            for (Cluster c : clusters) {
                if (c.getHashRange().getMax() + 1 == minHash) {   // 找到上一个 cluster
                    c.getHashRange().setMax(maxHash);
                    LOG.debug("Expand cluster " + cluster.getId() + " right to " + maxHash);
                    return;
                }
            }
        }

        throw new SsdbClientException("should not be here");
    }

    /**
     * 添加一个 Cluster
     *
     * @param newCluster  要加入的 Cluster
     * @param prevCluster 需要被分担负载的 Cluster
     */
    public synchronized void addCluster(Cluster newCluster, Cluster prevCluster) {

        DebugLogger.trace("Adding cluster {} after {}, current clusters: {}", newCluster, prevCluster, clusters);

        // check args
        if (clusters.contains(newCluster)) {
            return;
        }

        if (prevCluster == null) {
            throw new IllegalArgumentException("Argument prevCluster cannot be null");
        }

        if (!clusters.contains(prevCluster)) {
            throw new IllegalArgumentException("Argument prevCluster not found");
        }

        // insert into right position
        int prevClusterIndex = clusters.indexOf(prevCluster);
        if (prevClusterIndex == clusters.size() - 1) {
            clusters.add(newCluster);
        } else {
            clusters.add(prevClusterIndex + 1, newCluster);
        }

        // newCluster 和 prevCluster 的哈希段分配仍然依据各自的 weight 权重
        splitRangeToRight(newCluster, prevCluster);
    }

    // 将 toSplitCluster 的右边部分划分给 newCluster
    private void splitRangeToRight(Cluster c2, Cluster c1) {
        Range<Integer> r1 = c1.getHashRange();

        long split = r1.getMin() +
            (r1.getMax().longValue() - r1.getMin().longValue())
                * c1.getWeight() / (c1.getWeight() + c2.getWeight());

        // 更新范围
        setClusterRange(c1, r1.getMin(), (int) split);
        setClusterRange(c2, (int) split + 1, r1.getMax());
    }

    @Override
    public void initClusters() {

        if (clusters.isEmpty()) {
            return;
        } else {
            setClusterIdsIfDefault();
        }

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
                // 如果是最后一个节点，则 max 设为 Integer 的最大值
                min = ((int) (pointer + Integer.MIN_VALUE) + 1);
                max = (Integer.MAX_VALUE);
                setClusterRange(cluster, min, max);

            } else {
                // 非最后一个节点，则
                // 如果是第一个节点，则 min 为 Integer 的最小值，否则 min 为上一个节点最大值 + 1
                min = i == 0 ? Integer.MIN_VALUE : (int) (pointer + Integer.MIN_VALUE) + 1;

                // 非最后节点的 max 值计算：根据 weight 计算节点的哈希段占比（含值个数），加上 min 值即可
                weightcounter += cluster.getWeight();
                pointer = maxrange * weightcounter / maxWeight;
                max = ((int) (pointer + Integer.MIN_VALUE));

                setClusterRange(cluster, min, max);
            }
        }
    }

    private void setClusterIdsIfDefault() {
        for (int i = 0; i < clusters.size(); i++) {
            Cluster cluster = clusters.get(i);
            if (String.valueOf(cluster.hashCode()).equals(cluster.getId())) {
                cluster.setId("Cluster" + i);
            }
        }
    }

    private void setClusterRange(Cluster cluster, int minValue, int maxValue) {
        Range<Integer> range = new Range<Integer>(minValue, maxValue);
        cluster.setHashRange(range);
    }

    @Override
    public synchronized boolean clusterFailed(Cluster invalidCluster) {

        DebugLogger.trace("Cluster {} reporting failure, current clusters: {}", invalidCluster, clusters);

        if (invalidCluster == null) {
            return true;
        }

        if (this.spofStrategy == SPOFStrategy.AutoExpandStrategy) {
            invalidCluster.setInvalid(true);
            autoExpand(invalidCluster);
            return true;
        } else {
            // 保留 key 空间，直到 Cluster 恢复上线
            // 在此之前，对该 Cluster 的读写都将失败
            return false;
        }
    }

    /**
     * 自动扩展 key 空间，将 invalidCluster 的 key 空间交给其他 Cluster
     *
     * @param invalidCluster 已下线的 Cluster
     */
    private synchronized void autoExpand(Cluster invalidCluster) {

        DebugLogger.trace("Auto expanding to failed cluster {}, current clusters: {}",
            invalidCluster, clusters);

        if (!clusters.contains(invalidCluster)) {
            return;
        }

        if (noClusterAvailable()) {
            throw new SsdbNoClusterAvailableException();
        }

        int minHash = invalidCluster.getHashRange().getMin();
        int maxHash = invalidCluster.getHashRange().getMax();

        // 如果是第一个 Cluster，则将后面的向前扩展；否则令前面的向后扩展
        // 扩展本身不会修改 Cluster 的 range，
        boolean isFirstCluster = minHash == Integer.MIN_VALUE;

        if (isFirstCluster) {
            Cluster secondCluster = clusters.get(1);
            invalidCluster.setTakenOverBy(secondCluster);
            LOG.debug("Cluster " + secondCluster.getId() + " takes over " + invalidCluster.getId());
            return;
        } else {
            for (Cluster cluster : clusters) {
                if (cluster.getHashRange().getMax() + 1 == minHash) {   // 找到上一个 cluster
                    invalidCluster.setTakenOverBy(cluster);
                    LOG.debug("Cluster " + cluster.getId() + " takes over " + invalidCluster.getId());
                    return;
                }
            }
        }

        throw new SsdbClientException("should not be here");
    }

    private int getAvailableClusterCount() {
        int n = 0;
        for (Cluster cluster : clusters) {
            if (!cluster.isInvalid()) {
                n += 1;
            }
        }
        return n;
    }

    private boolean noClusterAvailable() {
        for (Cluster cluster : clusters) {
            if (!cluster.isInvalid()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Cluster getClusterByKey(String key) {

        if (noClusterAvailable()) {
            throw new SsdbNoClusterAvailableException();
        }

        int hash = MD5.md5Hash(key);

        for (Cluster cluster : clusters) {
            Cluster hostingCluster = cluster.getHashHostingCluster(hash);
            if (hostingCluster != null) {
                return hostingCluster;
            }
        }

        // 理论上 clusters 列表一定会包含 Integer 的所有值，
        // 所以执行到这里表示所有的 Cluster 都不可用
        throw new SsdbException("Unable to choose a cluster for key '" + key + "'");
    }
}
