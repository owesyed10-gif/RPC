package com.rpc.serialize;

import com.rpc.common.RpcMessage;
import java.io.*;

/**
 * JDK原生序列化实现
 * 优点：JDK内置，无需额外依赖，自动处理对象图引用
 * 缺点：序列化后体积大，性能一般，要求类实现Serializable
 * @author Syed
 */
public class JdkSerializer implements Serializer {

    @Override
    public byte getType() { return RpcMessage.SERIALIZE_JDK; }

    @Override
    public <T> byte[] serialize(T obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        oos.close();
        return bos.toByteArray();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bis);
        return (T) ois.readObject();
    }
}
