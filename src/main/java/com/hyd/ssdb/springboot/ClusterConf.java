package com.hyd.ssdb.springboot;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ClusterConf {

    private List<ServerConf> servers = new ArrayList<>();
}
