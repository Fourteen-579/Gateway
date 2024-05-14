package com.eastmoney.gateway2.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class OkHttpConfig {

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS) // 连接超时时间
                .readTimeout(10, TimeUnit.SECONDS) // 读取超时时间
                .writeTimeout(10, TimeUnit.SECONDS) // 写入超时时间
                .build();
    }
}
