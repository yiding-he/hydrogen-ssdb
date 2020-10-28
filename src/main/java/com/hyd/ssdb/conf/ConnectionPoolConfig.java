package com.hyd.ssdb.conf;

import com.hyd.ssdb.conn.Connection;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * 自定义连接池配置
 */
public class ConnectionPoolConfig extends GenericObjectPoolConfig<Connection> {

    /**
     * 默认的 toString() 输出内容太长
     */
    @Override
    public String toString() {
        return "ConnectionPoolConfig{" +
            "maxTotal=" + getMaxTotal() +
            ", maxIdle=" + getMaxIdle() +
            "}";
    }
}
