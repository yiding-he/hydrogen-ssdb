package com.hyd.ssdb.conf;

import com.hyd.ssdb.SsdbClientException;
import com.hyd.ssdb.SsdbNoServerAvailableException;
import com.hyd.ssdb.conn.ServerMonitorDaemon;
import com.hyd.ssdb.util.Range;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 表示一个集群。集群是负载均衡的基本单位，一个集群里面可以配置一台或多台服务器（{@link Server}）。
 * 这个类里面几个方法标上了 synchronized，考虑到：一个 Cluster 最多只会包含几台十几台服务器，
 * synchronized 方法执行速度会很快，而且服务器的变更不会很频繁，所以没有使用复杂的同步方式。
 * fillMasters() 方法没有加上 synchronized 是因为它在构造方法中执行，构造方法不会被多线程访问。
 * <p>
 * created at 15-12-3
 *
 * @author Yiding
 */
public class Cluster {

    public static final int DEFAULT_WEIGHT = 100;

    private static final Logger LOG = LoggerFactory.getLogger(Cluster.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    private String id = String.valueOf(hashCode());

    private boolean invalid = false;

    private List<Server> servers;

    private List<Server> masters = new ArrayList<Server>();

    private List<Server> invalidServers = new ArrayList<Server>();

    private int weight = DEFAULT_WEIGHT;

    private Range<Integer> hashRange;   // 该 Cluster 原本的哈希段

    private Cluster takenOverBy;        // 当 Cluster 不可用时，其哈希段会被另一个 Cluster 接管

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

    //////////////////////////////////////////////////////////////

    // 将一个 Server 列表转换为一个 Cluster 列表，其中每个 Cluster 包含一个 Server
    public static List<Cluster> toClusters(List<Server> servers) {
        ArrayList<Cluster> clusters = new ArrayList<Cluster>();
        for (Server server : servers) {
            clusters.add(new Cluster(server));
        }
        return clusters;
    }

    public static Cluster fromSingleServer(Server server) {
        return new Cluster(server);
    }

    public static Cluster fromSingleServer(String host, int port) {
        return fromSingleServer(new Server(host, port));
    }

    public static Cluster fromSingleServer(String host, int port, int timeoutSeconds) {
        return fromSingleServer(new Server(host, port, timeoutSeconds));
    }

    public static Cluster fromSingleServer(String host, int port, int timeoutSeconds, int bufferSize) {
        return fromSingleServer(new Server(host, port, timeoutSeconds, bufferSize));
    }

    public static Cluster fromSingleServer(String host, int port, String pass, int timeoutSeconds, int bufferSize) {
        return fromSingleServer(new Server(host, port, pass, true, timeoutSeconds, bufferSize));
    }

    public static Cluster fromSingleServer(String host, int port, String pass) {
        return fromSingleServer(new Server(host, port, pass));
    }

    public static Cluster fromServers(List<Server> servers) {
        return new Cluster(servers);
    }

    public static Cluster fromServers(Server... servers) {
        return new Cluster(Arrays.asList(servers));
    }

    //////////////////////////////////////////////////////////////

    // 将主服务器加入到 masters 列表
    private void fillMasters() {
        for (Server server : servers) {
            if (server.isMaster()) {
                this.masters.add(server);
            }
        }
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    public Range<Integer> getHashRange() {
        return hashRange;
    }

    public void setHashRange(Range<Integer> hashRange) {
        this.hashRange = hashRange;
    }

    public Cluster getTakenOverBy() {
        return takenOverBy;
    }

    public void setTakenOverBy(Cluster takenOverBy) {
        this.takenOverBy = takenOverBy;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public List<Server> getInvalidServers() {
        return Collections.unmodifiableList(invalidServers);
    }

    /**
     * 添加一台服务器
     *
     * @param server 要添加的服务器
     */
    public synchronized void addServer(Server server) {
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
    public synchronized void removeServer(Server server) {
        masters.remove(server);
        servers.remove(server);
    }

    /**
     * 获取一台主服务器（用于写入）。所有的服务器中必须至少有一台设置为 master=true
     *
     * @return 获取到的主服务器，如果找不到则抛出异常
     */
    public Server getMaster() {
        if (masters.isEmpty()) {
            throw new SsdbNoServerAvailableException(
                    "Unable to find master server in cluster '" + id + "'");
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
        if (servers.isEmpty()) {
            throw new SsdbNoServerAvailableException(
                    "Unable to find server in cluster '" + id + "'");
        }

        if (servers.size() == 1) {
            return servers.get(0);
        }

        return servers.get(RANDOM.nextInt(servers.size()));
    }

    /**
     * 将服务器标记为无效的
     *
     * @param invalid 需要被标记的服务器
     */
    public synchronized void markInvalid(Server invalid) {
        LOG.error("Server unavailable: " + invalid);

        this.servers.remove(invalid);
        if (invalid.isMaster()) {
            this.masters.remove(invalid);
        }

        if (!this.invalidServers.contains(invalid)) {
            this.invalidServers.add(invalid);
        }

        ServerMonitorDaemon.addInvalidServer(invalid, this);
    }

    /**
     * 将服务器标记为有效的
     *
     * @param server 恢复正常的服务器
     */
    public synchronized void markValid(Server server) {
        LOG.info("Server restored: " + server);

        this.servers.add(server);
        if (server.isMaster()) {
            this.masters.add(server);
        }

        this.invalidServers.remove(server);
        this.invalid = false;
    }

    /**
     * 返回指定 hash 所属的 Cluster
     *
     * @param hash 要查询的 hash
     *
     * @return 如果当前 Cluster 的哈希段包含该值，则返回当前 Cluster 或托管的 Cluster，否则返回 null
     */
    public Cluster getHashHostingCluster(int hash) {
        if (this.hashRange.contains(hash)) {
            return thisOrTakenOver();
        } else {
            return null;
        }
    }

    private Cluster thisOrTakenOver() {
        return this.invalid ? this.takenOverBy.thisOrTakenOver() : this;
    }

    @Override
    public String toString() {
        return "Cluster{" +
                "id='" + id + '\'' +
                ", weight=" + weight +
                '}';
    }

    /**
     * 判断 server 是否属于当前 cluster
     *
     * @param server 要判断的 server
     *
     * @return 如果属于当前 cluster 则返回 true
     */
    public synchronized boolean containsServer(Server server) {
        return this.servers.contains(server) || this.invalidServers.contains(server);
    }

    public Server findServer(String host, int port) {
        for (Server server : servers) {
            if (server.getHost().equals(host) && server.getPort() == port) {
                return server;
            }
        }

        return null;
    }
}
