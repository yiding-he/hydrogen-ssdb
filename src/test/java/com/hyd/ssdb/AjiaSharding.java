package com.hyd.ssdb;

import com.hyd.ssdb.conf.Cluster;
import com.hyd.ssdb.conf.Server;
import com.hyd.ssdb.conf.Sharding;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * (description)
 * created at 15-12-9
 *
 * @author Yiding
 */
public class AjiaSharding extends Sharding {

    private Map<String, Cluster> clusterMap = new HashMap<String, Cluster>();

    @Override
    public void initClusters() {

    }

    // projectId:range:type:props
    @Override
    public Cluster getClusterByKey(String key) {
        String projectId = StringUtils.substringBefore(key, ":");

        if (!clusterMap.containsKey(projectId)) {
            Server server = findServerByProject(projectId);
            clusterMap.put(projectId, new Cluster(server));
        }

        return clusterMap.get(projectId);
    }

    private Server findServerByProject(String projectId) {
        String host = "192.168.1.180";
        int port = 8888;
        return new Server(host, port);
    }

    @Override
    public void clusterFailed(Cluster invalidCluster) {

    }
}
