package com.eastmoney.gateway2.entity.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Fourteen_ksz
 * @version 1.0
 * @date 2023/10/9 14:05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    /**
     * 应用平台
     */
    private String appkey;
    /**
     * 用户通行证id
     */
    private String uid;
    /**
     * 授权方式
     */
    private String type;
    /**
     * 授权创建时间
     */
    private long createTime;
    /**
     * 授权码
     */
    private String token;
    /**
     * 授权码有效期 单位秒
     */
    private long tokenAlive;
    /**
     * 授权刷新码
     */
    private String refreshToken;
    /**
     * 授权刷新码有效期 单位秒
     */
    private long refreshTokenAlive;

    private String machine;
    /**
     * 系统当前时间
     */
    private String time;

}
