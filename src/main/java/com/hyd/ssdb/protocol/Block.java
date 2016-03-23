package com.hyd.ssdb.protocol;

import com.hyd.ssdb.SsdbException;
import com.hyd.ssdb.util.Bytes;

import java.io.UnsupportedEncodingException;

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

    public Block(String data) throws SsdbException {
        if (data == null || data.length() == 0) {
            this.data = new byte[0];
        } else {
            try {
                this.data = data.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new SsdbException(e);
            }
        }
    }

    /**
     * 生成要发送的字节串
     *
     * @return 要发送的字节串
     */
    public byte[] toBytes() {
        try {
            byte[] length = (String.valueOf(data.length) + "\n").getBytes("UTF-8");
            return Bytes.concat(length, data, new byte[]{'\n'});
        } catch (UnsupportedEncodingException e) {
            throw new SsdbException(e);
        }
    }

    @Override
    public String toString() {
        try {
            return new String(this.data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new SsdbException(e);
        }
    }
}
