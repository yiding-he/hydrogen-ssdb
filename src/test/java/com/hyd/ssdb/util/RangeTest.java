package com.hyd.ssdb.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RangeTest {

    @Test
    public void testContains() {
        Range<Long> longRange = new Range<>(10000000000L, 20000000000000L);
        assertTrue(longRange.contains(100000000000L));
        assertFalse(longRange.contains(1L));
        assertFalse(longRange.contains(null));
    }
}