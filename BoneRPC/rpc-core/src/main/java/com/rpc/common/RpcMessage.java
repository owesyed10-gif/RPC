package com.rpc.common;

/**
 * RPC协议消息体，承载自定义私有二进制协议的 Header + Body
 * <pre>
 * 协议头格式（固定12字节）:
 * ┌──────────────┬──────────────┬──────────────┬──────────────┐
 * │  2B 魔数     │  1B 序列化   │  1B 消息类型  │  4B 请求ID   │
 * │  0x1999      │  类型        │              │              │
 * ├──────────────┴──────────────┴──────────────┴──────────────┤
 * │  4B 消息体长度(bodyLength)                                │
 * ├───────────────────────────────────────────────────────────┤
 * │  消息体(body) 变长，长度 = bodyLength                      │
 * └───────────────────────────────────────────────────────────┘
 * </pre>
 * @author Syed
 */
public class RpcMessage {

    /** 魔数：0x1999，用于快速识别合法协议包，过滤脏数据 */
    public static final short MAGIC = (short) 0x1999;

    /** 消息类型：Request */
    public static final byte TYPE_REQUEST = 0;
    /** 消息类型：Response */
    public static final byte TYPE_RESPONSE = 1;
    /** 消息类型：心跳 */
    public static final byte TYPE_HEARTBEAT = 2;

    /** 序列化类型：JDK原生 */
    public static final byte SERIALIZE_JDK = 1;
    /** 序列化类型：Hessian2 */
    public static final byte SERIALIZE_HESSIAN = 2;

    /** 序列化类型标识 */
    private byte serializeType = SERIALIZE_HESSIAN;
    /** 消息类型标识 */
    private byte messageType;
    /** 全局请求ID，用于请求-响应配对 */
    private int requestId;
    /** 消息体数据：RpcRequest / RpcResponse / null(心跳) */
    private Object data;

    /** 构建心跳消息 */
    public static RpcMessage heartbeat() {
        RpcMessage msg = new RpcMessage();
        msg.messageType = TYPE_HEARTBEAT;
        return msg;
    }

    public byte getSerializeType() { return serializeType; }
    public void setSerializeType(byte serializeType) { this.serializeType = serializeType; }

    public byte getMessageType() { return messageType; }
    public void setMessageType(byte messageType) { this.messageType = messageType; }

    public int getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}
