package com.hyd.ssdb.conf;

import com.hyd.ssdb.SsdbClientException;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;

/**
 * 表示一个集群。集群是负载均衡的基本单位，一个集群里面可以配置一台或多台服务器（{@link Server}）。
 * created at 15-12-3
 *
 * @author Yiding
 */
public class Cluster {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static final int DEFAULT_WEIGHT = 100;

    ////////////////////////////////////////////////////////////////

    private String id = String.valueOf(hashCode());

    private List<Server> servers;

    private int weight = DEFAULT_WEIGHT;

    private int minHash = Integer.MIN_VALUE;

    private int maxHash = Integer.MAX_VALUE;

    public Cluster(List<Server> servers, int weight) {

        servers.removeAll(Collections.singleton((Server)null));
        if (servers.isEmpty()) {
            throw new SsdbClientException("servers is empty");
        }

        this.servers = Collections.unmodifiableList(servers);
        this.weight = weight;

        this.id = servers.get(0).getHost() + ":" + servers.get(0).getPort();
    }

    public Cluster(Server server, int weight) {
        this(Collections.singletonList(server), weight);
    }

    public Cluster(List<Server> servers) {
        this(servers, DEFAULT_WEIGHT);
    }

    public Cluster(Server server) {
        this(server, DEFAULT_WEIGHT);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getMinHash() {
        return minHash;
    }

    public void setMinHash(int minHash) {
        this.minHash = minHash;
    }

    public int getMaxHash() {
        return maxHash;
    }

    public void setMaxHash(int maxHash) {
        this.maxHash = maxHash;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public List<Server> getServers() {
        return servers;
    }

    /**
     * 获取主服务器。如果只有一台服务器，则认定它是主服务器；否则必须有一台设置为 master=true
     *
     * @return 主服务器，如果找不到则抛出异常
     */
    public Server getMaster() {
        if (servers.size() == 1) {
            return servers.get(0);
        }

        for (Server server : servers) {
            if (server.isMaster()) {
                return server;
            }
        }

        throw new SsdbClientException("Unable to find master server in cluster '" + id + "'");
    }

    public Server getRandomServer() {
        if (servers.size() == 1) {
            return servers.get(0);
        }

        return servers.get(RANDOM.nextInt(servers.size()));
    }
}
