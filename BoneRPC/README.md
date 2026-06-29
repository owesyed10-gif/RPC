# Simple-RPC

> Author: [Syed](https://github.com/Syed)

轻量级 Java RPC 框架，约 1100 行代码覆盖通信、序列化、注册发现、负载均衡、容错全链路。

---

## 特性

- **自定义二进制协议**：12 字节固定头，魔数校验 + 长度字段解决 TCP 粘包拆包
- **双序列化支持**：JDK 原生 / Hessian2，抽象接口运行时切换
- **服务注册与发现**：基于 ZooKeeper 临时节点 + PathChildrenCache 动态监听
- **多种负载均衡**：内置 Random、RoundRobin 策略，可扩展
- **Failover 容错**：调用失败自动重试
- **连接心跳保活**：Netty IdleStateHandler 检测读写空闲，自动断连恢复
- **零 Spring 依赖**：纯 Java 8 + Netty + ZooKeeper，理解底层原理的首选

---

## 架构

```
┌─────────────────────────────────────────────────┐
│                  Consumer                        │
│  ┌───────────────────────────────────────────┐  │
│  │ JDK Proxy (RpcInvocationHandler)          │  │
│  │  ├─ 直连模式: host:port                   │  │
│  │  └─ 注册中心模式: discovery → LB → host   │  │
│  └──────────────┬────────────────────────────┘  │
│                 │                                │
│  ┌──────────────▼────────────────────────────┐  │
│  │ RpcClient                                 │  │
│  │  ├─ Channel 缓存 (连接复用)               │  │
│  │  ├─ requestId → CompletableFuture (配对)  │  │
│  │  └─ 超时控制                              │  │
│  └──────────────┬────────────────────────────┘  │
└─────────────────┼───────────────────────────────┘
                  │  TCP 长连接
┌─────────────────┼───────────────────────────────┐
│  ┌──────────────▼────────────────────────────┐  │
│  │ RpcEncoder / RpcDecoder                   │  │
│  │  ┌────────────────────────────────────┐   │  │
│  │  │ 私有二进制协议 (12B Header)         │   │  │
│  │  │ 2B魔数│1B序列化│1B消息类型│4B请求ID  │   │  │
│  │  │          │4B体长│变长Body│           │   │  │
│  │  └────────────────────────────────────┘   │  │
│  └──────────────┬────────────────────────────┘  │
│                 │                                │
│  ┌──────────────▼────────────────────────────┐  │
│  │ RpcServerHandler                          │  │
│  │  ├─ serviceMap 查找 → 反射调用            │  │
│  │  └─ 线程池异步处理                        │  │
│  └───────────────────────────────────────────┘  │
│                  Provider                        │
└─────────────────────────────────────────────────┘

      ┌──────────┐
      │ZooKeeper │  ← 服务注册(临时节点) + 发现(PathChildrenCache)
      └──────────┘
```

### 协议格式

```
 Offset  0        2        3        4        8        12
       ┌────────┬────────┬────────┬────────┬────────┬─────────┐
       │ 2B 魔数│ 1B序列 │ 1B消息 │ 4B请求  │ 4B消息体│ 变长消息体│
       │ 0x1999 │ 化类型 │ 类型   │  ID    │ 长度    │  body   │
       └────────┴────────┴────────┴────────┴────────┴─────────┘
```

| 字段 | 长度 | 说明 |
|------|------|------|
| magic | 2B | 魔数 `0x1999`，识别合法协议包 |
| serializeType | 1B | `1`=JDK, `2`=Hessian2 |
| messageType | 1B | `0`=Request, `1`=Response, `2`=Heartbeat |
| requestId | 4B | 全局递增，请求-响应配对 |
| bodyLength | 4B | 消息体长度，解决粘包拆包 |
| body | 变长 | 序列化后的 RpcRequest / RpcResponse |

---

## 快速开始

### 环境要求

- JDK 8+
- Maven 3.6+
- ZooKeeper 3.5+（仅注册中心模式需要）

### 编译

```bash
cd simple-rpc
mvn clean install -DskipTests
```

### 直连模式（无需 ZooKeeper）

**终端 1 — 启动服务端：**

```bash
cd rpc-provider-demo
mvn exec:java -Dexec.mainClass="com.rpc.demo.ProviderApp"
```

**终端 2 — 启动客户端：**

```bash
cd rpc-consumer-demo
mvn exec:java -Dexec.mainClass="com.rpc.demo.ConsumerApp" -Dexec.args="direct 127.0.0.1 8080"
```

输出示例：

```
Calling sayHello...
Result: Hello, World! [from RPC server @ 1719667200000]
Done.
```

### 注册中心模式

先启动 ZooKeeper（默认 `127.0.0.1:2181`）。

**终端 1 — 启动服务端并注册：**

```bash
cd rpc-provider-demo
mvn exec:java -Dexec.mainClass="com.rpc.demo.ProviderApp" -Dexec.args="zk 127.0.0.1:2181"
```

**终端 2 — 启动客户端（从 ZK 发现服务）：**

```bash
cd rpc-consumer-demo
mvn exec:java -Dexec.mainClass="com.rpc.demo.ConsumerApp" -Dexec.args="zk 127.0.0.1:2181"
```

**终端 3 — 再启动一个 Provider（测试负载均衡）：**

修改 `ProviderApp.java` 的 `port` 为 `8081`，重新执行 Provider 启动命令。

---

## 项目结构

```
simple-rpc/
├── pom.xml
├── rpc-core/                        # 框架核心
│   └── src/main/java/com/rpc/
│       ├── common/                  # RpcMessage, RpcRequest, RpcResponse
│       ├── codec/                   # RpcEncoder, RpcDecoder
│       ├── serialize/               # Serializer 接口 + JDK/Hessian2 实现
│       ├── server/                  # RpcServer, RpcServerHandler
│       ├── client/                  # RpcClient, RpcInvocationHandler
│       ├── registry/                # ZkServiceRegistry, ZkServiceDiscovery
│       ├── loadbalance/             # Random, RoundRobin
│       ├── retry/                   # FailoverRetryPolicy
│       └── heartbeat/               # HeartbeatHandler
├── rpc-provider-demo/               # 服务提供者示例
│   └── ProviderApp, DemoServiceImpl
└── rpc-consumer-demo/               # 服务消费者示例
    └── ConsumerApp
```

### 代码量

| 模块 | 文件数 | 行数 |
|------|--------|------|
| 核心框架 | 24 | ~950 |
| 提供者示例 | 2 | ~45 |
| 消费者示例 | 1 | ~55 |
| POM 配置 | 4 | ~120 |
| **合计** | **31** | **~1170** |

---

## 使用方式

### 1. 定义服务接口

```java
public interface HelloService {
    String hello(String name);
}
```

### 2. 服务端实现并暴露

```java
public class HelloServiceImpl implements HelloService {
    @Override
    public String hello(String name) {
        return "Hi, " + name;
    }
}

// 启动
RpcServer server = new RpcServer(8080);
server.registerService(new HelloServiceImpl());
server.start();
```

### 3. 直连模式调用

```java
RpcClient client = new RpcClient();
RpcInvocationHandler handler = new RpcInvocationHandler(
        client, "com.example.HelloService", "127.0.0.1", 8080);
HelloService service = (HelloService) Proxy.newProxyInstance(
        HelloService.class.getClassLoader(),
        new Class[]{HelloService.class}, handler);
String result = service.hello("world");
```

### 4. 注册中心模式调用

```java
RpcClient client = new RpcClient();
ZkServiceDiscovery discovery = new ZkServiceDiscovery("127.0.0.1:2181");
RpcInvocationHandler handler = new RpcInvocationHandler(
        client, "com.example.HelloService", discovery, new RandomLoadBalancer());
handler.setRetryCount(2);
HelloService service = (HelloService) Proxy.newProxyInstance(
        HelloService.class.getClassLoader(),
        new Class[]{HelloService.class}, handler);
String result = service.hello("world");
```

### 5. 切换序列化类型

修改 `RpcClient.send()` 中的 `setSerializeType` 或在 `RpcMessage` 构建时指定：

```java
msg.setSerializeType(RpcMessage.SERIALIZE_JDK);  // 切换到JDK序列化
```

---

## 设计决策

### 为什么不用 HTTP 协议？

HTTP 报文头冗长（Cookie、User-Agent 等），文本协议解析性能不如二进制。自定二进制协议 12 字节紧凑头，Hessian2 序列化后体积约为 JSON 的 1/5，TCP 长连接避免了 HTTP keep-alive 的开销。

### 为什么自己实现粘包拆包而不是用 LengthFieldBasedFrameDecoder？

两种方式底层原理一致——都是读取长度字段，判断消息体是否到齐。自己手写 `ByteToMessageDecoder` 的 `markReaderIndex`/`resetReaderIndex` 流程，可以更直接地展示半包处理的完整逻辑，方便阅读者理解 TCP 字节流协议的边界处理本质。

### 为什么是 12 字节固定头？

这是权衡后的设计——头越小越省带宽，但需要包含全部必要字段。12 字节刚好放下魔数、类型、ID、长度四项核心信息，4 字节对齐也符合大多数 CPU 的访问偏好。

---

## 局限性

这是一个**学习项目**，以下场景不建议直接使用：

- **无熔断降级**：没有限流、熔断、隔离机制，级联故障会扩散
- **无监控埋点**：没有 QPS/RT/错误率统计和调用链追踪
- **无线程池管控**：`CachedThreadPool` 在高并发下有 OOM 风险
- **无连接池管理**：Channel 缓存无上限、无健康检查
- **无安全机制**：无认证、鉴权、TLS 加密
- **协议无扩展性**：不支持双向流、服务端推送等高级特性
- **跨语言不支持**：私有协议只有 Java 实现

如需生产级 RPC 框架，推荐 [Apache Dubbo](https://dubbo.apache.org/) 或 [gRPC](https://grpc.io/)。

---

## License

MIT
