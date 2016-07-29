package com.hyd.ssdb;

import com.hyd.ssdb.conn.Connection;
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
    public void testReceive2() throws Exception {
        Connection connection = new Connection("localhost", 8881, 1000, 8192);

        connection.send(new Request("set name hydrogen-ssdb").toBytes());
        Response response1 = connection.receivePacket();
        System.out.println(response1.getHead().toString());
        System.out.println(response1.getBody());

        connection.send(new Request("get name").toBytes());
        Response response2 = connection.receivePacket();
        System.out.println(response2.getHead().toString());
        System.out.println(response2.getBody());
    }
}