package com.rpc.loadbalance;

import com.rpc.registry.ServiceInstance;
import java.util.List;

/**
 * 负载均衡策略接口
 * @author Syed
 */
public interface LoadBalancer {
    /** 从实例列表中选择一个 */
    ServiceInstance select(List<ServiceInstance> instances);
}
