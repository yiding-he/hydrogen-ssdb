package com.hyd.ssdb;

/**
 * 与 Socket 有关的异常，通常出现在创建连接的时候或通信过程中。
 * created at 15-12-4
 *
 * @author Yiding
 */
public class SsdbSocketFailedException extends SsdbClientException {

    public SsdbSocketFailedException() {
    }

    public SsdbSocketFailedException(String message) {
        super(message);
    }

    public SsdbSocketFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public SsdbSocketFailedException(Throwable cause) {
        super(cause);
    }
}
