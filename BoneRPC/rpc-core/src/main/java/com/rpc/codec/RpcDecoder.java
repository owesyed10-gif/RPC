package com.rpc.codec;

import com.rpc.common.RpcMessage;
import com.rpc.common.RpcRequest;
import com.rpc.common.RpcResponse;
import com.rpc.serialize.Serializer;
import com.rpc.serialize.SerializerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

/**
 * RPC解码器：字节流(ByteBuf) -> RpcMessage
 * <p>
 * TCP粘包/拆包解决策略：
 * 1. 先读固定12字节Header，从中获取 bodyLength
 * 2. 判断可读字节数是否 >= bodyLength，不够则 resetReaderIndex 等待后续数据到达
 * 3. 够则读取完整消息体并反序列化
 * <p>
 * 非法包处理：魔数校验不通过直接关闭连接
 * @author Syed
 */
public class RpcDecoder extends ByteToMessageDecoder {

    /** 私有协议固定头部长度：12字节 */
    private static final int HEADER_LENGTH = 12;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 至少需要12字节头部
        if (in.readableBytes() < HEADER_LENGTH) {
            return;
        }

        // 标记当前读指针位置（出问题时回退用）
        in.markReaderIndex();

        // 校验魔数
        short magic = in.readShort();
        if (magic != RpcMessage.MAGIC) {
            System.err.println("Invalid magic number: 0x" + Integer.toHexString(magic & 0xFFFF));
            ctx.close(); // 非法包，断开连接
            return;
        }

        byte serializeType = in.readByte();
        byte messageType = in.readByte();
        int requestId = in.readInt();
        int bodyLength = in.readInt();

        // 消息体未完全到达，回退读指针等待后续数据（解决半包）
        if (in.readableBytes() < bodyLength) {
            in.resetReaderIndex();
            return;
        }

        // 读取消息体字节
        byte[] body = new byte[bodyLength];
        if (bodyLength > 0) {
            in.readBytes(body);
        }

        // 构建 RpcMessage
        RpcMessage msg = new RpcMessage();
        msg.setSerializeType(serializeType);
        msg.setMessageType(messageType);
        msg.setRequestId(requestId);

        // 非心跳消息且消息体非空 -> 反序列化
        if (messageType != RpcMessage.TYPE_HEARTBEAT && bodyLength > 0) {
            Serializer serializer = SerializerFactory.get(serializeType);
            Class<?> clazz = (messageType == RpcMessage.TYPE_REQUEST)
                    ? RpcRequest.class : RpcResponse.class;
            msg.setData(serializer.deserialize(body, clazz));
        }

        out.add(msg);
    }
}
