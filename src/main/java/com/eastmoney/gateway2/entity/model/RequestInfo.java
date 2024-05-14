package com.eastmoney.gateway2.entity.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author Fourteen_ksz
 * @version 1.0
 * @date 2023/10/20 11:27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestInfo {

    private String path;

    private String method;

    private Map<String, String> params;

    private String body;

    private String ipAddress;

    private LocalDateTime time;

}
