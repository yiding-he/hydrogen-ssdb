package com.hyd.ssdb;

/**
 * 表示所有的 Cluster 都失效，此时 SsdbClient 无法执行任何操作
 * created at 15-12-8
 *
 * @author Yiding
 */
public class SsdbNoClusterAvailableException extends SsdbClientException {

    public SsdbNoClusterAvailableException() {
    }

    public SsdbNoClusterAvailableException(String message) {
        super(message);
    }

    public SsdbNoClusterAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public SsdbNoClusterAvailableException(Throwable cause) {
        super(cause);
    }
}
