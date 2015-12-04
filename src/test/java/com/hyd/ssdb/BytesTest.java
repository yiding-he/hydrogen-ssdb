package com.hyd.ssdb;

import com.hyd.ssdb.util.Bytes;
import org.junit.Test;

import java.util.Arrays;

/**
 * (description)
 * created at 15-12-2
 *
 * @author Yiding
 */
public class BytesTest {

    @Test
    public void testConcat() throws Exception {
        byte[] arr1 = {1, 2, 3};
        byte[] arr2 = {4, 5, 6};
        byte[] arr3 = {7, 8, 9};
        System.out.println(Arrays.toString(Bytes.concat(arr1, arr2, arr3)));
    }
}