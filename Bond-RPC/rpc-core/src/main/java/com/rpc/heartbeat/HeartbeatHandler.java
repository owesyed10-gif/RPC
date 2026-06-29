package com.rpc.heartbeat;

import com.rpc.common.RpcMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * 心跳事件处理器
 * <p>
 * 配合 IdleStateHandler 使用：
 * - READER_IDLE 读空闲超时：对方长时间无数据，视为连接失效，主动关闭
 * - WRITER_IDLE 写空闲超时：自己长时间未发送数据，主动发送PING心跳保活
 * <p>
 * 服务端使用 READER_IDLE 检测死连接；客户端使用 WRITER_IDLE 发心跳，READER_IDLE 检测服务端失联
 * @author Syed
 */
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                System.out.println("Channel idle timeout, closing: " + ctx.channel().remoteAddress());
                ctx.close();
            } else if (event.state() == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush(RpcMessage.heartbeat());
            }
        }
    }
}
