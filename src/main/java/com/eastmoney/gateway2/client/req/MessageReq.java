package com.eastmoney.gateway2.client.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Fourteen_ksz
 * @version 1.0
 * @date 2023/11/20 10:55
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageReq {

    String title;

    String receiver;

    String content;

    String type;

}
