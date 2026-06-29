package com.rpc.server;

import com.rpc.common.RpcMessage;
import com.rpc.common.RpcRequest;
import com.rpc.common.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 服务端业务处理器
 * <p>
 * 处理流程：
 * 1. 心跳请求 -> 直接回复 PONG
 * 2. RPC请求 -> 从serviceMap获取服务实例 -> 反射调用 -> 封装响应写回
 * <p>
 * 使用线程池异步处理业务请求，避免阻塞Netty IO线程
 * @author Syed
 */
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcMessage> {

    private final Map<String, Object> serviceMap;
    /** 业务线程池：默认CachedThreadPool，可根据需要改为自定义线程池 */
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public RpcServerHandler(Map<String, Object> serviceMap) {
        this.serviceMap = serviceMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) {
        // 心跳消息：原样回复心跳即可
        if (msg.getMessageType() == RpcMessage.TYPE_HEARTBEAT) {
            RpcMessage pong = RpcMessage.heartbeat();
            pong.setRequestId(msg.getRequestId());
            ctx.writeAndFlush(pong);
            return;
        }

        // 业务请求：提交到线程池异步处理
        threadPool.submit(() -> {
            RpcRequest request = (RpcRequest) msg.getData();
            RpcResponse response = new RpcResponse();

            try {
                Object service = serviceMap.get(request.getServiceName());
                if (service == null) {
                    response.setErrorMessage("Service not found: " + request.getServiceName());
                } else {
                    // 反射调用目标方法
                    Method method = service.getClass().getMethod(
                            request.getMethodName(), request.getParameterTypes());
                    Object result = method.invoke(service, request.getParameters());
                    response.setResult(result);
                }
            } catch (Exception e) {
                response.setErrorMessage(e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
            }

            // 封装响应消息并写回客户端
            RpcMessage respMsg = new RpcMessage();
            respMsg.setSerializeType(msg.getSerializeType());
            respMsg.setMessageType(RpcMessage.TYPE_RESPONSE);
            respMsg.setRequestId(msg.getRequestId());
            respMsg.setData(response);
            ctx.writeAndFlush(respMsg);
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("Server handler exception: " + cause.getMessage());
        ctx.close();
    }
}
