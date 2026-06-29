package com.rpc.demo;

import com.rpc.registry.ServiceInstance;
import com.rpc.registry.ZkServiceRegistry;
import com.rpc.server.RpcServer;

/**
 * 服务提供者启动类
 * <p>
 * 两种运行模式：
 * 1. 直连模式（不依赖ZK）：mvn exec:java -pl rpc-provider-demo -Dexec.mainClass=com.rpc.demo.ProviderApp
 * 2. 注册中心模式：java -cp ... ProviderApp zk 127.0.0.1:2181
 * @author Syed
 */
public class ProviderApp {

    public static void main(String[] args) throws Exception {
        int port = 8080;

        // 1. 创建并启动RPC服务端
        RpcServer server = new RpcServer(port);
        server.registerService(new DemoServiceImpl());
        server.start();

        // 2. 判断是否需要注册到ZK
        String mode = args.length > 0 ? args[0] : "direct";
        if ("zk".equals(mode) && args.length > 1) {
            String zkAddr = args[1];
            ZkServiceRegistry registry = new ZkServiceRegistry(zkAddr);
            ServiceInstance instance = new ServiceInstance("127.0.0.1", port);
            registry.register("com.rpc.demo.DemoService", instance);

            // JVM退出时清理
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    registry.close();
                } catch (Exception ignored) {}
                server.stop();
            }));
            System.out.println("Service registered to ZK: " + zkAddr);
        } else {
            System.out.println("Running in direct mode (no registry)");
            System.out.println("Consumer should connect to 127.0.0.1:" + port);
        }

        // 阻塞主线程
        Thread.currentThread().join();
    }
}
