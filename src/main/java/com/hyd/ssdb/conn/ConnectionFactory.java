package com.hyd.ssdb.conn;

import com.hyd.ssdb.conf.Server;
import com.hyd.ssdb.protocol.Request;
import com.hyd.ssdb.protocol.Response;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 创建 Connection 对象并检查其状态的工厂类。
 * 一个 ConnectionFactory 对象只针对一个 SSDB 服务器
 *
 * @author Yiding
 */
public class ConnectionFactory implements PooledObjectFactory<Connection> {

    static final Logger LOG = LoggerFactory.getLogger(ConnectionFactory.class);

    private final Server server;

    /**
     * 构造方法
     *
     * @param server 服务器配置
     */
    public ConnectionFactory(Server server) {
        this.server = server;
    }

    public Server getServer() {
        return server;
    }

    public PooledObject<Connection> makeObject() throws Exception {
        LOG.debug("Creating connection with " + server);
        return new DefaultPooledObject<>(new Connection(server));
    }

    public void destroyObject(PooledObject<Connection> p) throws Exception {
        p.getObject().close();
    }

    public boolean validateObject(PooledObject<Connection> p) {
        return p.getObject().isAvailable();
    }

    public void activateObject(PooledObject<Connection> p) throws Exception {
        Connection connection = p.getObject();
        connection.send(new Request(System.getProperty("SSDB_PING_COMMAND", "dbsize")).toBytes());
        Response response = connection.receivePacket();
        LOG.info(response.joinBlocks("\n"));
    }

    public void passivateObject(PooledObject<Connection> p) throws Exception {

    }
}
