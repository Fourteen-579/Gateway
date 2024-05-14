package com.eastmoney.gateway2.filter;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.eastmoney.gateway2.client.SendDongMessageClient;
import com.eastmoney.gateway2.entity.Response;
import com.eastmoney.gateway2.entity.model.RequestInfo;
import com.eastmoney.gateway2.tool.ArraysUtil;
import com.eastmoney.gateway2.tool.RateLimiterUtils;
import com.google.common.net.HttpHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import redis.clients.jedis.JedisCluster;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.regex.Pattern;

import static com.eastmoney.gateway2.entity.Constant.USER_REQUEST_KEY;

/**
 * @author Fourteen_ksz
 * @version 1.0
 * @date 2023/10/16 17:13
 */
@Slf4j
@Component
@RefreshScope
@RequiredArgsConstructor
public class RateLimiterFilter implements GlobalFilter, Ordered {

    private final JedisCluster jedisCluster;

    private final RateLimiterUtils rateLimiterUtils;

    private final SendDongMessageClient sendDongMessageClient;

    @Value("${qyt.limiter.switch}")
    private Boolean limiterSwitch;

    @Value("${qyt.limiter.interface-limiter}")
    private String interfaceLimiter;

    @Value("${qyt.limiter.interface-white-list}")
    private String interfaceWhiteList;

    @Value("${qyt.limiter.ip-white-list}")
    private String ipWhiteList;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (Boolean.FALSE.equals(limiterSwitch)) {
            return chain.filter(exchange);
        }

        String interfacePath = exchange.getRequest().getPath().value();
        if (StrUtil.isBlank(interfacePath)) {
            log.error("RateLimiterFilter-未获取到访问路径！");
            return chain.filter(exchange);
        }

        log.info("RateLimiterFilter-Start!Request interface:{}", interfacePath);

        String ipAddress = getIpAddress(exchange.getRequest());
        if (StrUtil.isBlank(ipAddress)) {
            log.error("RateLimiterFilter-获取请求方ip地址失败，请求参数：{}", exchange.getRequest());
            return chain.filter(exchange);
        }

        //请求信息存储
        saveRequestInfo(exchange.getRequest(), ipAddress);

        //判断是否为白名单接口
        if (judgeInterfaceWhiteList(interfacePath, interfaceWhiteList)) {
            log.info("RateLimiterFilter-该接口存在接口白名单中，直接返回！请求接口：{}", interfacePath);
            return chain.filter(exchange);
        }

        //判断是否为ip白名单
        if (judgeIpWhiteList(ipAddress, ipWhiteList)) {
            log.info("RateLimiterFilter-该ip存在ip白名单中，直接返回！请求接口：{}", interfacePath);
            return chain.filter(exchange);
        }

        //获取 ip地址 令牌
        if (!rateLimiterUtils.getIpAcquire(ipAddress)) {
            sendDongMessageClient.sendLimiterMessageAsync(ipAddress, interfacePath, 0);
            return getToManyRequestResponse(exchange.getResponse());
        }

        //判断是否为需要格外限制流量的接口
        if (judgeInterfaceWhiteList(interfacePath, interfaceLimiter) &&
                !rateLimiterUtils.getInterfaceAcquire(interfacePath)) {
            sendDongMessageClient.sendLimiterMessageAsync(ipAddress, interfacePath, 1);
            return getToManyRequestResponse(exchange.getResponse());
        }

        log.info("RateLimiterFilter-Success");
        return chain.filter(exchange);
    }

    /**
     * 判断传入地址是否匹配传入白名单中地址
     *
     * @param interfacePath 待确认地址
     * @param whitePathList 白名单地址
     * @return 结果
     */
    private Boolean judgeInterfaceWhiteList(String interfacePath, String whitePathList) {
        String[] split = whitePathList.split(",");
        if (ArraysUtil.isNullOrEmpty(split)) {
            return false;
        }

        for (String whitePath : split) {
            if (Pattern.matches(whitePath, interfacePath)) {
                log.info("RateLimiterFilter-该接口为白名单接口！请求接口：{}", interfacePath);
                return true;
            }
        }

        return false;
    }

    /**
     * 判断传入ip是否匹配传入白名单中ip
     *
     * @param ip          待确认ip
     * @param whiteIpList 白名单ip
     * @return 结果
     */
    private Boolean judgeIpWhiteList(String ip, String whiteIpList) {
        String[] split = whiteIpList.split(",");
        if (ArraysUtil.isNullOrEmpty(split)) {
            return false;
        }

        return Arrays.stream(split).anyMatch(item -> ObjectUtil.equals(ip, item));
    }

    /**
     * 获取请求太多的结果体
     *
     * @param response 结果
     * @return 结果体
     */
    private Mono<Void> getToManyRequestResponse(ServerHttpResponse response) {
        log.info("RateLimiterFilter-ToManyRequest!");

        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String responseBody = JSONUtil.toJsonStr(new Response("Too many requests", 429));
        return response.writeWith(Mono.just(response.bufferFactory().wrap(responseBody.getBytes())));
    }

    /**
     * 获取请求ip地址
     * TODO 如果有代理的话 暂时还不是真实的地址
     *
     * @param request 请求体
     * @return ip地址
     */
    private String getIpAddress(ServerHttpRequest request) {
        String ipAddress = request.getHeaders().getFirst(HttpHeaders.X_FORWARDED_FOR);
        if (StrUtil.isBlank(ipAddress)) {
            ipAddress = request.getRemoteAddress().getAddress().getHostAddress();
        }

        if (StrUtil.isNotBlank(ipAddress)) {
            ipAddress = ipAddress.replace(":", ".");
        }

        return ipAddress;
    }

    /**
     * 保存请求信息
     *
     * @param request   请求体
     * @param ipAddress ip地址
     */
    @Async
    protected void saveRequestInfo(ServerHttpRequest request, String ipAddress) {
        RequestInfo requestInfo = new RequestInfo();

        requestInfo.setIpAddress(ipAddress);
        requestInfo.setTime(LocalDateTime.now());
        requestInfo.setPath(request.getPath().value());
        requestInfo.setMethod(request.getMethod().name());
        requestInfo.setParams(request.getQueryParams().toSingleValueMap());

        //从body中获取请求体较为复杂 略过这一步 如果之后有需求再添加
        //requestInfo.setBody(resolveBodyFromRequest(request));

        String jsonString = JSONUtil.toJsonStr(requestInfo);

        String key = USER_REQUEST_KEY + getCurrentDateStr() + ":" + ipAddress;
        if (!jedisCluster.exists(key)) {
            jedisCluster.rpush(key, jsonString);
            setKeyExpiration(key);
        } else {
            jedisCluster.rpush(key, jsonString);
        }
    }

    /**
     * 获取请求体中信息
     *
     * @param request 请求
     * @return 请求体中信息
     */
    private String resolveBodyFromRequest(ServerHttpRequest request) {
        return null;
    }

    /**
     * 设置key的过期时间为当天的最后
     *
     * @param key 待设置key
     */
    private void setKeyExpiration(String key) {
        LocalDate currentDate = LocalDate.now();
        LocalTime expirationTime = LocalTime.of(23, 59, 59);
        LocalDateTime expirationDateTime = LocalDateTime.of(currentDate, expirationTime);
        long expirationTimestamp = expirationDateTime.toEpochSecond(ZoneOffset.UTC);
        jedisCluster.expireAt(key, expirationTimestamp);
    }

    /**
     * 获取当天的日期格式化后字符串
     *
     * @return 日期格式化
     */
    private String getCurrentDateStr() {
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return now.format(formatter);
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
