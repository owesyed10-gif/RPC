package com.rpc.registry;

/**
 * 服务注册接口：服务提供者暴露服务时调用
 * @author Syed
 */
public interface ServiceRegistry {
    /** 注册服务实例到注册中心 */
    void register(String serviceName, ServiceInstance instance) throws Exception;
    /** 从注册中心移除服务实例 */
    void unregister(String serviceName, ServiceInstance instance) throws Exception;
    /** 关闭连接 */
    void close();
}
