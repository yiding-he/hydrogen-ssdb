package com.hyd.ssdb.util;

/**
 * 字节操作工具类
 * created at 15-12-2
 *
 * @author Yiding
 */
public class Bytes {

    /**
     * 组合多个 byte[] 数组
     *
     * @param byteArrays 要组合的数组
     *
     * @return 组合的结果
     */
    public static byte[] concat(byte[]... byteArrays) {
        int totalLength = 0;
        for (byte[] byteArray : byteArrays) {
            totalLength += byteArray.length;
        }

        byte[] result = new byte[totalLength];
        int counter = 0;
        for (byte[] byteArray : byteArrays) {
            System.arraycopy(byteArray, 0, result, counter, byteArray.length);
            counter += byteArray.length;
        }

        return result;
    }
}
