package com.eastmoney.gateway2.client;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.eastmoney.gateway2.client.req.MessageReq;
import com.eastmoney.gateway2.tool.DistributedLock;
import com.eastmoney.gateway2.tool.HttpUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;

import java.util.Dictionary;
import java.util.Hashtable;

import static com.eastmoney.gateway2.entity.Constant.*;

/**
 * @author Fourteen_ksz
 * @version 1.0
 * @date 2023/11/20 10:41
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendDongMessageClient {

    private final HttpUtils httpUtils;

    private final JedisCluster jedisCluster;

    private final DistributedLock distributedLock;

    @Value("${send-message.token}")
    private String token;

    @Value("${send-message.type}")
    private String type;

    @Value("${send-message.url}")
    private String url;

    @Value("${send-message.limiter-receiver}")
    private String limiterReceiver;

    @Value("${send-message.heart-beat}")
    private String heartReceiver;

    /**
     * 限流通知
     *
     * @param ip      请求ip地址
     * @param urlPath 请求url地址
     * @param type    发送消息类型 0-ip限制 1-接口限制
     */
    @Async
    public void sendLimiterMessageAsync(String ip, String urlPath, Integer type) {
        String lockKey = LOCK_SEND_MESSAGE + ip + ":" + urlPath;

        boolean locked = distributedLock.getLock(lockKey, LOCK_SEND_MESSAGE_VALUE, 5000);
        if (locked) {
            try {
                //添加限制 ip+地址 3分钟内只会告警一次
                String key = SEND_MESSAGE_LIMIT + ip + ":" + urlPath;
                if (jedisCluster.exists(key)) {
                    return;
                }

                sendMessage(ip, urlPath, type);

                jedisCluster.set(key, "true", SetParams.setParams().px(180000));
            } finally {
                distributedLock.unlock(lockKey, LOCK_SEND_MESSAGE_VALUE);
            }
        } else {
            log.warn("发送限流通知失败-获取锁失败！ip:{},urlPath:{}", ip, urlPath);
        }
    }

    /**
     * 发送消息
     *
     * @param ip          请求ip
     * @param urlPath     请求地址
     * @param messageType 发送消息类型 0-ip限制 1-接口限制
     */
    private void sendMessage(String ip, String urlPath, Integer messageType) {
        String message = """
                ip地址：%s
                访问接口:%s
                %s访问次数超过限制！""";

        String format = String.format(message, ip, urlPath, ObjectUtil.equals(messageType, 0) ? "ip" : "接口");

        sendMessage("限流告警", format, limiterReceiver);
    }

    /**
     * 发送心跳检测结果
     *
     * @param ip 需要发送的ip地址
     */
    public void sendHeartBeatMessage(String ip) {
        String message = """
                ip地址：%s
                心跳检测到服务不跳了！""";

        String format = String.format(message, ip);

        sendMessage("心跳检测", format, heartReceiver);
    }

    /**
     * 发送咚咚消息
     *
     * @param title    标题
     * @param content  内容
     * @param receiver 接收人
     */
    private void sendMessage(String title, String content, String receiver) {
        MessageReq messageReq = new MessageReq(title, receiver, content, type);

        String jsonParam = JSONUtil.toJsonStr(messageReq);

        Dictionary<String, String> headers = new Hashtable<>();
        headers.put("token", token);

        httpUtils.post(url, jsonParam, headers);
    }

}
