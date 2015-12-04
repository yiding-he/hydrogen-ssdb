package com.hyd.ssdb.conn;

import com.hyd.ssdb.conf.Server;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * 创建 Connection 对象并检查其状态的工厂类
 *
 * @author Yiding
 */
public class ConnectionFactory implements PooledObjectFactory<Connection> {

    private Server server;

    public ConnectionFactory(Server server) {
        this.server = server;
    }

    public Server getServer() {
        return server;
    }

    public PooledObject<Connection> makeObject() throws Exception {
        return new DefaultPooledObject<Connection>(new Connection(server));
    }

    public void destroyObject(PooledObject<Connection> p) throws Exception {
        p.getObject().close();
    }

    public boolean validateObject(PooledObject<Connection> p) {
        return p.getObject().isAvailable();
    }

    public void activateObject(PooledObject<Connection> p) throws Exception {

    }

    public void passivateObject(PooledObject<Connection> p) throws Exception {

    }
}
