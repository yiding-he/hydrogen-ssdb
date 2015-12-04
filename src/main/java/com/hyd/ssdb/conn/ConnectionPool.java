package com.hyd.ssdb.conn;

import com.hyd.ssdb.conf.Server;
import org.apache.commons.pool2.impl.GenericObjectPool;

/**
 * (description)
 * created at 15-12-2
 *
 * @author Yiding
 */
public class ConnectionPool extends GenericObjectPool<Connection> {

    public ConnectionPool(Server server) {
        super(new ConnectionFactory(server));
        setConfig(server.getPoolConfig());
    }

    public ConnectionFactory getConnectionFactory() {
        return (ConnectionFactory) this.getFactory();
    }
}
