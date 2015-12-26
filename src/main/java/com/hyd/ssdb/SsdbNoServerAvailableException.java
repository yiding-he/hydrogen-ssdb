package com.hyd.ssdb;

/**
 * 无可用服务器异常。出现这个异常表示某个 Cluster
 * 中完全没有可用的服务器，无法提供相应的 key 的存取服务。
 *
 * created at 15-12-5
 *
 * @author Yiding
 */
public class SsdbNoServerAvailableException extends SsdbClientException {

    public SsdbNoServerAvailableException() {
    }

    public SsdbNoServerAvailableException(String message) {
        super(message);
    }

    public SsdbNoServerAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public SsdbNoServerAvailableException(Throwable cause) {
        super(cause);
    }
}
