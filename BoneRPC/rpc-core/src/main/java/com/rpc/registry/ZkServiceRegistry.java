package com.rpc.registry;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

/**
 * 基于Zookeeper的服务注册实现
 * <p>
 * 核心设计：
 * - ZK路径结构：/bonerpc/{serviceName}/{host:port}
 * - 使用 EPHEMERAL(临时) 节点：服务提供者宕机或断连时ZK自动删除节点
 * - Curator客户端管理ZK连接和重试策略
 * @author Syed
 */
public class ZkServiceRegistry implements ServiceRegistry {

    /** ZK根路径 */
    private static final String ROOT_PATH = "/bonerpc";
    private final CuratorFramework client;

    public ZkServiceRegistry(String zkAddress) {
        client = CuratorFrameworkFactory.builder()
                .connectString(zkAddress)
                .sessionTimeoutMs(30000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start();
        System.out.println("ZK Registry connected: " + zkAddress);
    }

    @Override
    public void register(String serviceName, ServiceInstance instance) throws Exception {
        String path = ROOT_PATH + "/" + serviceName + "/" + instance.getHost() + ":" + instance.getPort();
        // creatingParentsIfNeeded: 自动创建父节点 /bonerpc/{serviceName}
        // withMode(EPHEMERAL): 创建临时节点，session断开后自动删除
        client.create().creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL)
                .forPath(path);
        System.out.println("Service registered: " + path);
    }

    @Override
    public void unregister(String serviceName, ServiceInstance instance) throws Exception {
        String path = ROOT_PATH + "/" + serviceName + "/" + instance.getHost() + ":" + instance.getPort();
        client.delete().quietly().forPath(path);
    }

    @Override
    public void close() {
        client.close();
    }
}
