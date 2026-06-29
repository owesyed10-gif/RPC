package com.rpc.serialize;

/**
 * 序列化器顶层抽象接口
 * 屏蔽底层序列化实现差异，支持运行时动态切换
 * @author Syed
 */
public interface Serializer {

    /** 将对象序列化为字节数组 */
    <T> byte[] serialize(T obj) throws Exception;

    /** 将字节数组反序列化为指定类型对象 */
    <T> T deserialize(byte[] bytes, Class<T> clazz) throws Exception;

    /** 返回序列化类型标识，与RpcMessage中定义的常量对应 */
    byte getType();
}
