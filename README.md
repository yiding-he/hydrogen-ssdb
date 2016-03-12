# hydrogen-ssdb
Java 编写的 SSDB 客户端

## 介绍

hydrogen-ssdb 是一个 Java 编写的 [SSDB](https://github.com/ideawu/ssdb)  客户端，支持多线程并发请求和多服务器的负载均衡（客户端分发请求）。

SSDB 是一个类似 Redis 的 NOSQL 数据库，兼容 Redis 协议，且支持多线程，内存占用小。

hydrogen-ssdb 是一个 SSDB 客户端，具有以下特性：

1. 易于配置，易于使用；
1. 支持 SSDB 主从集群与负载均衡；
1. 当集群中的服务器 down 掉时，能自动识别并跳过该服务器。

#### 【负载均衡的抽象和实现】

hydrogen-ssdb 将负载均衡抽象为`Sharding`类，该类负责决定一个请求应该发送给哪台 SSDB 服务器。

在`Sharding`中，所有的服务器（`Server`）被归为多个集群（`Cluster`），`Cluster` 是负载均衡的顶层单位。每个 `Cluster` 包含多个 `Server`，一个 `Server` 可以是主服务器，也可以是从服务器（这个必须与 SSDB 实际的主从配置严格一致），但 `Cluster` 中必须要有主服务器。

hydrogen-ssdb 缺省实现了基于一致性哈希环的负载均衡方式。如果这种方式不适合您的实际情况，您可以自己实现`Sharding`的子类，然后通过`SsdbClient`的构造方法传入。下面是一个如何使用自定义`Sharding`的例子，假设你已经实现了自定义的`MySharding`类：

```java
Cluster cluster = new Cluster(server);
MySharding mySharding = new MySharding(cluster);  // 自定义 Sharding
SsdbClient client = new SsdbClient(mySharding);
```

下面介绍 hydrogen-ssdb 缺省实现的负载均衡的原理。

#### 【基于一致性哈希环的负载均衡】

![](https://cloud.githubusercontent.com/assets/900606/11584478/2c30724c-9a9f-11e5-8fa2-3917230a227b.png)

#### 【对单点故障的处理】

在负载均衡当中，每个节点都负责整个一致性哈希环中的一部分（称为哈希段）。当负载均衡当中出现单点故障时，故障节点对应的哈希段将无法执行存取操作，因此有两种处理方式：

1. 故障节点前面的节点自动接管该哈希段。这种方式适用于将 SSDB 用于缓存，因为缓存丢失是可以重新填充的；
2. 保留哈希段的故障状态，直到故障节点恢复。这种方式适用于将 SSDB 用于数据库，这样能严格保证一个 key 会保存在对应的节点中。

hydrogen-ssdb 缺省情况下使用第一种方式来处理。如果需要修改，可以以下面的方式：

```java
ConsistentHashSharding sharding = (ConsistentHashSharding)ssdbClient.getSharding();
sharding.setSpofStrategy(SPOFStrategy.PreserveKeySpaceStrategy);
```

#### 【如何添加 Cluster】

对于一致性哈希环，每一个 Cluster 的哈希段都是固定的，所以每添加一个新的 Cluster，都只会给当前的其中 1 个 Cluster 减负，而不是给所有的 Cluster 减负。例如当前有 A、B、C 三个 Cluster，那么当添加一个 D 到 A 和 B 之间，形成 “A-D-B-C” 时，它只会分担 A 的一部分哈希段，B 和 C 的哈希段没有改变，也就是说 B 和 C 的负载没有变化。

由此可知，在添加 Cluster 之前，你需要明确的了解每个 Cluster 当前的负载情况，找到负载最重的 Cluster，将新的 Cluster 加在它后面。

所以，`ConsistentHashSharding` 的 `addCluster()` 方法有两个参数，第一个是要添加的 Cluster，第二个是需要被分担负载的 Cluster。

##项目依赖

hydrogen-ssdb 依赖于下面两个框架：

* Apache commons-pool2 （对象池框架）
* slf4j （日志框架）

##使用方法

#### 基本使用方法

```java
SsdbClient client = new SsdbClient(host, port);
client.set("key", "value");
System.out.println(client.get("key"));   // output "value"
client.close();    // 应用停止时需要调用 close() 方法，也可以配置在 Spring 的 destroy-method 中
```

#### 配置主从服务器
```java
List<Server> servers = Arrays.asList(
        new Server("192.168.1.180", 8888, null, true),  // 主服务器
        new Server("192.168.1.180", 8889, null, false)  // 从服务器
);

SsdbClient client = new SsdbClient(Sharding.fromServerList(servers));
client.set("name", "hydrogen-ssdb");    // 写入请求一定会发送给主服务器
System.out.println(client.get("name")); // 读取请求会随机发送给任意一台服务器
```

#### 配置负载均衡

```java
Sharding sharding = new ConsistentHashSharding(Arrays.asList(
        new Cluster(new Server("192.168.1.180", 8888), 100),  // 100 和 200 这两个参数指的是权重，
        new Cluster(new Server("192.168.1.180", 8889), 200)   // 权重越大的 Cluster 所保存的 key 越多。
));

SsdbClient ssdbClient = new SsdbClient(sharding);

```

#### Spring XML 配置

```XML
<!-- 单个 SSDB 服务器的配置，其他几个类似的构造方法在此略去 -->
<bean id="singleServerSsdbClient" class="com.hyd.ssdb.SsdbClient" destroy-method="close">
    <constructor-arg name="host" value="192.168.1.180"/>
    <constructor-arg name="port" value="8888"/>
</bean>

<!-- 自定义 Sharding -->
<bean id="custShardingClient" class="com.hyd.ssdb.SsdbClient" destroy-method="close">
    <constructor-arg name="sharding">
        <bean class="com.hyd.ssdb.AjiaSharding"/>
    </constructor-arg>
</bean>

<!-- 多台 SSDB 主从服务器的配置 -->
<bean id="ssdbServer1" class="com.hyd.ssdb.conf.Server">
    <property name="host" value="192.168.1.180"/>
    <property name="port" value="8888"/>
    <property name="master" value="true"/>
</bean>
<bean id="ssdbServer2" class="com.hyd.ssdb.conf.Server">
    <property name="host" value="192.168.1.180"/>
    <property name="port" value="8889"/>
    <property name="master" value="false"/>
</bean>
<bean id="singleClusterSsdbClient" class="com.hyd.ssdb.SsdbClient"
      factory-method="fromSingleCluster" destroy-method="close">
    <constructor-arg name="servers">
        <list value-type="com.hyd.ssdb.conf.Server">
            <ref bean="ssdbServer1"/>
            <ref bean="ssdbServer2"/>
        </list>
    </constructor-arg>
</bean>

<!-- 多台 SSDB 负载均衡的配置（每个 Cluster 一台服务器） -->
<bean id="ssdbCluster1" class="com.hyd.ssdb.conf.Cluster" factory-method="fromSingleServer">
    <constructor-arg name="server" ref="ssdbServer1"/>
</bean>
<bean id="ssdbCluster2" class="com.hyd.ssdb.conf.Cluster" factory-method="fromSingleServer">
    <constructor-arg name="server" ref="ssdbServer2"/>
</bean>
<bean id="shardingSsdbClient" class="com.hyd.ssdb.SsdbClient"
      factory-method="fromClusters" destroy-method="close">
    <constructor-arg name="clusters">
        <list value-type="com.hyd.ssdb.conf.Cluster">
            <ref bean="ssdbCluster1"/>
            <ref bean="ssdbCluster2"/>
        </list>
    </constructor-arg>
</bean>
```

### 使用注意

#### 线程安全

`SsdbClient` 对象包含了对整个负载均衡的拓扑结构的处理，所以对于每一个由多个 SSDB 服务器组成的负载均衡架构，只需创建一个 `SsdbClient` 对象即可。另外 `SsdbClient` 是线程安全的，所以可以让任意多个线程访问。

#### 误用导致内存占用过高

因为一个 `SsdbClient` 对象可能包含一个或多个连接池（每个连接池对应一个 SSDB 服务器），因此请不要创建大量的 `SsdbClient` 对象，这样完全没有必要，也会使得内存很容易被用光。





