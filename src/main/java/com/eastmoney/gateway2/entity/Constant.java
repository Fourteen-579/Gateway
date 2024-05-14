package com.eastmoney.gateway2.entity;

/**
 * 常量
 *
 * @author Fourteen_ksz
 * @version 1.0
 * @date 2023/8/15 10:45
 */
public class Constant {

    /**
     * 路由对应key值
     */
    public static final String ROUTER_KEY = "org.springframework.cloud.gateway.support.ServerWebExchangeUtils.gatewayRoute";

    /**
     * token存放的key
     */
    public static final String TOKEN_KEY = "token";

    /**
     * cookie在redis里面的key值前缀
     */
    public static final String COOKIE_WHITE_LIST_KEY = "cookie_white_list:";

    /**
     * 给请求设置cookies的key值
     */
    public static final String REQUEST_COOKIES_KEY = "gateway_cookies";

    /**
     * 获取请求头里面的cookie
     */
    public static final String COOKIES_KEY = "Cookie";

    /**
     * nacos中获取权重的key
     */
    public static final String SERVER_WEIGHT_KEY = "nacos.weight";

    /**
     * 请求存储key
     */
    public static final String USER_REQUEST_KEY = "user_request:";

    /**
     * 发送咚咚消息记录key
     */
    public static final String SEND_MESSAGE_LIMIT = "send_message_limit:";

    /**
     * 发送消息的分布式锁
     */
    public static final String LOCK_SEND_MESSAGE = "lock_send_message:";
    public static final String LOCK_SEND_MESSAGE_VALUE = "lock";

    /**
     * 各个平台不同存放token的key形式
     */
    public static final String QYT_TOKEN_FORMAT = "T@%s";
    public static final String CFH_TOKEN_FORMAT = "SFT@%s";
    public static final String BACKEND_TOKEN_FORMAT = "BET@%s";

}
