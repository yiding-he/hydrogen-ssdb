package com.hyd.ssdb;

/**
 * 与 Ssdb 有关的异常
 * created at 15-11-30
 *
 * @author Yiding
 */
public class SsdbException extends RuntimeException {

    protected static final ThreadLocal<SsdbException> EXCEPTION_HOLDER = new ThreadLocal<>();

    private String serverErrorCode;

    public SsdbException() {
        super(EXCEPTION_HOLDER.get());
        EXCEPTION_HOLDER.set(this);
    }

    public SsdbException(String message) {
        super(message, EXCEPTION_HOLDER.get());
        EXCEPTION_HOLDER.set(this);
    }

    public SsdbException(String message, Throwable cause) {
        super(message, cause);
        EXCEPTION_HOLDER.set(this);
    }

    public SsdbException(Throwable cause) {
        super(cause);
        EXCEPTION_HOLDER.set(this);
    }

    ////////////////////////////////////////////////////////////////

    public String getServerErrorCode() {
        return serverErrorCode;
    }

    public void setServerErrorCode(String serverErrorCode) {
        this.serverErrorCode = serverErrorCode;
    }
}
