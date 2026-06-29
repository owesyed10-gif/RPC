package com.rpc.loadbalance;

import com.rpc.registry.ServiceInstance;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡：按顺序依次分配请求到各实例
 * @author Syed
 */
public class RoundRobinLoadBalancer implements LoadBalancer {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public ServiceInstance select(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) return null;
        // 防止AtomicInteger溢出导致负数索引
        int idx = counter.getAndUpdate(i -> (i == Integer.MAX_VALUE) ? 0 : i + 1) % instances.size();
        return instances.get(idx);
    }
}
