package com.hyd.ssdb;

/**
 * (description)
 * created at 15-11-30
 *
 * @author Yiding
 */
public class SsdbException extends RuntimeException {

    private String serverErrorCode;

    public String getServerErrorCode() {
        return serverErrorCode;
    }

    public void setServerErrorCode(String serverErrorCode) {
        this.serverErrorCode = serverErrorCode;
    }

    ////////////////////////////////////////////////////////////////

    public SsdbException() {
    }

    public SsdbException(String message) {
        super(message);
    }

    public SsdbException(String message, Throwable cause) {
        super(message, cause);
    }

    public SsdbException(Throwable cause) {
        super(cause);
    }
}
