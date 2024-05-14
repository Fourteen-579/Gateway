package com.eastmoney.gateway2.choose;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.eastmoney.gateway2.client.GetUidByTokenClient;
import com.eastmoney.gateway2.entity.Constant;
import com.eastmoney.gateway2.tool.ArraysUtil;
import com.eastmoney.gateway2.tool.RandomUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.HOURS;

/**
 * @author Fourteen_ksz
 * @version 1.0
 * @date 2023/8/31 11:19
 */
@Slf4j
@Component
@RefreshScope
@RequiredArgsConstructor
public class BusinessChoose {

    private final JedisCluster jedisCluster;

    private final GetUidByTokenClient getUidByTokenClient;

    @Value("${qyt.publish.published.machine-ip}")
    private String publishMachineIp;

    @Value("${qyt.publish.published.web-uri}")
    private String publishWebUri;

    @Value("${qyt.publish.published.backend-uri}")
    private String publishBackendUri;

    @Value("${qyt.publish.not-publish.web-uri}")
    private String notPublishWebUri;

    @Value("${qyt.publish.not-publish.backend-uri}")
    private String notPublishBackendUri;

    @Value("${qyt.publish.white-uid}")
    private String whiteUid;

    /**
     * 根据业务逻辑返回选定的实例
     *
     * @param exchange  请求实例
     * @param instances 服务实例
     * @return
     */
    public List<ServiceInstance> selectInstanceByWhiteList(ServerWebExchange exchange, List<ServiceInstance> instances) {
        return getPublishService(chooseWhite(exchange), instances);
    }

    /**
     * 根据业务逻辑返回选定的实例
     *
     * @param exchange 请求实例
     * @return
     */
    public List<String> selectWebByWhiteList(ServerWebExchange exchange) {
        return getWebPublishService(chooseWhite(exchange));
    }

    /**
     * 根据业务逻辑返回选定的实例
     *
     * @param exchange 请求实例
     * @return
     */
    public List<String> selectBackendByWhiteList(ServerWebExchange exchange) {
        return getBackendPublishService(chooseWhite(exchange));
    }

    /**
     * 判断是否为白名单
     *
     * @param exchange 请求体
     * @return 是否为白名单
     */
    public Boolean chooseWhite(ServerWebExchange exchange) {
        if (ObjectUtil.isEmpty(exchange.getRequest()) || ObjectUtil.isEmpty(exchange.getRequest().getHeaders())) {
            log.error("负载均衡路由选择器-获取请求头失败！ServerWebExchange：{}", exchange);
            return false;
        }

        HttpHeaders headers = exchange.getRequest().getHeaders();
        Boolean isWhiteUser = false;

        if (headers.containsKey(Constant.COOKIES_KEY) && hasRequestCookies(headers)) {
            String cookieStr = null;
            List<String> cookieList = headers.get(Constant.COOKIES_KEY);
            if (CollUtil.isNotEmpty(cookieList)) {
                for (String cookie : cookieList) {
                    if (cookie.startsWith(Constant.REQUEST_COOKIES_KEY)) {
                        cookieStr = cookie.substring((Constant.REQUEST_COOKIES_KEY.length() + 1));
                        break;
                    }
                }
                String s = jedisCluster.get(Constant.COOKIE_WHITE_LIST_KEY + cookieStr);
                isWhiteUser = Boolean.valueOf(s);
            }
        } else if (headers.containsKey(Constant.TOKEN_KEY)) {
            isWhiteUser = isWhiteListUser(headers);
            //将是否为白名单的结果放入redis中
            String cookieStr = RandomUtils.generateRandomString(10);
            SetParams setParams = new SetParams();
            setParams.ex(7200);
            jedisCluster.set(Constant.COOKIE_WHITE_LIST_KEY + cookieStr, String.valueOf(isWhiteUser), setParams);

            //将cookie放入response
            ResponseCookie cookie = ResponseCookie.fromClientResponse(
                            Constant.REQUEST_COOKIES_KEY,
                            cookieStr)
                    .maxAge(Duration.of(2, HOURS))
                    .build();
            exchange.getResponse()
                    .addCookie(cookie);
        }

        return isWhiteUser;
    }

    /**
     * 判断请求中是否有cookie
     *
     * @param headers
     * @return
     */
    private boolean hasRequestCookies(HttpHeaders headers) {
        return Objects.requireNonNull(headers.get(Constant.COOKIES_KEY)).stream()
                .anyMatch(cookie -> cookie.startsWith(Constant.REQUEST_COOKIES_KEY));
    }

    /**
     * 根据条件获取服务
     *
     * @param getPublish 是否获取发版机器
     * @return
     */
    private List<String> getBackendPublishService(Boolean getPublish) {
        return getFontMachine(getPublish, publishBackendUri, notPublishBackendUri);
    }

    /**
     * 根据条件获取服务
     *
     * @param getPublish 是否获取发版机器
     * @return
     */
    private List<String> getWebPublishService(Boolean getPublish) {
        return getFontMachine(getPublish, publishWebUri, notPublishWebUri);
    }

    private List<String> getFontMachine(Boolean getPublish, String publishWebUri, String notPublishWebUri) {
        String[] machineList;
        if (getPublish) {
            machineList = publishWebUri.split(",");
        } else {
            machineList = notPublishWebUri.split(",");
        }

        if (ArraysUtil.isNullOrEmpty(machineList)) {
            log.info("负载均衡路由选择器-当前没有已发布机器！");
            return null;
        }

        return Arrays.asList(machineList);
    }

    /**
     * 根据条件获取服务
     *
     * @param getPublish 是否获取发版机器
     * @param instances
     * @return
     */
    private List<ServiceInstance> getPublishService(Boolean getPublish, List<ServiceInstance> instances) {
        List<ServiceInstance> result;

        String[] publishMachineList = publishMachineIp.split(",");
        if (ArraysUtil.isNullOrEmpty(publishMachineList)) {
            log.info("负载均衡路由选择器-当前没有已发布机器！");
            return Boolean.TRUE.equals(getPublish) ? null : instances;
        }

        List<String> publish = Arrays.asList(publishMachineList);

        result = instances.stream()
                .filter(instance -> ObjectUtil.equals(publish.contains(instance.getHost()), getPublish))
                .collect(Collectors.toList());

        return result;
    }

    /**
     * 根据当前token获取是否为白名单用户
     *
     * @param headers
     * @return
     */
    private Boolean isWhiteListUser(HttpHeaders headers) {
        String token = headers.get(Constant.TOKEN_KEY).get(0);

        String[] whiteUidList = whiteUid.split(",");
        if (ArraysUtil.isNullOrEmpty(whiteUidList)) {
            log.info("当前用户uid白名单列表为空！");
            return false;
        }

        List<String> whiteUids = Arrays.asList(whiteUidList);
        String uid = getUidByTokenClient.getUidByToken(token);
        if (!StringUtils.hasText(uid)) {
            log.error("负载均衡路由选择器-根据当前token：{}没有获取到对应用户uid", token);
            return false;
        }
        return whiteUids.contains(uid);
    }

}
