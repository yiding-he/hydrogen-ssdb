package com.hyd.ssdb.util;

/**
 * (description)
 * created at 17/01/18
 *
 * @author yidin
 */
public class Str {

    public static String ifBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }

        return "";
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().length() == 0;
    }
}
