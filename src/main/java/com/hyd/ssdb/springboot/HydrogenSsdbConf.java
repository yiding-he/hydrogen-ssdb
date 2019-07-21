package com.hyd.ssdb.springboot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("hydrogen-ssdb")
@Data
public class HydrogenSsdbConf {

    // TODO 改造配置类

    private List<ClusterConf> clusters = new ArrayList<>();
}
