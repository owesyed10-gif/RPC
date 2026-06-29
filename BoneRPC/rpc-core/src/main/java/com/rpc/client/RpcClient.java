package com.rpc.client;

import com.rpc.codec.RpcDecoder;
import com.rpc.codec.RpcEncoder;
import com.rpc.common.RpcMessage;
import com.rpc.common.RpcRequest;
import com.rpc.common.RpcResponse;
import com.rpc.heartbeat.HeartbeatHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RPC客户端：管理TCP连接池，发送请求，配对响应
 * <p>
 * 核心机制：
 * 1. 连接缓存：key=host:port, value=Channel，按需创建TCP连接并复用
 * 2. 请求-响应配对：使用ConcurrentHashMap(requestId -> CompletableFuture)实现异步响应匹配
 * 3. 超时控制：send()方法带超时参数，超时抛出TimeoutException
 * 4. 心跳保活：IdleStateHandler 10s写空闲发送心跳，60s读空闲断开重连
 * @author Syed
 */
public class RpcClient {

    private final EventLoopGroup group = new NioEventLoopGroup();
    /** 连接缓存：host:port -> Channel */
    private final Map<String, Channel> channelCache = new ConcurrentHashMap<>();
    /** 等待响应的请求：requestId -> CompletableFuture */
    private final Map<Integer, CompletableFuture<RpcResponse>> pendingRequests = new ConcurrentHashMap<>();
    /** 请求ID生成器 */
    private final AtomicInteger requestIdGenerator = new AtomicInteger(0);
    /** 默认超时时间(毫秒) */
    private long defaultTimeout = 5000L;

    /** 获取或建立到指定地址的连接 */
    public synchronized Channel getChannel(String host, int port) throws InterruptedException {
        String key = host + ":" + port;
        Channel channel = channelCache.get(key);
        if (channel == null || !channel.isActive()) {
            channel = connect(host, port);
            channelCache.put(key, channel);
        }
        return channel;
    }

    /** 建立Netty TCP连接 */
    private Channel connect(String host, int port) throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 心跳：10s写空闲发PING, 60s读空闲判定服务端失联
                        pipeline.addLast(new IdleStateHandler(60, 10, 0, TimeUnit.SECONDS));
                        pipeline.addLast(new HeartbeatHandler());
                        // 自定义协议编解码
                        pipeline.addLast(new RpcEncoder());
                        pipeline.addLast(new RpcDecoder());
                        // 响应处理
                        pipeline.addLast(new ClientResponseHandler());
                    }
                });
        return bootstrap.connect(host, port).sync().channel();
    }

    /** 发送RPC请求并同步等待响应 */
    public RpcResponse send(RpcRequest request, String host, int port, long timeout) throws Exception {
        Channel channel = getChannel(host, port);
        int requestId = requestIdGenerator.incrementAndGet();

        // 构建协议消息
        RpcMessage msg = new RpcMessage();
        msg.setSerializeType(RpcMessage.SERIALIZE_HESSIAN);
        msg.setMessageType(RpcMessage.TYPE_REQUEST);
        msg.setRequestId(requestId);
        msg.setData(request);

        // 注册Future等待响应
        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        try {
            channel.writeAndFlush(msg);
            return future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            RpcResponse timeoutResp = new RpcResponse();
            timeoutResp.setErrorMessage("Request timeout after " + timeout + "ms");
            return timeoutResp;
        } finally {
            pendingRequests.remove(requestId);
        }
    }

    /** 设置默认超时 */
    public void setTimeout(long timeout) { this.defaultTimeout = timeout; }
    public long getTimeout() { return defaultTimeout; }

    /** 关闭所有连接 */
    public void close() {
        channelCache.values().forEach(Channel::close);
        channelCache.clear();
        group.shutdownGracefully();
    }

    /**
     * 客户端响应处理器
     * 收到服务端响应后，取出对应的CompletableFuture并完成
     */
    private class ClientResponseHandler extends SimpleChannelInboundHandler<RpcMessage> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) {
            // 跳过心跳响应
            if (msg.getMessageType() == RpcMessage.TYPE_HEARTBEAT) return;

            CompletableFuture<RpcResponse> future = pendingRequests.remove(msg.getRequestId());
            if (future != null) {
                future.complete((RpcResponse) msg.getData());
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            // 连接断开时，让所有等待中的请求返回异常
            pendingRequests.forEach((id, f) ->
                    f.completeExceptionally(new RuntimeException("Connection closed")));
            pendingRequests.clear();
        }
    }
}
