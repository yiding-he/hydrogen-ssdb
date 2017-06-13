package com.hyd.ssdb.conn;

import com.hyd.ssdb.conf.Cluster;
import com.hyd.ssdb.conf.Server;
import com.hyd.ssdb.conf.SocketConfig;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * (description)
 * created at 2017/6/13
 *
 * @author yidin
 */
public class ServerMonitorDaemonTest {

    @Test
    public void testUnavailableServer() throws Exception {
        Server server = new Server("localhost", 55555, new SocketConfig(1000));
        long start = System.currentTimeMillis();
        assertFalse(ServerMonitorDaemon.isServerAvailable(server));
        System.out.println("time spend: " + (System.currentTimeMillis() - start));
    }

    @Test
    public void testAvailableServer() throws Exception {
        Server server = new Server("192.168.1.180", 8888, new SocketConfig(1000));
        long start = System.currentTimeMillis();
        assertTrue(ServerMonitorDaemon.isServerAvailable(server));
        System.out.println("time spend: " + (System.currentTimeMillis() - start));
    }

    @Test
    public void testMonitoring() throws Exception {
        Server server8888 = new Server("192.168.1.180", 8888);
        Server server8889 = new Server("192.168.1.180", 8889);
        Server server8890 = new Server("192.168.1.180", 8890);

        Cluster cluster = new Cluster(Arrays.asList(server8888, server8889, server8890));

        cluster.markInvalid(server8888);
        cluster.markInvalid(server8889);
        cluster.markInvalid(server8890);

        Thread.sleep(5000);

        System.out.println("Available: " + cluster.getServers());
        System.out.println("Unavailable: " + cluster.getInvalidServers());
        System.out.println("Monitoring: " + ServerMonitorDaemon.getCurrentMonitoringServers());
    }
}