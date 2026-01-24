package com.ntu.lottery.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RedisClientProperties.class)
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisClientProperties p) {
        String addr = String.format("redis://%s:%d", p.getHost(), p.getPort());

        int poolSize = p.getPoolSize() <= 0 ? 10 : p.getPoolSize();
        int minIdle = p.getMinIdleSize() <= 0 ? Math.min(5, poolSize) : p.getMinIdleSize();
        if (minIdle > poolSize) {
            // Prevent: connectionPoolSize can't be lower than connectionMinimumIdleSize
            minIdle = poolSize;
        }

        Config config = new Config();
        config.setCodec(new JsonJacksonCodec());

        SingleServerConfig single = config.useSingleServer()
                .setAddress(addr)
                .setConnectionPoolSize(poolSize)
                .setConnectionMinimumIdleSize(minIdle);

        return Redisson.create(config);
    }
}
