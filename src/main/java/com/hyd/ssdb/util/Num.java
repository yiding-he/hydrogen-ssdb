package com.hyd.ssdb.util;

/**
 * (description)
 * created at 17/01/18
 *
 * @author yidin
 */
public class Num {

    public static String ifNull(Long l, String value) {
        return l == null ? value : String.valueOf(l);
    }
}
