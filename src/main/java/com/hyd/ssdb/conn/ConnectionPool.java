package com.hyd.ssdb.conn;

import com.hyd.ssdb.conf.Server;
import org.apache.commons.pool2.impl.GenericObjectPool;

/**
 * 连接池。对每个 Server 都需要创建各自的连接池对象。
 */
public abstract class ConnectionPool extends GenericObjectPool<Connection> {

    /**
     * 构造方法
     * @param server 服务器配置
     */
    public ConnectionPool(Server server) {
        super(new ConnectionFactory(server));
    }

    public Server getServer() {
        return ((ConnectionFactory) this.getFactory()).getServer();
    }
}
