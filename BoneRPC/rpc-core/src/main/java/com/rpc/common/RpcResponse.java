package com.rpc.common;

import java.io.Serializable;

/**
 * RPC响应实体：封装远程调用的返回结果或异常信息
 * isSuccess() 判断调用是否成功：errorMessage 为空即成功
 * @author Syed
 */
public class RpcResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 调用成功时的返回值 */
    private Object result;
    /** 调用失败时的异常信息 */
    private String errorMessage;

    public RpcResponse() {}

    public boolean isSuccess() { return errorMessage == null; }

    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    @Override
    public String toString() {
        return isSuccess() ? "RpcResponse[result=" + result + "]"
                : "RpcResponse[error=" + errorMessage + "]";
    }
}
