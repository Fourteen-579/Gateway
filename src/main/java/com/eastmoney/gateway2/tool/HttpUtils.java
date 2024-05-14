package com.eastmoney.gateway2.tool;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.util.Dictionary;
import java.util.Enumeration;

/**
 * @author Fourteen_ksz
 * @version 1.0
 * @date 2023/11/20 10:46
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpUtils {

    private static final String JSON_TYPE = "application/json; charset=utf-8";

    private final OkHttpClient client;

    public String post(String url, String jsonParam, Dictionary<String, String> headers) {
        String result = null;
        //请求参数
        RequestBody requestBody = FormBody.create(MediaType.parse(JSON_TYPE), jsonParam);
        Request.Builder requestBuilder = new Request.Builder().url(url).post(requestBody);
        //添加请求头
        if (headers != null) {
            for (Enumeration<?> keys = headers.keys(); keys.hasMoreElements(); ) {
                Object key = keys.nextElement();
                Object value = headers.get(key);
                requestBuilder.addHeader(key.toString(), value.toString());
            }
        }
        Request request = requestBuilder.build();
        try (Response response = client.newCall(request).execute()) {
            if (response != null && response.code() == 200 && response.body() != null) {
                result = response.body().string();
            } else {
                log.error("HttpUtils -> post error, url={}, param={}, header={}, response={}", url, jsonParam, JSONUtil.toJsonStr(headers), response == null ? "" : response.toString());
            }
        } catch (Exception e) {
            log.error("HttpUtils -> post error, url={}, json={}, header={}, error={}", url, jsonParam, JSONUtil.toJsonStr(headers), e.getMessage(), e);
        }
        return result;
    }

}
