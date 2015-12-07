package com.hyd.ssdb;

/**
 * 表示发生在客户端的异常，包括服务器无法连接、收发失败或协议格式不正确等
 * created at 15-12-4
 *
 * @author Yiding
 */
public class SsdbClientException extends SsdbException {

    public SsdbClientException() {
    }

    public SsdbClientException(String message) {
        super(message);
    }

    public SsdbClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public SsdbClientException(Throwable cause) {
        super(cause);
    }
}
