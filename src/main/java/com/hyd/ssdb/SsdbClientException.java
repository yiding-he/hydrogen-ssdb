package com.hyd.ssdb;

/**
 * (description)
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
