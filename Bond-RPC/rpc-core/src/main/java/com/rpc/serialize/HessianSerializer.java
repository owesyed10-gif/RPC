package com.rpc.serialize;

import com.rpc.common.RpcMessage;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import java.io.*;

/**
 * Hessian2二进制序列化实现
 * 优点：跨语言、序列化体积小(约JDK的1/3)、性能优于JDK
 * 缺点：对复杂对象图支持不如JDK，循环引用需特殊处理
 * @author Syed
 */
public class HessianSerializer implements Serializer {

    @Override
    public byte getType() { return RpcMessage.SERIALIZE_HESSIAN; }

    @Override
    public <T> byte[] serialize(T obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Hessian2Output out = new Hessian2Output(bos);
        out.writeObject(obj);
        out.close();
        return bos.toByteArray();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        Hessian2Input in = new Hessian2Input(bis);
        return (T) in.readObject(clazz);
    }
}
