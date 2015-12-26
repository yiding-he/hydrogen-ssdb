package com.hyd.ssdb.conf;

/**
 * Socket 配置 created at 15-12-3
 *
 * @author Yiding
 */
public class SocketConfig {

    public static final int DEFAULT_SO_TIMEOUT = 1000;

    private int soTimeout = DEFAULT_SO_TIMEOUT;

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
}
