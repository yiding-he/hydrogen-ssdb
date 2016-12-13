package com.hyd.ssdb.protocol;

import com.hyd.ssdb.SsdbException;
import com.hyd.ssdb.util.Bytes;

import java.io.UnsupportedEncodingException;

import static com.hyd.ssdb.protocol.ProtocolConfig.DEFAULT_CHARSET;

/**
 * 一个区块。当发送区块时，按照 '长度\n内容\n' 的格式发送
 * created at 15-11-30
 *
 * @author Yiding
 */
public class Block {

    private String charset = DEFAULT_CHARSET;

    private byte[] data;

    public Block(byte[] data) {
        this.data = data;
    }

    public Block(String data) throws SsdbException {
        if (data == null || data.length() == 0) {
            this.data = new byte[0];
        } else {
            try {
                this.data = data.getBytes(this.charset);
            } catch (UnsupportedEncodingException e) {
                throw new SsdbException(e);
            }
        }
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    /**
     * 生成要发送的字节串
     *
     * @return 要发送的字节串
     */
    public byte[] toBytes() {
        try {
            byte[] length = (String.valueOf(data.length) + "\n").getBytes(this.charset);
            return Bytes.concat(length, data, new byte[]{'\n'});
        } catch (UnsupportedEncodingException e) {
            throw new SsdbException(e);
        }
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        try {
            return new String(this.data, this.charset);
        } catch (UnsupportedEncodingException e) {
            throw new SsdbException(e);
        }
    }
}
