package com.rpc.registry;

import java.util.List;

/**
 * 服务发现接口：消费者获取可用服务列表
 * @author Syed
 */
public interface ServiceDiscovery {
    /** 根据服务名获取所有可用实例 */
    List<ServiceInstance> discover(String serviceName) throws Exception;
    /** 关闭连接 */
    void close();
}
