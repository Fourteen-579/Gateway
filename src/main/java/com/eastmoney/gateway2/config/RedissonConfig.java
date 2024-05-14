package com.eastmoney.gateway2.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Fourteen_ksz
 * @version 1.0
 * @date 2023/11/20 16:52
 */
@Configuration
@Data
@Slf4j
public class RedissonConfig {
    @Value("${spring.redis.cluster.password}")
    private String password;
    @Value("#{'${spring.redis.cluster.nodes}'.split(',')}")
    private List<String> nodes;

    /**
     * 配置redisson --集群方式
     * Redisson是RedissonClient的实现类
     */
    @Bean(destroyMethod = "shutdown")
    public Redisson redisson() {
        List<String> clusterNodes = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            clusterNodes.add("redis://" + nodes.get(i));
        }
        Config config = new Config();
        ClusterServersConfig clusterServersConfig = config.useClusterServers()
                .addNodeAddress(clusterNodes.toArray(new String[0]));
        clusterServersConfig.setPassword(password);
        return (Redisson) Redisson.create(config);
    }

}
