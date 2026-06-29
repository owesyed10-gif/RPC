package com.rpc.common;

import java.io.Serializable;
import java.util.Arrays;

/**
 * RPC请求实体：封装一次远程调用的完整信息
 * 包含服务名、方法名、参数类型和参数值，服务端收到后通过反射执行
 * @author Syed
 */
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 服务接口全限定名，如 com.rpc.demo.DemoService */
    private String serviceName;
    /** 目标方法名 */
    private String methodName;
    /** 方法参数类型数组，用于反射定位重载方法 */
    private Class<?>[] parameterTypes;
    /** 方法参数值数组 */
    private Object[] parameters;

    public RpcRequest() {}

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public Class<?>[] getParameterTypes() { return parameterTypes; }
    public void setParameterTypes(Class<?>[] parameterTypes) { this.parameterTypes = parameterTypes; }

    public Object[] getParameters() { return parameters; }
    public void setParameters(Object[] parameters) { this.parameters = parameters; }

    @Override
    public String toString() {
        return String.format("RpcRequest[service=%s, method=%s, args=%s]",
                serviceName, methodName, Arrays.toString(parameters));
    }
}
