package com.eastmoney.gateway2.choose;

import cn.hutool.core.collection.CollUtil;
import com.eastmoney.gateway2.entity.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * @author Fourteen_ksz
 * @version 1.0
 * @date 2023/8/30 14:14
 */
@Slf4j
@Component
public class LoadBalanceChoose {

    /**
     * 根据负载均衡算法选择一个实例
     *
     * @param instances
     * @return
     */
    public ServiceInstance choose(List<ServiceInstance> instances) {
        int totalWeight = instances.stream()
                .mapToInt(this::getWeight)
                .sum();

        int randomWeight = new Random().nextInt(totalWeight + 1);

        int currentWeight = 0;
        for (ServiceInstance instance : instances) {
            currentWeight += getWeight(instance);
            if (currentWeight >= randomWeight) {
                return instance;
            }
        }

        return null;
    }

    public String chooseStr(List<String> instances) {
        // 检查列表是否为空
        if (CollUtil.isEmpty(instances)) {
            log.error("根据负载均衡算法获取一个服务ip失败！传入为空");
            return null;
        }

        Random random = new Random();
        int randomIndex = random.nextInt(instances.size());

        return instances.get(randomIndex);
    }

    /**
     * 获取服务的权重
     *
     * @param instance
     * @return
     */
    private int getWeight(ServiceInstance instance) {
        String weightStr = instance.getMetadata()
                .getOrDefault(Constant.SERVER_WEIGHT_KEY, "1");

        return (int) Float.parseFloat(weightStr);
    }

}
