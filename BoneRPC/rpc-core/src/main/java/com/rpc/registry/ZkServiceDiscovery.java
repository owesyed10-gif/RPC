package com.rpc.registry;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于Zookeeper的服务发现实现
 * <p>
 * 核心设计：
 * - 首次发现：读取 /bond-rpc/{serviceName} 下所有子节点构建实例列表
 * - 动态监听：使用 PathChildrenCache 监听子节点增删，实时刷新本地缓存
 * - 本地缓存：每个 serviceName 独立维护一份地址列表，线程安全
 * @author Syed
 */
public class ZkServiceDiscovery implements ServiceDiscovery {

    private static final String ROOT_PATH = "/bond-rpc";
    private final CuratorFramework client;
    /** 按服务名隔离的本地地址缓存：serviceName -> 实例列表 */
    private final Map<String, List<ServiceInstance>> instanceCache = new ConcurrentHashMap<>();
    /** 已注册的Watcher标记，避免重复注册 */
    private final Map<String, PathChildrenCache> watcherCache = new ConcurrentHashMap<>();

    public ZkServiceDiscovery(String zkAddress) {
        client = CuratorFrameworkFactory.builder()
                .connectString(zkAddress)
                .sessionTimeoutMs(30000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start();
    }

    @Override
    public List<ServiceInstance> discover(String serviceName) throws Exception {
        List<ServiceInstance> instances = instanceCache.get(serviceName);
        if (instances == null || instances.isEmpty()) {
            // 首次发现：拉取节点并启动Watcher
            instances = refreshInstances(serviceName);
            instanceCache.put(serviceName, instances);
            watchService(serviceName);
        }
        // 返回副本，避免外部修改影响缓存
        return new ArrayList<>(instances);
    }

    /** 从ZK拉取最新的服务实例列表 */
    private List<ServiceInstance> refreshInstances(String serviceName) throws Exception {
        List<ServiceInstance> instances = new ArrayList<>();
        String path = ROOT_PATH + "/" + serviceName;
        if (client.checkExists().forPath(path) == null) return instances;

        List<String> children = client.getChildren().forPath(path);
        for (String child : children) {
            String[] parts = child.split(":");
            if (parts.length == 2) {
                instances.add(new ServiceInstance(parts[0], Integer.parseInt(parts[1])));
            }
        }
        System.out.println("Discovered " + instances.size() + " providers for: " + serviceName);
        return instances;
    }

    /** 注册Watcher监听服务节点变更，自动刷新本地缓存 */
    private void watchService(String serviceName) throws Exception {
        // 避免重复注册Watcher
        if (watcherCache.containsKey(serviceName)) return;

        String path = ROOT_PATH + "/" + serviceName;
        PathChildrenCache cache = new PathChildrenCache(client, path, true);
        cache.getListenable().addListener((curator, event) -> {
            if (event.getType() == PathChildrenCacheEvent.Type.CHILD_ADDED
                    || event.getType() == PathChildrenCacheEvent.Type.CHILD_REMOVED
                    || event.getType() == PathChildrenCacheEvent.Type.CHILD_UPDATED) {
                System.out.println("Service node changed: " + event.getType());
                try {
                    List<ServiceInstance> newInstances = refreshInstances(serviceName);
                    instanceCache.put(serviceName, newInstances);
                } catch (Exception ignored) {}
            }
        });
        cache.start();
        watcherCache.put(serviceName, cache);
    }

    @Override
    public void close() {
        watcherCache.values().forEach(PathChildrenCache::close);
        watcherCache.clear();
        client.close();
    }
}
