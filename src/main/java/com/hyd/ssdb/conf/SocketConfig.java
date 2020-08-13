package com.hyd.ssdb.conf;

import java.time.Duration;

/**
 * Socket 配置 created at 15-12-3
 *
 * @author Yiding
 */
public class SocketConfig {

    /**
     * 缺省 Socket 超时时间
     */
    public static final int DEFAULT_SO_TIMEOUT = 1000;

    /**
     * 缺省缓冲区大小
     */
    public static final int DEFAULT_SO_BUFFER_SIZE = 8 * 1024;

    //////////////////////////////////////////////////////////////

    private int soTimeout = DEFAULT_SO_TIMEOUT;         // 超时时间 (ms)

    private int soBufferSize = DEFAULT_SO_BUFFER_SIZE;  // 缓存大小 (byte)

    public SocketConfig() {
    }

    public SocketConfig(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    public SocketConfig(Duration duration, int soBufferSize) {
        this.soTimeout = Math.toIntExact(duration.toMillis());
        this.soBufferSize = soBufferSize;
    }

    public SocketConfig(int soTimeout, int soBufferSize) {
        this.soTimeout = soTimeout;
        this.soBufferSize = soBufferSize;
    }

    /**
     * 获取 Socket 超时时间
     *
     * @return Socket 超时时间
     */
    public int getSoTimeout() {
        return soTimeout;
    }

    /**
     * 设置 Socket 超时时间
     *
     * @param soTimeout Socket 超时时间
     */
    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }


    /**
     * 获取读取数据使用 Buffer 的大小
     *
     * @return Buffer 大小
     */
    public int getSoBufferSize() {
        return soBufferSize;
    }

    /**
     * 设置读取数据使用 Buffer 大小
     *
     * @param soBufferSize Buffer 大小
     */
    public void setSoBufferSize(int soBufferSize) {
        this.soBufferSize = soBufferSize;
    }

    @Override
    public String toString() {
        return "SocketConfig{" +
            "soTimeout=" + soTimeout +
            ", soBufferSize=" + soBufferSize +
            '}';
    }
}
