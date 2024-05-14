package com.eastmoney.gateway2.tool;

import java.util.Random;

/**
 * @author Fourteen_ksz
 * @version 1.0
 * @date 2023/8/18 14:26
 */
public class RandomUtils {

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /**
     * 生成随机字符串
     *
     * @param length 字符串长度
     * @return
     */
    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(CHARACTERS.length());
            char randomChar = CHARACTERS.charAt(index);
            sb.append(randomChar);
        }

        return sb.toString();
    }

}
