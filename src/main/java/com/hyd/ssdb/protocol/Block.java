package com.hyd.ssdb.protocol;

import com.hyd.ssdb.util.Bytes;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 一个区块。当发送区块时，按照 '长度\n内容\n' 的格式发送
 * created at 15-11-30
 *
 * @author Yiding
 */
public class Block {

    private byte[] data;

    public Block(byte[] data) {
        this.data = data;
    }

    /**
     * 生成要发送的字节串
     *
     * @return 要发送的字节串
     */
    public byte[] toBytes() {
        byte[] length = (data.length + "\n").getBytes();
        return Bytes.concat(length, data, new byte[]{'\n'});
    }

    public byte[] getData() {
        return data;
    }

    public String toString(Charset charset) {
        return new String(this.data, charset);
    }

    @Override
    public String toString() {
        return toString(StandardCharsets.UTF_8);
    }
}
