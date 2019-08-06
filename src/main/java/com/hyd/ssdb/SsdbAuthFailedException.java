package com.hyd.ssdb;

public class SsdbAuthFailedException extends SsdbException {

    public SsdbAuthFailedException() {
    }

    public SsdbAuthFailedException(String message) {
        super(message);
    }

    public SsdbAuthFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public SsdbAuthFailedException(Throwable cause) {
        super(cause);
    }
}
