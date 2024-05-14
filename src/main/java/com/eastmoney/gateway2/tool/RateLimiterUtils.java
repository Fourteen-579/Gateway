package com.eastmoney.gateway2.tool;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;

import java.util.concurrent.TimeUnit;

/**
 * @author Fourteen_ksz
 * @version 1.0
 * @date 2023/10/20 9:23
 */
@Slf4j
@Component
@RefreshScope
@RequiredArgsConstructor
public class RateLimiterUtils {

    private final Redisson redisson;

    @Value("${qyt.limiter.ip-limit-num}")
    private Integer ipLimitNum;
    @Value("${qyt.limiter.interface-limit-num}")
    private Integer interfaceLimitNum;

    /**
     * 获取key对应限流器
     *
     * @param key              key
     * @param permitsPerSecond 每秒允许的令牌数量
     * @return 限流器
     */
    private RRateLimiter getRateLimiter(String key, int permitsPerSecond) {
        RRateLimiter rateLimiter = redisson.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL, permitsPerSecond, 1, RateIntervalUnit.SECONDS);

        return rateLimiter;
    }

    /**
     * 获取ip地址限流器的令牌
     *
     * @param ip      ip地址
     * @param timeout 超时时间 单位：毫秒
     * @return 是否成功获取令牌
     */
    public boolean getIpAcquire(String ip, long timeout) {
        if (StrUtil.isBlank(ip) || ObjectUtil.isEmpty(timeout)) {
            return false;
        }
        RRateLimiter rateLimiter = getRateLimiter(ip, ipLimitNum);
        return rateLimiter.tryAcquire(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取ip地址限流器的令牌
     *
     * @param ip ip地址
     * @return 是否成功获取令牌
     */
    public Boolean getIpAcquire(String ip) {
        return getIpAcquire(ip, 1000);
    }


    /**
     * 获取请求url限流器的令牌
     *
     * @param path    请求地址
     * @param timeout 超时时间 单位：毫秒
     * @return 是否成功获取到令牌
     */
    public Boolean getInterfaceAcquire(String path, long timeout) {
        if (StrUtil.isBlank(path) || ObjectUtil.isEmpty(timeout)) {
            return false;
        }

        RRateLimiter rateLimiter = getRateLimiter(path, interfaceLimitNum);
        return rateLimiter.tryAcquire(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取请求url限流器的令牌
     *
     * @param path 请求地址
     * @return 是否成功获取到令牌
     */
    public Boolean getInterfaceAcquire(String path) {
        return getInterfaceAcquire(path, 1000);
    }
}
