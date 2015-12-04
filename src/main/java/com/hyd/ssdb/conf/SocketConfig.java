package com.hyd.ssdb.conf;

/**
 * (description)
 * created at 15-12-3
 *
 * @author Yiding
 */
public class SocketConfig {

    public static final int DEFAULT_SO_TIMEOUT = 1000;

    private int soTimeout = DEFAULT_SO_TIMEOUT;

    public int getSoTimeout() {
        return soTimeout;
    }

    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }
}
