package com.ntu.lottery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bind to application-*.yml:
 * redis:
 *   sdk:
 *     config:
 *       host: 127.0.0.1
 *       port: 6379
 *       pool-size: 10
 *       min-idle-size: 5
 */
@ConfigurationProperties(prefix = "redis.sdk.config")
public class RedisClientProperties {

    private String host = "127.0.0.1";
    private int port = 6379;
    private int poolSize = 10;
    private int minIdleSize = 5;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getMinIdleSize() {
        return minIdleSize;
    }

    public void setMinIdleSize(int minIdleSize) {
        this.minIdleSize = minIdleSize;
    }
}
