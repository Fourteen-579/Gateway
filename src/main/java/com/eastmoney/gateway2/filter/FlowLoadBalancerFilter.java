package com.eastmoney.gateway2.filter;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.eastmoney.gateway2.choose.BusinessChoose;
import com.eastmoney.gateway2.choose.LoadBalanceChoose;
import com.eastmoney.gateway2.entity.Constant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;


/**
 * FlowLoadBalancer
 *
 * @author Fourteen_ksz
 * @version 1.0
 * @date 2023/7/26 9:45
 */
@Slf4j
@Component
@RefreshScope
@RequiredArgsConstructor
public class FlowLoadBalancerFilter implements GlobalFilter, Ordered {

    private final BusinessChoose businessChoose;

    private final DiscoveryClient discoveryClient;

    private final LoadBalanceChoose loadBalanceChoose;

    @Value("${qyt.publish.switch}")
    private Boolean publishSwitch;
    @Value("${qyt.nacos.request-server-id}")
    private String serverId;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (Boolean.FALSE.equals(publishSwitch)) {
            return chain.filter(exchange);
        }
        log.info("FlowLoadBalancer :Start");
        if (!exchange.getAttributes().containsKey(Constant.ROUTER_KEY)) {
            log.error("FlowLoadBalancer :No RouterKey");
            return chain.filter(exchange);
        }

        URI uri;
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/web")) {
            uri = getFontServiceInstance(0, exchange);
        } else if (path.startsWith("/backend")) {
            uri = getFontServiceInstance(1, exchange);
        } else {
            uri = getServiceInstance(exchange);
        }

        if (ObjectUtil.isEmpty(uri)) {
            return chain.filter(exchange);
        }

        //创建路由
        Route oldRouter = exchange.getAttribute(Constant.ROUTER_KEY);
        Route realRoute = Route.async()
                .asyncPredicate(oldRouter.getPredicate())
                .filters(oldRouter.getFilters())
                .id(oldRouter.getId())
                .order(oldRouter.getOrder())
                .uri(uri)
                .build();
        exchange.getAttributes().put(Constant.ROUTER_KEY, realRoute);

        log.info("FlowLoadBalancer : This selection is:{}", exchange.getAttributes().get(Constant.ROUTER_KEY));
        return chain.filter(exchange);
    }

    /**
     * 根据业务逻辑和负载均衡算法获取最终目标机器
     *
     * @param exchange
     * @return
     */
    private URI getServiceInstance(ServerWebExchange exchange) {
        List<ServiceInstance> allInstances = discoveryClient.getInstances(serverId);

        if (CollUtil.isEmpty(allInstances)) {
            log.error("FlowLoadBalancer -No usage service");
            return null;
        }

        List<ServiceInstance> instances = businessChoose.selectInstanceByWhiteList(exchange, allInstances);
        if (CollUtil.isEmpty(instances)) {
            log.error("FlowLoadBalancer -根据业务逻辑选择可用路由返回结果为空");
            instances = allInstances;
        }

        ServiceInstance result = loadBalanceChoose.choose(instances);
        if (ObjectUtils.isEmpty(result)) {
            log.error("FlowLoadBalancer -负载均衡选择可用路由返回结果为空");
        }

        if (ObjectUtil.isEmpty(result) || ObjectUtil.isEmpty(result.getUri())) {
            log.error("FlowLoadBalancer :The route selection result was empty");
            return null;
        }

        return result.getUri();
    }

    /**
     * 根据业务逻辑和负载均衡算法获取最终目标机器
     *
     * @param type     0-前台 1-后台
     * @param exchange
     * @return
     */
    private URI getFontServiceInstance(Integer type, ServerWebExchange exchange) {
        List<String> result;
        if (ObjectUtil.equals(type, 0)) {
            result = businessChoose.selectWebByWhiteList(exchange);
        } else {
            result = businessChoose.selectBackendByWhiteList(exchange);
        }


        if (CollUtil.isEmpty(result)) {
            log.error("FlowLoadBalancer -根据业务逻辑选择可用路由返回结果为空");
            return null;
        }

        String uriResult = loadBalanceChoose.chooseStr(result);
        if (StrUtil.isEmpty(uriResult)) {
            log.error("FlowLoadBalancer -负载均衡选择可用路由返回结果为空");
            return null;
        }

        return URI.create(uriResult);
    }

    /**
     * 设置过滤器的过滤顺序
     * 这里设置为第一顺序 只有在进行业务判断过滤后才会继续接下来的路由选择
     *
     * @return
     */
    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE + 1;
    }
}