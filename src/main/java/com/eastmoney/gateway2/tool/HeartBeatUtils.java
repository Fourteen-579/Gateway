package com.eastmoney.gateway2.tool;

import cn.hutool.core.util.StrUtil;
import com.eastmoney.gateway2.client.SendDongMessageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Fourteen_ksz
 * @version 1.0
 * @date 2023/11/30 17:03
 */
@Slf4j
@Component
@RefreshScope
@RequiredArgsConstructor
public class HeartBeatUtils {

    private final DiscoveryClient discoveryClient;
    private final SendDongMessageClient sendDongMessageClient;

    @Value("${qyt.heart-beat.server}")
    private String server;

    @Value("${qyt.heart-beat.server-id}")
    private String serverId;

    @Value("${qyt.heart-beat.switch}")
    private Boolean switchCheck;


    /**
     * 检测服务是否正常运行(5s)
     */
    @Scheduled(fixedRate = 5000)
    public void checkHeartBeat() {
        if (switchCheck) {
            //获取所有nacos检测到的服务
            List<ServiceInstance> allInstances = discoveryClient.getInstances(serverId);
            List<String> aliveServer = allInstances.stream().map(item -> item.getHost() + ":" + item.getPort()).toList();

            //获取所有应该存活的服务
            String[] needAliveServer = server.split(";");

            //排查哪些没有存活
            String result = Arrays.stream(needAliveServer)
                    .filter(server -> !aliveServer.contains(server))
                    .collect(Collectors.joining(";"));

            if (StrUtil.isNotBlank(result)) {
                //发送咚咚消息
                sendDongMessageClient.sendHeartBeatMessage(result);
            }
        }
    }

}
