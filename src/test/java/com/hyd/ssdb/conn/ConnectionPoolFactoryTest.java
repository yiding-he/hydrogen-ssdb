package com.hyd.ssdb.conn;

import com.hyd.ssdb.BaseTest;
import com.hyd.ssdb.SsdbClient;
import com.hyd.ssdb.conf.Server;
import com.hyd.ssdb.protocol.Request;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Assert;
import org.junit.Test;

public class ConnectionPoolFactoryTest extends BaseTest {

    private static class LocalhostConnectionPool extends ConnectionPool {
        public LocalhostConnectionPool(Server server) {
            super(server);
            GenericObjectPoolConfig<Connection> config = server.getPoolConfig();
            config.setTestWhileIdle(true);
            config.setTestOnBorrow(true);
            // 每秒检查一次连接。默认情况下检查连接使用 'dbsize' 命令，
            // 但也可以通过 System.setProperty("SSDB_PING_COMMAND") 来修改
            config.setTimeBetweenEvictionRunsMillis(1000);
            setConfig(config);
        }
    }

    @Test
    public void testCustomConnectionPoolFactory() throws Exception {

        // 演示如何创建自定义的 ConnectionPoolFactory
        ssdbClient.getConnectionPoolManager().setConnectionPoolFactory(server -> {
            if (server.getHost().equals("127.0.0.1") || server.getHost().equals("localhost")) {
                return new LocalhostConnectionPool(server);
            } else {
                return new DefaultConnectionPool(server);
            }
        });

        PoolAndConnection pac = ssdbClient.getConnectionPoolManager().getConnection(new Request("get name"));
        Assert.assertEquals(LocalhostConnectionPool.class, pac.getConnectionPool().getClass());
        pac.getConnectionPool().returnObject(pac.getConnection());

        Thread.sleep(10000);
    }

    @Test
    public void testHeartbeat() throws Exception {
        Server server = new Server("localhost", 8888);

        GenericObjectPoolConfig<Connection> poolConfig = server.getPoolConfig();
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRunsMillis(1000);

        SsdbClient client = new SsdbClient(server);
        client.set("key", "value");
        Thread.sleep(10000);
        client.close();
    }
}
