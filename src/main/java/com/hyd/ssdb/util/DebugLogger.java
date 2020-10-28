package com.hyd.ssdb.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用于跟踪疑难问题的日志，因为只打印 trace 级别日志，所以大多数情况下是关闭的。
 * 想要启用疑难问题日志，需要将 <code>com.hyd.ssdb.trace</code> 级别调整为 trace
 */
public class DebugLogger {

    private static final Logger DEBUG_LOGGER = LoggerFactory.getLogger("com.hyd.ssdb.trace");

    public static void trace(String pattern, Object... params) {
        DEBUG_LOGGER.trace(pattern, params);
    }
}
