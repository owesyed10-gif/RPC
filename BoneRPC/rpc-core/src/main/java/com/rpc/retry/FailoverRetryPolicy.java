package com.rpc.retry;

/**
 * Failover失败转移重试策略
 * 调用失败自动切换到下一个可用节点重试，直到成功或耗尽重试次数
 * @author Syed
 */
public class FailoverRetryPolicy implements RetryPolicy {

    private final int maxRetries;

    public FailoverRetryPolicy(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    @Override
    public boolean shouldRetry(int currentRetry, Exception e) {
        return currentRetry < maxRetries;
    }

    @Override
    public int getMaxRetries() {
        return maxRetries;
    }
}
