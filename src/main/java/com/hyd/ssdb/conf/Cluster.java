package com.hyd.ssdb.conf;

import com.hyd.ssdb.SsdbClientException;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 表示一个集群。集群是负载均衡的基本单位，一个集群里面可以配置一台或多台服务器（{@link Server}）。
 * created at 15-12-3
 *
 * @author Yiding
 */
public class Cluster {

    public static final int DEFAULT_WEIGHT = 100;

    private static final SecureRandom RANDOM = new SecureRandom();

    ////////////////////////////////////////////////////////////////

    private String id = String.valueOf(hashCode());

    private List<Server> servers;

    private List<Server> masters = new ArrayList<Server>();

    private int weight = DEFAULT_WEIGHT;

    private int minHash = Integer.MIN_VALUE;

    private int maxHash = Integer.MAX_VALUE;

    public Cluster(List<Server> servers, int weight) {

        servers.removeAll(Collections.singleton((Server) null));
        if (servers.isEmpty()) {
            throw new SsdbClientException("servers is empty");
        }

        this.servers = new ArrayList<Server>(servers);
        this.weight = weight;
        this.id = servers.get(0).getHost() + ":" + servers.get(0).getPort();

        fillMasters();
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

    // 将主服务器加入到 masters 列表
    private void fillMasters() {
        for (Server server : servers) {
            if (server.isMaster()) {
                this.masters.add(server);
            }
        }
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
        return Collections.unmodifiableList(servers);
    }

    /**
     * 添加一台服务器
     *
     * @param server 要添加的服务器
     */
    public void addServer(Server server) {
        if (!servers.contains(server)) {
            servers.add(server);

            if (server.isMaster() && !masters.contains(server)) {
                masters.add(server);
            }
        }
    }

    /**
     * 删除一台服务器
     *
     * @param server 要删除的服务器
     */
    public void removeServer(Server server) {
        if (masters.contains(server)) {
            masters.remove(server);
        }

        if (servers.contains(server)) {
            servers.remove(server);
        }
    }

    /**
     * 获取一台主服务器（用于写入）。所有的服务器中必须至少有一台设置为 master=true
     *
     * @return 获取到的主服务器，如果找不到则抛出异常
     */
    public Server getMaster() {
        if (masters.isEmpty()) {
            throw new SsdbClientException("Unable to find master server in cluster '" + id + "'");
        }

        if (masters.size() == 1) {
            return masters.get(0);
        }

        return masters.get(RANDOM.nextInt(masters.size()));
    }

    /**
     * 获取一个随机的服务器（用于读取）
     *
     * @return 一个随机的服务器
     */
    public Server getRandomServer() {
        if (servers.size() == 1) {
            return servers.get(0);
        }

        return servers.get(RANDOM.nextInt(servers.size()));
    }
}
