package com.rpc.server;

import com.rpc.codec.RpcDecoder;
import com.rpc.codec.RpcEncoder;
import com.rpc.heartbeat.HeartbeatHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * RPC服务端启动器
 * 基于Netty主从Reactor模型：
 * - bossGroup(1线程)：接收客户端连接
 * - workerGroup(多线程)：处理IO读写和业务分发
 * <p>
 * 服务注册：通过 registerService() 将本地服务实例存入Map，客户端请求时反射调用
 * @author Syed
 */
public class RpcServer {

    private final int port;
    /** 本地服务注册表：serviceName -> 服务实例 */
    private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public RpcServer(int port) {
        this.port = port;
    }

    /** 注册服务：取实现类的第一个接口名作为serviceName */
    public void registerService(Object serviceImpl) {
        Class<?>[] interfaces = serviceImpl.getClass().getInterfaces();
        if (interfaces.length == 0) {
            throw new IllegalArgumentException("Service must implement at least one interface");
        }
        serviceMap.put(interfaces[0].getName(), serviceImpl);
        System.out.println("Registered service: " + interfaces[0].getName());
    }

    /** 启动Netty服务 */
    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 服务端60秒读空闲无数据 -> 触发 READER_IDLE 事件，心跳处理器关闭连接
                        pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
                        pipeline.addLast(new HeartbeatHandler());
                        // 自定义协议编解码
                        pipeline.addLast(new RpcDecoder());
                        pipeline.addLast(new RpcEncoder());
                        // 业务处理
                        pipeline.addLast(new RpcServerHandler(serviceMap));
                    }
                });

        serverChannel = bootstrap.bind(port).sync().channel();
        System.out.println("RPC Server started on port " + port);
    }

    /** 优雅关闭 */
    public void stop() {
        if (serverChannel != null) serverChannel.close();
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }

    public int getPort() { return port; }
}
