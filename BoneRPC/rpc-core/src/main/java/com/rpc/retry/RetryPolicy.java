package com.rpc.retry;

/**
 * 重试策略接口：控制RPC调用失败后的重试行为
 * @author Syed
 */
public interface RetryPolicy {
    /** 判断是否应继续重试 */
    boolean shouldRetry(int currentRetry, Exception e);
    /** 最大重试次数 */
    int getMaxRetries();
}
