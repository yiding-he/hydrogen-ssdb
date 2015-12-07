package com.hyd.ssdb;

/**
 * (description)
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
