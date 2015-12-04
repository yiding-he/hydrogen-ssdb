package com.hyd.ssdb;

/**
 * (description)
 * created at 15-12-4
 *
 * @author Yiding
 */
public class SsdbServerException extends SsdbException {

    public SsdbServerException() {
    }

    public SsdbServerException(String message) {
        super(message);
    }

    public SsdbServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public SsdbServerException(Throwable cause) {
        super(cause);
    }
}
