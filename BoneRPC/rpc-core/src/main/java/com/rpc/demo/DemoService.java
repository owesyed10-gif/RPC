package com.rpc.demo;

/**
 * 示例服务接口
 * 放在 rpc-core 中方便 provider 和 consumer 共享
 * @author Syed
 */
public interface DemoService {
    String sayHello(String name);
}
