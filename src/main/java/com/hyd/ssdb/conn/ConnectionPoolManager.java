package com.hyd.ssdb.conn;

import com.hyd.ssdb.conf.Cluster;
import com.hyd.ssdb.conf.Server;
import com.hyd.ssdb.conf.Sharding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * (description)
 * created at 15-12-3
 *
 * @author Yiding
 */
public class ConnectionPoolManager {

    static final Logger LOG = LoggerFactory.getLogger(ConnectionPoolManager.class);

    private Sharding sharding;

    private Map<Server, ConnectionPool> connectionPoolMap = new ConcurrentHashMap<Server, ConnectionPool>();

    public ConnectionPoolManager(Sharding sharding) {
        this.sharding = sharding;
    }

    public ConnectionPool getConnectionPool(String key, boolean write) {
        Cluster cluster = sharding.getCluster(key);
        // LOG.debug("key {} related to cluster {}", key, cluster.getId());
        return pickServer(cluster, write);
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

    private ConnectionPool getConnectionPool(final Server server) {
        synchronized (server) {
            if (connectionPoolMap.containsKey(server)) {
                return connectionPoolMap.get(server);
            }

            ConnectionPool connectionPool = createConnectionPool(server);
            connectionPoolMap.put(server, connectionPool);
            return connectionPool;
        }
    }

    private ConnectionPool createConnectionPool(Server server) {
        return new ConnectionPool(server);
    }

    public void close() {
        for (ConnectionPool pool : connectionPoolMap.values()) {
            try {
                pool.close();
            } catch (Exception e) {
                LOG.error("Error closing connection pool", e);
            }
        }
    }
}
