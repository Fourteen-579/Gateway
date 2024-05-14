package com.eastmoney.gateway2.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Fourteen_ksz
 * @version 1.0
 * @date 2023/10/20 14:58
 */
@Data
@AllArgsConstructor
public class Response {

    private String message;

    private Integer code;

}
