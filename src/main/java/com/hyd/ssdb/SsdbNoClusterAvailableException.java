package com.hyd.ssdb;

/**
 * (description)
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
