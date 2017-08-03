package com.hyd.ssdb;

import com.hyd.ssdb.conf.Cluster;
import com.hyd.ssdb.sharding.ConsistentHashSharding;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * (description)
 * created at 2017/8/3
 *
 * @author yidin
 */
public class InvalidClusterTest {

    private static final AtomicInteger version = new AtomicInteger();

    public static void main(String[] args) throws Exception {
        SsdbClient ssdbClient = new SsdbClient(new ConsistentHashSharding(
                Cluster.fromSingleServer("heyiding.com", 18880),
                Cluster.fromSingleServer("heyiding.com", 18881)
        ));

        ssdbClient.del("name");  // init

        while (true) {
            try {
                Thread.sleep(500);
                System.out.println(readFromCache(ssdbClient));
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            } catch (Exception e) {
                System.err.println(e.toString());
            }
        }

        ssdbClient.close();
    }

    private static String readFromCache(SsdbClient ssdbClient) {
        String name = ssdbClient.get("name");
        if (name == null) {
            name = generateName();
            ssdbClient.set("name", name);
        }
        return name;
    }

    private static String generateName() {
        return "hydrogen-ssdb-v" + version.incrementAndGet();
    }
}
