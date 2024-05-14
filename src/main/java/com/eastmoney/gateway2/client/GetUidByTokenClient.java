package com.eastmoney.gateway2.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.eastmoney.gateway2.entity.model.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;

import static com.eastmoney.gateway2.entity.Constant.*;

/**
 * @author Fourteen_ksz
 * @version 1.0
 * @date 2023/7/26 9:59
 */
@Slf4j
@Component
@RefreshScope
@RequiredArgsConstructor
public class GetUidByTokenClient {

    private final JedisCluster jedisCluster;

    //选择返回0-白名单用户还是1-非白名单用户uid 或者是走2-正常逻辑
    @Value("${qyt.publish.choose-white-uid}")
    private Integer chooseWhiteUid;

    /**
     * 根据token获取用户uid
     *
     * @param token 请求token
     * @return 用户uid
     */
    public String getUidByToken(String token) {
        if (chooseWhiteUid.equals(0)) {
            return "4886476614151300";
        } else if (chooseWhiteUid.equals(1)) {
            return "2";
        }

        String resultUid = null;

        String[] formats = {QYT_TOKEN_FORMAT, CFH_TOKEN_FORMAT, BACKEND_TOKEN_FORMAT};
        for (String format : formats) {
            String result = jedisCluster.get(String.format(format, token));
            if (StrUtil.isNotBlank(result)) {
                UserInfo userInfo = JSONUtil.toBean(result, UserInfo.class, true);
                resultUid = userInfo.getUid();
                break;
            }
        }

        if (StrUtil.isBlank(resultUid)) {
            log.error("根据token获取用户uid失败，token：{}", token);
        }

        return resultUid;
    }

}
