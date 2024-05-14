package com.eastmoney.gateway2;

import com.eastmoney.gateway2.client.SendDongMessageClient;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class Gateway2ApplicationTests {

    @Resource
    SendDongMessageClient sendDongMessageClient;

    @Test
    void contextLoads() {
//        sendDongMessageClient.sendMessage("127.0.0.0", "/user");
    }

}
