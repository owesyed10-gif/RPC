package com.rpc.registry;

/**
 * 服务实例元信息：IP + 端口
 * @author Syed
 */
public class ServiceInstance {
    private String host;
    private int port;

    public ServiceInstance() {}

    public ServiceInstance(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    @Override
    public String toString() { return host + ":" + port; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceInstance)) return false;
        ServiceInstance that = (ServiceInstance) o;
        return port == that.port && host.equals(that.host);
    }

    @Override
    public int hashCode() { return 31 * host.hashCode() + port; }
}
