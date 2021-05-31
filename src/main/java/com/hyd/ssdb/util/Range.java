package com.hyd.ssdb.util;

/**
 * 表示数字范围
 * created at 15-12-8
 *
 * @author Yiding
 */
public class Range<T extends Number & Comparable<T>> {

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
        return value != null &&
            min.compareTo(value) <= 0 && max.compareTo(value) >= 0;
    }

    public Range<T> duplicate() {
        return new Range<>(min, max);
    }

    @Override
    public String toString() {
        return "Range{" +
            "min=" + min +
            ", max=" + max +
            '}';
    }
}
