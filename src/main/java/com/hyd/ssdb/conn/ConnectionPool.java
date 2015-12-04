package com.hyd.ssdb.conn;

import com.hyd.ssdb.conf.Server;
import org.apache.commons.pool2.impl.GenericObjectPool;

/**
 * 连接池
 * created at 15-12-2
 *
 * @author Yiding
 */
public class ConnectionPool extends GenericObjectPool<Connection> {

    /**
     * 构造方法
     *
     * @param server 包含服务器配置和连接池配置
     */
    public ConnectionPool(Server server) {
        super(new ConnectionFactory(server));
        setConfig(server.getPoolConfig());
    }

    public ConnectionFactory getConnectionFactory() {
        return (ConnectionFactory) this.getFactory();
    }
}
