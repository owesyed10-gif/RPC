package com.rpc.serialize;

import com.rpc.common.RpcMessage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列化器工厂：集中管理所有序列化实现，根据类型标识返回对应实例
 * 默认使用 Hessian2 (体积小、性能好)
 * @author Syed
 */
public class SerializerFactory {

    private static final Map<Byte, Serializer> serializerMap = new ConcurrentHashMap<>();

    static {
        addSerializer(new JdkSerializer());
        addSerializer(new HessianSerializer());
    }

    /** 注册自定义序列化器 */
    public static void addSerializer(Serializer serializer) {
        serializerMap.put(serializer.getType(), serializer);
    }

    /** 根据类型标识获取序列化器 */
    public static Serializer get(byte type) {
        Serializer serializer = serializerMap.get(type);
        if (serializer == null) {
            throw new IllegalArgumentException("Unknown serialize type: " + type);
        }
        return serializer;
    }

    /** 获取默认序列化器(Hessian2) */
    public static Serializer getDefault() {
        return get(RpcMessage.SERIALIZE_HESSIAN);
    }
}
