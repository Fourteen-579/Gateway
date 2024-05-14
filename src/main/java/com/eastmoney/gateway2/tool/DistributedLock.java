package com.eastmoney.gateway2.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;

/**
 * @author Fourteen_ksz
 * @version 1.0
 * @date 2023/11/20 15:30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLock {

    private final JedisCluster jedisCluster;

    /**
     * 获取锁
     *
     * @param key        锁key
     * @param value      锁value
     * @param expireTime 锁过期时间 单位：毫秒
     * @return 是否成功获取锁
     */
    private boolean lock(String key, String value, int expireTime) {
        String result = jedisCluster.set(key, value, SetParams.setParams().nx().px(expireTime));
        return "OK".equals(result);
    }

    /**
     * 释放锁
     *
     * @param key   锁key
     * @param value 锁value
     */
    public void unlock(String key, String value) {
        String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        jedisCluster.eval(luaScript, 1, key, value);
    }

    /**
     * 3次机会获取锁
     *
     * @param lockKey    锁key
     * @param lockValue  锁value
     * @param expireTime 锁过期时间 单位毫秒
     * @return 是否成功获取锁
     */
    public Boolean getLock(String lockKey, String lockValue, int expireTime) {
        int maxRetryTimes = 3;
        int retryInterval = 100;

        int retryCount = 0;
        boolean locked = false;

        while (retryCount < maxRetryTimes) {
            try {
                locked = lock(lockKey, lockValue, expireTime);
                if (locked) {
                    break;
                } else {
                    Thread.sleep(retryInterval);
                    retryCount++;
                }
            } catch (Exception e) {
                log.warn("第{}次尝试获取锁{}失败", retryCount, lockKey);
            }
        }

        return locked;
    }
}
