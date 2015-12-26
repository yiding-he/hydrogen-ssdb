package com.hyd.ssdb;

/**
 * 与 Ssdb 有关的异常
 * created at 15-11-30
 *
 * @author Yiding
 */
public class SsdbException extends RuntimeException {

    private String serverErrorCode;

    public SsdbException() {
    }

    public SsdbException(String message) {
        super(message);
    }

    ////////////////////////////////////////////////////////////////

    public SsdbException(String message, Throwable cause) {
        super(message, cause);
    }

    public SsdbException(Throwable cause) {
        super(cause);
    }

    public String getServerErrorCode() {
        return serverErrorCode;
    }

    public void setServerErrorCode(String serverErrorCode) {
        this.serverErrorCode = serverErrorCode;
    }
}
