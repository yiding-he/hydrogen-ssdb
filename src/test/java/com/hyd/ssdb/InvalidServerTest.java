package com.hyd.ssdb;

import com.hyd.ssdb.conf.Server;
import com.hyd.ssdb.conf.Sharding;
import org.junit.Test;

import java.util.Arrays;

/**
 * (description)
 * created at 15-12-7
 *
 * @author Yiding
 */
public class InvalidServerTest {

    @Test
    public void testInvalidServer() throws Exception {
        Sharding sharding = Sharding.fromServerList(Arrays.asList(
                new Server("192.168.1.180", 8888),
                new Server("192.168.1.180", 8889),
                new Server("192.168.1.180", 8890)
        ));

        SsdbClient client = new SsdbClient(sharding);
        client.set("name", "ssdb123");
        System.out.println(client.get("name"));
        System.out.println(client.get("name"));
        System.out.println(client.get("name"));
        System.out.println(client.get("name"));
        System.out.println(client.get("name"));
        System.out.println(client.get("name"));
        System.out.println(client.get("name"));

        client.close();
    }
}
