package com.hyd.ssdb.conn;

import com.hyd.ssdb.conf.Server;

/**
 * 根据 Server 配置创建 ConnectionPool 实例。通过创建它的实现类可以实现自定义的连接池。甚至可以针对单个 Server 实现自定义的连接池：
 * <pre>
 * ConnectionPoolFactory factory = (server) -&gt; {
 *     if (server.getHost().equals("localhost")) {
 *         return new ConnectionPoolForLocalhost(server);
 *     } else {
 *         return ConnectionPoolManager.DEFAULT_CONNECTION_POOL_FACTORY;
 *     }
 * }
 * connectionPoolManager.setConnectionPoolFactory(factory);
 * </pre>
 */
@FunctionalInterface
public interface ConnectionPoolFactory {

    ConnectionPool createConnectionPool(Server server);
}
