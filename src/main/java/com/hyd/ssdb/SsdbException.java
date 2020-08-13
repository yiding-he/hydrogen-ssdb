package com.hyd.ssdb;

/**
 * 与 Ssdb 有关的异常
 * created at 15-11-30
 *
 * @author Yiding
 */
public class SsdbException extends RuntimeException {

    /**
     * 当需要重试时，上次的失败信息暂存在这里。
     */
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

    /**
     * 清理可能遗留在 ThreadLocal 中的异常信息
     */
    public static void clearThreadLocal() {
        EXCEPTION_HOLDER.remove();
    }

    public String getServerErrorCode() {
        return serverErrorCode;
    }

    public void setServerErrorCode(String serverErrorCode) {
        this.serverErrorCode = serverErrorCode;
    }
}
