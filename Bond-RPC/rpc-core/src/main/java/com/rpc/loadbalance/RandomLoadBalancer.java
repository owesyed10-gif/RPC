package com.rpc.loadbalance;

import com.rpc.registry.ServiceInstance;
import java.util.List;
import java.util.Random;

/**
 * 随机负载均衡：从可用实例列表中随机选取一个
 * @author Syed
 */
public class RandomLoadBalancer implements LoadBalancer {

    private final Random random = new Random();

    @Override
    public ServiceInstance select(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) return null;
        return instances.get(random.nextInt(instances.size()));
    }
}
