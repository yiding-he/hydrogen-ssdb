package com.hyd.ssdb.springboot;

import com.hyd.ssdb.conf.Server;
import com.hyd.ssdb.conf.SocketConfig;
import lombok.Data;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Data
public class ServerConf {

    private String host;

    private int port;

    private boolean master = true;

    private String pass;

    @NestedConfigurationProperty
    private GenericObjectPoolConfig pool = Server.createDefaultPoolConfig();     // 连接池配置参数

    @NestedConfigurationProperty
    private SocketConfig socket = new SocketConfig();     // 网络配置参数

}
