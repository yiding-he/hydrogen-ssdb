package com.hyd.ssdb.util;

import java.util.Arrays;

/**
 * 键值对
 * created at 15-12-3
 *
 * @author Yiding
 */
public class KeyValue {

    private String key;

    private byte[] value;

    public KeyValue(String key, String value) {
        this.key = key;
        this.value = value.getBytes();
    }

    public KeyValue(String key, byte[] value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public byte[] getValueBytes() {
        return value;
    }

    public String getValue() {
        return new String(value);
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "{" + this.key + "=" + Arrays.toString(this.value) + "}";
    }
}
