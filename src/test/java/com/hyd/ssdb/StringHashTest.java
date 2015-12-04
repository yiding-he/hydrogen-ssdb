package com.hyd.ssdb;

/**
 * (description)
 * created at 15-12-3
 *
 * @author Yiding
 */
public class StringHashTest {

    public static void main(String[] args) {
        System.out.println("".hashCode());

        for (int i = 10000000; i < 10000005; i++) {
             System.out.println(String.valueOf(i).hashCode());
        }
    }

    private static int hashString(String str) {
        int h = 0;
        int len = str.length();
        if (len > 0) {
            int off = 0;
            char val[] = str.toCharArray();

            for (int i = 0; i < len; i++) {
                h = 31 * h + val[off++];
            }
        }
        return h;

    }
}
