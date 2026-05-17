# AGENTS.md

> 本文件为 OpenCode 代理的仓库特定指南。只包含从多个配置文件中推断出的、代理容易忽略的关键上下文。

## 项目概况

- **类型**：单模块 Java 库（JAR），非多模块项目
- **用途**：SSDB 数据库的 Java 客户端，支持连接池、主从集群、一致性哈希负载均衡
- **Java 版本**：1.8（`maven.compiler.source/target`）
- **构建工具**：Maven（`pom.xml`）
- **当前版本**：`1.2.8`

## 核心架构

```
com.hyd.ssdb
├── SsdbClient              # 用户唯一入口，线程安全，一个集群只创建一个实例
├── AbstractClient          # 底层命令实现
├── conf/                   # 配置：Cluster、Server、Sharding、连接池配置
├── conn/                   # 连接池：ConnectionPoolManager、ConnectionPoolFactory（可自定义）
├── protocol/               # SSDB 协议：Request、Response、Block、WriteRequest
├── sharding/               # 负载均衡：ConsistentHashSharding（默认）
├── springboot/             # Spring Boot 自动配置（spring.factories 注册）
└── ui/                     # Swing 调试界面（SsdbClientFrame）
```

**关键设计约束**：`SsdbClient` 是线程安全的，每个集群只应创建一个实例；创建大量实例会导致内存溢出（每个 Server 对应一个连接池）。

## 常用命令

```bash
# 编译
mvn clean compile

# 运行测试（**需要本地 SSDB 服务运行在 127.0.0.1:8888**）
mvn test

# 打包（跳过测试）
mvn clean package -DskipTests

# 发布到 Maven Central（需要 GPG 签名和 Sonatype 账号）
mvn -P maven-release -Dmaven.test.skip=true -DPASS=<gpg-password> clean deploy
```

## 测试要求

- **测试框架**：JUnit 4
- **基类**：`BaseTest` 通过系统属性读取 SSDB 连接信息
  - 默认：`127.0.0.1:8888`
  - 自定义主机：`-Dh=192.168.1.100 -Dp=8888`
  - 自定义密码：`-Dps=mypassword`
  - **多服务器测试**：用逗号分隔，如 `-Dh=host1,host2 -Dp=8888,8889 -Dps=pass1,pass2`
- **注意**：绝大多数测试需要真实 SSDB 实例，没有 Mock；若无 SSDB 服务，测试会失败
- 测试资源中包含二进制文件 `src/test/resources/binary/sample.jpg`，用于二进制数据读写测试

## 代码风格

- **缩进**：4 空格（`.editorconfig`）
- **换行**：LF（`end_of_line = lf`）
- **编码**：UTF-8
- **Git 提交信息**：遵循 `.gitmessage` 的规范
  - 格式：`type(scope): subject`
  - 类型：`feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `revert`
  - subject 不超过 50 字符，body 每行不超过 72 字符

## Spring Boot 自动配置

- 配置前缀：`hydrogen-ssdb`
- 自动配置类：`HydrogenSsdbConfigurator`
- 通过 `META-INF/spring.factories` 注册
- 示例配置见 `src/test/resources/application.properties`

## 依赖与工具链注意事项

- **commons-pool2**：连接池基于 Apache commons-pool2，`ConnectionPoolConfig` 继承 `GenericObjectPoolConfig`
- **Lombok**：项目中使用了 Lombok（如 `@Data`），IDE 或编译环境需要支持
- **Spring Boot**：`spring-boot-dependencies` 仅用于 BOM 管理，作用域多为 `provided`，不影响库用户
- **日志**：使用 SLF4J + Logback（`provided`），测试中使用 `logback.xml` 配置调试级别
