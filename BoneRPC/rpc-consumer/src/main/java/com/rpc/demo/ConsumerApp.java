package com.rpc.demo;

import com.rpc.client.RpcClient;
import com.rpc.client.RpcInvocationHandler;
import com.rpc.loadbalance.LoadBalancer;
import com.rpc.loadbalance.RandomLoadBalancer;
import com.rpc.registry.ServiceDiscovery;
import com.rpc.registry.ZkServiceDiscovery;
import java.lang.reflect.Proxy;

/**
 * 服务消费者启动类
 * <p>
 * 两种运行模式：
 * 1. 直连模式：java -cp ... ConsumerApp direct 127.0.0.1 8080
 *    硬编码目标IP端口，适合本地测试
 * 2. 注册中心模式：java -cp ... ConsumerApp zk 127.0.0.1:2181
 *    从ZK拉取服务列表，通过负载均衡选择节点调用
 * @author Syed
 */
public class ConsumerApp {

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "direct";

        // 1. 创建RPC客户端
        RpcClient client = new RpcClient();
        client.setTimeout(5000); // 5秒超时

        // 2. 根据模式创建代理
        DemoService service;
        String serviceName = "com.rpc.demo.DemoService";

        if ("zk".equals(mode) && args.length > 1) {
            // === 注册中心模式 ===
            String zkAddr = args[1];
            ServiceDiscovery discovery = new ZkServiceDiscovery(zkAddr);
            LoadBalancer loadBalancer = new RandomLoadBalancer();

            RpcInvocationHandler handler = new RpcInvocationHandler(
                    client, serviceName, discovery, loadBalancer);
            handler.setRetryCount(2);

            service = (DemoService) Proxy.newProxyInstance(
                    DemoService.class.getClassLoader(),
                    new Class[]{DemoService.class},
                    handler);
        } else {
            // === 直连模式 ===
            String host = args.length > 1 ? args[1] : "127.0.0.1";
            int port = args.length > 2 ? Integer.parseInt(args[2]) : 8080;

            RpcInvocationHandler handler = new RpcInvocationHandler(
                    client, serviceName, host, port);

            service = (DemoService) Proxy.newProxyInstance(
                    DemoService.class.getClassLoader(),
                    new Class[]{DemoService.class},
                    handler);
        }

        // 3. 发起RPC调用
        System.out.println("Calling sayHello...");
        String result = service.sayHello("World");
        System.out.println("Result: " + result);

        // 4. 关闭客户端
        client.close();
        System.out.println("Done.");
    }
}
