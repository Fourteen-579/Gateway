package com.eastmoney.gateway2.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * @author Fourteen_ksz
 * @version 1.0
 * @date 2023/8/18 11:04
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.redis.lettuce.pool")
public class JedisPoolConf {
    private int maxActive;
    private int minIdle;
    private int maxIdle;
    private Duration maxWait;
    private Duration timeBetweenEvictionRuns;
    private boolean testOnBorrow;
}
