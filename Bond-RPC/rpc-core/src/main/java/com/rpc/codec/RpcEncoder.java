package com.rpc.codec;

import com.rpc.common.RpcMessage;
import com.rpc.serialize.Serializer;
import com.rpc.serialize.SerializerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * RPC编码器：RpcMessage -> 字节流(ByteBuf)
 * 严格按私有协议格式写入：2B魔数 + 1B序列化类型 + 1B消息类型 + 4B请求ID + 4B体长 + 变长消息体
 * 心跳消息体长为0
 * @author Syed
 */
public class RpcEncoder extends MessageToByteEncoder<RpcMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage msg, ByteBuf out) throws Exception {
        // 1. 写魔数 0x1999
        out.writeShort(RpcMessage.MAGIC);
        // 2. 写序列化类型
        out.writeByte(msg.getSerializeType());
        // 3. 写消息类型
        out.writeByte(msg.getMessageType());
        // 4. 写全局请求ID
        out.writeInt(msg.getRequestId());

        // 5. 序列化消息体
        byte[] body;
        if (msg.getMessageType() == RpcMessage.TYPE_HEARTBEAT) {
            body = new byte[0]; // 心跳无消息体
        } else {
            Serializer serializer = SerializerFactory.get(msg.getSerializeType());
            body = serializer.serialize(msg.getData());
        }

        // 6. 写消息体长度 + 消息体内容
        out.writeInt(body.length);
        if (body.length > 0) {
            out.writeBytes(body);
        }
    }
}
