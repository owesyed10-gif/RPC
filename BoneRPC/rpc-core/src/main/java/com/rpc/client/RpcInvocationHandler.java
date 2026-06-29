package com.rpc.client;

import com.rpc.common.RpcRequest;
import com.rpc.common.RpcResponse;
import com.rpc.loadbalance.LoadBalancer;
import com.rpc.registry.ServiceDiscovery;
import com.rpc.registry.ServiceInstance;
import com.rpc.retry.FailoverRetryPolicy;
import com.rpc.retry.RetryPolicy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

/**
 * JDK动态代理InvocationHandler
 * <p>
 * 两种工作模式：
 * 1. 直连模式：new RpcInvocationHandler(client, serviceName, host, port)
 *    硬编码目标地址，适合测试和单机场景
 * 2. 注册中心模式：new RpcInvocationHandler(client, serviceName, discovery, loadBalancer)
 *    从ZK拉取服务列表，通过负载均衡选择目标节点
 * <p>
 * 内置 Failover 容错机制：失败自动重试，策略可通过 setRetryPolicy 替换
 * @author Syed
 */
public class RpcInvocationHandler implements InvocationHandler {

    private final RpcClient client;
    private final String serviceName;

    // 直连模式：固定目标地址
    private String directHost;
    private int directPort;

    // 注册中心模式：动态服务发现
    private ServiceDiscovery discovery;
    private LoadBalancer loadBalancer;

    /** 重试策略，默认Failover 2次重试 */
    private RetryPolicy retryPolicy = new FailoverRetryPolicy(2);

    // ==================== 构造器 ====================

    /** 直连模式 */
    public RpcInvocationHandler(RpcClient client, String serviceName, String host, int port) {
        this.client = client;
        this.serviceName = serviceName;
        this.directHost = host;
        this.directPort = port;
    }

    /** 注册中心模式 */
    public RpcInvocationHandler(RpcClient client, String serviceName,
                                 ServiceDiscovery discovery, LoadBalancer loadBalancer) {
        this.client = client;
        this.serviceName = serviceName;
        this.discovery = discovery;
        this.loadBalancer = loadBalancer;
    }

    public void setRetryPolicy(RetryPolicy retryPolicy) { this.retryPolicy = retryPolicy; }
    public void setRetryCount(int retryCount) { this.retryPolicy = new FailoverRetryPolicy(retryCount); }

    // ==================== 核心调用逻辑 ====================

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Object基类方法（toString/hashCode/equals）直接本地处理
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        // 构建RPC请求
        RpcRequest request = new RpcRequest();
        request.setServiceName(serviceName);
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);

        // 发送请求
        RpcResponse response = doInvoke(request);

        // 检查结果
        if (!response.isSuccess()) {
            throw new RuntimeException("RPC call failed: " + response.getErrorMessage());
        }
        return response.getResult();
    }

    /** 根据模式选择目标地址并发起调用 */
    private RpcResponse doInvoke(RpcRequest request) throws Exception {
        if (directHost != null) {
            // === 直连模式 ===
            return invokeWithRetry(request, directHost, directPort);
        } else {
            // === 注册中心模式 ===
            List<ServiceInstance> instances = discovery.discover(serviceName);
            if (instances.isEmpty()) {
                RpcResponse resp = new RpcResponse();
                resp.setErrorMessage("No available provider for: " + serviceName);
                return resp;
            }
            ServiceInstance target = loadBalancer.select(instances);
            return invokeWithRetry(request, target.getHost(), target.getPort());
        }
    }

    /** 带Failover重试的调用 */
    private RpcResponse invokeWithRetry(RpcRequest request, String host, int port) throws Exception {
        int attempt = 0;
        while (true) {
            try {
                return client.send(request, host, port, client.getTimeout());
            } catch (Exception e) {
                if (!retryPolicy.shouldRetry(attempt, e)) throw e;
                System.err.println("Retry attempt " + (attempt + 1) + " for " + host + ":" + port);
                attempt++;
            }
        }
    }
}
