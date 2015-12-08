package com.hyd.ssdb.util;

/**
 * 表示数字范围
 * created at 15-12-8
 *
 * @author Yiding
 */
public class Range<T extends Number> {

    private T min;

    private T max;

    public Range(T min, T max) {
        this.min = min;
        this.max = max;
    }

    public T getMin() {
        return min;
    }

    public void setMin(T min) {
        this.min = min;
    }

    public T getMax() {
        return max;
    }

    public void setMax(T max) {
        this.max = max;
    }

    public boolean contains(T value) {
        if (value instanceof Integer) {
            return (Integer) this.min <= (Integer) value && (Integer) value <= (Integer) this.max;
        } else if (value instanceof Long) {
            return (Long) this.min <= (Long) value && (Long) value <= (Long) this.max;
        } else if (value instanceof Double) {
            return (Double) this.min <= (Double) value && (Double) value <= (Double) this.max;
        } else if (value instanceof Float) {
            return (Float) this.min <= (Float) value && (Float) value <= (Float) this.max;
        } else if (value instanceof Short) {
            return (Short) this.min <= (Short) value && (Short) value <= (Short) this.max;
        } else if (value instanceof Byte) {
            return (Byte) this.min <= (Byte) value && (Byte) value <= (Byte) this.max;
        } else {
            throw new UnsupportedOperationException("Type '" + value.getClass() + "' not supported.");
        }
    }
}
