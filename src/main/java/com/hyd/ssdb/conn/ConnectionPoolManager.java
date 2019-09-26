package com.hyd.ssdb.conn;

import com.hyd.ssdb.*;
import com.hyd.ssdb.conf.*;
import com.hyd.ssdb.protocol.Request;
import com.hyd.ssdb.protocol.WriteRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NetworkManager 有两个职责：
 * 1、管理网络的拓扑结构（通过 Sharding 类），决定请求发送到哪个 SSDB 服务器；
 * 2、当请求发送失败时，自动更新失效的服务器列表，并尝试重新发送请求到同一
 * Cluster 的其他服务器，直到没有服务器可用，才抛出异常。
 *
 * @author Yiding
 */
public class ConnectionPoolManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionPoolManager.class);

    private Sharding sharding;  // 负载均衡拓扑结构

    private Map<Server, ConnectionPool> connectionPoolMap = new ConcurrentHashMap<Server, ConnectionPool>();

    public ConnectionPoolManager(Sharding sharding) {
        this.sharding = sharding;
        this.sharding.initClusters();
    }

    public Sharding getSharding() {
        return sharding;
    }

    public List<PoolAndConnection> getAllConnections(Request request) {
        boolean write = request instanceof WriteRequest;
        List<PoolAndConnection> result = new ArrayList<PoolAndConnection>();

        try {
            for (Cluster cluster : this.sharding.getClusters()) {
                ConnectionPool pool = pickServer(cluster, write);
                result.add(new PoolAndConnection(pool, pool.borrowObject()));
            }
        } catch (Exception e) {
            throw new SsdbClientException(e);
        }

        return result;
    }

    /**
     * 根据请求获取一个连接。如果 Cluster 的某个服务器无法创建连接，则自动切换
     * 到其他可用的服务器；如果所有的服务器都不可用，则抛出 SsdbNoServerAvailableException
     *
     * @param request 请求
     *
     * @return 连接和连接池
     */
    public PoolAndConnection getConnection(Request request)
            throws SsdbNoServerAvailableException {

        String key = request.getKey();   // 某些命令 key 可能为空
        boolean write = request instanceof WriteRequest;
        boolean retry = false;

        do {
            Cluster cluster = null;
            ConnectionPool connectionPool = null;
            try {

                // 如果指定了 forceServer，则 cluster 为空
                if (request.getForceServer() != null) {
                    connectionPool = getConnectionPool(request.getForceServer());
                } else {
                    cluster = sharding.getClusterByKey(key);
                    connectionPool = pickServer(cluster, write);
                }

                Connection connection = connectionPool.borrowObject();
                return new PoolAndConnection(connectionPool, connection);

            } catch (SsdbSocketFailedException e) { // 表示 server 连接创建失败
                LOG.error("Connection failed: ", e);
                if (connectionPool != null) {
                    Server server = connectionPool.getConnectionFactory().getServer();
                    // 将服务器标记为不可用，这样下次 do-while 循环就会跳过该服务器
                    reportInvalidConnection(server.getHost(), server.getPort());
                    retry = true;
                }

            } catch (SsdbNoServerAvailableException e) {  // 遇到 cluster 单点故障，尝试切换 Cluster

                // 只有未指定 forceServer 才可能抛出这个异常，此时 cluster 一定不为空
                // 向 Sharding 报告 Cluster 无法使用。如果 Sharding 不能接受，则返回 false
                boolean keepSearching = sharding.clusterFailed(cluster);

                if (!keepSearching) {
                    throw e;
                } else {
                    LOG.error("Connection failed: ", e);
                    retry = true;
                    // 当启用 AutoExpandStrategy 时，下次循环会再次调用 getClusterByKey()
                }

            } catch (SsdbNoClusterAvailableException e) {  // 无法再继续尝试切换 Cluster
                LOG.error("Connection failed: ", e);
                throw e;

            } catch (Exception e) {
                throw new SsdbClientException(e);
            }
        } while (retry);

        throw new SsdbClientException("should not be here");
    }

    /**
     * 取一个服务器。如果是写操作，则只取主服务器；否则随机取一个服务器
     *
     * @param cluster 集群配置
     * @param write   是否是写操作
     *
     * @return 取到的服务器。如果取不到，则抛出异常
     */
    private ConnectionPool pickServer(Cluster cluster, boolean write) {
        Server server;
        if (write) {
            server = cluster.getMaster();
        } else {
            server = cluster.getRandomServer();
        }
        return getConnectionPool(server);
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private ConnectionPool getConnectionPool(final Server server) {
        synchronized (server) {
            if (connectionPoolMap.containsKey(server)) {
                return connectionPoolMap.get(server);
            }

            ConnectionPool connectionPool = createConnectionPool(server);
            connectionPool.setTestOnReturn(true);
            connectionPool.setTestOnBorrow(true);
            connectionPoolMap.put(server, connectionPool);
            return connectionPool;
        }
    }

    private ConnectionPool createConnectionPool(Server server) {
        return new ConnectionPool(server);
    }

    /**
     * 关闭所有连接池
     */
    public void close() {
        for (ConnectionPool pool : connectionPoolMap.values()) {
            try {
                pool.close();
            } catch (Exception e) {
                LOG.error("Error closing connection pool", e);
            }
        }
    }

    public void reportInvalidConnection(Connection connection) {
        String host = connection.getProperty("host");
        Integer port = connection.getProperty("port");
        reportInvalidConnection(host, port);
    }

    public void reportInvalidConnection(String host, int port) {

        for (Cluster cluster : sharding.getClusters()) {
            Server server = cluster.findServer(host, port);
            if (server != null) {
                cluster.markInvalid(server);
            }
        }
    }
}
