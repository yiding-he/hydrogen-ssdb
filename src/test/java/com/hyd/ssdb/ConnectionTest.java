package com.hyd.ssdb;

import com.hyd.ssdb.conn.Connection;
import com.hyd.ssdb.protocol.Request;
import com.hyd.ssdb.protocol.Response;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (description)
 * created at 15-12-2
 *
 * @author Yiding
 */
public class ConnectionTest {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionTest.class);

    @Test
    public void testReceive2() throws Exception {
        Connection connection = new Connection("localhost", 18801, null, 1000, 8192);

        connection.send(new Request("set name hydrogen-ssdb").toBytes());
        Response response1 = connection.receivePacket();
        System.out.println(response1.getHead().toString());
        System.out.println(response1.getBody());

        connection.send(new Request("get name").toBytes());
        Response response2 = connection.receivePacket();
        System.out.println(response2.getHead().toString());
        System.out.println(response2.getBody());
    }

    @Test
    public void testConnectionFail() throws Exception {
        try {
            SsdbClient ssdbClient = new SsdbClient("localhost", 999);
            ssdbClient.set("name", "value");
        } catch (SsdbException e) {
            LOG.error("", e);
        }
    }
}