package com.hyd.ssdb.springboot;

import com.hyd.ssdb.SsdbClient;
import com.hyd.ssdb.conf.Cluster;
import com.hyd.ssdb.conf.Server;
import com.hyd.ssdb.sharding.ConsistentHashSharding;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(
    HydrogenSsdbConf.class
)
public class HydrogenSsdbConfigurator {

    @Bean
    @ConditionalOnMissingBean(SsdbClient.class)
    SsdbClient ssdbClient(HydrogenSsdbConf config) {
        return new SsdbClient(new ConsistentHashSharding(
                config.getClusters().stream()
                        .map(this::toCluster).collect(Collectors.toList())
        ));
    }

    private Cluster toCluster(ClusterConf clusterConf) {
        return new Cluster(clusterConf.getServers()
                .stream().map(this::toServer).collect(Collectors.toList()));
    }

    private Server toServer(ServerConf serverConf) {
        Server server = new Server();
        server.setPoolConfig(serverConf.getPool());
        server.setSocketConfig(serverConf.getSocket());
        server.setMaster(serverConf.isMaster());
        server.setHost(serverConf.getHost());
        server.setPort(serverConf.getPort());
        server.setPass(serverConf.getPass());
        return server;
    }
}
