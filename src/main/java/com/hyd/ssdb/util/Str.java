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
            if (value != null && value.trim().length() > 0) {
                return value;
            }
        }

        return "";
    }
}
