package com.eastmoney.gateway2.config;

import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Connection;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "spring.redis.cluster")
public class RedisClusterConfig {
    private String nodes;
    private int maxRedirects;
    private String password;
    private Duration connectionTimeout;
    private Duration soTimeout;

    @Resource
    private JedisPoolConf jedisPoolConf;


    @Bean(name = "JedisCluster", destroyMethod = "close")
    public JedisCluster getJedisCluster() {
        String[] nodeArray = nodes.split(",");
        Set<HostAndPort> hostAndPortSet = new HashSet<>(nodeArray.length);
        int count = 0;
        for (String node : nodeArray) {
            String[] address = node.split(":");
            hostAndPortSet.add(new HostAndPort(address[0], Integer.parseInt(address[1])));
            log.info("cluster node[{}] host:{}, port:{}", ++count, address[0], address[1]);
        }
        GenericObjectPoolConfig<Connection> config = new GenericObjectPoolConfig<>();

        config.setMaxTotal(jedisPoolConf.getMaxActive());
        config.setMaxIdle(jedisPoolConf.getMaxIdle());
        config.setMinIdle(jedisPoolConf.getMinIdle());
        config.setMaxWait(jedisPoolConf.getMaxWait());
        config.setTimeBetweenEvictionRuns(jedisPoolConf.getTimeBetweenEvictionRuns());
        config.setTestOnBorrow(jedisPoolConf.isTestOnBorrow());

        JedisCluster jedisCluster;
        if (StringUtils.isBlank(password)) {
            jedisCluster = new JedisCluster(hostAndPortSet, (int) connectionTimeout.toMillis(), (int) soTimeout.toMillis(), maxRedirects, config);
        } else {
            jedisCluster = new JedisCluster(hostAndPortSet, (int) connectionTimeout.toMillis(), (int) soTimeout.toMillis(), maxRedirects, password, config);
        }
        log.info("redis-cluster load success, jedisConfig:{}", config);
        return jedisCluster;
    }
}
