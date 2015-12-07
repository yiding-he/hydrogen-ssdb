package com.hyd.ssdb;

import com.hyd.ssdb.conf.Server;
import com.hyd.ssdb.conn.Connection;
import com.hyd.ssdb.conn.ConnectionPool;
import com.hyd.ssdb.protocol.Request;
import com.hyd.ssdb.protocol.Response;
import org.junit.Test;

/**
 * (description)
 * created at 15-12-2
 *
 * @author Yiding
 */
public class ConnectionTest {

    @Test
    public void testSend() throws Exception {
        // Connection connection = new Connection("heyiding.com", 27364);
        Connection connection = new Connection("192.168.1.180", 8888, 1000);
        Response response;

        connection.send(new Request("set name heyiding").toBytes());
        response = new Response(connection.receivePacket());
        System.out.println(response.getHeader());
        System.out.println(response.getBlocks());


        connection.send(new Request("get name").toBytes());
        response = new Response(connection.receivePacket());
        System.out.println(response.getHeader());
        System.out.println(response.getBlocks());

        connection.close();
    }

    @Test(expected = SsdbSocketFailedException.class)
    public void testConnectionFail() throws Exception {
        ConnectionPool connectionPool = new ConnectionPool(new Server("localhost", 12345));
        connectionPool.borrowObject();
    }
}