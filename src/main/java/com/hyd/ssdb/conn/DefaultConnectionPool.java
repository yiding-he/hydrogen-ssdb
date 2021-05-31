package com.hyd.ssdb.conn;

import com.hyd.ssdb.conf.Server;

/**
 * 连接池
 * created at 15-12-2
 *
 * @author Yiding
 */
public class DefaultConnectionPool extends ConnectionPool {

    /**
     * 构造方法
     *
     * @param server 包含服务器配置和连接池配置
     */
    public DefaultConnectionPool(Server server) {
        super(server);
        setConfig(server.getPoolConfig());
    }
}
