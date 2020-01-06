package com.hyd.ssdb.util;

import com.hyd.ssdb.AbstractClient;
import java.nio.charset.Charset;

/**
 * 键值对
 * created at 15-12-3
 *
 * @author Yiding
 */
public class KeyValue {

    private Charset charset;

    private byte[] key;

    private byte[] value;

    public KeyValue(byte[] key, byte[] value, Charset charset) {
        this.key = key;
        this.value = value;
        this.charset = charset;
    }

    public KeyValue(String key, byte[] value, Charset charset) {
        this((charset == null? key.getBytes(): key.getBytes(charset)), value, charset);
    }

    public KeyValue(String key, String value) {
        this(key, value, AbstractClient.DEFAULT_CHARSET);
    }

    public KeyValue(String key, String value, Charset charset) {
        this(key.getBytes(charset), value.getBytes(charset), charset);
    }

    public byte[] getKey() {
        return key;
    }

    public String getKeyString() {
        return this.charset == null? new String(this.key) :  new String(this.key, this.charset);
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    public byte[] getValue() {
        return value;
    }

    public String getValueString() {
        return new String(this.value, this.charset);
    }

    public void setValue(byte[] value) {
        this.value = value;
    }
}
