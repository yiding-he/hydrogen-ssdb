package com.hyd.ssdb.springboot;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(
    HydrogenSsdbConfiguration.class
)
public class HydrogenSsdbConfigurator {

}
