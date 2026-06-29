package com.rpc.demo;

/**
 * DemoService 实现类
 * @author Syed
 */
public class DemoServiceImpl implements DemoService {

    @Override
    public String sayHello(String name) {
        return "Hello, " + name + "! [from RPC server @ " +
                System.currentTimeMillis() + "]";
    }
}
