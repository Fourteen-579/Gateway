package com.eastmoney.gateway2.tool;

/**
 * @author Fourteen_ksz
 * @version 1.0
 * @date 2023/8/29 14:54
 */
public class ArraysUtil {

    /**
     * 判断数组是否为null或者为空
     *
     * @param arrays
     * @return
     */
    public static boolean isNullOrEmpty(Object[] arrays) {
        return arrays == null || arrays.length < 1;
    }

}
